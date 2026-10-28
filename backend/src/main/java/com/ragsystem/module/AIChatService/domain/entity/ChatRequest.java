package com.ragsystem.module.AIChatService.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    @JsonSerialize(using = ToStringSerializer.class)
    private String sessionId;           // 会话ID
    private String question;            // 用户问题
    private List<String> knowledgeBaseIds; // 指定查询哪些知识库（权限控制用）
}
