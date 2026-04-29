import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Java 8 Stream 流库综合体验
 * 修复：文件读取异常处理 + 路径正确性保证
 */
public class StreamExperience {
    // 学生实体类
    static class Student {
        private String name;    // 姓名
        private int age;        // 年龄
        private String gender;  // 性别
        private double score;   // 成绩

        public Student(String name, int age, String gender, double score) {
            this.name = name;
            this.age = age;
            this.gender = gender;
            this.score = score;
        }

        // Getter 方法
        public String getName() { return name; }
        public int getAge() { return age; }
        public String getGender() { return gender; }
        public double getScore() { return score; }

        @Override
        public String toString() {
            return "Student{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    ", gender='" + gender + '\'' +
                    ", score=" + score +
                    '}';
        }
    }

    public static void main(String[] args) {
        // 初始化测试数据：6个学生
        List<Student> students = Arrays.asList(
                new Student("张三", 18, "男", 90.5),
                new Student("李四", 19, "男", 85.0),
                new Student("王五", 18, "女", 95.5),
                new Student("赵六", 20, "女", 88.0),
                new Student("钱七", 19, "男", 78.5),
                new Student("孙八", 18, "女", 92.0)
        );

        // ====================== 模块1：Stream 的创建方式 ======================
        System.out.println("===== 1. Stream 的创建方式 =====");
        // 1.1 集合创建流（最常用）
        Stream<Student> collectionStream = students.stream();
        System.out.println("1.1 集合流 - 学生总数：" + collectionStream.count());

        // 1.2 数组创建流
        String[] arr = {"Java", "Python", "Stream"};
        Stream<String> arrayStream = Arrays.stream(arr);
        System.out.println("1.2 数组流 - 元素个数：" + arrayStream.count());

        // 1.3 单个值创建流
        Stream<String> valueStream = Stream.of("Hello", "Stream");
        System.out.println("1.3 单个值流 - 元素个数：" + valueStream.count());

        // 1.4 数值流（IntStream/LongStream/DoubleStream，避免自动装箱）
        IntStream intStream = IntStream.range(1, 5); // 1-4（左闭右开）
        System.out.println("1.4 数值流 - 1-4求和：" + intStream.sum());

        // 1.5 文件创建流（修复：添加异常处理，确保程序不崩溃）
        try {
            // 读取项目根目录的 test.txt 文件
            Stream<String> fileStream = Files.lines(Paths.get("test.txt"));
            long lineCount = fileStream.count();
            System.out.println("1.5 文件流 - test.txt 行数：" + lineCount);
            fileStream.close(); // 关闭流，释放资源
        } catch (IOException e) {
            System.err.println("1.5 文件流 - 读取失败：" + e.getMessage());
            System.err.println("  解决方案：请在项目根目录创建 test.txt 文件");
        }
        System.out.println("----------------------------------------");

        // ====================== 模块2：中间操作（懒加载） ======================
        System.out.println("===== 2. 中间操作 =====");
        // 2.1 filter：过滤女生
        List<Student> femaleStudents = students.stream()
                .filter(s -> "女".equals(s.getGender()))
                .collect(Collectors.toList());
        System.out.println("2.1 过滤女生：" + femaleStudents.stream().map(Student::getName).collect(Collectors.toList()));

        // 2.2 map：提取所有学生姓名
        List<String> names = students.stream()
                .map(Student::getName) // 方法引用，等价于 s -> s.getName()
                .collect(Collectors.toList());
        System.out.println("2.2 提取姓名：" + names);

        // 2.3 sorted：按成绩降序排序
        List<String> sortedByName = students.stream()
                .sorted((s1, s2) -> Double.compare(s2.getScore(), s1.getScore()))
                .map(Student::getName)
                .collect(Collectors.toList());
        System.out.println("2.3 按成绩降序（姓名）：" + sortedByName);

        // 2.4 distinct：年龄去重
        List<Integer> uniqueAges = students.stream()
                .map(Student::getAge)
                .distinct()
                .collect(Collectors.toList());
        System.out.println("2.4 年龄去重：" + uniqueAges);

        // 2.5 limit：取前3个学生
        List<String> top3Names = students.stream()
                .limit(3)
                .map(Student::getName)
                .collect(Collectors.toList());
        System.out.println("2.5 取前3个学生：" + top3Names);
        System.out.println("----------------------------------------");

        // ====================== 模块3：终止操作（触发执行） ======================
        System.out.println("===== 3. 终止操作 =====");
        // 3.1 forEach：遍历打印姓名
        System.out.print("3.1 遍历姓名：");
        students.stream().map(Student::getName).forEach(name -> System.out.print(name + " "));
        System.out.println();

        // 3.2 collect：转换为Map（姓名→成绩）
        Map<String, Double> scoreMap = students.stream()
                .collect(Collectors.toMap(Student::getName, Student::getScore));
        System.out.println("3.2 姓名-成绩Map：" + scoreMap);

        // 3.3 统计操作：平均成绩、最高成绩
        double avgScore = students.stream()
                .mapToDouble(Student::getScore)
                .average()
                .orElse(0.0); // 避免空指针
        double maxScore = students.stream()
                .mapToDouble(Student::getScore)
                .max()
                .orElse(0.0);
        System.out.println("3.3 平均成绩：" + String.format("%.2f", avgScore) + "，最高成绩：" + maxScore);

        // 3.4 匹配操作：是否有成绩≥90的学生
        boolean hasHighScore = students.stream().anyMatch(s -> s.getScore() >= 90);
        System.out.println("3.4 是否有高分学生（≥90）：" + hasHighScore);

        // 3.5 reduce：总成绩求和
        double totalScore = students.stream()
                .mapToDouble(Student::getScore)
                .reduce(0.0, Double::sum);
        System.out.println("3.5 总成绩：" + totalScore);
        System.out.println("----------------------------------------");

        // ====================== 模块4：分组/分区（高级收集） ======================
        System.out.println("===== 4. 分组/分区 =====");
        // 4.1 按性别分组
        Map<String, List<Student>> groupByGender = students.stream()
                .collect(Collectors.groupingBy(Student::getGender));
        // 简化输出：只打印每组姓名
        Map<String, List<String>> genderNameMap = new HashMap<>();
        groupByGender.forEach((gender, list) -> {
            List<String> nameList = list.stream().map(Student::getName).collect(Collectors.toList());
            genderNameMap.put(gender, nameList);
        });
        System.out.println("4.1 按性别分组：" + genderNameMap);

        // 4.2 按性别求平均成绩
        Map<String, Double> avgScoreByGender = students.stream()
                .collect(Collectors.groupingBy(
                        Student::getGender,
                        Collectors.averagingDouble(Student::getScore)
                ));
        System.out.println("4.2 按性别平均成绩：" + avgScoreByGender);

        // 4.3 按成绩≥90分区
        Map<Boolean, List<String>> partitionByName = students.stream()
                .collect(Collectors.partitioningBy(
                        s -> s.getScore() >= 90,
                        Collectors.mapping(Student::getName, Collectors.toList())
                ));
        System.out.println("4.3 按成绩≥90分区：" + partitionByName);
        System.out.println("----------------------------------------");

        // ====================== 模块5：并行流（性能对比） ======================
        System.out.println("===== 5. 并行流 =====");
        // 测试数据：1000万个数，统计偶数个数
        long dataSize = 10_000_000;

        // 5.1 串行流（单线程）
        long start = System.currentTimeMillis();
        long serialCount = IntStream.range(1, (int) dataSize).filter(i -> i % 2 == 0).count();
        long serialTime = System.currentTimeMillis() - start;

        // 5.2 并行流（多线程）
        start = System.currentTimeMillis();
        long parallelCount = IntStream.range(1, (int) dataSize).parallel().filter(i -> i % 2 == 0).count();
        long parallelTime = System.currentTimeMillis() - start;

        System.out.println("5.1 串行流 - 偶数个数：" + serialCount + "，耗时：" + serialTime + "ms");
        System.out.println("5.2 并行流 - 偶数个数：" + parallelCount + "，耗时：" + parallelTime + "ms");
        System.out.println("5.3 结论：并行流在大数据量下效率更高（耗时更短）");
    }
}