package com.qqchat.server;

import java.util.concurrent.ConcurrentHashMap;

public class HeartbeatMonitor extends Thread {
    private final SessionRegistry sessionRegistry;
    private volatile boolean running = true;
    private static final long TIMEOUT_MS = 90_000;
    private static final long CHECK_INTERVAL_MS = 30_000;

    // userId -> lastPingTime
    private final ConcurrentHashMap<String, Long> lastPingTimes = new ConcurrentHashMap<>();

    public HeartbeatMonitor(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
        setDaemon(true);
        setName("HeartbeatMonitor");
    }

    public void recordPing(String userId) {
        lastPingTimes.put(userId, System.currentTimeMillis());
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                break;
            }

            long now = System.currentTimeMillis();
            for (String userId : sessionRegistry.getOnlineUsers()) {
                Long lastPing = lastPingTimes.get(userId);
                if (lastPing != null && (now - lastPing) > TIMEOUT_MS) {
                    System.out.println("[HeartbeatMonitor] User " + userId + " timed out");
                    ClientHandler handler = sessionRegistry.getHandler(userId);
                    if (handler != null) {
                        handler.disconnect("Connection timed out");
                    }
                }
            }
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
