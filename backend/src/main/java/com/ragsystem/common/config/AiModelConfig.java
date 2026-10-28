package com.ragsystem.common.config;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.schemafields.SchemaField;
import com.ragsystem.module.AIChatService.service.Assistant;
import redis.clients.jedis.search.schemafields.TagField;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Configuration
public class AiModelConfig {

    // DeepSeek API Key
    @Value("${spring.ai.openai.api-key}")
    private String deepSeekApiKey;
    // 千问 Api Key
    @Value("${aliyun.dashscope.api-key}")
    private String dashscopeApiKey;
    // DeepSeek Base URL（默认值是 DeepSeek 官方地址）
    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String deepSeekBaseUrl;
    // 千问 URL（默认值是 千问 官方地址）
    @Value("${aliyun.dashscope.base-url}")
    private String dashScopeBaseUrl;
    // 可选 HTTP 代理，host 为空表示不使用代理
    @Value("${rag.ai.proxy.host:}")
    private String proxyHost;
    @Value("${rag.ai.proxy.port:0}")
    private int proxyPort;


    @Value("${rag.redis.index-name}")
    private String indexName;
    @Value("${rag.redis.prefix}")
    private String prefix;
    // 0 表示启动时按实际 embedding 模型自动探测；探测失败时才回退到这个值
    @Value("${rag.redis.dimension:0}")
    private int configuredDimension;
    // 向量化模型名，更换模型时只需改配置
    @Value("${rag.embedding.model:text-embedding-v1}")
    private String embeddingModelName;

    // 记录索引由哪个 embedding 模型生成，防止同维度换模型导致向量不可比
    private static final String EMBEDDING_MODEL_KEY = "rag-meta:embedding-model";


