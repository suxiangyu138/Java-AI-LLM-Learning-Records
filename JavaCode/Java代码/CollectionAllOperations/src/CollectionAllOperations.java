import java.util.*;
import java.util.stream.Collectors;

/**
 * Java 集合全操作体验（修复版）
 * 覆盖：List/Set/Map 的增删改查、排序、去重、遍历、批量操作、集合转换
 * 修复问题：ArrayList<String> 添加int类型导致的类型不匹配
 */
public class CollectionAllOperations {
    public static void main(String[] args) {
        // ====================== 模块1：List 集合核心操作（ArrayList/LinkedList） ======================
        System.out.println("===== 1. List 集合操作 =====");
        // 1.1 初始化 & 添加元素（字符串类型List）
        List<String> arrayList = new ArrayList<>();
        arrayList.add("Java"); // 单个添加
        arrayList.add("Python");
        arrayList.add("C++");
        arrayList.add(1, "Go"); // 指定索引添加
        arrayList.addAll(Arrays.asList("PHP", "JavaScript")); // 批量添加
        System.out.println("ArrayList初始化后：" + arrayList);

        // 1.2 查询 & 判断
        System.out.println("索引2的元素：" + arrayList.get(2)); // 按索引查
        System.out.println("是否包含Python：" + arrayList.contains("Python")); // 判断包含
        System.out.println("Python的索引：" + arrayList.indexOf("Python")); // 查索引
        System.out.println("集合是否为空：" + arrayList.isEmpty()); // 判断空
        System.out.println("集合大小：" + arrayList.size()); // 大小

        // 1.3 修改元素
        arrayList.set(0, "Java8"); // 按索引修改
        System.out.println("修改索引0后：" + arrayList);

        // 1.4 删除元素
        arrayList.remove(1); // 按索引删
        arrayList.remove("PHP"); // 按元素删
        arrayList.removeAll(Arrays.asList("C++", "JavaScript")); // 批量删除
        System.out.println("删除后：" + arrayList);

        // 1.5 排序
        List<Integer> numList = new ArrayList<>(Arrays.asList(5, 2, 8, 1, 9));
        Collections.sort(numList); // 自然升序
        System.out.println("numList升序：" + numList);
        Collections.sort(numList, Collections.reverseOrder()); // 倒序
        System.out.println("numList倒序：" + numList);

        // 1.6 遍历（4种方式）
        System.out.println("ArrayList遍历（for循环）：");
        for (int i = 0; i < arrayList.size(); i++) {
            System.out.print(arrayList.get(i) + " ");
        }
        System.out.println("\nArrayList遍历（for-each）：");
        for (String s : arrayList) {
            System.out.print(s + " ");
        }
        System.out.println("\nArrayList遍历（迭代器）：");
        Iterator<String> iterator = arrayList.iterator();
        while (iterator.hasNext()) {
            System.out.print(iterator.next() + " ");
        }
        System.out.println("\nArrayList遍历（forEach+Lambda）：");
        arrayList.forEach(s -> System.out.print(s + " "));

        // 1.7 ArrayList vs LinkedList 增删效率对比（修复核心：单独创建Integer类型集合）
        List<Integer> arrayListTest = new ArrayList<>(); // 数字类型ArrayList
        List<Integer> linkedListTest = new LinkedList<>(); // 数字类型LinkedList

        // 尾部添加（效率相近）
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            arrayListTest.add(i); // 类型匹配：Integer
        }
        System.out.println("\nArrayList尾部添加10万元素耗时：" + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            linkedListTest.add(i); // 类型匹配：Integer
        }
        System.out.println("LinkedList尾部添加10万元素耗时：" + (System.currentTimeMillis() - start) + "ms");

