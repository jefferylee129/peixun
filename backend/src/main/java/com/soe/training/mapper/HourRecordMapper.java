package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.domain.HourRecord;
import com.soe.training.domain.vo.HourRecordVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

public interface HourRecordMapper extends BaseMapper<HourRecord> {

    @Select("SELECT IFNULL(SUM(hours), 0) FROM biz_hour_record " +
            "WHERE del_flag = 0 AND status = '1' AND user_id = #{userId} AND year = #{year}")
    BigDecimal sumUserHours(@Param("userId") Long userId, @Param("year") Integer year);

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
            "<if test=\"keyword != null and keyword != ''\"> AND (u.nickname LIKE CONCAT('%', #{keyword}, '%') OR u.employee_no LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            " ORDER BY r.record_date DESC, r.id DESC" +
            "</script>")
    IPage<HourRecordVo> selectPageVo(Page<HourRecordVo> page, @Param("year") Integer year,
                                     @Param("userId") Long userId, @Param("deptId") Long deptId,
                                     @Param("source") Integer source, @Param("status") String status,
                                     @Param("keyword") String keyword);
}
