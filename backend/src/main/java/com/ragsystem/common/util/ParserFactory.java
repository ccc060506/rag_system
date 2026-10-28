package com.ragsystem.common.util;

import com.ragsystem.module.KnowledgeService.knowledgeBase.service.FileParser;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ParserFactory {
    @Autowired
    private TikaFileParser tikaParser;
    // @Autowired private PoiExcelParser poiParser; // 如果需要特殊处理 Excel 表格

    public FileParser getParser(String fileName) {
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        return switch (ext) {
            case "pdf", "docx", "xlsx", "pptx", "txt", "md" -> tikaParser;
            // case "xlsx" -> poiParser; // 特殊处理
            default -> tikaParser;
        };
    }
}