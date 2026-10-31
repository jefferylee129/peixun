package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 需求征集任务(年度培训需求调查)
 * 状态: 0 进行中 / 1 已结束
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_need_collection")
public class NeedCollection extends BaseEntity {

    private String title;
    private Integer year;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String description;
    private String status;
}
