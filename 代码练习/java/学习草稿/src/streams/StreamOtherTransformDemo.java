package streams;

import java.util.List;
import java.util.stream.Collectors;

public class StreamOtherTransformDemo {
    public static void main(String[] args) {
        List<Integer> numbers = List.of(3, 1, 4, 1, 5, 9);

        // sorted：排序
        List<Integer> sorted = numbers.stream()
                .sorted()
                .collect(Collectors.toList());
        System.out.println("sorted: " + sorted);

        // peek：调试用，查看中间结果
        List<Integer> peeked = numbers.stream()
                .peek(n -> System.out.println("处理前: " + n))
                .map(n -> n * 2)
                .peek(n -> System.out.println("处理后: " + n))
                .collect(Collectors.toList());
    }
}