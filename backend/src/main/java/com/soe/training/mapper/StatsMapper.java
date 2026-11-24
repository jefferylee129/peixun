package com.soe.training.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 统计分析 Mapper(只读聚合查询)
 */
public interface StatsMapper {

    @Select("SELECT COUNT(*) FROM biz_class WHERE del_flag = 0")
    int countClass();

    @Select("SELECT COUNT(*) FROM biz_class WHERE del_flag = 0 AND status IN ('1','2')")
    int countClassOngoing();

    @Select("SELECT COUNT(*) FROM biz_course WHERE del_flag = 0 AND status = '2'")
    int countCoursePublished();

    @Select("SELECT COUNT(*) FROM biz_trainer WHERE del_flag = 0 AND status = '0'")
    int countTrainer();

    @Select("SELECT COUNT(*) FROM biz_plan WHERE del_flag = 0 AND status = '2' AND year = #{year}")
    int countPlanApproved(@Param("year") Integer year);

    @Select("SELECT COUNT(DISTINCT plan_id) FROM biz_class WHERE del_flag = 0 AND status = '3' AND plan_id IS NOT NULL")
    int countPlanFinished();

    @Select("SELECT IFNULL(SUM(hours), 0) FROM biz_hour_record WHERE del_flag = 0 AND status = '1' AND year = #{year}")
    BigDecimal sumYearHours(@Param("year") Integer year);

    @Select("SELECT COUNT(*) FROM sys_user WHERE del_flag = 0 AND status = '0'")
    int countActiveUsers();

    @Select("SELECT COUNT(DISTINCT user_id) FROM biz_hour_record WHERE del_flag = 0 AND status = '1' AND year = #{year}")
    int countTrainees(@Param("year") Integer year);

    @Select("SELECT COUNT(*) FROM biz_hour_record WHERE del_flag = 0 AND status = '0' AND source IN (2, 3)")
    int countPendingHours();

    @Select("SELECT MONTH(record_date) AS month, IFNULL(SUM(hours), 0) AS hours " +
            "FROM biz_hour_record WHERE del_flag = 0 AND status = '1' AND year = #{year} " +
            "GROUP BY MONTH(record_date) ORDER BY month")
    List<Map<String, Object>> monthHours(@Param("year") Integer year);

    @Select("SELECT d.id AS deptId, d.dept_name AS deptName, IFNULL(SUM(r.hours), 0) AS totalHours, " +
            "COUNT(DISTINCT r.user_id) AS traineeCount " +
            "FROM biz_hour_record r " +
            "JOIN sys_user u ON u.id = r.user_id " +
            "JOIN sys_dept d ON d.id = u.dept_id " +
            "WHERE r.del_flag = 0 AND r.status = '1' AND r.year = #{year} " +
            "GROUP BY d.id, d.dept_name ORDER BY totalHours DESC")
    List<Map<String, Object>> deptHours(@Param("year") Integer year);

    @Select("SELECT category, COUNT(*) AS cnt FROM biz_course WHERE del_flag = 0 GROUP BY category")
    List<Map<String, Object>> courseCategory();

    @Select("SELECT status, COUNT(*) AS cnt FROM biz_class WHERE del_flag = 0 GROUP BY status")
    List<Map<String, Object>> classStatus();

    @Select("<script>SELECT status, COUNT(*) AS cnt FROM biz_plan WHERE del_flag = 0 " +
            "<if test='year != null'> AND year = #{year}</if> GROUP BY status</script>")
    List<Map<String, Object>> planStatus(@Param("year") Integer year);

    @Select("<script>SELECT need_type AS needType, COUNT(*) AS cnt FROM biz_need WHERE del_flag = 0 " +
            "<if test='year != null'> AND year = #{year}</if> GROUP BY need_type</script>")
    List<Map<String, Object>> needTypeStats(@Param("year") Integer year);

    /**
     * 员工年度学时汇总(含未参训员工, total 为 0)
     */
    @Select("<script>" +
            "SELECT u.id AS userId, u.nickname AS userName, u.employee_no AS employeeNo, " +
            "u.user_type AS userType, d.dept_name AS deptName, " +
            "IFNULL(h.total, 0) AS totalHours " +
            "FROM sys_user u " +
            "LEFT JOIN sys_dept d ON d.id = u.dept_id " +
            "LEFT JOIN (SELECT user_id, SUM(hours) AS total FROM biz_hour_record " +
            "           WHERE del_flag = 0 AND status = '1' AND year = #{year} GROUP BY user_id) h " +
            "ON h.user_id = u.id " +
            "WHERE u.del_flag = 0 AND u.status = '0' " +
            "<if test='deptId != null'> AND u.dept_id = #{deptId}</if>" +
            "<if test=\"keyword != null and keyword != ''\"> AND (u.nickname LIKE CONCAT('%', #{keyword}, '%') OR u.employee_no LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            " ORDER BY totalHours DESC" +
            "</script>")
    List<Map<String, Object>> userYearHours(@Param("year") Integer year,
                                            @Param("deptId") Long deptId,
                                            @Param("keyword") String keyword);

    /** 年度已批准计划预算合计(万元), 按主办部门 */
    @Select("SELECT IFNULL(dept_id, 0) AS deptId, IFNULL(SUM(budget), 0) AS amount, COUNT(*) AS cnt " +
            "FROM biz_plan WHERE del_flag = 0 AND status = '2' AND year = #{year} GROUP BY dept_id")
    List<Map<String, Object>> planBudgetByDept(@Param("year") Integer year);

    /**
     * 年度班次经费合计(万元), 按主办部门; 取消的班次不计。
     * 班次无独立年份列, 按开班日期归年。
     */
    @Select("SELECT IFNULL(dept_id, 0) AS deptId, IFNULL(SUM(budget), 0) AS amount, COUNT(*) AS cnt " +
            "FROM biz_class WHERE del_flag = 0 AND status <> '4' AND YEAR(start_date) = #{year} GROUP BY dept_id")
    List<Map<String, Object>> classBudgetByDept(@Param("year") Integer year);

    /** 年度班次经费合计(万元), 按关联计划; plan_id 为空的归入计划外(deptId 列复用为 0) */
    @Select("SELECT IFNULL(plan_id, 0) AS planId, IFNULL(SUM(budget), 0) AS amount, COUNT(*) AS cnt " +
            "FROM biz_class WHERE del_flag = 0 AND status <> '4' AND YEAR(start_date) = #{year} GROUP BY plan_id")
    List<Map<String, Object>> classBudgetByPlan(@Param("year") Integer year);
}
