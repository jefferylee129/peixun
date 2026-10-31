package com.soe.training.controller;

import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.SysRole;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理
 */
@RestController
@RequestMapping("/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final RoleService roleService;

    @RequirePerm("system:role:list")
    @GetMapping
    public Result<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(roleService.page(page, size, keyword));
    }

    @RequirePerm("system:role:list")
    @GetMapping("/all")
    public Result<List<SysRole>> all() {
        return Result.ok(roleService.allActive());
    }

    @RequirePerm("system:role:list")
    @GetMapping("/{id}/menuIds")
    public Result<List<Long>> menuIds(@PathVariable Long id) {
        return Result.ok(roleService.menuIds(id));
    }

    @OperLog(module = "角色管理", operation = "保存角色")
    @RequirePerm("system:role:add")
    @PostMapping
    public Result<Void> add(@RequestBody SysRole role) {
        roleService.save(role);
        return Result.ok();
    }

    @OperLog(module = "角色管理", operation = "修改角色")
    @RequirePerm("system:role:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleService.save(role);
        return Result.ok();
    }

    @OperLog(module = "角色管理", operation = "删除角色")
    @RequirePerm("system:role:remove")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @OperLog(module = "角色管理", operation = "分配菜单权限")
    @RequirePerm("system:role:edit")
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        roleService.assignMenus(id, body.get("menuIds"));
        return Result.ok();
    }
}
