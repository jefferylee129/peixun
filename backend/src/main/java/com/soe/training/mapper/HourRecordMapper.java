package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.domain.HourRecord;
import com.soe.training.domain.vo.HourRecordVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface HourRecordMapper extends BaseMapper<HourRecord> {

    @Select("SELECT IFNULL(SUM(hours), 0) FROM biz_hour_record " +
            "WHERE del_flag = 0 AND status = '1' AND user_id = #{userId} AND year = #{year}")
    BigDecimal sumUserHours(@Param("userId") Long userId, @Param("year") Integer year);

    /** 按来源汇总年度已认定学时(档案用): source → total */
    @Select("SELECT source, IFNULL(SUM(hours), 0) AS total FROM biz_hour_record " +
            "WHERE del_flag = 0 AND status = '1' AND user_id = #{userId} AND year = #{year} GROUP BY source")
    List<Map<String, Object>> sumBySource(@Param("userId") Long userId, @Param("year") Integer year);

    /** 某人已完成(已认定学时>0)的课程ID集合, 全年度口径; 用于学习地图必修完成判定 */
    @Select("<script>" +
            "SELECT DISTINCT course_id FROM biz_hour_record " +
            "WHERE del_flag = 0 AND status = '1' AND hours &gt; 0 AND user_id = #{userId} " +
            "AND course_id IN <foreach collection='courseIds' item='c' open='(' separator=',' close=')'>#{c}</foreach>" +
            "</script>")
    List<Long> learnedCourseIds(@Param("userId") Long userId, @Param("courseIds") List<Long> courseIds);

    /**
     * 物理删除签到自动生成的学时(可由签到记录重建的派生数据)。
     * 不能走 @TableLogic 软删: uk_hour_schedule_user 唯一键会与软删行冲突,
     * 导致补签/结班第二次重算插入时报 Duplicate entry。
     */
    @Delete("DELETE FROM biz_hour_record WHERE class_id = #{classId} AND source = 1")
    int deleteSignRecords(@Param("classId") Long classId);

    @Select("<script>" +
            "SELECT r.*, u.nickname AS userName, u.employee_no AS employeeNo, d.dept_name AS deptName, " +
            "c.class_name AS className, co.course_name AS courseName " +
            "FROM biz_hour_record r " +
            "LEFT JOIN sys_user u ON u.id = r.user_id " +
            "LEFT JOIN sys_dept d ON d.id = u.dept_id " +
            "LEFT JOIN biz_class c ON c.id = r.class_id " +
            "LEFT JOIN biz_course co ON co.id = r.course_id " +
            "WHERE r.del_flag = 0 " +
            "<if test='year != null'> AND r.year = #{year}</if>" +
            "<if test='userId != null'> AND r.user_id = #{userId}</if>" +
            "<if test='deptId != null'> AND u.dept_id = #{deptId}</if>" +
            "<if test='source != null'> AND r.source = #{source}</if>" +
            "<if test=\"status != null and status != ''\"> AND r.status = #{status}</if>" +
            "<if test=\"keyword != null and keyword != ''\"> AND (u.nickname LIKE CONCAT('%', #{keyword}, '%') OR u.employee_no LIKE CONCAT('%', #{keyword}, '%') OR r.remark LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            " ORDER BY r.record_date DESC, r.id DESC" +
            "</script>")
    IPage<HourRecordVo> selectPageVo(Page<HourRecordVo> page, @Param("year") Integer year,
                                     @Param("userId") Long userId, @Param("deptId") Long deptId,
                                     @Param("source") Integer source, @Param("status") String status,
                                     @Param("keyword") String keyword);
}
