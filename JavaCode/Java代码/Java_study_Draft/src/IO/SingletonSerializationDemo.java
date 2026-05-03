package IO;

import java.io.*;

enum Season implements Serializable {
    SPRING, SUMMER, AUTUMN, WINTER
}

public class SingletonSerializationDemo {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Season season = Season.SPRING;

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("season.dat"))) {
            oos.writeObject(season);
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("season.dat"))) {
            Season loaded = (Season) ois.readObject();
            System.out.println(loaded == Season.SPRING); // true（枚举序列化保证单例）
        }
    }
}