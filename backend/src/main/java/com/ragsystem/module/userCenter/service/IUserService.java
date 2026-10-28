package com.ragsystem.module.userCenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ragsystem.module.userCenter.domain.dto.UserDTO;
import com.ragsystem.module.userCenter.domain.dto.UserRegisterDTO;
import com.ragsystem.module.userCenter.domain.entity.User;
import com.ragsystem.module.userCenter.domain.vo.UserVO;

public interface IUserService extends IService<User> {

    // 修改个人信息
    void update(UserDTO userDTO);

    // 新增个人信息
    void insert(UserDTO userDTO);

    // 查询个人信息
    UserVO select(Long id);

    Boolean register(UserRegisterDTO dto);
}
