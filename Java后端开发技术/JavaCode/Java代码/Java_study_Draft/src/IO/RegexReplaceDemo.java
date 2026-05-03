package IO;

import java.util.regex.*;

public class RegexReplaceDemo {
    public static void main(String[] args) {
        String text = "Hello, Java 8! Java 17 is better.";
        String regex = "Java (\\d+)";
        String replacement = "Java $1 LTS"; // $1 引用第一个分组

        String result = text.replaceAll(regex, replacement);
        System.out.println(result); // Hello, Java 8 LTS! Java 17 LTS is better.
    }
}