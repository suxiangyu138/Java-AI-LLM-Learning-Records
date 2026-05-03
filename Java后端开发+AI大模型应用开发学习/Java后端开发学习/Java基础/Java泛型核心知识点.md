03.16 09:01
Java泛型核心知识点
---------------------------------------------------------------------------------------------------------------------------------------
一、泛型的基本概念
1. 定义
泛型是JDK5引入的特性，允许在定义类、接口、方法时使用类型参数，将类型确定推迟到使用时，实现代码复用和类型安全。
2. 核心作用
- 编译时类型检查，避免ClassCastException
- 消除强制类型转换，简化代码
- 实现通用算法，提高代码复用性
3. 简单示例（泛型类）
```java
// 泛型类：T是类型参数，代表任意引用类型
class Box<T> {
    private T data;
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
}
// 使用泛型类
public class GenericDemo {
    public static void main(String[] args) {
        // 指定类型为String
        Box<String> stringBox = new Box<>();
        stringBox.setData("Hello Generics");
        String str = stringBox.getData(); // 无需强制转换
        // 指定类型为Integer
        Box<Integer> intBox = new Box<>();
        intBox.setData(100);
        Integer num = intBox.getData();
    }
}
```
---------------------------------------------------------------------------------------------------------------------------------------
二、泛型的使用方式
1. 泛型类
- 语法：类名<类型参数>，类型参数通常用单个大写字母表示
  T：任意类型
  E：集合元素类型
  K：键类型
  V：值类型
  N：数值类型
