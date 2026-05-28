package IO;

import java.util.regex.*;

public class RegexSyntaxDemo {
    public static void main(String[] args) {
        String regex = "^[A-Za-z0-9]+@[a-zA-Z0-9]+\\.[a-zA-Z]{2,}$"; // 邮箱正则
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher("test@example.com");

        System.out.println("是否匹配：" + matcher.matches()); // true
    }
}
