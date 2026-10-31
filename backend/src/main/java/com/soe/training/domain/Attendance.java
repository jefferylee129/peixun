package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考勤签到(按节次)
 * signType: normal 正常 / late 迟到 / leave 请假 / absent 缺勤
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_attendance")
public class Attendance extends BaseEntity {

    private Long scheduleId;
    private Long classId;
    private Long userId;
    private String signType;
    private LocalDateTime signTime;
    /** 本节获得学时(节次结束时按规则折算) */
    private BigDecimal hoursObtained;
    private String remark;
}
