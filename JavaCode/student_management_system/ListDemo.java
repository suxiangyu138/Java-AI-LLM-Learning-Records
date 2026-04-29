package student_management_system;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * List：有序、可重复、索引访问
 * ArrayList：数组实现，查询快、增删慢
 * LinkedList：双向链表，增删快、查询慢
 */
public class ListDemo {
    public static void testList() {
        System.out.println("========== List 集合 ==========");
        ArrayList<Student> arrayList = new ArrayList<>();
        arrayList.add(new Student(3, "张三", 85));
        arrayList.add(new Student(1, "李四", 90));
        arrayList.add(new Student(2, "王五", 78));

        // 方式1：普通for遍历
        System.out.println("普通for遍历：");
        for (int i = 0; i < arrayList.size(); i++) {
            System.out.println(arrayList.get(i));
        }

        // 方式2：增强for
        System.out.println("\n增强for遍历：");
        for (Student s : arrayList) {
            System.out.println(s);
        }

        // 方式3：迭代器遍历
        System.out.println("\n迭代器遍历：");
        Iterator<Student> it = arrayList.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }

        // LinkedList 演示
        LinkedList<String> link = new LinkedList<>();
        link.add("Java");
        link.add("集合");
        link.addFirst("前端");
        System.out.println("\nLinkedList 首尾操作：" + link);
    }
}