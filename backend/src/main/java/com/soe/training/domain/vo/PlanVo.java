package com.soe.training.domain.vo;

import com.soe.training.domain.TrainingPlan;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 培训计划视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlanVo extends TrainingPlan {

    private String deptName;
}
