package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rental.config.DeepSeekConfig;
import com.rental.entity.AiConversation;
import com.rental.mapper.AiConversationMapper;
import com.rental.service.DeepSeekService;
import com.rental.service.LegalKnowledgeService;
import com.rental.common.RapidOcrEngine;
import dev.langchain4j.data.segment.TextSegment;
import okhttp3.*;
import okio.BufferedSource;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;
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

    @Autowired
    private LegalKnowledgeService legalKnowledgeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = "你是专业的租房法律顾问，精通房屋租赁相关法律法规。请基于中国法律为用户提供专业、准确的法律咨询建议。回答时请引用相关法律条文。";

    /**
     * 构建带 RAG 上下文的 system prompt：检索与问题最相关的 Top-5 法律条文注入，
     * 并要求模型在回答末尾以「依据：《...》第...条」格式列明引用，避免编造条文。
     */
    private String buildRagSystemPrompt(String question) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);
        // 单元测试手动 new 服务实例时该字段为 null，此时退回普通回答模式
        if (legalKnowledgeService == null) {
            return sb.toString();
        }
        try {
            List<TextSegment> hits = legalKnowledgeService.searchTop5(question);
            if (!hits.isEmpty()) {
                sb.append("\n\n以下是与用户问题最相关的现行法律条文，请优先依据这些条文回答，")
                  .append("不要引用这些条文之外的条文号，并在回答末尾另起一行，")
                  .append("以「依据：《法律名称》第X条」的格式列明本次回答所引用的条文：\n");
                for (TextSegment segment : hits) {
                    String source = segment.metadata().getString("source");
                    String article = segment.metadata().getString("article");
                    sb.append("\n【").append(source);
                    if (article != null) {
                        sb.append(" ").append(article);
                    }
                    sb.append("】\n").append(segment.text()).append('\n');
                }
            }
        } catch (Exception e) {
            log.warn("法律条文检索失败，退回普通回答模式", e);
        }
        return sb.toString();
    }

    @Override
    public Map<String, String> chat(String question, String conversationId, Long userId) {
        try {
            if (deepSeekConfig.getApiKey() == null || deepSeekConfig.getApiKey().isBlank()) {
                throw new RuntimeException("未配置 DeepSeek API Key，请在环境变量 DEEPSEEK_API_KEY 中配置");
            }
            if (conversationId == null || conversationId.isBlank()) {
                conversationId = UUID.randomUUID().toString();
            }

            // 组装多轮对话历史（最近10条），system prompt 注入 RAG 检索的相关法律条文
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", buildRagSystemPrompt(question)));
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
                Map.of("role", "system", "content",
                        "你是专业的合同审查专家，精通房屋租赁相关法律。你只报告对承租人权益有实际影响的"
                                + "实质性风险，不报告行业惯例条款；同一条款的多个风险点合并为一条记录；"
                                + "填写完整性问题以「提示：」开头且定级为 low。"
                                + "你必须严格按照JSON数组格式返回审查结果，不要包含任何其他文字。"),
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
        return "请审查以下房屋租赁合同，只识别对承租人权益有实际影响的实质性风险。\n\n"
                + "合同内容：\n" + contractContent + "\n\n"
                + "【实质性风险判定标准】仅报告对承租人权益有实际影响的问题，即：\n"
                + "- 金额明显超标或不合理（如违约金畸高、押金扣留条件苛刻）；\n"
                + "- 责任明显不对等（如仅约束一方、单方解除权失衡）；\n"
                + "- 权利义务严重缺失（如缺少关键保障约定且对承租人不利）；\n"
                + "- 约定与法律法规强制性规定冲突。\n\n"
                + "【风险定级标准】按以下锚点判定 riskLevel，实质风险不得一律定为 high：\n"
                + "- high：金钱损失达月租金量级（如违约金单月累计超过月租金30%、押金大部分或全部扣留、"
                + "赔偿金达月租金2倍及以上），或剥夺/严重限制承租人居住权益"
                + "（如出租人可无限制进入房屋、无补偿任意解除、买卖房屋后强制短期搬离）；\n"
                + "- medium：明显不利于承租人但金额有限或可依法请求调减"
                + "（如违约金0.5%/日、押金退还迟延或条件模糊、单项费用转嫁、限制转租权）；\n"
                + "- low：仅填写完整性提示（riskType 以「提示：」开头）。\n"
                + "违约金判定参考：日违约金不超过月租金0.5%属常见约定，不报风险；0.5%~1%报medium；"
                + "超过1%（单月累计超月租金30%）报high。0.5%/日不得标注为「畸高」。\n\n"
                + "【禁止报告】以下行业惯例条款不得作为风险输出：水费、电费、燃气费、网络费单项由租客承担、"
                + "交接清单签字确认、通知送达方式、日常合理使用注意事项、"
                + "税费常规约定等；费用分担惯例的排除仅限上述单项，物业管理费、供暖费等费用向租客转嫁"
                + "不属于行业惯例，应报medium（除非合同已明确写明含物业费/含暖气费的租金包干）；"
                + "其余条款除非存在明显异常（如费用重复收取、金额畸高、附加苛刻条件）方可豁免。\n\n"
                + "【填写完整性问题】日期空位、签署栏空白、待填项未填等不影响权利义务内容的填写问题，"
                + "单独归类：riskLevel 一律为 low，且 riskType 必须以「提示：」开头"
                + "（例如「提示：签署日期空白」），与实质性风险明确区分。\n\n"
                + "审查维度（参考）：\n"
                + "1. 押金金额与返还条件是否明显不合理\n"
                + "2. 租期与续租条款是否显失公平\n"
                + "3. 维修责任划分是否严重失衡\n"
                + "4. 违约责任是否明显不对等、违约金是否畸高\n"
                + "5. 提前退租与单方解除权是否被不当限制\n"
                + "6. 与法律强制性规定冲突的条款\n\n"
                + "【输出要求】\n"
                + "- 同一条款存在多个风险点时，必须合并为一条记录，riskType 内用顿号（、）连接，"
                + "禁止将同一条款拆成多条重复记录；\n"
                + "- 每条记录的 clauseContent 引用条款原文，同一条款在结果中只出现一次；\n"
                + "- clauseContent 必须逐字摘录合同原文，不得改写、缩略、拼接不同条款或补充原文没有的语句；"
                + "引用长度不足会造成风险定位失败；\n"
                + "- 严格按以下 JSON 数组格式返回，每个风险点作为一个元素，不要添加任何额外的解释文字：\n"
                + "[{\"clauseContent\": \"相关条款原文\", \"riskType\": \"风险类型\", \"riskLevel\": \"high/medium/low\", "
                + "\"riskExplanation\": \"风险说明\", \"suggestion\": \"修改建议\", \"isHighRisk\": true/false}]\n"
                + "- 没有实质性风险时返回空数组 []（填写完整性提示除外，其 riskLevel 为 low）。";
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
                String text = extractPdfText(fileBytes);
                // 提取文本为空或过短（去空白后 <50 字符）时判定为扫描件，转图片走 OCR 识别
                if (isTextTooShort(text)) {
                    log.info("PDF 文本层缺失或过短，判定为扫描件，启用 OCR 识别: {}", fileName);
                    text = ocrPdfText(fileBytes);
                }
                return text;
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
        log.warn("文件内容解析失败或暂不支持该文件类型，请上传含文本层的 TXT/PDF/Word 格式合同: {}", fileName);
        return null;
    }

    /**
     * 判断提取到的文本是否过短（去空白后不足 50 字符），过短视为无有效文本层（扫描件）。
     */
    private boolean isTextTooShort(String text) {
        return text == null || text.replaceAll("\\s+", "").length() < 50;
    }

    /**
     * 将扫描版 PDF 逐页渲染为 200dpi 位图，交由 RapidOCR 识别后拼接全部页面文本。
     */
    private String ocrPdfText(byte[] fileBytes) throws IOException {
        StringBuilder text = new StringBuilder();
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 200, ImageType.RGB);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "png", out);
                String pageText = RapidOcrEngine.recognize(out.toByteArray(), ".png");
                if (pageText != null && !pageText.isBlank()) {
                    text.append(pageText).append('\n');
                }
            }
        }
        return text.toString();
    }

    /**
     * 使用 PDFBox 提取 PDF 文本
     */
    private String extractPdfText(byte[] fileBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Override
    public void chatStream(String question, String conversationId, Long userId, StreamListener listener) {
        try {
            if (deepSeekConfig.getApiKey() == null || deepSeekConfig.getApiKey().isBlank()) {
                throw new RuntimeException("未配置 DeepSeek API Key，请在环境变量 DEEPSEEK_API_KEY 中配置");
            }
            if (conversationId == null || conversationId.isBlank()) {
                conversationId = UUID.randomUUID().toString();
            }
            String finalConversationId = conversationId;
            listener.onStart(finalConversationId);

            // 与同步 chat 相同的会话组装：RAG 条文注入 + 最近10条历史
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", buildRagSystemPrompt(question)));
            messages.addAll(loadHistory(userId, finalConversationId));
            messages.add(Map.of("role", "user", "content", question));

            String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", deepSeekConfig.getModel(),
                "messages", messages,
                "temperature", 0.7,
                "stream", true
            ));

            String url = deepSeekConfig.getBaseUrl() + "/chat/completions";
            log.info("发送DeepSeek流式请求: url={}, conversationId={}", url, finalConversationId);

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            StringBuilder answerBuilder = new StringBuilder();
            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("DeepSeek流式请求失败: status={}, body={}", response.code(), errorBody);
                    throw new RuntimeException("AI服务请求失败: " + response.code());
                }
                // 逐行读取 SSE 流：data: {...} / data: [DONE]
                BufferedSource source = response.body().source();
                String line;
                while ((line = source.readUtf8Line()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    String delta = extractDeltaContent(data);
                    if (delta != null && !delta.isEmpty()) {
                        answerBuilder.append(delta);
                        listener.onDelta(delta);
                    }
                }
            }

            // 完成后落库，与同步 chat 保持一致
            AiConversation conversation = new AiConversation();
            conversation.setUserId(userId);
            conversation.setQuestion(question);
            conversation.setAnswer(answerBuilder.toString());
            conversation.setConversationId(finalConversationId);
            aiConversationMapper.insert(conversation);

            log.info("DeepSeek流式对话完成: conversationId={}", finalConversationId);
        } catch (IOException e) {
            log.error("DeepSeek流式调用异常", e);
            throw new RuntimeException("AI服务调用失败，请稍后重试");
        }
    }

    /**
     * 从 SSE 分片 JSON 中提取增量内容 choices[0].delta.content。
     */
    @SuppressWarnings("unchecked")
    private String extractDeltaContent(String sseJson) {
        try {
            Map<String, Object> chunk = objectMapper.readValue(sseJson, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
            return delta == null ? null : (String) delta.get("content");
        } catch (IOException e) {
            log.warn("解析流式分片失败: {}", sseJson, e);
            return null;
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
