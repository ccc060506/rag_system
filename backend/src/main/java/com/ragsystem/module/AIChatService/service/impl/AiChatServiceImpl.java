package com.ragsystem.module.AIChatService.service.impl;

import com.ragsystem.common.util.DFAFilter;
import com.ragsystem.common.util.UserContext;
import com.ragsystem.module.AIChatService.service.IAiChatService;
import com.ragsystem.module.AIChatService.domain.entity.ChatRequest;
import com.ragsystem.module.AIChatService.domain.entity.Citation;
import com.ragsystem.module.AIChatService.service.ConversationMemory;
import com.ragsystem.module.manager.logManager.domain.entity.AIChatLog;
import com.ragsystem.module.manager.logManager.mapper.AIChatLogMapper;
import com.ragsystem.module.userCenter.mapper.UserConfigMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import com.ragsystem.module.AIChatService.domain.entity.ChatResponse;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiChatServiceImpl implements IAiChatService {

    private final StreamingChatModel streamingChatModel;
    private final EmbeddingModel embeddingModel;
    private final RedisEmbeddingStore embeddingStore;
    private final ConversationMemory conversationMemory;
    private final DFAFilter dfaFilter;

    @Autowired
    private AIChatLogMapper aiChatLogMapper;
    @Autowired
    private UserConfigMapper userConfigMapper;

    private static final int    TOP_K            = 8;
    private static final double HIGH_THRESHOLD   = 0.70;
    private static final double LOW_THRESHOLD    = 0.30;
    private static final int    MIN_RESULTS      = 3;
    private static final int    MEMORY_TURNS     = 4;
    private static final int    PREVIEW_LEN      = 100;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是企业知识库助手。
            1. 优先基于【参考资料】回答。
            2. 如果资料中没有，但问题属于通用常识（如 1+1 或日常问候），请直接回答。
            3. 严禁编造虚假的公司内部政策或业务数据。
            4. 请务必将你的计算过程和最终答案包裹在同一个 Markdown 代码块中。
               格式规范：
                   先输出一个空行。
                   使用 ``` 开启代码块（不带语言后缀）。
                   在块内展示过程和结果。
                   使用 ``` 结束代码块。
                   再输出一个空行。
                   严禁在代码块之外直接书写加号(+)等特殊符号，以防前端渲染出错
            5. 如果资料中提到了上传者(uploaderName)，请在回答中顺带说明。
                       例如：“根据 [张三] 上传的 [员工手册]，公司规定...”。
            6. 如果问题不在参考资料当中，那么回答时候不要强调参考资料没有或者使用你自己的知识库回答。
            【参考资料】
            %s
            """;

    @Override
    public Flux<ChatResponse> chatWithStream(ChatRequest request, Long userId) throws Exception {
        String sessionId = StringUtils.hasText(request.getSessionId())
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        // 1. 向量化问题 (dashscope Embedding)
        String question = request.getQuestion();
        List<String> userSensitiveWords = dfaFilter.check(question);
        if(!userSensitiveWords.isEmpty()){
            return Flux.just(ChatResponse.ofError(sessionId, "您的消息包含敏感词汇，请修改后重新提问！"));
        }
        Embedding questionEmbedding = embeddingModel.embed(question).content();

        // 2. 检索知识库 (走 Redis)
        List<EmbeddingMatch<TextSegment>> matches = retrieveWithFallback(questionEmbedding, userId);
        log.info("[RAG调试] 检索到的片段数量: {}, 检索到的内容预览: {}",
                matches.size(),
                matches.isEmpty() ? "无" : matches.get(0).embedded().text());

        // 预览片段
        String context = buildContext(matches);
        // 参考文献
        List<Citation> citations = buildCitations(matches);

        // 3. 构建提示词
        String memoryKey = userId + ":" + sessionId;
        restoreHistoryFromLog(memoryKey, sessionId, userId);
        List<ChatMessage> messages = buildMessages(memoryKey, request.getQuestion(), context);

        // 4.使用标准的流式生成逻辑
        return Flux.create(emitter -> {
            streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
                private final StringBuilder fullAnswer = new StringBuilder();

                @Override
                public void onPartialResponse(String partialResponse) {
                    List<String> sensitiveHits = dfaFilter.check(partialResponse);
                    String safePartial = partialResponse;
                    if (!sensitiveHits.isEmpty()) {
                        safePartial = replaceSensitiveWords(partialResponse);
                        log.warn("[敏感词拦截] AI流式输出中检测到敏感词: {}", sensitiveHits);
                    }

                    emitter.next(com.ragsystem.module.AIChatService.domain.entity
                            .ChatResponse.ofToken(sessionId, safePartial));
                    fullAnswer.append(safePartial);
                }

                @Override
                public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse completeResponse) {
                    // 响应结束
                    emitter.next(ChatResponse.ofDone(sessionId, citations));
                    emitter.complete(); // 结束 Flux 流

                    long totalTokens = (completeResponse.metadata().tokenUsage() != null)
                            ? completeResponse.metadata().tokenUsage().totalTokenCount() : 0L;

                    saveChatLogInternal(userId, request.getQuestion(),
                            fullAnswer.toString(), matches, totalTokens);
                    // 更新token消耗 使用累加
                    userConfigMapper.updateUsage(userId, totalTokens);
                    // 保存多轮对话记忆
                    conversationMemory.append(memoryKey,
                            UserMessage.from(request.getQuestion()),
                            completeResponse.aiMessage());

                    log.info("[RAG] session={} 回答完成，Tokens={}", sessionId, totalTokens);
                }

                @Override
                public void onError(Throwable error) {
                    log.error("[RAG] 流式调用异常 session={}", sessionId, error);
                    emitter.error(error);
                }
            });
        });
    }

    private List<EmbeddingMatch<TextSegment>> retrieveWithFallback(Embedding questionEmbedding, Long userId) {
        List<EmbeddingMatch<TextSegment>> highScore = doRetrieve(questionEmbedding, userId, HIGH_THRESHOLD);
        if (highScore.size() >= MIN_RESULTS) {
            return highScore;
        }
        // 阈值降级时合并结果，避免丢掉已经命中的高分片段
        List<EmbeddingMatch<TextSegment>> lowScore = doRetrieve(questionEmbedding, userId, LOW_THRESHOLD);
        return mergeMatches(highScore, lowScore, TOP_K);
    }

    private List<EmbeddingMatch<TextSegment>> doRetrieve(Embedding questionEmbedding, Long userId, double threshold) {
        // 权限过滤：普通用户只能检索自己的文档和公开文档；管理员可检索全部。
        // 拆成两次查询而不是 OR 过滤器，避免 RediSearch 混合查询对 OR 优先级的解析差异。
        if (UserContext.isAdmin()) {
            return search(questionEmbedding, threshold, null);
        }
        if (userId == null) {
            log.warn("[RAG] 未获取到用户ID，拒绝检索");
            return List.of();
        }
        List<EmbeddingMatch<TextSegment>> own = search(questionEmbedding, threshold,
                new IsEqualTo("ownerId", userId.toString()));
        List<EmbeddingMatch<TextSegment>> publicDocs = search(questionEmbedding, threshold,
                new IsEqualTo("isPublic", "true"));
        return mergeMatches(own, publicDocs, TOP_K);
    }

    private List<EmbeddingMatch<TextSegment>> search(Embedding questionEmbedding, double threshold, Filter filter) {
        EmbeddingSearchRequest.EmbeddingSearchRequestBuilder builder = EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding)
                .maxResults(TOP_K)
                .minScore(threshold);
        if (filter != null) {
            builder.filter(filter);
        }
        try {
            return embeddingStore.search(builder.build()).matches();
        } catch (Exception e) {
            // 检索失败时闭锁返回空结果，绝不退化成无过滤查询
            log.error("[RAG] 向量检索失败，已返回空结果", e);
            return List.of();
        }
    }

    private List<EmbeddingMatch<TextSegment>> mergeMatches(List<EmbeddingMatch<TextSegment>> first,
                                                           List<EmbeddingMatch<TextSegment>> second,
                                                           int limit) {
        Map<String, EmbeddingMatch<TextSegment>> merged = new LinkedHashMap<>();
        first.forEach(match -> merged.put(match.embeddingId(), match));
        second.forEach(match -> merged.putIfAbsent(match.embeddingId(), match));
        return merged.values().stream()
                .sorted(Comparator.comparingDouble(
                        (EmbeddingMatch<TextSegment> match) -> match.score()).reversed())
                .limit(limit)
                .toList();
    }

    private String buildContext(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches.isEmpty()) return "（未检索到相关知识库内容）";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            var meta = matches.get(i).embedded().metadata();
            String uploader = meta.getString("uploaderName");
            sb.append(String.format("[%d] (上传者:%s): %s\n\n",
                    i + 1, uploader, matches.get(i).embedded().text()));
        }
        return sb.toString().trim();
    }

    private List<ChatMessage> buildMessages(String memoryKey, String question, String context) {
        List<ChatMessage> messages = new ArrayList<>();
        // langchain4j 框架要求按照 系统提示，历史对话，当前问题 的顺序构建提示词
        messages.add(SystemMessage.from(String.format(SYSTEM_PROMPT_TEMPLATE, context)));
        messages.addAll(conversationMemory.getHistory(memoryKey, MEMORY_TURNS * 2));
        messages.add(UserMessage.from(question));
        return messages;
    }

    /**
     * 从历史对话记录恢复一轮上下文：sessionId 为 log-<id> 时，
     * 首次提问前把该条问答写入会话记忆，后续提问即可基于它继续。
     */
    private void restoreHistoryFromLog(String memoryKey, String sessionId, Long userId) {
        if (sessionId == null || !sessionId.startsWith("log-")) {
            return;
        }
        if (!conversationMemory.getHistory(memoryKey, 1).isEmpty()) {
            return;
        }
        long logId;
        try {
            logId = Long.parseLong(sessionId.substring(4));
        } catch (NumberFormatException e) {
            return;
        }
        AIChatLog logEntity = aiChatLogMapper.selectById(logId);
        if (logEntity == null) {
            return;
        }
        if (!Objects.equals(logEntity.getUserId(), userId) && !UserContext.isAdmin()) {
            log.warn("[RAG] 跳过无权访问的历史对话 logId={} userId={}", logId, userId);
            return;
        }
        if (!StringUtils.hasText(logEntity.getQuestion()) || !StringUtils.hasText(logEntity.getAnswer())) {
            return;
        }
        conversationMemory.append(memoryKey,
                UserMessage.from(logEntity.getQuestion()),
                AiMessage.from(logEntity.getAnswer()));
        log.info("[RAG] 历史对话已载入上下文 logId={} userId={}", logId, userId);
    }

    private List<Citation> buildCitations(List<EmbeddingMatch<TextSegment>> matches) {
        return matches.stream()
                .map(match -> {
                    var m = match.embedded().metadata();
                    return new Citation(m.getString("fileName"), m.getString("fileId"),
                            m.getInteger("chunkIndex"), match.embedded().text());
                }).collect(Collectors.toList());
    }

    private void saveChatLogInternal(Long userId, String question, String answer,
                                     List<EmbeddingMatch<TextSegment>> matches, long totalTokens) {
        try {
            AIChatLog logEntity = new AIChatLog();
            logEntity.setUserId(userId);
            logEntity.setQuestion(question);
            logEntity.setAnswer(answer);
            logEntity.setTokensUsed(totalTokens);
            logEntity.setModelName("deepseek-chat");
            logEntity.setCreateTime(LocalDateTime.now());
            // 处理来源文件名称
            if (matches != null && !matches.isEmpty()) {
                String sourceNames = matches.stream()
                        .map(match -> match.embedded().metadata().getString("fileName"))
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.joining(","));
                logEntity.setSourceContext(sourceNames);
            } else {
                logEntity.setSourceContext("无来源");
            }
            // 执行插入
            aiChatLogMapper.insert(logEntity);
            log.info("[Log] 聊天记录已成功存入数据库");
        } catch (Exception e) {
            log.error("[Log] 保存失败", e);
        }
    }
    private String replaceSensitiveWords(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        List<String> hits = dfaFilter.check(text);
        String result = text;
        for (String word : new HashSet<>(hits)) {  // 去重
            String stars = "*".repeat(word.length());
            result = result.replace(word, stars);
        }
        return result;
    }
}
