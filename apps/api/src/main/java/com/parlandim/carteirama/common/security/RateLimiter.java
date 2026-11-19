package com.parlandim.carteirama.common.security;

import com.parlandim.carteirama.common.error.TooManyRequestsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    public record Rule(String name, int max, Duration window) {
    }

    public static final Rule LOGIN_FAILURES = new Rule("login", 5, Duration.ofMinutes(15));
    public static final Rule REGISTER = new Rule("register", 5, Duration.ofHours(1));
    public static final Rule FORGOT_PASSWORD = new Rule("forgot", 5, Duration.ofHours(1));
    public static final Rule USER_REQUESTS = new Rule("user", 300, Duration.ofMinutes(1));

    private final Clock clock;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void hit(Rule rule, String key) {
        check(rule, key);
        record(rule, key);
    }

    public void check(Rule rule, String key) {
        long now = clock.millis();
        Deque<Long> q = hits.get(rule.name() + ":" + key);
        if (q == null) {
            return;
        }
        synchronized (q) {
            prune(q, now, rule.window());
            if (q.size() >= rule.max()) {
                long retryMs = q.peekFirst() + rule.window().toMillis() - now;
                throw new TooManyRequestsException(Math.max(1, (retryMs + 999) / 1000));
            }
        }
    }

    public void record(Rule rule, String key) {
        long now = clock.millis();
        Deque<Long> q = hits.computeIfAbsent(rule.name() + ":" + key, k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q, now, rule.window());
            q.addLast(now);
        }
    }

    public void reset(Rule rule, String key) {
        hits.remove(rule.name() + ":" + key);
    }

    public void clear() {
        hits.clear();
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000)
    void evictIdle() {
        long now = clock.millis();
        long maxWindow = Duration.ofHours(1).toMillis();
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Long last = e.getValue().peekLast();
                return last == null || now - last > maxWindow;
            }
        });
    }

    private static void prune(Deque<Long> q, long now, Duration window) {
        long cutoff = now - window.toMillis();
        while (!q.isEmpty() && q.peekFirst() <= cutoff) {
            q.pollFirst();
        }
    }
}
