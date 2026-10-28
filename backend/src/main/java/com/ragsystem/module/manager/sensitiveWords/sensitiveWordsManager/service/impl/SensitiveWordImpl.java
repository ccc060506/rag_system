package com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ragsystem.common.result.PageResult;
import com.ragsystem.common.util.DFAFilter;
import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.domain.dto.SensitiveWordDTO;
import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.domain.entity.SensitiveWord;
import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.domain.vo.SensitiveWordVO;
import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.mapper.SensitiveWordMapper;
import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.service.ISensitiveWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SensitiveWordImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord> implements ISensitiveWordService {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DFAFilter dfaFilter;

    @Transactional
    public void insert(List<SensitiveWordDTO> words){
        List<SensitiveWord> sensitiveWords = new ArrayList<>();
        for(SensitiveWordDTO word:words){
            SensitiveWord newWord = new SensitiveWord();
            BeanUtils.copyProperties(word,newWord);
            newWord.setCreateTime(LocalDateTime.now());
            newWord.setUpdateTime(LocalDateTime.now());
            sensitiveWords.add(newWord);
        }
        sensitiveWordMapper.insert(sensitiveWords);
        refreshDfa();
    }

    @Transactional
    public void updateWord(Long id, SensitiveWordDTO sensitiveWordDTO){
        SensitiveWord sensitiveWord = sensitiveWordMapper.selectById(id);
        if(sensitiveWord==null){
            throw new IllegalArgumentException("该敏感词不存在，无法修改");
        }
        if (sensitiveWordDTO.getWord() != null) {
            sensitiveWord.setWord(sensitiveWordDTO.getWord());
        }
        if (sensitiveWordDTO.getCategory() != null) {
            sensitiveWord.setCategory(sensitiveWordDTO.getCategory());
        }
        if (sensitiveWordDTO.getLevel() != null) {
            sensitiveWord.setLevel(sensitiveWordDTO.getLevel());
        }
        sensitiveWord.setUpdateTime(LocalDateTime.now());
        sensitiveWordMapper.updateById(sensitiveWord);
        refreshDfa();
    }

    public SensitiveWordVO getSensitiveWordById(Long id){
        SensitiveWord sensitiveWord = sensitiveWordMapper.selectById(id);
        if (sensitiveWord == null) {
            return null;
        }
        SensitiveWordVO sensitiveWordVO = new SensitiveWordVO();
        BeanUtils.copyProperties(sensitiveWord,sensitiveWordVO);
        return sensitiveWordVO;
    }

    @Transactional
    public void deleteSensitiveWordById(Long id){
        sensitiveWordMapper.deleteById(id);
        refreshDfa();
    }

    /**
     * 基础分页查询
     */
    public PageResult<SensitiveWordVO> getSensitiveWordPage(Integer pageNum, Integer pageSize) {
        pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Page<SensitiveWord> mpPage = new Page<>(pageNum, pageSize);
        Page<SensitiveWord> sensitiveWordPage = sensitiveWordMapper.selectPage(mpPage, null);
        List<SensitiveWord> sourceList = sensitiveWordPage.getRecords();
        List<SensitiveWordVO> voList = new ArrayList<>();
        if (sourceList != null && !sourceList.isEmpty()) {
            for (SensitiveWord sw : sourceList) {
                SensitiveWordVO vo = new SensitiveWordVO();
                BeanUtils.copyProperties(sw, vo);
                voList.add(vo);
            }
        }
        for (SensitiveWordVO word : voList) {
            if (word.getWord() != null) {
                String desensitizedWord = desensitize(word.getWord());
                word.setWord(desensitizedWord);
            }
        }
        PageResult<SensitiveWordVO> pageResult = new PageResult<>();
        pageResult.setTotal(sensitiveWordPage.getTotal());
        pageResult.setPageNum((int) sensitiveWordPage.getCurrent());
        pageResult.setPageSize((int) sensitiveWordPage.getSize());
        pageResult.setTotalPages((int) sensitiveWordPage.getPages());
        pageResult.setList(voList);
        return pageResult;
    }

    /**
     * 带条件分页查询
     */
    public PageResult<SensitiveWordVO> getSensitiveWordPageByOffer(Integer pageNum, Integer pageSize,String word) {
        pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Page<SensitiveWord> mpPage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        if (word != null && !word.isEmpty()) {
            wrapper.like(SensitiveWord::getWord, word); // 模糊查询 word 字段
        }
        Page<SensitiveWord> sensitiveWordPage = sensitiveWordMapper.selectPage(mpPage, wrapper);
        List<SensitiveWord> sourceList = sensitiveWordPage.getRecords();
        List<SensitiveWordVO> voList = new ArrayList<>();
        if (sourceList != null && !sourceList.isEmpty()) {
            for (SensitiveWord sw : sourceList) {
                SensitiveWordVO vo = new SensitiveWordVO();
                BeanUtils.copyProperties(sw, vo);
                voList.add(vo);
            }
        }
        for (SensitiveWordVO temp : voList) {
            if (temp.getWord() != null) {
                String desensitizedWord = desensitize(temp.getWord());
                temp.setWord(desensitizedWord);
            }
        }
        PageResult<SensitiveWordVO> pageResult = new PageResult<>();
        pageResult.setTotal(sensitiveWordPage.getTotal());
        pageResult.setPageNum((int) sensitiveWordPage.getCurrent());
        pageResult.setPageSize((int) sensitiveWordPage.getSize());
        pageResult.setTotalPages((int) sensitiveWordPage.getPages());
        pageResult.setList(voList);
        return pageResult;
    }

    // 获取所有敏感词
    public List<String> getAllValidWords(){
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SensitiveWord::getWord);
        List<Object> objList = sensitiveWordMapper.selectObjs(wrapper);

        if (CollectionUtils.isEmpty(objList)) {
            return Collections.emptyList();
        }
        return objList.stream()
                .map(obj -> (String) obj)
                .collect(Collectors.toList());
    }

    // 构建和保存到redis
    public void buildAndSaveToRedis(List<String> words){
        // 必须使用 Spring 单例，否则词树建在临时对象上，AiChatService 里的过滤器永远为空
        dfaFilter.buildDFA(words);
    }

    private void refreshDfa() {
        dfaFilter.buildDFA(getAllValidWords());
    }

    private String desensitize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        int length = word.length();
        if (length <= 2) {
            String s = word.charAt(0) + "*".repeat(Math.max(0, length - 1));
            StringBuilder sb = new StringBuilder(s);
            sb.append("***");
            return sb.toString();
        } else {
            String s =  word.charAt(0) + "*".repeat(length - 2) + word.charAt(length - 1);
            StringBuilder sb = new StringBuilder(s);
            sb.setCharAt(1,'*');
            sb.append("***");
            return sb.toString();
        }
    }
}
