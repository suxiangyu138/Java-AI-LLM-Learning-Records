# Java泛型程序设计
Java泛型（Generics）是JDK 5引入的核心特性，本质是**参数化类型**，允许在定义类、接口、方法时，不指定具体的数据类型，而是通过参数占位符（如`T`、`E`、`K`、`V`）表示，在使用时再指定具体类型。

泛型的核心价值是**类型安全**和**代码复用**，既能避免强制类型转换带来的错误，又能编写通用的代码适配多种数据类型，是Java面向对象编程中不可或缺的重要技术，也是面试高频考点。本文将全面梳理泛型的核心知识点、语法规则、实战用法及常见误区，帮你快速掌握并灵活运用泛型。

## 一、泛型的核心定义与作用
### 1. 核心定义
泛型，即“参数化类型”，通俗理解：将数据类型作为“参数”传递给类、接口或方法，在声明时不固定具体类型，在创建对象或调用方法时，再指定具体的类型。

例如，定义一个“容器类”，不固定存储`String`、`Integer`还是其他类型，而是用一个占位符`T`表示，使用时再指定`T`为`String`或`Integer`，实现**一个类适配多种类型**。

泛型的本质是**编译期语法糖**：编译阶段对类型进行检查，确保类型安全；编译后执行**类型擦除**（将泛型参数替换为`Object`或其上限类型），不会增加运行时开销。

### 2. 核心作用（为什么需要泛型）
- **类型安全**：编译期校验数据类型，禁止错误类型存入容器，提前暴露`ClassCastException`，避免运行时崩溃；
- **代码复用**：一套通用代码适配所有引用类型，无需为每种类型单独编写容器/工具类；
- **简化代码**：自动完成类型转换，省去手动强转冗余代码；
- **高扩展性**：新增数据类型无需修改通用逻辑，符合**开闭原则**。

### 3. 泛型出现前的问题（对比理解）
JDK5之前依靠`Object`实现通用容器，存在两大缺陷：类型不安全、强制转换繁琐。

```java
// 泛型出现前，用Object实现通用容器
class Container {
    private Object obj;
    // 存入数据
    public void setObj(Object obj) {
        this.obj = obj;
    }
    // 取出数据，需强制类型转换
    public Object getObj() {
        return obj;
    }
}

public class NoGenericTest {
    public static void main(String[] args) {
        Container container = new Container();
        container.setObj(100);
        // 手动强转，类型不匹配则运行时报错
        Integer num = (Integer) container.getObj(); 
        container.setObj("hello");
        String str = (String) container.getObj();
    }
}
```
上述代码中容器可存入任意对象，若存入`Integer`却强转为`String`，运行时抛出类型转换异常；泛型可在编译阶段拦截该错误。

## 二、泛型的基本语法与规范
### 1. 泛型参数占位符（行业约定）
使用单个大写字母作为类型占位符，约定命名如下（仅规范，非强制）：
- `T`（Type）：任意通用类型（泛型类、泛型方法最常用）
- `E`（Element）：集合元素类型（`List<E>`）
- `K`（Key）：键值对键（`Map<K,V>`）
- `V`（Value）：键值对值（`Map<K,V>`）
- `S/U/V`：多类型参数场景

> 限制：泛型参数**不能是基本类型**（`int`/`double`等），仅支持引用类型；基本类型需使用对应包装类（`Integer`/`Double`）。

### 2. 三大核心语法格式
#### 泛型类
```java
class 类名<类型参数1, 类型参数2,...> {
    private 类型参数1 变量名;
    public 类型参数1 方法名(类型参数2 参数名) {
        return 变量名;
    }
}
```

#### 泛型接口
```java
interface 接口名<类型参数1, 类型参数2,...> {
    类型参数1 方法名(类型参数2 参数名);
}
```

#### 泛型方法
> 关键：类型参数`<T>`写在**返回值前面**，独立于类泛型
```java
public <类型参数1, 类型参数2,...> 类型参数1 方法名(类型参数2 参数名) {
    // 方法实现
}
```

