package com.soe.training.controller;

import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.Exam;
import com.soe.training.domain.Question;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 在线考试(柯氏二级): 题库 / 考试管理 / 学员作答
 */
@RestController
@RequestMapping("/biz")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    // ---------- 题库 ----------

    @RequirePerm("biz:question:list")
    @GetMapping("/questions")
    public Result<PageResult<Question>> questions(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) String qType,
                                                  @RequestParam(required = false) Long courseId,
                                                  @RequestParam(required = false) String keyword) {
        return Result.ok(examService.pageQuestions(page, size, qType, courseId, keyword));
    }

    @OperLog(module = "在线考试", operation = "保存题目")
    @RequirePerm("biz:question:edit")
    @PostMapping("/questions")
    public Result<Void> addQuestion(@RequestBody Question q) {
        examService.saveQuestion(q);
        return Result.ok();
    }

    @OperLog(module = "在线考试", operation = "保存题目")
    @RequirePerm("biz:question:edit")
    @PutMapping("/questions/{id}")
    public Result<Void> updateQuestion(@PathVariable Long id, @RequestBody Question q) {
        q.setId(id);
        examService.saveQuestion(q);
        return Result.ok();
    }

    @OperLog(module = "在线考试", operation = "删除题目")
    @RequirePerm("biz:question:edit")
    @DeleteMapping("/questions/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        examService.deleteQuestion(id);
        return Result.ok();
    }

    // ---------- 考试管理 ----------

    @RequirePerm("biz:exam:list")
    @GetMapping("/exams")
    public Result<PageResult<Map<String, Object>>> exams(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) Long classId,
                                                         @RequestParam(required = false) String keyword) {
        return Result.ok(examService.pageExams(page, size, status, classId, keyword));
    }

    @OperLog(module = "在线考试", operation = "保存考试")
    @RequirePerm("biz:exam:edit")
    @PostMapping("/exams")
    public Result<Void> addExam(@RequestBody Exam e) {
        examService.saveExam(e);
        return Result.ok();
    }

    @OperLog(module = "在线考试", operation = "保存考试")
    @RequirePerm("biz:exam:edit")
    @PutMapping("/exams/{id}")
    public Result<Void> updateExam(@PathVariable Long id, @RequestBody Exam e) {
        e.setId(id);
        examService.saveExam(e);
        return Result.ok();
    }

    @OperLog(module = "在线考试", operation = "删除考试")
    @RequirePerm("biz:exam:edit")
    @DeleteMapping("/exams/{id}")
    public Result<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return Result.ok();
    }

    @OperLog(module = "在线考试", operation = "发布考试")
    @RequirePerm("biz:exam:publish")
    @PutMapping("/exams/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        examService.publish(id);
        return Result.ok();
    }

    @OperLog(module = "在线考试", operation = "截止考试")
    @RequirePerm("biz:exam:publish")
    @PutMapping("/exams/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        examService.close(id);
        return Result.ok();
    }

    /** 考试成绩单 */
    @RequirePerm("biz:exam:list")
    @GetMapping("/exams/{id}/records")
    public Result<Map<String, Object>> records(@PathVariable Long id) {
        return Result.ok(examService.recordsOfExam(id));
    }

    // ---------- 学员作答 ----------

    /** 我的考试列表 */
    @GetMapping("/exams/my")
    public Result<List<Map<String, Object>>> my() {
        return Result.ok(examService.my());
    }

    /** 获取试卷(不含答案; 首次取卷开始计时) */
    @GetMapping("/exams/{id}/paper")
    public Result<Map<String, Object>> paper(@PathVariable Long id) {
        return Result.ok(examService.paper(id));
    }

    /** 交卷(自动判分) */
    @OperLog(module = "在线考试", operation = "提交试卷")
    @PostMapping("/exams/{id}/submit")
    public Result<Map<String, Object>> submit(@PathVariable Long id,
                                              @RequestBody Map<String, Object> body) {
        Object answers = body == null ? null : body.get("answers");
        @SuppressWarnings("unchecked")
        Map<String, Object> ans = answers instanceof Map ? (Map<String, Object>) answers : Map.of();
        return Result.ok(examService.submit(id, ans));
    }
}
