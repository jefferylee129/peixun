package com.soe.training.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soe.training.common.ExcelExportUtil;
import com.soe.training.common.ExcelImportUtil;
import com.soe.training.common.ExcelTemplates;
import com.soe.training.common.ImportResult;
import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.NeedCollection;
import com.soe.training.domain.SysRole;
import com.soe.training.domain.SysUser;
import com.soe.training.domain.TrainingClass;
import com.soe.training.domain.vo.*;
import com.soe.training.mapper.RoleMapper;
import com.soe.training.mapper.UserMapper;
import com.soe.training.security.AuthContext;
import com.soe.training.security.LoginUser;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 培训管理各模块 Excel 导入 / 导出 / 模板下载
 */
@RestController
@RequestMapping("/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final NeedService needService;
    private final PlanService planService;
    private final CourseService courseService;
    private final TrainerService trainerService;
    private final ClassService classService;
    private final HoursService hoursService;
    private final EvalService evalService;
    private final UserService userService;
    private final ExcelImportService excelImportService;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final ArchiveService archiveService;

    /**
     * 一人一档导出(巡察/干部任免纸质归档用): 基本信息、年度学时、参训班次、证书与考试。
     * 权限: 导出本人, 或具备查他人档案权限。
     */
    @OperLog(module = "培训档案", operation = "导出一人一档")
    @GetMapping("/archive")
    public void exportArchive(@RequestParam Long userId,
                              @RequestParam(required = false) Integer year,
                              HttpServletResponse response) throws IOException {
        LoginUser ctx = AuthContext.get();
        if (ctx == null || (!ctx.getUserId().equals(userId) && !ctx.hasPerm("biz:archive:other"))) {
            throw new com.soe.training.common.BusinessException(403, "仅可导出本人培训档案");
        }
        Map<String, Object> a = archiveService.archive(userId, year);
        int exportYear = year == null ? java.time.LocalDate.now().getYear() : year;

        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) a.get("user");
        @SuppressWarnings("unchecked")
        Map<String, Object> hours = (Map<String, Object>) a.get("hours");
        List<List<Object>> base = new ArrayList<>();
        base.add(row("姓名", user.get("nickname")));
        base.add(row("工号", user.get("employeeNo")));
        base.add(row("部门", user.get("deptName")));
        base.add(row("人员类型", cn(USER_TYPE_CN, user.get("userType") == null ? null : String.valueOf(user.get("userType")))));
        base.add(row("岗位", user.get("position")));
        base.add(row("统计年度", exportYear));
        base.add(row("年度已认定学时", hours.get("total")));
        base.add(row("年度学时要求", hours.get("required")));
        base.add(row("达标情况", Boolean.TRUE.equals(hours.get("reached")) ? "已达标" : "未达标"));

        List<List<Object>> classRows = new ArrayList<>();
        for (Map<String, Object> c : castList(a.get("classes"))) {
            classRows.add(List.of(nvl(c.get("className")), nvl(c.get("startDate")), nvl(c.get("endDate")),
                    nvl(c.get("totalHours")), cn(STUDENT_STATUS_CN, c.get("studentStatus") == null ? null : String.valueOf(c.get("studentStatus")))));
        }
        List<List<Object>> certRows = new ArrayList<>();
        for (Map<String, Object> c : castList(a.get("certificates"))) {
            certRows.add(List.of(nvl(c.get("certNo")), nvl(c.get("className")), nvl(c.get("hours")),
                    nvl(c.get("grade")), nvl(c.get("issueDate"))));
        }
        List<List<Object>> examRows = new ArrayList<>();
        for (Map<String, Object> e : castList(a.get("exams"))) {
            examRows.add(List.of(nvl(e.get("title")), nvl(e.get("score")),
                    Integer.valueOf(1).equals(e.get("passed")) ? "及格" : "不及格", nvl(e.get("submitTime"))));
        }
        ExcelExportUtil.writeMulti(response, "培训档案-" + nvl(user.get("nickname")) + "-" + exportYear, List.of(
                new ExcelExportUtil.SheetSpec("基本信息", List.of("项目", "内容"), base),
                new ExcelExportUtil.SheetSpec("参训班次",
                        List.of("班次", "开始日期", "结束日期", "认定学时", "参训状态"), classRows),
                new ExcelExportUtil.SheetSpec("结业证书",
                        List.of("证书编号", "班次", "学时", "结论", "发证日期"), certRows),
                new ExcelExportUtil.SheetSpec("考试成绩",
                        List.of("考试", "成绩", "结论", "交卷时间"), examRows)));
    }

    private List<Object> row(String k, Object v) {
        List<Object> r = new ArrayList<>();
        r.add(k);
        r.add(v == null ? "" : v);
        return r;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object o) {
        return o instanceof List ? (List<Map<String, Object>>) o : List.of();
    }

    private static final Map<String, String> NEED_TYPE_CN = Map.of(
            "party", "党性教育", "mgmt", "管理能力", "skill", "岗位技能", "safety", "安全合规", "general", "通用素质");
    private static final Map<String, String> PLAN_TYPE_CN = Map.of(
            "year", "年度", "quarter", "季度", "month", "月度", "special", "专项");
    private static final Map<String, String> COURSE_TYPE_CN = Map.of(
            "internal", "内训", "external", "外训", "online", "线上", "mixed", "混合");
    private static final Map<String, String> NEED_STATUS_CN = Map.of(
            "0", "草稿", "1", "待审核", "2", "已采纳", "3", "已驳回");
    private static final Map<String, String> PLAN_STATUS_CN = Map.of(
            "0", "草稿", "1", "待审批", "2", "已批准", "3", "已驳回");
    private static final Map<String, String> COURSE_STATUS_CN = Map.of(
            "0", "草稿", "1", "待审核", "2", "已发布", "3", "已下架");
    private static final Map<String, String> CLASS_STATUS_CN = Map.of(
            "0", "筹备中", "1", "报名中", "2", "进行中", "3", "已结束", "4", "已取消");
    private static final Map<String, String> HOUR_SOURCE_CN = Map.of(
            "1", "签到自动", "2", "手工补录", "3", "外部认定", "4", "线上学习");
    private static final Map<String, String> HOUR_STATUS_CN = Map.of(
            "0", "待认定", "1", "已认定", "2", "已驳回");
    private static final Map<String, String> USER_TYPE_CN = Map.of(
            "manager", "管理干部", "professional", "专业技术人员", "staff", "一般员工");
    private static final Map<String, String> STUDENT_STATUS_CN = Map.of(
            "0", "已报名", "2", "已完成", "3", "缺勤");
    private static final Map<String, String> SCHEDULE_STATUS_CN = Map.of(
            "0", "待上课", "1", "已完成", "2", "已取消");

    // ==================== 模板下载 ====================

    @GetMapping("/template/{type}")
    public void template(@PathVariable String type, HttpServletResponse response) throws IOException {
        ExcelTemplates.Template t = ExcelTemplates.get(type);
        ExcelExportUtil.write(response, "导入模板-" + type, "导入模板", t.getHeaders(), t.getExample());
    }

    // ==================== 培训需求 ====================

    @RequirePerm("biz:need:list")
    @GetMapping("/need/export")
    public void needExport(HttpServletResponse response,
                           @RequestParam(required = false) Integer year,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String needType,
                           @RequestParam(required = false) Long deptId,
                           @RequestParam(required = false) String keyword) throws IOException {
        List<NeedVo> list = needService.listForExport(year, status, needType, deptId, keyword);
        List<List<Object>> rows = new ArrayList<>();
        for (NeedVo n : list) {
            rows.add(List.of(n.getYear(), n.getTitle(), cn(NEED_TYPE_CN, n.getNeedType()),
                    n.getDeptName(), n.getUserName(), nvl(n.getContent()), nvl(n.getExpectedTime()),
                    n.getParticipants() == null ? "" : n.getParticipants(),
                    cn(NEED_STATUS_CN, n.getStatus()), nvl(n.getAuditRemark())));
        }
        ExcelExportUtil.write(response, "培训需求台账", "培训需求",
                List.of("年度", "需求标题", "类别", "申报部门", "申报人", "需求描述", "期望时间", "建议人数", "状态", "审核意见"), rows);
    }

    @GetMapping("/need-my/export")
    public void needMyExport(HttpServletResponse response,
                             @RequestParam(required = false) Integer year,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String keyword) throws IOException {
        List<NeedVo> list = needService.listMyForExport(year, status, keyword);
        List<List<Object>> rows = new ArrayList<>();
        for (NeedVo n : list) {
            rows.add(List.of(n.getYear(), n.getTitle(), cn(NEED_TYPE_CN, n.getNeedType()),
                    nvl(n.getContent()), nvl(n.getExpectedTime()),
                    n.getParticipants() == null ? "" : n.getParticipants(),
                    cn(NEED_STATUS_CN, n.getStatus()), nvl(n.getAuditRemark())));
        }
        ExcelExportUtil.write(response, "我的培训需求", "我的需求",
                List.of("年度", "需求标题", "类别", "需求描述", "期望时间", "建议人数", "状态", "审核意见"), rows);
    }

    @OperLog(module = "培训需求", operation = "Excel导入需求")
    @PostMapping("/need/import")
    public Result<ImportResult> needImport(@RequestParam("file") MultipartFile file) throws IOException {
        LoginUser ctx = AuthContext.get();
        return Result.ok(excelImportService.importNeeds(ExcelImportUtil.read(file),
                new ExcelImportService.LoginUserContext(ctx.getUserId(), ctx.isAdmin())));
    }

    // ==================== 培训计划 ====================

    @RequirePerm("biz:plan:list")
    @GetMapping("/plan/export")
    public void planExport(HttpServletResponse response,
                           @RequestParam(required = false) Integer year,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String planType,
                           @RequestParam(required = false) Long deptId,
                           @RequestParam(required = false) String keyword) throws IOException {
        List<PlanVo> list = planService.listForExport(year, status, planType, deptId, keyword);
        List<List<Object>> rows = new ArrayList<>();
        for (PlanVo p : list) {
            rows.add(List.of(p.getPlanName(), p.getYear(), cn(PLAN_TYPE_CN, p.getPlanType()),
                    nvl(p.getDeptName()), nvl(p.getStartDate()), nvl(p.getEndDate()),
                    nvl(p.getTargetUsers()), nvl(p.getContent()),
                    p.getBudget() == null ? "" : p.getBudget(), cn(PLAN_STATUS_CN, p.getStatus())));
        }
        ExcelExportUtil.write(response, "培训计划台账", "培训计划",
                List.of("计划名称", "年度", "类型", "主办部门", "开始日期", "结束日期", "培训对象", "计划内容", "预算(万元)", "状态"), rows);
    }

    @OperLog(module = "培训计划", operation = "Excel导入计划")
    @RequirePerm("biz:plan:add")
    @PostMapping("/plan/import")
    public Result<ImportResult> planImport(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(excelImportService.importPlans(ExcelImportUtil.read(file)));
    }

    // ==================== 培训课程 ====================

    @RequirePerm("biz:course:list")
    @GetMapping("/course/export")
    public void courseExport(HttpServletResponse response,
                             @RequestParam(required = false) String category,
                             @RequestParam(required = false) String courseType,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String keyword) throws IOException {
        List<CourseVo> list = courseService.listForExport(category, courseType, status, keyword);
        List<List<Object>> rows = new ArrayList<>();
        for (CourseVo c : list) {
            rows.add(List.of(nvl(c.getCourseCode()), c.getCourseName(), cn(NEED_TYPE_CN, c.getCategory()),
                    cn(COURSE_TYPE_CN, c.getCourseType()), c.getHours(), nvl(c.getLevel()),
                    nvl(c.getTargetUsers()), nvl(c.getTrainerName()), cn(COURSE_STATUS_CN, c.getStatus())));
        }
        ExcelExportUtil.write(response, "培训课程库", "培训课程",
                List.of("课程编码", "课程名称", "分类", "形式", "标准学时", "层级", "培训对象", "主讲讲师", "状态"), rows);
    }

    @OperLog(module = "培训课程", operation = "Excel导入课程")
    @RequirePerm("biz:course:add")
    @PostMapping("/course/import")
    public Result<ImportResult> courseImport(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(excelImportService.importCourses(ExcelImportUtil.read(file)));
    }

    // ==================== 培训师资 ====================

    @RequirePerm("biz:trainer:list")
    @GetMapping("/trainer/export")
    public void trainerExport(HttpServletResponse response,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String trainerType,
                              @RequestParam(required = false) String status) throws IOException {
        List<com.soe.training.domain.Trainer> list = trainerService.listForExport(keyword, trainerType, status);
        List<List<Object>> rows = new ArrayList<>();
        for (com.soe.training.domain.Trainer t : list) {
            rows.add(List.of(t.getTrainerName(),
                    "internal".equals(t.getTrainerType()) ? "内部讲师" : "外部专家",
                    "1".equals(t.getGender()) ? "女" : "男", nvl(t.getOrgName()), nvl(t.getTitle()),
                    nvl(t.getSpecialty()), nvl(t.getLevel()), nvl(t.getPhone()), nvl(t.getEmail()),
                    "0".equals(t.getStatus()) ? "启用" : "停用"));
        }
        ExcelExportUtil.write(response, "培训师资台账", "培训师资",
                List.of("姓名", "类型", "性别", "所在单位", "职称", "专业领域", "讲师等级", "电话", "邮箱", "状态"), rows);
    }

    @OperLog(module = "培训师资", operation = "Excel导入师资")
    @RequirePerm("biz:trainer:add")
    @PostMapping("/trainer/import")
    public Result<ImportResult> trainerImport(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(excelImportService.importTrainers(ExcelImportUtil.read(file)));
    }

    // ==================== 培训班次 ====================

    @RequirePerm("biz:class:list")
    @GetMapping("/class/export")
    public void classExport(HttpServletResponse response,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) Integer year,
                            @RequestParam(required = false) Long deptId,
                            @RequestParam(required = false) String keyword) throws IOException {
        List<ClassVo> list = classService.listForExport(status, year, deptId, keyword);
        List<List<Object>> rows = new ArrayList<>();
        for (ClassVo c : list) {
            rows.add(List.of(c.getClassName(), cn(COURSE_TYPE_CN, c.getClassType()), nvl(c.getDeptName()),
                    nvl(c.getPlanName()), nvl(c.getStartDate()), nvl(c.getEndDate()), nvl(c.getLocation()),
                    c.getStudentCount() == null ? 0 : c.getStudentCount(),
                    c.getCapacity() == null ? "" : c.getCapacity(),
                    c.getBudget() == null ? "" : c.getBudget(), cn(CLASS_STATUS_CN, c.getStatus())));
        }
        ExcelExportUtil.write(response, "培训班次台账", "培训班次",
                List.of("班次名称", "类型", "主办部门", "关联计划", "开始日期", "结束日期", "地点", "报名人数", "人数上限", "经费(万元)", "状态"), rows);
    }

    @OperLog(module = "培训班次", operation = "Excel导入班次")
    @RequirePerm("biz:class:add")
    @PostMapping("/class/import")
    public Result<ImportResult> classImport(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(excelImportService.importClasses(ExcelImportUtil.read(file)));
    }

    @RequirePerm("biz:class:list")
    @GetMapping("/class/{id}/students/export")
    public void studentsExport(@PathVariable Long id, HttpServletResponse response) throws IOException {
        List<StudentVo> list = classService.studentsOf(id);
        TrainingClass c = classService.getClassInfo(id);
        List<List<Object>> rows = new ArrayList<>();
        for (StudentVo s : list) {
            rows.add(List.of(nvl(s.getEmployeeNo()), nvl(s.getNickname()), nvl(s.getDeptName()),
                    nvl(s.getEnrollTime()), cn(STUDENT_STATUS_CN, s.getStatus()),
                    s.getTotalHours() == null ? 0 : s.getTotalHours()));
        }
        ExcelExportUtil.write(response, "学员名单-" + c.getClassName(), "学员名单",
                List.of("工号", "姓名", "部门", "报名时间", "参训状态", "累计学时"), rows);
    }

    @OperLog(module = "培训班次", operation = "Excel导入学员")
    @RequirePerm("biz:class:edit")
    @PostMapping("/class/{id}/students/import")
    public Result<ImportResult> studentsImport(@PathVariable Long id,
                                               @RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(excelImportService.importClassStudents(id, ExcelImportUtil.read(file)));
    }

    @RequirePerm("biz:class:list")
    @GetMapping("/class/{id}/schedules/export")
    public void schedulesExport(@PathVariable Long id, HttpServletResponse response) throws IOException {
        List<ScheduleVo> list = classService.schedulesOf(id);
        TrainingClass c = classService.getClassInfo(id);
        List<List<Object>> rows = new ArrayList<>();
        for (ScheduleVo s : list) {
            rows.add(List.of(nvl(s.getTeachDate()),
                    s.getStartTime() == null ? "" : s.getStartTime().toString(),
                    s.getEndTime() == null ? "" : s.getEndTime().toString(),
                    nvl(s.getCourseName()), nvl(s.getTrainerName()),
                    s.getHours() == null ? "" : s.getHours(), nvl(s.getLocation()),
                    cn(SCHEDULE_STATUS_CN, s.getStatus())));
        }
        ExcelExportUtil.write(response, "课程安排-" + c.getClassName(), "课程安排",
                List.of("日期", "开始时间", "结束时间", "课程", "讲师", "学时", "地点", "状态"), rows);
    }

    // ==================== 学时台账与达标统计 ====================

    @RequirePerm("biz:hours:list")
    @GetMapping("/hours/export")
    public void hoursExport(HttpServletResponse response,
                            @RequestParam(required = false) Integer year,
                            @RequestParam(required = false) Long userId,
                            @RequestParam(required = false) Long deptId,
                            @RequestParam(required = false) Integer source,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String keyword) throws IOException {
        List<HourRecordVo> list = hoursService.listForExport(year, userId, deptId, source, status, keyword);
        List<List<Object>> rows = new ArrayList<>();
        for (HourRecordVo r : list) {
            rows.add(List.of(nvl(r.getEmployeeNo()), nvl(r.getUserName()), nvl(r.getDeptName()),
                    nvl(r.getClassName()), nvl(r.getCourseName()), cn(HOUR_SOURCE_CN, String.valueOf(r.getSource())),
                    r.getHours(), nvl(r.getRecordDate()), cn(HOUR_STATUS_CN, r.getStatus()), nvl(r.getRemark())));
        }
        ExcelExportUtil.write(response, "学时台账", "学时台账",
                List.of("工号", "姓名", "部门", "班次", "课程", "来源", "学时", "学时日期", "状态", "备注"), rows);
    }

    @OperLog(module = "培训学时", operation = "Excel导入学时")
    @RequirePerm("biz:hours:add")
    @PostMapping("/hours/import")
    public Result<ImportResult> hoursImport(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(excelImportService.importHours(ExcelImportUtil.read(file)));
    }

    @RequirePerm("biz:hours:list")
    @GetMapping("/hours-stats/export")
    public void hoursStatsExport(HttpServletResponse response,
                                 @RequestParam(required = false) Integer year,
                                 @RequestParam(required = false) Long deptId,
                                 @RequestParam(required = false) String keyword) throws IOException {
        List<Map<String, Object>> rows0 = hoursService.userStats(year, deptId, keyword);
        List<String> headers = List.of("姓名", "工号", "部门", "人员类型", "年度要求", "累计学时", "是否达标");
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> r : rows0) {
            rows.add(List.of(nvl(r.get("userName")), nvl(r.get("employeeNo")), nvl(r.get("deptName")),
                    cn(USER_TYPE_CN, str(r.get("userType"))), nvl(r.get("required")), nvl(r.get("totalHours")),
                    Boolean.TRUE.equals(r.get("reached")) ? "已达标" : "未达标"));
        }
        ExcelExportUtil.write(response, "学时达标统计" + (year == null ? "" : year), "达标统计", headers, rows);
    }

    // ==================== 培训评估 ====================

    @RequirePerm("biz:eval:list")
    @GetMapping("/eval/export")
    public void evalExport(@RequestParam Long classId, @RequestParam Long templateId,
                           HttpServletResponse response) throws IOException {
        EvalSummaryVo s = evalService.summary(classId, templateId);
        String className = classService.getClassInfo(classId).getClassName();

        List<List<Object>> sumRows = new ArrayList<>();
        sumRows.add(List.of("班次", className));
        sumRows.add(List.of("评估模板", s.getTemplate().getTemplateName()));
        sumRows.add(List.of("参评人数", s.getEvalCount()));
        sumRows.add(List.of("总体平均分", s.getOverallAvg()));
        for (Map<String, Object> item : s.getItems()) {
            if ("score".equals(item.get("type"))) {
                BigDecimal avg = s.getItemAvg().get(String.valueOf(item.get("key")));
                sumRows.add(List.of("条目均分 - " + item.get("title"),
                        avg == null ? 0 : avg));
            }
        }
        for (Map<String, String> a : s.getTextAnswers()) {
            sumRows.add(List.of("开放题 - " + a.get("title") + " (" + a.get("user") + ")", a.get("value")));
        }

        List<String> detailHeaders = new ArrayList<>(List.of("姓名", "部门", "提交时间"));
        for (Map<String, Object> item : s.getItems()) {
            detailHeaders.add(String.valueOf(item.get("title")) + ("score".equals(item.get("type")) ? "(1-5分)" : ""));
        }
        detailHeaders.add("总体评语");
        List<List<Object>> detailRows = new ArrayList<>();
        for (EvalRecordVo r : evalService.records(classId, templateId)) {
            Map<String, Object> scores = parseScores(r.getScores());
            List<Object> row = new ArrayList<>();
            row.add(nvl(r.getNickname()));
            row.add(nvl(r.getDeptName()));
            row.add(nvl(r.getSubmitTime()));
            for (Map<String, Object> item : s.getItems()) {
                Object v = scores.get(String.valueOf(item.get("key")));
                row.add(v == null ? "" : v);
            }
            row.add(nvl(r.getComment()));
            detailRows.add(row);
        }
        ExcelExportUtil.writeMulti(response, "培训评估-" + className, List.of(
                new ExcelExportUtil.SheetSpec("评估汇总", List.of("汇总项", "内容"), sumRows),
                new ExcelExportUtil.SheetSpec("评估明细", detailHeaders, detailRows)));
    }

    // ==================== 用户(系统管理) ====================

    @RequirePerm("system:user:list")
    @GetMapping("/user/export")
    public void userExport(HttpServletResponse response,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long deptId,
                           @RequestParam(required = false) String status) throws IOException {
        List<Map<String, Object>> list = userService.listForExport(keyword, deptId, status);
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> u : list) {
            rows.add(List.of(nvl(u.get("username")), nvl(u.get("nickname")), nvl(u.get("employeeNo")),
                    nvl(u.get("deptName")), nvl(u.get("position")), cn(USER_TYPE_CN, str(u.get("userType"))),
                    "1".equals(u.get("gender")) ? "女" : "男", nvl(u.get("phone")), nvl(u.get("email")),
                    "0".equals(u.get("status")) ? "正常" : "停用"));
        }
        ExcelExportUtil.write(response, "用户台账", "用户",
                List.of("登录账号", "姓名", "工号", "部门", "岗位", "人员类型", "性别", "手机", "邮箱", "状态"), rows);
    }

    @OperLog(module = "用户管理", operation = "Excel导入用户")
    @RequirePerm("system:user:add")
    @PostMapping("/user/import")
    public Result<ImportResult> userImport(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(excelImportService.importUsers(ExcelImportUtil.read(file)));
    }

    // ==================== 辅助 ====================

    private String cn(Map<String, String> map, String key) {
        return key == null ? "" : map.getOrDefault(key, key);
    }

    private String nvl(Object v) {
        return v == null ? "" : v.toString();
    }

    private String str(Object v) {
        return v == null ? null : v.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseScores(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
