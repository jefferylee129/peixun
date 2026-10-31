package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评估问卷模板(柯氏四级评估)
 * evalLevel: 1 一级反应层 / 2 二级学习层 / 3 三级行为层 / 4 四级结果层
 * items 为 JSON 数组字符串: [{"key":"q1","title":"课程内容实用性","type":"score","required":true}, ...]
 * type: score 打分(1-5) / text 文本
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_eval_template")
public class EvalTemplate extends BaseEntity {

    private String templateName;
    private Integer evalLevel;
    private String items;
    /** 0 启用 1 停用 */
    private String status;
    private String description;
}
