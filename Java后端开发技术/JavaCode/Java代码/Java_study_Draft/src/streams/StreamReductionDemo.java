package streams;

import java.util.Optional;
import java.util.stream.Stream;

public class StreamReductionDemo {
    public static void main(String[] args) {
        // 约简：求和
        int sum = Stream.of(1, 2, 3, 4, 5)
                .reduce(0, Integer::sum);
        System.out.println("sum: " + sum);

        // 约简：求最大值
        Optional<Integer> max = Stream.of(1, 2, 3, 4, 5)
                .reduce(Integer::max);
        max.ifPresent(v -> System.out.println("max: " + v));

        // 约简：连接字符串
        String joined = Stream.of("a", "b", "c")
                .reduce("", (s1, s2) -> s1 + s2);
        System.out.println("joined: " + joined);
    }
}