- 示例：
```java
// 泛型类定义
class GenericClass<T> {
    private T value;
    public GenericClass(T value) {
        this.value = value;
    }
    public T getValue() {
        return value;
    }
}
// 使用
public class TestGenericClass {
    public static void main(String[] args) {
        GenericClass<String> gc1 = new GenericClass<>("Java");
        GenericClass<Double> gc2 = new GenericClass<>(3.14);
        System.out.println(gc1.getValue()); // Java
        System.out.println(gc2.getValue()); // 3.14
    }
}
```
2. 泛型接口
```java
// 泛型接口
interface GenericInterface<T> {
    T getResult();
    void setParam(T param);
}
// 实现泛型接口（指定具体类型）
class StringInterfaceImpl implements GenericInterface<String> {
    private String param;
    @Override
    public String getResult() {
        return param;
    }
    @Override
    public void setParam(String param) {
        this.param = param;
    }
}
// 实现泛型接口（保留类型参数）
class GenericInterfaceImpl<T> implements GenericInterface<T> {
    private T param;
    @Override
    public T getResult() {
        return param;
    }
    @Override
    public void setParam(T param) {
        this.param = param;
    }
}
```
3. 泛型方法
- 语法：修饰符 <类型参数> 返回值类型 方法名(参数列表)
- 泛型方法可以定义在普通类或泛型类中
```java
class GenericMethodDemo {
    // 泛型方法
    public <T> void printArray(T[] array) {
        for (T element : array) {
            System.out.print(element + " ");
        }
        System.out.println();
    }
    // 带返回值的泛型方法
    public <T> T getFirstElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
}
// 测试泛型方法
public class TestGenericMethod {
    public static void main(String[] args) {
        GenericMethodDemo demo = new GenericMethodDemo();
        // 处理String数组
        String[] strArray = {"A", "B", "C"};
        demo.printArray(strArray);
        // 处理Integer数组
        Integer[] intArray = {1, 2, 3, 4};
        demo.printArray(intArray);
        // 获取集合第一个元素
        List<Double> doubleList = Arrays.asList(1.1, 2.2, 3.3);
        Double first = demo.getFirstElement(doubleList);
        System.out.println(first); // 1.1
    }
}
```
---------------------------------------------------------------------------------------------------------------------------------------
三、泛型通配符
1. 无界通配符（?）
- 表示任意类型，只能读取，不能写入（除了null）
```java
public void printList(List<?> list) {
    for (Object obj : list) {
        System.out.println(obj);
    }
    // list.add("test"); // 编译错误，不能写入
    list.add(null); // 唯一例外
}
```
2. 上界通配符（? extends T）
- 表示T或T的子类/实现类
- 只能读取，不能写入（除了null）
```java
// 求和方法，支持Number及其子类（Integer、Double等）
public double sum(List<? extends Number> list) {
    double total = 0.0;
    for (Number num : list) {
        total += num.doubleValue();
    }
    return total;
}
// 使用
List<Integer> intList = Arrays.asList(1, 2, 3);
List<Double> doubleList = Arrays.asList(1.1, 2.2);
System.out.println(sum(intList)); // 6.0
System.out.println(sum(doubleList)); // 3.3
```
3. 下界通配符（? super T）
- 表示T或T的父类
- 可以写入T类型的对象，读取时只能按Object处理
```java
// 添加整数到列表，支持Integer及其父类（Number、Object）
public void addIntegers(List<? super Integer> list) {
    list.add(10);
    list.add(20);
    // Integer num = list.get(0); // 编译错误，读取只能是Object
    Object obj = list.get(0); // 正确
}
// 使用
List<Object> objList = new ArrayList<>();
List<Number> numList = new ArrayList<>();
addIntegers(objList);
addIntegers(numList);
```
---------------------------------------------------------------------------------------------------------------------------------------
四、泛型的限制
1. 不能使用基本类型作为类型参数
- 错误：Box<int> box = new Box<>();
- 正确：Box<Integer> box = new Box<>();（使用包装类）
2. 不能创建泛型数组
```java
// 错误
// List<String>[] listArray = new List<String>[10];
// 正确（使用通配符）
List<?>[] listArray = new List<?>[10];
```
3. 不能在静态方法/静态变量中使用类的类型参数
```java
class StaticGenericError<T> {
    // 错误：静态变量不能使用类型参数T
    // private static T value;
    // 错误：静态方法不能使用类的类型参数T
    // public static T getValue() { return null; }
    // 正确：静态方法定义自己的泛型参数
    public static <E> E getDefaultValue() {
        return null;
    }
}
```
4. 不能使用instanceof检查泛型类型
```java
Box<String> box = new Box<>();
// 错误
// if (box instanceof Box<String>) {}
// 正确（擦除泛型）
if (box instanceof Box<?>) {}
```
5. 泛型类型擦除
- 编译后泛型信息被擦除，运行时无法获取泛型类型
- 擦除规则：无界泛型擦除为Object，有界泛型擦除为上界类型
```java
class ErasureDemo<T extends Number> {
    private T value;
    public T getValue() {
        return value;
    }
}
// 编译后等价于
class ErasureDemo {
    private Number value;
    public Number getValue() {
        return value;
    }
}
```
---------------------------------------------------------------------------------------------------------------------------------------
五、泛型的高级应用
1. 泛型限定（多上界）
```java
// T必须同时实现Comparable和Serializable接口
class BoundedGeneric<T extends Comparable<T> & java.io.Serializable> {
    private T data;
    public int compare(T other) {
        return data.compareTo(other);
    }
}
```
2. 泛型嵌套
```java
// 嵌套泛型
Map<String, List<Integer>> dataMap = new HashMap<>();
List<Integer> numList = Arrays.asList(1, 2, 3);
dataMap.put("numbers", numList);
// 遍历嵌套泛型
for (Map.Entry<String, List<Integer>> entry : dataMap.entrySet()) {
    String key = entry.getKey();
    List<Integer> values = entry.getValue();
    System.out.println(key + ": " + values);
}
```
3. 自定义泛型工具类
```java
// 泛型工具类：集合操作
class CollectionUtils {
    // 将数组转换为列表
    public static <T> List<T> arrayToList(T[] array) {
        List<T> list = new ArrayList<>();
        if (array != null) {
            Collections.addAll(list, array);
        }
        return list;
    }
    // 交换列表中两个元素的位置
    public static <T> void swap(List<T> list, int i, int j) {
        if (list == null || i < 0 || j < 0 || i >= list.size() || j >= list.size()) {
            return;
        }
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}
// 使用工具类
public class TestCollectionUtils {
    public static void main(String[] args) {
        String[] strArray = {"a", "b", "c"};
        List<String> strList = CollectionUtils.arrayToList(strArray);
        CollectionUtils.swap(strList, 0, 2);
        System.out.println(strList); // [c, b, a]
    }
}
```
---------------------------------------------------------------------------------------------------------------------------------------
六、泛型的实际应用场景
1. 集合框架（Java内置泛型）
```java
// List泛型
List<String> stringList = new ArrayList<>();
stringList.add("Java");
String s = stringList.get(0);
// Map泛型
Map<Integer, String> map = new HashMap<>();
map.put(1, "One");
String value = map.get(1);
// Set泛型
Set<Double> doubleSet = new HashSet<>();
doubleSet.add(3.14);
```
2. 自定义通用数据结构
```java
// 泛型链表节点
class Node<T> {
    T data;
    Node<T> next;
    Node(T data) {
        this.data = data;
    }
}
// 泛型链表
class LinkedList<T> {
    private Node<T> head;
    public void add(T data) {
        Node<T> newNode = new Node<>(data);
        if (head == null) {
            head = newNode;
        } else {
            Node<T> current = head;
            while (current.next != null) {
                current = current.next;
            }
            current.next = newNode;
        }
    }
    public void print() {
        Node<T> current = head;
        while (current != null) {
            System.out.print(current.data + " -> ");
            current = current.next;
        }
        System.out.println("null");
    }
}
// 使用泛型链表
public class TestLinkedList {
    public static void main(String[] args) {
        LinkedList<Integer> intList = new LinkedList<>();
        intList.add(1);
        intList.add(2);
        intList.add(3);
        intList.print(); // 1 -> 2 -> 3 -> null
        LinkedList<String> strList = new LinkedList<>();
        strList.add("A");
        strList.add("B");
        strList.print(); // A -> B -> null
    }
}
```
---------------------------------------------------------------------------------------------------------------------------------------
总结
1. 泛型核心是类型参数化，实现代码复用和类型安全
2. 泛型类/接口/方法是基础用法，通配符（?、extends、super）解决灵活适配问题
3. 注意泛型的限制：基本类型、数组、静态成员、类型擦除等
4. 实际开发中泛型广泛用于集合、通用工具类、自定义数据结构等场景
5. 遵循PECS原则：生产者（读取）用extends，消费者（写入）用super

