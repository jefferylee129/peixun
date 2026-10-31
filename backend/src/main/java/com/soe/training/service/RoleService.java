package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.SysRole;
import com.soe.training.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;

    public PageResult<SysRole> page(int page, int size, String keyword) {
        LambdaQueryWrapper<SysRole> w = new LambdaQueryWrapper<SysRole>()
                .and(keyword != null && !keyword.isBlank(), q -> q
                        .like(SysRole::getRoleName, keyword)
                        .or().like(SysRole::getRoleKey, keyword))
                .orderByAsc(SysRole::getOrderNum);
        Page<SysRole> p = roleMapper.selectPage(PageQuery.of(page, size), w);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public List<SysRole> allActive() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, "0").orderByAsc(SysRole::getOrderNum));
    }

    @Transactional
    public void save(SysRole role) {
        Long dup = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, role.getRoleKey())
                .ne(role.getId() != null, SysRole::getId, role.getId()));
        if (dup > 0) {
            throw new BusinessException("角色标识已存在: " + role.getRoleKey());
        }
        if (role.getId() == null) {
            roleMapper.insert(role);
        } else {
            roleMapper.updateById(role);
        }
    }

    @Transactional
    public void delete(Long id) {
        if (id == 1L) {
            throw new BusinessException("内置管理员角色不允许删除");
        }
        roleMapper.deleteById(id);
        roleMapper.deleteRoleMenu(id);
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMapper.deleteRoleMenu(roleId);
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                roleMapper.insertRoleMenu(roleId, menuId);
            }
        }
    }

    public List<Long> menuIds(Long roleId) {
        return roleMapper.selectMenuIds(roleId);
    }
}
