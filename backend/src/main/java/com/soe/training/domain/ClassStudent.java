package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 班级学员(报名/参训记录)
 * 状态: 0 已报名 / 2 已完成 / 3 缺勤
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_class_student")
public class ClassStudent extends BaseEntity {

    private Long classId;
    private Long userId;
    private LocalDateTime enrollTime;
    private String status;
    /** 本班累计获得学时(签到完成时自动汇总) */
    private BigDecimal totalHours;
    private String remark;
}
