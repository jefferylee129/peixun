package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soe.training.domain.ClassStudent;
import com.soe.training.domain.vo.StudentVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ClassStudentMapper extends BaseMapper<ClassStudent> {

    /** 物理删除, 配合唯一索引防并发重复报名 */
    @Delete("DELETE FROM biz_class_student WHERE class_id = #{classId} AND user_id = #{userId}")
    int deletePhysical(@Param("classId") Long classId, @Param("userId") Long userId);

    @Select("SELECT s.id, s.class_id AS classId, s.user_id AS userId, s.enroll_time AS enrollTime, " +
            "s.status, s.total_hours AS totalHours, s.remark, " +
            "u.nickname, u.employee_no AS employeeNo, d.dept_name AS deptName " +
            "FROM biz_class_student s " +
            "JOIN sys_user u ON u.id = s.user_id " +
            "LEFT JOIN sys_dept d ON d.id = u.dept_id " +
            "WHERE s.del_flag = 0 AND s.class_id = #{classId} ORDER BY s.id")
    List<StudentVo> selectByClassId(@Param("classId") Long classId);

    @Select("SELECT s.*, u.nickname FROM biz_class_student s JOIN sys_user u ON u.id = s.user_id " +
            "WHERE s.del_flag = 0 AND s.class_id = #{classId} AND s.user_id = #{userId}")
    ClassStudent selectOne(@Param("classId") Long classId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM biz_class_student s JOIN biz_class c ON c.id = s.class_id " +
            "WHERE s.del_flag = 0 AND s.user_id = #{userId}")
    int countByUser(@Param("userId") Long userId);
}
