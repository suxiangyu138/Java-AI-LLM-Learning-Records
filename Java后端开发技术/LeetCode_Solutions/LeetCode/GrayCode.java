package LeetCode;

import java.util.ArrayList;
import java.util.List;

public class GrayCode {

    // 方法1：递归构造法
    public List<Integer> grayCodeRecursive(int n) {
        if (n == 0) {
            List<Integer> res = new ArrayList<>();
            res.add(0);
            return res;
        }
        // 递归获取n-1位格雷码
        List<Integer> pre = grayCodeRecursive(n - 1);
        List<Integer> cur = new ArrayList<>(pre);
        int add = 1 << (n - 1); // 等价于 2^(n-1)
        // 逆序遍历并给每个数添加最高位1
        for (int i = pre.size() - 1; i >= 0; i--) {
            cur.add(pre.get(i) + add);
        }
        return cur;
    }

    // 方法2：公式法（更高效，推荐）
    public List<Integer> grayCodeFormula(int n) {
        List<Integer> res = new ArrayList<>();
        int total = 1 << n; // 格雷码总数为2^n
        for (int i = 0; i < total; i++) {
            res.add(i ^ (i >> 1)); // 核心公式：i 异或 (i右移1位)
        }
        return res;
    }

    // 主函数：用于测试运行
    public static void main(String[] args) {
        GrayCode grayCode = new GrayCode();
        int n = 5; // 测试生成3位格雷码，可自行修改n的值

        // 测试递归法
        List<Integer> resultRecursive = grayCode.grayCodeRecursive(n);
        System.out.println("递归法生成" + n + "位格雷码：");
        System.out.println(resultRecursive);

        // 测试公式法
        List<Integer> resultFormula = grayCode.grayCodeFormula(n);
        System.out.println("公式法生成" + n + "位格雷码：");
        System.out.println(resultFormula);
    }
}