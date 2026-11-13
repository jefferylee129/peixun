package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.ClassStudent;
import com.soe.training.domain.EvalRecord;
import com.soe.training.domain.EvalTemplate;
import com.soe.training.domain.TrainingClass;
import com.soe.training.domain.vo.EvalRecordVo;
import com.soe.training.domain.vo.EvalSummaryVo;
import com.soe.training.mapper.ClassMapper;
import com.soe.training.mapper.ClassStudentMapper;
import com.soe.training.mapper.EvalRecordMapper;
import com.soe.training.mapper.EvalTemplateMapper;
import com.soe.training.security.AuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 培训评估服务(柯氏四级评估问卷 + 汇总)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvalService {

    private final EvalTemplateMapper templateMapper;
    private final EvalRecordMapper recordMapper;
    private final ClassStudentMapper studentMapper;
    private final ClassMapper classMapper;
    private final ObjectMapper objectMapper;

    // ---------- 评估模板 ----------

    public PageResult<EvalTemplate> templatePage(int page, int size, String keyword) {
        LambdaQueryWrapper<EvalTemplate> w = new LambdaQueryWrapper<EvalTemplate>()
                .like(keyword != null && !keyword.isBlank(), EvalTemplate::getTemplateName, keyword)
                .orderByDesc(EvalTemplate::getId);
        Page<EvalTemplate> p = templateMapper.selectPage(PageQuery.of(page, size), w);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public Map<String, Object> templateDetail(Long id) {
        EvalTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException("评估模板不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", t.getId());
        result.put("templateName", t.getTemplateName());
        result.put("evalLevel", t.getEvalLevel());
        result.put("status", t.getStatus());
        result.put("description", t.getDescription());
        result.put("itemList", parseItems(t.getItems()));
        return result;
    }

    public void saveTemplate(EvalTemplate t) {
        if (t.getItems() == null || t.getItems().isBlank()) {
            throw new BusinessException("评估条目不能为空");
        }
        parseItems(t.getItems());
        if (t.getStatus() == null) {
            t.setStatus("0");
        }
        if (t.getId() == null) {
            templateMapper.insert(t);
        } else {
            EvalTemplate old = templateMapper.selectById(t.getId());
            if (old == null) {
                throw new BusinessException("评估模板不存在");
            }
            // 已有评估作答后条目冻结, 只允许改名称/状态/描述, 防止历史作答与条目错位
            if (!old.getItems().equals(t.getItems())) {
                Long used = recordMapper.selectCount(new LambdaQueryWrapper<EvalRecord>()
                        .eq(EvalRecord::getTemplateId, t.getId()));
                if (used > 0) {
                    throw new BusinessException("该模板已存在评估记录, 评估条目不可再修改");
                }
            }
            templateMapper.updateById(t);
        }
    }

    public void deleteTemplate(Long id) {
        Long used = recordMapper.selectCount(new LambdaQueryWrapper<EvalRecord>()
                .eq(EvalRecord::getTemplateId, id));
        if (used > 0) {
            throw new BusinessException("该模板已存在评估记录, 不能删除");
        }
        templateMapper.deleteById(id);
    }

    // ---------- 评估填写 ----------

    @Transactional
    public void fill(Long templateId, Long classId, Map<String, Object> scores, String comment) {
        Long userId = AuthContext.getUserId();
        EvalTemplate t = templateMapper.selectById(templateId);
        if (t == null || !"0".equals(t.getStatus())) {
            throw new BusinessException("评估模板不存在或已停用");
        }
        // 与 pending() 口径一致: 仅启用的一级(反应层)问卷可用于班次评估
        if (t.getEvalLevel() == null || t.getEvalLevel() != 1) {
            throw new BusinessException("仅一级(反应层)评估问卷可用于班次评估");
        }
        TrainingClass c = classMapper.selectById(classId);
        if (c == null) {
            throw new BusinessException("班次不存在");
        }
        if (!"2".equals(c.getStatus()) && !"3".equals(c.getStatus())) {
            throw new BusinessException("班次进行中或结束后才能评估");
        }
        if (studentMapper.selectOne(classId, userId) == null) {
            throw new BusinessException("您未参加该班次, 无需评估");
        }
        List<Map<String, Object>> items = parseItems(t.getItems());
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            if (!"score".equals(item.get("type"))) {
                continue;
            }
            Object v = scores.get(String.valueOf(item.get("key")));
            if (v == null) {
                if (Boolean.TRUE.equals(item.get("required"))) {
                    throw new BusinessException("请完成必填评估项: " + item.get("title"));
                }
                continue;
            }
            try {
                BigDecimal score = new BigDecimal(v.toString());
                if (score.compareTo(BigDecimal.ONE) < 0 || score.compareTo(new BigDecimal("5")) > 0) {
                    throw new BusinessException("评分须在1-5分之间");
                }
                total = total.add(score);
            } catch (NumberFormatException e) {
                throw new BusinessException("评分格式不正确");
            }
        }
        EvalRecord old = recordMapper.selectOne(classId, templateId, userId);
        if (old == null) {
            EvalRecord r = new EvalRecord();
            r.setTemplateId(templateId);
            r.setClassId(classId);
            r.setUserId(userId);
            r.setScores(writeJson(scores));
            r.setTotalScore(total);
            r.setComment(comment);
            r.setSubmitTime(LocalDateTime.now());
            recordMapper.insert(r);
        } else {
            old.setScores(writeJson(scores));
            old.setTotalScore(total);
            old.setComment(comment);
            old.setSubmitTime(LocalDateTime.now());
            recordMapper.updateById(old);
        }
    }

    public List<EvalRecordVo> records(Long classId, Long templateId) {
        return recordMapper.selectByClass(classId, templateId);
    }

    public EvalRecordVo myRecord(Long classId, Long templateId) {
        Long userId = AuthContext.getUserId();
        EvalRecord r = recordMapper.selectOne(classId, templateId, userId);
        if (r == null) {
            return null;
        }
        EvalRecordVo vo = new EvalRecordVo();
        org.springframework.beans.BeanUtils.copyProperties(r, vo);
        return vo;
    }

    /** 评估汇总: 逐项平均分 + 总体平均分 + 文本意见 */
    public EvalSummaryVo summary(Long classId, Long templateId) {
        EvalTemplate t = templateMapper.selectById(templateId);
        if (t == null) {
            throw new BusinessException("评估模板不存在");
        }
        List<Map<String, Object>> items = parseItems(t.getItems());
        List<EvalRecordVo> records = recordMapper.selectByClass(classId, templateId);

        Map<String, List<BigDecimal>> scoreMap = new HashMap<>();
        List<Map<String, String>> textAnswers = new ArrayList<>();
        List<Map<String, String>> comments = new ArrayList<>();
        BigDecimal scoreSum = BigDecimal.ZERO;
        int scoreCnt = 0;

        for (EvalRecordVo r : records) {
            Map<String, Object> scores = parseMap(r.getScores());
            for (Map<String, Object> item : items) {
                String key = String.valueOf(item.get("key"));
                String type = String.valueOf(item.get("type"));
                Object v = scores.get(key);
                if (v == null) {
                    continue;
                }
                if ("score".equals(type)) {
                    try {
                        BigDecimal val = new BigDecimal(v.toString());
                        scoreMap.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
                        scoreSum = scoreSum.add(val);
                        scoreCnt++;
                    } catch (NumberFormatException ignored) {
                    }
                } else if (!v.toString().isBlank()) {
                    Map<String, String> answer = new LinkedHashMap<>();
                    answer.put("user", r.getNickname() == null ? "" : r.getNickname());
                    answer.put("title", String.valueOf(item.get("title")));
                    answer.put("value", v.toString());
                    textAnswers.add(answer);
                }
            }
            if (r.getComment() != null && !r.getComment().isBlank()) {
                Map<String, String> cm = new LinkedHashMap<>();
                cm.put("user", r.getNickname() == null ? "" : r.getNickname());
                cm.put("comment", r.getComment());
                comments.add(cm);
            }
        }

        EvalSummaryVo vo = new EvalSummaryVo();
        vo.setTemplate(t);
        vo.setItems(items);
        Map<String, BigDecimal> itemAvg = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            String key = String.valueOf(item.get("key"));
            if ("score".equals(item.get("type"))) {
                List<BigDecimal> list = scoreMap.get(key);
                itemAvg.put(key, list == null || list.isEmpty() ? BigDecimal.ZERO
                        : avg(list));
            }
        }
        vo.setItemAvg(itemAvg);
        vo.setOverallAvg(scoreCnt == 0 ? BigDecimal.ZERO
                : scoreSum.divide(new BigDecimal(scoreCnt), 1, RoundingMode.HALF_UP));
        vo.setEvalCount(records.size());
        vo.setTextAnswers(textAnswers);
        vo.setComments(comments);
        return vo;
    }

    /** 我的待评估班次 */
    public List<Map<String, Object>> pending() {
        Long userId = AuthContext.getUserId();
        List<Map<String, Object>> result = new ArrayList<>();
        List<ClassStudent> mine = studentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getUserId, userId));
        List<EvalTemplate> actives = templateMapper.selectList(new LambdaQueryWrapper<EvalTemplate>()
                .eq(EvalTemplate::getStatus, "0")
                .eq(EvalTemplate::getEvalLevel, 1)
                .orderByDesc(EvalTemplate::getId));
        if (actives.isEmpty()) {
            return result;
        }
        EvalTemplate active = actives.get(0);
        for (ClassStudent s : mine) {
            TrainingClass c = classMapper.selectById(s.getClassId());
            if (c == null || (!"2".equals(c.getStatus()) && !"3".equals(c.getStatus()))) {
                continue;
            }
            EvalRecord filled = recordMapper.selectOne(c.getId(), active.getId(), userId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("classId", c.getId());
            item.put("className", c.getClassName());
            item.put("endDate", c.getEndDate());
            item.put("studentStatus", s.getStatus());
            item.put("templateId", active.getId());
            item.put("templateName", active.getTemplateName());
            item.put("filled", filled != null);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> parseItems(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new BusinessException("评估条目格式不正确");
        }
    }

    private Map<String, Object> parseMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("评估作答解析失败: {}", json);
            return Map.of();
        }
    }

    private String writeJson(Map<String, Object> scores) {
        try {
            return objectMapper.writeValueAsString(scores == null ? Map.of() : scores);
        } catch (Exception e) {
            return "{}";
        }
    }

    private BigDecimal avg(List<BigDecimal> list) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : list) {
            sum = sum.add(v);
        }
        return sum.divide(new BigDecimal(list.size()), 1, RoundingMode.HALF_UP);
    }
}
