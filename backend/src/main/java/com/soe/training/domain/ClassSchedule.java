package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 课程安排(班次课表, 按节次签到与计学时)
 * 状态: 0 待上课 / 1 已完成 / 2 已取消
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_class_schedule")
public class ClassSchedule extends BaseEntity {

    private Long classId;
    private Long courseId;
    /** 授课讲师 */
    private Long trainerId;
    private LocalDate teachDate;
    private LocalTime startTime;
    private LocalTime endTime;
    /** 本节学时 */
    private BigDecimal hours;
    private String location;
    private String status;
    private String remark;
}