## 三、泛型类与泛型接口（重点）
### 1. 泛型类
类声明时定义类型参数，全类属性/方法均可使用；实例化时必须指定具体类型，JDK7支持菱形语法`new 类名<>()`省略右侧类型。

实战示例：通用容器
```java
// 泛型类：Container<T>
class Container<T> {
    private T data;

    public Container(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void showType() {
        System.out.println("当前容器存储的类型：" + data.getClass().getName());
    }
}

// 测试泛型类
public class GenericClassTest {
    public static void main(String[] args) {
        // 指定T为Integer，菱形语法简化写法
        Container<Integer> intContainer = new Container<>(100);
        Integer intData = intContainer.getData();
        intContainer.showType();

        // 指定T为String
        Container<String> strContainer = new Container<>("hello");
        String strData = strContainer.getData();
        strContainer.showType();

        // 编译报错：类型不匹配
        // intContainer.setData("hello");
    }
}
```
关键说明：
1. 泛型参数一旦确定，全类统一约束，编译拦截非法类型赋值；
2. `Container<Integer>`与`Container<String>`属于两种完全不同的类型，不可互相赋值。

### 2. 泛型接口
接口定义泛型参数，实现类分两种写法：指定固定类型、保留泛型参数成为泛型实现类。

```java
// 泛型接口：生成指定类型数据
interface Generator<T> {
    T generate();
}

// 方式1：实现接口并固定类型 String
class StringGenerator implements Generator<String> {
    @Override
    public String generate() {
        return "随机字符串：" + Math.random();
    }
}

// 方式2：保留泛型参数，带上界约束
class NumberGenerator<T extends Number> implements Generator<T> {
    @Override
    public T generate() {
        return (T) Integer.valueOf((int) (Math.random() * 100));
    }
}

// 测试
public class GenericInterfaceTest {
    public static void main(String[] args) {
        Generator<String> strGen = new StringGenerator();
        System.out.println(strGen.generate());

        Generator<Integer> intGen = new NumberGenerator<>();
        System.out.println(intGen.generate());

        Generator<Double> doubleGen = new NumberGenerator<>();
        System.out.println(doubleGen.generate());
    }
}
```

## 四、泛型方法（重点）
泛型方法的类型参数**独立于类**，非泛型类也可定义；调用时编译器自动推断类型，支持手动显式指定`<T>`。

### 基础示例
```java
public class GenericMethodTest {
    // 打印任意类型数据
    public <T> void printData(T data) {
        System.out.println("数据类型：" + data.getClass().getName() + "，数据值：" + data);
    }

    // 带上界约束：仅支持可比较类型
    public <T extends Comparable<T>> T getMax(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static void main(String[] args) {
        GenericMethodTest test = new GenericMethodTest();
        // 自动推断类型
        test.printData(100);
        test.printData("hello");
        test.printData(3.14);
        // 手动指定泛型类型
        test.<Integer>printData(200);

        Integer maxInt = test.getMax(10, 20);
        String maxStr = test.getMax("apple", "banana");
        System.out.println("最大整数：" + maxInt);
        System.out.println("最大字符串：" + maxStr);
    }
}
```

### 核心特点
1. 类型自动推断，无需每次手动写明`<T>`；
2. 与类泛型解耦，普通类也能提供通用方法；
3. 支持多类型参数：`<T, U>` void func(T t, U u)。

## 五、泛型通配符（难点+面试高频）
通配符`?`用于不确定泛型参数类型的场景，分三类：无界、上界、下界，遵循**PECS设计原则**。

### 1. 无界通配符 `?`
等价于`? extends Object`，匹配任意泛型类型；**仅可读、不可写入**（无法确定具体类型，编译禁止set）。
```java
public class WildcardTest {
    public void printContainer(Container<?> container) {
        Object data = container.getData();
        System.out.println("容器数据：" + data);
        // 编译报错，无法写入
        // container.setData(200);
    }

    public static void main(String[] args) {
        Container<Integer> intContainer = new Container<>(100);
        Container<String> strContainer = new Container<>("hello");
        WildcardTest test = new WildcardTest();
        test.printContainer(intContainer);
        test.printContainer(strContainer);
    }
}
```

