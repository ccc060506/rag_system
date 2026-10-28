package com.ragsystem.common.util;

import com.ragsystem.module.KnowledgeService.knowledgeBase.domain.entity.ParsedDocument;
import com.ragsystem.module.KnowledgeService.knowledgeBase.service.FileParser;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.util.Map;

@Component
public class TikaFileParser implements FileParser {
    private final Tika tika = new Tika();

    @Override
    public ParsedDocument parse(InputStream is, String fileName) {
        try {
            Metadata metadata = new Metadata();
            String text = tika.parseToString(is, metadata);

            ParsedDocument doc = new ParsedDocument();
            doc.setFileName(fileName);
            doc.setMimeType(tika.detect(fileName));
            doc.setContent(text);
            doc.setMetadata((Map<String, Object>) metadata);
            return doc;
        } catch (Exception e) {
            throw new RuntimeException("解析失败", e);
        }
    }
}