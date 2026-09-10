package com.rental.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rental.config.DeepSeekConfig;
import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 合同文件内容提取单元测试：TXT、PDF、Word（docx）及不支持类型。
 */
class DeepSeekServiceImplTest {

    private final DeepSeekServiceImpl service = new DeepSeekServiceImpl();

    @Test
    void extractTxtText() {
        String text = service.extractTextFromFile(
                "房屋租赁合同正文".getBytes(StandardCharsets.UTF_8), "合同.txt");
        assertTrue(text.contains("房屋租赁合同正文"));
    }

    @Test
    void extractPdfText() throws Exception {
        byte[] pdf = createPdf();
        String text = service.extractTextFromFile(pdf, "合同.pdf");
        assertNotNull(text);
        assertTrue(text.contains("RENTAL CONTRACT TEST"), "PDF 提取结果应包含文本，实际: " + text);
    }

    @Test
    void extractDocxText() throws Exception {
        byte[] docx = createDocx();
        String text = service.extractTextFromFile(docx, "合同.docx");
        assertNotNull(text);
        assertTrue(text.contains("房屋租赁合同"), "docx 提取应包含段落文本");
        assertTrue(text.contains("押金"), "docx 提取应包含表格文本");
    }

    @Test
    void unsupportedTypeReturnsNull() {
        assertNull(service.extractTextFromFile(new byte[]{1, 2, 3}, "图片.jpg"));
    }

    @Test
    void nullFileNameReturnsNull() {
        assertNull(service.extractTextFromFile(new byte[]{1}, null));
    }

    @Test
    void reviewContractThrowsWhenParseFailsAfterRetry() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = startDeepSeekServer("这不是JSON数组", "仍然不是JSON数组", calls);
        try {
            DeepSeekServiceImpl service = buildService(server);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.reviewContract("合同内容"));
            assertTrue(ex.getMessage().contains("解析失败"),
                    "应提示解析失败，实际: " + ex.getMessage());
            assertEquals(2, calls.get(), "解析失败应自动重试 1 次，共调用 2 次");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reviewContractRetriesAndSucceedsOnSecondAttempt() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String valid = "[{\"clauseContent\":\"押金不退\",\"riskType\":\"押金条款\",\"riskLevel\":\"high\","
                + "\"riskExplanation\":\"风险说明\",\"suggestion\":\"修改建议\",\"isHighRisk\":true}]";
        HttpServer server = startDeepSeekServer("这不是JSON数组", valid, calls);
        try {
            DeepSeekServiceImpl service = buildService(server);
            Map<String, Object> result = service.reviewContract("合同内容");
            List<Map<String, Object>> records = (List<Map<String, Object>>) result.get("records");
            assertEquals(1, records.size());
            assertEquals("high", records.get(0).get("riskLevel"));
            assertEquals(2, calls.get(), "首次失败后应重试成功，共调用 2 次");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reviewContractTreatsEmptyArrayAsNoRisk() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = startDeepSeekServer("[]", "[]", calls);
        try {
            DeepSeekServiceImpl service = buildService(server);
            Map<String, Object> result = service.reviewContract("合同内容");
            List<Map<String, Object>> records = (List<Map<String, Object>>) result.get("records");
            assertNotNull(result.get("rawContent"));
            assertTrue(records.isEmpty(), "模型明确返回空数组 [] 应视为无风险，而非解析失败");
            assertEquals(1, calls.get(), "合法空数组不应触发重试");
        } finally {
            server.stop(0);
        }
    }

    /**
     * 用 JDK 内置 HttpServer 模拟 DeepSeek 接口：第一次返回 firstContent，后续返回 secondContent。
     */
    private static HttpServer startDeepSeekServer(String firstContent, String secondContent, AtomicInteger calls)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        ObjectMapper mapper = new ObjectMapper();
        server.createContext("/chat/completions", exchange -> {
            int n = calls.incrementAndGet();
            String content = n == 1 ? firstContent : secondContent;
            try {
                String body = "{\"choices\":[{\"message\":{\"content\":"
                        + mapper.writeValueAsString(content) + "}}]}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        server.start();
        return server;
    }

    private static DeepSeekServiceImpl buildService(HttpServer server) {
        DeepSeekServiceImpl service = new DeepSeekServiceImpl();
        DeepSeekConfig config = new DeepSeekConfig();
        config.setApiKey("test-key");
        config.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        config.setModel("test-model");
        ReflectionTestUtils.setField(service, "deepSeekConfig", config);
        ReflectionTestUtils.setField(service, "okHttpClient", new OkHttpClient());
        return service;
    }

    private byte[] createPdf() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                cs.newLineAtOffset(50, 700);
                cs.showText("RENTAL CONTRACT TEST");
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private byte[] createDocx() throws Exception {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p1 = doc.createParagraph();
            p1.createRun().setText("房屋租赁合同");
            XWPFParagraph p2 = doc.createParagraph();
            p2.createRun().setText("月租金：3000元");
            XWPFTable table = doc.createTable(1, 2);
            XWPFTableRow row = table.getRow(0);
            row.getCell(0).setText("押金");
            row.getCell(1).setText("3000元");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }
}
