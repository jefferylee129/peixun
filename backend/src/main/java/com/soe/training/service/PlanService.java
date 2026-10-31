package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.TrainingPlan;
import com.soe.training.domain.vo.PlanVo;
import com.soe.training.mapper.PlanMapper;
import com.soe.training.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 培训计划服务(编制 → 提交 → 审批下达)
 */
@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanMapper planMapper;

    public PageResult<PlanVo> page(int page, int size, Integer year, String status,
                                   String planType, Long deptId, String keyword) {
        Page<PlanVo> p = PageQuery.of(page, size);
        return PageResult.of(planMapper.selectPageVo(p, year, status, planType, deptId, keyword));
    }

    @Transactional
    public void save(TrainingPlan plan) {
        if (plan.getYear() == null) {
            plan.setYear(LocalDate.now().getYear());
        }
        if (plan.getId() == null) {
            plan.setStatus("0");
            planMapper.insert(plan);
        } else {
            TrainingPlan old = getOrThrow(plan.getId());
            if (!"0".equals(old.getStatus()) && !"3".equals(old.getStatus())) {
                throw new BusinessException("仅草稿或已驳回的计划可编辑");
            }
            plan.setStatus(null);
            plan.setSubmitTime(null);
            plan.setApproveUserId(null);
            plan.setApproveTime(null);
            plan.setApproveRemark(null);
            planMapper.updateById(plan);
        }
    }

    public void submit(Long id) {
        TrainingPlan plan = getOrThrow(id);
        if (!"0".equals(plan.getStatus()) && !"3".equals(plan.getStatus())) {
            throw new BusinessException("仅草稿或已驳回的计划可提交审批");
        }
        TrainingPlan u = new TrainingPlan();
        u.setId(id);
        u.setStatus("1");
        u.setSubmitTime(LocalDateTime.now());
        planMapper.updateById(u);
    }

    public void approve(Long id, boolean pass, String remark) {
        TrainingPlan plan = getOrThrow(id);
        if (!"1".equals(plan.getStatus())) {
            throw new BusinessException("该计划不在待审批状态");
        }
        TrainingPlan u = new TrainingPlan();
        u.setId(id);
        u.setStatus(pass ? "2" : "3");
        u.setApproveUserId(AuthContext.getUserId());
        u.setApproveTime(LocalDateTime.now());
        u.setApproveRemark(remark);
        planMapper.updateById(u);
    }

    public void delete(Long id) {
        TrainingPlan plan = getOrThrow(id);
        if (!"0".equals(plan.getStatus()) && !"3".equals(plan.getStatus())) {
            throw new BusinessException("仅草稿或已驳回的计划可删除");
        }
        planMapper.deleteById(id);
    }

    /** 导出用: 不分页拉取(上限5000) */
    public List<PlanVo> listForExport(Integer year, String status, String planType, Long deptId, String keyword) {
        Page<PlanVo> p = Page.of(1, 5000, false);
        return planMapper.selectPageVo(p, year, status, planType, deptId, keyword).getRecords();
    }

    /** 已批准计划下拉选项 */
    public List<TrainingPlan> options(Integer year) {
        return planMapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getStatus, "2")
                .eq(year != null, TrainingPlan::getYear, year)
                .orderByDesc(TrainingPlan::getId));
    }

    private TrainingPlan getOrThrow(Long id) {
        TrainingPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("培训计划不存在");
        }
        return plan;
    }
}
