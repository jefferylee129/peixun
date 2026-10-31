package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soe.training.domain.Trainer;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface TrainerMapper extends BaseMapper<Trainer> {

    /**
     * 讲师授课记录(来自班次课表)
     */
    @Select("SELECT s.teach_date AS teachDate, s.hours, s.status AS scheduleStatus, " +
            "c.class_name AS className, co.course_name AS courseName " +
            "FROM biz_class_schedule s " +
            "JOIN biz_class c ON c.id = s.class_id " +
            "LEFT JOIN biz_course co ON co.id = s.course_id " +
            "WHERE s.del_flag = 0 AND s.trainer_id = #{trainerId} AND c.del_flag = 0 " +
            "ORDER BY s.teach_date DESC")
    List<Map<String, Object>> selectTeachingRecords(@Param("trainerId") Long trainerId);
}
