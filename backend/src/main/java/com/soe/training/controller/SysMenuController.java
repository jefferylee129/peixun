package com.soe.training.controller;

import com.soe.training.common.Result;
import com.soe.training.domain.SysMenu;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 菜单管理
 */
@RestController
@RequestMapping("/system/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final MenuService menuService;

    @RequirePerm("system:menu:list")
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        return Result.ok(menuService.tree());
    }

    @OperLog(module = "菜单管理", operation = "保存菜单")
    @RequirePerm("system:menu:add")
    @PostMapping
    public Result<Void> add(@RequestBody SysMenu menu) {
        menuService.save(menu);
        return Result.ok();
    }

    @OperLog(module = "菜单管理", operation = "修改菜单")
    @RequirePerm("system:menu:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysMenu menu) {
        menu.setId(id);
        menuService.save(menu);
        return Result.ok();
    }

    @OperLog(module = "菜单管理", operation = "删除菜单")
    @RequirePerm("system:menu:remove")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}
