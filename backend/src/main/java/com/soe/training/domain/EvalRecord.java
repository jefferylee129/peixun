package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评估记录(学员对班次的评分)
 * scores 为 JSON 对象字符串: {"q1": 4, "c1": "建议增加案例"}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_eval_record")
public class EvalRecord extends BaseEntity {

    private Long templateId;
    private Long classId;
    private Long userId;
    private String scores;
    private BigDecimal totalScore;
    private String comment;
    private LocalDateTime submitTime;
}
