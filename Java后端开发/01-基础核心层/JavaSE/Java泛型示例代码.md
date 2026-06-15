## 一、泛型类 示例
```java
// 泛型类
class Box<T> {
    private T data;

    public void setData(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }
}

// 测试
public class Demo {
    public static void main(String[] args) {
        Box<String> strBox = new Box<>();
        strBox.setData("Java泛型");
        System.out.println(strBox.getData());

        Box<Integer> intBox = new Box<>();
        intBox.setData(666);
        System.out.println(intBox.getData());
    }
}
```

## 二、泛型接口 示例
```java
interface IUtil<T> {
    T getInfo();
}

// 实现类指定具体类型
class StringUtil implements IUtil<String> {
    @Override
    public String getInfo() {
        return "字符串类型";
    }
}
```

## 三、泛型方法 示例
```java
public class GenericMethod {
    // 泛型方法
    public static <T> void print(T t) {
        System.out.println(t);
    }

    public static void main(String[] args) {
        print(100);
        print("hello");
        print(3.14);
    }
}
```

## 四、泛型边界（extends 上界）
```java
// 只能是 Number 及其子类
class NumBox<T extends Number> {
    private T num;
    public NumBox(T num) {
        this.num = num;
    }
    public double getDouble() {
        return num.doubleValue();
    }
}

// 使用
NumBox<Integer> b1 = new NumBox<>(10);
NumBox<Double> b2 = new NumBox<>(3.14);
```

## 五、通配符 ? / PECS 完整示例
```java
import java.util.ArrayList;
import java.util.List;

public class WildcardDemo {
    // 上界：生产者 读数据
    public static void read(List<? extends Number> list) {
        for (Number n : list) {
            System.out.println(n);
        }
    }

    // 下界：消费者 写数据
    public static void write(List<? super Integer> list) {
        list.add(10);
        list.add(20);
    }

    public static void main(String[] args) {
        List<Integer> intList = new ArrayList<>();
        write(intList);
        read(intList);
    }
}
```

## 六、泛型擦除 关键演示
```java
List<String> list1 = new ArrayList<>();
List<Integer> list2 = new ArrayList<>();
// 运行时类型完全一致
System.out.println(list1.getClass() == list2.getClass()); // true
```
