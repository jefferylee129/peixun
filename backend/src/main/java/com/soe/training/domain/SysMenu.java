package com.soe.training.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.soe.training.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单(目录/菜单/按钮)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private Long parentId;
    private String menuName;
    /** 路由地址 */
    private String path;
    /** 前端组件路径 */
    private String component;
    /** 类型: M 目录 C 菜单 F 按钮 */
    private String menuType;
    /** 权限标识, 如 biz:need:audit */
    private String perms;
    private String icon;
    private Integer orderNum;
    /** 0 显示 1 隐藏 */
    private String visible;
    /** 0 正常 1 停用 */
    private String status;
}
