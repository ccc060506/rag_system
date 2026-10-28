package com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.controller;

import com.ragsystem.common.annotation.RequireAdmin;
import com.ragsystem.common.result.PageResult;
import com.ragsystem.common.result.Result;
import com.ragsystem.common.util.RateLimit;
import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.domain.dto.SensitiveWordDTO;
import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.domain.vo.SensitiveWordVO;
import com.ragsystem.module.manager.sensitiveWords.sensitiveWordsManager.service.ISensitiveWordService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/sensitiveWord")
@RequireAdmin
public class SensitiveWordController {

    @Autowired
    private ISensitiveWordService iSensitiveWordService;

    // 新增敏感词
    @PostMapping("/add")
    @Operation(summary = "新增敏感词")
    @RateLimit(key = "insertLimit" , count = 10 , message = "新增敏感词服务繁忙，请稍后再次尝试")
    public Result<String> insert(@RequestBody List<SensitiveWordDTO> words){
        iSensitiveWordService.insert(words);
        return Result.success("添加成功");
    }

    // 修改敏感词
    @PutMapping("/update/{id}")
    @Operation(summary = "修改敏感词")
    @RateLimit(key = "updateLimit" , count = 10 , message = "修改敏感词服务繁忙，请稍后再次尝试")
    public Result<String> update(@PathVariable Long id,@RequestBody SensitiveWordDTO sensitiveWordDTO){
        iSensitiveWordService.updateWord(id,sensitiveWordDTO);
        return Result.success("修改成功");
    }

    // 根据id删除敏感词
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "根据id删除敏感词")
    @RateLimit(key = "deleteSensitiveWordByIdLimit" , count = 10 , message = "根据id删除敏感词服务繁忙，请稍后再次尝试")
    public Result<String> deleteSensitiveWordById(@PathVariable Long id){
        log.info("根据id删除敏感词");
        iSensitiveWordService.deleteSensitiveWordById(id);
        return Result.success("删除成功");
    }

    // 根据id查询敏感词
    @GetMapping("/select/{id}")
    @Operation(summary = "根据id查询敏感词")
    @RateLimit(key = "getSensitiveWordByIdLimit" , count = 10 , message = "根据id查询敏感词服务繁忙，请稍后再次尝试")
    public Result<SensitiveWordVO> getSensitiveWordById(@PathVariable Long id){
        log.info("根据id查询敏感词");
        SensitiveWordVO sensitiveWordById = iSensitiveWordService.getSensitiveWordById(id);
        if (sensitiveWordById == null) {
            return Result.error("敏感词不存在");
        }
        return Result.success(sensitiveWordById);
    }

    // 分页查询全部敏感词
    @GetMapping("/pageAll/{num}/{size}")
    @Operation(summary = "分页查询全部敏感词")
    @RateLimit(key = "getSensitiveWordPageLimit" , count = 10 , message = "分页查询全部敏感词服务繁忙，请稍后再次尝试")
    public Result<PageResult<SensitiveWordVO>> getSensitiveWordPage(@PathVariable Integer num,@PathVariable Integer size){
        log.info("分页查询全部敏感词");
        PageResult<SensitiveWordVO> pg = iSensitiveWordService.getSensitiveWordPage(num,size);
        return Result.success(pg);
    }

    // 分页查询指定敏感词
    @GetMapping("/pageAllByOffer/{num}/{size}")
    @Operation(summary = "分页查询指定敏感词")
    @RateLimit(key = "getSensitiveWordPageByOfferLimit" , count = 10 , message = "分页查询指定敏感词服务繁忙，请稍后再次尝试")
    public Result<PageResult<SensitiveWordVO>> getSensitiveWordPageByOffer(@PathVariable Integer num,@PathVariable Integer size,@RequestParam String word){
        log.info("分页查询指定敏感词");
        PageResult<SensitiveWordVO> pg = iSensitiveWordService.getSensitiveWordPageByOffer(num,size,word);
        return Result.success(pg);
    }

    // 构建和保存到redis
    @PostMapping("/refresh")
    @Operation(summary = "构建和保存到redis")
    @RateLimit(key = "refreshDfaLimit" , count = 10 , message = "构建和保存到redis服务繁忙，请稍后再次尝试")
    public Result<String> refreshDfa() {
        try {
            List<String> words = iSensitiveWordService.getAllValidWords();
            if (words.isEmpty()) {
                return Result.success("无有效敏感词，无需刷新");
            }
            iSensitiveWordService.buildAndSaveToRedis(words);
            return Result.success("敏感词 DFA 树已刷新，词數：" + words.size());
        } catch (Exception e) {
            log.error("刷新敏感词 DFA 失败", e);
            return Result.error("刷新失败：" + e.getMessage());
        }
    }
}
