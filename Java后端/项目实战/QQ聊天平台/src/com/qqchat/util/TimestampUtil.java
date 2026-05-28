package com.qqchat.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimestampUtil {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    public static long now() {
        return System.currentTimeMillis();
    }

    public static String format(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp), ZONE);
        return dateTime.format(FORMATTER);
    }
}
