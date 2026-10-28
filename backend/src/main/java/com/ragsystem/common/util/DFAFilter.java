package com.ragsystem.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import com.ragsystem.common.constant.RedisKeyConstants;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class DFAFilter {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 本地缓存，提高检测性能；volatile 保证重建后其他线程立即可见
    private volatile Map<Character, Object> sensitiveWordMap = Collections.emptyMap();

    /**
     * 构建 DFA 树并同步到 Redis
     */
    public void buildDFA(List<String> words) {
        if (CollectionUtils.isEmpty(words)) {
            return;
        }

        // 1. 先构建本地临时树，避免多线程冲突
        Map<Character, Object> newTree = new HashMap<>();
        for (String word : words) {
            Map<Character, Object> cur = newTree;
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                // 强制类型转换优化
                cur = (Map<Character, Object>) cur.computeIfAbsent(c, k -> new HashMap<Character, Object>());
                if (i == word.length() - 1) {
                    cur.put('\0', null); // 终止标记
                }
            }
        }

        // 2. 更新本地缓存
        this.sensitiveWordMap = newTree;

        // 3. 异步或安全保存到 Redis
        try {
            // 确保 redisTemplate 已注入
            if (redisTemplate != null) {
                // 直接存储 Map 对象
                // spring-boot-start-data-redis 的JDK序列化
                redisTemplate.opsForValue().set(RedisKeyConstants.DFA_TREE, newTree, 30, TimeUnit.DAYS);
                log.info("DFA 敏感词树已同步至 Redis，词数：{}", words.size());
            } else {
                log.warn("RedisTemplate 尚未注入，DFA 树仅在本地内存生效");
            }
        } catch (Exception e) {
            log.error("保存 DFA 树到 Redis 失败", e);
        }
    }

    /**
     * 检测文本
     */
    public List<String> check(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> hits = new ArrayList<>();
        Map<Character, Object> root = sensitiveWordMap;
        for (int i = 0; i < text.length(); i++) {
            Map<Character, Object> cur = root;
            int j = i;
            while (j < text.length()) {
                char c = text.charAt(j);
                Object next = cur.get(c);
                if (next == null) {
                    break;
                }
                cur = (Map<Character, Object>) next;
                if (cur.containsKey('\0')) {
                    hits.add(text.substring(i, j + 1));
                }
                j++;
            }
        }
        return hits;
    }
}