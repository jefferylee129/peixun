package com.soe.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soe.training.domain.NotifyLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface NotifyLogMapper extends BaseMapper<NotifyLog> {

    /** 去重窗口内已发送的业务键(一次查询, 避免逐人查询) */
    @Select("<script>" +
            "SELECT biz_key FROM biz_notify_log WHERE notify_type = #{type} AND sent_time &gt; #{since}" +
            "<if test='userIds != null and userIds.size() > 0'> AND user_id IN " +
            "<foreach collection='userIds' item='u' open='(' separator=',' close=')'>#{u}</foreach>" +
            "</if>" +
            "</script>")
    List<String> sentKeys(@Param("type") String type, @Param("since") LocalDateTime since,
                          @Param("userIds") List<Long> userIds);
}
