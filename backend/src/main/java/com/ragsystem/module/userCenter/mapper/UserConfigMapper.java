package com.ragsystem.module.userCenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ragsystem.module.userCenter.domain.entity.UserConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserConfigMapper extends BaseMapper<UserConfig> {
    void updateUsage(Long userId,Long totalTokens);
}