package streams;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamDownstreamCollectorDemo {
    public static void main(String[] args) {
        // 群组+下游收集器：按长度分组，统计每组数量
        Map<Integer, Long> groupCount = Stream.of("apple", "banana", "cherry", "date", "fig")
                .collect(Collectors.groupingBy(String::length, Collectors.counting()));
        System.out.println("groupCount: " + groupCount);

        // 群组+下游收集器：按长度分组，求每组平均长度
        Map<Integer, Double> groupAvg = Stream.of("apple", "banana", "cherry", "date")
                .collect(Collectors.groupingBy(String::length, Collectors.averagingInt(String::length)));
        System.out.println("groupAvg: " + groupAvg);
    }
}