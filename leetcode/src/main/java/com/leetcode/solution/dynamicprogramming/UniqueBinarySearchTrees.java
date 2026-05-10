package com.leetcode.solution.dynamicprogramming;

public class UniqueBinarySearchTrees {

    public static class Solution {
        public int numTrees(int n) {
            int[] dp = new int[n + 1];
            dp[0] = 1;
            dp[1] = 1;

            for (int i = 2; i <= n; i++) {
                for (int j = 1; j <= i; j++) {
                    dp[i] += dp[j - 1] * dp[i - j];
                }
            }
            return dp[n];
        }
    }

    public static void main(String[] args) {
        Solution solution = new Solution();

        // 测试用例1：n = 3，预期输出 5
        int n1 = 3;
        System.out.println("测试用例1：n = " + n1);
        System.out.println("输出: " + solution.numTrees(n1)); // 5
        System.out.println("------------------------");

        // 测试用例2：n = 1，预期输出 1
        int n2 = 1;
        System.out.println("测试用例2：n = " + n2);
        System.out.println("输出: " + solution.numTrees(n2)); // 1
        System.out.println("------------------------");

        // 测试用例3：n = 4，预期输出 14
        int n3 = 4;
        System.out.println("测试用例3：n = " + n3);
        System.out.println("输出: " + solution.numTrees(n3)); // 14
        System.out.println("------------------------");

        // 测试用例3：n = 5，预期输出
        int n4= 5;
        System.out.println("测试用例3：n = " + n4);
        System.out.println("输出: " + solution.numTrees(n4)); // 14
    }
}
