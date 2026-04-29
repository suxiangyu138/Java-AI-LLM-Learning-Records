package IO;

import java.io.*;

class CloneableUser implements Serializable {
    String name;
    int age;

    CloneableUser(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // 序列化克隆
    public CloneableUser deepClone() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(this);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (CloneableUser) ois.readObject();
        }
    }
}

public class SerialCloneDemo {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        CloneableUser user = new CloneableUser("Alice", 20);
        CloneableUser clone = user.deepClone();
        System.out.println(user != clone); // true（不同对象）
        System.out.println(user.name.equals(clone.name) && user.age == clone.age); // true（内容相同）
    }
}
