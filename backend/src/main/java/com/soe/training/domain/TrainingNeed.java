package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 培训需求(员工/部门申报)
 * 状态: 0 草稿 / 1 待审核 / 2 已采纳 / 3 已驳回
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_need")
public class TrainingNeed extends BaseEntity {

    private Integer year;
    private Long deptId;
    /** 申报人 */
    private Long userId;
    /** 关联需求征集任务, 可空 */
    private Long collectionId;
    private String title;
    /** 需求类别: party 党性教育 / mgmt 管理能力 / skill 岗位技能 / safety 安全合规 / general 通用素质 */
    private String needType;
    /** 需求描述 */
    private String content;
    /** 申报理由 */
    private String reason;
    /** 期望培训时间, 如 2026-06 */
    private String expectedTime;
    /** 建议参训人数 */
    private Integer participants;
    private String status;
    private Long auditUserId;
    private LocalDateTime auditTime;
    private String auditRemark;
}
