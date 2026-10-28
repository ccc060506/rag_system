package com.ragsystem.module.KnowledgeService.knowledgeBase.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedDocument {
    private String fileName;
    private String mimeType;
    private String content;              // 完整文本
    private Map<String, Object> metadata;
    private List<DocumentChunk> chunks;  // 后续切分

    // 兼容 LangChain4j 的 Document
    public dev.langchain4j.data.document.Document toLangChainDocument() {
        return dev.langchain4j.data.document.Document.from(
                content,
                dev.langchain4j.data.document.Metadata.from(metadata)
        );
    }
}