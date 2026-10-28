package com.ragsystem.module.AIChatService.service;


import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import lombok.Data;

public interface StreamingResponseHandler<T> {
    void onNext(String token);
    void onComplete();
    void onComplete(Response<AiMessage> response);
    void onError(Throwable error);
    // 当收到 Token 统计时触发
    default void onUsage(TokenUsage usage) {}

    @Data
    public class TokenUsage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;
    }
}