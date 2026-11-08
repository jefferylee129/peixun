package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 外部同步日志
 * 动作 action: pull 拉取 / import 入库 / test 测试
 * 状态: 0 成功 / 1 失败 / 2 部分成功
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_sync_log")
public class SyncLog extends BaseEntity {

    private Long sourceId;
    private String sourceName;
    private String action;
    private String status;
    private Integer total;
    private Integer success;
    private Integer fail;
    private Integer skip;
    private String message;
    /** 逐条错误明细(换行分隔) */
    private String detail;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
