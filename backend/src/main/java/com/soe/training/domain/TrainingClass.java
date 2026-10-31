package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 培训班次(培训项目实施主体)
 * 状态: 0 筹备中 / 1 报名中 / 2 进行中 / 3 已结束 / 4 已取消
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_class")
public class TrainingClass extends BaseEntity {

    private String className;
    /** 关联培训计划, 可空 */
    private Long planId;
    /** 内训 / 外训 / 线上 / 混合 */
    private String classType;
    /** 主办部门 */
    private Long deptId;
    private LocalDate startDate;
    private LocalDate endDate;
    /** 培训地点 */
    private String location;
    /** 计划人数上限 */
    private Integer capacity;
    /** 培训经费(万元) */
    private BigDecimal budget;
    private String targetUsers;
    private String description;
    private String status;
}
