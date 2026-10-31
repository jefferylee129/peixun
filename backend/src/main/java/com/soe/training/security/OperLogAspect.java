package com.soe.training.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soe.training.domain.SysOperLog;
import com.soe.training.mapper.OperLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * 操作日志切面: 记录操作人、模块、参数、结果, 满足国企审计留痕要求
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperLogAspect {

    private final OperLogMapper operLogMapper;
    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = "@annotation(operLog)", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, OperLog operLog, Object result) {
        saveLog(joinPoint, operLog, null);
    }

    @AfterThrowing(pointcut = "@annotation(operLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, OperLog operLog, Exception e) {
        saveLog(joinPoint, operLog, e);
    }

    private void saveLog(JoinPoint joinPoint, OperLog operLog, Exception e) {
        try {
            SysOperLog logEntity = new SysOperLog();
            LoginUser user = AuthContext.get();
            if (user != null) {
                logEntity.setUserId(user.getUserId());
                logEntity.setUsername(user.getUsername());
            }
            logEntity.setModule(operLog.module());
            logEntity.setOperation(operLog.operation());
            logEntity.setMethod(joinPoint.getSignature().toShortString());
            logEntity.setParams(argsToJson(joinPoint.getArgs()));
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                logEntity.setIp(request.getRemoteAddr());
            }
            logEntity.setStatus(e == null ? "0" : "1");
            logEntity.setErrorMsg(e == null ? null : truncate(e.getMessage(), 500));
            operLogMapper.insert(logEntity);
        } catch (Exception ex) {
            log.warn("记录操作日志失败", ex);
        }
    }

    private String argsToJson(Object[] args) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Object arg : args) {
                if (arg == null || arg instanceof MultipartFile
                        || arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(objectMapper.writeValueAsString(arg));
            }
            // 审计日志脱敏: 不落任何明文口令
            return truncate(maskSensitive(sb.toString()), 2000);
        } catch (Exception e) {
            return null;
        }
    }

    /** 将 password/oldPassword/newPassword 字段值替换为 *** */
    private String maskSensitive(String json) {
        if (json == null) {
            return null;
        }
        return json.replaceAll(
                "(\"(?:password|oldPassword|newPassword|pwd)\"\\s*:\\s*\")([^\"]*)(\")",
                "$1***$3");
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
