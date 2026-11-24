package com.soe.training.common;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量 IP 滑动窗口限流(单机内存实现, 与登录锁定同策略; 集群部署改用 Redis)。
 * 用于无需登录的公开接口, 防止证书编号被脚本批量枚举。
 */
@Component
public class PublicRateLimiter {

    /** 每 IP 每窗口允许的次数 */
    private static final int LIMIT = 10;
    /** 窗口(毫秒) */
    private static final long WINDOW_MS = 60_000L;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong calls = new java.util.concurrent.atomic.AtomicLong();

    /** 记录一次访问; 允许返回 true, 超限返回 false */
    public boolean tryAcquire(String ip) {
        // 低频清理空闲 IP, 防止换源 IP 刷量导致 Map 无限增长
        if (calls.incrementAndGet() % 1000 == 0) {
            evictIdle();
        }
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(ip == null ? "unknown" : ip, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) {
                q.pollFirst();
            }
            if (q.size() >= LIMIT) {
                return false;
            }
            q.addLast(now);
            return true;
        }
    }

    /** 空闲 IP 的记录清理(由调用方在低峰时触发, 避免 Map 无限增长) */
    public void evictIdle() {
        long now = System.currentTimeMillis();
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() || now - e.getValue().peekLast() > WINDOW_MS * 10;
            }
        });
    }
}
