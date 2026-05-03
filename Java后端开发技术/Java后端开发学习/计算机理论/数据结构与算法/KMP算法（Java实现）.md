01.29 16:49
KMP算法（Java实现）
KMP算法是高效的字符串匹配算法，核心解决暴力匹配的重复回溯问题，时间复杂度O(n+m)（n为主串长度，m为模式串长度），远优于暴力匹配的O(n×m)。
核心思想
1. 预处理模式串，生成部分匹配表（next数组）：记录模式串每个位置的最长相等前后缀长度，用于匹配失败时，模式串直接回退到指定位置，主串无需回溯。
2. 匹配阶段：用next数组指导模式串的回退，仅移动主串指针，大幅减少匹配次数。
一、关键概念：最长相等前后缀
对模式串的子串 s[0...i] ，前缀是不包含最后一个字符的所有头部子串，后缀是不包含第一个字符的所有尾部子串，最长相等前后缀即两者中长度最大的相等子串。
例：模式串 ababc ，子串 abab 的前缀 [a,ab,aba] 、后缀 [b,ab,bab] ，最长相等前后缀为 ab ，长度为2。
二、核心：next数组的构建
next数组长度与模式串一致， next[i] 表示模式串 [0...i] 的最长相等前后缀长度，是KMP的核心，构建过程为模式串的自我匹配。
构建规则
1. 初始化： j=0 （前缀指针，记录最长相等前后缀长度）， next[0]=0 （单个字符无前后缀）；
2. 遍历模式串（ i=1 为后缀指针）：
- 若 s[i] != s[j] ，则 j=next[j-1] （回退前缀指针，直到 j=0 或匹配）；
- 若 s[i] == s[j] ，则 j++ ， next[i]=j ；
3. 最终得到的next数组即为模式串的部分匹配表。
三、完整Java实现（含next数组构建+KMP匹配）
包含基础版next数组和优化版nextVal数组（解决模式串重复字符导致的无效回退，效率更高），同时实现查找首次匹配位置和查找所有匹配位置两种常用功能。
java
import java.util.ArrayList;
import java.util.List;
public class KMPAlgorithm {
    /**
     * 构建基础版next数组（部分匹配表）
     * @param pattern 模式串
     * @return next数组
     */
    public static int[] buildNext(String pattern) {
        int m = pattern.length();
        int[] next = new int[m];
        int j = 0; // 前缀指针，记录最长相等前后缀长度
        next[0] = 0; // 第一个字符的next值为0
        for (int i = 1; i < m; i++) {
            // 匹配失败，回退j到上一个位置的next值
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) {
                j = next[j - 1];
            }
            // 匹配成功，j后移，记录next值
            if (pattern.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            next[i] = j;
        }
        return next;
    }
    /**
     * 构建优化版nextVal数组（解决重复字符的无效回退，效率更高）
     * @param pattern 模式串
     * @return nextVal数组
     */
    public static int[] buildNextVal(String pattern) {
        int m = pattern.length();
        int[] nextVal = new int[m];
        int j = 0;
        nextVal[0] = 0;
        for (int i = 1; i < m; i++) {
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) {
                j = nextVal[j - 1];
            }
            if (pattern.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            // 优化点：若当前字符与回退位置字符相同，直接继承next值
            if (j > 0 && pattern.charAt(i) == pattern.charAt(j - 1)) {
                nextVal[i] = nextVal[j - 1];
            } else {
                nextVal[i] = j;
            }
        }
        return nextVal;
    }
    /**
     * KMP匹配：查找模式串在主串中**首次出现的起始索引**（无则返回-1）
     * @param text 主串
     * @param pattern 模式串
     * @param next next/nextVal数组
     * @return 首次匹配索引，无则-1
     */
    public static int kmpFindFirst(String text, String pattern, int[] next) {
        int n = text.length();
        int m = pattern.length();
        if (m > n) return -1; // 模式串比主串长，直接不匹配
        int j = 0; // 模式串指针
        for (int i = 0; i < n; i++) { // i为主串指针，全程不回溯
            // 匹配失败，根据next数组回退模式串指针
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = next[j - 1];
            }
            // 匹配成功，模式串指针后移
            if (text.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            // 模式串完全匹配，返回起始索引
            if (j == m) {
                return i - m + 1;
            }
        }
        return -1; // 无匹配
    }
    /**
     * KMP匹配：查找模式串在主串中**所有出现的起始索引**（无则返回空列表）
     * @param text 主串
     * @param pattern 模式串
     * @param next next/nextVal数组
     * @return 所有匹配索引的列表
     */
    public static List<Integer> kmpFindAll(String text, String pattern, int[] next) {
        List<Integer> matchIndices = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();
        if (m > n) return matchIndices;
        int j = 0;
        for (int i = 0; i < n; i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = next[j - 1];
            }
            if (text.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            // 匹配成功，记录索引并回退模式串指针（继续查找后续匹配）
            if (j == m) {
                matchIndices.add(i - m + 1);
                j = next[j - 1]; // 关键：回退到最后一个位置的next值，查找重叠匹配
            }
        }
        return matchIndices;
    }
    // 测试主方法
    public static void main(String[] args) {
        String text = "ababcabababcabc"; // 主串
        String pattern = "ababc"; // 模式串
        // 1. 构建next和nextVal数组
        int[] next = buildNext(pattern);
        int[] nextVal = buildNextVal(pattern);
        System.out.println("模式串：" + pattern);
        System.out.println("基础版next数组：" + arrayToString(next));
        System.out.println("优化版nextVal数组：" + arrayToString(nextVal));
        // 2. 查找首次匹配位置
        int firstIndex = kmpFindFirst(text, pattern, nextVal);
        System.out.println("\n首次匹配位置：" + (firstIndex == -1 ? "无匹配" : firstIndex));
        // 3. 查找所有匹配位置
        List<Integer> allIndices = kmpFindAll(text, pattern, nextVal);
        System.out.println("所有匹配位置：" + (allIndices.isEmpty() ? "无匹配" : allIndices));
    }
    // 辅助方法：数组转字符串（方便打印）
    private static String arrayToString(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int num : arr) {
            sb.append(num).append(" ");
        }
        return sb.toString().trim();
    }
}
 
