package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soe.training.common.BusinessException;
import com.soe.training.domain.SysDept;
import com.soe.training.domain.SysUser;
import com.soe.training.mapper.DeptMapper;
import com.soe.training.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门服务
 */
@Service
@RequiredArgsConstructor
public class DeptService {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;

    public List<Map<String, Object>> tree() {
        List<SysDept> all = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getOrderNum));
        return buildTree(all, 0L);
    }

    public void save(SysDept dept) {
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getParentId() != 0L) {
            SysDept parent = deptMapper.selectById(dept.getParentId());
            if (parent == null) {
                throw new BusinessException("上级部门不存在");
            }
            dept.setAncestors(parent.getAncestors() + "," + parent.getId());
        } else {
            dept.setAncestors("0");
        }
        if (dept.getId() == null) {
            deptMapper.insert(dept);
        } else {
            deptMapper.updateById(dept);
        }
    }

    public void delete(Long id) {
        Long children = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, id));
        if (children > 0) {
            throw new BusinessException("存在下级部门, 不能删除");
        }
        Long users = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeptId, id));
        if (users > 0) {
            throw new BusinessException("部门下存在用户, 不能删除");
        }
        deptMapper.deleteById(id);
    }

    private List<Map<String, Object>> buildTree(List<SysDept> all, Long parentId) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (SysDept d : all) {
            if (!parentId.equals(d.getParentId())) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", d.getId());
            node.put("parentId", d.getParentId());
            node.put("deptName", d.getDeptName());
            node.put("orderNum", d.getOrderNum());
            node.put("leader", d.getLeader());
            node.put("phone", d.getPhone());
            node.put("status", d.getStatus());
            node.put("children", buildTree(all, d.getId()));
            nodes.add(node);
        }
        return nodes;
    }
}
