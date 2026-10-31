package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数配置(如学时要求、签到规则)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    private String configName;
    private String configKey;
    private String configValue;
    private String remark;
}
