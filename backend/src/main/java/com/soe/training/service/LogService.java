package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.SysOperLog;
import com.soe.training.mapper.OperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务
 */
@Service
@RequiredArgsConstructor
public class LogService {

    private final OperLogMapper operLogMapper;

    public PageResult<SysOperLog> page(int page, int size, String keyword, String status) {
        LambdaQueryWrapper<SysOperLog> w = new LambdaQueryWrapper<SysOperLog>()
                .and(keyword != null && !keyword.isBlank(), q -> q
                        .like(SysOperLog::getModule, keyword)
                        .or().like(SysOperLog::getOperation, keyword)
                        .or().like(SysOperLog::getUsername, keyword))
                .eq(status != null && !status.isBlank(), SysOperLog::getStatus, status)
                .orderByDesc(SysOperLog::getId);
        Page<SysOperLog> p = operLogMapper.selectPage(PageQuery.of(page, size), w);
        return PageResult.of(p.getTotal(), p.getRecords());
    }
}
