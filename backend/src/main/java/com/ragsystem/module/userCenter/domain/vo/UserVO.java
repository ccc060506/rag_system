package com.ragsystem.module.userCenter.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVO {

    // --- 基础信息 ---
    private String username;
    private String email;
    private String avatar; // 头像地址
    private Integer age;

}