        // 头部添加（LinkedList效率更高）
        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            arrayListTest.add(0, i);
        }
        System.out.println("ArrayList头部添加1万元素耗时：" + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            linkedListTest.add(0, i);
        }
        System.out.println("LinkedList头部添加1万元素耗时：" + (System.currentTimeMillis() - start) + "ms");
        System.out.println();

        // ====================== 模块2：Set 集合核心操作（HashSet/TreeSet） ======================
        System.out.println("===== 2. Set 集合操作 =====");
        // 2.1 HashSet（无序、不可重复）
        Set<String> hashSet = new HashSet<>();
        hashSet.add("Apple");
        hashSet.add("Banana");
        hashSet.add("Apple"); // 重复元素不添加
        hashSet.addAll(Arrays.asList("Orange", "Grape"));
        System.out.println("HashSet初始化：" + hashSet);

        // 2.2 查询 & 判断（无索引，只能判断是否包含）
        System.out.println("是否包含Banana：" + hashSet.contains("Banana"));

        // 2.3 删除
        hashSet.remove("Orange");
        hashSet.clear(); // 清空
        System.out.println("HashSet清空后：" + hashSet);

        // 2.4 TreeSet（有序、不可重复，自然排序）
        Set<Integer> treeSet = new TreeSet<>();
        treeSet.add(5);
        treeSet.add(2);
        treeSet.add(8);
        treeSet.add(1);
        System.out.println("TreeSet（自然排序）：" + treeSet);

        // 2.5 去重操作（Set核心场景）
        List<String> duplicateList = Arrays.asList("a", "b", "a", "c", "b", "d");
        Set<String> uniqueSet = new HashSet<>(duplicateList);
        List<String> uniqueList = new ArrayList<>(uniqueSet);
        System.out.println("原列表（含重复）：" + duplicateList);
        System.out.println("去重后列表：" + uniqueList);

        // 2.6 遍历（无索引，3种方式）
        System.out.println("TreeSet遍历（for-each）：");
        for (Integer num : treeSet) {
            System.out.print(num + " ");
        }
        System.out.println("\nTreeSet遍历（迭代器）：");
        Iterator<Integer> setIterator = treeSet.iterator();
        while (setIterator.hasNext()) {
            System.out.print(setIterator.next() + " ");
        }
        System.out.println("\nTreeSet遍历（forEach+Lambda）：");
        treeSet.forEach(num -> System.out.print(num + " "));
        System.out.println();

        // ====================== 模块3：Map 集合核心操作（HashMap/TreeMap） ======================
        System.out.println("===== 3. Map 集合操作 =====");
        // 3.1 HashMap（无序、键唯一）
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("Java", 95); // 单个添加
        hashMap.put("Python", 90);
        hashMap.put("Java", 98); // 重复键，覆盖值
        hashMap.putAll(Map.of("C++", 85, "Go", 88)); // 批量添加（Java 9+）
        System.out.println("HashMap初始化：" + hashMap);

        // 3.2 查询 & 判断
        System.out.println("Java的分值：" + hashMap.get("Java")); // 按键查值
        System.out.println("是否包含键Python：" + hashMap.containsKey("Python"));
        System.out.println("是否包含值85：" + hashMap.containsValue(85));
        System.out.println("Map大小：" + hashMap.size());

        // 3.3 修改
        hashMap.replace("Python", 92); // 按键修改值
        System.out.println("修改Python分值后：" + hashMap);

        // 3.4 删除
        hashMap.remove("C++"); // 按键删
        hashMap.remove("Go", 88); // 按键+值删（值匹配才删）
        System.out.println("删除后：" + hashMap);

        // 3.5 TreeMap（按键排序）
        Map<String, Integer> treeMap = new TreeMap<>(hashMap);
        System.out.println("TreeMap（按键排序）：" + treeMap);

        // 3.6 遍历（4种方式）
        System.out.println("HashMap遍历（keySet）：");
        for (String key : hashMap.keySet()) {
            System.out.println(key + "：" + hashMap.get(key));
        }
        System.out.println("HashMap遍历（values）：");
        for (Integer value : hashMap.values()) {
            System.out.print(value + " ");
        }
        System.out.println("\nHashMap遍历（entrySet）：");
        for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
            System.out.println(entry.getKey() + "：" + entry.getValue());
        }
        System.out.println("HashMap遍历（forEach+Lambda）：");
        hashMap.forEach((k, v) -> System.out.println(k + "：" + v));

        // 3.7 批量操作（Stream）
        // 过滤值>90的键值对
        Map<String, Integer> filterMap = hashMap.entrySet().stream()
                .filter(entry -> entry.getValue() > 90)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        System.out.println("过滤后（值>90）：" + filterMap);
        System.out.println();

        // ====================== 模块4：集合间转换 & 工具类 ======================
        System.out.println("===== 4. 集合转换 & 工具类 =====");
        // 4.1 List ↔ Set
        List<Integer> list = new ArrayList<>(Arrays.asList(1,2,3));
        Set<Integer> set = new HashSet<>(list); // List→Set
        List<Integer> list2 = new ArrayList<>(set); // Set→List

        // 4.2 Map ↔ List
        List<String> keyList = new ArrayList<>(hashMap.keySet()); // Map键→List
        List<Integer> valueList = new ArrayList<>(hashMap.values()); // Map值→List
        System.out.println("Map键列表：" + keyList);
        System.out.println("Map值列表：" + valueList);

        // 4.3 Collections工具类常用操作
        List<Integer> toolList = new ArrayList<>(Arrays.asList(3,1,4,2));
        Collections.reverse(toolList); // 反转
        System.out.println("反转后：" + toolList);
        Collections.shuffle(toolList); // 随机打乱
        System.out.println("随机打乱后：" + toolList);
        Collections.fill(toolList, 0); // 填充
        System.out.println("填充0后：" + toolList);

        // 4.4 不可变集合（Java 9+）
        List<String> unmodifiableList = List.of("a", "b", "c"); // 不可变List
        // unmodifiableList.add("d"); // 报错：UnsupportedOperationException
        Map<String, Integer> unmodifiableMap = Map.of("a",1, "b",2); // 不可变Map
        System.out.println("不可变List：" + unmodifiableList);
    }
}