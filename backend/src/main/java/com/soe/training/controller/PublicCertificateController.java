package com.soe.training.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soe.training.common.PublicRateLimiter;
import com.soe.training.common.Result;
import com.soe.training.domain.Certificate;
import com.soe.training.domain.SysUser;
import com.soe.training.domain.TrainingClass;
import com.soe.training.mapper.CertificateMapper;
import com.soe.training.mapper.ClassMapper;
import com.soe.training.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 证书防伪公开查验(无需登录; 路径 /public/** 已在拦截器放行)
 *
 * 安全设计(对标学信网式"证书编号 + 在线验证码"):
 *  - 双要素: 必须 证书编号 + 防伪码 同时匹配, 编号可被顺序枚举但防伪码不可猜;
 *  - 脱敏: 姓名仅保留首字, 工号仅保留后 4 位;
 *  - 限流: 每 IP 每分钟 10 次, 超限 429;
 *  - 不区分"编号不存在/防伪码错误/已作废", 一律返回 valid=false, 不给探测信号。
 */
@RestController
@RequestMapping("/public/certificates")
@RequiredArgsConstructor
public class PublicCertificateController {

    private final CertificateMapper certificateMapper;
    private final UserMapper userMapper;
    private final ClassMapper classMapper;
    private final PublicRateLimiter rateLimiter;

    @GetMapping("/verify")
    public Result<Map<String, Object>> verify(@RequestParam(required = false) String certNo,
                                              @RequestParam(required = false) String code,
                                              HttpServletRequest request, HttpServletResponse response) {
        String ip = clientIp(request);
        if (!rateLimiter.tryAcquire(ip)) {
            response.setStatus(429);
            return Result.fail(429, "查询过于频繁, 请稍后再试");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("valid", false);
        if (certNo == null || certNo.isBlank() || code == null || code.isBlank()) {
            return Result.ok(data);
        }
        Certificate c = certificateMapper.selectOne(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getCertNo, certNo.trim()));
        if (c == null || c.getVerifyCode() == null || !c.getVerifyCode().equals(code.trim())) {
            return Result.ok(data);
        }
        SysUser u = userMapper.selectById(c.getUserId());
        TrainingClass cls = classMapper.selectById(c.getClassId());
        data.put("valid", true);
        data.put("certNo", c.getCertNo());
        data.put("userName", maskName(u == null ? null : u.getNickname()));
        data.put("employeeNoTail", maskEmployeeNo(u == null ? null : u.getEmployeeNo()));
        data.put("className", cls == null ? null : cls.getClassName());
        data.put("hours", c.getHours());
        data.put("grade", c.getGrade());
        data.put("issueDate", c.getIssueDate());
        return Result.ok(data);
    }

    /** 取真实 IP: 反向代理场景取 X-Forwarded-For 首段, 否则取 remoteAddr */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String real = request.getHeader("X-Real-IP");
        return real != null && !real.isBlank() ? real.trim() : request.getRemoteAddr();
    }

    /** 姓名脱敏: 保留首字, 其余打码(单字姓名不打码) */
    static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        if (name.length() == 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /** 工号脱敏: 仅保留后 4 位 */
    static String maskEmployeeNo(String no) {
        if (no == null || no.isEmpty()) {
            return "";
        }
        return no.length() <= 4 ? no : "****" + no.substring(no.length() - 4);
    }
}
