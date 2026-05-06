package com.leetcode.solution.array;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TwoSum {
    public static class Solution {
        public int[] twoSum(int[] nums, int target) {
            Map<Integer, Integer> map = new HashMap<>();
            for (int i = 0; i < nums.length; i++) {
                int complement = target - nums[i];
                if (map.containsKey(complement)) {
                    return new int[]{map.get(complement), i};
                }
                map.put(nums[i], i);
            }
            return new int[]{-1, -1};
        }
    }

    public static void main(String[] args) {
        Solution solution = new Solution();

        int[] nums1 = {2, 7, 11, 15};
        int target1 = 9;
        int[] res1 = solution.twoSum(nums1, target1);
        System.out.println("测试用例1：nums = " + Arrays.toString(nums1) + ", target = " + target1);
        System.out.println("结果：" + Arrays.toString(res1) + "（预期：[0, 1]）\n");

        int[] nums2 = {3, 2, 4};
        int target2 = 6;
        int[] res2 = solution.twoSum(nums2, target2);
        System.out.println("测试用例2：nums = " + Arrays.toString(nums2) + ", target = " + target2);
        System.out.println("结果：" + Arrays.toString(res2) + "（预期：[1, 2]）\n");

        int[] nums3 = {3, 3};
        int target3 = 6;
        int[] res3 = solution.twoSum(nums3, target3);
        System.out.println("测试用例3：nums = " + Arrays.toString(nums3) + ", target = " + target3);
        System.out.println("结果：" + Arrays.toString(res3) + "（预期：[0, 1]）\n");
    }
}
