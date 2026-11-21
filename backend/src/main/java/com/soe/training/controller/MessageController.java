package com.soe.training.controller;

import com.soe.training.common.BusinessException;
import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.domain.BizMessage;
import com.soe.training.security.AuthContext;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 站内消息中心
 */
@RestController
@RequestMapping("/biz/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/my")
    public Result<PageResult<BizMessage>> my(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) Integer unread) {
        return Result.ok(messageService.my(page, size, type, unread));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(messageService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        messageService.read(id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> readAll() {
        messageService.readAll();
        return Result.ok();
    }

    @OperLog(module = "消息中心", operation = "发送系统公告")
    @RequirePerm("biz:archive:other")
    @PostMapping("/announce")
    public Result<Integer> announce(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            throw new BusinessException("请填写公告标题");
        }
        return Result.ok(messageService.announce(title, body.getOrDefault("content", "")));
    }
}
