package com.soe.training.domain.vo;

import com.soe.training.domain.EvalTemplate;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 评估汇总视图对象
 */
@Data
public class EvalSummaryVo {

    private EvalTemplate template;
    /** 模板条目列表(反序列化后) */
    private List<Map<String, Object>> items;
    /** 每项平均分: key -> 平均分 */
    private Map<String, java.math.BigDecimal> itemAvg;
    /** 总体平均分 */
    private java.math.BigDecimal overallAvg;
    /** 参评人数 */
    private Integer evalCount;
    /** 文本类意见汇总 */
    private java.util.List<Map<String, String>> textAnswers;
    /** 学员评语列表: 姓名 -> 评语 */
    private java.util.List<Map<String, String>> comments;
}
