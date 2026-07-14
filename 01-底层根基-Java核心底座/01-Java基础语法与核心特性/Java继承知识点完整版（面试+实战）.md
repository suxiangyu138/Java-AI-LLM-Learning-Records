Java继承知识点完整版（面试+实战）
继承是Java面向对象三大核心特性（封装、继承、多态）之一，核心作用是代码复用、简化开发、实现多态。它允许一个类（子类）继承另一个类（父类）的属性和方法，同时子类可以根据自身需求扩展新的属性和方法，或重写父类的方法，实现代码的复用与扩展，是构建复杂Java程序的基础。以下从核心定义、语法规则、继承特性、核心注意事项、实战案例及面试高频考点，全面梳理Java继承知识点，兼顾基础与实战，帮你快速掌握并规避常见误区。
一、Java继承的核心定义
继承（Inheritance）是指一个类（称为子类/派生类，Subclass）直接获取另一个类（称为父类/基类，Superclass）的非私有属性（成员变量）和方法（成员方法）的机制。
简单理解：子类站在父类的基础上开发，无需重复编写父类已有的代码，只需专注于自身特有的功能扩展，就像“子类继承了父类的本领，同时还能学会新本领”。
核心价值：
代码复用：减少重复代码，降低开发成本，提高开发效率；
代码扩展：子类可在父类基础上新增属性和方法，实现功能升级；
奠定多态基础：继承是多态的前提（子类对象可以赋值给父类引用），让程序更具灵活性和可扩展性。
二、Java继承的语法规则（重点掌握）
Java中通过 extends 关键字实现继承，语法格式简洁明了，核心规则需严格遵循，否则会出现编译错误。
1. 基本语法
    // 父类（基类）：定义公共的属性和方法
    class 父类名 {
    // 父类的属性（成员变量）
    访问修饰符 数据类型 属性名;
    // 父类的方法（成员方法）
    访问修饰符 返回值类型 方法名(参数列表) {
        // 方法体
    }
    }
    // 子类（派生类）：继承父类，扩展自身功能
    class 子类名 extends 父类名 {
    // 可选：新增子类特有的属性
    访问修饰符 数据类型 子类特有属性;
    // 可选：新增子类特有的方法
    访问修饰符 返回值类型 子类特有方法(参数列表) {
        // 方法体
    }
    // 可选：重写父类的方法（方法名、参数列表、返回值一致）
    @Override
    访问修饰符 返回值类型 父类方法名(参数列表) {
        // 子类重写后的逻辑
    }
    }
2. 实战案例（直观理解）
    // 父类：Person（人），定义所有人共有的属性和方法
    class Person {
    // 公共属性
    String name;
    int age;
    // 公共方法
    public void eat() {
        System.out.println(name + "在吃饭");
    }
    public void sleep() {
        System.out.println(name + "在睡觉");
    }
    }
    // 子类：Student（学生），继承Person，新增自身特有功能
    class Student extends Person {
    // 子类特有属性：学号
    String studentId;
    // 子类特有方法：学习
    public void study() {
        System.out.println(name + "（学号：" + studentId + "）在学习Java");
    }
    // 重写父类的eat方法（子类有自己的实现逻辑）
    @Override
    public void eat() {
        System.out.println(name + "在学校食堂吃饭，注重营养均衡");
    }
    }
    // 测试类
    public class TestInheritance {
    public static void main(String[] args) {
        // 创建子类对象
        Student student = new Student();
        // 调用父类继承的属性
        student.name = "张三";
        student.age = 18;
        // 调用子类特有属性
        student.studentId = "2024001";
        // 调用重写后的父类方法
        student.eat(); // 输出：张三在学校食堂吃饭，注重营养均衡
        // 调用继承的父类方法
        student.sleep(); // 输出：张三在睡觉
        // 调用子类特有方法
        student.study(); // 输出：张三（学号：2024001）在学习Java
    }
    }
    案例说明：Student类通过extends关键字继承了Person类的name、age属性和eat()、sleep()方法，同时新增了studentId属性和study()方法，还重写了eat()方法，实现了“复用父类代码+扩展自身功能”的核心目的。
    三、Java继承的核心特性（必记）
    Java继承有明确的特性限制，这些特性是面试高频考点，也是开发中必须遵循的规则，核心分为4点：
    1. 单继承特性（Java独有，重点）
    Java中一个子类只能继承一个父类，不能同时继承多个父类（即不支持多继承），这是为了避免多继承带来的“菱形继承”问题（多个父类有同名方法/属性时，子类无法确定调用哪个）。
    错误示例（编译报错）：
    // 错误：Java不支持多继承，子类不能同时继承两个父类
    class Student extends Person, Animal { 
    // 编译报错：'extends' cannot be followed by multiple classes
    }
    补充：Java通过“接口多实现”（一个类可以实现多个接口）来弥补单继承的局限性，既避免了菱形继承问题，又实现了多维度的功能扩展。
