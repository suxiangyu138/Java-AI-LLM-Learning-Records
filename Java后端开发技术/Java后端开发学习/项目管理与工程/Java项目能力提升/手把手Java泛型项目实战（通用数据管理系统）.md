03.31 00:42
手把手Java泛型项目实战（通用数据管理系统）
一、项目前提（必看）
1.1 环境准备
确保你有以下环境，新手直接用IDEA即可，无需复杂配置：
JDK 8及以上（泛型核心特性在JDK 5引入，8及以上更稳定，兼容所有代码）
开发工具：IntelliJ IDEA（社区版即可，免费）
基础储备：了解Java基础语法（类、方法、接口），无需提前懂泛型，跟着步骤走
1.2 项目目标
我们写一个「通用数据管理系统」，核心用泛型实现“一套代码，管理多种数据”（比如学生、老师、图书数据），避免重复开发。
核心需求：添加数据、删除数据、查询数据、修改数据，支持任意类型的实体类，全程用泛型贯穿，手把手写每一行代码。
二、第一步：创建项目（IDEA操作，手把手）
2.1 新建Java项目
打开IDEA，点击「New Project」，选择「Java」，点击「Next」；
取消「Create project from template」（不使用模板，从零写），点击「Next」；
项目名称：GenericDataManager（自定义也可以，建议和我一致，方便对照）；
项目路径：选一个你能找到的文件夹（比如桌面/JavaProjects），点击「Finish」；
右键点击src文件夹，选择「New」→「Package」，输入包名：com.generic.demo（规范命名，避免类名冲突）。
三、第二步：定义实体类（泛型操作的“数据载体”）
我们先定义3个不同类型的实体类（学生、老师、图书），后续用泛型统一管理这些类的对象，不用为每个类写一套管理代码。
3.1 学生实体类（Student.java）
package com.generic.demo;
// 学生实体类：包含id、姓名、年龄
public class Student {
    private Integer id; // 学生ID
    private String name; // 学生姓名
    private Integer age; // 学生年龄
    // 无参构造（必须有，后续泛型创建对象会用到）
    public Student() {}
    // 有参构造（方便快速创建对象）
    public Student(Integer id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }
    // getter和setter（必须有，用于操作私有属性）
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getAge() {
        return age;
    }
    public void setAge(Integer age) {
        this.age = age;
    }
    // toString方法（方便打印对象内容，不用自己拼接）
    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
3.2 老师实体类（Teacher.java）
package com.generic.demo;
// 老师实体类：包含id、姓名、学科
public class Teacher {
    private Integer id; // 老师ID
    private String name; // 老师姓名
    private String subject; // 所教学科
    // 无参构造
    public Teacher() {}
    // 有参构造
    public Teacher(Integer id, String name, String subject) {
        this.id = id;
        this.name = name;
        this.subject = subject;
    }
    // getter和setter
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }
    // toString方法
    @Override
    public String toString() {
        return "Teacher{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", subject='" + subject + '\'' +
                '}';
    }
}
3.3 图书实体类（Book.java）
package com.generic.demo;
// 图书实体类：包含id、书名、作者
public class Book {
    private Integer id; // 图书ID
    private String bookName; // 书名
    private String author; // 作者
    // 无参构造
    public Book() {}
    // 有参构造
    public Book(Integer id, String bookName, String author) {
        this.id = id;
        this.bookName = bookName;
        this.author = author;
    }
    // getter和setter
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getBookName() {
        return bookName;
    }
    public void setBookName(String bookName) {
        this.bookName = bookName;
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    // toString方法
    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", bookName='" + bookName + '\'' +
                ", author='" + author + '\'' +
                '}';
    }
}
四、第三步：核心泛型类（通用数据管理器）
这是整个项目的核心，用泛型<T>表示“任意类型”，我们写一个通用的管理类，实现对任意实体类（Student、Teacher、Book）的增删改查，这就是泛型的核心价值——代码复用。
泛型类命名：GenericManager.java，放在com.generic.demo包下，代码逐行写，每一行都有注释。
package com.generic.demo;
import java.util.ArrayList;
import java.util.List;
// 泛型类：<T>是类型参数，表示“任意类型”，后续使用时指定具体类型（如Student、Teacher）
// 泛型约束：T extends Object（默认，可以省略），表示T只能是引用类型（不能是基本类型，如int、double）
public class GenericManager<T> {
    // 用List存储数据，List也用泛型，和当前类的泛型T保持一致，确保存储的是同一类型的数据
    private List<T> dataList = new ArrayList<>();
    /**
     * 1. 添加数据（泛型方法：参数是T类型，接收任意实体类对象）
     * @param data 要添加的数据（Student/Teacher/Book对象）
     */
    public void addData(T data) {
        if (data != null) { // 避免添加null值
            dataList.add(data);
            System.out.println("添加成功：" + data);
        } else {
            System.out.println("添加失败：数据不能为空！");
        }
    }
    /**
     * 2. 删除数据（根据索引删除，泛型方法：返回删除的T类型数据）
     * @param index 要删除的数据的索引（从0开始）
     * @return 删除的对象（Student/Teacher/Book），删除失败返回null
     */
    public T deleteData(int index) {
        // 判断索引是否合法（0 <= index < 集合长度）
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
     * 3. 查询数据（根据索引查询，返回T类型数据）
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
     * 4. 修改数据（根据索引修改，参数是新的T类型数据）
     * @param index 要修改的数据的索引
     * @param newData 新的数据（替换原来的数据）
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
     * 5. 查询所有数据（返回存储所有T类型数据的List）
     * @return 所有数据的集合
     */
    public List<T> queryAllData() {
        System.out.println("所有数据：" + dataList);
        return dataList;
    }
}
五、第四步：测试类（手把手运行，验证泛型功能）
写一个测试类，分别用泛型管理Student、Teacher、Book三种数据，验证增删改查功能是否正常，这一步能让你直观看到泛型的作用——一套代码管多种数据。
测试类命名：GenericTest.java，放在com.generic.demo包下，代码可直接复制运行，注释详细说明每一步。
package com.generic.demo;
public class GenericTest {
    public static void main(String[] args) {
        // -------------- 测试1：用泛型管理学生数据 --------------
        System.out.println("========== 测试学生数据管理 ==========");
        // 1. 创建泛型管理器对象，指定泛型类型为Student（此时管理器只能管理Student对象）
        GenericManager<Student> studentManager = new GenericManager<>();
        // 2. 添加学生数据（用有参构造创建Student对象）
        studentManager.addData(new Student(1, "张三", 18));
        studentManager.addData(new Student(2, "李四", 19));
        studentManager.addData(new Student(3, "王五", 17));
        // 3. 查询所有学生
        studentManager.queryAllData();
        // 4. 查询索引为1的学生（李四）
        studentManager.queryData(1);
        // 5. 修改索引为2的学生（王五→赵六）
        studentManager.updateData(2, new Student(3, "赵六", 18));
        // 6. 删除索引为0的学生（张三）
        studentManager.deleteData(0);
        // 7. 再次查询所有学生，查看修改/删除结果
        studentManager.queryAllData();
        // -------------- 测试2：用泛型管理老师数据 --------------
        System.out.println("\n========== 测试老师数据管理 ==========");
        // 创建泛型管理器，指定泛型类型为Teacher
        GenericManager<Teacher> teacherManager = new GenericManager<>();
        teacherManager.addData(new Teacher(1, "李老师", "数学"));
        teacherManager.addData(new Teacher(2, "王老师", "语文"));
        teacherManager.queryAllData();
        teacherManager.updateData(1, new Teacher(2, "张老师", "英语"));
        teacherManager.queryAllData();
        // -------------- 测试3：用泛型管理图书数据 --------------
        System.out.println("\n========== 测试图书数据管理 ==========");
        // 创建泛型管理器，指定泛型类型为Book
        GenericManager<Book> bookManager = new GenericManager<>();
        bookManager.addData(new Book(1, "《Java编程思想》", "Bruce Eckel"));
        bookManager.addData(new Book(2, "《SpringBoot实战》", "Craig Walls"));
        bookManager.deleteData(1); // 删除第二本图书
        bookManager.queryAllData();
    }
}
六、第五步：运行测试，查看结果（关键一步）
6.1 运行操作
右键点击GenericTest.java文件，选择「Run 'GenericTest.main()'」，IDEA会自动编译运行，控制台会输出操作结果。
6.2 预期运行结果（对照查看，确保代码正确）
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
七、泛型核心知识点（结合项目讲解，秒懂）
通过上面的项目，你已经实际用了泛型，现在总结核心知识点，结合代码理解，不用死记硬背：
泛型类定义：class 类名<T>，T是“类型参数”，相当于一个“占位符”，使用时指定具体类型（如GenericManager<Student>）。
泛型的作用：避免重复代码（不用为Student、Teacher、Book分别写管理类），同时保证类型安全（比如不能给Student管理器添加Teacher对象，编译时就会报错）。
泛型约束：如果想限制T只能是某个类的子类，可以写<T extends 父类>（比如<T extends Person>，只能管理Person的子类）。
泛型方法：项目中addData、deleteData等方法，参数或返回值是T类型，就是泛型方法，和泛型类的T保持一致。
注意点：泛型不能用基本类型（如int、double），只能用引用类型（如Integer、String、自定义实体类），因为泛型在编译时会“擦除类型”，只保留Object类型。
八、扩展练习（可选，巩固泛型）
如果想进一步巩固，可以做一个小扩展，加深对泛型的理解：
新增实体类：User（id、username、password），用现有泛型管理器管理User数据；
给GenericManager添加一个方法：根据ID查询数据（需要用到泛型的“边界”，提示：<T extends HasId>，定义HasId接口，让所有实体类实现该接口，重写getId方法）；
尝试泛型接口：定义GenericDao<T>接口，包含增删改查方法，让GenericManager实现该接口。
九、常见问题解决（新手必看）
报错“找不到类”：检查包名是否正确（所有类都在com.generic.demo包下），IDEA是否正确识别项目；
运行无输出：检查是否右键点击了GenericTest.java运行，是否有语法错误（IDEA会标红，鼠标悬浮可查看错误）；
泛型报错“类型不匹配”：比如给GenericManager<Student>添加Teacher对象，编译时会报错，这是泛型的类型安全机制，修改为对应类型即可。
至此，一个完整的Java泛型项目就写完了！你已经手把手实现了泛型的核心用法，后续可以基于这个项目扩展更多功能，加深对泛型的理解。

