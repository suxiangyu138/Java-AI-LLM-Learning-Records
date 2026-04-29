package student_management_system;

import java.util.HashSet;
import java.util.TreeSet;
import java.util.Comparator;

/**
 * Set：无序、不可重复
 * HashSet：哈希表，去重，无序
 * TreeSet：红黑树，自动排序
 */
public class SetDemo {
    public static void testSet() {
        System.out.println("\n========== Set 集合 ==========");
        // HashSet 去重
        HashSet<Student> hashSet = new HashSet<>();
        hashSet.add(new Student(1, "李四", 90));
        hashSet.add(new Student(1, "李四", 90)); // 重复，自动去重
        hashSet.add(new Student(2, "王五", 78));
        System.out.println("HashSet 去重效果：");
        hashSet.forEach(System.out::println);

        // TreeSet 自然排序（实现Comparable）
        TreeSet<Student> treeSet = new TreeSet<>();
        treeSet.addAll(hashSet);
        System.out.println("\nTreeSet 默认按id排序：");
        treeSet.forEach(System.out::println);

        // 自定义比较器：按分数降序
        TreeSet<Student> scoreSortSet = new TreeSet<>(Comparator.comparing(Student::getScore).reversed());
        scoreSortSet.addAll(hashSet);
        System.out.println("\nTreeSet 自定义分数降序：");
        scoreSortSet.forEach(System.out::println);
    }
}