package Java_learning_notes.通用数据管理工具;

/**
 * 自定义实体类：用于测试泛型支持自定义类型
 */
public class User implements Comparable<User> {
    private String id;
    private String name;
    private int age;

    public User(String id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name=" + name + ", age=" + age + "}";
    }

    // 按年龄排序
    @Override
    public int compareTo(User o) {
        return Integer.compare(this.age, o.age);
    }

    // getter
    public int getAge() { return age; }
}