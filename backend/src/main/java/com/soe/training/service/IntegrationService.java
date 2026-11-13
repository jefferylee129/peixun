package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.Course;
import com.soe.training.domain.CourseSource;
import com.soe.training.domain.ExternalCourse;
import com.soe.training.domain.SyncLog;
import com.soe.training.mapper.CourseMapper;
import com.soe.training.mapper.CourseSourceMapper;
import com.soe.training.mapper.ExternalCourseMapper;
import com.soe.training.mapper.SyncLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 外部课程同步: 数据源管理 / 测试连接 / 同步拉取 / 暂存审核入库 / 日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationService {

    private final CourseSourceMapper sourceMapper;
    private final ExternalCourseMapper externalCourseMapper;
    private final SyncLogMapper syncLogMapper;
    private final CourseMapper courseMapper;
    private final IntegrationPullService pullService;
    private final ObjectMapper objectMapper;

    // ---------- 数据源 ----------

    public PageResult<CourseSource> pageSources(int page, int size, String keyword, String status) {
        Page<CourseSource> p = PageQuery.of(page, size);
        Page<CourseSource> result = sourceMapper.selectPage(p, new LambdaQueryWrapper<CourseSource>()
                .and(keyword != null && !keyword.isBlank(), q -> q
                        .like(CourseSource::getSourceName, keyword)
                        .or().like(CourseSource::getBaseUrl, keyword))
                .eq(status != null && !status.isBlank(), CourseSource::getStatus, status)
                .orderByDesc(CourseSource::getId));
        result.getRecords().forEach(this::maskSecret);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    public CourseSource getSource(Long id) {
        CourseSource src = sourceMapper.selectById(id);
        if (src == null) {
            throw new BusinessException("数据源不存在");
        }
        maskSecret(src);
        return src;
    }

    /** 保存数据源; 回传的掩码口令不覆盖真实值 */
    @Transactional
    public void saveSource(CourseSource src) {
        if (src.getSourceName() == null || src.getSourceName().isBlank()) {
            throw new BusinessException("数据源名称不能为空");
        }
        if (!List.of("yunxuetang", "api", "mock").contains(src.getSourceType())) {
            throw new BusinessException("不支持的数据源类型");
        }
        if (!"mock".equals(src.getSourceType())) {
            if (isBlank(src.getBaseUrl()) || isBlank(src.getListPath())) {
                throw new BusinessException("外部数据源必须配置开放平台地址与课程列表接口路径");
            }
        }
        validateJson(src.getCategoryMapping(), "分类映射");
        validateJson(src.getFieldMapping(), "字段映射");
        validateJson(src.getExtraConfig(), "其他配置");
        if (src.getStatus() == null) {
            src.setStatus("0");
        }
        if (src.getId() == null) {
            if (!"mock".equals(src.getSourceType()) && !isBlank(src.getAppSecret())) {
                // 新建时原样保存
            }
            sourceMapper.insert(src);
        } else {
            CourseSource old = sourceMapper.selectById(src.getId());
            if (old == null) {
                throw new BusinessException("数据源不存在");
            }
            if (isBlank(src.getAppSecret()) || "******".equals(src.getAppSecret())) {
                src.setAppSecret(old.getAppSecret());
            }
            src.setLastSyncTime(null);
            sourceMapper.updateById(src);
        }
    }

    @Transactional
    public void deleteSource(Long id) {
        sourceMapper.deleteById(id);
    }

    @Transactional
    public void changeStatus(Long id, String status) {
        if (sourceMapper.selectById(id) == null) {
            throw new BusinessException("数据源不存在");
        }
        if (!"0".equals(status) && !"1".equals(status)) {
            throw new BusinessException("状态仅允许 0启用 / 1停用");
        }
        CourseSource u = new CourseSource();
        u.setId(id);
        u.setStatus(status);
        sourceMapper.updateById(u);
    }

    // ---------- 测试连接 ----------

    public Map<String, Object> testConnection(Long id) {
        CourseSource src = sourceMapper.selectById(id);
        if (src == null) {
            throw new BusinessException("数据源不存在");
        }
        Map<String, Object> result = pullService.test(src);
        saveLog(src, "test", Boolean.TRUE.equals(result.get("ok")) ? "0" : "1",
                0, 0, 0, 0, String.valueOf(result.get("message")), null);
        return result;
    }

    // ---------- 同步拉取 ----------

    @Transactional
    public Map<String, Object> sync(Long id) {
        CourseSource src = sourceMapper.selectById(id);
        if (src == null) {
            throw new BusinessException("数据源不存在");
        }
        if (!"0".equals(src.getStatus())) {
            throw new BusinessException("数据源已停用, 请先启用");
        }
        LocalDateTime start = LocalDateTime.now();
        SyncLog logEntity = new SyncLog();
        logEntity.setSourceId(src.getId());
        logEntity.setSourceName(src.getSourceName());
        logEntity.setAction("pull");
        logEntity.setStartTime(start);
        try {
            IntegrationPullService.PullOutcome outcome = pullService.pull(src);
            String status = outcome.fail() == 0 ? "0" : "2";
            logEntity.setStatus(status);
            logEntity.setTotal(outcome.total());
            logEntity.setSuccess(outcome.success());
            logEntity.setFail(outcome.fail());
            logEntity.setSkip(outcome.skip());
            logEntity.setMessage("拉取完成: 共" + outcome.total() + "条, 新增/更新" + outcome.success() + ", 失败" + outcome.fail());
            logEntity.setDetail(joinErrors(outcome.errors()));
            syncLogMapper.insert(logEntity);
            CourseSource u = new CourseSource();
            u.setId(src.getId());
            u.setLastSyncTime(LocalDateTime.now());
            sourceMapper.updateById(u);
            return resultOf(logEntity);
        } catch (Exception e) {
            logEntity.setStatus("1");
            logEntity.setTotal(0);
            logEntity.setSuccess(0);
            logEntity.setFail(0);
            logEntity.setMessage("同步失败: " + e.getMessage());
            syncLogMapper.insert(logEntity);
            log.warn("外部课程同步失败 source={}", src.getId(), e);
            return resultOf(logEntity);
        }
    }

    // ---------- 暂存课程 ----------

    public PageResult<ExternalCourse> pageExternal(int page, int size, Long sourceId, String status, String keyword) {
        Page<ExternalCourse> p = PageQuery.of(page, size);
        Page<ExternalCourse> result = externalCourseMapper.selectPage(p, new LambdaQueryWrapper<ExternalCourse>()
                .eq(sourceId != null, ExternalCourse::getSourceId, sourceId)
                .eq(status != null && !status.isBlank(), ExternalCourse::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), ExternalCourse::getCourseName, keyword)
                .orderByDesc(ExternalCourse::getSyncTime));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 暂存课程入库: 按来源+外部编码幂等(存在则更新) */
    @Transactional
    public Map<String, Object> importToCourse(Long sourceId, List<Long> ids, boolean all) {
        List<ExternalCourse> list;
        if (ids != null && !ids.isEmpty()) {
            list = externalCourseMapper.selectBatchIds(ids);
        } else {
            LambdaQueryWrapper<ExternalCourse> w = new LambdaQueryWrapper<ExternalCourse>()
                    .eq(ExternalCourse::getStatus, "0")
                    .orderByDesc(ExternalCourse::getSyncTime)
                    .last("LIMIT 500");
            if (all) {
                w = new LambdaQueryWrapper<ExternalCourse>()
                        .eq(sourceId != null, ExternalCourse::getSourceId, sourceId)
                        .in(ExternalCourse::getStatus, "0", "1")
                        .orderByDesc(ExternalCourse::getSyncTime)
                        .last("LIMIT 500");
            }
            list = externalCourseMapper.selectList(w);
        }
        if (list.isEmpty()) {
            throw new BusinessException("没有可入库的课程(请先同步, 或所选记录均已处理)");
        }
        int success = 0, fail = 0;
        List<String> errors = new ArrayList<>();
        CourseSource src = sourceMapper.selectById(list.get(0).getSourceId());
        for (ExternalCourse ext : list) {
            try {
                if ("2".equals(ext.getStatus())) {
                    errors.add("课程「" + ext.getCourseName() + "」已忽略, 跳过");
                    continue;
                }
                CourseSource s = ext.getSourceId() != null && (src == null || !ext.getSourceId().equals(src.getId()))
                        ? sourceMapper.selectById(ext.getSourceId()) : src;
                String category = ext.getCategory();
                if (isBlank(category)) {
                    category = s == null ? "general" : defaultCategory(s);
                }
                Course c = courseMapper.selectOne(new LambdaQueryWrapper<Course>()
                        .eq(Course::getSourceId, ext.getSourceId())
                        .eq(Course::getExternalCode, ext.getExternalId()));
                boolean isNew = c == null;
                if (isNew) {
                    c = new Course();
                    c.setSourceId(ext.getSourceId());
                    c.setExternalCode(ext.getExternalId());
                }
                c.setCourseName(ext.getCourseName());
                c.setCategory(category);
                c.setCourseType(ext.getCourseType() == null ? "online" : ext.getCourseType());
                c.setHours(ext.getHours() == null ? BigDecimal.ZERO : ext.getHours());
                c.setLevel(ext.getLevel());
                c.setTargetUsers(ext.getTargetUsers());
                c.setOutline(ext.getDescription());
                c.setExternalUrl(ext.getUrl());
                if (isNew) {
                    c.setStatus("2");
                    c.setSourceType(1);
                    courseMapper.insert(c);
                } else {
                    courseMapper.updateById(c);
                }
                ext.setStatus("1");
                ext.setCourseId(c.getId());
                externalCourseMapper.updateById(ext);
                success++;
            } catch (Exception e) {
                fail++;
                errors.add("课程「" + ext.getCourseName() + "」入库失败: " + e.getMessage());
            }
        }
        SyncLog logEntity = new SyncLog();
        logEntity.setSourceId(src == null ? null : src.getId());
        logEntity.setSourceName(src == null ? null : src.getSourceName());
        logEntity.setAction("import");
        logEntity.setStatus(fail == 0 ? "0" : "2");
        logEntity.setTotal(list.size());
        logEntity.setSuccess(success);
        logEntity.setFail(fail);
        logEntity.setMessage("入库完成: 成功" + success + ", 失败" + fail);
        logEntity.setDetail(joinErrors(errors));
        logEntity.setStartTime(LocalDateTime.now());
        logEntity.setEndTime(LocalDateTime.now());
        syncLogMapper.insert(logEntity);
        Map<String, Object> result = resultOf(logEntity);
        result.put("errors", errors);
        return result;
    }

    @Transactional
    public int ignore(List<Long> ids) {
        int n = 0;
        for (Long id : ids) {
            if (externalCourseMapper.selectById(id) == null) {
                throw new BusinessException("暂存课程不存在: " + id);
            }
            ExternalCourse u = new ExternalCourse();
            u.setId(id);
            u.setStatus("2");
            externalCourseMapper.updateById(u);
            n++;
        }
        return n;
    }

    // ---------- 日志 ----------

    public PageResult<SyncLog> pageLogs(int page, int size, Long sourceId, String action) {
        Page<SyncLog> p = PageQuery.of(page, size);
        Page<SyncLog> result = syncLogMapper.selectPage(p, new LambdaQueryWrapper<SyncLog>()
                .eq(sourceId != null, SyncLog::getSourceId, sourceId)
                .eq(action != null && !action.isBlank(), SyncLog::getAction, action)
                .orderByDesc(SyncLog::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    public SyncLog logDetail(Long id) {
        SyncLog logEntity = syncLogMapper.selectById(id);
        if (logEntity == null) {
            throw new BusinessException("日志不存在");
        }
        return logEntity;
    }

    // ---------- 辅助 ----------

    private Map<String, Object> resultOf(SyncLog logEntity) {
        Map<String, Object> m = new HashMap<>();
        m.put("logId", logEntity.getId());
        m.put("status", logEntity.getStatus());
        m.put("message", logEntity.getMessage());
        m.put("total", logEntity.getTotal());
        m.put("success", logEntity.getSuccess());
        m.put("fail", logEntity.getFail());
        m.put("skip", logEntity.getSkip());
        return m;
    }

    private void saveLog(CourseSource src, String action, String status, int total, int success,
                         int fail, int skip, String message, String detail) {
        SyncLog logEntity = new SyncLog();
        logEntity.setSourceId(src.getId());
        logEntity.setSourceName(src.getSourceName());
        logEntity.setAction(action);
        logEntity.setStatus(status);
        logEntity.setTotal(total);
        logEntity.setSuccess(success);
        logEntity.setFail(fail);
        logEntity.setSkip(skip);
        logEntity.setMessage(message);
        logEntity.setDetail(detail);
        logEntity.setStartTime(LocalDateTime.now());
        logEntity.setEndTime(LocalDateTime.now());
        syncLogMapper.insert(logEntity);
    }

    private String joinErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        String s = String.join("\n", errors);
        return s.length() > 4000 ? s.substring(0, 4000) : s;
    }

    private String defaultCategory(CourseSource src) {
        String def = pullService.cfg(src, "defaultCategory", "general");
        return isBlank(def) ? "general" : def;
    }

    private void maskSecret(CourseSource src) {
        if (!isBlank(src.getAppSecret())) {
            src.setAppSecret("******");
        }
    }

    private void validateJson(String json, String label) {
        if (isBlank(json)) {
            return;
        }
        try {
            objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BusinessException(label + "配置不是合法 JSON");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
