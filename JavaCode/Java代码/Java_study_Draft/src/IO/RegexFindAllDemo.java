package IO;

import java.util.regex.*;
import java.util.*;

public class RegexFindAllDemo {
    public static void main(String[] args) {
        String text = "Numbers: 123, 456, 789";
        String regex = "\\d+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        List<String> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
        System.out.println(numbers); // [123, 456, 789]
    }
}
