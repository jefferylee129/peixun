package com.soe.training.service;

import com.soe.training.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计分析服务
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;
    private final HoursService hoursService;

    public Map<String, Object> overview(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        int activeUsers = statsMapper.countActiveUsers();
        int trainees = statsMapper.countTrainees(y);
        BigDecimal yearHours = statsMapper.sumYearHours(y);
        int planApproved = statsMapper.countPlanApproved(y);
        int planFinished = statsMapper.countPlanFinished();

        Map<String, Object> result = new HashMap<>();
        result.put("year", y);
        result.put("classCount", statsMapper.countClass());
        result.put("classOngoing", statsMapper.countClassOngoing());
        result.put("courseCount", statsMapper.countCoursePublished());
        result.put("trainerCount", statsMapper.countTrainer());
        result.put("pendingHours", statsMapper.countPendingHours());
        result.put("yearHours", yearHours.setScale(1, RoundingMode.HALF_UP));
        result.put("avgUserHours", activeUsers == 0 ? BigDecimal.ZERO
                : yearHours.divide(new BigDecimal(activeUsers), 1, RoundingMode.HALF_UP));
        result.put("coverage", activeUsers == 0 ? 0
                : Math.round(trainees * 10000.0 / activeUsers) / 100.0);
        result.put("planApproved", planApproved);
        result.put("planCompletionRate", planApproved == 0 ? 0
                : Math.round(Math.min(planFinished, planApproved) * 10000.0 / planApproved) / 100.0);
        return result;
    }

    public Map<String, Object> userStatsSafe(Integer year, Long deptId, String keyword) {
        Map<String, Object> result = new HashMap<>();
        result.put("rows", hoursService.userStats(year, deptId, keyword));
        return result;
    }

    public List<Map<String, Object>> monthStats(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        return statsMapper.monthHours(y);
    }

    public List<Map<String, Object>> deptStats(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        return statsMapper.deptHours(y);
    }

    public List<Map<String, Object>> courseCategory() {
        return statsMapper.courseCategory();
    }

    public List<Map<String, Object>> classStatus() {
        return statsMapper.classStatus();
    }

    public List<Map<String, Object>> planStatus(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        return statsMapper.planStatus(y);
    }

    public List<Map<String, Object>> needTypeStats(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        return statsMapper.needTypeStats(y);
    }
}
