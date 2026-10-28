package com.ragsystem.interceptor;

import com.ragsystem.common.annotation.RequireAdmin;
import com.ragsystem.common.constant.RedisKeyConstants;
import com.ragsystem.common.util.JwtUtil;
import com.ragsystem.common.util.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import java.io.IOException;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // CORS 预检请求不带 Token，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 不是方法直接放行（静态资源）
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String token = extractToken(request);
        if (token == null) {
            return writeJson(response, 401, "未登录，缺少Token");
        }

        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (Exception e) {
            return writeJson(response, 401, "登录已过期或Token无效");
        }

        // 校验 Redis 会话：登出或管理员踢下线后，未过期的 JWT 也会立即失效
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeyConstants.USER_TOKEN_PREFIX + token))) {
            return writeJson(response, 401, "登录已失效，请重新登录");
        }

        Long userId = claims.get("userId", Long.class);
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);
        UserContext.setCurrentUser(userId, username, role);

        RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);
        if (requireAdmin == null) {
            requireAdmin = handlerMethod.getBeanType().getAnnotation(RequireAdmin.class);
        }
        if (requireAdmin != null && !UserContext.isAdmin()) {
            return writeJson(response, 403, "需要管理员权限");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清理线程变量
        UserContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }

    private boolean writeJson(HttpServletResponse response, int status, String msg)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                String.format("{\"code\":%d,\"msg\":\"%s\"}", status, msg)
        );
        return false;
    }
}
