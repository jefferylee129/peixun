package com.soe.training.domain.vo;

import com.soe.training.domain.TrainingNeed;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 培训需求视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NeedVo extends TrainingNeed {

    private String deptName;
    private String userName;
}
