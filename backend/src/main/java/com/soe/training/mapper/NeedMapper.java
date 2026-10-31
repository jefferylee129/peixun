package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.domain.TrainingNeed;
import com.soe.training.domain.vo.NeedVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface NeedMapper extends BaseMapper<TrainingNeed> {

    @Select("<script>" +
            "SELECT n.*, d.dept_name AS deptName, u.nickname AS userName FROM biz_need n " +
            "LEFT JOIN sys_dept d ON d.id = n.dept_id " +
            "LEFT JOIN sys_user u ON u.id = n.user_id " +
            "WHERE n.del_flag = 0 " +
            "<if test='year != null'> AND n.year = #{year}</if>" +
            "<if test=\"status != null and status != ''\"> AND n.status = #{status}</if>" +
            "<if test=\"needType != null and needType != ''\"> AND n.need_type = #{needType}</if>" +
            "<if test='deptId != null'> AND n.dept_id = #{deptId}</if>" +
            "<if test=\"keyword != null and keyword != ''\"> AND (n.title LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "<if test='onlyMine'> AND n.user_id = #{userId}</if>" +
            " ORDER BY n.id DESC" +
            "</script>")
    IPage<NeedVo> selectPageVo(Page<NeedVo> page, @Param("year") Integer year,
                               @Param("status") String status, @Param("needType") String needType,
                               @Param("deptId") Long deptId, @Param("keyword") String keyword,
                               @Param("onlyMine") boolean onlyMine, @Param("userId") Long userId);
}
