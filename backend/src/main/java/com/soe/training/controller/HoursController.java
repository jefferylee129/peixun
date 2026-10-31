package com.soe.training.controller;

import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.HourRecord;
import com.soe.training.domain.vo.HourRecordVo;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.HoursService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 培训学时管理(台账/认定/达标统计)
 */
@RestController
@RequestMapping("/biz/hours")
@RequiredArgsConstructor
public class HoursController {

    private final HoursService hoursService;

    @RequirePerm("biz:hours:list")
    @GetMapping
    public Result<PageResult<HourRecordVo>> page(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) Integer year,
                                                 @RequestParam(required = false) Long userId,
                                                 @RequestParam(required = false) Long deptId,
                                                 @RequestParam(required = false) Integer source,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword) {
        return Result.ok(hoursService.page(page, size, year, userId, deptId, source, status, keyword));
    }

    /** 我的学时台账 */
    @GetMapping("/my")
    public Result<PageResult<HourRecordVo>> my(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) Integer year) {
        return Result.ok(hoursService.my(page, size, year));
    }

    /** 我的年度学时汇总 */
    @GetMapping("/my/summary")
    public Result<Map<String, Object>> mySummary(@RequestParam(required = false) Integer year) {
        return Result.ok(hoursService.mySummary(year));
    }

    @OperLog(module = "培训学时", operation = "手工补录学时")
    @RequirePerm("biz:hours:add")
    @PostMapping
    public Result<Void> add(@RequestBody HourRecord record) {
        hoursService.add(record);
        return Result.ok();
    }

    @OperLog(module = "培训学时", operation = "认定学时")
    @RequirePerm("biz:hours:audit")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = Boolean.TRUE.equals(body.get("pass"));
        String remark = body.get("remark") == null ? null : body.get("remark").toString();
        hoursService.audit(id, pass, remark);
        return Result.ok();
    }

    @OperLog(module = "培训学时", operation = "删除学时记录")
    @RequirePerm("biz:hours:remove")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        hoursService.delete(id);
        return Result.ok();
    }

    /** 员工年度学时达标统计 */
    @RequirePerm("biz:hours:list")
    @GetMapping("/stats/user")
    public Result<List<Map<String, Object>>> userStats(@RequestParam(required = false) Integer year,
                                                       @RequestParam(required = false) Long deptId,
                                                       @RequestParam(required = false) String keyword) {
        return Result.ok(hoursService.userStats(year, deptId, keyword));
    }

    /** 部门学时统计 */
    @RequirePerm("biz:hours:list")
    @GetMapping("/stats/dept")
    public Result<List<Map<String, Object>>> deptStats(@RequestParam(required = false) Integer year) {
        return Result.ok(hoursService.deptStats(year));
    }
}
