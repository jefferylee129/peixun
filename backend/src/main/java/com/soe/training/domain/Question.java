package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 考试题库
 * q_type: single 单选 / multiple 多选 / judge 判断
 * options JSON: [{"key":"A","text":"..."}]; 判断题 ["对","错"]
 * answer: A / ABD / 对
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_question")
public class Question extends BaseEntity {

    private Long courseId;
    /**
     * 题型: single 单选 / multiple 多选 / judge 判断。
     * 显式指定 JSON 名: Lombok 生成的 getQType 会被 Jackson 推导为 "qtype",
     * 导致入参 qType 绑不上、出参键名也错。
     */
    @JsonProperty("qType")
    private String qType;
    private String stem;
    private String options;
    private String answer;
    private String analysis;
    /** 0 启用 1 停用 */
    private String status;
}
