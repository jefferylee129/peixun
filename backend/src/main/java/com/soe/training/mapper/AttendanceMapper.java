package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soe.training.domain.Attendance;
import com.soe.training.domain.vo.AttendanceVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AttendanceMapper extends BaseMapper<Attendance> {

    /** 物理删除, 配合唯一索引防并发重复签到 */
    @Delete("DELETE FROM biz_attendance WHERE schedule_id = #{scheduleId} AND user_id = #{userId}")
    int deletePhysical(@Param("scheduleId") Long scheduleId, @Param("userId") Long userId);

    @Delete("DELETE FROM biz_attendance WHERE class_id = #{classId} AND user_id = #{userId}")
    int deletePhysicalByClass(@Param("classId") Long classId, @Param("userId") Long userId);

    @Select("SELECT a.*, u.nickname, u.employee_no AS employeeNo, d.dept_name AS deptName " +
            "FROM biz_attendance a " +
            "JOIN sys_user u ON u.id = a.user_id " +
            "LEFT JOIN sys_dept d ON d.id = u.dept_id " +
            "WHERE a.del_flag = 0 AND a.schedule_id = #{scheduleId} ORDER BY u.id")
    List<AttendanceVo> selectByScheduleId(@Param("scheduleId") Long scheduleId);

    @Select("SELECT * FROM biz_attendance WHERE del_flag = 0 AND schedule_id = #{scheduleId} AND user_id = #{userId}")
    Attendance selectByScheduleAndUser(@Param("scheduleId") Long scheduleId, @Param("userId") Long userId);
}
