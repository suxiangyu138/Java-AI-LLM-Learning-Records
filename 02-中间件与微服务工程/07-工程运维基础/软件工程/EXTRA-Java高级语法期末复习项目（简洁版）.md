# Java 高级语法期末复习项目（简洁版）

> 通过实战编码巩固 Java 高级核心知识点，覆盖集合框架、多线程、IO 流、反射、异常处理、Lambda/Stream、泛型等期末高频考点。

---

## 目录

- [一、项目概述](#一项目概述)
- [二、项目分步实现](#二项目分步实现)
- [第一步：创建项目](#第一步创建项目)
- [第二步：泛型模块](#第二步泛型模块)
- [第三步：集合框架模块](#第三步集合框架模块)
- [第四步：异常处理模块](#第四步异常处理模块)
- [第五步：IO 流模块](#第五步io-流模块)
- [第六步：反射模块](#第六步反射模块)
- [第七步：多线程模块](#第七步多线程模块)

---

## 一、项目概述

### 核心覆盖知识点

| 分类 | 考点 |
|------|------|
| **集合框架** | ArrayList、HashMap、HashSet，迭代器、增强 for，集合排序 |
| **多线程** | Thread 类、Runnable 接口，线程同步（synchronized、Lock），线程池 |
| **IO 流** | 字节流、字符流，文件读写，缓冲流，对象序列化/反序列化 |
| **反射** | 获取 Class 对象，调用构造/方法/变量，注解应用 |
| **异常处理** | 自定义异常，try-catch-finally，throws/throw |
| **Lambda/Stream** | Lambda 表达式与 Stream 流，注解与泛型 |

### 项目环境

| 项 | 说明 |
|----|------|
| JDK | 1.8+ |
| IDE | IntelliJ IDEA / Eclipse |
| 依赖 | 仅 JDK 自带类库，无需第三方依赖 |

### 项目结构

```
com.java.review
├── collection       // 集合框架模块
├── thread           // 多线程模块
├── io               // IO 流模块
├── reflection       // 反射模块
├── exception        // 异常处理模块
├── lambda           // Lambda 与 Stream 模块
├── generic          // 泛型模块
└── Main.java        // 项目入口，整合测试
```

---

## 二、项目分步实现

### 第一步：创建项目

1. 新建 Java 项目，JDK 选 1.8+，取消模板
2. 命名 `JavaAdvancedReview`
3. 在 src 下创建 `com.java.review` 包及子包

---

### 第二步：泛型模块

#### 泛型类

```java
package com.java.review.generic;

public class GenericClass<T> {
    private T data;

    public GenericClass(T data) { this.data = data; }
    public T getData() { return data; }

    public void showData() {
        System.out.println("数据：" + data + "，类型：" + data.getClass().getName());
    }
}
```

#### 泛型方法

```java
package com.java.review.generic;

import java.util.List;

public class GenericMethod {
    public <E> void printData(E data) {
        System.out.println("打印：" + data);
    }

    public static <E> E getMax(E[] array) {
        if (array == null || array.length == 0) return null;
        E max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (((Comparable<E>) max).compareTo(array[i]) < 0) max = array[i];
        }
        return max;
    }

    public void printList(List<?> list) {
        for (Object obj : list) System.out.println(obj);
    }
}
```

---

### 第三步：集合框架模块（期末重点）

#### 集合基础

```java
package com.java.review.collection;

import java.util.*;

public class CollectionTest {
    public static void main(String[] args) {
        // ArrayList（有序可重复）
        List<String> arrayList = new ArrayList<>();
        arrayList.add("Java"); arrayList.add("复习");
        arrayList.remove(0); arrayList.set(1, "期末");
        for (String str : arrayList) System.out.print(str + " ");

        // HashSet（无序不可重复）
        Set<String> hashSet = new HashSet<>();
        hashSet.add("A"); hashSet.add("A"); // 重复添加失败
    }
}
```

#### Map 测试

```java
package com.java.review.collection;

import java.util.*;

public class MapTest {
    public static void main(String[] args) {
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("Java", 98); hashMap.put("英语", 95);
        hashMap.remove("英语");
        for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
            System.out.println(entry.getKey() + "：" + entry.getValue());
        }
    }
}
```

#### 集合排序

```java
package com.java.review.collection;

import java.util.*;

class Student implements Comparable<Student> {
    private String name; private int score;
    public Student(String name, int score) { this.name = name; this.score = score; }
    @Override public int compareTo(Student o) { return o.score - this.score; }
    @Override public String toString() { return "Student{name='" + name + "', score=" + score + "}"; }
}

public class CollectionSort {
    public static void main(String[] args) {
        List<Student> list = new ArrayList<>();
        list.add(new Student("张三", 85)); list.add(new Student("李四", 98));
        Collections.sort(list); // 自然排序
        Collections.sort(list, (o1, o2) -> o1.name.compareTo(o2.name)); // 定制排序
    }
}
```

---

### 第四步：异常处理模块

```java
package com.java.review.exception;

// 编译时异常
class ScoreException extends Exception {
    public ScoreException(String message) { super(message); }
}

// 运行时异常
class AgeException extends RuntimeException {
    public AgeException(String message) { super(message); }
}

public class ExceptionTest {
    public static void main(String[] args) {
        try {
            checkScore(105);
        } catch (ScoreException e) {
            System.out.println(e.getMessage());
        } finally { System.out.println("测试结束"); }
        checkAge(-5); // 运行时异常，可省略 try-catch
    }

    public static void checkScore(int score) throws ScoreException {
        if (score < 0 || score > 100)
            throw new ScoreException("分数需在 0-100 之间");
    }

    public static void checkAge(int age) {
        if (age < 0)
            throw new AgeException("年龄不能为负");
    }
}
```

---

### 第五步：IO 流模块（期末重点）

```java
package com.java.review.io;

import java.io.*;

public class FileIO {
    private static final String TXT_PATH = "test.txt";

    public static void main(String[] args) throws IOException {
        // 写入文本
        try (FileWriter writer = new FileWriter(TXT_PATH)) {
            writer.write("Java 期末复习");
            writer.flush();
        }
        // 读取文本
        try (FileReader reader = new FileReader(TXT_PATH)) {
            int ch;
            while ((ch = reader.read()) != -1)
                System.out.print((char) ch);
        }
    }
}

// 缓冲流优化
try (BufferedWriter bw = new BufferedWriter(new FileWriter("test.txt"))) {
    bw.write("缓冲流测试"); bw.newLine();
}

// 序列化
class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private transient int age; // transient 不参与序列化
}
```

---

### 第六步：反射模块（期末难点）

```java
package com.java.review.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

class Person {
    public String name;
    public Person(String name) { this.name = name; }
    public void sayHello() { System.out.println("Hello, " + name); }
}

public class ReflectionTest {
    public static void main(String[] args) throws Exception {
        // 获取 Class 对象（3 种方式）
        Class<?> clazz = Class.forName("com.java.review.reflection.Person");

        // 调用有参构造创建对象
        Constructor<?> constructor = clazz.getConstructor(String.class);
        Person p = (Person) constructor.newInstance("张三");

        // 调用方法
        Method method = clazz.getMethod("sayHello");
        method.invoke(p);
    }
}
```

---

### 第七步：多线程模块（期末重点+难点）

#### 线程创建

```java
package com.java.review.thread;

class MyRunnable implements Runnable {
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(Thread.currentThread().getName() + "执行：" + i);
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }
    }
}

public class ThreadTest {
    public static void main(String[] args) {
        Thread t1 = new Thread(new MyRunnable(), "线程1");
        Thread t2 = new Thread(new MyRunnable(), "线程2");
        t1.start(); t2.start();
    }
}
```

#### 线程同步（ReentrantLock）

```java
package com.java.review.thread;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Ticket implements Runnable {
    private int ticketNum = 10;
    private final Lock lock = new ReentrantLock();

    @Override
    public void run() {
        while (ticketNum > 0) {
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName()
                        + "卖出第" + ticketNum + "张");
                ticketNum--;
                Thread.sleep(100);
            } catch (InterruptedException e) {
            } finally { lock.unlock(); }
        }
    }
}

public class ThreadSync {
    public static void main(String[] args) {
        Ticket ticket = new Ticket();
        new Thread(ticket, "窗口1").start();
        new Thread(ticket, "窗口2").start();
    }
}
```
