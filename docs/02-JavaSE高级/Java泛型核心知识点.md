# Java 泛型核心知识点（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | Java 泛型机制  
> **版本要求**：JDK 5+  
> **前置基础**：Java 基础语法、面向对象编程、集合框架

---

## 一、核心概念

### 1.1 泛型的定义

泛型（Generics）是 JDK 5 引入的特性，允许在定义类、接口、方法时使用 **类型参数**，将具体类型的确定推迟到使用时，实现代码复用和编译时类型安全。

### 1.2 核心作用

| 作用 | 说明 |
|------|------|
| **编译时类型检查** | 将运行时 `ClassCastException` 提前到编译期发现 |
| **消除强制类型转换** | 取元素时无需 `(Type) obj` 的显式转换 |
| **代码复用** | 同一套逻辑适配多种数据类型，实现通用算法 |

### 1.3 类型参数命名规范

| 字母 | 含义 | 典型场景 |
|------|------|----------|
| `T` | Type（任意类型） | 通用泛型类/方法 |
| `E` | Element（集合元素） | `List<E>`, `Set<E>` |
| `K` | Key（键） | `Map<K, V>` |
| `V` | Value（值） | `Map<K, V>` |
| `N` | Number（数值类型） | `Number` 及其子类 |
| `?` | 未知类型（通配符） | 方法参数中的灵活适配 |

### 1.4 PECS 原则

**Producer Extends, Consumer Super** —— 生产者（读取）用 `? extends T`，消费者（写入）用 `? super T`。

---

## 二、底层原理

### 2.1 类型擦除机制

泛型信息仅在编译期存在，编译后会被 **擦除（Erasure）**，运行时无法获取泛型的具体类型。

**擦除规则**：

| 泛型定义 | 擦除后等价代码 |
|----------|---------------|
| `<T>`（无界） | 擦除为 `Object` |
| `<T extends Number>`（上界） | 擦除为 `Number` |
| `<T extends Comparable<T> & Serializable>`（多界） | 擦除为第一个边界类型 `Comparable` |

```java
class ErasureDemo<T extends Number> {
    private T value;
    public T getValue() { return value; }
}
// 编译后等价于：
// class ErasureDemo {
//     private Number value;
//     public Number getValue() { return value; }
// }
```

### 2.2 编译期 vs 运行时对比

| 维度 | 编译期 | 运行时 |
|------|--------|--------|
| 类型参数 | 有效，进行类型检查 | 被擦除，无法获取具体类型 |
| `List<String>` vs `List<Integer>` | 不同类型 | 同为 `List`（原始类型） |
| `instanceof` 检查 | 可检查 `obj instanceof List` | 禁止 `obj instanceof List<String>` |

---

## 三、代码实现

### 3.1 泛型类

```java
/**
 * 泛型类 —— 类型参数 T 代表任意引用类型。
 *
 * @param <T> 存储的数据类型
 */
class Box<T> {
    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

/**
 * 泛型类使用示例。
 */
public class GenericDemo {
    public static void main(String[] args) {
        // 指定类型为 String
        Box<String> stringBox = new Box<>();
        stringBox.setData("Hello Generics");
        String str = stringBox.getData(); // 无需强制转换

        // 指定类型为 Integer
        Box<Integer> intBox = new Box<>();
        intBox.setData(100);
        Integer num = intBox.getData();
    }
}
```

### 3.2 泛型接口

```java
/**
 * 泛型接口。
 *
 * @param <T> 接口参数类型
 */
interface GenericInterface<T> {
    /** 获取结果 */
    T getResult();
    /** 设置参数 */
    void setParam(T param);
}

/**
 * 实现泛型接口 —— 指定具体类型。
 */
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

/**
 * 实现泛型接口 —— 保留类型参数。
 *
 * @param <T> 接口参数类型
 */
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

### 3.3 泛型方法

```java
/**
 * 泛型方法示例 —— 泛型方法可定义在普通类或泛型类中。
 */
class GenericMethodDemo {

    /**
     * 泛型方法：打印数组。
     *
     * @param <T>   数组元素类型
     * @param array 需要打印的数组（可为 null）
     */
    public <T> void printArray(T[] array) {
        if (array == null) {
            return;
        }
        for (T element : array) {
            System.out.print(element + " ");
        }
        System.out.println();
    }

