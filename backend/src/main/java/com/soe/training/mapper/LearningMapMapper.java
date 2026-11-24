package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soe.training.domain.LearningMap;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface LearningMapMapper extends BaseMapper<LearningMap> {

    /** 物理删除单条配置(避免软删行占用唯一键) */
    @Delete("DELETE FROM biz_learning_map WHERE id = #{id}")
    int deletePhysical(@Param("id") Long id);

    /** 按年度+人员类型整体覆盖: 先清后插(事务内) */
    @Delete("DELETE FROM biz_learning_map WHERE year = #{year} AND user_type = #{userType}")
    int deleteByType(@Param("year") Integer year, @Param("userType") String userType);
}
