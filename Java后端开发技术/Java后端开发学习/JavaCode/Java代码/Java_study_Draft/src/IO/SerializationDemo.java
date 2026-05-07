package IO;

import java.io.*;

class User implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    transient int password; // transient 修饰的字段不会被序列化

    User(String name, int password) {
        this.name = name;
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{name='" + name + "', password=" + password + "}";
    }
}

public class SerializationDemo {
    /**
 * 主函数，演示Java对象的序列化与反序列化过程。
 *
 * 该函数首先创建一个User对象，然后将其序列化到文件"user.dat"中，
 * 接着从该文件中反序列化出User对象并打印其内容。
 *
 * @param args 命令行参数（未使用）
 * @throws IOException 当文件操作发生错误时抛出
 * @throws ClassNotFoundException 当反序列化过程中找不到类时抛出
 */
public static void main(String[] args) throws IOException, ClassNotFoundException {
    User user = new User("Alice", 123456);

    // 将User对象序列化到文件"user.dat"
    try (ObjectOutputStream oos = new ObjectOutputStream(
            new FileOutputStream("user.dat"))) {
        oos.writeObject(user);
    }

    // 从文件"user.dat"中反序列化User对象并打印
    try (ObjectInputStream ois = new ObjectInputStream(
            new FileInputStream("user.dat"))) {
        User loaded = (User) ois.readObject();
        System.out.println(loaded); // password 为 0（transient）
    }
}

}
