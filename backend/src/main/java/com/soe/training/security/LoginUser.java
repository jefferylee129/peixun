package com.soe.training.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

/**
 * 当前登录用户(线程上下文)
 */
@Data
@AllArgsConstructor
public class LoginUser {

    private Long userId;
    private String username;
    private String nickname;
    /** 角色标识集合 */
    private Set<String> roles;
    /** 权限标识集合, 管理员为 ["*"] */
    private Set<String> perms;
    private boolean admin;

    public boolean hasPerm(String perm) {
        return admin || perms.contains(perm);
    }
}
