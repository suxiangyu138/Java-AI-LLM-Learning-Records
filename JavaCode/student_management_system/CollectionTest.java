package student_management_system;

import java.util.ArrayList;
import java.util.Collections;

/**
 * 集合框架总入口 + Collections工具类
 */
public class CollectionTest {
    public static void main(String[] args) {
        // List 全部功能
        ListDemo.testList();
        // Set 全部功能
        SetDemo.testSet();
        // Map 全部功能
        MapDemo.testMap();

        // Collections 工具类
        System.out.println("\n========== Collections 工具类 ==========");
        ArrayList<Integer> numList = new ArrayList<>();
        numList.add(5);
        numList.add(1);
        numList.add(9);
        System.out.println("排序前：" + numList);
        Collections.sort(numList);
        System.out.println("排序后：" + numList);
        System.out.println("最大值：" + Collections.max(numList));
        System.out.println("最小值：" + Collections.min(numList));
    }
}