package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rental.config.DeepSeekConfig;
import com.rental.entity.AiConversation;
import com.rental.mapper.AiConversationMapper;
import com.rental.service.DeepSeekService;
import okhttp3.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DeepSeekServiceImpl implements DeepSeekService {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekServiceImpl.class);


    @Autowired
    private DeepSeekConfig deepSeekConfig;

    @Autowired
    private OkHttpClient okHttpClient;

    @Autowired
    private AiConversationMapper aiConversationMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = "你是专业的租房法律顾问，精通房屋租赁相关法律法规。请基于中国法律为用户提供专业、准确的法律咨询建议。回答时请引用相关法律条文。";

    @Override
    public Map<String, String> chat(String question, String conversationId, Long userId) {
        try {
            if (deepSeekConfig.getApiKey() == null || deepSeekConfig.getApiKey().isBlank()) {
                throw new RuntimeException("未配置 DeepSeek API Key，请在环境变量 DEEPSEEK_API_KEY 中配置");
            }
            if (conversationId == null || conversationId.isBlank()) {
                conversationId = UUID.randomUUID().toString();
            }

            // 组装多轮对话历史（最近10条）
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
            messages.addAll(loadHistory(userId, conversationId));
            messages.add(Map.of("role", "user", "content", question));

            String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", deepSeekConfig.getModel(),
                "messages", messages,
                "temperature", 0.7
            ));

            String url = deepSeekConfig.getBaseUrl() + "/chat/completions";
            log.info("发送DeepSeek请求: url={}", url);

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("DeepSeek API请求失败: status={}, body={}", response.code(), errorBody);
                    throw new RuntimeException("AI服务请求失败: " + response.code());
                }

                String responseBody = response.body().string();
                Map<String, Object> resultMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) resultMap.get("choices");
                if (choices == null || choices.isEmpty()) {
                    throw new RuntimeException("AI服务返回结果为空");
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String answer = (String) message.get("content");

                // 保存对话记录
                AiConversation conversation = new AiConversation();
                conversation.setUserId(userId);
                conversation.setQuestion(question);
                conversation.setAnswer(answer);
                conversation.setConversationId(conversationId);
                aiConversationMapper.insert(conversation);

                log.info("DeepSeek对话完成: conversationId={}", conversationId);
                return Map.of("answer", answer, "conversationId", conversationId);
            }
        } catch (IOException e) {
            log.error("DeepSeek API调用异常", e);
            throw new RuntimeException("AI服务调用失败，请稍后重试");
        }
    }

    /**
     * 加载指定会话的最近对话历史，按时间正序返回（旧在前）。
     */
    private List<Map<String, String>> loadHistory(Long userId, String conversationId) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getConversationId, conversationId)
                .orderByDesc(AiConversation::getId)
                .last("LIMIT 10");
        List<AiConversation> records = aiConversationMapper.selectList(wrapper);
        Collections.reverse(records);
        List<Map<String, String>> history = new ArrayList<>();
        for (AiConversation record : records) {
            history.add(Map.of("role", "user", "content", record.getQuestion()));
            if (record.getAnswer() != null) {
                history.add(Map.of("role", "assistant", "content", record.getAnswer()));
            }
        }
        return history;
    }

    @Override
    public List<Map<String, Object>> listConversations(Long userId) {
        List<AiConversation> records = aiConversationMapper.selectList(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .orderByAsc(AiConversation::getId));
        // 按会话 ID 聚合（记录按 id 升序，首条即最早问题）
        Map<String, List<AiConversation>> groups = new LinkedHashMap<>();
        for (AiConversation record : records) {
            if (record.getConversationId() == null || record.getConversationId().isBlank()) {
                continue;
            }
            groups.computeIfAbsent(record.getConversationId(), k -> new ArrayList<>()).add(record);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<AiConversation>> entry : groups.entrySet()) {
            List<AiConversation> list = entry.getValue();
            String question = list.get(0).getQuestion();
            Map<String, Object> item = new HashMap<>();
            item.put("conversationId", entry.getKey());
            item.put("title", question != null && question.length() > 30 ? question.substring(0, 30) : question);
            item.put("messageCount", list.size());
            item.put("lastTime", list.get(list.size() - 1).getCreatedAt());
            result.add(item);
        }
        // 按最后活动时间倒序
        result.sort((a, b) -> String.valueOf(b.get("lastTime")).compareTo(String.valueOf(a.get("lastTime"))));
        return result;
    }

    @Override
    public List<Map<String, Object>> getConversationMessages(String conversationId, Long userId) {
        List<AiConversation> records = aiConversationMapper.selectList(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .eq(AiConversation::getConversationId, conversationId)
                        .orderByAsc(AiConversation::getId));
        List<Map<String, Object>> messages = new ArrayList<>();
        for (AiConversation record : records) {
            Map<String, Object> question = new HashMap<>();
            question.put("role", "user");
            question.put("content", record.getQuestion());
            question.put("time", record.getCreatedAt());
            messages.add(question);
            if (record.getAnswer() != null) {
                Map<String, Object> answer = new HashMap<>();
                answer.put("role", "assistant");
                answer.put("content", record.getAnswer());
                answer.put("time", record.getCreatedAt());
                messages.add(answer);
            }
        }
        return messages;
    }

    @Override
    public void deleteConversation(String conversationId, Long userId) {
        aiConversationMapper.delete(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .eq(AiConversation::getConversationId, conversationId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> reviewContract(String contractContent) {
        try {
            if (deepSeekConfig.getApiKey() == null || deepSeekConfig.getApiKey().isBlank()) {
                throw new RuntimeException("未配置 DeepSeek API Key，请在环境变量 DEEPSEEK_API_KEY 中配置");
            }
            // 首次解析失败时自动重试 1 次，避免偶发的非结构化输出导致误判
            Map<String, Object> result = reviewOnce(contractContent);
            if (result == null) {
                log.warn("合同审查结果解析失败，自动重试 1 次");
                result = reviewOnce(contractContent);
            }
            if (result == null) {
                throw new RuntimeException("合同审查结果解析失败，请重试");
            }
            return result;
        } catch (IOException e) {
            log.error("合同审查API调用异常", e);
            throw new RuntimeException("合同审查服务调用失败，请稍后重试");
        }
    }

    /**
     * 单次调用 DeepSeek 进行合同审查并解析结构化结果。
     * 返回 null 表示结果无法解析（可重试）；模型明确返回合法空数组 [] 时表示未发现风险，不返回 null。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> reviewOnce(String contractContent) throws IOException {
        String reviewPrompt = buildReviewPrompt(contractContent);

        String requestBody = objectMapper.writeValueAsString(Map.of(
            "model", deepSeekConfig.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", "你是专业的合同审查专家，精通房屋租赁合同法。你必须严格按照JSON数组格式返回审查结果，不要包含任何其他文字。"),
                Map.of("role", "user", "content", reviewPrompt)
            ),
            "temperature", 0.3
        ));

        String url = deepSeekConfig.getBaseUrl() + "/chat/completions";
        log.info("发送合同审查请求: url={}", url);

        Request httpRequest = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = okHttpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("合同审查API请求失败: status={}, body={}", response.code(), errorBody);
                throw new RuntimeException("合同审查请求失败: " + response.code());
            }

            String responseBody = response.body().string();
            Map<String, Object> resultMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

            List<Map<String, Object>> choices = (List<Map<String, Object>>) resultMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("合同审查返回结果为空");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String reviewContent = (String) message.get("content");

            // 尝试从返回内容中提取JSON数组；提取不到说明模型未按约定输出，视为解析失败
            String jsonArrayStr = extractJsonArray(reviewContent);
            if (jsonArrayStr == null) {
                log.warn("审查结果中未找到JSON数组: {}", reviewContent);
                return null;
            }
            List<Map<String, Object>> records;
            try {
                records = objectMapper.readValue(jsonArrayStr, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                log.error("解析审查结果JSON失败: {}", jsonArrayStr, e);
                return null;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("rawContent", reviewContent);
            log.info("合同审查完成，发现{}个风险点", records.size());
            return result;
        }
    }

    /**
     * 从AI返回的文本中提取JSON数组
     */
    private String extractJsonArray(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        String trimmed = content.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return null;
    }

    private String buildReviewPrompt(String contractContent) {
        return "请仔细审查以下房屋租赁合同内容，识别其中可能存在的法律风险条款和不合理约定。\n\n"
                + "合同内容：\n" + contractContent + "\n\n"
                + "请从以下几个维度进行审查：\n"
                + "1. 租金与押金条款是否合理\n"
                + "2. 租期与续租条款是否公平\n"
                + "3. 维修责任划分是否明确\n"
                + "4. 违约条款是否对等\n"
                + "5. 退租与押金返还条款是否合理\n"
                + "6. 其他可能存在的法律风险\n\n"
                + "请严格按照以下JSON数组格式返回审查结果，每个风险点作为一个元素，不要添加任何额外的解释文字：\n"
                + "[{\"clauseContent\": \"相关条款原文\", \"riskType\": \"风险类型\", \"riskLevel\": \"high/medium/low\", "
                + "\"riskExplanation\": \"风险说明\", \"suggestion\": \"修改建议\", \"isHighRisk\": true/false}]\n"
                + "如果没有发现风险，请返回空数组 []。";
    }

    @Override
    public String extractTextFromFile(byte[] fileBytes, String fileName) {
        if (fileName == null) {
            return null;
        }

        String lowerName = fileName.toLowerCase();
        try {
            if (lowerName.endsWith(".txt")) {
                return new String(fileBytes, StandardCharsets.UTF_8);
            }
            if (lowerName.endsWith(".pdf")) {
                return extractPdfText(fileBytes);
            }
            if (lowerName.endsWith(".docx")) {
                return extractDocxText(fileBytes);
            }
            if (lowerName.endsWith(".doc")) {
                return extractDocText(fileBytes);
            }
        } catch (Exception e) {
            log.error("文件内容解析失败: {}", fileName, e);
        }
        log.warn("暂不支持解析该文件类型，请上传 TXT/PDF/Word 格式合同: {}", fileName);
        return null;
    }

    /**
     * 使用 PDFBox 提取 PDF 文本
     */
    private String extractPdfText(byte[] fileBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    /**
     * 使用 POI 提取 .docx 文本（段落 + 表格）
     */
    private String extractDocxText(byte[] fileBytes) throws IOException {
        StringBuilder text = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (paragraph.getText() != null && !paragraph.getText().isBlank()) {
                    text.append(paragraph.getText()).append('\n');
                }
            }
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        text.append(cell.getText()).append('\t');
                    }
                    text.append('\n');
                }
            }
        }
        return text.toString();
    }

    /**
     * 使用 POI HWPF 提取旧版 .doc 文本
     */
    private String extractDocText(byte[] fileBytes) throws IOException {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(fileBytes))) {
            return new WordExtractor(document).getText();
        }
    }
}
