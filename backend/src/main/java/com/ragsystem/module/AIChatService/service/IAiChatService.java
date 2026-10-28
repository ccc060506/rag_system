package com.ragsystem.module.AIChatService.service;

import com.ragsystem.module.AIChatService.domain.entity.ChatRequest;
import com.ragsystem.module.AIChatService.domain.entity.ChatResponse;
import reactor.core.publisher.Flux;

public interface IAiChatService {

    Flux<ChatResponse> chatWithStream(ChatRequest request, Long userId) throws Exception;
}
