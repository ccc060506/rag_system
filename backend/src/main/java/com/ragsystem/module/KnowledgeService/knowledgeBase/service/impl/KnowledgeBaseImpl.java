package com.ragsystem.module.KnowledgeService.knowledgeBase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ragsystem.common.result.PageResult;
import com.ragsystem.common.util.UserContext;
import com.ragsystem.module.KnowledgeService.knowledgeBase.domain.entity.KnowledgeFile;
import com.ragsystem.module.KnowledgeService.knowledgeBase.domain.entity.ParsedDocument;
import com.ragsystem.module.KnowledgeService.knowledgeBase.mapper.KnowledgeBaseMapper;
import com.ragsystem.module.KnowledgeService.knowledgeBase.service.IKnowledgeBaseService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.logical.And;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 使用 LangChain4j + Apache Tika 3.2.3 实现文件解析和切分
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseImpl extends ServiceImpl<KnowledgeBaseMapper,KnowledgeFile> implements IKnowledgeBaseService{

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final AliOssServiceImpl aliOssService;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ApacheTikaDocumentParser tikaParser = new ApacheTikaDocumentParser();
    private final DocumentSplitter splitter = DocumentSplitters.recursive(400, 100);

    @Override
    @Transactional
    public String uploadAndProcess(MultipartFile multipartFile,Boolean isPublic) {
        Long ownerId = UserContext.getUserId();
        if (ownerId == null) {
            throw new SecurityException("未登录，无法上传文件");
        }
        boolean publicFile = Boolean.TRUE.equals(isPublic);
        // 1: 保存文件元数据
        KnowledgeFile knowledgeFile = saveKnowledgeFile(multipartFile, publicFile, ownerId);
        // 2: 使用 LangChain4j + Tika 解析文件
        ParsedDocument parsedDocument = parseWithLangChain4jTika(multipartFile);
        // 3: 切块 + 生成 embedding + 存入 Redis
        int chunkCount = processAndStoreChunks(parsedDocument, knowledgeFile.getId(), publicFile,
                ownerId, UserContext.getUsername());
        return String.format(" 文件【%s】处理成功！共生成 %d 个向量块，已存入 Redis Stack。",
                knowledgeFile.getFileName(), chunkCount);
    }

    @Override
    public ParsedDocument parseFile(MultipartFile file) {
        return parseWithLangChain4jTika(file);
    }

    // 分页查询：普通用户只看自己的和公开的文件，管理员看全部
    public PageResult<KnowledgeFile> pageSelect(Integer pageNum,Integer pageSize){
        pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Page<KnowledgeFile> mpPage = new Page<>(pageNum,pageSize);
        LambdaQueryWrapper<KnowledgeFile> wrapper = new LambdaQueryWrapper<>();
        applyVisibility(wrapper);
        wrapper.orderByDesc(KnowledgeFile::getUploadTime);
        Page<KnowledgeFile> knowledgeFilePage = knowledgeBaseMapper.selectPage(mpPage, wrapper);
        return PageResult.fromPageHelper(knowledgeFilePage);
    }

    // 带条件分页查询
    public PageResult<KnowledgeFile> pageSelectByOffer(Integer pageNum, Integer pageSize, String text){
        pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Page<KnowledgeFile> mpPage = new Page<>(pageNum,pageSize);
        LambdaQueryWrapper<KnowledgeFile> wrapper = new LambdaQueryWrapper<>();
        if(text!=null&&!text.isEmpty()){
            wrapper.like(KnowledgeFile::getFileName,text);
        }
        applyVisibility(wrapper);
        wrapper.orderByDesc(KnowledgeFile::getUploadTime);
        Page<KnowledgeFile> knowledgeFilePage = knowledgeBaseMapper.selectPage(mpPage, wrapper);
        return PageResult.fromPageHelper(knowledgeFilePage);
    }

    public void deleteById(Long id){
        KnowledgeFile file = knowledgeBaseMapper.selectById(id);
        if (file == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        if (!UserContext.isAdmin() && !Objects.equals(file.getOwnerId(), UserContext.getUserId())) {
            throw new SecurityException("无权删除该文件");
        }
        // 先删向量和 OSS 对象，再删数据库记录；任何一步失败都会保留记录，便于重试
        removeVectors(new IsEqualTo("fileId", id.toString()), true);
        // 兼容旧数据：修复前上传的文档 fileId 元数据是 "null"，按文件名 + 归属人再清一次
        if (file.getFileName() != null && file.getOwnerId() != null) {
            removeVectors(new And(
                    new IsEqualTo("fileName", file.getFileName()),
                    new IsEqualTo("ownerId", file.getOwnerId().toString())), false);
        }
        aliOssService.deleteFile(file.getFileUrl());
        knowledgeBaseMapper.deleteById(id);
    }

    /**
     * 使用 LangChain4j 的 ApacheTikaDocumentParser 解析文件（支持 PDF、Word、Excel 等）
     */
    private ParsedDocument parseWithLangChain4jTika(MultipartFile multipartFile) {
        try {
            Document langchainDoc = tikaParser.parse(multipartFile.getInputStream());

            ParsedDocument parsed = new ParsedDocument();
            parsed.setFileName(multipartFile.getOriginalFilename());
            parsed.setMimeType(multipartFile.getContentType());
            parsed.setContent(langchainDoc.text());
            parsed.setMetadata(langchainDoc.metadata().toMap()); // 将原始完整文件的相关属性先提取，后续再把切分得到的元数据存储

            return parsed;
        } catch (Exception e) {
            throw new RuntimeException("文件解析失败: " + multipartFile.getOriginalFilename(), e);
        }
    }

    /**
     * 切块 + 分批生成向量 + 批量存入 Redis
     */
    private int processAndStoreChunks(ParsedDocument parsedDocument, Long fileId, boolean isPublic,
                                      Long ownerId, String uploaderName) {
        if (fileId == null) {
            throw new IllegalStateException("文件元数据保存失败：未获取到主键");
        }
        // 写入业务核心元数据（操作人 + 权限信息，必须字段）
        Metadata metadata = Metadata.from(parsedDocument.getMetadata());
        metadata.put("fileId", String.valueOf(fileId));
        metadata.put("uploaderName", uploaderName == null ? "" : uploaderName);
        metadata.put("fileName", parsedDocument.getFileName());
        metadata.put("source", "tika-langchain4j");
        metadata.put("ownerId", String.valueOf(ownerId));
        metadata.put("isPublic", String.valueOf(isPublic));
        // 此对象包括完整的文本
        Document conceptDoc = Document.from(parsedDocument.getContent(), metadata);

        // 获取所有切块，并为每块写入 chunkIndex（Citation 依赖该字段）
        List<TextSegment> allChunks = splitter.split(conceptDoc);
        for (int i = 0; i < allChunks.size(); i++) {
            TextSegment chunk = allChunks.get(i);
            Metadata chunkMetadata = chunk.metadata().copy();
            chunkMetadata.put("chunkIndex", String.valueOf(i));
            allChunks.set(i, TextSegment.from(chunk.text(), chunkMetadata));
        }

        // 分批处理逻辑
        int batchSize = 20; // 设定每批处理 20 条
        int totalSize = allChunks.size();

        for (int i = 0; i < totalSize; i += batchSize) {
            int end = Math.min(i + batchSize, totalSize);
            List<TextSegment> batchChunks = allChunks.subList(i, end); // 得到当前批次的分块数据集合
            List<Embedding> batchEmbeddings = embeddingModel.embedAll(batchChunks).content(); // 进行向量化
            embeddingStore.addAll(batchEmbeddings, batchChunks);
        }
        return totalSize;
    }

    /**
     * 删除向量。RedisEmbeddingStore 在过滤结果为空时会执行无参数的 DEL 命令并报错，
     * 这种情况等同于“没有需要删除的向量”，不应阻断后续删除。
     */
    private void removeVectors(dev.langchain4j.store.embedding.filter.Filter filter, boolean required) {
        try {
            embeddingStore.removeAll(filter);
        } catch (Exception e) {
            if (isEmptyDelete(e)) {
                return;
            }
            if (required) {
                throw new RuntimeException("删除知识库向量失败，请稍后重试", e);
            }
            log.warn("[RAG] 兼容旧数据的向量清理失败，已跳过", e);
        }
    }

    private boolean isEmptyDelete(Exception e) {
        String message = e.getMessage();
        return message != null && message.contains("wrong number of arguments");
    }

    private void applyVisibility(LambdaQueryWrapper<KnowledgeFile> wrapper) {
        if (UserContext.isAdmin()) {
            return;
        }
        Long userId = UserContext.getUserId();
        if (userId == null) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.and(w -> w.eq(KnowledgeFile::getOwnerId, userId)
                .or().eq(KnowledgeFile::getIsPublic, true));
    }

    private KnowledgeFile saveKnowledgeFile(MultipartFile file, boolean isPublic, Long ownerId) {
        String ossUrl = aliOssService.uploadFile(file); // 上传文件到 OSS，返回 URL
        KnowledgeFile kf = new KnowledgeFile();
        kf.setFileName(file.getOriginalFilename());
        kf.setFileType(getFileExtension(file.getOriginalFilename()));
        kf.setFileUrl(ossUrl);
        kf.setFileSize(file.getSize());
        kf.setOwnerId(ownerId);
        kf.setIsPublic(isPublic);
        kf.setUploadTime(LocalDateTime.now());
        knowledgeBaseMapper.insertToKnowledgeBase(kf);
        return kf;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "unknown";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
