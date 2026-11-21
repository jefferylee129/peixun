package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.Certificate;
import com.soe.training.domain.SysUser;
import com.soe.training.domain.TrainingClass;
import com.soe.training.domain.vo.StudentVo;
import com.soe.training.mapper.CertificateMapper;
import com.soe.training.mapper.ClassMapper;
import com.soe.training.mapper.ClassStudentMapper;
import com.soe.training.mapper.UserMapper;
import com.soe.training.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结业证书服务(对标国企结业证管理):
 * 班次结班自动对"已完成"学员发证, 编号 QGPX-YYYY-序号, 班次+人员唯一, 可幂等补发。
 */
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateMapper certificateMapper;
    private final ClassMapper classMapper;
    private final ClassStudentMapper studentMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    @Transactional
    public int issueForClass(Long classId) {
        TrainingClass c = classMapper.selectById(classId);
        if (c == null) {
            throw new BusinessException("班次不存在");
        }
        if (!"3".equals(c.getStatus())) {
            throw new BusinessException("班次结业后才能发证");
        }
        int year = LocalDate.now().getYear();
        int n = 0;
        for (StudentVo st : studentMapper.selectByClassId(classId)) {
            if (!"2".equals(st.getStatus())) {
                continue;
            }
            Long dup = certificateMapper.selectCount(new LambdaQueryWrapper<Certificate>()
                    .eq(Certificate::getClassId, classId)
                    .eq(Certificate::getUserId, st.getUserId()));
            if (dup != null && dup > 0) {
                continue;
            }
            Certificate cert = new Certificate();
            cert.setUserId(st.getUserId());
            cert.setClassId(classId);
            cert.setHours(st.getTotalHours() == null ? BigDecimal.ZERO : st.getTotalHours());
            cert.setGrade("合格");
            cert.setIssueDate(LocalDate.now());
            certificateMapper.insert(cert);
            Certificate u = new Certificate();
            u.setId(cert.getId());
            u.setCertNo(String.format("QGPX-%d-%06d", year, cert.getId()));
            certificateMapper.updateById(u);
            messageService.send(st.getUserId(), "cert", "结业证书已颁发",
                    "您在「" + c.getClassName() + "」的结业证书已生成, 编号 " + u.getCertNo()
                            + ", 认定学时 " + cert.getHours() + "。", cert.getId());
            n++;
        }
        return n;
    }

    public PageResult<Map<String, Object>> page(int page, int size, Integer year, String keyword) {
        Page<Certificate> p = PageQuery.of(page, size);
        LambdaQueryWrapper<Certificate> q = new LambdaQueryWrapper<Certificate>()
                .orderByDesc(Certificate::getId);
        if (year != null) {
            q.apply("YEAR(issue_date) = {0}", year);
        }
        if (keyword != null && !keyword.isBlank()) {
            List<Long> uids = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getNickname, keyword))
                    .stream().map(SysUser::getId).toList();
            if (uids.isEmpty()) {
                return PageResult.of(0, List.of());
            }
            q.in(Certificate::getUserId, uids);
        }
        Page<Certificate> result = certificateMapper.selectPage(p, q);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Certificate c : result.getRecords()) {
            rows.add(toRow(c));
        }
        return PageResult.of(result.getTotal(), rows);
    }

    public List<Map<String, Object>> my() {
        List<Certificate> list = certificateMapper.selectList(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getUserId, AuthContext.getUserId())
                .orderByDesc(Certificate::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Certificate c : list) {
            rows.add(toRow(c));
        }
        return rows;
    }

    public Map<String, Object> detail(Long id) {
        Certificate c = certificateMapper.selectById(id);
        if (c == null) {
            throw new BusinessException("证书不存在");
        }
        return toRow(c);
    }

    private Map<String, Object> toRow(Certificate c) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", c.getId());
        row.put("certNo", c.getCertNo());
        row.put("userId", c.getUserId());
        row.put("classId", c.getClassId());
        row.put("hours", c.getHours());
        row.put("grade", c.getGrade());
        row.put("issueDate", c.getIssueDate());
        row.put("createTime", c.getCreateTime());
        SysUser u = userMapper.selectById(c.getUserId());
        row.put("userName", u == null ? null : u.getNickname());
        row.put("employeeNo", u == null ? null : u.getEmployeeNo());
        TrainingClass cls = classMapper.selectById(c.getClassId());
        row.put("className", cls == null ? null : cls.getClassName());
        row.put("classStartDate", cls == null ? null : cls.getStartDate());
        row.put("classEndDate", cls == null ? null : cls.getEndDate());
        return row;
    }
}
