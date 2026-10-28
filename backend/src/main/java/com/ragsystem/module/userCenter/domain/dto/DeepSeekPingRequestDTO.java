package com.ragsystem.module.userCenter.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DeepSeek API 拨测请求体 DTO（兼容 OpenAI 格式）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeepSeekPingRequestDTO {
    // 模型名称（DeepSeek 通用模型）
    private String model = "deepseek-chat";
    // 对话消息列表（极小规模请求，仅验证 Key 有效性）
    private List<Message> messages;
    // 最大 tokens（最小取值，几乎无费用）
    private Integer max_tokens = 1;

    /**
     * 对话消息子结构
     */
    @Data
    public static class Message {
        // 角色（user/assistant/system）
        private String role = "user";
        // 消息内容
        private String content = "ping";
    }
}