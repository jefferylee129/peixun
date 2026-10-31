package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.domain.Course;
import com.soe.training.domain.vo.CourseVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CourseMapper extends BaseMapper<Course> {

    @Select("<script>" +
            "SELECT c.*, t.trainer_name AS trainerName FROM biz_course c " +
            "LEFT JOIN biz_trainer t ON t.id = c.trainer_id " +
            "WHERE c.del_flag = 0 " +
            "<if test=\"category != null and category != ''\"> AND c.category = #{category}</if>" +
            "<if test=\"courseType != null and courseType != ''\"> AND c.course_type = #{courseType}</if>" +
            "<if test=\"status != null and status != ''\"> AND c.status = #{status}</if>" +
            "<if test=\"keyword != null and keyword != ''\"> AND (c.course_name LIKE CONCAT('%', #{keyword}, '%') OR c.course_code LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            " ORDER BY c.id DESC" +
            "</script>")
    IPage<CourseVo> selectPageVo(Page<CourseVo> page, @Param("category") String category,
                                 @Param("courseType") String courseType, @Param("status") String status,
                                 @Param("keyword") String keyword);
}
