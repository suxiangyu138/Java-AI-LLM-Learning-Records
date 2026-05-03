import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java 正则表达式（Regex）综合体验
 * 包含：基础语法、常用校验、匹配/替换/提取/分割、实战案例
 */
public class RegexExperience {
    public static void main(String[] args) {
        // ====================== 模块1：正则基础语法 & 核心API ======================
        System.out.println("===== 1. 正则基础语法 & 核心API =====");
        // 1.1 核心API：Pattern（编译正则） + Matcher（匹配器）
        String testStr = "Java Regex 123 456";
        String regex = "\\d+"; // 匹配1个或多个数字（\\d是数字，+是量词）

        // 步骤1：编译正则表达式（复用Pattern提升性能）
        Pattern pattern = Pattern.compile(regex);
        // 步骤2：创建匹配器
        Matcher matcher = pattern.matcher(testStr);

        // 查找所有匹配的子串
        System.out.println("原始字符串：" + testStr);
        System.out.print("匹配的数字：");
        while (matcher.find()) {
            System.out.print(matcher.group() + " "); // group()获取匹配结果
        }
        System.out.println("\n");

        // ====================== 模块2：常用元字符 & 量词 ======================
        System.out.println("===== 2. 常用元字符 & 量词 =====");
        // 元字符：.（任意字符）、\d（数字）、\w（字母/数字/下划线）、\s（空白符）、^（开头）、$（结尾）
        // 量词：*（0+）、+（1+）、?（0/1）、{n}（恰好n次）、{n,}（n+）、{n,m}（n-m次）

        // 示例1：匹配以J开头，以a结尾的单词（忽略中间字符）
        String regex1 = "^J.*a$"; // ^开头，.*任意字符0+，$结尾
        System.out.println("\"Java\" 匹配 ^J.*a$：" + Pattern.matches(regex1, "Java")); // true
        System.out.println("\"JavaScript\" 匹配 ^J.*a$：" + Pattern.matches(regex1, "JavaScript")); // false

        // 示例2：匹配恰好3位数字
        String regex2 = "\\d{3}";
        System.out.println("\"123\" 匹配 \\d{3}：" + Pattern.matches(regex2, "123")); // true
        System.out.println("\"1234\" 匹配 \\d{3}：" + Pattern.matches(regex2, "1234")); // false

        // 示例3：匹配0或1个a（可选字符）
        String regex3 = "a?";
        System.out.println("\"\" 匹配 a?：" + Pattern.matches(regex3, "")); // true
        System.out.println("\"a\" 匹配 a?：" + Pattern.matches(regex3, "a")); // true
        System.out.println("\"aa\" 匹配 a?：" + Pattern.matches(regex3, "aa")); // false
        System.out.println();

        // ====================== 模块3：高频场景正则校验 ======================
        System.out.println("===== 3. 高频场景正则校验 =====");
        // 3.1 手机号校验（11位，以1开头，第二位3-9）
        String phoneRegex = "^1[3-9]\\d{9}$";
        String[] phones = {"13812345678", "12812345678", "1381234567", "138123456789"};
        System.out.println("手机号校验：");
        for (String phone : phones) {
            boolean isValid = Pattern.matches(phoneRegex, phone);
            System.out.println(phone + "：" + (isValid ? "合法" : "非法"));
        }

        // 3.2 邮箱校验（xxx@xxx.xxx 基础版）
        String emailRegex = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
        String[] emails = {"test@163.com", "test@com", "test.123@qq.com", "test@123.456.789"};
        System.out.println("\n邮箱校验：");
        for (String email : emails) {
            boolean isValid = Pattern.matches(emailRegex, email);
            System.out.println(email + "：" + (isValid ? "合法" : "非法"));
        }

        // 3.3 身份证号校验（18位，最后一位可为X/x）
        String idCardRegex = "^[1-9]\\d{5}(19|20)\\d{2}((0[1-9])|(1[0-2]))((0[1-9])|([1-2]\\d)|(3[0-1]))\\d{3}([0-9Xx])$";
        String[] idCards = {"11010119900307701X", "11010119900307701", "11010119900307701x", "11010199900307701X"};
        System.out.println("\n身份证号校验：");
        for (String idCard : idCards) {
            boolean isValid = Pattern.matches(idCardRegex, idCard);
            System.out.println(idCard + "：" + (isValid ? "合法" : "非法"));
        }

        // 3.4 日期校验（yyyy-MM-dd）
        String dateRegex = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2]\\d|3[0-1])$";
        String[] dates = {"2026-01-22", "2026-13-22", "2026-02-30", "2026-01-2"};
        System.out.println("\n日期校验（yyyy-MM-dd）：");
        for (String date : dates) {
            boolean isValid = Pattern.matches(dateRegex, date);
            System.out.println(date + "：" + (isValid ? "合法" : "非法"));
        }
        System.out.println();

        // ====================== 模块4：正则实战操作（替换/提取/分割） ======================
        System.out.println("===== 4. 正则实战操作 =====");
        String content = "Java 基础教程：https://www.xxx.com/java，Python教程：https://www.xxx.com/python，手机号：13812345678";

        // 4.1 文本替换：隐藏手机号中间4位
        String replacePhone = content.replaceAll("(1[3-9]\\d)\\d{4}(\\d{4})", "$1****$2");
        System.out.println("替换手机号后：" + replacePhone);

        // 4.2 内容提取：提取所有URL
        String urlRegex = "https?://[\\w.-]+(/[\\w./?%&=]*)?";
        Pattern urlPattern = Pattern.compile(urlRegex);
        Matcher urlMatcher = urlPattern.matcher(content);
        System.out.print("\n提取的URL：");
        while (urlMatcher.find()) {
            System.out.print(urlMatcher.group() + " ");
        }

        // 4.3 字符串分割：按任意空白符（空格/制表符/换行）分割
        String splitStr = "Java  Regex\t教程\n2026";
        String[] splitResult = splitStr.split("\\s+"); // \\s+匹配1+个空白符
        System.out.println("\n\n按空白符分割：");
        for (String s : splitResult) {
            System.out.print("[" + s + "]");
        }

        // 4.4 分组提取：从日期中提取年/月/日
        String dateStr = "2026-01-22";
        String dateGroupRegex = "(\\d{4})-(\\d{2})-(\\d{2})";
        Pattern dateGroupPattern = Pattern.compile(dateGroupRegex);
        Matcher dateGroupMatcher = dateGroupPattern.matcher(dateStr);
        if (dateGroupMatcher.matches()) {
            String year = dateGroupMatcher.group(1); // 第1个分组：年
            String month = dateGroupMatcher.group(2); // 第2个分组：月
            String day = dateGroupMatcher.group(3); // 第3个分组：日
            System.out.println("\n\n日期分组提取：");
            System.out.println("年：" + year + "，月：" + month + "，日：" + day);
        }

        // ====================== 模块5：贪婪/非贪婪匹配 ======================
        System.out.println("\n===== 5. 贪婪/非贪婪匹配 =====");
        String greedyStr = "<div>内容1</div><div>内容2</div>";
        // 贪婪匹配（默认）：尽可能匹配更多内容
        String greedyRegex = "<div>.*</div>";
        Matcher greedyMatcher = Pattern.compile(greedyRegex).matcher(greedyStr);
        if (greedyMatcher.find()) {
            System.out.println("贪婪匹配结果：" + greedyMatcher.group());
        }

        // 非贪婪匹配（加?）：尽可能匹配更少内容
        String nonGreedyRegex = "<div>.*?</div>";
        Matcher nonGreedyMatcher = Pattern.compile(nonGreedyRegex).matcher(greedyStr);
        System.out.print("非贪婪匹配结果：");
        while (nonGreedyMatcher.find()) {
            System.out.print(greedyMatcher.group() + " | ");
        }
    }
}