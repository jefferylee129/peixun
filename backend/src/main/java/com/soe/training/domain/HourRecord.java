package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学时台账(个人年度学时的唯一口径)
 * 来源 source: 1 签到自动 / 2 手工补录 / 3 外部认定 / 4 线上学习
 * 状态: 0 待认定 / 1 已认定 / 2 已驳回
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_hour_record")
public class HourRecord extends BaseEntity {

    private Long userId;
    private Integer year;
    private Long classId;
    private Long scheduleId;
    private Long courseId;
    private Integer source;
    private BigDecimal hours;
    private String status;
    private LocalDate recordDate;
    private Long auditUserId;
    private LocalDateTime auditTime;
    private String auditRemark;
    private String remark;
}