2. 传递性
    继承具有传递性，即“子类继承父类，父类继承祖父类，则子类间接继承祖父类的非私有属性和方法”。
    示例：
    // 祖父类
    class Grandfather {
    public void run() {
        System.out.println("祖父会跑步");
    }
    }
    // 父类：继承祖父类
    class Father extends Grandfather {
    public void work() {
        System.out.println("父亲会工作");
    }
    }
    // 子类：继承父类，间接继承祖父类
    class Son extends Father {
    public void play() {
        System.out.println("儿子会玩耍");
    }
    }
    // 测试：子类可以调用祖父类、父类、自身的方法
    public class TestTransmit {
    public static void main(String[] args) {
        Son son = new Son();
        son.run(); // 继承祖父类的方法
        son.work(); // 继承父类的方法
        son.play(); // 自身的方法
    }
    }
3. 子类不能继承父类的私有成员
    父类中被 private 修饰的属性和方法（私有成员），子类无法直接继承、访问，因为private访问修饰符的作用是“仅当前类可见”。
    注意：子类虽然不能直接访问父类的私有成员，但可以通过父类提供的 public 或 protected 修饰的“getter/setter方法”间接访问。
    示例：
    class Person {
    // 私有属性：子类无法直接访问
    private String idCard;
    // 公共的getter方法：子类可通过该方法间接访问私有属性
    public String getIdCard() {
        return idCard;
    }
    // 公共的setter方法：子类可通过该方法间接修改私有属性
    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }
    }
    class Student extends Person {
    public void showIdCard() {
        // 错误：无法直接访问父类的私有属性
        // System.out.println(idCard);
        // 正确：通过父类的getter方法间接访问
        System.out.println("身份证号：" + getIdCard());
    }
    }
    public class TestPrivate {
    public static void main(String[] args) {
        Student student = new Student();
        student.setIdCard("110101199901011234");
        student.showIdCard(); // 输出：身份证号：110101199901011234
    }
    }
4. 子类可以重写父类的方法（方法重写）
    方法重写（Override）是继承的核心扩展特性：子类可以定义与父类“方法名、参数列表、返回值类型（或其子类）”完全一致的方法，覆盖父类的方法实现，实现子类特有的逻辑。
    方法重写的核心规则（必记）：
    方法名、参数列表必须与父类完全一致（参数个数、类型、顺序都要相同）；
    返回值类型：子类重写方法的返回值类型，必须是父类方法返回值类型的“子类或本身”（不能是无关类型）；
    访问修饰符：子类重写方法的访问权限，不能低于父类方法的访问权限（如父类是public，子类不能是protected/private）；
    不能重写的方法：父类中被 final 修饰的方法（最终方法）、static修饰的静态方法（静态方法属于类，不属于对象，无法重写）；
    注解：重写方法时，建议添加 @Override 注解，用于校验重写规则（若不符合重写规则，编译报错），同时提高代码可读性。
    四、Java继承的核心注意事项（实战避坑）
    开发中使用继承时，需注意以下细节，避免出现编译错误或逻辑异常，这也是面试中常考的“避坑点”：
    1. 构造方法的继承规则（重点、易错）
    子类不能继承父类的构造方法（构造方法是专门用于创建对象的，与类名一致，子类类名与父类不同，无法继承）；
    子类构造方法执行时，会默认先调用父类的无参构造方法（隐式调用，无需手动编写），若父类没有无参构造方法（只有有参构造），则子类必须在自身构造方法中，通过 super() 手动调用父类的有参构造方法，否则编译报错；
    super() 必须放在子类构造方法的第一行（否则编译报错），用于先初始化父类的属性和方法，再初始化子类自身。
    示例（正确写法）：
    class Person {
    // 父类有参构造（无无参构造）
    public Person(String name) {
        this.name = name;
    }
    String name;
    }
    class Student extends Person {
    // 子类构造方法：必须手动调用父类的有参构造
    public Student(String name, String studentId) {
        super(name); // 调用父类有参构造，必须放在第一行
        this.studentId = studentId;
    }
    String studentId;
    }
