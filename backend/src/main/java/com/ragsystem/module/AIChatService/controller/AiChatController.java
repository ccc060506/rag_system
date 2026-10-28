package com.ragsystem.module.AIChatService.controller;

import com.ragsystem.common.util.RateLimit;
import com.ragsystem.common.util.UserContext;
import com.ragsystem.module.AIChatService.domain.entity.ChatRequest;
import com.ragsystem.module.AIChatService.domain.entity.ChatResponse;
import com.ragsystem.module.AIChatService.service.IAiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/chat")
public class AiChatController {

    @Autowired
    private IAiChatService aiChatService;

    /**
     * 企业级 RAG 聊天接口 - 支持流式输出
     */
    @RateLimit(key = "chatStreamLimit" , count = 5 , message = "对话太频繁了，稍微休息一下吧")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> chatStream(@RequestBody ChatRequest request) throws Exception {

        // 获取当前用户（用于权限控制）
        Long userId = UserContext.getUserId();
        return aiChatService.chatWithStream(request, userId);
    }
}
