package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.Course;
import com.soe.training.domain.vo.CourseVo;
import com.soe.training.mapper.CourseMapper;
import com.soe.training.mapper.ScheduleMapper;
import com.soe.training.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 培训课程服务(课程库: 起草 → 审核 → 发布 → 下架)
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final ScheduleMapper scheduleMapper;

    public PageResult<CourseVo> page(int page, int size, String category, String courseType,
                                     String status, String keyword) {
        Page<CourseVo> p = PageQuery.of(page, size);
        return PageResult.of(courseMapper.selectPageVo(p, category, courseType, status, keyword));
    }

    @Transactional
    public void save(Course course) {
        if (course.getId() == null) {
            course.setStatus("0");
            courseMapper.insert(course);
        } else {
            Course old = getOrThrow(course.getId());
            if ("2".equals(old.getStatus())) {
                throw new BusinessException("已发布课程不可编辑, 请先下架");
            }
            course.setStatus(null);
            course.setAuditUserId(null);
            course.setAuditTime(null);
            course.setAuditRemark(null);
            // 外部同步课程的来源标识不可被编辑覆盖, 保证幂等入库键稳定
            course.setSourceType(old.getSourceType());
            course.setSourceId(old.getSourceId());
            course.setExternalCode(old.getExternalCode());
            courseMapper.updateById(course);
        }
    }

    public void submit(Long id) {
        Course course = getOrThrow(id);
        if (!"0".equals(course.getStatus()) && !"3".equals(course.getStatus())) {
            throw new BusinessException("仅草稿或已下架课程可提交审核");
        }
        Course u = new Course();
        u.setId(id);
        u.setStatus("1");
        courseMapper.updateById(u);
    }

    public void audit(Long id, boolean pass, String remark) {
        Course course = getOrThrow(id);
        if (!"1".equals(course.getStatus())) {
            throw new BusinessException("该课程不在待审核状态");
        }
        Course u = new Course();
        u.setId(id);
        u.setStatus(pass ? "2" : "0");
        u.setAuditUserId(AuthContext.getUserId());
        u.setAuditTime(LocalDateTime.now());
        u.setAuditRemark(remark);
        courseMapper.updateById(u);
    }

    public void offline(Long id) {
        Course course = getOrThrow(id);
        if (!"2".equals(course.getStatus())) {
            throw new BusinessException("仅已发布课程可下架");
        }
        Course u = new Course();
        u.setId(id);
        u.setStatus("3");
        courseMapper.updateById(u);
    }

    public void delete(Long id) {
        Course course = getOrThrow(id);
        if ("2".equals(course.getStatus())) {
            throw new BusinessException("已发布课程请先下架再删除");
        }
        // 已被班次课表引用的课程不允许删除, 防止历史课表悬空
        Long refs = scheduleMapper.selectCount(
                new LambdaQueryWrapper<com.soe.training.domain.ClassSchedule>()
                        .eq(com.soe.training.domain.ClassSchedule::getCourseId, id));
        if (refs > 0) {
            throw new BusinessException("该课程已被班次课表引用, 不能删除");
        }
        courseMapper.deleteById(id);
    }

    /** 导出用: 不分页拉取(上限5000) */
    public List<CourseVo> listForExport(String category, String courseType, String status, String keyword) {
        Page<CourseVo> p = Page.of(1, 5000, false);
        return courseMapper.selectPageVo(p, category, courseType, status, keyword).getRecords();
    }

    /** 已发布课程下拉选项 */
    public List<Course> published() {
        return courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, "2")
                .orderByDesc(Course::getId));
    }

    private Course getOrThrow(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }
}
