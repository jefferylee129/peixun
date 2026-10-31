package com.soe.training.domain.vo;

import com.soe.training.domain.ClassSchedule;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课表视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleVo extends ClassSchedule {

    private String courseName;
    private String trainerName;
}
