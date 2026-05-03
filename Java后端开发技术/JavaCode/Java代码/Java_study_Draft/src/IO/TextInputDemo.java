package IO;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class TextInputDemo {
    public static void main(String[] args) throws IOException {
        // 方式1：BufferedReader
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream("text.txt"), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            System.out.println(line);
        }

        // 方式2：Scanner
        try (Scanner scanner = new Scanner(new File("text.txt"), StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }
        }
    }
}