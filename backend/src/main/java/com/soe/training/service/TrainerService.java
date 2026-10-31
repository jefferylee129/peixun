package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.Trainer;
import com.soe.training.mapper.ScheduleMapper;
import com.soe.training.mapper.TrainerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 培训师资服务
 */
@Service
@RequiredArgsConstructor
public class TrainerService {

    private final TrainerMapper trainerMapper;
    private final ScheduleMapper scheduleMapper;

    public PageResult<Trainer> page(int page, int size, String keyword, String trainerType, String status) {
        LambdaQueryWrapper<Trainer> w = new LambdaQueryWrapper<Trainer>()
                .and(keyword != null && !keyword.isBlank(), q -> q
                        .like(Trainer::getTrainerName, keyword)
                        .or().like(Trainer::getSpecialty, keyword)
                        .or().like(Trainer::getOrgName, keyword))
                .eq(trainerType != null && !trainerType.isBlank(), Trainer::getTrainerType, trainerType)
                .eq(status != null && !status.isBlank(), Trainer::getStatus, status)
                .orderByDesc(Trainer::getId);
        Page<Trainer> p = trainerMapper.selectPage(PageQuery.of(page, size), w);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public List<Trainer> allActive() {
        return trainerMapper.selectList(new LambdaQueryWrapper<Trainer>()
                .eq(Trainer::getStatus, "0").orderByDesc(Trainer::getId));
    }

    @Transactional
    public void save(Trainer trainer) {
        if (trainer.getId() == null) {
            trainer.setStatus("0");
            trainerMapper.insert(trainer);
        } else {
            trainerMapper.updateById(trainer);
        }
    }

    @Transactional
    public void delete(Long id) {
        Long schedules = scheduleMapper.selectCount(
                new LambdaQueryWrapper<com.soe.training.domain.ClassSchedule>()
                        .eq(com.soe.training.domain.ClassSchedule::getTrainerId, id));
        if (schedules > 0) {
            throw new BusinessException("该讲师已有授课安排, 不能删除");
        }
        trainerMapper.deleteById(id);
    }

    /** 导出用: 不分页拉取(上限5000) */
    public List<Trainer> listForExport(String keyword, String trainerType, String status) {
        LambdaQueryWrapper<Trainer> w = new LambdaQueryWrapper<Trainer>()
                .and(keyword != null && !keyword.isBlank(), q -> q
                        .like(Trainer::getTrainerName, keyword)
                        .or().like(Trainer::getSpecialty, keyword)
                        .or().like(Trainer::getOrgName, keyword))
                .eq(trainerType != null && !trainerType.isBlank(), Trainer::getTrainerType, trainerType)
                .eq(status != null && !status.isBlank(), Trainer::getStatus, status)
                .orderByDesc(Trainer::getId)
                .last("LIMIT 5000");
        return trainerMapper.selectList(w);
    }

    /** 师资详情 + 授课统计 */
    public Map<String, Object> detail(Long id) {
        Trainer trainer = trainerMapper.selectById(id);
        if (trainer == null) {
            throw new BusinessException("师资不存在");
        }
        List<Map<String, Object>> teaching = trainerMapper.selectTeachingRecords(id);
        BigDecimal totalHours = BigDecimal.ZERO;
        int finishedCount = 0;
        for (Map<String, Object> row : teaching) {
            Object hours = row.get("hours");
            if ("1".equals(String.valueOf(row.get("scheduleStatus"))) && hours != null) {
                totalHours = totalHours.add(new BigDecimal(hours.toString()));
            }
            if ("1".equals(String.valueOf(row.get("scheduleStatus")))) {
                finishedCount++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("trainer", trainer);
        result.put("teachingRecords", teaching);
        result.put("teachCount", finishedCount);
        result.put("totalHours", totalHours.setScale(1, RoundingMode.HALF_UP));
        return result;
    }
}
