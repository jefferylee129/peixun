package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soe.training.domain.ExternalCourse;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ExternalCourseMapper extends BaseMapper<ExternalCourse> {

    @Select("SELECT * FROM biz_external_course WHERE del_flag = 0 " +
            "AND source_id = #{sourceId} AND external_id = #{externalId}")
    ExternalCourse selectBySourceAndExternalId(@Param("sourceId") Long sourceId,
                                               @Param("externalId") String externalId);
}
