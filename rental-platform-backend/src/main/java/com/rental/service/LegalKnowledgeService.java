package com.rental.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15q.BgeSmallZhV15QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 法律知识库（RAG）：加载 classpath:legal/ 下的法律语料，按条文分块，
 * 使用进程内 bge-small-zh-v1.5 量化模型向量化后存入内存向量库，提供 Top-K 语义检索。
 * 语料规模小（约 60 个条文），无需引入向量数据库。
 */
@Service
public class LegalKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(LegalKnowledgeService.class);

    /** 法律语料文件（classpath 路径） */
    private static final String[] CORPUS_FILES = {
            "legal/civil-code-lease.md",
            "legal/housing-lease-admin-measures.md"
    };

    /** 条文起始行正例：如「第七百一十二条」「第九条」（行首） */
    private static final Pattern ARTICLE_HEAD = Pattern.compile("^第[零一二三四五六七八九十百]+条");

    /** 检索返回的最大条文数 */
    private static final int TOP_K = 5;

    private final EmbeddingModel embeddingModel = new BgeSmallZhV15QuantizedEmbeddingModel();
    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    @PostConstruct
    public void init() {
        List<TextSegment> segments = new ArrayList<>();
        for (String path : CORPUS_FILES) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
                if (in == null) {
                    log.error("法律语料文件缺失: {}", path);
                    continue;
                }
                segments.addAll(splitByArticle(new String(in.readAllBytes(), StandardCharsets.UTF_8), path));
            } catch (IOException e) {
                log.error("法律语料文件读取失败: {}", path, e);
            }
        }
        if (segments.isEmpty()) {
            log.warn("法律知识库为空，RAG 检索将不可用");
            return;
        }
        // 批量向量化并写入内存向量库
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);
        log.info("法律知识库加载完成，共 {} 个条文", segments.size());
    }

    /**
     * 语义检索与问题最相关的法律条文（Top-5）。
     */
    public List<TextSegment> searchTop5(String query) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(TOP_K)
                        .build());
        return result.matches().stream()
                .map(dev.langchain4j.store.embedding.EmbeddingMatch::embedded)
                .toList();
    }

    /**
     * 按条文分块：遇到行首「第X条」即开启新块，块内保留条款的款项结构。
     * 元数据记录来源文件与条文号，供生成引用使用。
     */
    private List<TextSegment> splitByArticle(String content, String path) {
        String source = path.endsWith("civil-code-lease.md")
                ? "《中华人民共和国民法典》"
                : "《商品房屋租赁管理办法》";
        List<TextSegment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String currentArticle = null;

        for (String line : content.split("\n")) {
            String trimmed = line.strip();
            Matcher matcher = ARTICLE_HEAD.matcher(trimmed);
            if (matcher.find()) {
                flush(segments, current, source, currentArticle);
                current = new StringBuilder(trimmed);
                currentArticle = trimmed.substring(matcher.start(), matcher.end());
            } else if (!trimmed.isEmpty() && current.length() > 0) {
                // 条文款项、标题、元信息之外的行：正文注释行不属于条文，跳过以「#」开头的标题行
                if (!trimmed.startsWith("#") && !trimmed.startsWith("（20")) {
                    current.append('\n').append(trimmed);
                }
            }
        }
        flush(segments, current, source, currentArticle);
        return segments;
    }

    private void flush(List<TextSegment> segments, StringBuilder buffer, String source, String article) {
        String text = buffer.toString().strip();
        if (text.isEmpty()) {
            return;
        }
        Metadata metadata = new Metadata();
        metadata.put("source", source);
        if (article != null) {
            metadata.put("article", article);
        }
        segments.add(TextSegment.from(text, metadata));
    }
}
