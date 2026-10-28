package com.ragsystem.module.AIChatService.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Citation {
    private String fileName;
    @JsonSerialize(using = ToStringSerializer.class)
    private String fileId;
    private Integer chunkIndex;
    private String snippet;        // 相关片段预览
}