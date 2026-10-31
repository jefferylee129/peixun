package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soe.training.domain.EvalRecord;
import com.soe.training.domain.vo.EvalRecordVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface EvalRecordMapper extends BaseMapper<EvalRecord> {

    @Select("<script>" +
            "SELECT r.*, u.nickname, d.dept_name AS deptName FROM biz_eval_record r " +
            "JOIN sys_user u ON u.id = r.user_id " +
            "LEFT JOIN sys_dept d ON d.id = u.dept_id " +
            "WHERE r.del_flag = 0 AND r.class_id = #{classId} " +
            "<if test='templateId != null'> AND r.template_id = #{templateId}</if>" +
            " ORDER BY r.submit_time DESC" +
            "</script>")
    List<EvalRecordVo> selectByClass(@Param("classId") Long classId, @Param("templateId") Long templateId);

    @Select("SELECT * FROM biz_eval_record WHERE del_flag = 0 AND class_id = #{classId} " +
            "AND template_id = #{templateId} AND user_id = #{userId}")
    EvalRecord selectOne(@Param("classId") Long classId, @Param("templateId") Long templateId,
                         @Param("userId") Long userId);
}
