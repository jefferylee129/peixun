package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soe.training.domain.ClassSchedule;
import com.soe.training.domain.vo.ScheduleVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ScheduleMapper extends BaseMapper<ClassSchedule> {

    @Select("SELECT s.*, co.course_name AS courseName, t.trainer_name AS trainerName " +
            "FROM biz_class_schedule s " +
            "LEFT JOIN biz_course co ON co.id = s.course_id " +
            "LEFT JOIN biz_trainer t ON t.id = s.trainer_id " +
            "WHERE s.del_flag = 0 AND s.class_id = #{classId} " +
            "ORDER BY s.teach_date, s.start_time")
    List<ScheduleVo> selectByClassId(@Param("classId") Long classId);
}
