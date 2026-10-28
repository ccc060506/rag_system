package com.ragsystem.module.AIChatService.service.impl;
import com.ragsystem.module.AIChatService.service.ConversationMemory;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ConversationMemoryImpl implements ConversationMemory {
    private final Map<String, List<ChatMessage>> memory = new ConcurrentHashMap<>();

    @Override
    public void append(String sessionId, UserMessage userMessage, AiMessage aiMessage) {
        memory.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(userMessage);
        memory.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(aiMessage);
    }

    @Override
    public List<ChatMessage> getHistory(String sessionId, int maxTurns) {
        List<ChatMessage> all = memory.getOrDefault(sessionId, new ArrayList<>());
        int size = all.size();
        log.info("session {} 加载历史消息 {} 条", sessionId, Math.min(all.size(), maxTurns*2));
        if (size <= maxTurns) {
            return new ArrayList<>(all);
        } else {
            return new ArrayList<>(all.subList(size - maxTurns, size));
        }
    }
}
