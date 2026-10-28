package com.ragsystem.module.AIChatService.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.List;

public interface ConversationMemory {

    void append(String sessionId, UserMessage userMessage, AiMessage aiMessage);

    List<ChatMessage> getHistory(String sessionId, int i);
}
