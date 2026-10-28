package com.ragsystem.module.AIChatService.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ChatResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private String sessionId;
    private String content;             // 回答内容
    private boolean isDone;             // 是否结束
    private List<Citation> citations;   // 来源引用

    public static ChatResponse ofToken(String sessionId, String token) {
        return new ChatResponse(sessionId, token, false, null);
    }
    public static ChatResponse ofDone(String sessionId, List<Citation> citations) {
        return new ChatResponse(sessionId, null, true, citations);
    }
    public static ChatResponse ofError(String sessionId, String message) {
        return new ChatResponse(sessionId, message, true, null);
    }
}