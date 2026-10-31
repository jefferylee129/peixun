package com.soe.training.controller;

import com.soe.training.common.BusinessException;
import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.EvalTemplate;
import com.soe.training.domain.vo.EvalRecordVo;
import com.soe.training.domain.vo.EvalSummaryVo;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.EvalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 培训评估管理(评估模板 + 评估记录 + 汇总)
 */
@RestController
@RequestMapping("/biz/evals")
@RequiredArgsConstructor
public class EvalController {

    private final EvalService evalService;
    private final ObjectMapper objectMapper;

    // ---------- 模板 ----------

    @RequirePerm("biz:eval:list")
    @GetMapping("/templates")
    public Result<PageResult<EvalTemplate>> templates(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      @RequestParam(required = false) String keyword) {
        return Result.ok(evalService.templatePage(page, size, keyword));
    }

    @RequirePerm("biz:eval:list")
    @GetMapping("/templates/{id}")
    public Result<Map<String, Object>> templateDetail(@PathVariable Long id) {
        return Result.ok(evalService.templateDetail(id));
    }

    /** 员工填写评估时获取模板条目(仅需登录, 不需要管理权限) */
    @GetMapping("/templates/{id}/fill-view")
    public Result<Map<String, Object>> templateFillView(@PathVariable Long id) {
        return Result.ok(evalService.templateDetail(id));
    }

    @OperLog(module = "培训评估", operation = "保存评估模板")
    @RequirePerm("biz:eval:template")
    @PostMapping("/templates")
    public Result<Void> addTemplate(@RequestBody Map<String, Object> body) {
        evalService.saveTemplate(toTemplate(body, null));
        return Result.ok();
    }

    @OperLog(module = "培训评估", operation = "修改评估模板")
    @RequirePerm("biz:eval:template")
    @PutMapping("/templates/{id}")
    public Result<Void> updateTemplate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        evalService.saveTemplate(toTemplate(body, id));
        return Result.ok();
    }

    /** 兼容 items 传 JSON 字符串或原生数组两种形式 */
    private EvalTemplate toTemplate(Map<String, Object> body, Long id) {
        EvalTemplate t = new EvalTemplate();
        t.setId(id);
        t.setTemplateName((String) body.get("templateName"));
        Object level = body.get("evalLevel");
        if (level != null) {
            t.setEvalLevel(Integer.valueOf(level.toString()));
        }
        Object status = body.get("status");
        if (status != null) {
            t.setStatus(status.toString());
        }
        t.setDescription((String) body.get("description"));
        Object items = body.get("items");
        if (items instanceof String s) {
            t.setItems(s);
        } else if (items != null) {
            try {
                t.setItems(objectMapper.writeValueAsString(items));
            } catch (Exception e) {
                throw new BusinessException("评估条目格式不正确");
            }
        }
        return t;
    }

    @OperLog(module = "培训评估", operation = "删除评估模板")
    @RequirePerm("biz:eval:template")
    @DeleteMapping("/templates/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        evalService.deleteTemplate(id);
        return Result.ok();
    }

    // ---------- 评估填写与汇总 ----------

    /** 学员提交评估 */
    @OperLog(module = "培训评估", operation = "提交评估")
    @PostMapping("/records")
    public Result<Void> fill(@RequestBody Map<String, Object> body) {
        Long templateId = Long.valueOf(body.get("templateId").toString());
        Long classId = Long.valueOf(body.get("classId").toString());
        @SuppressWarnings("unchecked")
        Map<String, Object> scores = (Map<String, Object>) body.get("scores");
        String comment = body.get("comment") == null ? null : body.get("comment").toString();
        evalService.fill(templateId, classId, scores, comment);
        return Result.ok();
    }

    @RequirePerm("biz:eval:list")
    @GetMapping("/records")
    public Result<List<EvalRecordVo>> records(@RequestParam Long classId,
                                              @RequestParam(required = false) Long templateId) {
        return Result.ok(evalService.records(classId, templateId));
    }

    /** 我的作答 */
    @GetMapping("/records/my")
    public Result<EvalRecordVo> myRecord(@RequestParam Long classId, @RequestParam Long templateId) {
        return Result.ok(evalService.myRecord(classId, templateId));
    }

    @RequirePerm("biz:eval:list")
    @GetMapping("/summary")
    public Result<EvalSummaryVo> summary(@RequestParam Long classId, @RequestParam Long templateId) {
        return Result.ok(evalService.summary(classId, templateId));
    }

    /** 我的待评估班次 */
    @GetMapping("/pending")
    public Result<List<Map<String, Object>>> pending() {
        return Result.ok(evalService.pending());
    }
}
