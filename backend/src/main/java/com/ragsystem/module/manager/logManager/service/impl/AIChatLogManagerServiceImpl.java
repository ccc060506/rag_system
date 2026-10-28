package com.ragsystem.module.manager.logManager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ragsystem.module.manager.logManager.domain.entity.AIChatLog;
import com.ragsystem.module.manager.logManager.domain.vo.AIChatLogVO;
import com.ragsystem.module.manager.logManager.mapper.AIChatLogMapper;
import com.ragsystem.module.manager.logManager.service.IAIChatLogManagerService;
import com.ragsystem.module.userCenter.domain.vo.UserVO;
import com.ragsystem.module.userCenter.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AIChatLogManagerServiceImpl extends ServiceImpl<AIChatLogMapper, AIChatLog> implements IAIChatLogManagerService {

    @Autowired
    private AIChatLogMapper aiChatLogMapper;
    @Autowired
    private IUserService userService; // 用于获取用户名

    public Page<AIChatLogVO> getChatLogPage(Integer pageNum, Integer pageSize, Long userId) {
        if (userId == null) {
            throw new SecurityException("未登录，无法查询对话日志");
        }
        // 1. 构建分页对象和查询条件
        Page<AIChatLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AIChatLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIChatLog::getUserId, userId).orderByDesc(AIChatLog::getCreateTime);
        log.info("当前用户id："+userId);
        // 2. 执行数据库查询
        Page<AIChatLog> resultPage = aiChatLogMapper.selectPage(page, wrapper);
        // 3. 将 Entity 转换为 VO
        List<AIChatLogVO> voList = resultPage.getRecords().stream().map(entity -> {
            AIChatLogVO vo = new AIChatLogVO();
            // 使用 BeanUtils 快速拷贝相同字段 (id, question, answer, modelName)
            BeanUtils.copyProperties(entity, vo);
            // 手动处理特殊字段
            vo.setCreateTimeStr(entity.getCreateTime());
            if (entity.getSourceContext() != null && !entity.getSourceContext().isBlank()) {
                vo.setSourceContext(Arrays.stream(entity.getSourceContext().split(","))
                        .map(String::trim)
                        .filter(item -> !item.isEmpty())
                        .toList());
            }
            vo.setTokensUsed("消耗" + entity.getTokensUsed() + " Tokens");
            // 获取用户名
            UserVO temp = userService.select(entity.getUserId());
            if (temp != null) {
                vo.setUsername(temp.getUsername());
            }
            return vo;
        }).collect(Collectors.toList());

        // 4. 返回封装后的分页结果
        Page<AIChatLogVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setRecords(voList);
        voPage.setTotal(resultPage.getTotal());
        return voPage;
    }


}
