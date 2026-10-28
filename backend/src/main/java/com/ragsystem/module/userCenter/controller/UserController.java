package com.ragsystem.module.userCenter.controller;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ragsystem.common.constant.RedisKeyConstants;
import com.ragsystem.common.result.Result;
import com.ragsystem.common.util.PasswordService;
import com.ragsystem.common.util.RateLimit;
import com.ragsystem.common.util.UserContext;
import com.ragsystem.common.util.JwtUtil;
import com.ragsystem.module.userCenter.domain.dto.LoginDTO;
import com.ragsystem.module.userCenter.domain.dto.UserDTO;
import com.ragsystem.module.userCenter.domain.dto.UserRegisterDTO;
import com.ragsystem.module.userCenter.domain.entity.User;
import com.ragsystem.module.userCenter.domain.vo.UserConfigVO;
import com.ragsystem.module.userCenter.domain.vo.UserVO;
import com.ragsystem.module.userCenter.mapper.UserMapper;
import com.ragsystem.module.userCenter.service.IUserConfigService;
import com.ragsystem.module.userCenter.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/user/center")
@Tag(name = "用户信息中心")
public class UserController {

    @Autowired
    private IUserService userService;
    @Autowired
    private IUserConfigService userConfigService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordService passwordService;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;
    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;
    @Value("${aliyun.sts.role-arn}")
    private String roleArn;

    // 登录接口
    @PostMapping("/login")
    @Operation(summary = "登录接口")
    @RateLimit(key = "loginLimit" , count = 10 , message = "登录服务繁忙，请稍后再次尝试")
    public Result<Map<String,String>> login(@RequestBody LoginDTO loginDTO){
        // 验证(查数据库)
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = userMapper.selectOne(queryWrapper);

        // 统一提示，避免用户名枚举
        if (user == null || !passwordService.matches(loginDTO.getPassword(), user.getPassword(), user.getSalt())) {
            return Result.error(401, "用户名或密码错误");
        }
        // 旧版 SHA-256 密码在登录成功后自动升级为 BCrypt
        if (passwordService.isLegacy(user.getPassword())) {
            user.setPassword(passwordService.encode(loginDTO.getPassword()));
            userMapper.updateById(user);
        }
        String role = user.getRole() == null ? UserContext.ROLE_USER : user.getRole();
        // 生成token
        String token = jwtUtil.createToken(user.getUserId(), user.getUsername(), role);
        // token 存入 redis，TTL 与 JWT 保持一致，便于登出/踢下线
        redisTemplate.opsForValue().set(RedisKeyConstants.USER_TOKEN_PREFIX + token,
                user.getUserId(), jwtUtil.getExpirationMillis(), TimeUnit.MILLISECONDS);
        // 返回前端
        return Result.success(Map.of("token", token, "username", user.getUsername()));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<String> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            redisTemplate.delete(RedisKeyConstants.USER_TOKEN_PREFIX + header.substring(7));
        }
        return Result.success("已退出登录");
    }

    @GetMapping("/oss/sts")
    @Operation(summary = "获取OSS临时授权")
    @RateLimit(key = "getOssStsLimit" , count = 10 , message = "获取OSS临时授权服务繁忙，请稍后再次尝试")
    public Result<Map<String, String>> getOssSts() {
        try {
            DefaultProfile profile = DefaultProfile.getProfile("cn-beijing", accessKeyId, accessKeySecret);
            DefaultAcsClient client = new DefaultAcsClient(profile);
            AssumeRoleRequest request = new AssumeRoleRequest();
            request.setSysMethod(MethodType.POST);
            request.setRoleArn(roleArn);
            request.setRoleSessionName("ExternalToken");
            request.setDurationSeconds(3600L); // 凭证有效时间：1小时
            AssumeRoleResponse response = client.getAcsResponse(request);
            AssumeRoleResponse.Credentials credentials = response.getCredentials();
            Map<String, String> res = new HashMap<>();
            res.put("accessKeyId", credentials.getAccessKeyId());
            res.put("accessKeySecret", credentials.getAccessKeySecret());
            res.put("securityToken", credentials.getSecurityToken());
            return Result.success(res);
        } catch (ClientException e) {
            log.error("STS获取失败详情: Code={}, Msg={}", e.getErrCode(), e.getErrMsg());
            return Result.error(500, "获取授权失败: " + e.getErrMsg());
        }
    }

    // 注册接口
    @RequestMapping("/register")
    @RateLimit(key = "registerLimit" , count = 10 , message = "注册接口用户服务繁忙，请稍后再次尝试")
    public Result<String> register(@RequestBody UserRegisterDTO dto) {
        Boolean res = userService.register(dto);
        if (res){
            return Result.success("注册成功");
        }
        else {
            return Result.success("注册失败，请重试");
        }
    }

    // 新增用户个人信息
    @PostMapping("/insert")
    @Operation(summary = "新增用户个人信息")
    @RateLimit(key = "insertLimit" , count = 10 , message = "新增用户个人信息服务繁忙，请稍后再次尝试")
    public Result<String> insert(@RequestBody UserDTO userDTO){
        userService.insert(userDTO);
        return Result.success();
    }

    // 修改用户个人信息
    @PutMapping("/update")
    @Operation(summary = "修改个人信息")
    @RateLimit(key = "updateUserLimit" , count = 10 , message = "修改个人信息信息服务繁忙，请稍后再次尝试")
    public Result<String> update(@RequestBody UserDTO userDTO){
        userService.update(userDTO);
        return Result.success("修改成功");
    }

    // 查询用户个人信息
    @GetMapping("/select")
    @Operation(summary = "查询个人信息")
    @RateLimit(key = "selectUserLimit" , count = 10 , message = "查询个人信息服务繁忙，请稍后再次尝试")
    public Result<UserVO> select(){
        Long id = UserContext.getUserId();
        UserVO userVO = userService.select(id);
        if (userVO == null) {
            return Result.error("用户不存在");
        }
        return Result.success(userVO);
    }

    // 查询用户配置信息
    @GetMapping("/select/config")
    @Operation(summary = "查询用户配置信息")
    @RateLimit(key = "selectConfigLimit" , count = 10 , message = "查询用户配置信息服务繁忙，请稍后再次尝试")
    public Result<UserConfigVO> selectConfig(){
        Long id = UserContext.getUserId();
        UserConfigVO userConfigVO = userConfigService.selectConfig(id);
        return Result.success(userConfigVO);
    }

}