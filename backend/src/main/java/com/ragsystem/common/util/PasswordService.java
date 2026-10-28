package com.ragsystem.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码哈希服务。新密码统一使用 BCrypt，同时兼容旧的加盐 SHA-256 历史数据。
 */
@Component
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedHash, String salt) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        if (isBcrypt(storedHash)) {
            return encoder.matches(rawPassword, storedHash);
        }
        // 兼容旧版 SHA-256(salt + password)
        return storedHash.equals(SHA256.encode(rawPassword, salt == null ? "" : salt));
    }

    public boolean isLegacy(String storedHash) {
        return storedHash != null && !isBcrypt(storedHash);
    }

    private boolean isBcrypt(String hash) {
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }
}
