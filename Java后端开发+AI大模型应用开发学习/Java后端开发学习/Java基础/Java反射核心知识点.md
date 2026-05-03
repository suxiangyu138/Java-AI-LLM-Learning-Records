03.16 09:04
Java反射核心知识点
---------------------------------------------------------------------------------------------------------------------------------------
一、反射的基本概念
1. 定义
反射是Java提供的一种机制，允许程序在运行时获取类的完整信息（类名、属性、方法、构造器等），并能动态操作类的成员（调用方法、访问属性、创建对象），无需在编译期确定具体类。
2. 核心作用
- 运行时获取类的元数据
- 动态创建对象、调用方法、修改属性
- 突破封装性（可访问私有成员）
- 实现框架的核心功能（如Spring、MyBatis）
3. 核心类（java.lang.reflect包）
- Class：表示类的字节码对象，是反射的入口
- Field：表示类的成员变量
- Method：表示类的方法
- Constructor：表示类的构造器
---------------------------------------------------------------------------------------------------------------------------------------
二、获取Class对象的三种方式
```java
public class ReflectDemo {
    public static void main(String[] args) throws ClassNotFoundException {
        // 方式1：对象.getClass()
        Student student = new Student();
        Class<?> clazz1 = student.getClass();
        // 方式2：类名.class（编译期确定）
        Class<?> clazz2 = Student.class;
        // 方式3：Class.forName("全类名")（运行时动态加载，最常用）
        Class<?> clazz3 = Class.forName("com.example.Student");
        // 三种方式获取的是同一个Class对象（类的字节码对象唯一）
        System.out.println(clazz1 == clazz2); // true
        System.out.println(clazz1 == clazz3); // true
    }
}
// 测试用实体类
class Student {
    private String name;
    private int age;
    public String gender;
    public Student() {}
    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    private void study(String course) {
        System.out.println(name + "正在学习" + course);
    }
    @Override
    public String toString() {
        return "Student{name='" + name + "', age=" + age + "}";
    }
}
```
---------------------------------------------------------------------------------------------------------------------------------------
三、反射操作类的成员
1. 动态创建对象
```java
import java.lang.reflect.Constructor;
public class ReflectNewInstance {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("com.example.Student");
        // 方式1：调用无参构造器（要求无参构造器必须存在）
        Student s1 = (Student) clazz.newInstance();
        s1.setName("张三");
        System.out.println(s1);
        // 方式2：调用有参构造器（更灵活，可指定构造器参数）
        Constructor<?> constructor = clazz.getConstructor(String.class, int.class);
        Student s2 = (Student) constructor.newInstance("李四", 20);
        System.out.println(s2);
    }
}
```
2. 反射操作属性
```java
import java.lang.reflect.Field;
public class ReflectField {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("com.example.Student");
        Student student = (Student) clazz.newInstance();
        // 1. 获取公共属性并赋值
        Field genderField = clazz.getField("gender");
        genderField.set(student, "男");
        System.out.println(genderField.get(student)); // 男
        // 2. 获取私有属性（需打破封装）
        Field nameField = clazz.getDeclaredField("name");
        nameField.setAccessible(true); // 取消访问检查，允许操作私有属性
        nameField.set(student, "王五");
        System.out.println(nameField.get(student)); // 王五
        // 3. 获取所有属性（包括私有）
        Field[] allFields = clazz.getDeclaredFields();
        for (Field field : allFields) {
            System.out.println("属性名：" + field.getName() + "，类型：" + field.getType());
        }
    }
}
```
3. 反射调用方法
```java
import java.lang.reflect.Method;
public class ReflectMethod {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("com.example.Student");
        Student student = (Student) clazz.newInstance();
        // 1. 调用公共方法
        Method setNameMethod = clazz.getMethod("setName", String.class);
        setNameMethod.invoke(student, "赵六"); // 执行方法
        Method getNameMethod = clazz.getMethod("getName");
        String name = (String) getNameMethod.invoke(student);
        System.out.println(name); // 赵六
        // 2. 调用私有方法
        Method studyMethod = clazz.getDeclaredMethod("study", String.class);
        studyMethod.setAccessible(true); // 取消访问检查
        studyMethod.invoke(student, "Java反射"); // 赵六正在学习Java反射
        // 3. 获取所有方法
        Method[] allMethods = clazz.getDeclaredMethods();
        for (Method method : allMethods) {
            System.out.println("方法名：" + method.getName() + "，返回值类型：" + method.getReturnType());
        }
    }
}
```
---------------------------------------------------------------------------------------------------------------------------------------
四、反射的常用API汇总
1. Class类核心方法
- getSimpleName()：获取类名（不含包名）
- getName()：获取全类名（含包名）
- getSuperclass()：获取父类Class对象
- getInterfaces()：获取实现的所有接口
- getFields()：获取所有公共属性（含父类）
- getDeclaredFields()：获取所有属性（仅本类，含私有）
- getMethods()：获取所有公共方法（含父类）
- getDeclaredMethods()：获取所有方法（仅本类，含私有）
- getConstructors()：获取所有公共构造器
- getDeclaredConstructors()：获取所有构造器（含私有）
2. Field类核心方法
- set(Object obj, Object value)：给对象的该属性赋值
- get(Object obj)：获取对象的该属性值
- setAccessible(boolean flag)：设置是否取消访问检查
- getName()：获取属性名
- getType()：获取属性类型
3. Method类核心方法
- invoke(Object obj, Object... args)：调用对象的该方法
- setAccessible(boolean flag)：设置是否取消访问检查
- getName()：获取方法名
- getParameterTypes()：获取方法参数类型数组
- getReturnType()：获取方法返回值类型
4. Constructor类核心方法
- newInstance(Object... args)：创建对象
- setAccessible(boolean flag)：设置是否取消访问检查
- getParameterTypes()：获取构造器参数类型数组
---------------------------------------------------------------------------------------------------------------------------------------
五、反射的限制与注意事项
1. 性能问题
- 反射操作比直接调用慢（需运行时解析），高频场景慎用
- 优化：缓存Class/Method/Field对象，减少重复获取
2. 封装性破坏
- 可访问私有成员，违反面向对象封装原则，需谨慎使用
3. 编译期类型检查失效
- 反射调用的错误（如参数类型错误）只能在运行时发现
4. 安全限制
- 受安全管理器限制，某些环境下可能禁止反射访问
5. 泛型擦除影响
- 反射无法获取泛型的具体类型（如List<String>擦除为List）
---------------------------------------------------------------------------------------------------------------------------------------
六、反射的实际应用场景
1. 框架开发（核心场景）
- Spring：IOC容器通过反射创建Bean、注入属性
- MyBatis：通过反射封装结果集为实体对象
- JUnit：通过反射执行@Test注解的方法
2. 动态代理
```java
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
// 接口
interface UserService {
    void add();
}
// 实现类
class UserServiceImpl implements UserService {
    @Override
    public void add() {
        System.out.println("执行添加用户操作");
    }
}
// 动态代理处理器
class MyInvocationHandler implements InvocationHandler {
    private Object target; // 目标对象
    public MyInvocationHandler(Object target) {
        this.target = target;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("前置增强：记录日志");
        Object result = method.invoke(target, args); // 调用目标方法
        System.out.println("后置增强：事务提交");
        return result;
    }
}
// 测试动态代理
public class DynamicProxyDemo {
    public static void main(String[] args) {
        UserService target = new UserServiceImpl();
        // 通过反射创建代理对象
        UserService proxy = (UserService) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            new MyInvocationHandler(target)
        );
        proxy.add();
    }
}
```
3. 注解解析
```java
import java.lang.annotation.*;
import java.lang.reflect.Method;
// 自定义注解
@Retention(RetentionPolicy.RUNTIME) // 运行时保留，可通过反射获取
@Target(ElementType.METHOD)
@interface MyAnnotation {
    String value();
}
// 测试注解解析
class AnnotationDemo {
    @MyAnnotation("测试注解")
    public void test() {}
    public static void main(String[] args) throws Exception {
        Method method = AnnotationDemo.class.getMethod("test");
        // 通过反射获取注解
        MyAnnotation annotation = method.getAnnotation(MyAnnotation.class);
        System.out.println(annotation.value()); // 测试注解
    }
}
```
---------------------------------------------------------------------------------------------------------------------------------------
总结
1. 反射是Java动态性的核心，允许运行时操作类的成员
2. 核心步骤：获取Class对象 → 获取成员（Field/Method/Constructor） → 取消访问检查（私有成员） → 执行操作
3. 优势：灵活、可实现通用框架；劣势：性能差、破坏封装、编译期无检查
4. 核心应用：框架开发、动态代理、注解解析、配置解析等
5. 实际开发中需权衡灵活性与性能，避免滥用反射

