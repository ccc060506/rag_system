package com.ragsystem.module.KnowledgeService.knowledgeBase.service.impl;

import com.ragsystem.module.KnowledgeService.knowledgeBase.service.IKnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@Slf4j
@Service // 标记为 Spring 服务，让容器管理
public class AliOssServiceImpl{

    // 从配置文件读取 OSS 配置
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;
    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;
    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    public String uploadFile(MultipartFile file) {
        try {
            // 1. 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + originalFilename;
            // 2. 创建 OSS 客户端
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            // 3. 上传文件到 OSS
            ossClient.putObject(bucketName, fileName, file.getInputStream());
            // 4. 拼接访问 URL
            String fileUrl = "https://" + bucketName + "." + endpoint.substring(endpoint.indexOf("//") + 2) + "/" + fileName;
            // 5. 关闭客户端
            ossClient.shutdown();
            log.info("文件上传 OSS 成功：{}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("文件上传 OSS 失败", e);
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 删除 OSS 上的文件。fileUrl 来自知识库记录，例如 https://bucket.endpoint/key。
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        String objectKey = extractObjectKey(fileUrl);
        if (objectKey == null || objectKey.isBlank()) {
            log.warn("无法从 OSS 地址解析对象名，跳过删除：{}", fileUrl);
            return;
        }
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            ossClient.deleteObject(bucketName, objectKey);
            log.info("OSS 文件删除成功：{}", objectKey);
        } catch (Exception e) {
            log.error("OSS 文件删除失败：{}", objectKey, e);
            throw new RuntimeException("删除 OSS 文件失败：" + e.getMessage(), e);
        } finally {
            ossClient.shutdown();
        }
    }

    private String extractObjectKey(String fileUrl) {
        String hostMarker = "://" + bucketName + ".";
        int hostIndex = fileUrl.indexOf(hostMarker);
        if (hostIndex < 0) {
            return null;
        }
        int pathIndex = fileUrl.indexOf('/', hostIndex + hostMarker.length());
        return pathIndex < 0 ? null : fileUrl.substring(pathIndex + 1);
    }

}