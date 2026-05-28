package Java_learning_notes;

import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LambdaDemo {
    public static void main(String[] args) {
        // 1. 无参Lambda 线程
        new Thread(() -> System.out.println("Lambda 线程执行")).start();

        // 2. 单个参数简写
        Consumer<String> c2 = s -> System.out.println(s);
        c2.accept("单个参数简写");

        // 3. 多参数单行返回
        BinaryOperator<Integer> sum = (a, b) -> a + b;
        System.out.println(sum.apply(10, 20));

        // 4. 多行代码Lambda
        Predicate<Integer> predicate = num -> {
            System.out.println("判断数字");
            return num > 0;
        };
        System.out.println(predicate.test(5));

        // 5. 四大核心函数式接口
        Consumer<String> consumer = msg -> System.out.println("消费：" + msg);
        consumer.accept("hello");

        Supplier<Integer> supplier = () -> 666;
        System.out.println(supplier.get());

        Function<Integer, String> func = n -> "数字：" + n;
        System.out.println(func.apply(99));

        Predicate<String> pre = str -> str.length() > 3;
        System.out.println(pre.test("abcd"));

        // 6. 集合遍历
        List<String> list = Arrays.asList("A","B","C");
        list.forEach(item -> System.out.println(item));

        // 7. 集合排序
        List<Integer> numList = Arrays.asList(5,2,9,1);
        numList.sort((o1, o2) -> o1 - o2);
        System.out.println(numList);

        // 8. 方法引用
        List<String> strList = Arrays.asList("Java","Lambda");
        strList.forEach(System.out::println);
    }
}