    /**
     * 泛型方法：获取列表的第一个元素。
     *
     * @param <T>  列表元素类型
     * @param list 列表
     * @return 第一个元素，如果列表为 null 或空则返回 null
     */
    public <T> T getFirstElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
}

/** 测试泛型方法 */
public class TestGenericMethod {
    public static void main(String[] args) {
        GenericMethodDemo demo = new GenericMethodDemo();

        String[] strArray = {"A", "B", "C"};
        demo.printArray(strArray); // A B C

        Integer[] intArray = {1, 2, 3, 4};
        demo.printArray(intArray); // 1 2 3 4

        List<Double> doubleList = Arrays.asList(1.1, 2.2, 3.3);
        Double first = demo.getFirstElement(doubleList);
        System.out.println(first); // 1.1
    }
}
```

### 3.4 泛型通配符

#### 无界通配符 `?`

```java
/**
 * 无界通配符示例 —— 表示任意类型，只能读取，不能写入（null 除外）。
 */
public void printList(List<?> list) {
    for (Object obj : list) {
        System.out.println(obj);
    }
    // list.add("test"); // 编译错误：不能写入
    list.add(null);       // 唯一例外
}
```

#### 上界通配符 `? extends T`

```java
/**
 * 上界通配符示例 —— 表示 T 或 T 的子类/实现类。
 * 遵循 PECS 原则：作为 Producer（生产者）使用，只能读取。
 *
 * @param list Number 及其子类的列表
 * @return 列表中数值的总和
 */
public double sum(List<? extends Number> list) {
    double total = 0.0;
    for (Number num : list) {
        total += num.doubleValue();
    }
    return total;
}

// 使用示例
List<Integer> intList = Arrays.asList(1, 2, 3);
List<Double> doubleList = Arrays.asList(1.1, 2.2);
System.out.println(sum(intList));    // 6.0
System.out.println(sum(doubleList)); // 3.3
```

#### 下界通配符 `? super T`

```java
/**
 * 下界通配符示例 —— 表示 T 或 T 的父类。
 * 遵循 PECS 原则：作为 Consumer（消费者）使用，可以写入 T 类型对象。
 *
 * @param list Integer 及其父类（Number、Object）的列表
 */
public void addIntegers(List<? super Integer> list) {
    list.add(10);
    list.add(20);
    // Integer num = list.get(0); // 编译错误：读取只能是 Object
    Object obj = list.get(0);     // 正确
}

// 使用示例
List<Object> objList = new ArrayList<>();
List<Number> numList = new ArrayList<>();
addIntegers(objList); // 可以传入 List<Object>
addIntegers(numList); // 可以传入 List<Number>
```

### 3.5 泛型限定（多上界）

```java
/**
 * 多上界泛型类 —— T 必须同时实现 Comparable 和 Serializable 接口。
 *
 * @param <T> 元素类型
 */
class BoundedGeneric<T extends Comparable<T> & java.io.Serializable> {
    private T data;

    public int compare(T other) {
        return data.compareTo(other);
    }
}
```

### 3.6 泛型嵌套

```java
// 嵌套泛型使用示例
Map<String, List<Integer>> dataMap = new HashMap<>();
List<Integer> numList = Arrays.asList(1, 2, 3);
dataMap.put("numbers", numList);

for (Map.Entry<String, List<Integer>> entry : dataMap.entrySet()) {
    String key = entry.getKey();
    List<Integer> values = entry.getValue();
    System.out.println(key + ": " + values);
}
```

### 3.7 自定义泛型工具类

```java
/**
 * 泛型集合工具类。
 */
class CollectionUtils {

    /**
     * 将数组转换为 List。
     *
     * @param <T>   元素类型
     * @param array 数组（可为 null）
     * @return 包含数组元素的 List
     */
    public static <T> List<T> arrayToList(T[] array) {
        List<T> list = new ArrayList<>();
        if (array != null) {
            Collections.addAll(list, array);
        }
        return list;
    }

