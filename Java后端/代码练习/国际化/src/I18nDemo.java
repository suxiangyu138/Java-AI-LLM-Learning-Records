import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Java 国际化（i18n）核心示例
 */
public class I18nDemo {
    // 资源包基础名（对应属性文件的前缀：messages）
    private static final String BASE_NAME = "messages";

    public static void main(String[] args) {
        // 1. 测试中文（中国）环境
        System.out.println("===== 中文（中国） =====");
        showMessages(Locale.CHINA);

        // 2. 测试英文（美国）环境
        System.out.println("\n===== 英文（美国） =====");
        showMessages(Locale.US);

        // 3. 测试日语（日本）环境（兜底用默认文件）
        System.out.println("\n===== 日语（日本）（兜底） =====");
        showMessages(Locale.JAPAN);
    }

    /**
     * 根据 Locale 显示对应语言的文本
     * @param locale 语言环境
     */
    private static void showMessages(Locale locale) {
        // 加载资源包（核心：ResourceBundle）
        ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, locale);

        // 1. 获取简单文本（无参数）
        String welcome = bundle.getString("welcome");
        String username = bundle.getString("username");
        String password = bundle.getString("password");
        String submit = bundle.getString("submit");

        System.out.println(welcome);
        System.out.println(username);
        System.out.println(password);
        System.out.println(submit);

        // 2. 获取带参数的文本（如包含时间变量）
        String datetimeTemplate = bundle.getString("datetime");
        // 格式化带参数的文本（MessageFormat）
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String datetimeMsg = MessageFormat.format(datetimeTemplate, currentTime);
        System.out.println(datetimeMsg);
    }
}