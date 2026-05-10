package streams;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamSubCombineDemo {
    public static void main(String[] args) {
        // 抽取子流：limit、skip、distinct
        List<Integer> numbers = List.of(1, 2, 2, 3, 4, 5, 6);
        List<Integer> limited = numbers.stream()
                .limit(3)
                .collect(Collectors.toList());
        System.out.println("limit(3): " + limited);

        List<Integer> skipped = numbers.stream()
                .skip(2)
                .collect(Collectors.toList());
        System.out.println("skip(2): " + skipped);

        List<Integer> distinct = numbers.stream()
                .distinct()
                .collect(Collectors.toList());
        System.out.println("distinct: " + distinct);

        // 组合流：concat
        Stream<String> stream1 = Stream.of("a", "b");
        Stream<String> stream2 = Stream.of("c", "d");
        Stream<String> combined = Stream.concat(stream1, stream2);
        System.out.println("concat结果: " + combined.collect(Collectors.toList()));
    }
}