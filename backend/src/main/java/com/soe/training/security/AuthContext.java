package com.soe.training.security;

import com.soe.training.common.BusinessException;

/**
 * 登录用户上下文(ThreadLocal)
 */
public class AuthContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = get();
        if (user == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return user.getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