### 2. 上界通配符 `? extends T`
代表泛型参数为`T`或`T`的子类；仅读取，不能写入，适用于**生产者（只读）**场景。
```java
public class UpperBoundTest {
    // 仅接收Number及其子类容器
    public void printNumberContainer(Container<? extends Number> container) {
        Number data = container.getData();
        System.out.println("数字容器数据：" + data);
        // 禁止写入
        // container.setData(200);
    }

    public static void main(String[] args) {
        Container<Integer> intContainer = new Container<>(100);
        Container<Double> doubleContainer = new Container<>(3.14);
        // String不属于Number，编译报错
        // Container<String> strContainer = new Container<>("hello");
        UpperBoundTest test = new UpperBoundTest();
        test.printNumberContainer(intContainer);
        test.printNumberContainer(doubleContainer);
    }
}
```

### 3. 下界通配符 `? super T`
代表泛型参数为`T`或`T`的父类；**可写入T及其子类对象**，读取只能转为Object，适用于**消费者（写入）**场景。
```java
public class LowerBoundTest {
    // 接收Integer、Number、Object类型容器
    public void addInteger(Container<? super Integer> container, Integer data) {
        container.setData(data);
    }

    public static void main(String[] args) {
        Container<Integer> intContainer = new Container<>(100);
        Container<Number> numberContainer = new Container<>(0);
        LowerBoundTest test = new LowerBoundTest();
        test.addInteger(intContainer, 200);
        test.addInteger(numberContainer, 300);
        // String不是Integer父类，编译报错
        // Container<String> strContainer = new Container<>("hello");
        // test.addInteger(strContainer, 400);

        Object data1 = intContainer.getData();
        Object data2 = numberContainer.getData();
    }
}
```

### 4. PECS 核心总结
> Producer Extends，Consumer Super
- 生产者（只读取数据）：使用`? extends T`；
- 消费者（只写入数据）：使用`? super T`；
- 既要读又要写：不使用通配符，固定泛型参数`<T>`。

| 通配符类型 | 范围 | 读 | 写 | 适用场景 |
| ---- | ---- | ---- | ---- | ---- |
| `?` | 任意类型 | √ Object | × | 统一打印、遍历 |
| `? extends T` | T及子类 | √ T | × | 只读数据源 |
| `? super T` | T及父类 | √ Object | √ T子类 | 写入数据容器 |

## 六、泛型的类型擦除（底层核心原理）
Java泛型是纯编译期语法糖，编译完成后擦除泛型标记，运行时不存在泛型信息。
1. 无上限泛型`<T>` → 擦除替换为`Object`；
2. 带上界`<T extends Number>` → 擦除替换为上限`Number`。

### 擦除示例
原始泛型类：
```java
class Container<T> {
    private T data;
    public T getData() { return data; }
}
```
编译擦除后等价代码：
```java
class Container {
    private Object data;
    public Object getData() { return data; }
}
```

带上限泛型类：
```java
class NumberContainer<T extends Number> {
    private T data;
    public T getData() { return data; }
}
```
擦除后：
```java
class NumberContainer {
    private Number data;
    public Number getData() { return data; }
}
```

### 类型擦除带来的限制（高频坑）
1. 无法通过泛型参数创建实例：`new T()` 编译报错；
2. 不能直接定义泛型数组：`T[] arr = new T[10]` 非法；
3. 泛型参数不支持基本类型（擦除为Object，基本类型无继承关系）；
4. catch异常不能使用泛型：`catch(T e)` 报错；
5. 泛型类静态方法无法使用类定义的泛型参数（静态属于类，泛型属于实例）。

## 七、泛型常见问题与避坑指南
1. **泛型不能使用基本类型**
    解决：使用包装类 `int→Integer`、`boolean→Boolean`。
