package com.ragsystem.module.manager.logManager.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AIChatLogVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String username;     // 通过 userId 关联出的用户名
    private String question;
    private String answer;
    private String modelName;
    private LocalDateTime createTimeStr; // 提问时间
    private List<String> sourceContext;  // 用于溯源
    private String tokensUsed;;  // 拼接成 "消耗 xxx Tokens"
}
