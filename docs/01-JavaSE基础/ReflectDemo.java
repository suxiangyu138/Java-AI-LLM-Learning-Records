package Java_learning_notes;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

class User {
    private String name;
    public int age;

    public User() {}
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    private void privateMethod() {
        System.out.println("私有方法执行");
    }

    public void showInfo(String msg) {
        System.out.println("公有方法：" + msg);
    }
}

public class ReflectDemo {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = User.class;

        // 修复：替代已废弃的 newInstance()
        User user = (User) clazz.getDeclaredConstructor().newInstance();

        // 有参构造创建对象
        Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, int.class);
        User user2 = (User) constructor.newInstance("张三", 20);

        // 修复：不能直接访问私有字段，通过反射读取
        Field nameField = clazz.getDeclaredField("name");
        nameField.setAccessible(true);
        String userName = (String) nameField.get(user2);

        System.out.println("构造对象：姓名=" + userName + " 年龄=" + user2.age);

        // 获取公有属性并赋值
        Field ageField = clazz.getField("age");
        ageField.set(user2, 25);
        System.out.println("修改后公有age：" + ageField.get(user2));

        // 修改私有属性
        nameField.set(user2, "李四");
        System.out.println("修改后私有name：" + nameField.get(user2));

        // 调用公有方法
        Method showMethod = clazz.getDeclaredMethod("showInfo", String.class);
        showMethod.invoke(user2, "反射调用成功！");

        // 调用私有方法
        Method privateMethod = clazz.getDeclaredMethod("privateMethod");
        privateMethod.setAccessible(true);
        privateMethod.invoke(user2);
    }
}