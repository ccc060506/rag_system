package com.ragsystem.module.KnowledgeService.knowledgeBase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ragsystem.module.KnowledgeService.knowledgeBase.domain.entity.KnowledgeFile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeFile> {

    void insertToKnowledgeBase(KnowledgeFile kf);
}
