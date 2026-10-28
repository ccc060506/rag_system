package com.ragsystem.module.KnowledgeService.knowledgeBase.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentChunk {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long fileId;                    // 关联 KnowledgeFile
    private String chunkText;               // 一段文本（500-1000 字符）
    private int chunkIndex;
    private Map<String, Object> metadata;   // 来源文件、页码、表格标题等
    private float[] embedding;              // 向量（存 PGVector / Milvus / Qdrant）
}