package com.ragsystem.common.util;

import com.google.common.util.concurrent.RateLimiter;
import com.ragsystem.common.exception.RateLimitException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class RateLimitAspect {

    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = rateLimit.key();
        double count = rateLimit.count();

        // 如果没有这个 key 的限流器，就创建一个
        RateLimiter limiter = limiters.computeIfAbsent(key, k -> RateLimiter.create(count));

        // 尝试获取令牌
        if (limiter.tryAcquire()) {
            // 拿到令牌，执行原方法
            return point.proceed();
        } else {
            // 没拿到令牌，交给全局异常处理器转换为 429
            throw new RateLimitException(rateLimit.message());
        }
    }
}
