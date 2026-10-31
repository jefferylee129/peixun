package com.soe.training.controller;

import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.Trainer;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.TrainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 培训师资管理
 */
@RestController
@RequestMapping("/biz/trainers")
@RequiredArgsConstructor
public class TrainerController {

    private final TrainerService trainerService;

    @RequirePerm("biz:trainer:list")
    @GetMapping
    public Result<PageResult<Trainer>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String trainerType,
                                            @RequestParam(required = false) String status) {
        return Result.ok(trainerService.page(page, size, keyword, trainerType, status));
    }

    /** 启用师资下拉选项 */
    @RequirePerm("biz:class:list")
    @GetMapping("/all")
    public Result<List<Trainer>> all() {
        return Result.ok(trainerService.allActive());
    }

    @RequirePerm("biz:trainer:list")
    @GetMapping("/{id}/teaching")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(trainerService.detail(id));
    }

    @OperLog(module = "培训师资", operation = "新增师资")
    @RequirePerm("biz:trainer:add")
    @PostMapping
    public Result<Void> add(@RequestBody Trainer trainer) {
        trainerService.save(trainer);
        return Result.ok();
    }

    @OperLog(module = "培训师资", operation = "修改师资")
    @RequirePerm("biz:trainer:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Trainer trainer) {
        trainer.setId(id);
        trainerService.save(trainer);
        return Result.ok();
    }

    @OperLog(module = "培训师资", operation = "删除师资")
    @RequirePerm("biz:trainer:remove")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        trainerService.delete(id);
        return Result.ok();
    }
}
