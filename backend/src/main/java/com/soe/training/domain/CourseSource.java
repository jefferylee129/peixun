package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 外部课程数据源(云学堂/通用REST/演示)
 * 状态: 0 启用 / 1 停用
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_course_source")
public class CourseSource extends BaseEntity {

    private String sourceName;
    /** yunxuetang / api / mock */
    private String sourceType;
    private String baseUrl;
    private String tokenPath;
    private String listPath;
    private String appKey;
    private String appSecret;
    /** 列表数据所在 JSON 路径, 如 data.list */
    private String dataPath;
    /** 字段映射 JSON: externalId/name/category/hours/url/level/description */
    private String fieldMapping;
    /** 分类映射 JSON: 源分类原文 -> 本系统分类编码 */
    private String categoryMapping;
    /** 其他配置 JSON: defaultCategory/maxPages/pageSize/tokenBody/tokenField */
    private String extraConfig;
    private String status;
    private LocalDateTime lastSyncTime;
    private String remark;
}
