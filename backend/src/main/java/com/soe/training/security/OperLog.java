package com.soe.training.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解, 标注在需要审计的接口方法上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /** 业务模块, 如: 用户管理 */
    String module();

    /** 操作内容, 如: 新增用户 */
    String operation();
}
