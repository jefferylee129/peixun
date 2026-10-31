package com.soe.training.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.HourRecord;
import com.soe.training.domain.SysUser;
import com.soe.training.domain.vo.HourRecordVo;
import com.soe.training.mapper.HourRecordMapper;
import com.soe.training.mapper.StatsMapper;
import com.soe.training.mapper.UserMapper;
import com.soe.training.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 培训学时服务(台账 + 达标统计)
 */
@Service
@RequiredArgsConstructor
public class HoursService {

    private final HourRecordMapper hourRecordMapper;
    private final StatsMapper statsMapper;
    private final UserMapper userMapper;
    private final ConfigService configService;

    public PageResult<HourRecordVo> page(int page, int size, Integer year, Long userId, Long deptId,
                                         Integer source, String status, String keyword) {
        Page<HourRecordVo> p = PageQuery.of(page, size);
        return PageResult.of(hourRecordMapper.selectPageVo(p, year, userId, deptId, source, status, keyword));
    }

    public PageResult<HourRecordVo> my(int page, int size, Integer year) {
        Long userId = AuthContext.getUserId();
        Page<HourRecordVo> p = PageQuery.of(page, size);
        return PageResult.of(hourRecordMapper.selectPageVo(p, year, userId, null, null, null, null));
    }

    /** 手工补录/外部认定, 待认定后进入台账 */
    @Transactional
    public void add(HourRecord r) {
        if (r.getUserId() == null) {
            throw new BusinessException("请选择学时归属人");
        }
        if (r.getHours() == null || r.getHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("学时必须大于0");
        }
        if (r.getRecordDate() == null) {
            r.setRecordDate(LocalDate.now());
        }
        r.setYear(r.getRecordDate().getYear());
        // 学时来源收敛: 台账手工入口只允许 2手工补录 / 3外部认定, 1签到来源只能由系统生成
        if (r.getSource() == null || (r.getSource() != 2 && r.getSource() != 3)) {
            r.setSource(2);
        }
        r.setStatus("0");
        hourRecordMapper.insert(r);
    }

    public void audit(Long id, boolean pass, String remark) {
        HourRecord r = hourRecordMapper.selectById(id);
        if (r == null) {
            throw new BusinessException("学时记录不存在");
        }
        if (!"0".equals(r.getStatus())) {
            throw new BusinessException("该记录不在待认定状态");
        }
        HourRecord u = new HourRecord();
        u.setId(id);
        u.setStatus(pass ? "1" : "2");
        u.setAuditUserId(AuthContext.getUserId());
        u.setAuditTime(LocalDateTime.now());
        u.setAuditRemark(remark);
        hourRecordMapper.updateById(u);
    }

    public void delete(Long id) {
        HourRecord r = hourRecordMapper.selectById(id);
        if (r == null) {
            throw new BusinessException("学时记录不存在");
        }
        if (!"0".equals(r.getStatus())) {
            throw new BusinessException("仅待认定记录可删除");
        }
        hourRecordMapper.deleteById(id);
    }

    /** 导出用: 不分页拉取(上限5000) */
    public List<HourRecordVo> listForExport(Integer year, Long userId, Long deptId,
                                            Integer source, String status, String keyword) {
        Page<HourRecordVo> p = Page.of(1, 5000, false);
        return hourRecordMapper.selectPageVo(p, year, userId, deptId, source, status, keyword).getRecords();
    }

    /** 我的年度学时汇总 */
    public Map<String, Object> mySummary(Integer year) {
        Long userId = AuthContext.getUserId();
        int y = year == null ? LocalDate.now().getYear() : year;
        SysUser user = userMapper.selectById(userId);
        BigDecimal total = hourRecordMapper.sumUserHours(userId, y);
        BigDecimal required = requiredOf(user == null ? "staff" : user.getUserType());
        Map<String, Object> result = new HashMap<>();
        result.put("year", y);
        result.put("total", total.setScale(1, RoundingMode.HALF_UP));
        result.put("required", required);
        result.put("reached", total.compareTo(required) >= 0);
        result.put("percent", percent(total, required));
        return result;
    }

    /** 员工年度学时达标统计(管理人员/专业技术人员/一般员工分类要求) */
    public List<Map<String, Object>> userStats(Integer year, Long deptId, String keyword) {
        int y = year == null ? LocalDate.now().getYear() : year;
        List<Map<String, Object>> rows = statsMapper.userYearHours(y, deptId, keyword);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String userType = row.get("userType") == null ? "staff" : row.get("userType").toString();
            BigDecimal required = requiredOf(userType);
            BigDecimal total = row.get("totalHours") == null
                    ? BigDecimal.ZERO : new BigDecimal(row.get("totalHours").toString());
            Map<String, Object> item = new HashMap<>(row);
            item.put("required", required);
            item.put("reached", total.compareTo(required) >= 0);
            item.put("percent", percent(total, required));
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> deptStats(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        return statsMapper.deptHours(y);
    }

    public List<Map<String, Object>> monthStats(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        return statsMapper.monthHours(y);
    }

    private BigDecimal requiredOf(String userType) {
        String value = switch (userType) {
            case "manager" -> configService.getValue("hour.required.manager", "110");
            case "professional" -> configService.getValue("hour.required.professional", "90");
            default -> configService.getValue("hour.required.staff", "90");
        };
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return new BigDecimal("90");
        }
    }

    private int percent(BigDecimal total, BigDecimal required) {
        if (required.compareTo(BigDecimal.ZERO) <= 0) {
            return 100;
        }
        int percent = total.multiply(new BigDecimal("100"))
                .divide(required, 0, RoundingMode.DOWN).intValue();
        return Math.min(percent, 100);
    }
}
