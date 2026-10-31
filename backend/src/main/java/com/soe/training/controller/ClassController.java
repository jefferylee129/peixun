package com.soe.training.controller;

import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.ClassSchedule;
import com.soe.training.domain.TrainingClass;
import com.soe.training.domain.vo.AttendanceVo;
import com.soe.training.domain.vo.ClassVo;
import com.soe.training.domain.vo.StudentVo;
import com.soe.training.domain.vo.ScheduleVo;
import com.soe.training.security.AuthContext;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 培训班次管理(开班/报名/排课/签到)
 */
@RestController
@RequestMapping("/biz/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @RequirePerm("biz:class:list")
    @GetMapping
    public Result<PageResult<ClassVo>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) Integer year,
                                            @RequestParam(required = false) Long deptId,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(classService.page(page, size, status, year, deptId, keyword));
    }

    /** 我报名的班次 */
    @GetMapping("/my")
    public Result<PageResult<ClassVo>> my(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return Result.ok(classService.myClasses(page, size));
    }

    /** 我可报名的班次 */
    @GetMapping("/enrollable")
    public Result<PageResult<ClassVo>> enrollable(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.ok(classService.enrollable(page, size));
    }

    @RequirePerm("biz:class:list")
    @GetMapping("/{id}/detail")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(classService.detail(id));
    }

    @OperLog(module = "培训班次", operation = "新增班次")
    @RequirePerm("biz:class:add")
    @PostMapping
    public Result<Void> add(@RequestBody TrainingClass trainingClass) {
        classService.save(trainingClass);
        return Result.ok();
    }

    @OperLog(module = "培训班次", operation = "修改班次")
    @RequirePerm("biz:class:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody TrainingClass trainingClass) {
        trainingClass.setId(id);
        classService.save(trainingClass);
        return Result.ok();
    }

    @OperLog(module = "培训班次", operation = "删除班次")
    @RequirePerm("biz:class:remove")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        classService.delete(id);
        return Result.ok();
    }

    @OperLog(module = "培训班次", operation = "班次状态流转")
    @RequirePerm("biz:class:edit")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        classService.changeStatus(id, status);
        return Result.ok();
    }

    // ---------- 学员 ----------

    @RequirePerm("biz:class:list")
    @GetMapping("/{id}/students")
    public Result<List<StudentVo>> students(@PathVariable Long id) {
        return Result.ok(classService.studentsOf(id));
    }

    @OperLog(module = "培训班次", operation = "批量添加学员")
    @RequirePerm("biz:class:edit")
    @PostMapping("/{id}/students")
    public Result<Void> addStudents(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        classService.addStudents(id, body.get("userIds"));
        return Result.ok();
    }

    @OperLog(module = "培训班次", operation = "移除学员")
    @RequirePerm("biz:class:edit")
    @DeleteMapping("/{id}/students/{userId}")
    public Result<Void> removeStudent(@PathVariable Long id, @PathVariable Long userId) {
        classService.removeStudent(id, userId);
        return Result.ok();
    }

    /** 员工自主报名 */
    @OperLog(module = "培训班次", operation = "自主报名")
    @PostMapping("/{id}/enroll")
    public Result<Void> enroll(@PathVariable Long id) {
        classService.selfEnroll(id);
        return Result.ok();
    }

    // ---------- 课表 ----------

    @RequirePerm("biz:class:list")
    @GetMapping("/{id}/schedules")
    public Result<List<ScheduleVo>> schedules(@PathVariable Long id) {
        return Result.ok(classService.schedulesOf(id));
    }

    @OperLog(module = "培训班次", operation = "新增课节")
    @RequirePerm("biz:class:edit")
    @PostMapping("/{id}/schedules")
    public Result<Void> addSchedule(@PathVariable Long id, @RequestBody ClassSchedule schedule) {
        schedule.setClassId(id);
        classService.addSchedule(schedule);
        return Result.ok();
    }

    @OperLog(module = "培训班次", operation = "修改课节")
    @RequirePerm("biz:class:edit")
    @PutMapping("/schedules/{sid}")
    public Result<Void> updateSchedule(@PathVariable Long sid, @RequestBody ClassSchedule schedule) {
        classService.updateSchedule(sid, schedule);
        return Result.ok();
    }

    @OperLog(module = "培训班次", operation = "删除课节")
    @RequirePerm("biz:class:edit")
    @DeleteMapping("/schedules/{sid}")
    public Result<Void> deleteSchedule(@PathVariable Long sid) {
        classService.deleteSchedule(sid);
        return Result.ok();
    }

    /** 结束课节: 按签到生成学时台账 */
    @OperLog(module = "培训班次", operation = "结束课节并计学时")
    @RequirePerm("biz:class:sign")
    @PutMapping("/schedules/{sid}/finish")
    public Result<Void> finishSchedule(@PathVariable Long sid) {
        classService.finishSchedule(sid);
        return Result.ok();
    }

    @RequirePerm("biz:class:list")
    @GetMapping("/schedules/{sid}/attendance")
    public Result<List<AttendanceVo>> attendance(@PathVariable Long sid) {
        return Result.ok(classService.attendanceOf(sid));
    }

    @OperLog(module = "培训班次", operation = "学员签到")
    @RequirePerm("biz:class:sign")
    @PostMapping("/schedules/{sid}/attendance")
    public Result<Void> sign(@PathVariable Long sid, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String signType = body.get("signType") == null ? "normal" : body.get("signType").toString();
        classService.sign(sid, userId, signType);
        return Result.ok();
    }
}
