package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.domain.TrainingClass;
import com.soe.training.domain.vo.ClassVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ClassMapper extends BaseMapper<TrainingClass> {

    @Select("SELECT * FROM biz_class WHERE id = #{id} FOR UPDATE")
    TrainingClass lockById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT c.*, d.dept_name AS deptName, p.plan_name AS planName, " +
            "(SELECT COUNT(*) FROM biz_class_student s WHERE s.class_id = c.id AND s.del_flag = 0) AS studentCount " +
            "FROM biz_class c " +
            "LEFT JOIN sys_dept d ON d.id = c.dept_id " +
            "LEFT JOIN biz_plan p ON p.id = c.plan_id " +
            "WHERE c.del_flag = 0 " +
            "<if test=\"status != null and status != ''\"> AND c.status = #{status}</if>" +
            "<if test='year != null'> AND YEAR(c.start_date) = #{year}</if>" +
            "<if test='deptId != null'> AND c.dept_id = #{deptId}</if>" +
            "<if test=\"keyword != null and keyword != ''\"> AND c.class_name LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "<if test='enrollable'> AND c.status = 1 AND (c.capacity IS NULL OR c.capacity > (SELECT COUNT(*) FROM biz_class_student s2 WHERE s2.class_id = c.id AND s2.del_flag = 0)) AND NOT EXISTS (SELECT 1 FROM biz_class_student s3 WHERE s3.class_id = c.id AND s3.user_id = #{userId} AND s3.del_flag = 0)</if>" +
            " ORDER BY c.id DESC" +
            "</script>")
    IPage<ClassVo> selectPageVo(Page<ClassVo> page, @Param("status") String status,
                                @Param("year") Integer year, @Param("deptId") Long deptId,
                                @Param("keyword") String keyword,
                                @Param("enrollable") boolean enrollable, @Param("userId") Long userId);

    @Select("SELECT c.*, d.dept_name AS deptName, p.plan_name AS planName, " +
            "(SELECT COUNT(*) FROM biz_class_student s WHERE s.class_id = c.id AND s.del_flag = 0) AS studentCount " +
            "FROM biz_class c " +
            "LEFT JOIN sys_dept d ON d.id = c.dept_id " +
            "LEFT JOIN biz_plan p ON p.id = c.plan_id " +
            "WHERE c.del_flag = 0 AND c.id = #{id}")
    ClassVo selectVoById(@Param("id") Long id);

    @Select("SELECT c.*, d.dept_name AS deptName, p.plan_name AS planName, " +
            "s2.status AS myStudentStatus, s2.total_hours AS myTotalHours, " +
            "EXISTS(SELECT 1 FROM biz_eval_record er WHERE er.class_id = c.id AND er.user_id = #{userId} AND er.del_flag = 0) AS evalFilled " +
            "FROM biz_class c " +
            "JOIN biz_class_student s2 ON s2.class_id = c.id AND s2.user_id = #{userId} AND s2.del_flag = 0 " +
            "LEFT JOIN sys_dept d ON d.id = c.dept_id " +
            "LEFT JOIN biz_plan p ON p.id = c.plan_id " +
            "WHERE c.del_flag = 0 ORDER BY c.id DESC")
    IPage<ClassVo> selectMyClasses(Page<ClassVo> page, @Param("userId") Long userId);
}
