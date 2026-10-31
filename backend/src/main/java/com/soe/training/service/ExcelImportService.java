package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soe.training.common.BusinessException;
import com.soe.training.common.ExcelImportUtil;
import com.soe.training.common.ImportResult;
import com.soe.training.domain.*;
import com.soe.training.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Excel 批量导入服务(各模块行级校验 + 落库)
 */
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final NeedMapper needMapper;
    private final PlanMapper planMapper;
    private final CourseMapper courseMapper;
    private final TrainerMapper trainerMapper;
    private final ClassMapper classMapper;
    private final ClassStudentMapper classStudentMapper;
    private final HourRecordMapper hourRecordMapper;
    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final RoleMapper roleMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final Map<String, String> NEED_TYPE = Map.of(
            "党性教育", "party", "管理能力", "mgmt", "岗位技能", "skill", "安全合规", "safety", "通用素质", "general");
    private static final Map<String, String> PLAN_TYPE = Map.of(
            "年度", "year", "年度计划", "year", "季度", "quarter", "季度计划", "quarter",
            "月度", "month", "月度计划", "month", "专项", "special", "专项计划", "special");
    private static final Map<String, String> COURSE_TYPE = Map.of(
            "内训", "internal", "外训", "external", "线上", "online", "混合", "mixed");
    private static final Map<String, String> TRAINER_TYPE = Map.of(
            "内部", "internal", "内部讲师", "internal", "外部", "external", "外部专家", "external");
    private static final Map<String, String> USER_TYPE = Map.of(
            "管理干部", "manager", "专业技术人员", "professional", "一般员工", "staff");

    // ---------- 培训需求 ----------

    @Transactional
    public ImportResult importNeeds(List<Map<Integer, String>> rows, LoginUserContext ctx) {
        ImportResult r = base(rows);
        for (int i = 0; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            int no = i + 2;
            try {
                TrainingNeed n = new TrainingNeed();
                String yearS = ExcelImportUtil.str(row, 0);
                n.setYear(yearS == null || yearS.isBlank()
                        ? LocalDate.now().getYear() : Integer.parseInt(yearS.trim().split("\\.")[0]));
                String title = ExcelImportUtil.str(row, 1);
                if (title == null || title.isBlank()) {
                    r.error(no, "需求标题不能为空");
                    continue;
                }
                n.setTitle(title);
                String typeS = ExcelImportUtil.str(row, 2);
                String type = typeS == null ? null : NEED_TYPE.getOrDefault(typeS.trim(), typeS.trim());
                if (type == null || !NEED_TYPE.containsValue(type)) {
                    r.error(no, "需求类别无效: " + typeS + "(可用: 党性教育/管理能力/岗位技能/安全合规/通用素质)");
                    continue;
                }
                n.setNeedType(type);
                n.setContent(ExcelImportUtil.str(row, 3));
                n.setReason(ExcelImportUtil.str(row, 4));
                n.setExpectedTime(ExcelImportUtil.str(row, 5));
                String ps = ExcelImportUtil.str(row, 6);
                if (ps != null && !ps.isBlank()) {
                    n.setParticipants((int) Double.parseDouble(ps));
                }
                Long ownerId = ctx.userId();
                String emp = ExcelImportUtil.str(row, 7);
                if (emp != null && !emp.isBlank()) {
                    SysUser owner = findUser(emp);
                    if (owner == null) {
                        r.error(no, "申报人工号不存在: " + emp);
                        continue;
                    }
                    if (!ctx.admin() && !owner.getId().equals(ctx.userId())) {
                        r.error(no, "无权限为其他人申报(工号 " + emp + ")");
                        continue;
                    }
                    ownerId = owner.getId();
                }
                n.setUserId(ownerId);
                SysUser u = userMapper.selectById(ownerId);
                n.setDeptId(u == null ? null : u.getDeptId());
                n.setStatus("0");
                needMapper.insert(n);
                r.setSuccess(r.getSuccess() + 1);
            } catch (BusinessException e) {
                r.error(no, e.getMessage());
            } catch (Exception e) {
                r.error(no, "解析失败: " + e.getMessage());
            }
        }
        return r;
    }

    // ---------- 培训计划 ----------

    @Transactional
    public ImportResult importPlans(List<Map<Integer, String>> rows) {
        ImportResult r = base(rows);
        for (int i = 0; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            int no = i + 2;
            try {
                TrainingPlan p = new TrainingPlan();
                String name = ExcelImportUtil.str(row, 0);
                if (name == null || name.isBlank()) {
                    r.error(no, "计划名称不能为空");
                    continue;
                }
                p.setPlanName(name);
                String yearS = ExcelImportUtil.str(row, 1);
                if (yearS == null || yearS.isBlank()) {
                    r.error(no, "年度不能为空");
                    continue;
                }
                p.setYear(Integer.parseInt(yearS.trim().split("\\.")[0]));
                String typeS = ExcelImportUtil.str(row, 2);
                String type = typeS == null ? null : PLAN_TYPE.getOrDefault(typeS.trim(), typeS.trim());
                if (type == null || !PLAN_TYPE.containsValue(type)) {
                    r.error(no, "计划类型无效: " + typeS + "(可用: 年度/季度/月度/专项)");
                    continue;
                }
                p.setPlanType(type);
                SysDept dept = findDept(ExcelImportUtil.str(row, 3));
                if (dept == null) {
                    r.error(no, "主办部门不存在: " + ExcelImportUtil.str(row, 3));
                    continue;
                }
                p.setDeptId(dept.getId());
                LocalDate start = ExcelImportUtil.parseDate(ExcelImportUtil.str(row, 4));
                LocalDate end = ExcelImportUtil.parseDate(ExcelImportUtil.str(row, 5));
                if (start == null || end == null) {
                    r.error(no, "开始/结束日期不能为空且格式应为 YYYY-MM-DD");
                    continue;
                }
                p.setStartDate(start);
                p.setEndDate(end);
                p.setTargetUsers(ExcelImportUtil.str(row, 6));
                p.setContent(ExcelImportUtil.str(row, 7));
                String budget = ExcelImportUtil.str(row, 8);
                if (budget != null && !budget.isBlank()) {
                    p.setBudget(new BigDecimal(budget));
                }
                p.setStatus("0");
                planMapper.insert(p);
                r.setSuccess(r.getSuccess() + 1);
            } catch (Exception e) {
                r.error(no, "解析失败: " + e.getMessage());
            }
        }
        return r;
    }

    // ---------- 培训课程 ----------

    @Transactional
    public ImportResult importCourses(List<Map<Integer, String>> rows) {
        ImportResult r = base(rows);
        for (int i = 0; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            int no = i + 2;
            try {
                Course c = new Course();
                String name = ExcelImportUtil.str(row, 1);
                if (name == null || name.isBlank()) {
                    r.error(no, "课程名称不能为空");
                    continue;
                }
                c.setCourseName(name);
                c.setCourseCode(ExcelImportUtil.str(row, 0));
                String catS = ExcelImportUtil.str(row, 2);
                String cat = catS == null ? null : NEED_TYPE.getOrDefault(catS.trim(), catS.trim());
                if (cat == null || !NEED_TYPE.containsValue(cat)) {
                    r.error(no, "课程分类无效: " + catS);
                    continue;
                }
                c.setCategory(cat);
                String typeS = ExcelImportUtil.str(row, 3);
                String type = typeS == null ? null : COURSE_TYPE.getOrDefault(typeS.trim(), typeS.trim());
                if (type == null || !COURSE_TYPE.containsValue(type)) {
                    r.error(no, "课程形式无效: " + typeS + "(可用: 内训/外训/线上)");
                    continue;
                }
                c.setCourseType(type);
                String hours = ExcelImportUtil.str(row, 4);
                if (hours == null || hours.isBlank()) {
                    r.error(no, "标准学时不能为空");
                    continue;
                }
                c.setHours(new BigDecimal(hours));
                c.setLevel(ExcelImportUtil.str(row, 5));
                c.setTargetUsers(ExcelImportUtil.str(row, 6));
                c.setOutline(ExcelImportUtil.str(row, 7));
                String trainerName = ExcelImportUtil.str(row, 8);
                if (trainerName != null && !trainerName.isBlank()) {
                    Trainer t = trainerMapper.selectOne(new LambdaQueryWrapper<Trainer>()
                            .eq(Trainer::getTrainerName, trainerName));
                    if (t == null) {
                        r.error(no, "讲师不存在: " + trainerName);
                        continue;
                    }
                    c.setTrainerId(t.getId());
                }
                Long dup = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                        .eq(Course::getCourseName, name).eq(Course::getCategory, cat));
                if (dup > 0) {
                    r.skip(no, "课程已存在, 跳过: " + name);
                    continue;
                }
                c.setStatus("0");
                courseMapper.insert(c);
                r.setSuccess(r.getSuccess() + 1);
            } catch (Exception e) {
                r.error(no, "解析失败: " + e.getMessage());
            }
        }
        return r;
    }

    // ---------- 培训师资 ----------

    @Transactional
    public ImportResult importTrainers(List<Map<Integer, String>> rows) {
        ImportResult r = base(rows);
        for (int i = 0; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            int no = i + 2;
            try {
                Trainer t = new Trainer();
                String name = ExcelImportUtil.str(row, 0);
                if (name == null || name.isBlank()) {
                    r.error(no, "姓名不能为空");
                    continue;
                }
                t.setTrainerName(name);
                String typeS = ExcelImportUtil.str(row, 1);
                String type = typeS == null ? null : TRAINER_TYPE.getOrDefault(typeS.trim(), typeS.trim());
                if (type == null || !TRAINER_TYPE.containsValue(type)) {
                    r.error(no, "师资类型无效: " + typeS + "(可用: 内部/外部)");
                    continue;
                }
                t.setTrainerType(type);
                t.setGender(gender(ExcelImportUtil.str(row, 2)));
                t.setOrgName(ExcelImportUtil.str(row, 3));
                t.setTitle(ExcelImportUtil.str(row, 4));
                t.setSpecialty(ExcelImportUtil.str(row, 5));
                t.setLevel(ExcelImportUtil.str(row, 6));
                t.setPhone(ExcelImportUtil.str(row, 7));
                t.setEmail(ExcelImportUtil.str(row, 8));
                t.setIntro(ExcelImportUtil.str(row, 9));
                Long dup = trainerMapper.selectCount(new LambdaQueryWrapper<Trainer>()
                        .eq(Trainer::getTrainerName, name).eq(Trainer::getTrainerType, type));
                if (dup > 0) {
                    r.skip(no, "同名同类型师资已存在, 跳过: " + name);
                    continue;
                }
                t.setStatus("0");
                trainerMapper.insert(t);
                r.setSuccess(r.getSuccess() + 1);
            } catch (Exception e) {
                r.error(no, "解析失败: " + e.getMessage());
            }
        }
        return r;
    }

    // ---------- 培训班次 ----------

    @Transactional
    public ImportResult importClasses(List<Map<Integer, String>> rows) {
        ImportResult r = base(rows);
        for (int i = 0; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            int no = i + 2;
            try {
                TrainingClass c = new TrainingClass();
                String name = ExcelImportUtil.str(row, 0);
                if (name == null || name.isBlank()) {
                    r.error(no, "班次名称不能为空");
                    continue;
                }
                c.setClassName(name);
                String typeS = ExcelImportUtil.str(row, 1);
                String type = typeS == null ? null : COURSE_TYPE.getOrDefault(typeS.trim(), typeS.trim());
                if (type == null || !COURSE_TYPE.containsValue(type)) {
                    r.error(no, "班次类型无效: " + typeS + "(可用: 内训/外训/线上/混合)");
                    continue;
                }
                c.setClassType(type);
                SysDept dept = findDept(ExcelImportUtil.str(row, 2));
                if (dept == null) {
                    r.error(no, "主办部门不存在: " + ExcelImportUtil.str(row, 2));
                    continue;
                }
                c.setDeptId(dept.getId());
                LocalDate start = ExcelImportUtil.parseDate(ExcelImportUtil.str(row, 3));
                LocalDate end = ExcelImportUtil.parseDate(ExcelImportUtil.str(row, 4));
                if (start == null || end == null) {
                    r.error(no, "开始/结束日期不能为空且格式应为 YYYY-MM-DD");
                    continue;
                }
                c.setStartDate(start);
                c.setEndDate(end);
                c.setLocation(ExcelImportUtil.str(row, 5));
                String cap = ExcelImportUtil.str(row, 6);
                if (cap != null && !cap.isBlank()) {
                    c.setCapacity((int) Double.parseDouble(cap));
                }
                String budget = ExcelImportUtil.str(row, 7);
                if (budget != null && !budget.isBlank()) {
                    c.setBudget(new BigDecimal(budget));
                }
                c.setTargetUsers(ExcelImportUtil.str(row, 8));
                c.setDescription(ExcelImportUtil.str(row, 9));
                c.setStatus("0");
                classMapper.insert(c);
                r.setSuccess(r.getSuccess() + 1);
            } catch (Exception e) {
                r.error(no, "解析失败: " + e.getMessage());
            }
        }
        return r;
    }

    // ---------- 班级学员(按工号) ----------

    @Transactional
    public ImportResult importClassStudents(Long classId, List<Map<Integer, String>> rows) {
        ImportResult r = base(rows);
        TrainingClass cls = classMapper.selectById(classId);
        if (cls == null) {
            throw new BusinessException("班次不存在");
        }
        if ("3".equals(cls.getStatus()) || "4".equals(cls.getStatus())) {
            throw new BusinessException("班次已结束或已取消, 不能导入学员");
        }
        int current = classStudentMapper.selectByClassId(classId).size();
        for (int i = 0; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            int no = i + 2;
            String emp = ExcelImportUtil.str(row, 0);
            if (emp == null || emp.isBlank()) {
                r.error(no, "工号不能为空");
                continue;
            }
            SysUser u = findUser(emp);
            if (u == null) {
                r.error(no, "工号不存在: " + emp);
                continue;
            }
            if (classStudentMapper.selectOne(classId, u.getId()) != null) {
                r.skip(no, "已在该班次中: " + u.getNickname());
                continue;
            }
            if (cls.getCapacity() != null && current >= cls.getCapacity()) {
                r.error(no, "已超出人数上限(" + cls.getCapacity() + "), 未导入: " + u.getNickname());
                continue;
            }
            ClassStudent s = new ClassStudent();
            s.setClassId(classId);
            s.setUserId(u.getId());
            s.setEnrollTime(java.time.LocalDateTime.now());
            s.setStatus("0");
            s.setTotalHours(BigDecimal.ZERO);
            try {
                classStudentMapper.insert(s);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                r.skip(no, "已在该班次中: " + u.getNickname());
                continue;
            }
            current++;
            r.setSuccess(r.getSuccess() + 1);
        }
        return r;
    }

    // ---------- 学时(补录/外部认定批量导入) ----------

    @Transactional
    public ImportResult importHours(List<Map<Integer, String>> rows) {
        ImportResult r = base(rows);
        for (int i = 0; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            int no = i + 2;
            try {
                String emp = ExcelImportUtil.str(row, 0);
                SysUser u = emp == null ? null : findUser(emp);
                if (u == null) {
                    r.error(no, "工号不存在或为空: " + emp);
                    continue;
                }
                String hoursS = ExcelImportUtil.str(row, 1);
                BigDecimal hours;
                try {
                    hours = new BigDecimal(hoursS == null ? "" : hoursS);
                } catch (Exception e) {
                    hours = BigDecimal.ZERO;
                }
                if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                    r.error(no, "学时必须大于0");
                    continue;
                }
                LocalDate date = ExcelImportUtil.parseDate(ExcelImportUtil.str(row, 2));
                if (date == null) {
                    r.error(no, "学时日期不能为空且格式应为 YYYY-MM-DD");
                    continue;
                }
                String srcS = ExcelImportUtil.str(row, 3);
                int source = switch (srcS == null ? "" : srcS.trim()) {
                    case "手工补录", "2" -> 2;
                    case "外部认定", "3" -> 3;
                    default -> -1;
                };
                if (source < 0) {
                    r.error(no, "来源无效: " + srcS + "(可用: 手工补录/外部认定)");
                    continue;
                }
                HourRecord rec = new HourRecord();
                rec.setUserId(u.getId());
                rec.setHours(hours);
                rec.setRecordDate(date);
                rec.setYear(date.getYear());
                rec.setSource(source);
                rec.setRemark(ExcelImportUtil.str(row, 4));
                String className = ExcelImportUtil.str(row, 5);
                if (className != null && !className.isBlank()) {
                    TrainingClass c = classMapper.selectOne(new LambdaQueryWrapper<TrainingClass>()
                            .eq(TrainingClass::getClassName, className));
                    if (c == null) {
                        r.error(no, "关联班次不存在: " + className);
                        continue;
                    }
                    rec.setClassId(c.getId());
                }
                Long dup = hourRecordMapper.selectCount(new LambdaQueryWrapper<HourRecord>()
                        .eq(HourRecord::getUserId, u.getId())
                        .eq(HourRecord::getRecordDate, date)
                        .eq(HourRecord::getHours, hours)
                        .eq(HourRecord::getSource, source));
                if (dup > 0) {
                    r.skip(no, "同学时记录已存在, 跳过");
                    continue;
                }
                rec.setStatus("0");
                hourRecordMapper.insert(rec);
                r.setSuccess(r.getSuccess() + 1);
            } catch (Exception e) {
                r.error(no, "解析失败: " + e.getMessage());
            }
        }
        return r;
    }

    // ---------- 用户 ----------

    @Transactional
    public ImportResult importUsers(List<Map<Integer, String>> rows) {
        ImportResult r = base(rows);
        for (int i = 0; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            int no = i + 2;
            try {
                String username = ExcelImportUtil.str(row, 0);
                String nickname = ExcelImportUtil.str(row, 1);
                if (username == null || username.isBlank() || nickname == null || nickname.isBlank()) {
                    r.error(no, "登录账号与姓名不能为空");
                    continue;
                }
                Long dup = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));
                if (dup > 0) {
                    r.skip(no, "登录账号已存在, 跳过: " + username);
                    continue;
                }
                SysUser u = new SysUser();
                u.setUsername(username);
                u.setNickname(nickname);
                u.setEmployeeNo(ExcelImportUtil.str(row, 2));
                SysDept dept = findDept(ExcelImportUtil.str(row, 3));
                if (dept != null) {
                    u.setDeptId(dept.getId());
                }
                u.setPosition(ExcelImportUtil.str(row, 4));
                String utS = ExcelImportUtil.str(row, 5);
                u.setUserType(utS == null || utS.isBlank() ? "staff"
                        : USER_TYPE.getOrDefault(utS.trim(), utS.trim()));
                u.setGender(gender(ExcelImportUtil.str(row, 6)));
                u.setPhone(ExcelImportUtil.str(row, 7));
                u.setEmail(ExcelImportUtil.str(row, 8));
                String pwd = ExcelImportUtil.str(row, 9);
                u.setPassword(passwordEncoder.encode(pwd == null || pwd.isBlank() ? "123456" : pwd));
                u.setStatus("0");
                userMapper.insert(u);
                // 默认授予普通员工角色
                SysRole empRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleKey, "employee"));
                if (empRole != null) {
                    userMapper.insertUserRole(u.getId(), empRole.getId());
                }
                r.setSuccess(r.getSuccess() + 1);
            } catch (Exception e) {
                r.error(no, "解析失败: " + e.getMessage());
            }
        }
        return r;
    }

    // ---------- 公共辅助 ----------

    private ImportResult base(List<Map<Integer, String>> rows) {
        ImportResult r = new ImportResult();
        r.setTotal(rows.size());
        return r;
    }

    private String gender(String s) {
        if (s == null || s.isBlank()) {
            return "0";
        }
        return switch (s.trim()) {
            case "女", "1" -> "1";
            default -> "0";
        };
    }

    private SysUser findUser(String noOrUsername) {
        SysUser u = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmployeeNo, noOrUsername));
        if (u == null) {
            u = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, noOrUsername));
        }
        return u;
    }

    private SysDept findDept(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return deptMapper.selectOne(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptName, name.trim()));
    }

    /** 导入方上下文(避免直接依赖安全上下文, 便于测试) */
    public record LoginUserContext(Long userId, boolean admin) {
    }
}
