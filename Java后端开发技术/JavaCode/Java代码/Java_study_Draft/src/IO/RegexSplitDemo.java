package IO;

import java.util.Arrays;

public class RegexSplitDemo {
    public static void main(String[] args) {
        String text = "a,b;c d|e";
        String[] parts = text.split("[,; |]"); // 按逗号、分号、空格、竖线分割
        System.out.println(Arrays.toString(parts)); // [a, b, c, d, e]
    }
}