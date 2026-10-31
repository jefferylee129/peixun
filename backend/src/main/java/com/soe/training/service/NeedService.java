package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.NeedCollection;
import com.soe.training.domain.SysUser;
import com.soe.training.domain.TrainingNeed;
import com.soe.training.domain.vo.NeedVo;
import com.soe.training.mapper.NeedCollectionMapper;
import com.soe.training.mapper.NeedMapper;
import com.soe.training.mapper.UserMapper;
import com.soe.training.security.AuthContext;
import com.soe.training.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 培训需求服务(需求征集 → 需求申报 → 需求审核)
 */
@Service
@RequiredArgsConstructor
public class NeedService {

    private final NeedMapper needMapper;
    private final NeedCollectionMapper collectionMapper;
    private final UserMapper userMapper;

    public PageResult<NeedVo> page(int page, int size, Integer year, String status,
                                   String needType, Long deptId, String keyword) {
        Page<NeedVo> p = PageQuery.of(page, size);
        return PageResult.of(needMapper.selectPageVo(p, year, status, needType, deptId, keyword, false, null));
    }

    public PageResult<NeedVo> my(int page, int size, Integer year, String status, String keyword) {
        Long userId = AuthContext.getUserId();
        Page<NeedVo> p = PageQuery.of(page, size);
        return PageResult.of(needMapper.selectPageVo(p, year, status, null, null, keyword, true, userId));
    }

    @Transactional
    public void save(TrainingNeed need) {
        LoginUser ctx = AuthContext.get();
        if (need.getId() == null) {
            need.setUserId(ctx.getUserId());
            if (need.getDeptId() == null) {
                SysUser u = userMapper.selectById(ctx.getUserId());
                need.setDeptId(u == null ? null : u.getDeptId());
            }
            if (need.getYear() == null) {
                need.setYear(LocalDate.now().getYear());
            }
            need.setStatus("0");
            needMapper.insert(need);
        } else {
            TrainingNeed old = getOrThrow(need.getId());
            if (!ctx.isAdmin() && !old.getUserId().equals(ctx.getUserId())) {
                throw new BusinessException("只能修改本人的需求");
            }
            if (!"0".equals(old.getStatus()) && !"3".equals(old.getStatus())) {
                throw new BusinessException("当前状态不可编辑");
            }
            // 属主不可转移: 管理员代改也不改变申报人
            need.setUserId(null);
            need.setStatus(null);
            need.setAuditUserId(null);
            need.setAuditTime(null);
            need.setAuditRemark(null);
            needMapper.updateById(need);
        }
    }

    public void submit(Long id) {
        TrainingNeed need = getOrThrow(id);
        LoginUser ctx = AuthContext.get();
        if (!ctx.isAdmin() && !need.getUserId().equals(ctx.getUserId())) {
            throw new BusinessException("只能提交本人的需求");
        }
        if (!"0".equals(need.getStatus())) {
            throw new BusinessException("仅草稿状态可提交审核");
        }
        TrainingNeed u = new TrainingNeed();
        u.setId(id);
        u.setStatus("1");
        needMapper.updateById(u);
    }

    public void audit(Long id, boolean adopt, String remark) {
        TrainingNeed need = getOrThrow(id);
        if (!"1".equals(need.getStatus())) {
            throw new BusinessException("该需求不在待审核状态");
        }
        TrainingNeed u = new TrainingNeed();
        u.setId(id);
        u.setStatus(adopt ? "2" : "3");
        u.setAuditUserId(AuthContext.getUserId());
        u.setAuditTime(LocalDateTime.now());
        u.setAuditRemark(remark);
        needMapper.updateById(u);
    }

    public void delete(Long id) {
        TrainingNeed need = getOrThrow(id);
        LoginUser ctx = AuthContext.get();
        if (!ctx.isAdmin() && !need.getUserId().equals(ctx.getUserId())) {
            throw new BusinessException("只能删除本人的需求");
        }
        // 已提交/已采纳的需求不可删除, 保证审核留痕完整
        if (!"0".equals(need.getStatus()) && !"3".equals(need.getStatus())) {
            throw new BusinessException("已提交或已采纳的需求不可删除");
        }
        needMapper.deleteById(id);
    }

    private TrainingNeed getOrThrow(Long id) {
        TrainingNeed need = needMapper.selectById(id);
        if (need == null) {
            throw new BusinessException("培训需求不存在");
        }
        return need;
    }

    /** 导出用: 不分页拉取(上限5000) */
    public List<NeedVo> listForExport(Integer year, String status, String needType, Long deptId, String keyword) {
        Page<NeedVo> p = Page.of(1, 5000, false);
        return needMapper.selectPageVo(p, year, status, needType, deptId, keyword, false, null).getRecords();
    }

    public List<NeedVo> listMyForExport(Integer year, String status, String keyword) {
        Long userId = AuthContext.getUserId();
        Page<NeedVo> p = Page.of(1, 5000, false);
        return needMapper.selectPageVo(p, year, status, null, null, keyword, true, userId).getRecords();
    }

    // ---------- 需求征集任务 ----------

    public List<NeedCollection> collections(String status) {
        return collectionMapper.selectList(new LambdaQueryWrapper<NeedCollection>()
                .eq(status != null && !status.isBlank(), NeedCollection::getStatus, status)
                .orderByDesc(NeedCollection::getId));
    }

    public List<NeedCollection> activeCollections() {
        return collectionMapper.selectList(new LambdaQueryWrapper<NeedCollection>()
                .eq(NeedCollection::getStatus, "0")
                .orderByDesc(NeedCollection::getId));
    }

    public void saveCollection(NeedCollection c) {
        if (c.getYear() == null) {
            c.setYear(LocalDate.now().getYear());
        }
        if (c.getId() == null) {
            c.setStatus("0");
            collectionMapper.insert(c);
        } else {
            collectionMapper.updateById(c);
        }
    }

    public void closeCollection(Long id) {
        NeedCollection c = collectionMapper.selectById(id);
        if (c == null) {
            throw new BusinessException("征集任务不存在");
        }
        NeedCollection u = new NeedCollection();
        u.setId(id);
        u.setStatus("1");
        collectionMapper.updateById(u);
    }

    public void deleteCollection(Long id) {
        collectionMapper.deleteById(id);
    }
}
