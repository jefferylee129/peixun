package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soe.training.common.BusinessException;
import com.soe.training.domain.SysMenu;
import com.soe.training.mapper.MenuMapper;
import com.soe.training.security.AuthContext;
import com.soe.training.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单服务
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuMapper menuMapper;

    /** 全部菜单树(管理端维护用) */
    public List<Map<String, Object>> tree() {
        List<SysMenu> all = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getOrderNum));
        return buildTree(all, 0L, true);
    }

    /** 用户侧边栏菜单树(仅目录与菜单) */
    public List<Map<String, Object>> buildUserMenus(Long userId) {
        LoginUser ctx = AuthContext.get();
        List<SysMenu> menus;
        if (ctx != null && ctx.isAdmin()) {
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getMenuType, "M", "C")
                    .eq(SysMenu::getVisible, "0")
                    .orderByAsc(SysMenu::getOrderNum));
        } else {
            menus = menuMapper.selectMenusByUserId(userId).stream()
                    .filter(m -> ("M".equals(m.getMenuType()) || "C".equals(m.getMenuType()))
                            && "0".equals(m.getVisible()))
                    .toList();
        }
        return buildTree(menus, 0L, false);
    }

    public void save(SysMenu menu) {
        if (menu.getId() == null) {
            menuMapper.insert(menu);
        } else {
            menuMapper.updateById(menu);
        }
    }

    public void delete(Long id) {
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (children > 0) {
            throw new BusinessException("存在下级菜单, 不能删除");
        }
        menuMapper.deleteById(id);
    }

    private List<Map<String, Object>> buildTree(List<SysMenu> all, Long parentId, boolean withPerms) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (SysMenu m : all) {
            if (!parentId.equals(m.getParentId())) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", m.getId());
            node.put("parentId", m.getParentId());
            node.put("menuName", m.getMenuName());
            node.put("path", m.getPath());
            node.put("component", m.getComponent());
            node.put("icon", m.getIcon());
            node.put("menuType", m.getMenuType());
            node.put("orderNum", m.getOrderNum());
            node.put("visible", m.getVisible());
            node.put("status", m.getStatus());
            if (withPerms) {
                node.put("perms", m.getPerms());
            }
            node.put("children", buildTree(all, m.getId(), withPerms));
            nodes.add(node);
        }
        return nodes;
    }
}
