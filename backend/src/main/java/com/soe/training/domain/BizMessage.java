package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 站内消息
 * msg_type: warn 达标预警 / enroll 报名开班 / eval 评估催办 / cert 证书 / exam 考试 / system 公告
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_message")
public class BizMessage extends BaseEntity {

    private Long userId;
    private String title;
    private String content;
    private String msgType;
    private Long refId;
    private Integer isRead;
}