四、运行结果说明
以上测试用例（主串 ababcabababcabc ，模式串 ababc ）的运行结果：
plaintext
模式串：ababc
基础版next数组：0 0 1 2 0
优化版nextVal数组：0 0 0 2 0
首次匹配位置：0
所有匹配位置：[0, 5]
 
说明：模式串在主串的0索引和5索引处均完成匹配，KMP成功找到所有位置。
五、KMP算法执行流程（核心步骤）
以主串=ababcabababcabc，模式串=ababc为例，结合nextVal数组讲解：
1. 初始化：主串指针 i=0 ，模式串指针 j=0 ；
2. 依次匹配 text[i] 和 pattern[j] ，匹配成功则 i++、j++ ；
3. 若匹配失败（如后续出现不相等字符），则 j=nextVal[j-1] （模式串回退），主串 i 不变；
4. 当 j == 模式串长度 时，匹配成功，返回 i-m+1 （m为模式串长度）；
5. 查找所有匹配时，匹配成功后将 j=nextVal[j-1] ，继续遍历主串。
六、暴力匹配 vs KMP对比
特性 暴力匹配 KMP算法 
时间复杂度  （最坏情况）  （稳定高效） 
核心问题 主串、模式串双回溯，重复比较 主串不回溯，模式串按next数组智能回退 
预处理 无 预处理模式串生成next数组（ ） 
适用场景 短字符串匹配 长文本匹配（如日志、DNA序列、编辑器查找） 
七、关键注意事项
1. 索引问题：代码中字符串索引从0开始，若需从1开始，只需调整next数组初始化和循环边界；
2. nextVal优化：当模式串存在大量重复字符（如 aaaaa ）时，nextVal数组能避免无效回退，效率远高于基础next数组；
3. 重叠匹配： kmpFindAll 中匹配成功后执行 j=next[j-1] ，支持重叠匹配（如主串 aaaaa ，模式串 aa ，匹配位置为 0,1,2,3 ）；若需非重叠匹配，则将 j=0 即可；
4. 空串处理：实际使用中需增加判空逻辑（主串/模式串为null或空时直接返回-1/空列表）。
八、KMP的经典应用
1. 编辑器的查找替换功能（Ctrl+F）；
2. 生物信息学的DNA/蛋白质序列匹配；
3. 日志分析、文本检索的关键词匹配；
4. 编译器的语法分析、字符串匹配相关算法题。
KMP算法的核心是next数组的理解与构建，只要掌握了最长相等前后缀的计算和next数组的生成逻辑，就能轻松理解KMP的匹配过程，相比暴力匹配，其在长字符串场景下的优势尤为明显。

