package streams;

import java.util.List;
import java.util.Optional;

public class StreamReduceDemo {
    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        // count：计数
        long count = numbers.stream().count();
        System.out.println("count: " + count);

        // max/min：最大/最小值
        Optional<Integer> max = numbers.stream().max(Integer::compareTo);
        max.ifPresent(v -> System.out.println("max: " + v));

        // reduce：求和
        int sum = numbers.stream()
                .reduce(0, Integer::sum);
        System.out.println("sum: " + sum);
    }
}