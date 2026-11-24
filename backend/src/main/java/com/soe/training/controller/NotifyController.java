package com.soe.training.controller;

import com.soe.training.common.Result;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.NotifyJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 自动提醒任务(定时执行; 此处提供管理员手动触发入口, 不受开关限制)
 */
@RestController
@RequestMapping("/biz/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyJobService notifyJobService;

    @OperLog(module = "自动提醒", operation = "手动执行提醒扫描")
    @RequirePerm("biz:archive:other")
    @PostMapping("/run")
    public Result<Map<String, Object>> run() {
        return Result.ok(notifyJobService.runDaily());
    }
}
