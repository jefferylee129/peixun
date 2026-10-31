package com.soe.training.controller;

import com.soe.training.common.Result;
import com.soe.training.domain.SysDept;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 部门管理
 */
@RestController
@RequestMapping("/system/depts")
@RequiredArgsConstructor
public class SysDeptController {

    private final DeptService deptService;

    @RequirePerm("system:dept:list")
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        return Result.ok(deptService.tree());
    }

    @OperLog(module = "部门管理", operation = "保存部门")
    @RequirePerm("system:dept:add")
    @PostMapping
    public Result<Void> add(@RequestBody SysDept dept) {
        deptService.save(dept);
        return Result.ok();
    }

    @OperLog(module = "部门管理", operation = "修改部门")
    @RequirePerm("system:dept:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysDept dept) {
        dept.setId(id);
        deptService.save(dept);
        return Result.ok();
    }

    @OperLog(module = "部门管理", operation = "删除部门")
    @RequirePerm("system:dept:remove")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok();
    }
}
