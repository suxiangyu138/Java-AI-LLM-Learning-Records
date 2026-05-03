import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 日期时间加减、比较、间隔计算
 */
public class DateTimeCalculateDemo {
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 1. 日期加减（推荐：plus/minus 方法）
        LocalDate tomorrow = today.plusDays(1); // 明天
        LocalDate lastMonth = today.minusMonths(1); // 上个月
        LocalDateTime nextHour = now.plus(1, ChronoUnit.HOURS); // 1小时后
        System.out.println("📅 明天：" + tomorrow);
        System.out.println("📅 上个月今天：" + lastMonth);
        System.out.println("⏰ 1小时后：" + nextHour);

        // 2. 日期比较（isBefore/isAfter/isEqual）
        LocalDate birthday = LocalDate.of(2026, 2, 1);
        if (birthday.isAfter(today)) {
            System.out.println("🎉 生日还没到！");
        }

        // 3. 计算两个日期的间隔
        long daysBetween = ChronoUnit.DAYS.between(today, birthday);
        System.out.println("🎂 距离生日还有：" + daysBetween + " 天");

        // 4. 获取日期的具体字段
        int year = today.getYear(); // 年
        int month = today.getMonthValue(); // 月（1-12，非0开始）
        int day = today.getDayOfMonth(); // 日
        int week = today.getDayOfWeek().getValue(); // 星期（1=周一，7=周日）
        System.out.printf("📌 今天：%d年%d月%d日 星期%d%n", year, month, day, week);
    }
}