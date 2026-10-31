package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soe.training.common.BusinessException;
import com.soe.training.domain.SysUser;
import com.soe.training.mapper.UserMapper;
import com.soe.training.security.AuthContext;
import com.soe.training.security.JwtUtil;
import com.soe.training.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final MenuService menuService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    /** 登录失败锁定: 同一账叿5次失败锁定10分钟(内存实现, 集群部署可接 Redis) */
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MILLIS = 10 * 60 * 1000L;
    private static final Map<String, long[]> LOGIN_FAILURES = new ConcurrentHashMap<>();

    public Map<String, Object> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("用户名和密码不能为空");
        }
        checkLocked(username);
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            recordFailure(username);
            throw new BusinessException("用户名或密码错误");
        }
        if (!"0".equals(user.getStatus())) {
            throw new BusinessException("账号已停用, 请联系管理员");
        }
        LOGIN_FAILURES.remove(username);
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getNickname());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", basicInfo(user));
        return result;
    }

    public Map<String, Object> me() {
        Long userId = AuthContext.getUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在或已删除");
        }
        LoginUser ctx = AuthContext.get();
        Map<String, Object> result = new HashMap<>();
        result.put("user", basicInfo(user));
        result.put("roles", ctx.getRoles());
        // 前端以 "*" 表示全部权限; 管理员下发 "*" 以便按钮级权限判断
        result.put("perms", ctx.isAdmin() ? java.util.Set.of("*") : ctx.getPerms());
        result.put("menus", menuService.buildUserMenus(userId));
        return result;
    }

    public void changePassword(String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }
        Long userId = AuthContext.getUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);
    }

    private Map<String, Object> basicInfo(SysUser user) {
        Map<String, Object> u = new HashMap<>();
        u.put("id", user.getId());
        u.put("username", user.getUsername());
        u.put("nickname", user.getNickname());
        u.put("deptId", user.getDeptId());
        u.put("userType", user.getUserType());
        u.put("employeeNo", user.getEmployeeNo());
        return u;
    }

    private void checkLocked(String username) {
        long[] fail = LOGIN_FAILURES.get(username);
        if (fail != null && fail[0] >= MAX_ATTEMPTS
                && System.currentTimeMillis() - fail[1] < LOCK_MILLIS) {
            throw new BusinessException(429, "登录失败次数过多, 账号已锁定10分钟");
        }
    }

    private void recordFailure(String username) {
        LOGIN_FAILURES.compute(username, (k, v) -> {
            long now = System.currentTimeMillis();
            if (v == null || now - v[1] >= LOCK_MILLIS) {
                return new long[]{1, now};
            }
            return new long[]{v[0] + 1, v[1]};
        });
    }
}
