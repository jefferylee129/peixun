package com.soe.training.controller;

import com.soe.training.common.PageResult;
import com.soe.training.common.Result;
import com.soe.training.security.AuthContext;
import com.soe.training.security.OperLog;
import com.soe.training.security.RequirePerm;
import com.soe.training.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 结业证书
 */
@RestController
@RequestMapping("/biz/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @RequirePerm("biz:cert:list")
    @GetMapping
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) Integer year,
                                                        @RequestParam(required = false) String keyword) {
        return Result.ok(certificateService.page(page, size, year, keyword));
    }

    @GetMapping("/my")
    public Result<List<Map<String, Object>>> my() {
        return Result.ok(certificateService.my());
    }

    @RequirePerm("biz:cert:list")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(certificateService.detail(id));
    }

    /** 幂等补发: 对结业班次的已完成学员补齐证书 */
    @OperLog(module = "结业证书", operation = "班次补发证书")
    @RequirePerm("biz:cert:issue")
    @PostMapping("/class/{classId}/issue")
    public Result<Integer> issue(@PathVariable Long classId) {
        return Result.ok(certificateService.issueForClass(classId));
    }
}
