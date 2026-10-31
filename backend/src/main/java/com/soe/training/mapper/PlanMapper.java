package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.domain.TrainingPlan;
import com.soe.training.domain.vo.PlanVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PlanMapper extends BaseMapper<TrainingPlan> {

    @Select("<script>" +
            "SELECT p.*, d.dept_name AS deptName FROM biz_plan p " +
            "LEFT JOIN sys_dept d ON d.id = p.dept_id " +
            "WHERE p.del_flag = 0 " +
            "<if test='year != null'> AND p.year = #{year}</if>" +
            "<if test=\"status != null and status != ''\"> AND p.status = #{status}</if>" +
            "<if test=\"planType != null and planType != ''\"> AND p.plan_type = #{planType}</if>" +
            "<if test='deptId != null'> AND p.dept_id = #{deptId}</if>" +
            "<if test=\"keyword != null and keyword != ''\"> AND p.plan_name LIKE CONCAT('%', #{keyword}, '%')</if>" +
            " ORDER BY p.id DESC" +
            "</script>")
    IPage<PlanVo> selectPageVo(Page<PlanVo> page, @Param("year") Integer year,
                               @Param("status") String status, @Param("planType") String planType,
                               @Param("deptId") Long deptId, @Param("keyword") String keyword);
}
