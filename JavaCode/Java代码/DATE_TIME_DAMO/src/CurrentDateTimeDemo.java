import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.ZoneId;

/**
 * 获取当前日期时间（最常用场景）
 */
public class CurrentDateTimeDemo {
    public static void main(String[] args) {
        // 1. 本地日期（仅年月日）
        LocalDate today = LocalDate.now();
        System.out.println("📅 当前日期：" + today); // 输出：2026-01-22

        // 2. 本地时间（仅时分秒）
        LocalTime nowTime = LocalTime.now();
        System.out.println("⏰ 当前时间：" + nowTime); // 输出：10:45:30.123

        // 3. 本地日期时间（年月日+时分秒）
        LocalDateTime nowDateTime = LocalDateTime.now();
        System.out.println("📆 当前日期时间：" + nowDateTime); // 输出：2026-01-22T10:45:30.123

        // 4. 带时区的日期时间（上海时区）
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        System.out.println("🌐 上海时区时间：" + zonedDateTime);

        // 5. 时间戳（UTC 纪元秒数，1970-01-01 00:00:00 至今）
        Instant instant = Instant.now();
        System.out.println("⌛ 当前时间戳（秒）：" + instant.getEpochSecond());
        System.out.println("⌛ 当前时间戳（毫秒）：" + instant.toEpochMilli());
    }
}