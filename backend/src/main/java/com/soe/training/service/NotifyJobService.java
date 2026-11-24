package com.soe.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.soe.training.domain.EvalRecord;
import com.soe.training.domain.NotifyLog;
import com.soe.training.domain.TrainingClass;
import com.soe.training.domain.vo.StudentVo;
import com.soe.training.mapper.ClassMapper;
import com.soe.training.mapper.ClassStudentMapper;
import com.soe.training.mapper.EvalRecordMapper;
import com.soe.training.mapper.NotifyLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 定时自动提醒(对标企业学习平台"系统自动触达", 替代人工逐个催办)
 *
 * 每日 08:30 扫描三类事项并发站内消息:
 *  1. classStart  开班提醒: N 天内开班的班次, 提醒已报名学员(默认提前 3 天)
 *  2. evalPending 评估催办: 结班满 N 天仍未评估的学员(默认 3 天)
 *  3. hourWarn    学时预警: 按黄/橙/红规则(复用 ArchiveService), 约每月一次
 *
 * 幂等: 同类提醒同一业务对象在去重窗口内不重复发送(biz_notify_log)。
 * 开关: sys_config notify.enabled=0 时定时任务不执行; 管理员手动触发不受开关限制。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyJobService {

    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("M月d日");

    private final ClassMapper classMapper;
    private final ClassStudentMapper studentMapper;
    private final EvalRecordMapper evalRecordMapper;
    private final NotifyLogMapper notifyLogMapper;
    private final MessageService messageService;
    private final ArchiveService archiveService;
    private final ConfigService configService;

    /** 每日 08:30 自动执行 */
    @Scheduled(cron = "0 30 8 * * ?")
    public void scheduled() {
        if (!"1".equals(configService.getValue("notify.enabled", "1"))) {
            return;
        }
        try {
            Map<String, Object> r = runDaily();
            log.info("定时提醒完成: {}", r);
        } catch (Exception e) {
            log.error("定时提醒执行失败", e);
        }
    }

    /** 立即执行一轮扫描, 返回各类发送条数(管理员手动触发入口) */
    public Map<String, Object> runDaily() {
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new HashMap<>();
        result.put("date", today.toString());
        result.put("classStart", remindClassStart(today));
        result.put("evalPending", remindEvalPending(today));
        result.put("hourWarn", remindHourWarn());
        return result;
    }

    private int remindClassStart(LocalDate today) {
        int days = intConfig("notify.class.remind.days", 3);
        LocalDate until = today.plusDays(Math.max(days, 0));
        List<TrainingClass> classes = classMapper.selectList(new LambdaQueryWrapper<TrainingClass>()
                .in(TrainingClass::getStatus, "1", "2")
                .between(TrainingClass::getStartDate, today, until));
        if (classes.isEmpty()) {
            return 0;
        }
        List<Pending> all = new ArrayList<>();
        for (TrainingClass c : classes) {
            for (StudentVo st : studentMapper.selectByClassId(c.getId())) {
                if (!"0".equals(st.getStatus())) {
                    continue;
                }
                String open = c.getStartDate() == null ? "" : c.getStartDate().format(MD);
                all.add(new Pending("classStart", "classStart:" + c.getId() + ":user:" + st.getUserId(),
                        st.getUserId(), "开班提醒",
                        "您报名的「" + c.getClassName() + "」将于 " + open + " 开班"
                                + (c.getLocation() == null ? "" : ", 地点: " + c.getLocation())
                                + ", 请准时参加。", c.getId()));
            }
        }
        return dispatch("classStart", all, 7);
    }

    private int remindEvalPending(LocalDate today) {
        int days = intConfig("notify.eval.remind.days", 3);
        LocalDate endedBefore = today.minusDays(Math.max(days, 0));
        // 只催近 60 天内结业的班次, 避免翻旧账式催办
        LocalDate earliest = today.minusDays(60);
        List<TrainingClass> classes = classMapper.selectList(new LambdaQueryWrapper<TrainingClass>()
                .eq(TrainingClass::getStatus, "3")
                .between(TrainingClass::getEndDate, earliest, endedBefore));
        if (classes.isEmpty()) {
            return 0;
        }
        List<Pending> all = new ArrayList<>();
        for (TrainingClass c : classes) {
            for (StudentVo st : studentMapper.selectByClassId(c.getId())) {
                if ("3".equals(st.getStatus())) {
                    continue;
                }
                Long answered = evalRecordMapper.selectCount(new LambdaQueryWrapper<EvalRecord>()
                        .eq(EvalRecord::getClassId, c.getId())
                        .eq(EvalRecord::getUserId, st.getUserId()));
                if (answered != null && answered > 0) {
                    continue;
                }
                all.add(new Pending("evalPending", "evalPending:" + c.getId() + ":user:" + st.getUserId(),
                        st.getUserId(), "培训评估待填写",
                        "「" + c.getClassName() + "」已结业 " + days + " 天, 您尚未完成培训评估, "
                                + "请及时填写, 您的反馈用于培训持续改进。", c.getId()));
            }
        }
        return dispatch("evalPending", all, 7);
    }

    private int remindHourWarn() {
        int year = LocalDate.now().getYear();
        List<Pending> all = new ArrayList<>();
        for (Map<String, Object> w : archiveService.allWarnings(year)) {
            Long userId = ((Number) w.get("userId")).longValue();
            String level = String.valueOf(w.get("level"));
            String levelText = "red".equals(level) ? "红色(严重滞后)"
                    : "orange".equals(level) ? "橙色(明显滞后)" : "黄色(进度偏慢)";
            all.add(new Pending("hourWarn", "hourWarn:" + year + ":user:" + userId, userId,
                    "年度学时预警提醒",
                    "您的 " + year + " 年度培训学时为 " + w.get("totalHours") + "/" + w.get("required")
                            + ", 预警级别: " + levelText + ", 请合理安排时间参加培训。", null));
        }
        // 28 天窗口: 每天扫描但约每月触达一次, 不依赖某一天系统必须在线
        return dispatch("hourWarn", all, 28);
    }

    /** 过滤去重窗口内已发送的项, 逐一发送并留痕 */
    private int dispatch(String type, List<Pending> all, int windowDays) {
        if (all.isEmpty()) {
            return 0;
        }
        List<Long> userIds = all.stream().map(p -> p.userId).distinct().toList();
        Set<String> sent = new HashSet<>(notifyLogMapper.sentKeys(type,
                LocalDateTime.now().minusDays(windowDays), userIds));
        int n = 0;
        for (Pending p : all) {
            if (sent.contains(p.key)) {
                continue;
            }
            messageService.send(p.userId, type.equals("hourWarn") ? "warn"
                    : type.equals("evalPending") ? "eval" : "enroll", p.title, p.content, p.refId);
            NotifyLog l = new NotifyLog();
            l.setNotifyType(type);
            l.setBizKey(p.key);
            l.setUserId(p.userId);
            l.setSentTime(LocalDateTime.now());
            notifyLogMapper.insert(l);
            n++;
        }
        return n;
    }

    private int intConfig(String key, int def) {
        try {
            return Integer.parseInt(configService.getValue(key, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private record Pending(String type, String key, Long userId, String title, String content, Long refId) {
    }
}
