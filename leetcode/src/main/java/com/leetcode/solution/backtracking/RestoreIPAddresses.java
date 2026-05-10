package com.leetcode.solution.backtracking;

import java.util.ArrayList;
import java.util.List;

public class RestoreIPAddresses {

    public static class Solution {
        public List<String> restoreIpAddresses(String s) {
            List<String> result = new ArrayList<>();
            if (s == null || s.length() < 4 || s.length() > 12) {
                return result;
            }
            backtrack(s, 0, new ArrayList<>(), result);
            return result;
        }

        private void backtrack(String s, int start, List<String> path, List<String> result) {
            if (path.size() == 4) {
                if (start == s.length()) {
                    result.add(String.join(".", path));
                }
                return;
            }

            for (int i = start; i < s.length() && i < start + 3; i++) {
                String segment = s.substring(start, i + 1);
                if (isValidSegment(segment)) {
                    path.add(segment);
                    backtrack(s, i + 1, path, result);
                    path.remove(path.size() - 1);
                }
            }
        }

        private boolean isValidSegment(String segment) {
            int len = segment.length();
            if (len > 3) return false;
            if (len > 1 && segment.charAt(0) == '0') return false;
            int num = Integer.parseInt(segment);
            return num >= 0 && num <= 255;
        }
    }

    public static void main(String[] args) {
        Solution solution = new Solution();

        // 测试用例1
        String s1 = "25525511135";
        System.out.println("测试用例1输入: " + s1);
        System.out.println("输出: " + solution.restoreIpAddresses(s1));
        System.out.println("------------------------");

        // 测试用例2
        String s2 = "0000";
        System.out.println("测试用例2输入: " + s2);
        System.out.println("输出: " + solution.restoreIpAddresses(s2));
        System.out.println("------------------------");

        // 测试用例3
        String s3 = "101023";
        System.out.println("测试用例3输入: " + s3);
        System.out.println("输出: " + solution.restoreIpAddresses(s3));
    }
}
