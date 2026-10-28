package com.ragsystem.common.exception;

/**
 * 触发限流时抛出，统一由全局异常处理器转换为 429。
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
