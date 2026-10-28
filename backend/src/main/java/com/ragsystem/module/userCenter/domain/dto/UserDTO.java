package com.ragsystem.module.userCenter.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonProperty("nickname")
    private String username;
    private String password;
    private String email;
    private String avatar; // 头像地址
    private String apiKey; // 用户输入的明文 Key
    private Integer age;
}