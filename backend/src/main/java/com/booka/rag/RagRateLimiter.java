package com.booka.rag;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RagRateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int limitPerMinute) {
        long currentMinute = Instant.now().getEpochSecond() / 60;
        Window window = windows.computeIfAbsent(key, k -> new Window(currentMinute));
        synchronized (window) {
            if (window.minute != currentMinute) {
                window.minute = currentMinute;
                window.count.set(0);
            }
            if (window.count.get() >= limitPerMinute) {
                return false;
            }
            window.count.incrementAndGet();
            return true;
        }
    }

    private static class Window {
        long minute;
        AtomicInteger count = new AtomicInteger(0);

        Window(long minute) {
            this.minute = minute;
        }
    }
}
