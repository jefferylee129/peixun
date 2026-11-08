package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培训课程(课程库)
 * 状态: 0 草稿 / 1 待审核 / 2 已发布 / 3 已下架
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_course")
public class Course extends BaseEntity {

    private String courseCode;
    private String courseName;
    /** 课程分类: party 党性教育 / mgmt 管理能力 / skill 岗位技能 / safety 安全合规 / general 通用素质 */
    private String category;
    /** 课程形式: internal 内训 / external 外训 / online 线上 */
    private String courseType;
    /** 标准学时 */
    private BigDecimal hours;
    /** 课程层级: 初级 / 中级 / 高级 */
    private String level;
    private String targetUsers;
    /** 课程大纲 */
    private String outline;
    /** 主讲讲师 */
    private Long trainerId;
    private String status;
    private Long auditUserId;
    private LocalDateTime auditTime;
    private String auditRemark;
    /** 0 自建 1 外部同步 */
    private Integer sourceType;
    /** 来源外部系统ID */
    private Long sourceId;
    /** 外部课程唯一编码 */
    private String externalCode;
    /** 外部学习链接 */
    private String externalUrl;
}
