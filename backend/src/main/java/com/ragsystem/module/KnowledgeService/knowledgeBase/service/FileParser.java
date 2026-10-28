package com.ragsystem.module.KnowledgeService.knowledgeBase.service;

import com.ragsystem.module.KnowledgeService.knowledgeBase.domain.entity.ParsedDocument;
import java.io.InputStream;

public interface FileParser {
    ParsedDocument parse(InputStream inputStream, String fileName) throws Exception;
}
