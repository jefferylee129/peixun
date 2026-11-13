package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.SysDept;
import com.soe.training.domain.SysUser;
import com.soe.training.domain.dto.UserSaveDto;
import com.soe.training.domain.ClassStudent;
import com.soe.training.domain.HourRecord;
import com.soe.training.domain.TrainingNeed;
import com.soe.training.mapper.DeptMapper;
import com.soe.training.mapper.UserMapper;
import com.soe.training.mapper.HourRecordMapper;
import com.soe.training.mapper.ClassStudentMapper;
import com.soe.training.mapper.NeedMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final HourRecordMapper hourRecordMapper;
    private final ClassStudentMapper classStudentMapper;
    private final NeedMapper needMapper;

    public PageResult<Map<String, Object>> page(int page, int size, String keyword,
                                                Long deptId, String status) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<SysUser>()
                .select(SysUser.class, f -> !"password".equals(f.getProperty()))
                .and(keyword != null && !keyword.isBlank(), q -> q
                        .like(SysUser::getNickname, keyword)
                        .or().like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getEmployeeNo, keyword))
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .eq(status != null && !status.isBlank(), SysUser::getStatus, status)
                .orderByAsc(SysUser::getId);
        Page<SysUser> p = userMapper.selectPage(PageQuery.of(page, size), w);

        Set<Long> deptIds = p.getRecords().stream()
                .map(SysUser::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> deptNames = deptIds.isEmpty() ? Map.of()
                : deptMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));

        List<Map<String, Object>> rows = p.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("deptId", u.getDeptId());
            m.put("deptName", deptNames.get(u.getDeptId()));
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("employeeNo", u.getEmployeeNo());
            m.put("gender", u.getGender());
            m.put("phone", u.getPhone());
            m.put("email", u.getEmail());
            m.put("position", u.getPosition());
            m.put("userType", u.getUserType());
            m.put("status", u.getStatus());
            m.put("remark", u.getRemark());
            m.put("createTime", u.getCreateTime());
            return m;
        }).toList();
        return PageResult.of(p.getTotal(), rows);
    }

    public Map<String, Object> detail(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("deptId", u.getDeptId());
        m.put("username", u.getUsername());
        m.put("nickname", u.getNickname());
        m.put("employeeNo", u.getEmployeeNo());
        m.put("gender", u.getGender());
        m.put("phone", u.getPhone());
        m.put("email", u.getEmail());
        m.put("position", u.getPosition());
        m.put("userType", u.getUserType());
        m.put("status", u.getStatus());
        m.put("remark", u.getRemark());
        m.put("roleIds", userMapper.selectRoleIds(id));
        return m;
    }

    @Transactional
    public void save(UserSaveDto dto) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new BusinessException("登录账号不能为空");
        }
        Long dup = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername())
                .ne(dto.getId() != null, SysUser::getId, dto.getId()));
        if (dup > 0) {
            throw new BusinessException("登录账号已存在: " + dto.getUsername());
        }
        if (dto.getId() == null) {
            String rawPwd = (dto.getPassword() == null || dto.getPassword().isBlank())
                    ? "123456" : dto.getPassword();
            dto.setPassword(passwordEncoder.encode(rawPwd));
            userMapper.insert(dto);
            assignRoles(dto.getId(), dto.getRoleIds());
        } else {
            if (dto.getPassword() == null || dto.getPassword().isBlank()) {
                dto.setPassword(null);
            } else {
                dto.setPassword(passwordEncoder.encode(dto.getPassword()));
            }
            userMapper.updateById(dto);
            // 仅当显式传入角色时才重新分配, 避免未传 roleIds 的更新把用户角色清空
            if (dto.getRoleIds() != null) {
                assignRoles(dto.getId(), dto.getRoleIds());
            }
        }
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userMapper.deleteUserRole(userId);
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                userMapper.insertUserRole(userId, roleId);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        if (id == 1L) {
            throw new BusinessException("内置管理员不允许删除");
        }
        if (userMapper.selectById(id) == null) {
            throw new BusinessException("用户不存在");
        }
        // 有关联业务数据的用户不删, 避免台账/报名/需求悬空
        if (hourRecordMapper.selectCount(new LambdaQueryWrapper<HourRecord>()
                .eq(HourRecord::getUserId, id)) > 0) {
            throw new BusinessException("该用户存在学时台账记录, 不能删除");
        }
        if (classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getUserId, id)) > 0) {
            throw new BusinessException("该用户存在班次报名记录, 不能删除");
        }
        if (needMapper.selectCount(new LambdaQueryWrapper<TrainingNeed>()
                .eq(TrainingNeed::getUserId, id)) > 0) {
            throw new BusinessException("该用户存在培训需求记录, 不能删除");
        }
        userMapper.deleteById(id);
        userMapper.deleteUserRole(id);
    }

    @Transactional
    public String resetPassword(Long id) {
        if (id == 1L) {
            throw new BusinessException("内置管理员请通过右上角修改密码自助更换");
        }
        String raw = "123456";
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(raw));
        userMapper.updateById(update);
        return raw;
    }

    public void changeStatus(Long id, String status) {
        if (!"0".equals(status) && !"1".equals(status)) {
            throw new BusinessException("状态仅允许 0启用 / 1停用");
        }
        if (id == 1L && "1".equals(status)) {
            throw new BusinessException("内置管理员不允许停用");
        }
        if (userMapper.selectById(id) == null) {
            throw new BusinessException("用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
    }

    /** 导出用: 不分页拉取(上限5000), 含部门名称 */
    public List<Map<String, Object>> listForExport(String keyword, Long deptId, String status) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<SysUser>()
                .select(SysUser.class, f -> !"password".equals(f.getProperty()))
                .and(keyword != null && !keyword.isBlank(), q -> q
                        .like(SysUser::getNickname, keyword)
                        .or().like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getEmployeeNo, keyword))
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .eq(status != null && !status.isBlank(), SysUser::getStatus, status)
                .orderByAsc(SysUser::getId)
                .last("LIMIT 5000");
        List<SysUser> users = userMapper.selectList(w);
        Set<Long> deptIds = users.stream()
                .map(SysUser::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> deptNames = deptIds.isEmpty() ? Map.of()
                : deptMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));
        return users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("employeeNo", u.getEmployeeNo());
            m.put("deptName", deptNames.get(u.getDeptId()));
            m.put("position", u.getPosition());
            m.put("userType", u.getUserType());
            m.put("gender", u.getGender());
            m.put("phone", u.getPhone());
            m.put("email", u.getEmail());
            m.put("status", u.getStatus());
            return m;
        }).toList();
    }

    /** 简单用户下拉列表(选择学员用) */
    public List<Map<String, Object>> simpleList(Long deptId, String keyword) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<SysUser>()
                .select(SysUser.class, f -> !"password".equals(f.getProperty()))
                .eq(SysUser::getStatus, "0")
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .and(keyword != null && !keyword.isBlank(), q -> q
                        .like(SysUser::getNickname, keyword)
                        .or().like(SysUser::getEmployeeNo, keyword))
                .orderByAsc(SysUser::getId);
        return userMapper.selectList(w).stream().limit(200).map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("nickname", u.getNickname());
            m.put("employeeNo", u.getEmployeeNo());
            m.put("deptId", u.getDeptId());
            m.put("userType", u.getUserType());
            return m;
        }).toList();
    }
}
