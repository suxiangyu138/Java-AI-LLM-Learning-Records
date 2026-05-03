package LeetCode;

public class DecodeWays {
    // 核心Solution逻辑（和上面一致）
    public static class Solution {
        public int numDecodings(String s) {
            if (s == null || s.length() == 0 || s.charAt(0) == '0') {
                return 0;
            }
            int n = s.length();
            int[] dp = new int[n + 1];
            dp[0] = 1;
            dp[1] = 1;

            for (int i = 2; i <= n; i++) {
                // 单个字符解码
                char singleChar = s.charAt(i - 1);
                if (singleChar != '0') {
                    dp[i] += dp[i - 1];
                }

                // 两个字符解码
                int twoDigit = Integer.parseInt(s.substring(i - 2, i));
                if (twoDigit >= 10 && twoDigit <= 26) {
                    dp[i] += dp[i - 2];
                }
            }
            return dp[n];
        }

        public char[] isBipartite(int[][] g1) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'isBipartite'");
        }
    }

    // 主函数：测试用例运行
    public static void main(String[] args) {
        Solution solution = new Solution();

        // 测试用例1：题目示例
        String s1 = "11106";
        int res1 = solution.numDecodings(s1);
        System.out.println("测试用例1：s = \"" + s1 + "\"");
        System.out.println("解码方法数：" + res1 + "（预期：2）\n");

        // 测试用例2：边界值-首字符为0
        String s2 = "0";
        int res2 = solution.numDecodings(s2);
        System.out.println("测试用例2：s = \"" + s2 + "\"");
        System.out.println("解码方法数：" + res2 + "（预期：0）\n");

        // 测试用例3：合法两位数
        String s3 = "26";
        int res3 = solution.numDecodings(s3);
        System.out.println("测试用例3：s = \"" + s3 + "\"");
        System.out.println("解码方法数：" + res3 + "（预期：2）\n");

        // 测试用例4：含10的情况
        String s4 = "10";
        int res4 = solution.numDecodings(s4);
        System.out.println("测试用例4：s = \"" + s4 + "\"");
        System.out.println("解码方法数：" + res4 + "（预期：1）\n");

        // 测试用例5：空字符串（防御性测试）
        String s5 = "";
        int res5 = solution.numDecodings(s5);
        System.out.println("测试用例5：s = \"" + s5 + "\"");
        System.out.println("解码方法数：" + res5 + "（预期：0）\n");
    }
}