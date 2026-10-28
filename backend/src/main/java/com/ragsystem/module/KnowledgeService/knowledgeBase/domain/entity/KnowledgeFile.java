package com.ragsystem.module.KnowledgeService.knowledgeBase.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeFile {
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String fileName;        // 原文件名
    private String fileType;        // pdf / xlsx / docx
    private String fileUrl;         // OSS 存储路径
    private Long fileSize;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ownerId;           // 上传者
    private Boolean isPublic;       // 是否公开给其他用户检索
    private LocalDateTime uploadTime;
}