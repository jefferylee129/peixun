package com.soe.training.controller;

import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.SysOperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志
 */
@RestController
@RequestMapping("/system/logs")
@RequiredArgsConstructor
public class SysLogController {

    private final LogService logService;

    @RequirePerm("system:log:list")
    @GetMapping
    public Result<PageResult<SysOperLog>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String status) {
        return Result.ok(logService.page(page, size, keyword, status));
    }
}
