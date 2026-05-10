package com.leetcode.solution.array;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TwoSumTest {

    @Test
    void testTwoSumBasic() {
        TwoSum.Solution solution = new TwoSum.Solution();
        int[] nums = {2, 7, 11, 15};
        int[] result = solution.twoSum(nums, 9);
        assertArrayEquals(new int[]{0, 1}, result);
    }

    @Test
    void testTwoSumWithDuplicateValues() {
        TwoSum.Solution solution = new TwoSum.Solution();
        int[] nums = {3, 3};
        int[] result = solution.twoSum(nums, 6);
        assertArrayEquals(new int[]{0, 1}, result);
    }

    @Test
    void testTwoSumNotFound() {
        TwoSum.Solution solution = new TwoSum.Solution();
        int[] nums = {1, 2, 3};
        int[] result = solution.twoSum(nums, 10);
        assertArrayEquals(new int[]{-1, -1}, result);
    }
}
