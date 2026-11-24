package com.soe.training.controller;

import com.soe.training.common.BusinessException;
import com.soe.training.common.Result;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.LearningMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 岗位学习地图(年度必修课包)
 */
@RestController
@RequestMapping("/biz/learning-map")
@RequiredArgsConstructor
public class LearningMapController {

    private final LearningMapService learningMapService;

    /** 配置矩阵: 已发布课程 × 人员类型 */
    @RequirePerm("biz:map:list")
    @GetMapping("/matrix")
    public Result<Map<String, Object>> matrix(@RequestParam(required = false) Integer year) {
        return Result.ok(learningMapService.matrix(year));
    }

    @OperLog(module = "学习地图", operation = "保存必修课配置")
    @RequirePerm("biz:map:edit")
    @PostMapping("/save")
    public Result<Integer> save(@RequestBody Map<String, Object> body) {
        Integer year = body.get("year") == null ? null : Integer.valueOf(body.get("year").toString());
        Object type = body.get("userType");
        if (type == null) {
            throw new BusinessException("请选择人员类型");
        }
        @SuppressWarnings("unchecked")
        List<Object> raw = body.get("courseIds") instanceof List ? (List<Object>) body.get("courseIds") : List.of();
        List<Long> courseIds = raw.stream()
                .filter(v -> v != null && !v.toString().isBlank())
                .map(v -> Long.valueOf(v.toString().trim())).toList();
        return Result.ok(learningMapService.save(year, type.toString(), courseIds));
    }

    /** 我的学习地图 */
    @GetMapping("/my")
    public Result<Map<String, Object>> my(@RequestParam(required = false) Integer year) {
        return Result.ok(learningMapService.my(year));
    }

    /** 各人员类型必修完成率 */
    @RequirePerm("biz:map:list")
    @GetMapping("/completion")
    public Result<List<Map<String, Object>>> completion(@RequestParam(required = false) Integer year) {
        return Result.ok(learningMapService.completionStats(year));
    }

    /** 指定人员的学习地图(需查他人档案权限) */
    @RequirePerm("biz:archive:other")
    @GetMapping("/user/{userId}")
    public Result<Map<String, Object>> ofUser(@PathVariable Long userId,
                                              @RequestParam(required = false) Integer year) {
        return Result.ok(learningMapService.ofUser(userId, year));
    }
}
