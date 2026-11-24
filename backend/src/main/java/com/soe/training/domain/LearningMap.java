package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位学习地图: 某年度某人员类型的必修课程
 * 说明: 属配置数据, 删除走物理删除(LearningMapMapper.deletePhysical),
 *       避免 @TableLogic 软删行占用 uk_map(year,user_type,course_id) 唯一键。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_learning_map")
public class LearningMap extends BaseEntity {

    private Integer year;
    private String userType;
    private Long courseId;
}
