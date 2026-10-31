package com.soe.training.domain.vo;

import com.soe.training.domain.EvalRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评估记录视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EvalRecordVo extends EvalRecord {

    private String nickname;
    private String deptName;
}
