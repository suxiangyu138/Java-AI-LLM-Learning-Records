import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.Duration;

/**
 * Period（日期差）& Duration（时间差）
 */
public class PeriodDurationDemo {
    public static void main(String[] args) {
        // 1. Period：计算日期间隔（年/月/日）
        LocalDate startDate = LocalDate.of(2020, 1, 1);
        LocalDate endDate = LocalDate.now();
        Period period = Period.between(startDate, endDate);
        System.out.printf("📅 间隔：%d年%d月%d日%n",
                period.getYears(), period.getMonths(), period.getDays());

        // 2. Duration：计算时间间隔（时/分/秒）
        LocalTime startTime = LocalTime.of(8, 0, 0);
        LocalTime endTime = LocalTime.now();
        Duration duration = Duration.between(startTime, endTime);
        System.out.printf("⏰ 间隔：%d小时%d分钟%n",
                duration.toHours(), duration.toMinutes() % 60);
    }
}