package com.ragsystem.module.userCenter.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserConfigVO {

    // --- 额度与统计 (加工后的数据) ---
    private String apiKey;           // tk-xxx-xxx
    private String preferredModel;

    private Long totalTokenQuota;   // 总额度
    private Long usedTokenQuota;    // 已消耗额度
    private Double tokenUsageRate;  // 进度条百分比：(used/total)*100

    // 知识库限制
    private Integer maxKnowledgeBases; // 最大允许创建的知识库数量
    private Long maxStorageSize;       // 最大允许上传的文件总量 (MB)
}