    /**
     * DeepSeek 聊天模型（OpenAI 兼容接口）
     * 用于对话、生成回答等场景
     */
    @Bean
    public StreamingChatModel deepSeekStreamingChatModel() {
        java.net.http.HttpClient.Builder proxyBuilder = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60));
        if (!proxyHost.isBlank()) {
            proxyBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));
        }

        return OpenAiStreamingChatModel.builder()
                .apiKey(deepSeekApiKey)
                .baseUrl(deepSeekBaseUrl)
                .modelName("deepseek-chat")
                .temperature(0.7)
                .timeout(Duration.ofSeconds(60))
                .httpClientBuilder(JdkHttpClient.builder().httpClientBuilder(proxyBuilder))
                .build();
    }


    @Bean
    public ChatModel deepSeekChatModel() {
        return dev.langchain4j.model.openai.OpenAiChatModel.builder()
                .apiKey(deepSeekApiKey)
                .baseUrl(deepSeekBaseUrl)
                .modelName("deepseek-chat")
                .temperature(0.7)
                .build();
    }


    /**
     * 阿里千问 向量化模型
     */
    @Bean
    public OpenAiEmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(dashscopeApiKey)
                .baseUrl(dashScopeBaseUrl)
                .modelName(embeddingModelName)
                .maxRetries(3)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public RedisEmbeddingStore redisEmbeddingStore(
            OpenAiEmbeddingModel embeddingModel,
            @Value("${spring.data.redis.host}") String redisHost,
            @Value("${spring.data.redis.port}") int redisPort,
            @Value("${spring.data.redis.password:}") String redisPassword,
            @Value("${spring.data.redis.database:0}") int redisDatabase
    ) {
        // 本地 Redis 可无密码；云 Redis 通常需要密码，这里与 spring.data.redis.* 保持一致
        DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder()
                .database(redisDatabase);
        if (redisPassword != null && !redisPassword.isBlank()) {
            clientConfigBuilder.password(redisPassword);
        }
        JedisPooled jedis = new JedisPooled(
                new HostAndPort(redisHost, redisPort), clientConfigBuilder.build());
        int dimension = resolveDimension(embeddingModel);

        boolean indexExists = jedis.ftList().contains(indexName);
        if (indexExists) {
            validateIndexDimension(jedis, embeddingModelName, dimension);
            validateEmbeddingModel(jedis, embeddingModelName);
        }

        Map<String, SchemaField> metadataConfig = new HashMap<>();
        // Redis Stack 使用 JSON 索引，字段必须写成 JSONPath 并显式指定查询别名。
        // 旧写法 new TagField("ownerId") 只适用于 HASH 索引，会导致建索引失败、过滤查询报错。
        metadataConfig.put("ownerId", new TagField("$.ownerId").as("ownerId"));
        metadataConfig.put("isPublic", new TagField("$.isPublic").as("isPublic"));
        metadataConfig.put("fileId", new TagField("$.fileId").as("fileId"));
        metadataConfig.put("fileName", new TagField("$.fileName").as("fileName"));
        metadataConfig.put("uploaderName", new TagField("$.uploaderName").as("uploaderName"));
        // Metadata 的值统一是字符串，因此 chunkIndex 也用 TAG，避免 NUMERIC 字段索引失败
        metadataConfig.put("chunkIndex", new TagField("$.chunkIndex").as("chunkIndex"));

        RedisEmbeddingStore store = new RedisEmbeddingStore(
                jedis,
                indexName,
                prefix,
                dimension,
                metadataConfig
        );
        // 索引可能是旧版本创建的，补齐缺失字段后才能安全地按 ownerId/fileId 过滤
        ensureIndexMetadata(jedis, indexName, metadataConfig);
        // 新建索引时记录生成它的模型，后续换模型会被拦截
        if (!indexExists) {
            jedis.set(EMBEDDING_MODEL_KEY, embeddingModelName);
        }
        return store;
    }

    /**
     * 自动获取 embedding 维度。LangChain4j 的 dimension() 会优先使用已知映射，
     * 未知模型则自动做一次极小 probe 并把结果缓存下来。
     */
    private int resolveDimension(OpenAiEmbeddingModel embeddingModel) {
        if (dashscopeApiKey == null || dashscopeApiKey.isBlank()) {
            throw new IllegalStateException(
                    "DashScope API Key 为空：请启用 local profile（--spring.profiles.active=local，"
                            + "或在 IDEA 的 Active profiles 填 local），或设置 DASHSCOPE_API_KEY 环境变量。"
                            + "default profile 下不会加载 application-local.yaml。");
        }
        try {
            int actual = embeddingModel.dimension();
            if (configuredDimension > 0 && configuredDimension != actual) {
                log.warn("[RAG] rag.redis.dimension={} 与实际模型维度 {} 不一致，以实际维度为准",
                        configuredDimension, actual);
            }
            log.info("[RAG] embedding 模型 {} 的向量维度：{}", embeddingModelName, actual);
            return actual;
        } catch (Exception e) {
            if (configuredDimension > 0) {
                log.warn("[RAG] 自动探测 embedding 维度失败，回退到配置值 {}", configuredDimension, e);
                return configuredDimension;
            }
            throw new IllegalStateException(
                    "无法自动获取 embedding 维度：请检查 DASHSCOPE_API_KEY 和网络，或显式设置 rag.redis.dimension", e);
        }
    }

    /**
     * 已有索引时校验维度。维度不一致会直接启动失败，避免把不同维度的向量混在一起。
     */
    private void validateIndexDimension(JedisPooled jedis, String modelName, int expectedDimension) {
        try {
            Map<String, Object> info = jedis.ftInfo(indexName);
            Object attributes = info.get("attributes");
            if (!(attributes instanceof Iterable<?> iterable)) {
                return;
            }
            for (Object attribute : iterable) {
                Map<String, Object> map = toAlternatingMap(attribute);
                if (!"VECTOR".equalsIgnoreCase(String.valueOf(map.get("type")))) {
                    continue;
                }
                Object dim = map.get("dim");
                if (dim == null) {
                    dim = map.get("dimension");
                }
                if (dim == null) {
                    log.warn("[RAG] 无法从 Redis 索引 {} 读取向量维度，跳过维度校验", indexName);
                    return;
                }
                int actualDimension = Integer.parseInt(String.valueOf(dim));
                if (actualDimension != expectedDimension) {
                    throw new IllegalStateException(
                            "Redis 索引 " + indexName + " 的向量维度是 " + actualDimension
                                    + "，当前模型 " + modelName + " 的维度是 " + expectedDimension
                                    + "。请执行 FT.DROPINDEX " + indexName + " DD 后重新上传文档。");
                }
                log.info("[RAG] Redis 索引维度校验通过：{} 维", actualDimension);
                return;
            }
            log.warn("[RAG] Redis 索引 {} 中没有找到向量字段，跳过维度校验", indexName);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[RAG] 读取 Redis 索引 {} 的维度失败，跳过校验", indexName, e);
        }
    }

    /**
     * Jedis 的 FT.INFO 对嵌套结构返回的是 [key1, value1, key2, value2, ...] 的 List，
     * 这里统一转成 Map 方便读取。
     */
    private Map<String, Object> toAlternatingMap(Object raw) {
        Map<String, Object> map = new HashMap<>();
        if (raw instanceof List<?> list) {
            for (int i = 0; i + 1 < list.size(); i += 2) {
                map.put(String.valueOf(list.get(i)), list.get(i + 1));
            }
        }
        return map;
    }

    /**
     * 校验索引是由同一个 embedding 模型生成的。
     * 仅比较维度不够：text-embedding-v1/v2 都是 1536 维，但向量不可混用。
     */
    private void validateEmbeddingModel(JedisPooled jedis, String currentModel) {
        String previousModel = jedis.get(EMBEDDING_MODEL_KEY);
        if (previousModel == null) {
            log.warn("[RAG] Redis 索引已存在但没有模型标记，无法确认历史向量使用的模型；"
                    + "当前按 {} 继续使用，如曾更换过模型请删除索引并重新上传文档。", currentModel);
            jedis.set(EMBEDDING_MODEL_KEY, currentModel);
            return;
        }
        if (!previousModel.equals(currentModel)) {
            throw new IllegalStateException(
                    "Redis 向量索引由模型 " + previousModel + " 生成，当前配置是 " + currentModel
                            + "。请执行 FT.DROPINDEX " + indexName + " DD 后重新上传文档。");
        }
    }

    /**
     * 索引已存在时，用 FT.ALTER 补齐缺失的元数据字段。
     * 若旧索引不是 JSON 类型（升级前的 HASH 索引），FT.ALTER 会失败，此时只能删除索引后重新导入文档。
     */
    private void ensureIndexMetadata(JedisPooled jedis, String indexName, Map<String, SchemaField> metadataConfig) {
        try {
            Map<String, Object> info = jedis.ftInfo(indexName);
            Map<String, Object> definitionMap = toAlternatingMap(info.get("index_definition"));
            Object keyType = definitionMap.get("key_type");
            if (keyType != null && !"JSON".equalsIgnoreCase(String.valueOf(keyType))) {
                log.error("[RAG] Redis 索引 {} 的类型是 {}，当前版本只支持 JSON 索引。"
                        + "请执行 FT.DROPINDEX {} DD 后重新上传文档。", indexName, keyType, indexName);
                return;
            }
            Set<String> existing = new HashSet<>();
            Map<String, String> existingPaths = new HashMap<>();
            Object attributes = info.get("attributes");
            if (attributes instanceof Iterable<?> iterable) {
                for (Object attribute : iterable) {
                    Map<String, Object> map = toAlternatingMap(attribute);
                    if (map.isEmpty()) {
                        continue;
                    }
                    // JSON 索引：identifier 是 JSONPath（$.fileName），attribute 是查询别名（fileName）
                    // HASH 索引：两者都是字段名
                    Object identifier = map.get("identifier");
                    Object attributeName = map.get("attribute");
                    String name = attributeName != null
                            ? String.valueOf(attributeName)
                            : (identifier == null ? null : String.valueOf(identifier));
                    if (name != null) {
                        existing.add(name);
                        existingPaths.put(name, identifier == null ? null : String.valueOf(identifier));
                    }
                }
            }

            // 旧版本可能建出了错误 JSONPath 的同名字段，FT.ALTER 无法修正，只能提示重建
            for (Map.Entry<String, SchemaField> entry : metadataConfig.entrySet()) {
                String path = existingPaths.get(entry.getKey());
                if (path != null && !("$." + entry.getKey()).equals(path)) {
                    log.error("[RAG] Redis 索引 {} 的字段 {} 使用了错误的 JSONPath: {}（应为 $.{}），"
                                    + "过滤查询会失败。请执行 FT.DROPINDEX {} DD 后重新上传文档。",
                            indexName, entry.getKey(), path, entry.getKey(), indexName);
                }
            }

            List<String> missingKeys = new ArrayList<>();
            List<SchemaField> missingFields = new ArrayList<>();
            for (Map.Entry<String, SchemaField> entry : metadataConfig.entrySet()) {
                if (!existing.contains(entry.getKey())) {
                    missingKeys.add(entry.getKey());
                    missingFields.add(entry.getValue());
                }
            }
            if (missingFields.isEmpty()) {
                return;
            }
            jedis.ftAlter(indexName, missingFields);
            log.info("[RAG] Redis 索引 {} 已补充字段: {}", indexName, missingKeys);
        } catch (Exception e) {
            log.error("[RAG] Redis 索引 {} 字段校验失败，检索过滤可能不可用。"
                            + "请确认索引是 JSON 类型且包含 ownerId/isPublic 等字段；必要时删除该索引并重新上传文档。",
                    indexName, e);
        }
    }

    /**
     * 关联 ChatModel + Redis存储 + Embedding模型
     */
    @Bean
    public Assistant assistant(ChatModel chatModel,
                               EmbeddingStore<TextSegment> embeddingStore,
                               OpenAiEmbeddingModel embeddingModel) {

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.5)
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();
    }
}