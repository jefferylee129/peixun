package com.soe.training.domain.vo;

import com.soe.training.domain.ClassStudent;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 班级学员视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentVo extends ClassStudent {

    private String nickname;
    private String employeeNo;
    private String deptName;
}
