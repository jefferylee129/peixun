package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.soe.training.common.BusinessException;
import com.soe.training.common.PageQuery;
import com.soe.training.common.PageResult;
import com.soe.training.domain.BizMessage;
import com.soe.training.domain.SysUser;
import com.soe.training.mapper.MessageMapper;
import com.soe.training.mapper.UserMapper;
import com.soe.training.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 站内消息服务(达标预警 / 开班提醒 / 评估催办 / 证书 / 考试 / 系统公告)
 * 对标企业学习平台的消息中心: 业务动作自动触达, 未读红点引导待办。
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    public PageResult<BizMessage> my(int page, int size, String type, Integer unread) {
        Page<BizMessage> p = PageQuery.of(page, size);
        return PageResult.of(messageMapper.selectPage(p, new LambdaQueryWrapper<BizMessage>()
                .eq(BizMessage::getUserId, AuthContext.getUserId())
                .eq(type != null && !type.isBlank(), BizMessage::getMsgType, type)
                .eq(unread != null && unread == 1, BizMessage::getIsRead, 0)
                .orderByDesc(BizMessage::getId)));
    }

    public long unreadCount() {
        Long c = messageMapper.selectCount(new LambdaQueryWrapper<BizMessage>()
                .eq(BizMessage::getUserId, AuthContext.getUserId())
                .eq(BizMessage::getIsRead, 0));
        return c == null ? 0 : c;
    }

    public void read(Long id) {
        BizMessage m = messageMapper.selectById(id);
        if (m == null || !m.getUserId().equals(AuthContext.getUserId())) {
            throw new BusinessException("消息不存在");
        }
        BizMessage u = new BizMessage();
        u.setId(id);
        u.setIsRead(1);
        messageMapper.updateById(u);
    }

    public void readAll() {
        BizMessage u = new BizMessage();
        u.setIsRead(1);
        messageMapper.update(u, new LambdaQueryWrapper<BizMessage>()
                .eq(BizMessage::getUserId, AuthContext.getUserId())
                .eq(BizMessage::getIsRead, 0));
    }

    @Transactional
    public void send(Long userId, String type, String title, String content, Long refId) {
        BizMessage m = new BizMessage();
        m.setUserId(userId);
        m.setMsgType(type);
        m.setTitle(title);
        m.setContent(content);
        m.setRefId(refId);
        m.setIsRead(0);
        messageMapper.insert(m);
    }

    @Transactional
    public void sendBatch(List<Long> userIds, String type, String title, String content, Long refId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long uid : userIds) {
            send(uid, type, title, content, refId);
        }
    }

    /** 全体在册用户ID(启用且未删除) */
    public List<Long> activeUserIds() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, "0"))
                .stream().map(SysUser::getId).toList();
    }

    /** 全员系统公告(标题/内容长度与列宽对齐, 超长返回400而非500) */
    @Transactional
    public int announce(String title, String content) {
        if (title != null && title.length() > 100) {
            throw new BusinessException("公告标题过长(最多100字)");
        }
        if (content != null && content.length() > 1000) {
            throw new BusinessException("公告内容过长(最多1000字)");
        }
        List<Long> ids = activeUserIds();
        sendBatch(ids, "system", title, content, null);
        return ids.size();
    }
}
