package com.rental.common;

import com.benjaminwan.ocrlibrary.OcrResult;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * RapidOCR 识别引擎封装（ONNXRuntime 推理，纯 Java 实现，无 Python 依赖）。
 * PP-OCR 检测/识别模型已内置于 rapidocr-onnx-platform 依赖中，首次调用时懒加载初始化。
 */
public final class RapidOcrEngine {

    private static final Logger log = LoggerFactory.getLogger(RapidOcrEngine.class);

    private static volatile InferenceEngine engine;

    private RapidOcrEngine() {
    }

    /**
     * 懒加载获取 OCR 引擎单例（首次调用会初始化 ONNX 模型，耗时数秒）。
     */
    private static InferenceEngine getEngine() {
        if (engine == null) {
            synchronized (RapidOcrEngine.class) {
                if (engine == null) {
                    log.info("初始化 RapidOCR 引擎（PP-OCR ONNX 模型）...");
                    engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);
                }
            }
        }
        return engine;
    }

    /**
     * 对图片字节进行 OCR 识别，返回按行拼接的文本。
     * RapidOCR 仅支持图片路径入参，这里先落盘为临时文件（UUID 命名，避免中文路径乱码），识别后删除。
     */
    public static String recognize(byte[] imageBytes, String suffix) throws IOException {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("rapid-ocr-" + UUID.randomUUID(), suffix);
            Files.write(tempFile, imageBytes);
            OcrResult result = getEngine().runOcr(tempFile.toAbsolutePath().toString());
            return result == null || result.getStrRes() == null ? "" : result.getStrRes().trim();
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}
