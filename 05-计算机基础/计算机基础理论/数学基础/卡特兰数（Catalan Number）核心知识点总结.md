卡特兰数（Catalan Number）核心知识点总结
卡特兰数是组合数学中的一种经典数列，常用于解决各类“合法路径”“括号匹配”“树形结构计数”等离散计数问题，在计算机算法、数据结构领域应用广泛。

 -----------------------------------------------------------------------------------------------
一、 定义与通项公式
1. 第 n 个卡特兰数 记为 C_n，核心通项公式有两种形式：
    - 形式1（基本形式）
    C_n = \frac{1}{n+1}\binom{2n}{n} = \frac{(2n)!}{(n+1)!n!} \quad (n\ge0)
    - 形式2（递推公式）
    C_0 = 1,\quad C_{n+1} = \sum_{i=0}^n C_iC_{n-i} \quad (n\ge0)
    递推含义：第 n+1 个卡特兰数可拆分为两个子问题的卡特兰数乘积之和。
2. 前几项卡特兰数（n 从 0 开始）：
    C_0=1,\ C_1=1,\ C_2=2,\ C_3=5,\ C_4=14,\ C_5=42,\ C_6=132\cdots

 -----------------------------------------------------------------------------------------------
二、 计算机领域的典型应用场景
1. 括号匹配问题
    - 问题：n 对括号有多少种合法的匹配方式？
    - 示例：n=2 时，合法方式为  (()) 、 ()()  → C_2=2。
    - 原理：用 C_i 表示前 i 对括号的合法数，新增一对括号时，需嵌套或并列已有的合法结构。
2. 出栈序列问题
    - 问题：一个栈的进栈序列为 1,2,\dots,n，有多少种合法的出栈序列？
    - 示例：n=3 时，合法序列有 5 种 → C_3=5。
    - 原理：假设第 k 个元素最后出栈，前 k-1 个元素的出栈数为 C_{k-1}，后 n-k 个为 C_{n-k}，总和即递推公式。
3. 二叉搜索树计数
    - 问题：有 n 个不同节点，能构成多少种不同的二叉搜索树（BST）？
    - 原理：选一个节点作为根，左子树有 i 个节点（C_i 种），右子树有 n-1-i 个节点（C_{n-1-i} 种），总和为 C_n。
4. 凸多边形三角剖分问题
    - 问题：一个凸 n+2 边形，用不相交的对角线将其分成三角形，有多少种剖分方式？
    - 示例：凸 4 边形（n=2）有 2 种剖分方式 → C_2=2。
5. 网格路径问题
    - 问题：从 (0,0) 到 (n,n) 的网格中，只向右或向上走，且不越过对角线 y=x 的路径数 → 答案为 C_n。

 -----------------------------------------------------------------------------------------------
三、 Java 代码实现（计算卡特兰数）
1.  递归法（适合小 n，存在重复计算）
    java  
    public class CatalanNumber {
    // 递归计算第 n 个卡特兰数
    public static long catalanRecursive(int n) {
        if (n == 0 || n == 1) {
            return 1;
        }
        long res = 0;
        for (int i = 0; i < n; i++) {
            res += catalanRecursive(i) * catalanRecursive(n - 1 - i);
        }
        return res;
    }
    public static void main(String[] args) {
        int n = 5;
        System.out.println("第 " + n + " 个卡特兰数：" + catalanRecursive(n)); // 输出 42
    }
    }
 
2.  动态规划法（优化重复计算，适合中等 n）
    java  
    public class CatalanNumber {
    public static long catalanDP(int n) {
        long[] dp = new long[n + 1];
        dp[0] = 1;
        dp[1] = 1;
        // 递推计算 dp[2] 到 dp[n]
        for (int i = 2; i <= n; i++) {
            for (int j = 0; j < i; j++) {
                dp[i] += dp[j] * dp[i - 1 - j];
            }
        }
        return dp[n];
    }
    public static void main(String[] args) {
        int n = 6;
        System.out.println("第 " + n + " 个卡特兰数：" + catalanDP(n)); // 输出 132
    }
    }
 
3.  公式法（直接计算，适合较大 n，需注意溢出）
    java  
    import java.math.BigInteger;
    public class CatalanNumber {
    // 用 BigInteger 避免溢出，计算更大的卡特兰数
    public static BigInteger catalanFormula(int n) {
        // 计算 (2n)! / [(n+1)! * n!]
        BigInteger twoNFact = factorial(2 * n);
        BigInteger nPlus1Fact = factorial(n + 1);
        BigInteger nFact = factorial(n);
        return twoNFact.divide(nPlus1Fact.multiply(nFact));
    }
    private static BigInteger factorial(int num) {
        BigInteger res = BigInteger.ONE;
        for (int i = 2; i <= num; i++) {
            res = res.multiply(BigInteger.valueOf(i));
        }
        return res;
    }
    public static void main(String[] args) {
        int n = 10;
        System.out.println("第 " + n + " 个卡特兰数：" + catalanFormula(n)); // 输出 16796
    }
    }

 -----------------------------------------------------------------------------------------------
四、 核心特点
1. 卡特兰数增长速度非常快，n 较大时（如 n>20），普通  long  类型会溢出，需用  BigInteger 。
2. 本质是分治计数，所有应用场景都可抽象为“将大问题拆分为两个独立子问题”的模型。
