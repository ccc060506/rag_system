package com.ragsystem.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 用户 API Key 的对称加密工具（AES-256-GCM）。
 * 加密密钥来自环境变量 DATA_ENCRYPTION_KEY，未配置时禁止写入新 Key。
 */
@Slf4j
@Component
public class ApiKeyCipher {

    private static final String PREFIX = "enc:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;

    public ApiKeyCipher(@Value("${rag.security.data-key:}") String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) {
            this.key = null;
            log.warn("未配置 DATA_ENCRYPTION_KEY，用户 API Key 的保存功能将不可用");
        } else {
            this.key = new SecretKeySpec(sha256(configuredKey), "AES");
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank() || plainText.startsWith(PREFIX)) {
            return plainText;
        }
        if (key == null) {
            throw new IllegalStateException("未配置 DATA_ENCRYPTION_KEY，无法加密用户 API Key");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("API Key 加密失败", e);
        }
    }

    public String decrypt(String storedValue) {
        if (storedValue == null || !storedValue.startsWith(PREFIX)) {
            // 兼容历史明文数据
            return storedValue;
        }
        if (key == null) {
            throw new IllegalStateException("未配置 DATA_ENCRYPTION_KEY，无法解密用户 API Key");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(storedValue.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("API Key 解密失败", e);
        }
    }

    /**
     * 返回脱敏后的 Key，用于接口展示。
     */
    public String mask(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return storedValue;
        }
        String plain = decrypt(storedValue);
        if (plain.length() <= 8) {
            return "****";
        }
        return plain.substring(0, 3) + "****" + plain.substring(plain.length() - 4);
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
