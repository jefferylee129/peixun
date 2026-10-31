package com.soe.training.controller;

import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.dto.UserSaveDto;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理
 */
@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final UserService userService;

    @RequirePerm("system:user:list")
    @GetMapping
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Long deptId,
                                                        @RequestParam(required = false) String status) {
        return Result.ok(userService.page(page, size, keyword, deptId, status));
    }

    @RequirePerm("system:user:list")
    @GetMapping("/simple")
    public Result<List<Map<String, Object>>> simple(@RequestParam(required = false) Long deptId,
                                                    @RequestParam(required = false) String keyword) {
        return Result.ok(userService.simpleList(deptId, keyword));
    }

    @RequirePerm("system:user:list")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    @OperLog(module = "用户管理", operation = "新增用户")
    @RequirePerm("system:user:add")
    @PostMapping
    public Result<Void> add(@RequestBody UserSaveDto dto) {
        userService.save(dto);
        return Result.ok();
    }

    @OperLog(module = "用户管理", operation = "修改用户")
    @RequirePerm("system:user:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserSaveDto dto) {
        dto.setId(id);
        userService.save(dto);
        return Result.ok();
    }

    @OperLog(module = "用户管理", operation = "删除用户")
    @RequirePerm("system:user:remove")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @OperLog(module = "用户管理", operation = "重置密码")
    @RequirePerm("system:user:resetPwd")
    @PutMapping("/{id}/resetPassword")
    public Result<String> resetPassword(@PathVariable Long id) {
        return Result.ok("密码已重置", userService.resetPassword(id));
    }

    @OperLog(module = "用户管理", operation = "变更用户状态")
    @RequirePerm("system:user:edit")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        userService.changeStatus(id, status);
        return Result.ok();
    }
}
