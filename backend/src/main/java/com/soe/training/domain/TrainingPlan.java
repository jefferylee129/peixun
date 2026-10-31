package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 培训计划
 * 状态: 0 草稿 / 1 待审批 / 2 已批准 / 3 已驳回
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_plan")
public class TrainingPlan extends BaseEntity {

    private String planName;
    private Integer year;
    /** 计划类型: year 年度 / quarter 季度 / month 月度 / special 专项 */
    private String planType;
    /** 主办部门 */
    private Long deptId;
    private LocalDate startDate;
    private LocalDate endDate;
    /** 培训对象 */
    private String targetUsers;
    /** 计划内容 */
    private String content;
    /** 预算(万元) */
    private BigDecimal budget;
    private String status;
    private LocalDateTime submitTime;
    private Long approveUserId;
    private LocalDateTime approveTime;
    private String approveRemark;
}
