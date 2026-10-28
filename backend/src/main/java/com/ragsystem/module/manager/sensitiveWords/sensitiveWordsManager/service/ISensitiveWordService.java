package com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ragsystem.common.result.PageResult;
import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.domain.dto.SensitiveWordDTO;
import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.domain.entity.SensitiveWord;
import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.domain.vo.SensitiveWordVO;
import java.util.List;

public interface ISensitiveWordService extends IService<SensitiveWord> {

    List<String> getAllValidWords();

    void buildAndSaveToRedis(List<String> words);

    void insert(List<SensitiveWordDTO> words);

    void updateWord(Long id, SensitiveWordDTO sensitiveWordDTO);

    void deleteSensitiveWordById(Long id);

    SensitiveWordVO getSensitiveWordById(Long id);

    PageResult<SensitiveWordVO> getSensitiveWordPage(Integer num, Integer size);

    PageResult<SensitiveWordVO> getSensitiveWordPageByOffer(Integer num, Integer size, String word);
}
