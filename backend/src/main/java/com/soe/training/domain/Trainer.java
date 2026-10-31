package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 培训师资(内部讲师 / 外部专家)
 * 状态: 0 启用 / 1 停用
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_trainer")
public class Trainer extends BaseEntity {

    private String trainerName;
    /** 0 男 1 女 */
    private String gender;
    private String phone;
    private String email;
    /** internal 内部讲师 / external 外部专家 */
    private String trainerType;
    /** 所在单位(外部专家必填) */
    private String orgName;
    /** 职称/职务 */
    private String title;
    /** 专业领域 */
    private String specialty;
    /** 讲师等级: 初级讲师 / 中级讲师 / 高级讲师 / 特邀专家 */
    private String level;
    /** 师资简介 */
    private String intro;
    private String status;
}
