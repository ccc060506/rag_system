package com.ragsystem.module.userCenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ragsystem.common.util.PasswordService;
import com.ragsystem.common.util.UserContext;
import com.ragsystem.module.userCenter.domain.dto.UserDTO;
import com.ragsystem.module.userCenter.domain.dto.UserRegisterDTO;
import com.ragsystem.module.userCenter.domain.entity.User;
import com.ragsystem.module.userCenter.domain.vo.UserVO;
import com.ragsystem.module.userCenter.mapper.UserMapper;
import com.ragsystem.module.userCenter.service.IUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordService passwordService;

    @Override
    public Boolean register(UserRegisterDTO dto) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, dto.getUsername());
        if (baseMapper.selectCount(query) > 0) {
            throw new IllegalArgumentException("该用户名已被注册");
        }
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(passwordService.encode(dto.getPassword()));
        user.setSalt(null);
        user.setUserId(IdWorker.getId());
        user.setId(user.getUserId());
        user.setRole(UserContext.ROLE_ADMIN);
        user.setCreateTime(LocalDateTime.now());
        try {
            return this.save(user);
        } catch (DuplicateKeyException e) {
            // 并发注册时由数据库唯一索引兜底
            throw new IllegalArgumentException("该用户名已被注册");
        }
    }

    public void insert(UserDTO userDTO) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, userDTO.getUsername());
        if (baseMapper.selectCount(query) > 0) {
            throw new IllegalArgumentException("该用户名已被注册");
        }
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(passwordService.encode(userDTO.getPassword()));
        user.setSalt(null);
        user.setUserId(IdWorker.getId());
        user.setId(user.getUserId());
        user.setRole(UserContext.ROLE_USER);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
    }

    public void update(UserDTO userDTO) {
        Long currentUserId = UserContext.getUserId();
        User existing = findByUserId(currentUserId);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setId(existing.getId());
        user.setUserId(currentUserId);
        // 个人资料接口不允许修改密码和角色，避免越权
        user.setPassword(null);
        user.setSalt(null);
        user.setRole(null);
        userMapper.updateById(user);
    }

    public UserVO select(Long userId) {
        User user = findByUserId(userId);
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    private User findByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUserId, userId);
        return userMapper.selectOne(query);
    }
}
