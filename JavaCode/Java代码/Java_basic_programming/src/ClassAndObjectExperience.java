/**
 * Java 类与对象核心操作综合体验（修复编译错误版）
 * 覆盖：类定义、对象创建、封装、构造方法、方法重载、继承、多态、this/super关键字
 */
public class ClassAndObjectExperience {
    public static void main(String[] args) {
        // ====================== 模块1：类的定义与对象创建 ======================
        System.out.println("===== 模块1：类的定义与对象创建 =====");
        // 1. 创建对象（实例化）：类名 对象名 = new 类名();
        Student student1 = new Student();
        // 2. 访问对象的属性和方法
        student1.name = "张三";
        student1.age = 18;
        student1.gender = "男";
        // 调用对象方法
        student1.showInfo();
        student1.study("Java"); // 修复：移除多余的course:
        System.out.println();

        // ====================== 模块2：封装（private+get/set方法） ======================
        System.out.println("===== 模块2：封装（private+get/set） =====");
        Teacher teacher1 = new Teacher();
        // 直接访问private属性会报错，必须通过get/set方法
        teacher1.setName("李老师");
        teacher1.setAge(35); // 触发年龄校验
        teacher1.setSubject("数学");
        // 获取属性值
        System.out.println("教师姓名：" + teacher1.getName());
        System.out.println("教师年龄：" + teacher1.getAge());
        teacher1.teach();
        System.out.println();

        // ====================== 模块3：构造方法（初始化对象） ======================
        System.out.println("===== 模块3：构造方法 =====");
        // 无参构造（默认）
        Student student2 = new Student();
        // 有参构造（自定义）
        Student student3 = new Student("李四", 19, "女");
        student3.showInfo();
        // 重载构造方法
        Teacher teacher2 = new Teacher("王老师", 40, "语文");
        System.out.println("重载构造创建的教师：" + teacher2.getName() + "，教" + teacher2.getSubject());
        System.out.println();

        // ====================== 模块4：方法重载（Overload） ======================
        System.out.println("===== 模块4：方法重载 =====");
        Calculator calc = new Calculator();
        // 调用不同参数的add方法（方法名相同，参数列表不同）
        System.out.println("整数相加：" + calc.add(10, 20));
        System.out.println("浮点数相加：" + calc.add(3.5, 2.5));
        System.out.println("三个整数相加：" + calc.add(1, 2, 3));
        System.out.println();

        // ====================== 模块5：继承与super关键字 ======================
        System.out.println("===== 模块5：继承与super =====");
        // 创建子类对象
        GraduateStudent gradStudent = new GraduateStudent("赵五", 22, "男", "计算机科学");
        // 调用父类继承的方法
        gradStudent.showInfo();
        // 调用子类特有方法
        gradStudent.doResearch("人工智能");
        // 调用重写后的方法
        gradStudent.study("深度学习");
        System.out.println();

        // ====================== 模块6：多态（父类引用指向子类对象） ======================
        System.out.println("===== 模块6：多态 =====");
        // 父类引用指向子类对象
        Person p1 = new Student("孙六", 20, "男");
        Person p2 = new Teacher("周老师", 38, "英语");
        // 编译看父类，运行看子类（动态绑定）
        p1.showInfo(); // 执行Student的showInfo
        p2.showInfo(); // 执行Teacher的showInfo
        // 多态的前提：继承 + 方法重写
    }
}

// -------------------------- 基础父类：Person（补充get/set方法） --------------------------
/**
 * 父类：人（抽取公共属性和方法）
 */
class Person {
    String name; // 姓名
    int age;     // 年龄

    // 无参构造
    public Person() {}

    // 有参构造
    public Person(String name, int age) {
        this.name = name; // this：当前对象的引用
        this.age = age;
    }

    // ===== 新增：name和age的get/set方法 =====
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    // 通用方法：展示信息（子类可重写）
    public void showInfo() {
        System.out.println("姓名：" + name + "，年龄：" + age);
    }
}

// -------------------------- 子类1：Student（继承Person） --------------------------
/**
 * 子类：学生（继承Person）
 */
class Student extends Person {
    String gender; // 特有属性：性别

    // 无参构造
    public Student() {}

    // 有参构造1（继承+特有属性）
    public Student(String name, int age, String gender) {
        super(name, age); // super：调用父类构造方法
        this.gender = gender;
    }

    // 特有方法：学习
    public void study(String course) {
        System.out.println(name + "正在学习" + course + "课程");
    }

    // 重写父类方法（Override）
    @Override
    public void showInfo() {
        // 调用父类的showInfo
        super.showInfo();
        System.out.println("性别：" + gender + "，身份：学生");
    }
}

// -------------------------- 子类2：GraduateStudent（继承Student） --------------------------
/**
 * 子类的子类：研究生（继承Student）
 */
class GraduateStudent extends Student {
    String major; // 特有属性：专业

    // 构造方法
    public GraduateStudent(String name, int age, String gender, String major) {
        super(name, age, gender); // 调用父类（Student）构造
        this.major = major;
    }

    // 特有方法：做研究
    public void doResearch(String topic) {
        System.out.println(name + "（研究生，专业：" + major + "）正在研究" + topic);
    }

    // 重写study方法
    @Override
    public void study(String course) {
        System.out.println(name + "（研究生）深入学习" + course + "，结合" + major + "研究");
    }
}

// -------------------------- 子类3：Teacher（继承Person，修复@Override） --------------------------
/**
 * 子类：教师（继承Person，演示封装）
 */
class Teacher extends Person {
    // 私有属性（封装：只能本类访问）
    private String subject; // 授课科目

    // 无参构造
    public Teacher() {}

    // 重载构造方法（参数列表不同）
    public Teacher(String name, int age, String subject) {
        super(name, age);
        this.subject = subject;
    }

    // 特有方法：授课
    public void teach() {
        System.out.println(name + "正在教授" + subject + "课程");
    }

    // 重写父类方法
    @Override
    public void showInfo() {
        System.out.println("姓名：" + name + "，年龄：" + age + "，身份：教师，授课科目：" + subject);
    }

    // get/set方法（封装的核心：控制属性访问）
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    // 重写父类的setAge方法（现在父类有该方法，@Override合法）
    @Override
    public void setAge(int age) {
        if (age < 22 || age > 65) {
            System.out.println("年龄不合法！教师年龄应在22-65之间");
            this.age = 30; // 默认值
        } else {
            this.age = age;
        }
    }
}

// -------------------------- 工具类：Calculator（演示方法重载） --------------------------
/**
 * 计算器类（演示方法重载）
 */
class Calculator {
    // 方法重载：方法名相同，参数类型/个数不同
    // 1. 两个int相加
    public int add(int a, int b) {
        return a + b;
    }

    // 2. 两个double相加
    public double add(double a, double b) {
        return a + b;
    }

    // 3. 三个int相加
    public int add(int a, int b, int c) {
        return a + b + c;
    }
}