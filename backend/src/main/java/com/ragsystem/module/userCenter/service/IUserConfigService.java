package com.ragsystem.module.userCenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ragsystem.module.userCenter.domain.entity.UserConfig;
import com.ragsystem.module.userCenter.domain.vo.UserConfigVO;

public interface IUserConfigService extends IService<UserConfig> {

    /**
     * 验证 DeepSeek API Key 有效性，并保存/更新到数据库
     * @param apiKey 待验证的 API Key
     * @return true-有效，false-无效
     */
    boolean verifyAndSaveDeepSeekApiKey(String apiKey);

    /**
     * 核心拨测方法：验证 API Key 是否有效
     * @param apiKey 待验证的 API Key
     * @return true-有效，false-无效
     */
    boolean doDeepSeekPing(String apiKey);

    // 查询用户配置信息
    UserConfigVO selectConfig(Long id);
}