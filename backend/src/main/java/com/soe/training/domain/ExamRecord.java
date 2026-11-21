package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试作答(一人一卷, 重复提交拒绝)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_exam_record")
public class ExamRecord extends BaseEntity {

    private Long examId;
    private Long userId;
    private String answers;
    private BigDecimal score;
    private Integer passed;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
}
