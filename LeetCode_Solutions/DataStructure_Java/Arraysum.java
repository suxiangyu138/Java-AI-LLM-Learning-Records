package DataStructure_Java;

public class Arraysum {
    public static void main(String[] args) {
        // 1. 定义一个数组
        int[] arr = {1, 2, 3, 4, 5};

        // 2. 定义变量存总和
        int sum = 0;

        // 3. 遍历数组，累加
        for (int num : arr) {
            sum += num;  // 把每个元素加到 sum 里
        }

        // 4. 输出结果
        System.out.println("数组总和：" + sum);
    }
}