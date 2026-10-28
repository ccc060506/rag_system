package com.ragsystem.module.userCenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ragsystem.common.util.ApiKeyCipher;
import com.ragsystem.common.util.UserContext;
import com.ragsystem.module.userCenter.domain.dto.DeepSeekPingRequestDTO;
import com.ragsystem.module.userCenter.domain.entity.Balance;
import com.ragsystem.module.userCenter.domain.entity.UserConfig;
import com.ragsystem.module.userCenter.domain.vo.UserConfigVO;
import com.ragsystem.module.userCenter.mapper.UserConfigMapper;
import com.ragsystem.module.userCenter.service.IUserConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UserConfigServiceImpl extends ServiceImpl<UserConfigMapper, UserConfig> implements IUserConfigService {

    @Autowired
    private UserConfigMapper userConfigMapper;
    @Autowired
    private ApiKeyCipher apiKeyCipher;

    // DeepSeek API 地址
    private final String DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions";
    private RestTemplate restTemplate;

    // 构造器注入 RestTemplate
    public UserConfigServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 验证并保存/更新 API Key
     */
    @Override
    public boolean verifyAndSaveDeepSeekApiKey(String apiKey) {
        // 1. 先拨测验证 Key 有效性
        boolean isValid = doDeepSeekPing(apiKey);
        if (!isValid) {
            return false;
        }
        // 总共的 token
        Long realTokenQuota = getRealTokenQuota(apiKey);
        Long userId = UserContext.getUserId();
        if (userId == null) {
            log.error("验证 API Key 失败：未获取到当前用户ID");
            return false;
        }
        // 3. 查询用户是否存在（老用户更新，新用户新增）
        UserConfig user = this.getById(userId);
        if (user == null) {
            UserConfig newUser = new UserConfig();
            newUser.setUserId(userId);
            newUser.setApiKey(apiKeyCipher.encrypt(apiKey));
            // 修改这里：初始化为 0，而不是死数据
            newUser.setUsedTokenQuota(0L);
            newUser.setTotalTokenQuota(realTokenQuota);
            newUser.setPreferredModel("deepseek-v3");
            this.save(newUser);
        } else {
            user.setApiKey(apiKeyCipher.encrypt(apiKey));
            userConfigMapper.updateById(user);
        }
        return true;
    }

    /**
     * 核心拨测方法：发送极小请求验证 Key
     */
    @Override
    public boolean doDeepSeekPing(String apiKey) {
        try {
            // 构建请求头
            DeepSeekPingRequestDTO deepSeekPingRequestDTO = new DeepSeekPingRequestDTO();
            DeepSeekPingRequestDTO.Message testMessage = new DeepSeekPingRequestDTO.Message();
            testMessage.setRole("user");      // 必须指定角色
            testMessage.setContent("Hi");     // 必须指定内容
            // 将消息放入列表
            deepSeekPingRequestDTO.setMessages(Collections.singletonList(testMessage));
            // 别忘了设置模型名称（DeepSeek 强制要求 model 字段）
            deepSeekPingRequestDTO.setModel("deepseek-chat");
            // 封装请求
            HttpEntity<DeepSeekPingRequestDTO> requestEntity = new HttpEntity<>(deepSeekPingRequestDTO, getHeaders(apiKey));

            // 发送请求
            ResponseEntity<Map> response = restTemplate.exchange(DEEPSEEK_API_URL, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> usage = (Map<String, Object>) response.getBody().get("usage");
                if (usage != null) {
                    Integer totalTokens = (Integer) usage.get("total_tokens");
                    log.info("用户 {} 拨测成功，消耗 Token: {}", UserContext.getUserId(), totalTokens);
                    this.updateUsage(UserContext.getUserId(), totalTokens);
                }
                return true;
            }
        } catch (Exception e) {
            log.error("DeepSeek 拨测异常", e);
        }
        return false;
    }

    // 查询用户配置信息
    public UserConfigVO selectConfig(Long id){
        UserConfig userConfig = this.getById(id);
        UserConfigVO userConfigVO = new UserConfigVO();
        if(userConfig!=null){
            BeanUtils.copyProperties(userConfig,userConfigVO);
            // 永远不返回明文 Key
            userConfigVO.setApiKey(apiKeyCipher.mask(userConfig.getApiKey()));
            if (userConfigVO.getTotalTokenQuota() != null && userConfigVO.getTotalTokenQuota() > 0) {
                double rate = (double) userConfigVO.getUsedTokenQuota() / userConfigVO.getTotalTokenQuota() * 100;
                userConfigVO.setTokenUsageRate(Math.round(rate * 100.0) / 100.0); // 保留两位小数
            } else {
                userConfigVO.setTokenUsageRate(0.0);
            }
        }
        return userConfigVO;
    }

    public void updateUsage(Long userId, Integer currentTotalTokens) {
        this.update()
                .eq("user_id", userId)
                .setSql("used_token_quota = used_token_quota + " + currentTotalTokens)
                .update();
    }

    private HttpHeaders getHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        return headers;
    }

    /**
     * 获取 DeepSeek 账户真实余额并换算为 Token 额度
     */
    private Long getRealTokenQuota(String apiKey) {
        try {
            String BALANCE_URL = "https://api.deepseek.com/user/balance";
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders(apiKey));

            ResponseEntity<Map> response = restTemplate.exchange(BALANCE_URL, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> balanceInfos = (List<Map<String, Object>>) body.get("balance_infos");

                if (balanceInfos != null && !balanceInfos.isEmpty()) {
                    // 拿到总余额（人民币）
                    String totalBalanceStr = balanceInfos.get(0).get("total_balance").toString();
                    double balance = Double.parseDouble(totalBalanceStr);

                    // 策略：将金额换算成 Token 额度
                    // 假设 DeepSeek V3 价格约为 2元/100万Token (具体按官网为准)
                    // 这里我们简单起见：1元 = 50万 Token
                    return (long) (balance * 500000);
                }
            }
        } catch (Exception e) {
            log.error("查询 DeepSeek 余额失败", e);
        }
        return 100000L; // 失败时给个保底值
    }
}