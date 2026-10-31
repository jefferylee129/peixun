package com.soe.training.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导入模板定义(表头 + 示例行; 示例行本身即合法数据, 便于导入自测)
 */
public class ExcelTemplates {

    @Data
    @AllArgsConstructor
    public static class Template {
        private String type;
        private List<String> headers;
        private List<List<Object>> example;
    }

    private static final Map<String, Template> ALL = new LinkedHashMap<>();

    private static void reg(String type, List<String> headers, List<List<Object>> example) {
        ALL.put(type, new Template(type, headers, example));
    }

    static {
        reg("need",
                Arrays.asList("年度", "需求标题", "需求类别", "需求描述", "申报理由", "期望时间", "建议人数", "申报人工号(选填, 仅管理员可用)"),
                List.of(Arrays.<Object>asList(2026, "业财融合专题培训", "岗位技能", "希望开展业财融合与数字化工具培训", "数字化转型需要", "2026-06", 20, "EMP003")));
        reg("plan",
                Arrays.asList("计划名称", "年度", "计划类型", "主办部门", "开始日期", "结束日期", "培训对象", "计划内容", "预算(万元)"),
                List.of(Arrays.<Object>asList("2026年专项技能培训计划", 2026, "专项", "人力资源部", "2026-06-01", "2026-06-30", "业务骨干", "专题技能集中培训", 5)));
        reg("course",
                Arrays.asList("课程编码", "课程名称", "课程分类", "课程形式", "标准学时", "课程层级", "培训对象", "课程大纲", "讲师姓名"),
                List.of(Arrays.<Object>asList("", "办公软件高级应用", "通用素质", "内训", 6, "初级", "全员", "Excel进阶;PPT呈现", "陈建国")));
        reg("trainer",
                Arrays.asList("姓名", "师资类型", "性别", "所在单位", "职称", "专业领域", "讲师等级", "电话", "邮箱", "师资简介"),
                List.of(Arrays.<Object>asList("王建国", "内部", "男", "", "高级工程师", "安全管理", "高级讲师", "", "", "20年安全管理工作经验")));
        reg("class",
                Arrays.asList("班次名称", "班次类型", "主办部门", "开始日期", "结束日期", "培训地点", "人数上限", "经费(万元)", "培训对象", "班次说明"),
                List.of(Arrays.<Object>asList("2026年第一期安全培训班", "内训", "安全生产部", "2026-11-01", "2026-11-02", "培训中心", 40, 3, "安全管理人员", "批量导入示例")));
        reg("hours",
                Arrays.asList("工号", "学时", "学时日期", "来源", "备注", "关联班次名称(选填)"),
                List.of(Arrays.<Object>asList("EMP003", 8, "2026-10-20", "外部认定", "注册会计师继续教育", "")));
        reg("class-student",
                Arrays.asList("工号"),
                List.of(Arrays.<Object>asList("EMP003")));
        reg("user",
                Arrays.asList("登录账号", "姓名", "工号", "部门名称", "岗位", "人员类型", "性别", "手机", "邮箱", "初始密码(选填, 默认123456)"),
                List.of(Arrays.<Object>asList("testuser01", "测试用户", "EMP901", "人力资源部", "职员", "一般员工", "男", "13800000000", "", "123456")));
    }

    public static Template get(String type) {
        Template t = ALL.get(type);
        if (t == null) {
            throw new IllegalArgumentException("未知模板类型: " + type);
        }
        return t;
    }

    public static boolean exists(String type) {
        return ALL.containsKey(type);
    }
}
