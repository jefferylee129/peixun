package com.soe.training.controller;

import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.SysConfig;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统参数管理
 */
@RestController
@RequestMapping("/system/configs")
@RequiredArgsConstructor
public class SysConfigController {

    private final ConfigService configService;

    @RequirePerm("system:config:list")
    @GetMapping
    public Result<PageResult<SysConfig>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword) {
        return Result.ok(configService.page(page, size, keyword));
    }

    @RequirePerm("system:config:list")
    @GetMapping("/key/{key}")
    public Result<String> getByKey(@PathVariable String key) {
        return Result.ok(configService.getValue(key, null));
    }

    @OperLog(module = "参数设置", operation = "保存参数")
    @RequirePerm("system:config:add")
    @PostMapping
    public Result<Void> add(@RequestBody SysConfig config) {
        configService.save(config);
        return Result.ok();
    }

    @OperLog(module = "参数设置", operation = "修改参数")
    @RequirePerm("system:config:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysConfig config) {
        config.setId(id);
        configService.save(config);
        return Result.ok();
    }

    @OperLog(module = "参数设置", operation = "删除参数")
    @RequirePerm("system:config:remove")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return Result.ok();
    }
}