2. super关键字的使用
    super 关键字用于“访问父类的成员”，核心用法有3种：
    super.属性名：访问父类的非私有属性（当子类与父类有同名属性时，用于区分父类属性）；
    super.方法名(参数)：调用父类的非私有方法（当子类重写了父类方法，仍想调用父类原方法时使用）；
    super()：调用父类的构造方法（无参/有参），仅能在子类构造方法中使用，且必须放在第一行。
3. final关键字与继承的关系
    final修饰的类：不能被继承（如Java中的String类、Math类，都是final类，无法创建其子类）；
    final修饰的方法：不能被重写（父类中final方法，子类无法覆盖，只能直接继承使用）；
    final修饰的属性：是常量，不能被修改，但不影响继承（子类可以继承父类的final常量）。
4. 继承的使用场景（避免滥用）
    继承的核心是“is-a”关系（子类是父类的一种），只有满足这种关系，才适合使用继承，否则会导致代码耦合度高、难以维护。
    正确场景：Student is a Person（学生是人）、Dog is a Animal（狗是动物），适合用继承；
    错误场景：Student 和 Teacher（都是人，但二者是平级关系，不适合互相继承，应共同继承Person类）。
    五、面试高频考点（必背）
    1. Java为什么不支持多继承？
    核心原因：避免“菱形继承”（歧义）问题。假设子类同时继承两个父类，两个父类有同名的方法/属性，子类调用该方法/属性时，无法确定调用哪个父类的实现，会导致编译歧义。Java通过“单继承+接口多实现”的方式，既解决了多继承的需求，又避免了歧义问题。
2. 方法重写（Override）和方法重载（Overload）的区别？（高频）
    对比维度
    方法重写（Override）
    方法重载（Overload）
    定义
    子类重写父类的方法，方法名、参数列表、返回值一致
    同一类中，方法名相同，参数列表（个数、类型、顺序）不同
    所属范围
    子类与父类之间
    同一类中
    返回值要求
    子类返回值需是父类返回值的子类或本身
    无要求（可相同、可不同）
    访问修饰符
    子类权限不能低于父类
    无要求
    核心作用
    扩展父类方法，实现子类特有逻辑
    同一方法名，处理不同参数的逻辑，简化调用
3. super和this关键字的区别？
    this：指代当前对象，用于访问当前类的属性、方法、构造方法；
    super：指代父类对象，用于访问父类的属性、方法、构造方法；
    使用场景：当子类与父类有同名属性/方法时，用this区分当前类，用super区分父类；this()调用当前类的构造方法，super()调用父类的构造方法。
4. 父类的私有方法，子类能继承吗？
    不能。父类的私有方法（private修饰）仅能在父类内部访问，子类无法直接继承、访问，也无法重写；但子类可以通过父类提供的公共getter/setter方法，间接访问父类的私有属性。
    六、总结
    1. 继承是Java面向对象的核心特性，核心价值是代码复用、扩展和奠定多态基础，通过extends关键字实现；
    2. 核心规则：单继承（一个子类只能继承一个父类）、传递性、子类不能继承父类私有成员、子类可重写父类方法；
    3. 重点细节：构造方法不能继承，子类构造方法默认调用父类无参构造，无则需手动用super()调用；super关键字用于访问父类成员；
    4. 面试重点：不支持多继承的原因、方法重写与重载的区别、super与this的区别，需结合实战案例记忆；
    5. 实战原则：继承仅用于“is-a”关系，避免滥用，降低代码耦合度，同时注意规避构造方法、方法重写的常见误区。
