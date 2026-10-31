package com.soe.training.controller;

import com.soe.training.common.Result;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统计分析
 */
@RestController
@RequestMapping("/biz/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @RequirePerm("biz:stats:view")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(required = false) Integer year) {
        return Result.ok(statsService.overview(year));
    }

    @RequirePerm("biz:stats:view")
    @GetMapping("/month-hours")
    public Result<List<Map<String, Object>>> monthHours(@RequestParam(required = false) Integer year) {
        return Result.ok(statsService.monthStats(year));
    }

    @RequirePerm("biz:stats:view")
    @GetMapping("/dept-hours")
    public Result<List<Map<String, Object>>> deptHours(@RequestParam(required = false) Integer year) {
        return Result.ok(statsService.deptStats(year));
    }

    @RequirePerm("biz:stats:view")
    @GetMapping("/course-category")
    public Result<List<Map<String, Object>>> courseCategory() {
        return Result.ok(statsService.courseCategory());
    }

    @RequirePerm("biz:stats:view")
    @GetMapping("/class-status")
    public Result<List<Map<String, Object>>> classStatus() {
        return Result.ok(statsService.classStatus());
    }

    @RequirePerm("biz:stats:view")
    @GetMapping("/plan-status")
    public Result<List<Map<String, Object>>> planStatus(@RequestParam(required = false) Integer year) {
        return Result.ok(statsService.planStatus(year));
    }

    @RequirePerm("biz:stats:view")
    @GetMapping("/need-type")
    public Result<List<Map<String, Object>>> needType(@RequestParam(required = false) Integer year) {
        return Result.ok(statsService.needTypeStats(year));
    }

    @RequirePerm("biz:stats:view")
    @GetMapping("/user-hours")
    public Result<Map<String, Object>> userHours(@RequestParam(required = false) Integer year,
                                                 @RequestParam(required = false) Long deptId,
                                                 @RequestParam(required = false) String keyword) {
        return Result.ok(statsService.userStatsSafe(year, deptId, keyword));
    }
}
