package com.leetcode.solution.dynamicprogramming;

import java.util.ArrayList;
import java.util.List;

public class UniqueBinarySearchTreesII {

    // 二叉树节点定义
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode() {}
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    public static class Solution {
        public List<TreeNode> generateTrees(int n) {
            if (n == 0) {
                return new ArrayList<>();
            }
            return buildTrees(1, n);
        }

        private List<TreeNode> buildTrees(int start, int end) {
            List<TreeNode> allTrees = new ArrayList<>();
            if (start > end) {
                allTrees.add(null);
                return allTrees;
            }

            for (int i = start; i <= end; i++) {
                List<TreeNode> leftTrees = buildTrees(start, i - 1);
                List<TreeNode> rightTrees = buildTrees(i + 1, end);

                for (TreeNode left : leftTrees) {
                    for (TreeNode right : rightTrees) {
                        TreeNode root = new TreeNode(i);
                        root.left = left;
                        root.right = right;
                        allTrees.add(root);
                    }
                }
            }
            return allTrees;
        }
    }

    // 辅助方法：将树序列化为 LeetCode 风格的字符串，方便打印查看
    private static String serializeTree(TreeNode root) {
        if (root == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        serializeHelper(root, sb);
        // 移除末尾的逗号和空格
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("]");
        return sb.toString();
    }

    private static void serializeHelper(TreeNode node, StringBuilder sb) {
        if (node == null) {
            sb.append("null, ");
            return;
        }
        sb.append(node.val).append(", ");
        serializeHelper(node.left, sb);
        serializeHelper(node.right, sb);
    }

    public static void main(String[] args) {
        Solution solution = new Solution();

        // 测试用例1：n = 3
        int n1 = 3;
        System.out.println("测试用例1：n = " + n1);
        List<TreeNode> trees1 = solution.generateTrees(n1);
        for (TreeNode tree : trees1) {
            System.out.println(serializeTree(tree));
        }
        System.out.println("------------------------");

        // 测试用例2：n = 1
        int n2 = 1;
        System.out.println("测试用例2：n = " + n2);
        List<TreeNode> trees2 = solution.generateTrees(n2);
        for (TreeNode tree : trees2) {
            System.out.println(serializeTree(tree));
        }
        System.out.println("------------------------");


        // 测试用例2：n = 1
        int n3 = 4;
        System.out.println("测试用例2：n = " + n3);
        List<TreeNode> trees3 = solution.generateTrees(n3);
        for (TreeNode tree : trees3) {
            System.out.println(serializeTree(tree));
        }
    }
}
