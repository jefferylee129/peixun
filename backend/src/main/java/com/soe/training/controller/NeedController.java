package com.soe.training.controller;

import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.NeedCollection;
import com.soe.training.domain.TrainingNeed;
import com.soe.training.domain.vo.NeedVo;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.NeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 培训需求管理(征集 + 申报 + 审核)
 */
@RestController
@RequestMapping("/biz/needs")
@RequiredArgsConstructor
public class NeedController {

    private final NeedService needService;

    @RequirePerm("biz:need:list")
    @GetMapping
    public Result<PageResult<NeedVo>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) Integer year,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String needType,
                                           @RequestParam(required = false) Long deptId,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(needService.page(page, size, year, status, needType, deptId, keyword));
    }

    /** 我的申报 */
    @GetMapping("/my")
    public Result<PageResult<NeedVo>> my(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String keyword) {
        return Result.ok(needService.my(page, size, year, status, keyword));
    }

    @OperLog(module = "培训需求", operation = "保存需求申报")
    @PostMapping
    public Result<Void> add(@RequestBody TrainingNeed need) {
        needService.save(need);
        return Result.ok();
    }

    @OperLog(module = "培训需求", operation = "修改需求申报")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody TrainingNeed need) {
        need.setId(id);
        needService.save(need);
        return Result.ok();
    }

    @OperLog(module = "培训需求", operation = "删除需求申报")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        needService.delete(id);
        return Result.ok();
    }

    @OperLog(module = "培训需求", operation = "提交需求审核")
    @PutMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        needService.submit(id);
        return Result.ok();
    }

    @OperLog(module = "培训需求", operation = "审核需求")
    @RequirePerm("biz:need:audit")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean adopt = Boolean.TRUE.equals(body.get("adopt"));
        String remark = body.get("remark") == null ? null : body.get("remark").toString();
        needService.audit(id, adopt, remark);
        return Result.ok();
    }

    // ---------- 需求征集任务 ----------

    @RequirePerm("biz:need:list")
    @GetMapping("/collections")
    public Result<List<NeedCollection>> collections(@RequestParam(required = false) String status) {
        return Result.ok(needService.collections(status));
    }

    /** 进行中的征集任务(全员申报页展示) */
    @GetMapping("/collections/active")
    public Result<List<NeedCollection>> activeCollections() {
        return Result.ok(needService.activeCollections());
    }

    @OperLog(module = "需求征集", operation = "保存征集任务")
    @RequirePerm("biz:need:audit")
    @PostMapping("/collections")
    public Result<Void> addCollection(@RequestBody NeedCollection collection) {
        needService.saveCollection(collection);
        return Result.ok();
    }

    @OperLog(module = "需求征集", operation = "修改征集任务")
    @RequirePerm("biz:need:audit")
    @PutMapping("/collections/{id}")
    public Result<Void> updateCollection(@PathVariable Long id, @RequestBody NeedCollection collection) {
        collection.setId(id);
        needService.saveCollection(collection);
        return Result.ok();
    }

    @OperLog(module = "需求征集", operation = "结束征集任务")
    @RequirePerm("biz:need:audit")
    @PutMapping("/collections/{id}/close")
    public Result<Void> closeCollection(@PathVariable Long id) {
        needService.closeCollection(id);
        return Result.ok();
    }

    @OperLog(module = "需求征集", operation = "删除征集任务")
    @RequirePerm("biz:need:audit")
    @DeleteMapping("/collections/{id}")
    public Result<Void> deleteCollection(@PathVariable Long id) {
        needService.deleteCollection(id);
        return Result.ok();
    }
}
