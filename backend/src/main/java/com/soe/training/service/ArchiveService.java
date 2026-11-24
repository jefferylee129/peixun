package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soe.training.common.BusinessException;
import com.soe.training.domain.Certificate;
import com.soe.training.domain.ClassStudent;
import com.soe.training.domain.EvalRecord;
import com.soe.training.domain.EvalTemplate;
import com.soe.training.domain.Exam;
import com.soe.training.domain.ExamRecord;
import com.soe.training.domain.SysDept;
import com.soe.training.domain.SysUser;
import com.soe.training.domain.TrainingClass;
import com.soe.training.mapper.CertificateMapper;
import com.soe.training.mapper.ClassMapper;
import com.soe.training.mapper.ClassStudentMapper;
import com.soe.training.mapper.DeptMapper;
import com.soe.training.mapper.EvalRecordMapper;
import com.soe.training.mapper.EvalTemplateMapper;
import com.soe.training.mapper.ExamMapper;
import com.soe.training.mapper.ExamRecordMapper;
import com.soe.training.mapper.HourRecordMapper;
import com.soe.training.mapper.UserMapper;
import com.soe.training.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一人一档培训档案 + 学时达标预警(对标国企干部培训档案与学时刚性管理):
 * 聚合学时/班次/评估/证书/考试形成个人培训履历; 按年度时间进度对比学时进度分级预警。
 */
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final HourRecordMapper hourRecordMapper;
    private final ClassStudentMapper studentMapper;
    private final ClassMapper classMapper;
    private final EvalRecordMapper evalRecordMapper;
    private final EvalTemplateMapper templateMapper;
    private final CertificateMapper certificateMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamMapper examMapper;
    private final ConfigService configService;
    private final HoursService hoursService;
    private final MessageService messageService;

    /** 个人培训档案(全量聚合; year 为空查全部年度) */
    public Map<String, Object> archive(Long userId, Integer year) {
        SysUser u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        int y = year == null ? LocalDate.now().getYear() : year;
        boolean allYear = year == null;
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> user = new HashMap<>();
        user.put("id", u.getId());
        user.put("username", u.getUsername());
        user.put("nickname", u.getNickname());
        user.put("employeeNo", u.getEmployeeNo());
        user.put("userType", u.getUserType());
        user.put("position", u.getPosition());
        SysDept dept = u.getDeptId() == null ? null : deptMapper.selectById(u.getDeptId());
        user.put("deptName", dept == null ? null : dept.getDeptName());
        result.put("user", user);

        // 学时汇总(按年度)
        BigDecimal total = hourRecordMapper.sumUserHours(userId, y);
        BigDecimal required = hoursService.requiredOf(u.getUserType());
        Map<String, Object> hours = new HashMap<>();
        hours.put("year", y);
        hours.put("total", total.setScale(1, RoundingMode.HALF_UP));
        hours.put("required", required);
        hours.put("reached", total.compareTo(required) >= 0);
        hours.put("percent", total.multiply(new BigDecimal("100"))
                .divide(required, 0, RoundingMode.DOWN).intValue());
        hours.put("bySource", hourRecordMapper.sumBySource(userId, y));
        result.put("hours", hours);

        // 参训班次
        List<Map<String, Object>> classes = new ArrayList<>();
        for (ClassStudent cs : studentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getUserId, userId))) {
            TrainingClass c = classMapper.selectById(cs.getClassId());
            if (c == null) {
                continue;
            }
            if (!allYear && (c.getStartDate() == null || c.getStartDate().getYear() != y)) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("classId", c.getId());
            row.put("className", c.getClassName());
            row.put("classType", c.getClassType());
            row.put("startDate", c.getStartDate());
            row.put("endDate", c.getEndDate());
            row.put("status", c.getStatus());
            row.put("studentStatus", cs.getStatus());
            row.put("totalHours", cs.getTotalHours());
            classes.add(row);
        }
        classes.sort(Comparator.comparing(r -> String.valueOf(r.get("startDate")),
                Comparator.nullsLast(Comparator.reverseOrder())));
        result.put("classes", classes);

        // 评估作答
        List<Map<String, Object>> evals = new ArrayList<>();
        Map<Long, String> tplNames = new HashMap<>();
        for (EvalRecord r : evalRecordMapper.selectList(new LambdaQueryWrapper<EvalRecord>()
                .eq(EvalRecord::getUserId, userId)
                .orderByDesc(EvalRecord::getSubmitTime))) {
            if (!allYear && (r.getSubmitTime() == null || r.getSubmitTime().getYear() != y)) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("classId", r.getClassId());
            TrainingClass c = classMapper.selectById(r.getClassId());
            row.put("className", c == null ? "(班次已删除)" : c.getClassName());
            row.put("templateId", r.getTemplateId());
            String tplName = tplNames.computeIfAbsent(r.getTemplateId(), tid -> {
                EvalTemplate t = templateMapper.selectById(tid);
                return t == null ? null : t.getTemplateName();
            });
            row.put("templateName", tplName);
            row.put("totalScore", r.getTotalScore());
            row.put("submitTime", r.getSubmitTime());
            evals.add(row);
        }
        result.put("evals", evals);

        // 证书
        List<Map<String, Object>> certs = new ArrayList<>();
        for (Certificate c : certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getUserId, userId)
                .orderByDesc(Certificate::getId))) {
            if (!allYear && (c.getIssueDate() == null || c.getIssueDate().getYear() != y)) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", c.getId());
            row.put("certNo", c.getCertNo());
            row.put("classId", c.getClassId());
            TrainingClass tc = classMapper.selectById(c.getClassId());
            row.put("className", tc == null ? null : tc.getClassName());
            row.put("hours", c.getHours());
            row.put("grade", c.getGrade());
            row.put("issueDate", c.getIssueDate());
            certs.add(row);
        }
        result.put("certificates", certs);

        // 考试成绩
        List<Map<String, Object>> exams = new ArrayList<>();
        for (ExamRecord r : examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId)
                .isNotNull(ExamRecord::getSubmitTime)
                .orderByDesc(ExamRecord::getSubmitTime))) {
            if (!allYear && (r.getSubmitTime().getYear() != y)) {
                continue;
            }
            Exam e = examMapper.selectById(r.getExamId());
            if (e == null) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("examId", e.getId());
            row.put("title", e.getTitle());
            row.put("score", r.getScore());
            row.put("passed", r.getPassed());
            row.put("submitTime", r.getSubmitTime());
            exams.add(row);
        }
        result.put("exams", exams);

        // 履历时间线(倒序, 最多 100 条)
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (Map<String, Object> c : classes) {
            timeline.add(Map.of("time", String.valueOf(c.get("startDate")), "type", "class",
                    "title", "参加培训班次「" + c.get("className") + "」",
                    "detail", "认定学时 " + c.get("totalHours")));
        }
        for (Map<String, Object> e : evals) {
            timeline.add(Map.of("time", String.valueOf(e.get("submitTime")), "type", "eval",
                    "title", "完成评估「" + e.get("templateName") + "」",
                    "detail", "班次「" + e.get("className") + "」"));
        }
        for (Map<String, Object> c : certs) {
            timeline.add(Map.of("time", String.valueOf(c.get("issueDate")), "type", "cert",
                    "title", "获颁结业证书 " + c.get("certNo"),
                    "detail", "班次「" + c.get("className") + "」"));
        }
        for (Map<String, Object> e : exams) {
            timeline.add(Map.of("time", String.valueOf(e.get("submitTime")), "type", "exam",
                    "title", "参加考试「" + e.get("title") + "」",
                    "detail", "成绩 " + e.get("score") + " 分" +
                            (Integer.valueOf(1).equals(e.get("passed")) ? "(及格)" : "(不及格)")));
        }
        timeline.sort(Comparator.comparing(m -> (String) m.get("time"), Comparator.reverseOrder()));
        if (timeline.size() > 100) {
            timeline = new ArrayList<>(timeline.subList(0, 100));
        }
        result.put("timeline", timeline);
        return result;
    }

    /**
     * 全员预警计算(不依赖登录上下文, 供定时任务与管理员推送复用)。
     * 返回按 red → orange → yellow 排序的预警行。
     */
    public List<Map<String, Object>> allWarnings(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        List<Map<String, Object>> rows = hoursService.userStats(y, null, null);
        double tp = timeProgress(y);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String level = levelOf(row, tp);
            if (level == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>(row);
            item.put("level", level);
            item.put("timeProgress", Math.round(tp * 100));
            result.add(item);
        }
        Map<String, Integer> severity = Map.of("red", 0, "orange", 1, "yellow", 2);
        result.sort(Comparator.comparing(m -> severity.getOrDefault(String.valueOf(m.get("level")), 9)));
        return result;
    }

    /** 学时达标预警(管理员看全员, 员工看自己); level: yellow橙黄/orange橙/red红 */
    public List<Map<String, Object>> warnings(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        boolean viewer = canViewAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : allWarnings(y)) {
            if (!viewer && !item.get("userId").equals(AuthContext.getUserId())) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    /** 我的预警(供工作台卡片), 未触发返回 null */
    public Map<String, Object> myWarning(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        for (Map<String, Object> row : hoursService.userStats(y, null, null)) {
            if (row.get("userId").equals(AuthContext.getUserId())) {
                String level = levelOf(row, timeProgress(y));
                if (level == null) {
                    return null;
                }
                Map<String, Object> item = new HashMap<>(row);
                item.put("level", level);
                return item;
            }
        }
        return null;
    }

    /** 推送预警消息: 对当前触发预警的人员逐一发站内信, 返回发送条数 */
    @Transactional
    public int pushWarnings(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        int n = 0;
        for (Map<String, Object> w : allWarnings(y)) {
            String level = String.valueOf(w.get("level"));
            String levelText = switch (level) {
                case "red" -> "红色(严重滞后)";
                case "orange" -> "橙色(明显滞后)";
                default -> "黄色(进度偏慢)";
            };
            messageService.send((Long) w.get("userId"), "warn", "年度学时预警提醒",
                    "您的 " + y + " 年度培训学时为 " + w.get("totalHours") + "/" + w.get("required")
                            + ", 预警级别: " + levelText + ", 请合理安排时间参加培训。", null);
            n++;
        }
        return n;
    }

    private String levelOf(Map<String, Object> row, double tp) {
        BigDecimal total = row.get("totalHours") == null ? BigDecimal.ZERO
                : new BigDecimal(row.get("totalHours").toString());
        BigDecimal required = row.get("required") == null ? BigDecimal.ZERO
                : new BigDecimal(row.get("required").toString());
        if (required.compareTo(BigDecimal.ZERO) <= 0 || total.compareTo(required) >= 0) {
            return null;
        }
        double hp = total.divide(required, 4, RoundingMode.DOWN).doubleValue();
        double mid = Double.parseDouble(configService.getValue("warn.ratio.mid", "0.3"));
        double q3 = Double.parseDouble(configService.getValue("warn.ratio.q3", "0.5"));
        double end = Double.parseDouble(configService.getValue("warn.ratio.end", "0.7"));
        if (tp >= 0.9 && hp < end) {
            return "red";
        }
        if (tp >= 0.75 && hp < q3) {
            return "orange";
        }
        if (tp >= 0.5 && hp < mid) {
            return "yellow";
        }
        return null;
    }

    /** 年度时间进度(0~1): 历史年度=1, 未来年度=0 */
    private double timeProgress(int year) {
        LocalDate today = LocalDate.now();
        if (year < today.getYear()) {
            return 1.0;
        }
        if (year > today.getYear()) {
            return 0.0;
        }
        int days = today.lengthOfYear();
        return (double) today.getDayOfYear() / days;
    }

    private boolean canViewAll() {
        var ctx = AuthContext.get();
        return ctx != null && (ctx.isAdmin() || ctx.getPerms().contains("biz:archive:other"));
    }
}
