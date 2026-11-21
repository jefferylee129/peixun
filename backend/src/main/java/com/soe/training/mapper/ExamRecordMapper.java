package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soe.training.domain.ExamRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    /**
     * 原子交卷守卫: 仅当尚未交卷时写入成绩, 并发双交卷只有一方生效,
     * 返回受影响行数(0 表示已被并发请求先交卷)。
     */
    @Update("UPDATE biz_exam_record SET answers = #{answers}, score = #{score}, passed = #{passed}, " +
            "submit_time = #{submitTime} WHERE id = #{id} AND submit_time IS NULL")
    int markSubmitted(@Param("id") Long id, @Param("answers") String answers,
                      @Param("score") BigDecimal score, @Param("passed") Integer passed,
                      @Param("submitTime") LocalDateTime submitTime);
}
