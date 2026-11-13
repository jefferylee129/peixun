package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.Attendance;
import com.soe.training.domain.ClassSchedule;
import com.soe.training.domain.ClassStudent;
import com.soe.training.domain.HourRecord;
import com.soe.training.domain.TrainingClass;
import com.soe.training.domain.vo.AttendanceVo;
import com.soe.training.domain.vo.ClassVo;
import com.soe.training.domain.vo.ScheduleVo;
import com.soe.training.domain.vo.StudentVo;
import com.soe.training.mapper.AttendanceMapper;
import com.soe.training.mapper.ClassMapper;
import com.soe.training.mapper.ClassStudentMapper;
import com.soe.training.mapper.HourRecordMapper;
import com.soe.training.mapper.ScheduleMapper;
import com.soe.training.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 培训班次服务(开班 → 报名 → 排课 → 签到 → 学时汇总)
 */
@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassMapper classMapper;
    private final ClassStudentMapper studentMapper;
    private final ScheduleMapper scheduleMapper;
    private final AttendanceMapper attendanceMapper;
    private final HourRecordMapper hourRecordMapper;
    private final ConfigService configService;

    public PageResult<ClassVo> page(int page, int size, String status, Integer year,
                                    Long deptId, String keyword) {
        Page<ClassVo> p = PageQuery.of(page, size);
        return PageResult.of(classMapper.selectPageVo(p, status, year, deptId, keyword, false, null));
    }

    /** 我报名的班次 */
    public PageResult<ClassVo> myClasses(int page, int size) {
        Long userId = AuthContext.getUserId();
        Page<ClassVo> p = PageQuery.of(page, size);
        return PageResult.of(classMapper.selectMyClasses(p, userId));
    }

    /** 我可报名的班次 */
    public PageResult<ClassVo> enrollable(int page, int size) {
        Long userId = AuthContext.getUserId();
        Page<ClassVo> p = PageQuery.of(page, size);
        return PageResult.of(classMapper.selectPageVo(p, null, null, null, null, true, userId));
    }

    public Map<String, Object> detail(Long id) {
        ClassVo vo = classMapper.selectVoById(id);
        if (vo == null) {
            throw new BusinessException("培训班次不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("classInfo", vo);
        result.put("students", studentMapper.selectByClassId(id));
        result.put("schedules", scheduleMapper.selectByClassId(id));
        return result;
    }

    public List<StudentVo> studentsOf(Long classId) {
        getOrThrow(classId);
        return studentMapper.selectByClassId(classId);
    }

    public List<ScheduleVo> schedulesOf(Long classId) {
        getOrThrow(classId);
        return scheduleMapper.selectByClassId(classId);
    }

    /** 导出用: 不分页拉取(上限5000) */
    public List<ClassVo> listForExport(String status, Integer year, Long deptId, String keyword) {
        Page<ClassVo> p = Page.of(1, 5000, false);
        return classMapper.selectPageVo(p, status, year, deptId, keyword, false, null).getRecords();
    }

    public TrainingClass getClassInfo(Long id) {
        return classMapper.selectById(id);
    }

    @Transactional
    public void save(TrainingClass c) {
        checkDateRange(c.getStartDate(), c.getEndDate());
        if (c.getId() == null) {
            c.setStatus("0");
            classMapper.insert(c);
        } else {
            TrainingClass old = getOrThrow(c.getId());
            if ("2".equals(old.getStatus()) || "3".equals(old.getStatus())) {
                throw new BusinessException("进行中或已结束的班次不可编辑");
            }
            if (c.getCapacity() != null) {
                Long cnt = studentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, c.getId()));
                if (cnt > c.getCapacity()) {
                    throw new BusinessException("计划人数不能少于当前已报名人数(" + cnt + ")");
                }
            }
            c.setStatus(null);
            classMapper.updateById(c);
        }
    }

    @Transactional
    public void delete(Long id) {
        TrainingClass c = getOrThrow(id);
        if (!"0".equals(c.getStatus()) && !"4".equals(c.getStatus())) {
            throw new BusinessException("仅筹备中或已取消的班次可删除");
        }
        classMapper.deleteById(id);
    }

    /** 状态流转: 0筹备中 → 1报名中 → 2进行中 → 3已结束; 0/1 可取消; 0 可直接开始 */
    @Transactional
    public void changeStatus(Long id, String status) {
        TrainingClass c = getOrThrow(id);
        String cur = c.getStatus();
        boolean ok = switch (status) {
            case "1" -> "0".equals(cur);
            // 内派/指令性班次可从筹备中直接开始, 跳过公开报名
            case "2" -> "1".equals(cur) || "0".equals(cur);
            case "3" -> "2".equals(cur);
            case "4" -> "0".equals(cur) || "1".equals(cur);
            default -> false;
        };
        if (!ok) {
            throw new BusinessException("班次当前状态不允许该操作");
        }
        TrainingClass u = new TrainingClass();
        u.setId(id);
        u.setStatus(status);
        classMapper.updateById(u);
        // 结班时: 先自动结束未上课节 → 重算签到学时 → 再把无学时的报名学员标缺勤
        // 顺序不能反, 否则已签到但学时还未生成的学员会被误标缺勤; 已取消(status=2)的课节不动
        if ("3".equals(status)) {
            for (ClassSchedule s : scheduleMapper.selectList(new LambdaQueryWrapper<ClassSchedule>()
                    .eq(ClassSchedule::getClassId, id)
                    .eq(ClassSchedule::getStatus, "0"))) {
                ClassSchedule su = new ClassSchedule();
                su.setId(s.getId());
                su.setStatus("1");
                scheduleMapper.updateById(su);
            }
            recomputeClassHours(id);
            for (StudentVo st : studentMapper.selectByClassId(id)) {
                if ("0".equals(st.getStatus())
                        && (st.getTotalHours() == null
                            || st.getTotalHours().compareTo(BigDecimal.ZERO) == 0)) {
                    ClassStudent su = new ClassStudent();
                    su.setId(st.getId());
                    su.setStatus("3");
                    studentMapper.updateById(su);
                }
            }
        }
    }

    // ---------- 学员管理 ----------

    @Transactional
    public void addStudents(Long classId, List<Long> userIds) {
        TrainingClass c = classMapper.lockById(classId);
        if (c == null || Integer.valueOf(1).equals(c.getDelFlag())) {
            throw new BusinessException("培训班次不存在");
        }
        if ("3".equals(c.getStatus()) || "4".equals(c.getStatus())) {
            throw new BusinessException("班次已结束或已取消, 不能调整学员");
        }
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<StudentVo> existing = studentMapper.selectByClassId(classId);
        Set<Long> existIds = new HashSet<>();
        for (StudentVo s : existing) {
            existIds.add(s.getUserId());
        }
        int current = existIds.size();
        for (Long uid : userIds) {
            if (existIds.contains(uid)) {
                continue;
            }
            if (c.getCapacity() != null && current >= c.getCapacity()) {
                throw new BusinessException("已超出计划人数上限(" + c.getCapacity() + ")");
            }
            ClassStudent s = new ClassStudent();
            s.setClassId(classId);
            s.setUserId(uid);
            s.setEnrollTime(LocalDateTime.now());
            s.setStatus("0");
            s.setTotalHours(BigDecimal.ZERO);
            studentMapper.insert(s);
            current++;
        }
    }

    @Transactional
    public void removeStudent(Long classId, Long userId) {
        TrainingClass c = getOrThrow(classId);
        if ("3".equals(c.getStatus())) {
            throw new BusinessException("班次已结束, 不能移除学员");
        }
        ClassStudent s = studentMapper.selectOne(classId, userId);
        if (s == null) {
            throw new BusinessException("该学员不在本班次中");
        }
        if (s.getTotalHours() != null && s.getTotalHours().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("该学员已产生学时, 不能移除");
        }
        studentMapper.deletePhysical(classId, userId);
        attendanceMapper.deletePhysicalByClass(classId, userId);
    }

    /** 员工自主报名 */
    @Transactional
    public void selfEnroll(Long classId) {
        Long userId = AuthContext.getUserId();
        TrainingClass c = classMapper.lockById(classId);
        if (c == null || Integer.valueOf(1).equals(c.getDelFlag())) {
            throw new BusinessException("培训班次不存在");
        }
        if (!"1".equals(c.getStatus())) {
            throw new BusinessException("班次不在报名中状态");
        }
        if (studentMapper.selectOne(classId, userId) != null) {
            throw new BusinessException("您已报名该班次");
        }
        if (c.getCapacity() != null) {
            Long cnt = studentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, classId));
            if (cnt >= c.getCapacity()) {
                throw new BusinessException("报名人数已满");
            }
        }
        ClassStudent s = new ClassStudent();
        s.setClassId(classId);
        s.setUserId(userId);
        s.setEnrollTime(LocalDateTime.now());
        s.setStatus("0");
        s.setTotalHours(BigDecimal.ZERO);
        studentMapper.insert(s);
    }
    // ---------- 课程安排(课表) ----------

    public void addSchedule(ClassSchedule s) {
        TrainingClass c = getOrThrow(s.getClassId());
        if ("3".equals(c.getStatus()) || "4".equals(c.getStatus())) {
            throw new BusinessException("班次已结束或已取消, 不能排课");
        }
        if (s.getHours() == null || s.getHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("本节学时必须大于0");
        }
        if (s.getStatus() == null) {
            s.setStatus("0");
        }
        scheduleMapper.insert(s);
    }

    public void updateSchedule(Long sid, ClassSchedule s) {
        ClassSchedule old = scheduleMapper.selectById(sid);
        if (old == null) {
            throw new BusinessException("课节不存在");
        }
        if (!"0".equals(old.getStatus())) {
            throw new BusinessException("仅待上课的课节可编辑");
        }
        assertClassEditable(old.getClassId());
        s.setId(sid);
        s.setStatus(null);
        scheduleMapper.updateById(s);
    }

    public void deleteSchedule(Long sid) {
        ClassSchedule old = scheduleMapper.selectById(sid);
        if (old == null) {
            throw new BusinessException("课节不存在");
        }
        if (!"0".equals(old.getStatus())) {
            throw new BusinessException("仅待上课的课节可删除");
        }
        assertClassEditable(old.getClassId());
        scheduleMapper.deleteById(sid);
    }

    /** 已结束/已取消班次的课表不允许再变动, 防止学时被事后篡改 */
    private void assertClassEditable(Long classId) {
        TrainingClass c = classMapper.selectById(classId);
        if (c == null || "3".equals(c.getStatus()) || "4".equals(c.getStatus())) {
            throw new BusinessException("班次已结束或已取消, 不能调整课表");
        }
    }

    /** 结束课节: 按签到与规则折算学时并生成台账 */
    @Transactional
    public void finishSchedule(Long sid) {
        ClassSchedule s = scheduleMapper.selectById(sid);
        if (s == null) {
            throw new BusinessException("课节不存在");
        }
        if ("1".equals(s.getStatus())) {
            throw new BusinessException("该课节已结束");
        }
        if ("2".equals(s.getStatus())) {
            throw new BusinessException("已取消的课节不能结束");
        }
        TrainingClass c = classMapper.selectById(s.getClassId());
        if (c == null || !"2".equals(c.getStatus())) {
            throw new BusinessException("班次不在进行中, 不能结束课节");
        }
        ClassSchedule u = new ClassSchedule();
        u.setId(sid);
        u.setStatus("1");
        scheduleMapper.updateById(u);
        recomputeClassHours(s.getClassId());
    }

    /**
     * 按班次全部已完成课节重新汇总签到学时(幂等, 可重复执行):
     * 正常 = 课程节学时; 迟到 = 节学时 × 迟到系数; 请假/缺勤 = 0
     */
    @Transactional
    public void recomputeClassHours(Long classId) {
        BigDecimal lateFactor = new BigDecimal(configService.getValue("hour.factor.late", "0.5"));
        // 派生数据物理删除, 避免软删行占用 uk_hour_schedule_user 唯一键
        hourRecordMapper.deleteSignRecords(classId);

        List<ScheduleVo> finished = new ArrayList<>();
        for (ScheduleVo s : scheduleMapper.selectByClassId(classId)) {
            if ("1".equals(s.getStatus())) {
                finished.add(s);
            }
        }
        List<StudentVo> students = studentMapper.selectByClassId(classId);
        Map<Long, BigDecimal> totals = new HashMap<>();
        Set<Long> attendedUsers = new HashSet<>();

        for (ScheduleVo s : finished) {
            List<Attendance> atts = attendanceMapper.selectList(new LambdaQueryWrapper<Attendance>()
                    .eq(Attendance::getScheduleId, s.getId()));
            for (Attendance a : atts) {
                BigDecimal h = BigDecimal.ZERO;
                BigDecimal base = s.getHours() == null ? BigDecimal.ZERO : s.getHours();
                if ("normal".equals(a.getSignType())) {
                    h = base;
                } else if ("late".equals(a.getSignType())) {
                    h = base.multiply(lateFactor).setScale(1, RoundingMode.HALF_UP);
                }
                a.setHoursObtained(h);
                attendanceMapper.updateById(a);
                if (h.compareTo(BigDecimal.ZERO) > 0) {
                    HourRecord r = new HourRecord();
                    r.setUserId(a.getUserId());
                    r.setYear(s.getTeachDate() == null ? LocalDateTime.now().getYear() : s.getTeachDate().getYear());
                    r.setClassId(classId);
                    r.setScheduleId(s.getId());
                    r.setCourseId(s.getCourseId());
                    r.setSource(1);
                    r.setHours(h);
                    r.setStatus("1");
                    r.setRecordDate(s.getTeachDate());
                    r.setRemark("签到自动生成");
                    hourRecordMapper.insert(r);
                }
                totals.merge(a.getUserId(), h, BigDecimal::add);
                if ("normal".equals(a.getSignType()) || "late".equals(a.getSignType())) {
                    attendedUsers.add(a.getUserId());
                }
            }
        }
        for (StudentVo st : students) {
            BigDecimal total = totals.getOrDefault(st.getUserId(), BigDecimal.ZERO)
                    .setScale(1, RoundingMode.HALF_UP);
            ClassStudent u = new ClassStudent();
            u.setId(st.getId());
            u.setTotalHours(total);
            if (attendedUsers.contains(st.getUserId())) {
                u.setStatus("2");
            } else if ("2".equals(st.getStatus())) {
                // 补签/重算后未再参训的学员回退为已报名; 缺勤(3)不回退
                u.setStatus("0");
            }
            studentMapper.updateById(u);
        }
    }

    // ---------- 签到 ----------

    public List<AttendanceVo> attendanceOf(Long scheduleId) {
        return attendanceMapper.selectByScheduleId(scheduleId);
    }

    @Transactional
    public void sign(Long sid, Long userId, String signType) {
        ClassSchedule s = scheduleMapper.selectById(sid);
        if (s == null) {
            throw new BusinessException("课节不存在");
        }
        if ("2".equals(s.getStatus())) {
            throw new BusinessException("已取消的课节不能签到");
        }
        // 仅进行中班次可签到(含对已结束课节的补签); 筹备/报名/结束/取消均不可
        TrainingClass c = classMapper.selectById(s.getClassId());
        if (c == null || !"2".equals(c.getStatus())) {
            throw new BusinessException("班次未在进行中, 不能签到");
        }
        if (!"normal".equals(signType) && !"late".equals(signType)
                && !"leave".equals(signType) && !"absent".equals(signType)) {
            throw new BusinessException("无效的签到类型");
        }
        if (studentMapper.selectOne(s.getClassId(), userId) == null) {
            // 签到自动入班同样受人数上限约束, 防止绕过报名限制; 行锁防并发超卖
            TrainingClass cls = classMapper.lockById(s.getClassId());
            if (cls == null || Integer.valueOf(1).equals(cls.getDelFlag())) {
                throw new BusinessException("培训班次不存在");
            }
            if (cls.getCapacity() != null) {
                Long cnt = studentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, s.getClassId()));
                if (cnt >= cls.getCapacity()) {
                    throw new BusinessException("报名人数已满, 不能通过签到加入");
                }
            }
            ClassStudent ns = new ClassStudent();
            ns.setClassId(s.getClassId());
            ns.setUserId(userId);
            ns.setEnrollTime(LocalDateTime.now());
            ns.setStatus("0");
            ns.setTotalHours(BigDecimal.ZERO);
            try {
                studentMapper.insert(ns);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发报名下已存在, 继续签到更新
            }
        }
        Attendance a = attendanceMapper.selectByScheduleAndUser(sid, userId);
        if (a == null) {
            a = new Attendance();
            a.setScheduleId(sid);
            a.setClassId(s.getClassId());
            a.setUserId(userId);
            a.setSignType(signType);
            a.setSignTime(LocalDateTime.now());
            try {
                attendanceMapper.insert(a);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发重复签到: 改为更新已有记录, 防止重复计学时
                Attendance exist = attendanceMapper.selectByScheduleAndUser(sid, userId);
                if (exist != null) {
                    exist.setSignType(signType);
                    exist.setSignTime(LocalDateTime.now());
                    attendanceMapper.updateById(exist);
                }
            }
        } else {
            a.setSignType(signType);
            a.setSignTime(LocalDateTime.now());
            attendanceMapper.updateById(a);
        }
        // 对已结束课节补签后立即重算学时, 保证台账及时反映补签结果
        if ("1".equals(s.getStatus())) {
            recomputeClassHours(s.getClassId());
        }
    }

    private TrainingClass getOrThrow(Long id) {
        TrainingClass c = classMapper.selectById(id);
        if (c == null) {
            throw new BusinessException("培训班次不存在");
        }
        return c;
    }

    private void checkDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
    }
}
