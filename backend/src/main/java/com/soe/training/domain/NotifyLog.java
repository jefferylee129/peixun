package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自动提醒发送留痕: 同类提醒同一业务对象在去重窗口内不重复发送
 */
@Data
@TableName("biz_notify_log")
public class NotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String notifyType;
    private String bizKey;
    private Long userId;
    private LocalDateTime sentTime;
}
