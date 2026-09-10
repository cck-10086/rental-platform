package com.rental.controller;

import com.rental.common.Result;
import com.rental.common.SecurityUtil;
import com.rental.dto.AiChatDTO;
import com.rental.service.DeepSeekService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final DeepSeekService deepSeekService;

    public AiController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @PostMapping("/chat")
    public Result<Map<String, String>> chat(@Valid @RequestBody AiChatDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(deepSeekService.chat(dto.getQuestion(), dto.getConversationId(), userId));
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
