package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private String roleName;
    /** 角色标识, 如 admin / training_admin / employee */
    private String roleKey;
    private Integer orderNum;
    /** 0 正常 1 停用 */
    private String status;
    private String remark;
}
