package streams;

import java.util.Optional;

public class OptionalDemo {
    public static void main(String[] args) {
        // 1.7.1 获取Optional值
        Optional<String> opt = Optional.of("hello");
        if (opt.isPresent()) {
            System.out.println("存在值: " + opt.get());
        }
        String orElse = opt.orElse("默认值");
        System.out.println("orElse: " + orElse);

        // 1.7.2 消费Optional值
        opt.ifPresent(v -> System.out.println("消费值: " + v));

        // 1.7.3 管道化Optional值
        Optional<Integer> length = opt.map(String::length);
        System.out.println("map后长度: " + length.orElse(0));

        // 1.7.5 创建Optional值
        Optional<String> emptyOpt = Optional.empty();
        Optional<String> ofNullableOpt = Optional.ofNullable(null);

        // 1.7.6 flatMap构建Optional值
        Optional<String> flatMapped = opt.flatMap(s -> Optional.of(s.toUpperCase()));
        System.out.println("flatMap后: " + flatMapped.orElse(""));

        // 1.7.7 Optional转流
        opt.stream().forEach(System.out::println);
    }
}