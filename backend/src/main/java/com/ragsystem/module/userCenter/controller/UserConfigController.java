package com.ragsystem.module.userCenter.controller;

import com.ragsystem.common.result.Result;
import com.ragsystem.common.util.RateLimit;
import com.ragsystem.common.util.UserContext;
import com.ragsystem.module.userCenter.domain.vo.UserConfigVO;
import com.ragsystem.module.userCenter.domain.vo.UserVO;
import com.ragsystem.module.userCenter.service.IUserConfigService;
import com.ragsystem.module.userCenter.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/config")
@Tag(name = "用户配置接口")
public class UserConfigController {

    @Autowired
    private IUserConfigService userConfigService;
    @Autowired
    private IUserService userService;

    /**
     * 验证并保存/更新 DeepSeek API Key
     * @param apiKey 前端传入的 DeepSeek API Key
     * @return 验证结果
     */
    @PostMapping("/verify")
    @Operation(summary = "验证并保存DeepSeek API Key")
    @RateLimit(key = "verifyAndSaveApiKeyLimit" , count = 10 , message = "验证并保存DeepSeek API Key服务繁忙，请稍后再次尝试")
    public Result<String> verifyAndSaveApiKey(@RequestParam String apiKey) {
        // 调用 Service 层验证并保存/更新 Key
        boolean isValid = userConfigService.verifyAndSaveDeepSeekApiKey(apiKey);
        if (isValid) {
            return Result.success("API Key 验证通过，已保存/更新");
        } else {
            return Result.error("API Key 无效或余额不足");
        }
    }

    // 查询当前 Api 配置信息
    @GetMapping("/select")
    @Operation(summary = "查询配置信息")
    @RateLimit(key = "selectApiConfigLimit" , count = 10 , message = "查询用户Api配置信息服务繁忙，请稍后再次尝试")
    public Result<UserConfigVO> selectConfig(){
        Long id = UserContext.getUserId();
        UserVO userVO = userService.select(id);
        if(userVO==null){
            return Result.error("当前用户不存在，请注册");
        }
        UserConfigVO userConfigVO = userConfigService.selectConfig(id);
        return Result.success(userConfigVO);
    }
}