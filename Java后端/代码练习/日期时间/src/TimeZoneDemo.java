import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 时区转换 + Instant（时间戳）互转
 */
public class TimeZoneDemo {
    public static void main(String[] args) {
        // 1. 本地时间转 UTC 时间
        LocalDateTime localDateTime = LocalDateTime.now();
        ZonedDateTime shanghaiTime = localDateTime.atZone(ZoneId.of("Asia/Shanghai"));
        ZonedDateTime utcTime = shanghaiTime.withZoneSameInstant(ZoneId.of("UTC"));
        System.out.println("🇨🇳 上海时间：" + shanghaiTime);
        System.out.println("🌍 UTC 时间：" + utcTime);

        // 2. Instant（时间戳）转 LocalDateTime
        Instant instant = Instant.now();
        LocalDateTime instantToLocal = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Shanghai"));
        System.out.println("⌛ 时间戳转本地时间：" + instantToLocal);

        // 3. LocalDateTime 转 Instant（时间戳）
        Instant localToInstant = localDateTime.atZone(ZoneId.of("Asia/Shanghai")).toInstant();
        System.out.println("📆 本地时间转时间戳（秒）：" + localToInstant.getEpochSecond());
    }
}