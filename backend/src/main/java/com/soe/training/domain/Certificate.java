package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 结业证书(班次结班自动生成, 编号 QGPX-YYYY-序号)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_certificate")
public class Certificate extends BaseEntity {

    private String certNo;
    private Long userId;
    private Long classId;
    private BigDecimal hours;
    private String grade;
    /** 防伪码(6位): 公开查验需 证书编号+防伪码 双要素 */
    private String verifyCode;
    private LocalDate issueDate;
}
