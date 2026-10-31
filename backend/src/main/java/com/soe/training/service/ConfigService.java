package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.SysConfig;
import com.soe.training.mapper.ConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 系统参数服务
 */
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final ConfigMapper configMapper;

    public PageResult<SysConfig> page(int page, int size, String keyword) {
        LambdaQueryWrapper<SysConfig> w = new LambdaQueryWrapper<SysConfig>()
                .and(keyword != null && !keyword.isBlank(), q -> q
                        .like(SysConfig::getConfigName, keyword)
                        .or().like(SysConfig::getConfigKey, keyword))
                .orderByAsc(SysConfig::getId);
        Page<SysConfig> p = configMapper.selectPage(PageQuery.of(page, size), w);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public void save(SysConfig config) {
        Long dup = configMapper.selectCount(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, config.getConfigKey())
                .ne(config.getId() != null, SysConfig::getId, config.getId()));
        if (dup > 0) {
            throw new BusinessException("参数键名已存在: " + config.getConfigKey());
        }
        if (config.getId() == null) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
    }

    public void delete(Long id) {
        configMapper.deleteById(id);
    }

    /** 按键名取值, 不存在返回默认值 */
    public String getValue(String key, String defaultValue) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));
        return config == null || config.getConfigValue() == null ? defaultValue : config.getConfigValue();
    }
}
