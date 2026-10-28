package com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.domain.entity.SensitiveWord;
import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.domain.vo.SensitiveWordVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {

    List<SensitiveWordVO> getHotSensitiveWord();
}
