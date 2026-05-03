import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Java 核心 API 综合体验
 * 覆盖：字符串/集合/日期/IO/并发/数学/反射/Stream 流 API
 */
public class JavaApiExperience {
    public static void main(String[] args) throws Exception {
        // ====================== 模块1：字符串 API（java.lang.String） ======================
        System.out.println("===== 1. 字符串 API =====");
        String str = "  Hello Java API!  ";

        // 核心方法
        System.out.println("原字符串：'" + str + "'");
        System.out.println("trim() 去空格：'" + str.trim() + "'"); // 去首尾空格
        System.out.println("length() 长度：" + str.length()); // 长度
        System.out.println("substring(3, 8) 截取：" + str.substring(3, 8)); // [3,8)
        System.out.println("replace(' ', '-') 替换：" + str.replace(' ', '-')); // 替换字符
        System.out.println("split(' ') 分割：" + Arrays.toString(str.split(" "))); // 分割数组
        System.out.println("equalsIgnoreCase('hello java api!')：" + str.trim().equalsIgnoreCase("hello java api!")); // 忽略大小写比较

        // 性能对比：String（不可变） vs StringBuilder（可变）
        long start = System.currentTimeMillis();
        String s = "";
        for (int i = 0; i < 10000; i++) {
            s += i; // 每次创建新对象，性能差
        }
        System.out.println("String拼接耗时：" + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append(i); // 可变字符序列，性能好
        }
        System.out.println("StringBuilder拼接耗时：" + (System.currentTimeMillis() - start) + "ms");
        System.out.println();

        // ====================== 模块2：集合 API（java.util） ======================
        System.out.println("===== 2. 集合 API =====");
        // List：有序、可重复
        List<String> list = new ArrayList<>();
        list.add("Java");
        list.add("Python");
        list.add("C++");
        System.out.println("List遍历（for-each）：");
        for (String lang : list) {
            System.out.print(lang + " ");
        }
        // 排序
        Collections.sort(list); // 自然排序
        System.out.println("\nList排序后：" + list);

        // Map：键值对、键唯一
        Map<String, Integer> map = new HashMap<>();
        map.put("Java", 95);
        map.put("Python", 90);
        map.put("C++", 85);
        System.out.println("Map取值：Java=" + map.get("Java"));
        System.out.println("Map遍历（entrySet）：");
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println(entry.getKey() + "：" + entry.getValue());
        }

        // Set：无序、不可重复
        Set<String> set = new HashSet<>(list);
        set.add("Java"); // 重复元素不添加
        System.out.println("Set去重后：" + set);
        System.out.println();

        // ====================== 模块3：日期时间 API（java.time，Java 8+） ======================
        System.out.println("===== 3. 日期时间 API =====");
        // 本地日期时间
        LocalDateTime now = LocalDateTime.now();
        System.out.println("当前时间：" + now);

        // 格式化
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formatNow = now.format(formatter);
        System.out.println("格式化后：" + formatNow);

        // 解析字符串为日期
        LocalDateTime parseTime = LocalDateTime.parse("2026-01-22 10:00:00", formatter);
        System.out.println("解析后的时间：" + parseTime);

        // 时间加减
        LocalDateTime nextDay = now.plusDays(1);
        LocalDateTime lastHour = now.minusHours(1);
        System.out.println("加1天：" + nextDay.format(formatter));
        System.out.println("减1小时：" + lastHour.format(formatter));

        // 日期比较
        boolean isAfter = now.isAfter(parseTime);
        System.out.println("当前时间是否晚于2026-01-22 10:00:00：" + isAfter);
        System.out.println();

        // ====================== 模块4：IO API（java.io） ======================
        System.out.println("===== 4. IO API =====");
        // 文本文件写入（try-with-resources 自动关闭流）
        String content = "Java API 体验：IO 流操作";
        try (FileWriter writer = new FileWriter("test.txt")) {
            writer.write(content);
            System.out.println("文件写入成功：test.txt");
        }

        // 文本文件读取
        try (BufferedReader reader = new BufferedReader(new FileReader("test.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("文件读取内容：" + line);
            }
        }
        System.out.println();

        // ====================== 模块5：并发 API（java.util.concurrent） ======================
        System.out.println("===== 5. 并发 API =====");
        // 线程池（常用）
        ExecutorService executor = Executors.newFixedThreadPool(2); // 固定2个线程

        // 提交任务
        executor.submit(() -> {
            System.out.println("线程1执行：" + Thread.currentThread().getName());
            try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) {}
        });
        executor.submit(() -> {
            System.out.println("线程2执行：" + Thread.currentThread().getName());
            try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) {}
        });

        // 关闭线程池
        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS); // 等待任务完成
        System.out.println("所有线程执行完成");
        System.out.println();

        // ====================== 模块6：数学 API（java.lang.Math） ======================
        System.out.println("===== 6. 数学 API =====");
        double num = 3.14159;
        System.out.println("Math.round(" + num + ") 四舍五入：" + Math.round(num));
        System.out.println("Math.ceil(" + num + ") 向上取整：" + Math.ceil(num));
        System.out.println("Math.floor(" + num + ") 向下取整：" + Math.floor(num));
        System.out.println("Math.random() 随机数：" + Math.random()); // 0~1 随机数
        System.out.println("Math.pow(2, 10) 2的10次方：" + Math.pow(2, 10));
        System.out.println();

        // ====================== 模块7：反射 API（java.lang.reflect） ======================
        System.out.println("===== 7. 反射 API =====");
        // 反射获取类信息
        Class<?> clazz = User.class;
        System.out.println("类名：" + clazz.getName());

        // 获取属性
        Field[] fields = clazz.getDeclaredFields();
        System.out.println("类属性：");
        for (Field field : fields) {
            System.out.println("  " + field.getName() + "（类型：" + field.getType().getSimpleName() + "）");
        }

        // 调用方法
        User user = new User("张三", 20);
        Method method = clazz.getDeclaredMethod("sayHello");
        method.invoke(user); // 调用sayHello方法
        System.out.println();

        // ====================== 模块8：Stream 流 API（java.util.stream） ======================
        System.out.println("===== 8. Stream 流 API =====");
        List<Integer> numList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // 过滤+映射+统计
        List<Integer> result = numList.stream()
                .filter(n -> n % 2 == 0) // 过滤偶数
                .map(n -> n * 2) // 乘以2
                .sorted(Comparator.reverseOrder()) // 倒序
                .collect(Collectors.toList()); // 收集为List

        System.out.println("Stream处理结果（偶数×2倒序）：" + result);

        // 统计
        long count = numList.stream().filter(n -> n > 5).count();
        int sum = numList.stream().mapToInt(Integer::intValue).sum();
        System.out.println("大于5的数字个数：" + count);
        System.out.println("所有数字总和：" + sum);
    }

    // 反射测试用类
    static class User {
        private String name;
        private int age;

        public User() {}
        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public void sayHello() {
            System.out.println("Hello，我是" + name + "，年龄" + age);
        }
    }
}