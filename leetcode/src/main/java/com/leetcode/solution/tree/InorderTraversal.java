package com.leetcode.solution.tree;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class InorderTraversal {

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

    // 递归法
    public static class SolutionRecursive {
        public List<Integer> inorderTraversal(TreeNode root) {
            List<Integer> res = new ArrayList<>();
            inorder(root, res);
            return res;
        }

        private void inorder(TreeNode node, List<Integer> res) {
            if (node == null) return;
            inorder(node.left, res);
            res.add(node.val);
            inorder(node.right, res);
        }
    }

    // 迭代法
    public static class SolutionIterative {
        public List<Integer> inorderTraversal(TreeNode root) {
            List<Integer> res = new ArrayList<>();
            Deque<TreeNode> stack = new LinkedList<>();
            TreeNode cur = root;

            while (cur != null || !stack.isEmpty()) {
                while (cur != null) {
                    stack.push(cur);
                    cur = cur.left;
                }
                cur = stack.pop();
                res.add(cur.val);
                cur = cur.right;
            }
            return res;
        }
    }

    // 辅助方法：根据数组构建二叉树（LeetCode 输入格式）
    private static TreeNode buildTree(Integer[] arr) {
        if (arr == null || arr.length == 0) return null;
        TreeNode[] nodes = new TreeNode[arr.length];
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != null) {
                nodes[i] = new TreeNode(arr[i]);
            }
        }
        for (int i = 0; i < arr.length / 2; i++) {
            if (nodes[i] != null) {
                int leftIdx = 2 * i + 1;
                int rightIdx = 2 * i + 2;
                if (leftIdx < arr.length) nodes[i].left = nodes[leftIdx];
                if (rightIdx < arr.length) nodes[i].right = nodes[rightIdx];
            }
        }
        return nodes[0];
    }

    public static void main(String[] args) {
        // 测试用例1：题目示例
        Integer[] arr1 = {1, null, 2, null, null, 3, null};
        TreeNode root1 = buildTree(arr1);

        SolutionRecursive solRec = new SolutionRecursive();
        SolutionIterative solIter = new SolutionIterative();

        System.out.println("测试用例1：");
        System.out.println("递归法输出: " + solRec.inorderTraversal(root1)); // 预期 [1, 3, 2]
        System.out.println("迭代法输出: " + solIter.inorderTraversal(root1)); // 预期 [1, 3, 2]
        System.out.println("------------------------");

        // 测试用例2：空树
        Integer[] arr2 = {};
        TreeNode root2 = buildTree(arr2);
        System.out.println("测试用例2（空树）：");
        System.out.println("递归法输出: " + solRec.inorderTraversal(root2)); // 预期 []
        System.out.println("迭代法输出: " + solIter.inorderTraversal(root2)); // 预期 []
        System.out.println("------------------------");

        // 测试用例3：单节点
        Integer[] arr3 = {1};
        TreeNode root3 = buildTree(arr3);
        System.out.println("测试用例3（单节点）：");
        System.out.println("递归法输出: " + solRec.inorderTraversal(root3)); // 预期 [1]
        System.out.println("迭代法输出: " + solIter.inorderTraversal(root3)); // 预期 [1]
        System.out.println("------------------------");

        Integer[] arr4 = {1,2,3,4,5,6,7,8,9};
        TreeNode root4 = buildTree(arr4);
        System.out.println("测试用例4（单节点）：");
        System.out.println("递归法输出: " + solRec.inorderTraversal(root4)); //
        System.out.println("迭代法输出: " + solIter.inorderTraversal(root4)); //
    }
}
