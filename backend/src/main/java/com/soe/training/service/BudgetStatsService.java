package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soe.training.domain.SysDept;
import com.soe.training.domain.TrainingPlan;
import com.soe.training.mapper.DeptMapper;
import com.soe.training.mapper.PlanMapper;
import com.soe.training.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 培训经费预算执行统计(国企经费合规看板)
 *
 * 口径(与页面说明保持一致, 避免误读为财务实付):
 *  - 计划预算: 当年已批准(status=2)培训计划的预算合计;
 *  - 班次经费: 当年开班(按开班日期归年)且未取消(status<>4)的班次经费合计;
 *  - 执行率 = 班次经费 / 计划预算; 计划预算为 0 而有班次经费时标记为超预算。
 */
@Service
@RequiredArgsConstructor
public class BudgetStatsService {

    private final StatsMapper statsMapper;
    private final DeptMapper deptMapper;
    private final PlanMapper planMapper;

    public Map<String, Object> stats(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;

        Map<Long, BigDecimal> planByDept = new HashMap<>();
        BigDecimal planTotal = BigDecimal.ZERO;
        for (Map<String, Object> r : statsMapper.planBudgetByDept(y)) {
            BigDecimal amt = dec(r.get("amount"));
            Long deptId = ((Number) r.get("deptId")).longValue();
            planByDept.merge(deptId, amt, BigDecimal::add);
            planTotal = planTotal.add(amt);
        }

        Map<Long, BigDecimal> classByDept = new HashMap<>();
        BigDecimal classTotal = BigDecimal.ZERO;
        for (Map<String, Object> r : statsMapper.classBudgetByDept(y)) {
            BigDecimal amt = dec(r.get("amount"));
            Long deptId = ((Number) r.get("deptId")).longValue();
            classByDept.merge(deptId, amt, BigDecimal::add);
            classTotal = classTotal.add(amt);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("year", y);
        result.put("planBudget", planTotal);
        result.put("classBudget", classTotal);
        result.put("usageRate", rate(classTotal, planTotal));

        // 按主办部门
        Map<Long, String> deptNames = new HashMap<>();
        for (SysDept d : deptMapper.selectList(new LambdaQueryWrapper<>())) {
            deptNames.put(d.getId(), d.getDeptName());
        }
        List<Map<String, Object>> byDept = new ArrayList<>();
        for (Long deptId : union(planByDept.keySet(), classByDept.keySet())) {
            BigDecimal p = planByDept.getOrDefault(deptId, BigDecimal.ZERO);
            BigDecimal c = classByDept.getOrDefault(deptId, BigDecimal.ZERO);
            Map<String, Object> row = new HashMap<>();
            row.put("deptId", deptId);
            row.put("deptName", deptNames.getOrDefault(deptId, deptId == 0 ? "未指定部门" : "部门" + deptId));
            row.put("planBudget", p);
            row.put("classBudget", c);
            row.put("usageRate", rate(c, p));
            row.put("over", c.compareTo(p) > 0);
            byDept.add(row);
        }
        byDept.sort((a, b) -> ((BigDecimal) b.get("classBudget")).compareTo((BigDecimal) a.get("classBudget")));
        result.put("byDept", byDept);

        // 按计划
        Map<Long, BigDecimal> classByPlan = new HashMap<>();
        Map<Long, Long> countByPlan = new HashMap<>();
        for (Map<String, Object> r : statsMapper.classBudgetByPlan(y)) {
            Long planId = ((Number) r.get("planId")).longValue();
            classByPlan.merge(planId, dec(r.get("amount")), BigDecimal::add);
            countByPlan.merge(planId, ((Number) r.get("cnt")).longValue(), Long::sum);
        }
        Map<Long, TrainingPlan> plans = new HashMap<>();
        for (TrainingPlan p : planMapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getYear, y)
                .eq(TrainingPlan::getStatus, "2"))) {
            plans.put(p.getId(), p);
        }
        List<Map<String, Object>> byPlan = new ArrayList<>();
        for (TrainingPlan p : plans.values()) {
            BigDecimal pb = p.getBudget() == null ? BigDecimal.ZERO : p.getBudget();
            BigDecimal cb = classByPlan.getOrDefault(p.getId(), BigDecimal.ZERO);
            Map<String, Object> row = new HashMap<>();
            row.put("planId", p.getId());
            row.put("planName", p.getPlanName());
            row.put("planType", p.getPlanType());
            row.put("planBudget", pb);
            row.put("classBudget", cb);
            row.put("classCount", countByPlan.getOrDefault(p.getId(), 0L));
            row.put("usageRate", rate(cb, pb));
            row.put("over", cb.compareTo(pb) > 0);
            byPlan.add(row);
        }
        // 归属其他年度计划但今年开班的班次, 单列为"跨年计划班次", 不混入计划外
        BigDecimal crossYear = BigDecimal.ZERO;
        for (Long planId : classByPlan.keySet()) {
            if (planId != 0 && !plans.containsKey(planId)) {
                crossYear = crossYear.add(classByPlan.get(planId));
            }
        }
        byPlan.sort((a, b) -> ((BigDecimal) b.get("planBudget")).compareTo((BigDecimal) a.get("planBudget")));
        result.put("byPlan", byPlan);
        result.put("unplannedBudget", classByPlan.getOrDefault(0L, BigDecimal.ZERO));
        result.put("crossYearBudget", crossYear);
        return result;
    }

    private BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    /** 执行率(%); 计划预算为 0 时: 有支出返回 null 并由 over 标识, 无支出返回 0 */
    private Integer rate(BigDecimal used, BigDecimal planned) {
        if (planned == null || planned.compareTo(BigDecimal.ZERO) <= 0) {
            return used != null && used.compareTo(BigDecimal.ZERO) > 0 ? null : 0;
        }
        return used.multiply(new BigDecimal("100"))
                .divide(planned, 0, RoundingMode.HALF_UP).intValue();
    }

    private List<Long> union(java.util.Set<Long> a, java.util.Set<Long> b) {
        java.util.LinkedHashSet<Long> s = new java.util.LinkedHashSet<>(a);
        s.addAll(b);
        return new ArrayList<>(s);
    }
}
