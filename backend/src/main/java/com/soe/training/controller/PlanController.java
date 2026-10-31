package com.soe.training.controller;

import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.TrainingPlan;
import com.soe.training.domain.vo.PlanVo;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 培训计划管理(编制 → 审批下达)
 */
@RestController
@RequestMapping("/biz/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @RequirePerm("biz:plan:list")
    @GetMapping
    public Result<PageResult<PlanVo>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) Integer year,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String planType,
                                           @RequestParam(required = false) Long deptId,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(planService.page(page, size, year, status, planType, deptId, keyword));
    }

    /** 已批准计划下拉选项 */
    @RequirePerm("biz:class:list")
    @GetMapping("/options")
    public Result<List<TrainingPlan>> options(@RequestParam(required = false) Integer year) {
        return Result.ok(planService.options(year));
    }

    @OperLog(module = "培训计划", operation = "新增计划")
    @RequirePerm("biz:plan:add")
    @PostMapping
    public Result<Void> add(@RequestBody TrainingPlan plan) {
        planService.save(plan);
        return Result.ok();
    }

    @OperLog(module = "培训计划", operation = "修改计划")
    @RequirePerm("biz:plan:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody TrainingPlan plan) {
        plan.setId(id);
        planService.save(plan);
        return Result.ok();
    }

    @OperLog(module = "培训计划", operation = "删除计划")
    @RequirePerm("biz:plan:remove")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return Result.ok();
    }

    @OperLog(module = "培训计划", operation = "提交计划审批")
    @RequirePerm("biz:plan:edit")
    @PutMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        planService.submit(id);
        return Result.ok();
    }

    @OperLog(module = "培训计划", operation = "审批计划")
    @RequirePerm("biz:plan:approve")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = Boolean.TRUE.equals(body.get("pass"));
        String remark = body.get("remark") == null ? null : body.get("remark").toString();
        planService.approve(id, pass, remark);
        return Result.ok();
    }
}
