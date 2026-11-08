package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 外部课程暂存(审核入库制)
 * 状态: 0 待入库 / 1 已入库 / 2 已忽略
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_external_course")
public class ExternalCourse extends BaseEntity {

    private Long sourceId;
    private String externalId;
    private String courseName;
    /** 源分类原文 */
    private String categoryRaw;
    /** 映射后本系统分类 */
    private String category;
    private String courseType;
    private BigDecimal hours;
    private String level;
    private String targetUsers;
    private String description;
    private String url;
    /** 原始报文留档(审计) */
    private String rawJson;
    private String status;
    /** 入库后的课程ID */
    private Long courseId;
    private LocalDateTime syncTime;
}
