package com.soe.training.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.soe.training.domain.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户保存参数(含角色)
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserSaveDto extends SysUser {

    @TableField(exist = false)
    private List<Long> roleIds;
}
