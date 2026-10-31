package com.soe.training.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 通用分页查询参数
 */
@Data
public class PageQuery {

    private Integer page = 1;
    private Integer size = 10;

    public <T> Page<T> toPage() {
        return of(page, size);
    }

    /** 统一分页钳制: 页码>=1, 单页≤200, 防止 size=-1 全表拉取 */
    public static <T> Page<T> of(Integer pageNo, Integer pageSize) {
        int p = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int s = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        return new Page<>(p, s);
    }
}
