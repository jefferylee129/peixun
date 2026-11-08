package com.soe.training.controller;

import com.soe.training.common.ImportResult;
import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.CourseSource;
import com.soe.training.domain.ExternalCourse;
import com.soe.training.domain.SyncLog;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.IntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 外部课程同步(云学堂等)集成接口
 */
@RestController
@RequestMapping("/biz/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;

    // ---------- 数据源 ----------

    @RequirePerm("biz:integration:list")
    @GetMapping("/sources")
    public Result<PageResult<CourseSource>> sources(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String status) {
        return Result.ok(integrationService.pageSources(page, size, keyword, status));
    }

    @RequirePerm("biz:integration:list")
    @GetMapping("/sources/{id}")
    public Result<CourseSource> source(@PathVariable Long id) {
        return Result.ok(integrationService.getSource(id));
    }

    @OperLog(module = "外部课程同步", operation = "保存数据源")
    @RequirePerm("biz:integration:edit")
    @PostMapping("/sources")
    public Result<Long> addSource(@RequestBody CourseSource source) {
        integrationService.saveSource(source);
        return Result.ok(source.getId());
    }

    @OperLog(module = "外部课程同步", operation = "修改数据源")
    @RequirePerm("biz:integration:edit")
    @PutMapping("/sources/{id}")
    public Result<Void> updateSource(@PathVariable Long id, @RequestBody CourseSource source) {
        source.setId(id);
        integrationService.saveSource(source);
        return Result.ok();
    }

    @OperLog(module = "外部课程同步", operation = "删除数据源")
    @RequirePerm("biz:integration:edit")
    @DeleteMapping("/sources/{id}")
    public Result<Void> deleteSource(@PathVariable Long id) {
        integrationService.deleteSource(id);
        return Result.ok();
    }

    @OperLog(module = "外部课程同步", operation = "启停数据源")
    @RequirePerm("biz:integration:edit")
    @PutMapping("/sources/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        integrationService.changeStatus(id, status);
        return Result.ok();
    }

    @OperLog(module = "外部课程同步", operation = "测试数据源连接")
    @RequirePerm("biz:integration:sync")
    @PostMapping("/sources/{id}/test")
    public Result<Map<String, Object>> test(@PathVariable Long id) {
        return Result.ok(integrationService.testConnection(id));
    }

    @OperLog(module = "外部课程同步", operation = "同步外部课程")
    @RequirePerm("biz:integration:sync")
    @PostMapping("/sources/{id}/sync")
    public Result<Map<String, Object>> sync(@PathVariable Long id) {
        return Result.ok(integrationService.sync(id));
    }

    // ---------- 暂存课程 ----------

    @RequirePerm("biz:integration:list")
    @GetMapping("/external-courses")
    public Result<PageResult<ExternalCourse>> externalCourses(@RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "10") int size,
                                                              @RequestParam(required = false) Long sourceId,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(required = false) String keyword) {
        return Result.ok(integrationService.pageExternal(page, size, sourceId, status, keyword));
    }

    /** 入库到课程库: 支持按 ids 或 sourceId+all */
    @OperLog(module = "外部课程同步", operation = "外部课程入库")
    @RequirePerm("biz:integration:sync")
    @PostMapping("/external-courses/import-to-course")
    public Result<Map<String, Object>> importToCourse(@RequestBody Map<String, Object> body) {
        List<Long> ids = body.get("ids") == null ? null
                : ((List<?>) body.get("ids")).stream().map(v -> Long.valueOf(v.toString())).toList();
        Long sourceId = body.get("sourceId") == null ? null : Long.valueOf(body.get("sourceId").toString());
        boolean all = Boolean.TRUE.equals(body.get("all"));
        return Result.ok(integrationService.importToCourse(sourceId, ids, all));
    }

    @OperLog(module = "外部课程同步", operation = "忽略外部课程")
    @RequirePerm("biz:integration:sync")
    @PostMapping("/external-courses/ignore")
    public Result<Integer> ignore(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("ids");
        List<Long> ids = raw == null ? List.of() : raw.stream().map(v -> Long.valueOf(v.toString())).toList();
        return Result.ok(integrationService.ignore(ids));
    }

    // ---------- 日志 ----------

    @RequirePerm("biz:integration:list")
    @GetMapping("/logs")
    public Result<PageResult<SyncLog>> logs(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) Long sourceId,
                                            @RequestParam(required = false) String action) {
        return Result.ok(integrationService.pageLogs(page, size, sourceId, action));
    }

    @RequirePerm("biz:integration:list")
    @GetMapping("/logs/{id}")
    public Result<SyncLog> logDetail(@PathVariable Long id) {
        return Result.ok(integrationService.logDetail(id));
    }
}
