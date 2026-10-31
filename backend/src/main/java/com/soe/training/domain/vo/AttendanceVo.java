package com.soe.training.domain.vo;

import com.soe.training.domain.Attendance;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 签到视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceVo extends Attendance {

    private String nickname;
    private String employeeNo;
    private String deptName;
}
