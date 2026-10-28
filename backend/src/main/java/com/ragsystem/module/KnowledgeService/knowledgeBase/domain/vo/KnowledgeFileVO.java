package com.ragsystem.module.KnowledgeService.knowledgeBase.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeFileVO {
    private String fileName;        // 原文件名
    private String fileType;        // pdf / xlsx / docx
    private String fileUrl;         // OSS 存储路径
    private Long fileSize;
    private LocalDateTime uploadTime;
}
