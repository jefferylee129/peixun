package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soe.training.common.BusinessException;
import com.soe.training.domain.Course;
import com.soe.training.domain.LearningMap;
import com.soe.training.domain.SysUser;
import com.soe.training.mapper.CourseMapper;
import com.soe.training.mapper.HourRecordMapper;
import com.soe.training.mapper.LearningMapMapper;
import com.soe.training.mapper.UserMapper;
import com.soe.training.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 岗位学习地图(年度必修课包)
 * 对标企业学习平台"岗位学习地图/必修课": 按人员类型配置年度必修课程,
 * 员工看到逐课完成状态与完成率, 管理端按人员类型看完成率。
 * 完成口径: 该课程存在已认定学时(与学时台账唯一口径一致, 不另设口径)。
 */
@Service
@RequiredArgsConstructor
public class LearningMapService {

    public static final List<String> USER_TYPES = List.of("manager", "professional", "staff");

    private final LearningMapMapper learningMapMapper;
    private final CourseMapper courseMapper;
    private final HourRecordMapper hourRecordMapper;
    private final UserMapper userMapper;

    /** 管理端配置矩阵: 全部已发布课程 × 三类人员类型, 标记是否已设为必修 */
    public Map<String, Object> matrix(Integer year) {
        int y = yearOf(year);
        Map<Long, Set<String>> configured = new HashMap<>();
        for (LearningMap m : learningMapMapper.selectList(new LambdaQueryWrapper<LearningMap>()
                .eq(LearningMap::getYear, y))) {
            configured.computeIfAbsent(m.getCourseId(), k -> new HashSet<>()).add(m.getUserType());
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Course c : courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, "2")
                .orderByAsc(Course::getId))) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("courseId", c.getId());
            row.put("courseCode", c.getCourseCode());
            row.put("courseName", c.getCourseName());
            row.put("category", c.getCategory());
            row.put("hours", c.getHours());
            row.put("courseType", c.getCourseType());
            Set<String> types = configured.getOrDefault(c.getId(), Set.of());
            row.put("required", types);
            rows.add(row);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("year", y);
        result.put("rows", rows);
        return result;
    }

    /** 覆盖式保存某人员类型某年度的必修课(先清后插, 同一事务) */
    @Transactional
    public int save(Integer year, String userType, List<Long> courseIds) {
        int y = yearOf(year);
        if (userType == null || !USER_TYPES.contains(userType)) {
            throw new BusinessException("无效的人员类型");
        }
        List<Long> ids = courseIds == null ? List.of()
                : courseIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (!ids.isEmpty()) {
            long published = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                    .in(Course::getId, ids)
                    .eq(Course::getStatus, "2"));
            if (published != ids.size()) {
                throw new BusinessException("仅已发布课程可设为必修, 请检查课程状态");
            }
        }
        learningMapMapper.deleteByType(y, userType);
        for (Long cid : ids) {
            LearningMap m = new LearningMap();
            m.setYear(y);
            m.setUserType(userType);
            m.setCourseId(cid);
            learningMapMapper.insert(m);
        }
        return ids.size();
    }

    /** 我的学习地图: 必修课清单 + 逐课完成状态 + 完成率 */
    public Map<String, Object> my(Integer year) {
        return ofUser(AuthContext.getUserId(), year);
    }

    /** 指定人员的学习地图: 按其真实人员类型取必修清单(管理员查看用) */
    public Map<String, Object> ofUser(Long userId, Integer year) {
        SysUser u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        Map<String, Object> result = personMap(userId, u.getUserType(), year);
        result.put("userId", userId);
        result.put("userName", u.getNickname());
        return result;
    }

    /** 指定人员的学习地图(管理员查看/统计复用) */
    public Map<String, Object> personMap(Long userId, String userType, Integer year) {
        int y = yearOf(year);
        List<LearningMap> maps = learningMapMapper.selectList(new LambdaQueryWrapper<LearningMap>()
                .eq(LearningMap::getYear, y)
                .eq(LearningMap::getUserType, userType)
                .orderByAsc(LearningMap::getId));
        Map<String, Object> result = new HashMap<>();
        result.put("year", y);
        result.put("userType", userType);
        List<Map<String, Object>> items = new ArrayList<>();
        if (maps.isEmpty()) {
            result.put("items", items);
            result.put("total", 0);
            result.put("done", 0);
            result.put("percent", 0);
            result.put("configured", false);
            return result;
        }
        Map<Long, Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .in(Course::getId, maps.stream().map(LearningMap::getCourseId).toList()))
                .stream().collect(Collectors.toMap(Course::getId, Function.identity()));
        // 课程可能已下架/删除: 仍列出但标注, 避免必修清单"悄悄消失"
        Set<Long> learned = new HashSet<>(hourRecordMapper.learnedCourseIds(userId,
                maps.stream().map(LearningMap::getCourseId).toList()));
        int done = 0;
        for (LearningMap m : maps) {
            Course c = courses.get(m.getCourseId());
            boolean ok = learned.contains(m.getCourseId());
            if (ok) {
                done++;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("mapId", m.getId());
            item.put("courseId", m.getCourseId());
            item.put("courseName", c == null ? "(课程已删除)" : c.getCourseName());
            item.put("courseCode", c == null ? null : c.getCourseCode());
            item.put("category", c == null ? null : c.getCategory());
            item.put("hours", c == null ? null : c.getHours());
            item.put("courseAvailable", c != null && "2".equals(c.getStatus()));
            item.put("done", ok);
            items.add(item);
        }
        result.put("items", items);
        result.put("total", maps.size());
        result.put("done", done);
        result.put("percent", percent(done, maps.size()));
        result.put("configured", true);
        return result;
    }

    /**
     * 各人员类型必修完成率(管理端统计):
     * completion = 全员逐人完成率的平均; finished = 已全部完成必修的人数。
     * 该年度未配置必修的类型不返回。
     */
    public List<Map<String, Object>> completionStats(Integer year) {
        int y = yearOf(year);
        Set<String> configuredTypes = learningMapMapper.selectList(new LambdaQueryWrapper<LearningMap>()
                        .eq(LearningMap::getYear, y))
                .stream().map(LearningMap::getUserType).collect(Collectors.toSet());
        List<Map<String, Object>> result = new ArrayList<>();
        for (String type : USER_TYPES) {
            if (!configuredTypes.contains(type)) {
                continue;
            }
            int people = 0;
            int finished = 0;
            BigDecimal sum = BigDecimal.ZERO;
            for (SysUser u : userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUserType, type)
                    .eq(SysUser::getStatus, "0"))) {
                Map<String, Object> pm = personMap(u.getId(), type, y);
                int total = (Integer) pm.get("total");
                if (total == 0) {
                    continue;
                }
                people++;
                sum = sum.add(new BigDecimal((Integer) pm.get("percent")));
                if ((Integer) pm.get("done") == total) {
                    finished++;
                }
            }
            Map<String, Object> row = new HashMap<>();
            row.put("userType", type);
            row.put("people", people);
            row.put("finished", finished);
            row.put("avgPercent", people == 0 ? 0
                    : sum.divide(new BigDecimal(people), 0, RoundingMode.HALF_UP).intValue());
            result.add(row);
        }
        return result;
    }

    private int yearOf(Integer year) {
        return year == null ? LocalDate.now().getYear() : year;
    }

    private int percent(int done, int total) {
        return total == 0 ? 0 : done * 100 / total;
    }
}
