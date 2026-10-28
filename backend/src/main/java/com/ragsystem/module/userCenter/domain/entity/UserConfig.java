package com.ragsystem.module.userCenter.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户配额与模型配置
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_config")
public class UserConfig {

    @TableId(value = "user_id", type = IdType.INPUT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    // API 配置
    private String apiKey;
    private String preferredModel; // 默认使用的模型，如 "deepseek-v3"

    // 额度管理
    private Long totalTokenQuota;   // 总额度
    private Long usedTokenQuota;    // 已消耗额度

    // 知识库限制
    private Integer maxKnowledgeBases; // 最大允许创建的知识库数量
    private Long maxStorageSize;       // 最大允许上传的文件总量 (MB)
}