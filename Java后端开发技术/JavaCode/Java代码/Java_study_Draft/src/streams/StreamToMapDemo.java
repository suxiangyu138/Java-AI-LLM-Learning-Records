package streams;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamToMapDemo {
    public static void main(String[] args) {
        // 键：单词，值：单词长度
        Map<String, Integer> wordLengthMap = Stream.of("apple", "banana", "cherry")
                .collect(Collectors.toMap(s -> s, String::length));
        System.out.println("wordLengthMap: " + wordLengthMap);

        // 处理重复键
        Map<String, Integer> duplicateKeyMap = Stream.of("apple", "app", "banana")
                .collect(Collectors.toMap(
                        s -> s.substring(0, 1), // 首字母为键
                        String::length,
                        (oldVal, newVal) -> oldVal // 冲突时保留旧值
                ));
        System.out.println("duplicateKeyMap: " + duplicateKeyMap);
    }
}