2. **泛型类静态方法不能使用类泛型参数**
    解决：静态方法定义独立泛型 `public static <T> void func(T t)`。
3. **不能 new T() 创建泛型对象**
    解决：传入`Class<T>`反射创建实例。
4. **泛型数组创建失败**
    解决：`T[] arr = (T[]) new Object[10]`，会出现unchecked警告。
5. **不同泛型实例无法互相赋值**
    说明：类型安全设计，需要兼容则使用通配符。
6. **带通配符容器无法写入数据**
    区分：无界/上界通配符只读；下界通配符允许写入对应子类。

## 八、综合实战：通用集合工具类
整合泛型类、泛型方法、通配符，实现集合增删、查找、排序、打印通用工具：
```java
import java.util.ArrayList;
import java.util.List;

public class GenericCollectionUtil {
    // 添加元素
    public static <T> void addElement(List<T> list, T element) {
        list.add(element);
        System.out.println("添加元素：" + element);
    }

    // 查找元素下标
    public static <T> int findElement(List<T> list, T element) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(element)) {
                return i;
            }
        }
        return -1;
    }

    // 冒泡排序，约束必须实现Comparable
    public static <T extends Comparable<T>> void sortList(List<T> list) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1 - i; j++) {
                if (list.get(j).compareTo(list.get(j + 1)) > 0) {
                    T temp = list.get(j);
                    list.set(j, list.get(j + 1));
                    list.set(j + 1, temp);
                }
            }
        }
        System.out.println("排序后集合：" + list);
    }

    // 无界通配符，打印任意集合
    public static void printList(List<?> list) {
        System.out.print("集合元素：");
        for (Object obj : list) {
            System.out.print(obj + " ");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        List<Integer> intList = new ArrayList<>();
        addElement(intList, 10);
        addElement(intList, 5);
        addElement(intList, 15);
        printList(intList);
        sortList(intList);
        System.out.println("元素10的索引：" + findElement(intList, 10));

        List<String> strList = new ArrayList<>();
        addElement(strList, "apple");
        addElement(strList, "banana");
        addElement(strList, "orange");
        printList(strList);
        sortList(strList);
    }
}
```

## 九、面试高频考点（背诵版）
### 1. Java泛型两大核心作用
1. **类型安全**：编译期类型校验，提前拦截类型转换异常；
2. **代码复用**：一套通用代码适配所有引用类型，消除重复容器类。

### 2. 什么是类型擦除？带来哪些影响？
泛型是编译期语法糖，编译后擦除泛型标识，运行时JVM不感知泛型。
影响：不能`new T()`、不能创建泛型数组、不支持基本类型、静态方法不能使用类泛型参数。

### 3. 三种通配符及使用场景
1. `?` 无界：匹配任意类型，仅读取；
2. `? extends T` 上界：T及其子类，只读，生产者；
3. `? super T` 下界：T及其父类，可写入，消费者。

### 4. 泛型类与泛型方法区别
1. 泛型类参数作用于整个类，实例化时指定；泛型方法参数仅作用于当前方法，调用自动推断；
2. 泛型方法可独立存在，非泛型类也能定义；
3. 泛型类必须实例化指定类型，泛型方法调用可自动推导。

### 5. 为什么泛型不能使用基本数据类型？
编译后泛型擦除为`Object`，基本类型不继承Object，无法向上转型，因此只能使用包装类。

## 十、全文总结
1. 泛型JDK5引入，核心是参数化类型，优势为类型安全、代码复用，底层依靠编译期类型擦除实现；
2. 三大基础用法：泛型类（实例级通用）、泛型接口、泛型方法（方法独立通用）；
3. 通配符是难点，严格遵循PECS原则区分上界/下界使用场景；
4. 开发需规避擦除带来的限制：禁止直接new泛型对象、泛型数组、基本类型；
5. 集合框架`List<E>`、`Map<K,V>`大量使用泛型，是通用工具类、框架封装必备技术，面试必考。