package com.soe.training.controller;

import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.Course;
import com.soe.training.domain.vo.CourseVo;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 培训课程管理(课程库)
 */
@RestController
@RequestMapping("/biz/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @RequirePerm("biz:course:list")
    @GetMapping
    public Result<PageResult<CourseVo>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) String courseType,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String keyword) {
        return Result.ok(courseService.page(page, size, category, courseType, status, keyword));
    }

    /** 已发布课程下拉选项 */
    @RequirePerm("biz:class:list")
    @GetMapping("/published")
    public Result<List<Course>> published() {
        return Result.ok(courseService.published());
    }

    @OperLog(module = "培训课程", operation = "新增课程")
    @RequirePerm("biz:course:add")
    @PostMapping
    public Result<Void> add(@RequestBody Course course) {
        courseService.save(course);
        return Result.ok();
    }

    @OperLog(module = "培训课程", operation = "修改课程")
    @RequirePerm("biz:course:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Course course) {
        course.setId(id);
        courseService.save(course);
        return Result.ok();
    }

    @OperLog(module = "培训课程", operation = "删除课程")
    @RequirePerm("biz:course:remove")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.ok();
    }

    @OperLog(module = "培训课程", operation = "提交课程审核")
    @RequirePerm("biz:course:edit")
    @PutMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        courseService.submit(id);
        return Result.ok();
    }

    @OperLog(module = "培训课程", operation = "审核课程")
    @RequirePerm("biz:course:audit")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = Boolean.TRUE.equals(body.get("pass"));
        String remark = body.get("remark") == null ? null : body.get("remark").toString();
        courseService.audit(id, pass, remark);
        return Result.ok();
    }

    @OperLog(module = "培训课程", operation = "下架课程")
    @RequirePerm("biz:course:audit")
    @PutMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        courseService.offline(id);
        return Result.ok();
    }
}
