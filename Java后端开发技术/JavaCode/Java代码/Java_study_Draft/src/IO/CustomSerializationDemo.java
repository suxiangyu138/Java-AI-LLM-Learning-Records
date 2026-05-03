package IO;

import java.io.*;

class CustomUser implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    int age;

    CustomUser(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // 自定义序列化
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject(); // 默认序列化
        out.writeInt(age * 2); // 额外写入
    }

    // 自定义反序列化
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject(); // 默认反序列化
        int extra = in.readInt(); // 读取额外数据
        this.age = extra / 2; // 还原
    }

    @Override
    public String toString() {
        return "CustomUser{name='" + name + "', age=" + age + "}";
    }
}

public class CustomSerializationDemo {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        CustomUser user = new CustomUser("Bob", 25);

        // 序列化对象到文件
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("custom_user.dat"))) {
            oos.writeObject(user);
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("custom_user.dat"))) {
            CustomUser loaded = (CustomUser) ois.readObject();
            System.out.println(loaded); // age=25
        }
    }
}
