public class InterleavingString {

    public static class Solution {
        public boolean isInterleave(String s1, String s2, String s3) {
            int len1 = s1.length();
            int len2 = s2.length();
            int len3 = s3.length();

            if (len1 + len2 != len3) {
                return false;
            }

            boolean[][] dp = new boolean[len1 + 1][len2 + 1];
            dp[0][0] = true;

            for (int i = 1; i <= len1; i++) {
                dp[i][0] = dp[i-1][0] && (s1.charAt(i-1) == s3.charAt(i-1));
            }

            for (int j = 1; j <= len2; j++) {
                dp[0][j] = dp[0][j-1] && (s2.charAt(j-1) == s3.charAt(j-1));
            }

            for (int i = 1; i <= len1; i++) {
                for (int j = 1; j <= len2; j++) {
                    boolean fromS1 = dp[i-1][j] && (s1.charAt(i-1) == s3.charAt(i+j-1));
                    boolean fromS2 = dp[i][j-1] && (s2.charAt(j-1) == s3.charAt(i+j-1));
                    dp[i][j] = fromS1 || fromS2;
                }
            }

            return dp[len1][len2];
        }
    }

    public static void main(String[] args) {
        Solution solution = new Solution();

        // 测试用例1：题目示例
        String s1 = "aabcc";
        String s2 = "dbbca";
        String s3 = "aadbbcbcac";
        System.out.println("测试用例1：");
        System.out.println("s1 = " + s1 + ", s2 = " + s2 + ", s3 = " + s3);
        System.out.println("是否交错: " + solution.isInterleave(s1, s2, s3)); // 预期 true
        System.out.println("------------------------");

        // 测试用例2：反例
        s1 = "aabcc";
        s2 = "dbbca";
        s3 = "aadbbbaccc";
        System.out.println("测试用例2：");
        System.out.println("s1 = " + s1 + ", s2 = " + s2 + ", s3 = " + s3);
        System.out.println("是否交错: " + solution.isInterleave(s1, s2, s3)); // 预期 false
        System.out.println("------------------------");

        // 测试用例3：边界情况
        s1 = "";
        s2 = "";
        s3 = "";
        System.out.println("测试用例3（空字符串）：");
        System.out.println("是否交错: " + solution.isInterleave(s1, s2, s3)); // 预期 true
    }
}
