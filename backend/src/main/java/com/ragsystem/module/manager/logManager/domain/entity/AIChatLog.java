package com.ragsystem.module.manager.logManager.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_chat_log")
public class AIChatLog {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long userId;        // 哪个用户问的
    private String question;    // 用户的提问内容
    private String answer;      // AI 的回答内容
    private String modelName;   // 使用的模型（如 deepseek-v3）
    private Long tokensUsed;    // 消耗的 Token 数
    private String sourceContext;  // 用于溯源
    private LocalDateTime createTime; // 提问时间
}