package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    private Long parentId;
    /** 祖级路径, 逗号分隔 */
    private String ancestors;
    private String deptName;
    private Integer orderNum;
    private String leader;
    private String phone;
    /** 0 正常 1 停用 */
    private String status;
}
