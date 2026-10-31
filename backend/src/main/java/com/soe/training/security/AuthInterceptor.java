package com.soe.training.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soe.training.common.Result;
import com.soe.training.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 认证 + 权限拦截器
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Claims claims = token == null ? null : jwtUtil.parse(token);
        if (claims == null) {
            write(response, 401, 401, "未登录或登录已过期");
            return false;
        }
        Long userId = claims.get("uid", Long.class);

        List<Map<String, Object>> roleRows = userMapper.selectRoles(userId);
        Set<String> roles = new HashSet<>();
        boolean admin = false;
        for (Map<String, Object> row : roleRows) {
            String roleKey = String.valueOf(row.get("roleKey"));
            roles.add(roleKey);
            if ("admin".equals(roleKey)) {
                admin = true;
            }
        }
        Set<String> perms = new HashSet<>(userMapper.selectPerms(userId));
        LoginUser loginUser = new LoginUser(userId, claims.getSubject(),
                claims.get("nick", String.class), roles, perms, admin);
        AuthContext.set(loginUser);

        if (handler instanceof HandlerMethod hm) {
            RequirePerm rp = hm.getMethodAnnotation(RequirePerm.class);
            if (rp == null) {
                rp = hm.getBeanType().getAnnotation(RequirePerm.class);
            }
            if (rp != null && !loginUser.hasPerm(rp.value())) {
                write(response, 200, 403, "无操作权限:" + rp.value());
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }

    private void write(HttpServletResponse response, int httpStatus, int code, String msg) throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(objectMapper.writeValueAsBytes(Result.fail(code, msg)));
    }
}
