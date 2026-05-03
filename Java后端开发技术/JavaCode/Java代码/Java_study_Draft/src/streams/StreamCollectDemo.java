package streams;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamCollectDemo {
    public static void main(String[] args) {
        Stream<String> stream = Stream.of("a", "b", "c", "a");

        // 收集到List
        List<String> list = stream.collect(Collectors.toList());
        System.out.println("list: " + list);

        // 收集到Set（去重）
        Set<String> set = Stream.of("a", "b", "c", "a").collect(Collectors.toSet());
        System.out.println("set: " + set);

        // 收集到字符串
        String joined = Stream.of("a", "b", "c").collect(Collectors.joining(", "));
        System.out.println("joined: " + joined);

        // 统计汇总
        int sumLength = Stream.of("apple", "banana").collect(Collectors.summingInt(String::length));
        System.out.println("总长度: " + sumLength);
    }
}