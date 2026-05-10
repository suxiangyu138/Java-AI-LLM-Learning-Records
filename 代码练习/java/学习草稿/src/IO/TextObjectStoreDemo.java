package IO;

import java.io.*;
import java.nio.charset.StandardCharsets; // 添加这一行
import java.util.ArrayList;
import java.util.List;

class Person {
    String name;
    int age;

    Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public String toString() {
        return name + "," + age;
    }
}

public class TextObjectStoreDemo {
    public static void main(String[] args) throws IOException {
        List<Person> people = List.of(new Person("Alice", 20), new Person("Bob", 30));

        // 写入文本
        try (PrintWriter pw = new PrintWriter("people.txt", StandardCharsets.UTF_8)) {
            for (Person p : people) {
                pw.println(p);
            }
        }

        // 读取文本并还原对象
        List<Person> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("people.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                loaded.add(new Person(parts[0], Integer.parseInt(parts[1])));
            }
        }
        loaded.forEach(System.out::println);
    }
}
