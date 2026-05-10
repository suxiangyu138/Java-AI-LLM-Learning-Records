03.31 01:08
Java高级语法期末复习项目（简洁版）
一、项目概述
1.1 项目目标
通过实战编码巩固Java高级核心知识点，覆盖期末高频考点，适配期末备考、知识点复盘及课程设计参考，适用于掌握Java基础、需强化高级语法应用的学生。
1.2 核心覆盖知识点（期末高频）
集合框架：ArrayList、HashMap、HashSet，迭代器、增强for，集合排序
多线程：Thread类、Runnable接口，线程同步（synchronized、Lock），线程池
IO流：字节流、字符流，文件读写，缓冲流，对象序列化/反序列化
反射：获取Class对象，调用构造/方法/变量，注解应用
异常处理：自定义异常，try-catch-finally，throws/throw
Lambda表达式与Stream流，注解与泛型
1.3 项目环境
JDK：1.8+（推荐，贴合期末考核）
开发工具：IntelliJ IDEA（或Eclipse）
配置：仅用JDK自带类库，无需第三方依赖
1.4 项目整体结构
com.java.review
├─ collection       // 集合框架模块
├─ thread           // 多线程模块
├─ io               // IO流模块
├─ reflection       // 反射模块
├─ exception        // 异常处理模块
├─ lambda           // Lambda与Stream模块
├─ generic          // 泛型模块
└─ Main.java        // 项目入口，整合测试
二、项目分步实现（简洁版）
每个模块独立实现，先单独调试，再通过Main类整合；代码保留核心逻辑与考点注释。
第一步：创建项目（IDEA）
新建Java项目，JDK选1.8+，取消模板，命名为JavaAdvancedReview；
在src下创建com.java.review包，再创建各子包（collection、thread等）。
第二步：泛型模块
2.1 泛型类（GenericClass.java）
package com.java.review.generic;
public class GenericClass<T> {
    private T data;
    public GenericClass(T data) { this.data = data; }
    public T getData() { return data; }
    public void showData() {
        System.out.println("数据：" + data + "，类型：" + data.getClass().getName());
    }
}
2.2 泛型方法（GenericMethod.java）
package com.java.review.generic;
import java.util.List;
public class GenericMethod {
    public <E> void printData(E data) { System.out.println("打印：" + data); }
    public static <E> E getMax(E[] array) {
        if (array == null || array.length == 0) return null;
        E max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (((Comparable<E>) max).compareTo(array[i]) < 0) max = array[i];
        }
        return max;
    }
    public void printList(List<?> list) { for (Object obj : list) System.out.println(obj); }
}
第三步：集合框架模块（期末重点）
3.1 集合基础（CollectionTest.java）
package com.java.review.collection;
import java.util.ArrayList;import java.util.HashSet;import java.util.Iterator;import java.util.List;import java.util.Set;
public class CollectionTest {
    public static void main(String[] args) {
        // ArrayList（有序可重复）
        List<String> arrayList = new ArrayList<>();
        arrayList.add("Java");arrayList.add("复习");
        arrayList.remove(0);arrayList.set(1, "期末");
        // 遍历：增强for、迭代器、普通for
        for (String str : arrayList) System.out.print(str + " ");
        // HashSet（无序不可重复）
        Set<String> hashSet = new HashSet<>();
        hashSet.add("A");hashSet.add("A"); // 重复添加失败
    }
}
3.2 Map测试（HashMap.java）
package com.java.review.collection;
import java.util.HashMap;import java.util.Map;import java.util.Set;
public class MapTest {
    public static void main(String[] args) {
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("Java", 98);hashMap.put("英语", 95);
        hashMap.remove("英语");
        // 遍历：keySet、entrySet（推荐）
        for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
            System.out.println(entry.getKey() + "：" + entry.getValue());
        }
    }
}
3.3 集合排序（CollectionSort.java）
package com.java.review.collection;
import java.util.ArrayList;import java.util.Collections;import java.util.Comparator;import java.util.List;
class Student implements Comparable<Student> {
    private String name;private int score;
    public Student(String name, int score) { this.name = name;this.score = score; }
    @Override public int compareTo(Student o) { return o.score - this.score; }
    @Override public String toString() { return "Student{name='"+name+"', score="+score+"}"; }
}
public class CollectionSort {
    public static void main(String[] args) {
        List<Student> list = new ArrayList<>();
        list.add(new Student("张三",85));list.add(new Student("李四",98));
        Collections.sort(list); // 自然排序（按成绩降序）
        // 定制排序（按姓名）
        Collections.sort(list, (o1,o2)->o1.name.compareTo(o2.name));
    }
}
第四步：异常处理模块
4.1 自定义异常（CustomException.java）
package com.java.review.exception;
// 编译时异常
public class ScoreException extends Exception {
    public ScoreException(String message) { super(message); }
}
// 运行时异常
class AgeException extends RuntimeException {
    public AgeException(String message) { super(message); }
}
4.2 异常测试（ExceptionTest.java）
package com.java.review.exception;
public class ExceptionTest {
    public static void main(String[] args) {
        try {
            checkScore(105);
        } catch (ScoreException e) {
            System.out.println(e.getMessage());
        } finally { System.out.println("测试结束"); }
        checkAge(-5); // 运行时异常，可省略try-catch
    }
    public static void checkScore(int score) throws ScoreException {
        if (score<0||score>100) throw new ScoreException("分数需在0-100之间");
    }
    public static void checkAge(int age) {
        if (age<0) throw new AgeException("年龄不能为负");
    }
}
第五步：IO流模块（期末重点）
5.1 文件读写（FileIO.java）
package com.java.review.io;
import java.io.FileReader;import java.io.FileWriter;import java.io.IOException;
public class FileIO {
    private static final String TXT_PATH = "test.txt";
    public static void main(String[] args) throws IOException {
        // 写入文本
        try (FileWriter writer = new FileWriter(TXT_PATH)) {
            writer.write("Java期末复习");writer.flush();
        }
        // 读取文本
        try (FileReader reader = new FileReader(TXT_PATH)) {
            int ch;while ((ch=reader.read())!=-1) System.out.print((char)ch);
        }
    }
}
5.2 缓冲流与序列化
// BufferIO.java核心（缓冲流优化）
try (BufferedWriter bw = new BufferedWriter(new FileWriter("test.txt"))) {
    bw.write("缓冲流测试");bw.newLine();
}
// SerializeTest.java核心（序列化）
import java.io.Serializable;
class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;private transient int age; // transient不参与序列化
}
第六步：反射模块（期末难点）
package com.java.review.reflection;
import java.lang.reflect.Constructor;import java.lang.reflect.Method;
class Person {
    public String name;public Person(String name) { this.name = name; }
    public void sayHello() { System.out.println("Hello, "+name); }
}
public class ReflectionTest {
    public static void main(String[] args) throws Exception {
        // 获取Class对象（3种方式）
        Class<?> clazz = Class.forName("com.java.review.reflection.Person");
        // 调用有参构造创建对象
        Constructor<?> constructor = clazz.getConstructor(String.class);
        Person p = (Person) constructor.newInstance("张三");
        // 调用方法
        Method method = clazz.getMethod("sayHello");
        method.invoke(p);
    }
}
第七步：多线程模块（期末重点+难点）
7.1 线程创建（ThreadTest.java）
package com.java.review.thread;
// 实现Runnable（推荐）
class MyRunnable implements Runnable {
    @Override public void run() {
        for (int i=0;i<5;i++) {
            System.out.println(Thread.currentThread().getName()+"执行："+i);
            try { Thread.sleep(500); } catch (InterruptedException e) {e.printStackTrace();}
        }
    }
}
public class ThreadTest {
    public static void main(String[] args) {
        Thread t1 = new Thread(new MyRunnable(), "线程1");
        Thread t2 = new Thread(new MyRunnable(), "线程2");
        t1.start();t2.start();
    }
}
7.2 线程同步（ThreadSync.java）
package com.java.review.thread;
import java.util.concurrent.locks.Lock;import java.util.concurrent.locks.ReentrantLock;
class Ticket implements Runnable {
    private int ticketNum = 10;
    // Lock锁（推荐，灵活）
    private final Lock lock = new ReentrantLock();
    @Override public void run() {
        while (ticketNum>0) {
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName()+"卖出第"+ticketNum+"张");
                ticketNum--;Thread.sleep(100);
            } catch (InterruptedException e) {e.printStackTrace();}
            finally { lock.unlock(); }
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

