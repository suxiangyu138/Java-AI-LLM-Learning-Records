package streams;

import java.util.List;
import java.util.stream.Collectors;

public class StreamMapFilterDemo {
    public static void main(String[] args) {
        List<String> words = List.of("hello", "world", "java", "stream");

        // filter：筛选长度>5的单词
        List<String> longWords = words.stream()
                .filter(w -> w.length() > 5)
                .collect(Collectors.toList());
        System.out.println("filter结果: " + longWords);

        // map：将单词转为大写
        List<String> upperWords = words.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        System.out.println("map结果: " + upperWords);

        // flatMap：将每个单词拆分为字符流并合并
        List<Character> chars = words.stream()
                .flatMap(w -> w.chars().mapToObj(c -> (char) c))
                .collect(Collectors.toList());
        System.out.println("flatMap结果: " + chars);
    }
}