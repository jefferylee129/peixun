package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private Long deptId;
    private String username;
    private String password;
    private String nickname;
    /** 工号 */
    private String employeeNo;
    /** 0 男 1 女 */
    private String gender;
    private String phone;
    private String email;
    /** 岗位 */
    private String position;
    /** 人员类型: manager 管理干部 / professional 专业技术人员 / staff 一般员工 */
    private String userType;
    /** 0 正常 1 停用 */
    private String status;
    private String remark;
}
