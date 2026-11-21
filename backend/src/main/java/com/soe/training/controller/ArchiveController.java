package com.soe.training.controller;

import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.security.AuthContext;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 一人一档培训档案 + 学时达标预警
 */
@RestController
@RequestMapping("/biz/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    /** 我的档案 */
    @GetMapping("/my")
    public Result<Map<String, Object>> my(@RequestParam(required = false) Integer year) {
        return Result.ok(archiveService.archive(AuthContext.getUserId(), year));
    }

    /** 我的预警(工作台卡片) */
    @GetMapping("/my-warning")
    public Result<Map<String, Object>> myWarning(@RequestParam(required = false) Integer year) {
        return Result.ok(archiveService.myWarning(year));
    }

    /** 他人档案(培训管理员/超管) */
    @RequirePerm("biz:archive:other")
    @GetMapping("/user/{userId}")
    public Result<Map<String, Object>> of(@PathVariable Long userId,
                                          @RequestParam(required = false) Integer year) {
        return Result.ok(archiveService.archive(userId, year));
    }

    /** 达标预警名单(管理员全员, 员工本人) */
    @GetMapping("/warnings")
    public Result<List<Map<String, Object>>> warnings(@RequestParam(required = false) Integer year) {
        return Result.ok(archiveService.warnings(year));
    }

    /** 推送预警消息 */
    @OperLog(module = "学时预警", operation = "推送预警消息")
    @RequirePerm("biz:archive:other")
    @PostMapping("/warnings/push")
    public Result<Integer> pushWarnings(@RequestParam(required = false) Integer year) {
        return Result.ok(archiveService.pushWarnings(year));
    }
}
