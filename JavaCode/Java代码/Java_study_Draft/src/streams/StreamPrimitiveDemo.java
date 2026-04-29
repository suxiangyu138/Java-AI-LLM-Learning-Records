package streams;

import java.util.IntSummaryStatistics;
import java.util.stream.IntStream;

public class StreamPrimitiveDemo {
    public static void main(String[] args) {
        // IntStream：基本类型流
        IntStream intStream = IntStream.rangeClosed(1, 5); // 1-5

        // 求和
        int sum = intStream.sum();
        System.out.println("sum: " + sum);

        // 统计信息
        IntSummaryStatistics stats = IntStream.rangeClosed(1, 5).summaryStatistics();
        System.out.println("max: " + stats.getMax());
        System.out.println("min: " + stats.getMin());
        System.out.println("avg: " + stats.getAverage());

        // 基本类型流转对象流
        IntStream.rangeClosed(1, 3).boxed().forEach(System.out::println);
    }
}