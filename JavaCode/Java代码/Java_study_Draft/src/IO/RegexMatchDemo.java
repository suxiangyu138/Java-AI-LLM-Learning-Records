package IO;

import java.util.regex.*;

public class RegexMatchDemo {
    public static void main(String[] args) {
        String text = "Hello, Java 8! Java 17 is better.";
        String regex = "Java \\d+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            System.out.println("找到匹配：" + matcher.group());
        }
    }
}