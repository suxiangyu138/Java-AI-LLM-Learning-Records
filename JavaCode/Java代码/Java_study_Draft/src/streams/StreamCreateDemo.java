package streams;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StreamCreateDemo {
    public static void main(String[] args) {
        // 1. 从集合创建
        List<String> list = List.of("a", "b", "c");
        Stream<String> streamFromList = list.stream();

        // 2. 从数组创建
        String[] array = {"x", "y", "z"};
        Stream<String> streamFromArray = Arrays.stream(array);

        // 3. 静态方法Stream.of()
        Stream<String> streamOf = Stream.of("1", "2", "3");

        // 4. 基本类型流
        IntStream intStream = IntStream.range(1, 5); // 1,2,3,4

        // 5. 无限流
        Stream<Integer> infiniteStream = Stream.iterate(0, n -> n + 2); // 0,2,4,6...
    }

    public static class StreamIterationDemo {
        public static void main(String[] args) {
            List<String> words = List.of("apple", "banana", "cherry", "date", "elderberry");

            // 传统迭代：筛选长度>5的单词并计数
            long countLoop = 0;
            for (String w : words) {
                if (w.length() > 5) countLoop++;
            }
            System.out.println("循环统计: " + countLoop);

            // 流处理：筛选长度>5的单词并计数
            long countStream = words.stream()
                    .filter(w -> w.length() > 5)
                    .count();
            System.out.println("流统计: " + countStream);
        }
    }
}