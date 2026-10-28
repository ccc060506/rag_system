package com.ragsystem.common.constant;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyConstants {
    public static final String DFA_TREE = "sensitive:dfa:tree";
    public static final String USER_TOKEN_PREFIX = "user:token:";
    public static final int TOKEN_EXPIRE_DAYS = 70;
}