    /**
     * 交换列表中两个位置的元素。
     *
     * @param <T>  元素类型
     * @param list 列表
     * @param i    第一个位置索引
     * @param j    第二个位置索引
     */
    public static <T> void swap(List<T> list, int i, int j) {
        if (list == null || i < 0 || j < 0 || i >= list.size() || j >= list.size()) {
            return;
        }
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}
```

---

## 四、实战要点

### 4.1 集合框架中的泛型应用

```java
// List 泛型
List<String> stringList = new ArrayList<>();
stringList.add("Java");
String s = stringList.get(0); // 无需强制转换

// Map 泛型
Map<Integer, String> map = new HashMap<>();
map.put(1, "One");
String value = map.get(1);

// Set 泛型
Set<Double> doubleSet = new HashSet<>();
doubleSet.add(3.14);
```

### 4.2 自定义泛型数据结构

```java
/**
 * 泛型链表节点。
 */
class Node<T> {
    T data;
    Node<T> next;

    Node(T data) {
        this.data = data;
    }
}

/**
 * 泛型单向链表。
 */
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
```

---

## 五、避坑总结

### 5.1 泛型五大限制

| 限制 | 错误写法 | 正确写法 | 原因 |
|------|----------|----------|------|
| **不能使用基本类型** | `Box<int>` | `Box<Integer>` | 泛型擦除后是 `Object`，基本类型不是对象 |
| **不能创建泛型数组** | `new List<String>[10]` | `new List<?>[10]` | 类型擦除导致数组无法保证类型安全 |
| **静态成员不能用类类型参数** | `static T value;` | `static <E> E getValue()` | 静态成员先于实例存在，类型参数未确定 |
| **不能 instanceof 检查泛型** | `obj instanceof Box<String>` | `obj instanceof Box<?>` | 运行时泛型信息已擦除 |
| **运行时无泛型信息** | 反射获取 `List<String>` 的类型 | 使用 `ParameterizedType` | 类型擦除机制 |

### 5.2 类型擦除导致的重载冲突

```java
// 编译错误：两个方法签名在擦除后相同（都是 List list）
// public void process(List<String> list) {}
// public void process(List<Integer> list) {}
```

### 5.3 PECS 速查表

| 场景 | 通配符 | 可读 | 可写 |
|------|--------|------|------|
| 只读数据 | `? extends T` | ✅ T 类型 | ❌ |
| 只写数据 | `? super T` | ✅ Object | ✅ T 类型 |
| 读写数据 | 不使用通配符 `<T>` | ✅ | ✅ |
| 任意类型 | `?` | ✅ Object | ❌（仅 null） |

### 5.4 静态方法中的泛型陷阱

```java
class StaticGenericError<T> {
    // ❌ 错误：静态变量不能使用类的类型参数 T
    // private static T value;

    // ❌ 错误：静态方法不能使用类的类型参数 T
    // public static T getValue() { return null; }

    // ✅ 正确：静态方法定义自己的泛型参数
    public static <E> E getDefaultValue() {
        return null;
    }
}
```

---

## 六、企业级最佳实践

### 6.1 泛型使用原则

| 原则 | 说明 |
|------|------|
| **PECS 原则** | 生产者（读取数据）用 `? extends`，消费者（写入数据）用 `? super` |
| **泛型方法优先** | 工具类方法优先使用泛型方法而非泛型类，更灵活 |
| **有界通配符提高安全性** | `List<? extends Number>` 比 `List<?>` 更安全 |
| **避免原始类型** | 不使用 `List` 而非 `List<String>`，会绕过编译期类型检查 |

### 6.2 泛型与集合框架选型

- 所有 Java 集合框架类（`List`、`Set`、`Map`）都广泛使用泛型
- 自定义数据结构（如链表、二叉树、缓存）通常设计为泛型类
- 工具类的静态方法优先设计为泛型方法，提供最大灵活性

### 6.3 本章小结

1. 泛型核心是 **类型参数化**，实现代码复用和编译时类型安全
2. 泛型类/接口/方法是基础用法，通配符（`?`、`? extends`、`? super`）解决灵活适配
3. 注意泛型的五大限制：基本类型、泛型数组、静态成员、`instanceof`、类型擦除
4. 实际开发中泛型广泛用于集合、通用工具类、自定义数据结构等场景
5. 严格遵循 **PECS 原则**：生产者用 `extends`，消费者用 `super`
