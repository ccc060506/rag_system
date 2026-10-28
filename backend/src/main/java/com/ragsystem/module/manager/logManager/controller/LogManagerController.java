package com.ragsystem.module.manager.logManager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ragsystem.common.result.PageResult;
import com.ragsystem.common.result.Result;
import com.ragsystem.common.util.RateLimit;
import com.ragsystem.common.util.UserContext;
import com.ragsystem.module.manager.logManager.domain.entity.AIChatLog;
import com.ragsystem.module.manager.logManager.domain.vo.AIChatLogVO;
import com.ragsystem.module.manager.logManager.mapper.AIChatLogMapper;
import com.ragsystem.module.manager.logManager.service.IAIChatLogManagerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/log")
public class LogManagerController {

    @Autowired
    private IAIChatLogManagerService AIChatLogManagerService;
    @Autowired
    private AIChatLogMapper aiChatLogMapper;

    // 分页查询用户 AI 对话日志
    @GetMapping("/page/{num}/{size}")
    @Operation(summary = "分页查询用户 AI 对话日志")
    @RateLimit(key = "getChatLogPageLimit" , count = 20 , message = "查询用户 AI 对话日志服务繁忙，请稍后再次尝试")
    public Result<PageResult<AIChatLogVO>> getChatLogPage(@PathVariable Integer num,@PathVariable Integer size){
        log.info("分页查询用户 AI 对话日志");
        Long id = UserContext.getUserId();
        Page<AIChatLogVO> pg = AIChatLogManagerService.getChatLogPage(num,size,id);
        return Result.success(PageResult.fromPageHelper(pg));
    }

    // 查询用户 AI 对话详情
    @GetMapping("/chat/detail/{id}")
    @Operation(summary = "获取对话详情")
    @RateLimit(key = "getDetailLimit" , count = 5 , message = "查询 AI 对话详情服务繁忙，请稍后再次尝试")
    public Result<AIChatLog> getDetail(@PathVariable Long id) {
        AIChatLog detail = aiChatLogMapper.selectById(id);
        if (detail == null) {
            return Result.error("日志不存在");
        }
        if (!UserContext.isAdmin() && !java.util.Objects.equals(detail.getUserId(), UserContext.getUserId())) {
            return Result.error(403, "无权查看该日志");
        }
        return Result.success(detail);
    }
}
