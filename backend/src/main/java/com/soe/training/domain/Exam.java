package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 在线考试(柯氏二级·学习层落地)
 * question_ids: 题目ID逗号分隔, 交卷时按 total_score 均分每题
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_exam")
public class Exam extends BaseEntity {

    private String title;
    private Long classId;
    private Long courseId;
    private Integer durationMin;
    private BigDecimal passScore;
    private BigDecimal totalScore;
    private BigDecimal hourReward;
    private String questionIds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 0 草稿 1 已发布 2 已截止 */
    private String status;
}
