package student_management_system;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Set;

/**
 * Map：键值对、键唯一、值可重复
 * HashMap：哈希表，存取快，无序
 * TreeMap：按键排序
 */
public class MapDemo {
    public static void testMap() {
        System.out.println("\n========== Map 集合 ==========");
        HashMap<Integer, Student> hashMap = new HashMap<>();
        hashMap.put(101, new Student(1, "李四", 90));
        hashMap.put(102, new Student(2, "王五", 78));
        hashMap.put(101, new Student(1, "李四", 95)); // 键重复，覆盖value

        // 遍历1：键集遍历
        System.out.println("HashMap 按键遍历：");
        Set<Integer> keySet = hashMap.keySet();
        for (Integer key : keySet) {
            System.out.println("key=" + key + "，" + hashMap.get(key));
        }

        // 遍历2：键值对遍历
        System.out.println("\n键值对遍历：");
        hashMap.entrySet().forEach(entry ->
                System.out.println(entry.getKey() + " : " + entry.getValue())
        );

        // TreeMap 按键自然排序
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(5, "数学");
        treeMap.put(2, "语文");
        treeMap.put(8, "英语");
        System.out.println("\nTreeMap 按键排序：" + treeMap);
    }
}
