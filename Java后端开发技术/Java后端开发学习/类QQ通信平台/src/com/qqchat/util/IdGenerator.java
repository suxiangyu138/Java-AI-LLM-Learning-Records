package com.qqchat.util;

import java.util.concurrent.ThreadLocalRandom;

public class IdGenerator {
    public static String generateMessageId() {
        long ts = System.currentTimeMillis();
        int suffix = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "m_" + ts + "_" + suffix;
    }

    public static String generateGroupId() {
        long ts = System.currentTimeMillis();
        int suffix = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "g_" + ts + "_" + suffix;
    }
}
