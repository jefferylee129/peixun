package com.soe.training.domain.vo;

import com.soe.training.domain.HourRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学时台账视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HourRecordVo extends HourRecord {

    private String userName;
    private String employeeNo;
    private String deptName;
    private String className;
    private String courseName;
}
