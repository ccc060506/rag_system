package com.ragsystem.module.KnowledgeService.knowledgeBase.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ragsystem.common.result.PageResult;
import com.ragsystem.module.KnowledgeService.knowledgeBase.domain.entity.KnowledgeFile;
import com.ragsystem.module.KnowledgeService.knowledgeBase.domain.entity.ParsedDocument;
import com.ragsystem.module.KnowledgeService.knowledgeBase.domain.vo.KnowledgeFileVO;
import org.springframework.web.multipart.MultipartFile;

public interface IKnowledgeBaseService extends IService<KnowledgeFile> {

    String uploadAndProcess(MultipartFile file,Boolean isPublic);

    ParsedDocument parseFile(MultipartFile file);

    PageResult<KnowledgeFile> pageSelect(Integer pageNum, Integer PageSize);

    PageResult<KnowledgeFile> pageSelectByOffer(Integer pageNum, Integer pageSize, String text);

    void deleteById(Long id);
}
