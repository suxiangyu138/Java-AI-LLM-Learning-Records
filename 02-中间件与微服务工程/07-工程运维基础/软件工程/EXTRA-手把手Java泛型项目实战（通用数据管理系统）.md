# 手把手 Java 泛型项目实战（通用数据管理系统）

> 实战目标：写一个「通用数据管理系统」，核心用泛型实现"一套代码，管理多种数据"（学生、老师、图书），避免重复开发。

---

## 目录

- [1. 环境准备与项目目标](#1-环境准备与项目目标)
- [2. 创建项目](#2-创建项目)
- [3. 定义实体类](#3-定义实体类)
- [4. 核心泛型类](#4-核心泛型类)
- [5. 测试类](#5-测试类)
- [6. 运行测试与预期结果](#6-运行测试与预期结果)
- [7. 泛型核心知识点](#7-泛型核心知识点)
- [8. 扩展练习](#8-扩展练习)
- [9. 常见问题解决](#9-常见问题解决)

---

## 1. 环境准备与项目目标

### 1.1 环境准备

| 工具 | 说明 |
|------|------|
| JDK | 8 及以上（泛型核心特性在 JDK 5 引入） |
| 开发工具 | IntelliJ IDEA（社区版即可） |
| 基础储备 | Java 基础语法（类、方法、接口），无需提前懂泛型 |

### 1.2 项目目标

**核心需求**：添加数据、删除数据、查询数据、修改数据，支持任意类型的实体类，全程用泛型贯穿。

---

## 2. 创建项目

### 2.1 新建 Java 项目

1. 打开 IDEA，点击 **New Project**，选择 **Java**，点击 **Next**
2. 取消 **Create project from template**（从零写），点击 **Next**
3. 项目名称：`GenericDataManager`
4. 项目路径：选择你找得到的文件夹
5. 右键 `src` → **New** → **Package**，输入包名：`com.generic.demo`

---

## 3. 定义实体类

### 3.1 学生实体类（Student.java）

```java
package com.generic.demo;

public class Student {
    private Integer id;
    private String name;
    private Integer age;

    public Student() {}
    public Student(Integer id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    // getter & setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    @Override
    public String toString() {
        return "Student{id=" + id + ", name='" + name + '\'' + ", age=" + age + '}';
    }
}
```

### 3.2 老师实体类（Teacher.java）

```java
package com.generic.demo;

public class Teacher {
    private Integer id;
    private String name;
    private String subject;

    public Teacher() {}
    public Teacher(Integer id, String name, String subject) {
        this.id = id;
        this.name = name;
        this.subject = subject;
    }

    // getter & setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    @Override
    public String toString() {
        return "Teacher{id=" + id + ", name='" + name + '\'' + ", subject='" + subject + '\'' + '}';
    }
}
```

### 3.3 图书实体类（Book.java）

```java
package com.generic.demo;

public class Book {
    private Integer id;
    private String bookName;
    private String author;

    public Book() {}
    public Book(Integer id, String bookName, String author) {
        this.id = id;
        this.bookName = bookName;
        this.author = author;
    }

    // getter & setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    @Override
    public String toString() {
        return "Book{id=" + id + ", bookName='" + bookName + '\'' + ", author='" + author + '\'' + '}';
    }
}
```

---

## 4. 核心泛型类

这是整个项目的核心——用泛型 `<T>` 表示"任意类型"，实现对任意实体类的增删改查。

```java
package com.generic.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * 泛型类：<T>是类型参数，表示"任意类型"
 * 泛型约束：T 只能是引用类型（不能是基本类型如int、double）
 */
public class GenericManager<T> {
    private List<T> dataList = new ArrayList<>();

    /**
     * 1. 添加数据
     * @param data 要添加的数据对象
     */
    public void addData(T data) {
        if (data != null) {
            dataList.add(data);
            System.out.println("添加成功：" + data);
        } else {
            System.out.println("添加失败：数据不能为空！");
        }
    }

    /**
     * 2. 删除数据（根据索引）
     * @param index 要删除的数据索引（从0开始）
     * @return 删除的对象，删除失败返回null
     */
    public T deleteData(int index) {
        if (index >= 0 && index < dataList.size()) {
            T deletedData = dataList.remove(index);
            System.out.println("删除成功：" + deletedData);
            return deletedData;
        } else {
            System.out.println("删除失败：索引不合法！");
            return null;
        }
    }

    /**
     * 3. 查询数据（根据索引）
     * @param index 要查询的索引
     * @return 查询到的对象，查询失败返回null
     */
    public T queryData(int index) {
        if (index >= 0 && index < dataList.size()) {
            T data = dataList.get(index);
            System.out.println("查询到数据：" + data);
            return data;
        } else {
            System.out.println("查询失败：索引不合法！");
            return null;
        }
    }

    /**
     * 4. 修改数据（根据索引）
     * @param index 要修改的数据索引
     * @param newData 新的数据
     * @return 修改成功返回true，失败返回false
     */
    public boolean updateData(int index, T newData) {
        if (index >= 0 && index < dataList.size() && newData != null) {
            dataList.set(index, newData);
            System.out.println("修改成功：新数据为" + newData);
            return true;
        } else {
            System.out.println("修改失败：索引不合法或新数据为空！");
            return false;
        }
    }

    /**
     * 5. 查询所有数据
     * @return 所有数据的集合
     */
    public List<T> queryAllData() {
        System.out.println("所有数据：" + dataList);
        return dataList;
    }
}
```

---

## 5. 测试类

```java
package com.generic.demo;

public class GenericTest {
    public static void main(String[] args) {
        // ========== 测试1：用泛型管理学生数据 ==========
        System.out.println("========== 测试学生数据管理 ==========");
        GenericManager<Student> studentManager = new GenericManager<>();
        studentManager.addData(new Student(1, "张三", 18));
        studentManager.addData(new Student(2, "李四", 19));
        studentManager.addData(new Student(3, "王五", 17));
        studentManager.queryAllData();
        studentManager.queryData(1);
        studentManager.updateData(2, new Student(3, "赵六", 18));
        studentManager.deleteData(0);
        studentManager.queryAllData();

        // ========== 测试2：用泛型管理老师数据 ==========
        System.out.println("\n========== 测试老师数据管理 ==========");
        GenericManager<Teacher> teacherManager = new GenericManager<>();
        teacherManager.addData(new Teacher(1, "李老师", "数学"));
        teacherManager.addData(new Teacher(2, "王老师", "语文"));
        teacherManager.queryAllData();
        teacherManager.updateData(1, new Teacher(2, "张老师", "英语"));
        teacherManager.queryAllData();

        // ========== 测试3：用泛型管理图书数据 ==========
        System.out.println("\n========== 测试图书数据管理 ==========");
        GenericManager<Book> bookManager = new GenericManager<>();
        bookManager.addData(new Book(1, "《Java编程思想》", "Bruce Eckel"));
        bookManager.addData(new Book(2, "《SpringBoot实战》", "Craig Walls"));
        bookManager.deleteData(1);
        bookManager.queryAllData();
    }
}
```

---

## 6. 运行测试与预期结果

### 6.1 运行操作

右键点击 `GenericTest.java`，选择 **Run 'GenericTest.main()'**。

### 6.2 预期运行结果

```
========== 测试学生数据管理 ==========
添加成功：Student{id=1, name='张三', age=18}
添加成功：Student{id=2, name='李四', age=19}
添加成功：Student{id=3, name='王五', age=17}
所有数据：[Student{id=1, name='张三', age=18}, Student{id=2, name='李四', age=19}, Student{id=3, name='王五', age=17}]
查询到数据：Student{id=2, name='李四', age=19}
修改成功：新数据为Student{id=3, name='赵六', age=18}
删除成功：Student{id=1, name='张三', age=18}
所有数据：[Student{id=2, name='李四', age=19}, Student{id=3, name='赵六', age=18}]

========== 测试老师数据管理 ==========
添加成功：Teacher{id=1, name='李老师', subject='数学'}
添加成功：Teacher{id=2, name='王老师', subject='语文'}
所有数据：[Teacher{id=1, name='李老师', subject='数学'}, Teacher{id=2, name='王老师', subject='语文'}]
修改成功：新数据为Teacher{id=2, name='张老师', subject='英语'}
所有数据：[Teacher{id=1, name='李老师', subject='数学'}, Teacher{id=2, name='张老师', subject='英语'}]

========== 测试图书数据管理 ==========
添加成功：Book{id=1, bookName='《Java编程思想》', author='Bruce Eckel'}
添加成功：Book{id=2, bookName='《SpringBoot实战》', author='Craig Walls'}
删除成功：Book{id=2, bookName='《SpringBoot实战》', author='Craig Walls'}
所有数据：[Book{id=1, bookName='《Java编程思想》', author='Bruce Eckel'}]
```

---

## 7. 泛型核心知识点

| 概念 | 说明 |
|------|------|
| 泛型类定义 | `class 类名<T>`，T 是"类型参数"，使用时指定具体类型 |
| 泛型的作用 | 避免重复代码 + 保证类型安全（编译器检查） |
| 泛型约束 | `<T extends 父类>` 限制 T 只能是某类的子类 |
| 泛型方法 | 方法参数或返回值是 T 类型 |
| 注意点 | 泛型不能用基本类型（int、double），只能用引用类型 |

---

## 8. 扩展练习

1. **新增实体类**：User（id、username、password），用现有泛型管理器管理
2. **根据 ID 查询**：给 `GenericManager` 添加根据 ID 查询的方法（需定义 `HasId` 接口）
3. **泛型接口**：定义 `GenericDao<T>` 接口，让 `GenericManager` 实现该接口

---

## 9. 常见问题解决

| 问题 | 解决方案 |
|------|----------|
| 报错"找不到类" | 检查包名是否正确（所有类都在 `com.generic.demo` 包下） |
| 运行无输出 | 检查是否右键点击了 `GenericTest.java` 运行，是否有语法错误 |
| 泛型报错"类型不匹配" | 例如给 `GenericManager<Student>` 添加 Teacher 对象，修改为对应类型即可 |
