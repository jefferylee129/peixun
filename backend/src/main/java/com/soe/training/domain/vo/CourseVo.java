package com.soe.training.domain.vo;

import com.soe.training.domain.Course;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseVo extends Course {

    private String trainerName;
}
