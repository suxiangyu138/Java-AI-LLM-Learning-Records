package Java_learning_notes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamDemo {
    public static void main(String[] args) {
        List<Integer> numList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<String> strList = Arrays.asList("java", "lambda", "stream", "java", "spring");

        // 1. filter 过滤
        List<Integer> evenList = numList.stream()
                .filter(n -> n % 2 == 0)
                .collect(Collectors.toList());
        System.out.println("过滤偶数：" + evenList);

        // 2. map 映射/类型转换
        List<String> numStr = numList.stream()
                .map(n -> "数字" + n)
                .collect(Collectors.toList());
        System.out.println("map映射：" + numStr);

        // 3. distinct 去重
        List<String> distinctList = strList.stream()
                .distinct()
                .collect(Collectors.toList());
        System.out.println("去重：" + distinctList);

        // 4. sorted 排序
        List<Integer> sortedAsc = numList.stream()
                .sorted()
                .collect(Collectors.toList());

        List<Integer> sortedDesc = numList.stream()
                .sorted((a, b) -> b - a)
                .collect(Collectors.toList());
        System.out.println("升序：" + sortedAsc);
        System.out.println("降序：" + sortedDesc);

        // 5. limit 限制条数
        List<Integer> limitList = numList.stream()
                .limit(3)
                .collect(Collectors.toList());
        System.out.println("取前3条：" + limitList);

        // 6. skip 跳过元素
        List<Integer> skipList = numList.stream()
                .skip(5)
                .collect(Collectors.toList());
        System.out.println("跳过前5条：" + skipList);

        // 7. count 统计数量
        long count = numList.stream().count();
        System.out.println("集合总数：" + count);

        // 8. anyMatch / allMatch 匹配
        boolean any = numList.stream().anyMatch(n -> n > 5);
        boolean all = numList.stream().allMatch(n -> n > 0);
        System.out.println("存在大于5：" + any);
        System.out.println("全部大于0：" + all);

        // 9. 转为Set集合
        Set<String> strSet = strList.stream()
                .collect(Collectors.toSet());
        System.out.println("转Set：" + strSet);

        // 10. 并行流 parallelStream
        numList.parallelStream()
                .forEach(n -> System.out.print(n + " "));

        // 11. reduce 归约求和
        Integer sum = numList.stream()
                .reduce(0, Integer::sum);
        System.out.println("\n求和：" + sum);

        // 12. 链式组合：过滤+映射+排序+收集
        List<String> result = numList.stream()
                .filter(n -> n % 2 == 0)
                .map(n -> "偶数：" + n)
                .sorted()
                .collect(Collectors.toList());
        System.out.println("组合操作：" + result);
    }
}