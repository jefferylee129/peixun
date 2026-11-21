package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.ClassStudent;
import com.soe.training.domain.Exam;
import com.soe.training.domain.ExamRecord;
import com.soe.training.domain.HourRecord;
import com.soe.training.domain.Question;
import com.soe.training.domain.TrainingClass;
import com.soe.training.domain.vo.StudentVo;
import com.soe.training.mapper.ClassMapper;
import com.soe.training.mapper.ClassStudentMapper;
import com.soe.training.mapper.EvalRecordMapper;
import com.soe.training.mapper.ExamMapper;
import com.soe.training.mapper.ExamRecordMapper;
import com.soe.training.mapper.HourRecordMapper;
import com.soe.training.mapper.QuestionMapper;
import com.soe.training.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 在线考试服务(柯氏二级·学习层落地, 对标企业学习平台考试模块):
 * 题库(单选/多选/判断) → 组卷挂班次 → 发布限时作答 → 自动判分 → 及格计学时入台账。
 */
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamMapper examMapper;
    private final QuestionMapper questionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ClassMapper classMapper;
    private final ClassStudentMapper studentMapper;
    private final HourRecordMapper hourRecordMapper;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    // ---------- 题库 ----------

    public PageResult<Question> pageQuestions(int page, int size, String qType, Long courseId, String keyword) {
        Page<Question> p = PageQuery.of(page, size);
        return PageResult.of(questionMapper.selectPage(p, new LambdaQueryWrapper<Question>()
                .eq(qType != null && !qType.isBlank(), Question::getQType, qType)
                .eq(courseId != null, Question::getCourseId, courseId)
                .like(keyword != null && !keyword.isBlank(), Question::getStem, keyword)
                .orderByDesc(Question::getId)));
    }

    @Transactional
    public void saveQuestion(Question q) {
        if (q.getQType() == null || !List.of("single", "multiple", "judge").contains(q.getQType())) {
            throw new BusinessException("无效的题型");
        }
        if (q.getStem() == null || q.getStem().isBlank()) {
            throw new BusinessException("请填写题干");
        }
        if (q.getAnswer() == null || q.getAnswer().isBlank()) {
            throw new BusinessException("请填写正确答案");
        }
        String answer = normalizeAnswer(q.getQType(), q.getAnswer());
        if ("judge".equals(q.getQType())) {
            if (!"对".equals(answer) && !"错".equals(answer)) {
                throw new BusinessException("判断题答案仅允许 对/错");
            }
            q.setOptions("[\"对\",\"错\"]");
        } else {
            if (q.getOptions() == null || q.getOptions().isBlank()) {
                throw new BusinessException("请填写选项");
            }
            List<Map<String, Object>> opts;
            try {
                opts = objectMapper.readValue(q.getOptions(),
                        new TypeReference<List<Map<String, Object>>>() { });
            } catch (Exception e) {
                throw new BusinessException("选项不是合法 JSON 数组");
            }
            if (opts.size() < 2) {
                throw new BusinessException("至少两个选项");
            }
            Set<String> keys = new HashSet<>();
            for (Map<String, Object> o : opts) {
                keys.add(String.valueOf(o.get("key")));
            }
            for (char ch : answer.toCharArray()) {
                if (!keys.contains(String.valueOf(ch))) {
                    throw new BusinessException("答案 " + ch + " 不在选项中");
                }
            }
        }
        q.setAnswer(answer);
        if (q.getStatus() == null) {
            q.setStatus("0");
        }
        if (q.getId() == null) {
            questionMapper.insert(q);
        } else {
            if (questionMapper.selectById(q.getId()) == null) {
                throw new BusinessException("题目不存在");
            }
            questionMapper.updateById(q);
        }
    }

    @Transactional
    public void deleteQuestion(Long id) {
        if (questionMapper.selectById(id) == null) {
            throw new BusinessException("题目不存在");
        }
        for (Exam e : examMapper.selectList(new LambdaQueryWrapper<>())) {
            if (questionIdList(e.getQuestionIds()).contains(id)) {
                throw new BusinessException("题目已被考试「" + e.getTitle() + "」引用, 不能删除(可停用)");
            }
        }
        questionMapper.deleteById(id);
    }

    // ---------- 考试管理 ----------

    public PageResult<Map<String, Object>> pageExams(int page, int size, String status, Long classId, String keyword) {
        Page<Exam> p = PageQuery.of(page, size);
        Page<Exam> result = examMapper.selectPage(p, new LambdaQueryWrapper<Exam>()
                .eq(status != null && !status.isBlank(), Exam::getStatus, status)
                .eq(classId != null, Exam::getClassId, classId)
                .like(keyword != null && !keyword.isBlank(), Exam::getTitle, keyword)
                .orderByDesc(Exam::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Exam e : result.getRecords()) {
            rows.add(examRow(e));
        }
        return PageResult.of(result.getTotal(), rows);
    }

    private Map<String, Object> examRow(Exam e) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", e.getId());
        row.put("title", e.getTitle());
        row.put("classId", e.getClassId());
        row.put("courseId", e.getCourseId());
        row.put("durationMin", e.getDurationMin());
        row.put("passScore", e.getPassScore());
        row.put("totalScore", e.getTotalScore());
        row.put("hourReward", e.getHourReward());
        row.put("questionCount", questionIdList(e.getQuestionIds()).size());
        row.put("startTime", e.getStartTime());
        row.put("endTime", e.getEndTime());
        row.put("status", e.getStatus());
        TrainingClass c = classMapper.selectById(e.getClassId());
        row.put("className", c == null ? null : c.getClassName());
        Long submitted = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, e.getId())
                .isNotNull(ExamRecord::getSubmitTime));
        row.put("submittedCount", submitted == null ? 0 : submitted);
        Long passed = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, e.getId())
                .eq(ExamRecord::getPassed, 1));
        row.put("passedCount", passed == null ? 0 : passed);
        return row;
    }

    @Transactional
    public void saveExam(Exam e) {
        if (e.getTitle() == null || e.getTitle().isBlank()) {
            throw new BusinessException("请填写考试名称");
        }
        TrainingClass c = classMapper.selectById(e.getClassId());
        if (c == null) {
            throw new BusinessException("关联班次不存在");
        }
        if ("3".equals(c.getStatus()) || "4".equals(c.getStatus())) {
            throw new BusinessException("班次已结束或已取消, 不能挂考试");
        }
        if (e.getDurationMin() == null || e.getDurationMin() <= 0) {
            e.setDurationMin(30);
        }
        if (e.getTotalScore() == null || e.getTotalScore().compareTo(BigDecimal.ZERO) <= 0) {
            e.setTotalScore(new BigDecimal("100"));
        }
        if (e.getPassScore() == null || e.getPassScore().compareTo(BigDecimal.ZERO) <= 0) {
            e.setPassScore(new BigDecimal("60"));
        }
        if (e.getPassScore().compareTo(e.getTotalScore()) > 0) {
            throw new BusinessException("及格线不能高于总分");
        }
        if (e.getHourReward() == null) {
            e.setHourReward(BigDecimal.ZERO);
        }
        List<Long> qids = questionIdList(e.getQuestionIds());
        if (qids.isEmpty()) {
            throw new BusinessException("请从题库选题组卷");
        }
        List<Question> qs = questionMapper.selectBatchIds(qids);
        if (qs.size() != qids.size()) {
            throw new BusinessException("存在无效题目, 请重新选题");
        }
        for (Question q : qs) {
            if ("1".equals(q.getStatus())) {
                throw new BusinessException("题目「" + shortStem(q.getStem()) + "」已停用, 请更换");
            }
        }
        e.setQuestionIds(qids.stream().map(String::valueOf).collect(Collectors.joining(",")));
        if (e.getId() == null) {
            e.setStatus("0");
            examMapper.insert(e);
        } else {
            Exam old = examMapper.selectById(e.getId());
            if (old == null) {
                throw new BusinessException("考试不存在");
            }
            if (!"0".equals(old.getStatus())) {
                throw new BusinessException("已发布考试不可编辑, 请新建考试");
            }
            e.setStatus(null);
            examMapper.updateById(e);
        }
    }

    @Transactional
    public void deleteExam(Long id) {
        Exam e = getOrThrow(id);
        if (!"0".equals(e.getStatus())) {
            throw new BusinessException("已发布考试不可删除, 可执行截止");
        }
        examMapper.deleteById(id);
    }

    @Transactional
    public void publish(Long id) {
        Exam e = getOrThrow(id);
        if (!"0".equals(e.getStatus())) {
            throw new BusinessException("仅草稿状态可发布");
        }
        Exam u = new Exam();
        u.setId(id);
        u.setStatus("1");
        u.setStartTime(e.getStartTime() == null ? LocalDateTime.now() : e.getStartTime());
        u.setEndTime(e.getEndTime() == null ? LocalDateTime.now().plusDays(7) : e.getEndTime());
        if (!u.getEndTime().isAfter(u.getStartTime())) {
            throw new BusinessException("截止时间必须晚于开始时间");
        }
        examMapper.updateById(u);
        List<Long> uids = studentMapper.selectByClassId(e.getClassId())
                .stream().map(StudentVo::getUserId).toList();
        messageService.sendBatch(uids, "exam", "新考试已发布",
                "「" + e.getTitle() + "」已发布, 时长 " + e.getDurationMin() + " 分钟, 请于 "
                        + u.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " 前完成。", id);
    }

    @Transactional
    public void close(Long id) {
        Exam e = getOrThrow(id);
        if (!"1".equals(e.getStatus())) {
            throw new BusinessException("仅已发布状态可截止");
        }
        Exam u = new Exam();
        u.setId(id);
        u.setStatus("2");
        examMapper.updateById(u);
    }

    // ---------- 学员作答 ----------

    /** 获取试卷(开始/续答); 超时未交卷自动按已存答案判分 */
    @Transactional
    public Map<String, Object> paper(Long examId) {
        Exam e = getOrThrow(examId);
        Long uid = AuthContext.getUserId();
        boolean member = studentMapper.selectOne(e.getClassId(), uid) != null;
        if (!member && !isAdmin()) {
            throw new BusinessException("您不是该班次学员, 不能参加考试");
        }
        ExamRecord rec = recordOf(examId, uid);
        if (rec == null) {
            if (!"1".equals(e.getStatus())) {
                throw new BusinessException("考试未发布或已截止");
            }
            rec = new ExamRecord();
            rec.setExamId(examId);
            rec.setUserId(uid);
            rec.setStartTime(LocalDateTime.now());
            try {
                examRecordMapper.insert(rec);
            } catch (DuplicateKeyException ex) {
                rec = recordOf(examId, uid);
            }
        }
        if (rec != null && rec.getSubmitTime() != null) {
            throw new BusinessException("已交卷, 不能重复作答");
        }
        LocalDateTime deadline = deadlineOf(e, rec.getStartTime());
        if (LocalDateTime.now().isAfter(deadline)) {
            // 超时未交卷: 按已存答案(未存按0分)自动判分, 防止无限续答
            return gradeAndFinish(e, rec, parseAnswers(rec.getAnswers()), true);
        }
        List<Map<String, Object>> questions = new ArrayList<>();
        for (Question q : questionMapper.selectBatchIds(questionIdList(e.getQuestionIds()))) {
            Map<String, Object> qi = new LinkedHashMap<>();
            qi.put("id", q.getId());
            qi.put("qType", q.getQType());
            qi.put("stem", q.getStem());
            qi.put("options", parseOptions(q));
            questions.add(qi);
        }
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> examInfo = new HashMap<>();
        examInfo.put("id", e.getId());
        examInfo.put("title", e.getTitle());
        examInfo.put("durationMin", e.getDurationMin());
        examInfo.put("totalScore", e.getTotalScore());
        examInfo.put("passScore", e.getPassScore());
        examInfo.put("endTime", e.getEndTime());
        result.put("exam", examInfo);
        result.put("questions", questions);
        result.put("deadline", deadline);
        result.put("remainingSeconds", Duration.between(LocalDateTime.now(), deadline).getSeconds());
        result.put("savedAnswers", parseAnswers(rec.getAnswers()));
        return result;
    }

    @Transactional
    public Map<String, Object> submit(Long examId, Map<String, Object> answers) {
        Exam e = getOrThrow(examId);
        Long uid = AuthContext.getUserId();
        if (!"1".equals(e.getStatus())) {
            throw new BusinessException("考试未发布或已截止");
        }
        if (studentMapper.selectOne(e.getClassId(), uid) == null && !isAdmin()) {
            throw new BusinessException("您不是该班次学员, 不能交卷");
        }
        ExamRecord rec = recordOf(examId, uid);
        if (rec == null) {
            throw new BusinessException("请先获取试卷");
        }
        if (rec.getSubmitTime() != null) {
            throw new BusinessException("已交卷, 不能重复提交");
        }
        if (LocalDateTime.now().isAfter(deadlineOf(e, rec.getStartTime()))) {
            throw new BusinessException("已超过交卷时间");
        }
        return gradeAndFinish(e, rec, answers == null ? Map.of() : answers, false);
    }

    /** 我的考试列表(所在班次的已发布/已截止考试) */
    public List<Map<String, Object>> my() {
        Long uid = AuthContext.getUserId();
        List<ClassStudent> mine = studentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getUserId, uid));
        if (mine.isEmpty()) {
            return List.of();
        }
        Set<Long> classIds = mine.stream().map(ClassStudent::getClassId).collect(Collectors.toSet());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Exam e : examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .in(Exam::getClassId, classIds)
                .ne(Exam::getStatus, "0")
                .orderByDesc(Exam::getId))) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", e.getId());
            row.put("title", e.getTitle());
            row.put("className", className(e.getClassId()));
            row.put("status", e.getStatus());
            row.put("startTime", e.getStartTime());
            row.put("endTime", e.getEndTime());
            row.put("durationMin", e.getDurationMin());
            row.put("totalScore", e.getTotalScore());
            row.put("passScore", e.getPassScore());
            row.put("hourReward", e.getHourReward());
            ExamRecord rec = recordOf(e.getId(), uid);
            row.put("submitted", rec != null && rec.getSubmitTime() != null);
            row.put("score", rec == null ? null : rec.getScore());
            row.put("passed", rec == null ? null : rec.getPassed());
            result.add(row);
        }
        return result;
    }

    /** 考试成绩单(管理员): 班次全员 + 作答情况 */
    public Map<String, Object> recordsOfExam(Long examId) {
        Exam e = getOrThrow(examId);
        Map<String, Object> result = examRow(e);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (StudentVo st : studentMapper.selectByClassId(e.getClassId())) {
            Map<String, Object> row = new HashMap<>();
            row.put("userId", st.getUserId());
            row.put("userName", st.getNickname());
            row.put("employeeNo", st.getEmployeeNo());
            row.put("deptName", st.getDeptName());
            ExamRecord rec = recordOf(examId, st.getUserId());
            row.put("submitted", rec != null && rec.getSubmitTime() != null);
            row.put("score", rec == null ? null : rec.getScore());
            row.put("passed", rec == null ? null : rec.getPassed());
            row.put("submitTime", rec == null ? null : rec.getSubmitTime());
            rows.add(row);
        }
        result.put("rows", rows);
        return result;
    }

    // ---------- 私有辅助 ----------

    private Map<String, Object> gradeAndFinish(Exam e, ExamRecord rec, Map<String, Object> answers, boolean timeout) {
        List<Question> qs = questionMapper.selectBatchIds(questionIdList(e.getQuestionIds()));
        BigDecimal per = qs.isEmpty() ? BigDecimal.ZERO
                : e.getTotalScore().divide(new BigDecimal(qs.size()), 1, RoundingMode.HALF_UP);
        BigDecimal score = BigDecimal.ZERO;
        for (Question q : qs) {
            Object v = answers.get(String.valueOf(q.getId()));
            if (v == null) {
                continue;
            }
            String ua = normalizeAnswer(q.getQType(), v.toString());
            if (ua.equalsIgnoreCase(q.getAnswer().trim())) {
                score = score.add(per);
            }
        }
        if (score.compareTo(e.getTotalScore()) > 0) {
            score = e.getTotalScore();
        }
        boolean passed = score.compareTo(e.getPassScore()) >= 0;
        ExamRecord u = new ExamRecord();
        u.setId(rec.getId());
        try {
            u.setAnswers(objectMapper.writeValueAsString(answers));
        } catch (Exception ignored) {
            // 作答快照失败不影响判分
        }
        u.setScore(score);
        u.setPassed(passed ? 1 : 0);
        u.setSubmitTime(LocalDateTime.now());
        examRecordMapper.updateById(u);
        // 及格计学时(线上学习来源, 直接认定; 同一考试仅计一次)
        BigDecimal rewarded = BigDecimal.ZERO;
        if (passed && e.getHourReward() != null && e.getHourReward().compareTo(BigDecimal.ZERO) > 0) {
            String remark = "考试及格认定: " + e.getTitle();
            Long dup = hourRecordMapper.selectCount(new LambdaQueryWrapper<HourRecord>()
                    .eq(HourRecord::getUserId, rec.getUserId())
                    .eq(HourRecord::getYear, LocalDate.now().getYear())
                    .eq(HourRecord::getSource, 4)
                    .eq(HourRecord::getRemark, remark));
            if (dup == null || dup == 0) {
                HourRecord r = new HourRecord();
                r.setUserId(rec.getUserId());
                r.setYear(LocalDate.now().getYear());
                r.setClassId(e.getClassId());
                r.setCourseId(e.getCourseId());
                r.setSource(4);
                r.setHours(e.getHourReward());
                r.setStatus("1");
                r.setRecordDate(LocalDate.now());
                r.setRemark(remark);
                hourRecordMapper.insert(r);
                rewarded = e.getHourReward();
            }
        }
        messageService.send(rec.getUserId(), "exam", "考试成绩发布",
                "您参加的「" + e.getTitle() + "」成绩 " + score + " 分, "
                        + (passed ? "及格" : "不及格")
                        + (rewarded.compareTo(BigDecimal.ZERO) > 0 ? ", 认定学时 " + rewarded : "")
                        + (timeout ? "(超时自动交卷)" : "") + "。", e.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("passed", passed);
        result.put("hourReward", rewarded);
        result.put("timeout", timeout);
        return result;
    }

    private ExamRecord recordOf(Long examId, Long userId) {
        return examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, userId));
    }

    private LocalDateTime deadlineOf(Exam e, LocalDateTime startTime) {
        LocalDateTime deadline = startTime.plusMinutes(e.getDurationMin());
        if (e.getEndTime() != null && e.getEndTime().isBefore(deadline)) {
            deadline = e.getEndTime();
        }
        return deadline;
    }

    private String normalizeAnswer(String type, String raw) {
        String a = raw == null ? "" : raw.trim();
        if ("multiple".equals(type)) {
            TreeSet<String> set = new TreeSet<>();
            for (char ch : a.toUpperCase().toCharArray()) {
                if (ch >= 'A' && ch <= 'Z') {
                    set.add(String.valueOf(ch));
                }
            }
            return String.join("", set);
        }
        return a;
    }

    private List<Long> questionIdList(String csv) {
        List<Long> ids = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return ids;
        }
        for (String s : csv.split(",")) {
            if (s.isBlank()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
                // 脏数据直接跳过
            }
        }
        return ids;
    }

    private List<Object> parseOptions(Question q) {
        try {
            return objectMapper.readValue(q.getOptions(), new TypeReference<List<Object>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnswers(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String className(Long classId) {
        TrainingClass c = classMapper.selectById(classId);
        return c == null ? null : c.getClassName();
    }

    private boolean isAdmin() {
        com.soe.training.security.LoginUser ctx = AuthContext.get();
        return ctx != null && ctx.isAdmin();
    }

    private String shortStem(String stem) {
        return stem == null ? "" : (stem.length() > 20 ? stem.substring(0, 20) + "…" : stem);
    }

    private Exam getOrThrow(Long id) {
        Exam e = examMapper.selectById(id);
        if (e == null) {
            throw new BusinessException("考试不存在");
        }
        return e;
    }
}
