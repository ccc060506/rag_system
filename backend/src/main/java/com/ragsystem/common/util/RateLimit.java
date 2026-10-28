package com.ragsystem.common.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 限流的 Key，唯一标识
     */
    String key() default "default_limit";

    /**
     * 限流阈值
     */
    double count() default 10.0;
    String message() default "请求太频繁，请稍后再试";
}
