import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 格式化（对象→字符串）& 解析（字符串→对象）
 */
public class FormatParseDemo {
    public static void main(String[] args) {
        // 1. 定义格式化模板（常用）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 2. 格式化：LocalDateTime → 字符串
        LocalDateTime now = LocalDateTime.now();
        String formatted = now.format(formatter);
        System.out.println("✅ 格式化后：" + formatted); // 输出：2026-01-22 10:50:20

        // 3. 解析：字符串 → LocalDateTime
        String dateStr = "2026-01-01 00:00:00";
        try {
            LocalDateTime parseDateTime = LocalDateTime.parse(dateStr, formatter);
            System.out.println("✅ 解析后：" + parseDateTime); // 输出：2026-01-01T00:00
        } catch (DateTimeParseException e) {
            System.err.println("❌ 解析失败：格式不匹配 → " + e.getMessage());
        }

        // 4. 内置格式化器（无需自定义）
        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        String isoDate = LocalDate.now().format(isoFormatter);
        System.out.println("📌 ISO 格式日期：" + isoDate); // 输出：2026-01-22
    }
}