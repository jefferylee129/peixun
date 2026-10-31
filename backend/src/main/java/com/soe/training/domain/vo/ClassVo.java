package com.soe.training.domain.vo;

import com.soe.training.domain.TrainingClass;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 培训班次视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClassVo extends TrainingClass {

    private String deptName;
    private String planName;
    /** 已报名人数 */
    private Integer studentCount;
    /** 我的报名状态(我的班级用) */
    private String myStudentStatus;
    /** 我在本班累计学时 */
    private java.math.BigDecimal myTotalHours;
    /** 我是否已提交评估 */
    private Boolean evalFilled;
}
