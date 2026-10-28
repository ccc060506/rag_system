package com.ragsystem.common.util;

import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.service.ISensitiveWordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SensitiveWordInitializer implements CommandLineRunner {
    private Logger log = LoggerFactory.getLogger(SensitiveWordInitializer.class);

    @Autowired
    private ISensitiveWordService sensitiveWordService;  // 你的 Service

    @Override
    public void run(String... args) throws Exception {
        log.info("===== 开始初始化敏感词 DFA 树 =====");

        // 调用 Service 方法
        List<String> words = sensitiveWordService.getAllValidWords();

        if (words.isEmpty()) {
            log.warn("无有效敏感词，跳过构建");
            return;
        }

        // 或者 sensitiveWordService.buildAndSaveToRedis(words);
        sensitiveWordService.buildAndSaveToRedis(words);

        log.info("===== 敏感词 DFA 树初始化完成，词数：{} =====", words.size());
    }
}