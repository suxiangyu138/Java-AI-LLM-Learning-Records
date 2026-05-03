package LeetCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubsetsII {

    public List<List<Integer>> subsetsWithDup(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        // 先排序，使相同元素相邻
        Arrays.sort(nums);
        backtrack(nums, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrack(int[] nums, int start, List<Integer> path, List<List<Integer>> result) {
        // 将当前路径加入结果集
        result.add(new ArrayList<>(path));

        for (int i = start; i < nums.length; i++) {
            // 跳过重复元素：如果当前元素和前一个相同，且前一个未被选择（i > start），则跳过
            if (i > start && nums[i] == nums[i - 1]) {
                continue;
            }
            // 选择当前元素
            path.add(nums[i]);
            // 递归到下一层
            backtrack(nums, i + 1, path, result);
            // 回溯，撤销选择
            path.remove(path.size() - 1);
        }
    }

    public static void main(String[] args) {
        SubsetsII solution = new SubsetsII();

        // 测试示例1
        int[] nums1 = {1, 2, 2,3,33,3,4,43};
        List<List<Integer>> result1 = solution.subsetsWithDup(nums1);
        System.out.println("示例1输出：");
        System.out.println(result1);

        // 测试示例2
        int[] nums2 = {0};
        List<List<Integer>> result2 = solution.subsetsWithDup(nums2);
        System.out.println("示例2输出：");
        System.out.println(result2);
    }
}
