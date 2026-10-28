package com.ragsystem.module.manager.logManager.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ragsystem.module.manager.logManager.domain.entity.AIChatLog;
import com.ragsystem.module.manager.logManager.domain.vo.AIChatLogVO;

public interface IAIChatLogManagerService extends IService<AIChatLog> {

    Page<AIChatLogVO> getChatLogPage(Integer pageNum, Integer pageSize, Long userId);

}
