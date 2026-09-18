package com.rental.controller;

import com.rental.common.Result;
import com.rental.common.SecurityUtil;
import com.rental.dto.AiChatDTO;
import com.rental.service.DeepSeekService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    /** 流式对话专用执行器：虚拟线程逐请求处理，不占用 Tomcat 请求线程 */
    private static final ExecutorService STREAM_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    /** SSE 超时时间（毫秒）：流式回答通常 60 秒内完成，留足余量 */
    private static final long SSE_TIMEOUT_MS = 180_000L;

    private final DeepSeekService deepSeekService;

    public AiController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @PostMapping("/chat")
    public Result<Map<String, String>> chat(@Valid @RequestBody AiChatDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(deepSeekService.chat(dto.getQuestion(), dto.getConversationId(), userId));
    }

    /**
     * 流式对话（SSE）：POST 请求，返回事件流。
     * 事件协议（data: JSON）：
     *   {"type":"meta","conversationId":"..."}  会话开始，下发会话 ID
     *   {"type":"delta","content":"..."}        增量内容，前端逐字渲染
     *   {"type":"done"}                          回答完成（已落库）
     *   {"type":"error","message":"..."}         服务异常
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody AiChatDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        STREAM_EXECUTOR.execute(() -> {
            try {
                deepSeekService.chatStream(dto.getQuestion(), dto.getConversationId(), userId,
                        new DeepSeekService.StreamListener() {
                            @Override
                            public void onStart(String conversationId) {
                                send(emitter, Map.of("type", "meta", "conversationId", conversationId));
                            }

                            @Override
                            public void onDelta(String delta) {
                                send(emitter, Map.of("type", "delta", "content", delta));
                            }
                        });
                send(emitter, Map.of("type", "done"));
                emitter.complete();
            } catch (Exception e) {
                log.error("流式对话异常: {}", e.getMessage(), e);
                send(emitter, Map.of("type", "error",
                        "message", e.getMessage() != null ? e.getMessage() : "AI服务调用失败"));
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /**
     * 向 SSE 通道发送 JSON 事件；客户端已断开时抛出异常以终止上游读取。
     */
    private void send(SseEmitter emitter, Map<String, Object> payload) {
        try {
            emitter.send(SseEmitter.event().data(payload));
        } catch (IOException e) {
            throw new IllegalStateException("SSE 发送失败（客户端可能已断开）", e);
        }
    }

    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> conversations() {
        return Result.success(deepSeekService.listConversations(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/conversations/{conversationId}")
    public Result<List<Map<String, Object>>> conversationDetail(@PathVariable String conversationId) {
        return Result.success(deepSeekService.getConversationMessages(conversationId, SecurityUtil.getCurrentUserId()));
    }

    @DeleteMapping("/conversations/{conversationId}")
    public Result<?> deleteConversation(@PathVariable String conversationId) {
        deepSeekService.deleteConversation(conversationId, SecurityUtil.getCurrentUserId());
        return Result.success();
    }
}
