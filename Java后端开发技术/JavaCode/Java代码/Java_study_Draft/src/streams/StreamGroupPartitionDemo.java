package streams;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamGroupPartitionDemo {
    public static void main(String[] args) {
        // 群组：按长度分组
        Map<Integer, List<String>> groupByLength = Stream.of("apple", "banana", "cherry", "date")
                .collect(Collectors.groupingBy(String::length));
        System.out.println("groupByLength: " + groupByLength);

        // 分区：按是否长度>5分区
        Map<Boolean, List<String>> partitionByLength = Stream.of("apple", "banana", "cherry", "date")
                .collect(Collectors.partitioningBy(s -> s.length() > 5));
        System.out.println("partitionByLength: " + partitionByLength);
    }
}