package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rental.common.SecurityUtil;
import com.rental.common.PageResult;
import com.rental.config.MinioConfig;
import com.rental.entity.Contract;
import com.rental.entity.ReviewRecord;
import com.rental.mapper.ContractMapper;
import com.rental.mapper.ReviewRecordMapper;
import com.rental.service.ContractService;
import com.rental.service.DeepSeekService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ContractServiceImpl implements ContractService {
    private static final Logger log = LoggerFactory.getLogger(ContractServiceImpl.class);


    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private ReviewRecordMapper reviewRecordMapper;

    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private DeepSeekService deepSeekService;

    @Override
    public Contract upload(MultipartFile file, String title) {
        Long userId = SecurityUtil.getCurrentUserId();

        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String objectName = "contracts/" + userId + "/" + UUID.randomUUID().toString()
                    + (originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "");

            // 上传到MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );

            // 从上传文件中提取合同文本内容（支持 TXT/PDF/docx/doc，扫描版 PDF 需 OCR 后支持）
            String content = deepSeekService.extractTextFromFile(file.getBytes(), originalFilename);

            // 构建文件访问路径
            String filePath = minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/" + objectName;

            // 创建合同记录
            Contract contract = new Contract();
            contract.setUserId(userId);
            contract.setTitle(title != null ? title : (originalFilename != null ? originalFilename : "未命名合同"));
            contract.setFileName(originalFilename);
            contract.setFilePath(filePath);
            contract.setFileType(getFileType(originalFilename));
            contract.setContent(content);
            contract.setStatus("pending");
            contractMapper.insert(contract);

            log.info("合同上传成功: id={}, fileName={}", contract.getId(), originalFilename);
            return contract;
        } catch (Exception e) {
            log.error("合同上传失败", e);
            throw new RuntimeException("合同上传失败，请稍后重试");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> review(Long contractId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        if (!contract.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权审查该合同");
        }

        // 读取合同文件内容（上传时已提取，仅TXT支持解析）
        String contractContent = contract.getContent();
        if (contractContent == null || contractContent.isBlank()) {
            throw new RuntimeException("该合同暂无可审查的文本内容：请上传包含可解析文本的 TXT/PDF/Word 合同");
        }

        // 调用AI审查
        Map<String, Object> reviewResult = deepSeekService.reviewContract(contractContent);
        List<Map<String, Object>> records = (List<Map<String, Object>>) reviewResult.get("records");

        if (records == null || records.isEmpty()) {
            // 没有发现风险点
            contract.setStatus("completed");
            contract.setRiskLevel("低风险");
            contract.setRiskCount(0);
            contractMapper.updateById(contract);

            Map<String, Object> result = new HashMap<>();
            result.put("riskLevel", "低风险");
            result.put("riskCount", 0);
            result.put("records", Collections.emptyList());
            return result;
        }

        // 重新审查前清除旧审查记录，避免重复累积
        reviewRecordMapper.delete(new LambdaQueryWrapper<ReviewRecord>().eq(ReviewRecord::getContractId, contractId));

        // 保存审查记录并统计风险
        int highRiskCount = 0;
        int totalRiskCount = records.size();
        String highestLevel = "低风险";

        for (Map<String, Object> item : records) {
            ReviewRecord record = new ReviewRecord();
            record.setContractId(contractId);
            record.setUserId(contract.getUserId());
            record.setClauseContent((String) item.get("clauseContent"));
            record.setRiskType((String) item.get("riskType"));

            // 将英文风险等级转换为中文
            String riskLevel = (String) item.get("riskLevel");
            String cnRiskLevel = convertRiskLevel(riskLevel);
            record.setRiskLevel(cnRiskLevel);

            String riskExplanation = (String) item.get("riskExplanation");
            record.setRiskExplanation(riskExplanation != null && riskExplanation.length() > 1000
                    ? riskExplanation.substring(0, 1000) : riskExplanation);

            record.setSuggestion((String) item.get("suggestion"));

            Boolean isHighRisk = (Boolean) item.get("isHighRisk");
            if (isHighRisk == null) {
                isHighRisk = "high".equalsIgnoreCase(riskLevel);
            }
            record.setIsHighRisk(isHighRisk ? 1 : 0);

            reviewRecordMapper.insert(record);

            if (isHighRisk) {
                highRiskCount++;
            }

            // 确定最高风险等级
            if ("高风险".equals(cnRiskLevel)) {
                highestLevel = "高风险";
            } else if ("中风险".equals(cnRiskLevel) && !"高风险".equals(highestLevel)) {
                highestLevel = "中风险";
            }
        }

        // 更新合同状态
        contract.setStatus("completed");
        contract.setRiskLevel(highestLevel);
        contract.setRiskCount(totalRiskCount);
        contractMapper.updateById(contract);

        Map<String, Object> result = new HashMap<>();
        result.put("riskLevel", highestLevel);
        result.put("riskCount", totalRiskCount);
        result.put("highRiskCount", highRiskCount);
        result.put("records", records);
        log.info("合同审查完成: contractId={}, riskLevel={}, riskCount={}", contractId, highestLevel, totalRiskCount);
        return result;
    }

    /** 将 DeepSeek 返回的英文风险等级转换为中文 */
    private String convertRiskLevel(String level) {
        if (level == null) return "低风险";
        return switch (level.toLowerCase()) {
            case "high" -> "高风险";
            case "medium" -> "中风险";
            default -> "低风险";
        };
    }

    @Override
    public PageResult<Contract> listByUser(String keyword, long current, long size) {
        Long userId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contract::getUserId, userId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Contract::getTitle, keyword).or().like(Contract::getFileName, keyword));
        }
        wrapper.orderByDesc(Contract::getCreatedAt);
        Page<Contract> page = contractMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    @Override
    public Contract getDetail(Long contractId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        if (!contract.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权查看该合同");
        }
        return contract;
    }

    @Override
    public List<ReviewRecord> getReviewRecords(Long contractId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        if (!contract.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权查看该合同");
        }
        LambdaQueryWrapper<ReviewRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewRecord::getContractId, contractId).orderByDesc(ReviewRecord::getIsHighRisk);
        return reviewRecordMapper.selectList(wrapper);
    }

    @Override
    public void delete(Long contractId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        if (!contract.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权删除该合同");
        }
        // 同步删除MinIO中的文件对象，避免孤儿文件
        deleteObjectFromMinio(contract.getFilePath());
        // 逻辑删除
        contractMapper.deleteById(contractId);
        log.info("合同逻辑删除成功: id={}", contractId);
    }

    @Override
    public byte[] exportReviewReport(Long contractId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        if (!contract.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权查看该合同");
        }
        List<ReviewRecord> records = reviewRecordMapper.selectList(
                new LambdaQueryWrapper<ReviewRecord>()
                        .eq(ReviewRecord::getContractId, contractId)
                        .orderByDesc(ReviewRecord::getIsHighRisk));

        try (PDDocument document = new PDDocument()) {
            PDFont font = loadChineseFont(document);
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(document, page);
            float y = 760;
            try {
                // 标题
                y = drawLine(cs, font, 22, "合同审查报告", 50, y, false);
                y -= 12;
                // 合同信息
                y = drawLine(cs, font, 11, "合同标题：" + safe(contract.getTitle()), 50, y, true);
                y = drawLine(cs, font, 11, "文件名：" + safe(contract.getFileName()), 50, y, true);
                y = drawLine(cs, font, 11, "上传时间：" + safe(String.valueOf(contract.getCreatedAt())), 50, y, true);
                y = drawLine(cs, font, 11, "风险等级：" + safe(contract.getRiskLevel()) + "　风险条款数：" + records.size(), 50, y, true);
                y -= 16;

                if (records.isEmpty()) {
                    y = drawLine(cs, font, 12, "未发现风险条款。", 50, y, false);
                } else {
                    int index = 1;
                    for (ReviewRecord record : records) {
                        if (y < 90) {
                            // 换页：关闭当前内容流，创建绑定新页的内容流
                            cs.close();
                            page = new PDPage();
                            document.addPage(page);
                            cs = new PDPageContentStream(document, page);
                            y = 760;
                        }
                        y = drawLine(cs, font, 13, "风险" + index + "　【" + safe(record.getRiskLevel()) + "】" + safe(record.getRiskType()), 50, y, false);
                        y -= 4;
                        y = drawLine(cs, font, 10, "条款原文：" + safe(record.getClauseContent()), 60, y, true);
                        y = drawLine(cs, font, 10, "风险解读：" + safe(record.getRiskExplanation()), 60, y, true);
                        y = drawLine(cs, font, 10, "修改建议：" + safe(record.getSuggestion()), 60, y, true);
                        y -= 12;
                        index++;
                    }
                }
            } finally {
                cs.close();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("审查报告生成失败: contractId={}", contractId, e);
            throw new RuntimeException("审查报告生成失败，请稍后重试");
        }
    }

    @Override
    public ContractFilePayload loadContractFile(Long contractId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        if (!contract.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权查看该合同");
        }

        String objectName = getObjectName(contract.getFilePath());
        if (objectName == null) {
            throw new RuntimeException("合同文件路径无效");
        }
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectName)
                        .build())) {
            return new ContractFilePayload(
                    response.readAllBytes(),
                    contract.getFileName(),
                    contentTypeOf(contract.getFileType()));
        } catch (Exception e) {
            log.error("读取合同文件失败: contractId={}, object={}", contractId, objectName, e);
            throw new RuntimeException("合同文件读取失败，请稍后重试");
        }
    }

    /**
     * 在 PDF 中绘制一行（支持自动换行），返回新的 y 坐标。
     */
    private float drawLine(PDPageContentStream cs, PDFont font, float size, String text,
                           float x, float y, boolean wrap) throws IOException {
        float lineHeight = size + 7;
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        if (!wrap || text.length() <= 40) {
            cs.showText(text);
            cs.endText();
            return y - lineHeight;
        }
        // 按 40 个字符宽度手动换行
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + 40, text.length());
            cs.showText(text.substring(start, end));
            cs.newLineAtOffset(0, -lineHeight);
            start = end;
        }
        cs.endText();
        return y - ((text.length() / 40 + 1) * lineHeight);
    }

    /**
     * 加载系统中文字体，找不到时回退到标准字体。
     */
    private PDFont loadChineseFont(PDDocument document) throws IOException {
        String[] candidates = {
                "C:\\Windows\\Fonts\\Deng.ttf",
                "C:\\Windows\\Fonts\\simhei.ttf",
                "C:\\Windows\\Fonts\\msyh.ttc",
                "C:\\Windows\\Fonts\\simsun.ttc",
                "C:\\Windows\\Fonts\\simkai.ttf"
        };
        for (String path : candidates) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    return PDType0Font.load(document, file);
                } catch (Exception e) {
                    log.warn("中文字体加载失败，尝试下一个: {}", path);
                }
            }
        }
        log.warn("未找到可用的中文字体，报告中文可能显示异常");
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * 删除MinIO中的合同文件对象
     */
    private void deleteObjectFromMinio(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        String objectName = getObjectName(filePath);
        if (objectName == null) {
            return;
        }
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            log.warn("删除MinIO文件失败: object={}", objectName, e);
        }
    }

    /**
     * 从存储路径中解析 MinIO 对象名
     */
    private String getObjectName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        String prefix = minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/";
        if (!filePath.startsWith(prefix)) {
            return null;
        }
        return filePath.substring(prefix.length());
    }

    /**
     * 按文件类型映射 Content-Type
     */
    private String contentTypeOf(String fileType) {
        if (fileType == null) {
            return "application/octet-stream";
        }
        return switch (fileType.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt" -> "text/plain; charset=utf-8";
            default -> "application/octet-stream";
        };
    }

    /**
     * 根据文件名获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null) {
            return "unknown";
        }
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            return "pdf";
        } else if (lowerName.endsWith(".docx")) {
            return "docx";
        } else if (lowerName.endsWith(".doc")) {
            return "doc";
        } else if (lowerName.endsWith(".txt")) {
            return "txt";
        } else {
            return "other";
        }
    }

}
