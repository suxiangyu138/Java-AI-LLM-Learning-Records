# LeetCode Hot 100 题目精讲（高频题型版）

> 说明：LeetCode Hot 100 官方题单包含 100 道经典高频题，完整逐题展开会形成超长文档，不利于复习与背诵。这个文档采用“按题型覆盖 + 代表题精讲”的方式，覆盖 Hot 100 中最核心、面试最高频的题目，并给出你偏好的极简可运行 LeetCode 代码、核心思路、复杂度分析与易错点。LeetCode 官方题单可在 Hot 100 页面查看。[cite:1]

## 使用建议

- 第一轮：只看“核心思路 + 代码模板”，建立题型映射。
- 第二轮：自己手写一遍代码，再看“易错点”。
- 第三轮：按“数组双指针 → 哈希 → 滑窗 → 链表 → 二叉树 → 回溯 → 动态规划”的顺序刷。
- 面试前：重点重刷标注为“必须秒掉”的题。

---

## 1. 两数之和（1）——必须秒掉

### 题意
给定整数数组 `nums` 和目标值 `target`，返回两个数的下标，使得两数之和等于目标值。[cite:1]

### 核心思路
使用哈希表记录“值 -> 下标”。遍历到 `nums[i]` 时，先检查 `target - nums[i]` 是否已出现；若出现，直接返回答案。这个做法把暴力枚举的两层循环优化为一层遍历。

### 极简代码（Java）
```java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int x = target - nums[i];
            if (map.containsKey(x)) return new int[]{map.get(x), i};
            map.put(nums[i], i);
        }
        return new int[0];
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(n)`
- 易错点：先查再放，避免同一个元素被重复使用。

---

## 2. 字母异位词分组（49）——哈希分组模板

### 题意
将字符串数组中互为字母异位词的字符串分组返回。[cite:1]

### 核心思路
异位词排序后结果相同，所以把每个字符串排序后的结果作为 key，原字符串加入对应列表即可。

### 极简代码（Java）
```java
class Solution {
    public List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> map = new HashMap<>();
        for (String s : strs) {
            char[] c = s.toCharArray();
            Arrays.sort(c);
            String key = new String(c);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }
        return new ArrayList<>(map.values());
    }
}
```

### 分析
- 时间复杂度：`O(n * k log k)`，`k` 为单个字符串平均长度
- 空间复杂度：`O(nk)`
- 易错点：key 必须是排序后的新字符串，不能直接拿字符数组当 key。

---

## 3. 最长连续序列（128）——集合去重 + 起点判断

### 题意
给定未排序整数数组，找出数字连续的最长序列长度，要求时间复杂度为 `O(n)`。[cite:1]

### 核心思路
先把所有数放入 `HashSet`。只有当 `x - 1` 不存在时，`x` 才是连续序列起点，再向后扩展统计长度。这样每个元素只会被访问常数次。

### 极简代码（Java）
```java
class Solution {
    public int longestConsecutive(int[] nums) {
        Set<Integer> set = new HashSet<>();
        for (int x : nums) set.add(x);
        int ans = 0;
        for (int x : set) {
            if (!set.contains(x - 1)) {
                int cur = x;
                int len = 1;
                while (set.contains(cur + 1)) {
                    cur++;
                    len++;
                }
                ans = Math.max(ans, len);
            }
        }
        return ans;
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(n)`
- 易错点：不要对数组中每个元素都暴力向后找，否则会退化。

---

## 4. 移动零（283）——双指针覆盖

### 题意
将数组中的 0 移动到末尾，同时保持非零元素相对顺序不变。[cite:1]

### 核心思路
`j` 指向当前应该填入非零元素的位置，`i` 负责扫描；先把所有非零数按顺序写到前面，再把剩余位置补 0。

### 极简代码（Java）
```java
class Solution {
    public void moveZeroes(int[] nums) {
        int j = 0;
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] != 0) nums[j++] = nums[i];
        }
        while (j < nums.length) nums[j++] = 0;
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(1)`
- 易错点：这是“覆盖写法”，比频繁交换更稳，也更适合面试表达。

---

## 5. 盛最多水的容器（11）——双指针贪心

### 题意
给定数组 `height`，每个元素表示竖线高度，选两条线和 x 轴组成容器，求最大装水量。[cite:1]

### 核心思路
左右指针从两端向中间收缩。每次面积由短板决定，因此移动较短的一边才有可能让面积变大；移动更高的一边没有意义。

### 极简代码（Java）
```java
class Solution {
    public int maxArea(int[] height) {
        int l = 0, r = height.length - 1, ans = 0;
        while (l < r) {
            ans = Math.max(ans, (r - l) * Math.min(height[l], height[r]));
            if (height[l] < height[r]) l++;
            else r--;
        }
        return ans;
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(1)`
- 易错点：核心证明是“短板效应”，这是面试常问点。

---

## 6. 三数之和（15）——排序 + 双指针

### 题意
找出数组中所有和为 0 且不重复的三元组。[cite:1]

### 核心思路
先排序，枚举第一个数 `i`，问题转为在 `i+1...n-1` 中找两数和为 `-nums[i]`。通过左右指针逼近，同时对 `i`、`l`、`r` 去重。

### 极简代码（Java）
```java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> ans = new ArrayList<>();
        for (int i = 0; i < nums.length - 2; i++) {
            if (i > 0 && nums[i] == nums[i - 1]) continue;
            int l = i + 1, r = nums.length - 1;
            while (l < r) {
                int s = nums[i] + nums[l] + nums[r];
                if (s < 0) l++;
                else if (s > 0) r--;
                else {
                    ans.add(Arrays.asList(nums[i], nums[l], nums[r]));
                    l++;
                    r--;
                    while (l < r && nums[l] == nums[l - 1]) l++;
                    while (l < r && nums[r] == nums[r + 1]) r--;
                }
            }
        }
        return ans;
    }
}
```

### 分析
- 时间复杂度：`O(n^2)`
- 空间复杂度：`O(log n)` 到 `O(n)`，取决于排序实现
- 易错点：去重必须写对，尤其是找到答案后 `l++`、`r--` 之后再去重。

---

## 7. 无重复字符的最长子串（3）——滑动窗口模板

### 题意
求一个字符串中不含重复字符的最长子串长度。[cite:1]

### 核心思路
维护一个滑动窗口 `[l, r]` 和字符出现集合。右指针扩展时，若出现重复字符，就不断移动左指针直到窗口合法。

### 极简代码（Java）
```java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l = 0, ans = 0;
        for (int r = 0; r < s.length(); r++) {
            while (set.contains(s.charAt(r))) set.remove(s.charAt(l++));
            set.add(s.charAt(r));
            ans = Math.max(ans, r - l + 1);
        }
        return ans;
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(字符集大小)`
- 易错点：窗口收缩条件是“当前字符重复”，不是固定收缩一次。

---

## 8. 找到字符串中所有字母异位词（438）——定长滑窗

### 题意
在字符串 `s` 中找到所有 `p` 的异位词子串起始下标。[cite:1]

### 核心思路
因为只包含小写字母，可用两个长度为 26 的数组统计窗口和目标串频次。窗口长度固定为 `p.length()`，每次滑动一格比较数组是否相等。

### 极简代码（Java）
```java
class Solution {
    public List<Integer> findAnagrams(String s, String p) {
        List<Integer> ans = new ArrayList<>();
        if (s.length() < p.length()) return ans;
        int[] a = new int[26], b = new int[26];
        for (int i = 0; i < p.length(); i++) {
            a[s.charAt(i) - 'a']++;
            b[p.charAt(i) - 'a']++;
        }
        if (Arrays.equals(a, b)) ans.add(0);
        for (int i = p.length(); i < s.length(); i++) {
            a[s.charAt(i) - 'a']++;
            a[s.charAt(i - p.length()) - 'a']--;
            if (Arrays.equals(a, b)) ans.add(i - p.length() + 1);
        }
        return ans;
    }
}
```

### 分析
- 时间复杂度：`O(n)`，这里比较长度固定为 26，可视作常数
- 空间复杂度：`O(1)`
- 易错点：这是“固定窗口长度”题型，和上一题“可变窗口长度”不同。

---

## 9. 最大子数组和（53）——Kadane 模板

### 题意
求连续子数组的最大和。[cite:1]

### 核心思路
定义 `dp[i]` 为“以 `i` 结尾的最大子数组和”，转移为：要么接上前面的子数组，要么从当前元素重新开始。因为只依赖前一个状态，所以可以压缩成一个变量。

### 极简代码（Java）
```java
class Solution {
    public int maxSubArray(int[] nums) {
        int cur = nums[0], ans = nums[0];
        for (int i = 1; i < nums.length; i++) {
            cur = Math.max(nums[i], cur + nums[i]);
            ans = Math.max(ans, cur);
        }
        return ans;
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(1)`
- 易错点：`cur` 表示“必须以当前位置结尾”，这是状态定义的关键。

---

## 10. 轮转数组（189）——数组翻转技巧

### 题意
把数组向右轮转 `k` 个位置。[cite:1]

### 核心思路
整体翻转，再翻转前 `k` 段，再翻转后半段。这个方法把轮转转化为局部逆序，空间复杂度为 `O(1)`。

### 极简代码（Java）
```java
class Solution {
    public void rotate(int[] nums, int k) {
        k %= nums.length;
        reverse(nums, 0, nums.length - 1);
        reverse(nums, 0, k - 1);
        reverse(nums, k, nums.length - 1);
    }

    void reverse(int[] nums, int l, int r) {
        while (l < r) {
            int t = nums[l];
            nums[l] = nums[r];
            nums[r] = t;
            l++;
            r--;
        }
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(1)`
- 易错点：必须先 `k %= nums.length`，否则 `k` 可能越界。

---

## 11. 相交链表（160）——双指针同路程

### 题意
找到两个单链表相交的起始节点。[cite:1]

### 核心思路
两个指针分别从两链表头出发，走到末尾后切换到另一条链表头。这样两人最终都走了 `a+b+c` 的总路程，因此会在交点相遇；若不相交则同时为 `null`。

### 极简代码（Java）
```java
public class Solution {
    public ListNode getIntersectionNode(ListNode headA, ListNode headB) {
        ListNode a = headA, b = headB;
        while (a != b) {
            a = a == null ? headB : a.next;
            b = b == null ? headA : b.next;
        }
        return a;
    }
}
```

### 分析
- 时间复杂度：`O(m+n)`
- 空间复杂度：`O(1)`
- 易错点：不是比较节点值，而是比较节点引用是否相同。

---

## 12. 反转链表（206）——链表基本功

### 题意
反转单链表并返回新头结点。[cite:1]

### 核心思路
用 `pre`、`cur`、`next` 三指针逐个反转边指向。这个模板几乎会在所有链表题中复用。

### 极简代码（Java）
```java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre = null, cur = head;
        while (cur != null) {
            ListNode nxt = cur.next;
            cur.next = pre;
            pre = cur;
            cur = nxt;
        }
        return pre;
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(1)`
- 易错点：修改 `cur.next` 前必须先保存 `nxt`。

---

## 13. 回文链表（234）——链表 + 快慢指针

### 题意
判断链表是否为回文链表。[cite:1]

### 核心思路
用快慢指针找到中点，反转后半段，然后从两端同时比较。面试中这是链表综合题，常考“找中点 + 反转 + 比较”。

### 极简代码（Java）
```java
class Solution {
    public boolean isPalindrome(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        ListNode pre = null;
        while (slow != null) {
            ListNode nxt = slow.next;
            slow.next = pre;
            pre = slow;
            slow = nxt;
        }
        while (pre != null) {
            if (head.val != pre.val) return false;
            head = head.next;
            pre = pre.next;
        }
        return true;
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(1)`
- 易错点：奇偶长度链表的中点位置要能统一处理，上述写法可以直接过。

---

## 14. 二叉树的中序遍历（94）——树遍历基础

### 题意
返回二叉树的中序遍历结果。[cite:1]

### 核心思路
中序顺序是“左 -> 根 -> 右”。递归写法最简洁，也是理解树 DFS 的起点。

### 极简代码（Java）
```java
class Solution {
    List<Integer> ans = new ArrayList<>();

    public List<Integer> inorderTraversal(TreeNode root) {
        dfs(root);
        return ans;
    }

    void dfs(TreeNode root) {
        if (root == null) return;
        dfs(root.left);
        ans.add(root.val);
        dfs(root.right);
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(h)`，`h` 为树高
- 易错点：遍历顺序不要背混，前中后序本质就是“根节点处理时机”不同。

---

## 15. 二叉树的层序遍历（102）——BFS 模板

### 题意
按层返回二叉树节点值。[cite:1]

### 核心思路
使用队列做 BFS。每次先记录当前层节点数 `size`，循环弹出这一层节点，并把下一层子节点入队。

### 极简代码（Java）
```java
class Solution {
    public List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> ans = new ArrayList<>();
        if (root == null) return ans;
        Queue<TreeNode> q = new LinkedList<>();
        q.offer(root);
        while (!q.isEmpty()) {
            int size = q.size();
            List<Integer> level = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                TreeNode node = q.poll();
                level.add(node.val);
                if (node.left != null) q.offer(node.left);
                if (node.right != null) q.offer(node.right);
            }
            ans.add(level);
        }
        return ans;
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(n)`
- 易错点：分层遍历的关键是先保存 `size`，不能一边遍历一边直接用动态队列长度。

---

## 16. 验证二叉搜索树（98）——上下界递归

### 题意
判断一棵二叉树是否为合法二叉搜索树。[cite:1]

### 核心思路
不是只比较当前节点和左右孩子，而是每个节点都必须落在一个合法区间 `(low, high)` 内。左子树更新上界，右子树更新下界。

### 极简代码（Java）
```java
class Solution {
    public boolean isValidBST(TreeNode root) {
        return dfs(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    boolean dfs(TreeNode root, long low, long high) {
        if (root == null) return true;
        if (root.val <= low || root.val >= high) return false;
        return dfs(root.left, low, root.val) && dfs(root.right, root.val, high);
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(h)`
- 易错点：必须用上下界思想，且边界建议用 `long` 防溢出。

---

## 17. 对称二叉树（101）——镜像递归

### 题意
判断二叉树是否轴对称。[cite:1]

### 核心思路
判断两棵树是否互为镜像：根值相等，且左树的左子树等于右树的右子树，左树的右子树等于右树的左子树。

### 极简代码（Java）
```java
class Solution {
    public boolean isSymmetric(TreeNode root) {
        return root == null || dfs(root.left, root.right);
    }

    boolean dfs(TreeNode a, TreeNode b) {
        if (a == null || b == null) return a == b;
        if (a.val != b.val) return false;
        return dfs(a.left, b.right) && dfs(a.right, b.left);
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(h)`
- 易错点：比较方向是“外侧 + 内侧”，不是同方向比较。

---

## 18. 岛屿数量（200）——网格 DFS 模板

### 题意
给定二维网格，统计岛屿数量。[cite:1]

### 核心思路
遍历网格，遇到 `'1'` 就计数并通过 DFS 把整块陆地淹没为 `'0'`。这类题的本质是“连通块计数”。

### 极简代码（Java）
```java
class Solution {
    public int numIslands(char[][] grid) {
        int ans = 0;
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (grid[i][j] == '1') {
                    ans++;
                    dfs(grid, i, j);
                }
            }
        }
        return ans;
    }

    void dfs(char[][] grid, int i, int j) {
        if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length || grid[i][j] != '1') return;
        grid[i][j] = '0';
        dfs(grid, i + 1, j);
        dfs(grid, i - 1, j);
        dfs(grid, i, j + 1);
        dfs(grid, i, j - 1);
    }
}
```

### 分析
- 时间复杂度：`O(mn)`
- 空间复杂度：`O(mn)`，最坏为递归栈深度
- 易错点：访问过的格子要立刻标记，否则会重复搜索。

---

## 19. 腐烂的橘子（994）——多源 BFS

### 题意
每分钟腐烂橘子会感染上下左右的新鲜橘子，求全部腐烂所需最短时间。[cite:1]

### 核心思路
把所有初始腐烂橘子一起入队，进行多源 BFS，每一层代表一分钟。这是图最短扩散问题的标准模板。

### 极简代码（Java）
```java
class Solution {
    public int orangesRotting(int[][] grid) {
        Queue<int[]> q = new LinkedList<>();
        int fresh = 0, time = 0;
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (grid[i][j] == 2) q.offer(new int[]{i, j});
                else if (grid[i][j] == 1) fresh++;
            }
        }
        int[][] d = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!q.isEmpty() && fresh > 0) {
            int size = q.size();
            time++;
            for (int k = 0; k < size; k++) {
                int[] cur = q.poll();
                for (int[] x : d) {
                    int ni = cur[0] + x[0], nj = cur[1] + x[1];
                    if (ni >= 0 && ni < grid.length && nj >= 0 && nj < grid[0].length && grid[ni][nj] == 1) {
                        grid[ni][nj] = 2;
                        fresh--;
                        q.offer(new int[]{ni, nj});
                    }
                }
            }
        }
        return fresh == 0 ? time : -1;
    }
}
```

### 分析
- 时间复杂度：`O(mn)`
- 空间复杂度：`O(mn)`
- 易错点：不是从一个点 BFS，而是所有腐烂点同时出发。

---

## 20. 括号生成（22）——回溯基本模板

### 题意
生成 `n` 对括号的所有合法组合。[cite:1]

### 核心思路
回溯过程中维护左右括号已使用数量。左括号数量可以小于 `n` 时继续加左括号；右括号数量小于左括号数量时才能加右括号。

### 极简代码（Java）
```java
class Solution {
    List<String> ans = new ArrayList<>();

    public List<String> generateParenthesis(int n) {
        dfs(new StringBuilder(), 0, 0, n);
        return ans;
    }

    void dfs(StringBuilder path, int l, int r, int n) {
        if (path.length() == 2 * n) {
            ans.add(path.toString());
            return;
        }
        if (l < n) {
            path.append('(');
            dfs(path, l + 1, r, n);
            path.deleteCharAt(path.length() - 1);
        }
        if (r < l) {
            path.append(')');
            dfs(path, l, r + 1, n);
            path.deleteCharAt(path.length() - 1);
        }
    }
}
```

### 分析
- 时间复杂度：与合法方案数相关
- 空间复杂度：递归深度 `O(n)`
- 易错点：回溯题重点不是背代码，而是明确“剪枝条件”和“递归状态”。

---

## 21. 全排列（46）——回溯 + used 数组

### 题意
返回一个数组的所有全排列。[cite:1]

### 核心思路
每一层枚举一个还未使用的数字，加入路径后递归，再撤销选择。`used[]` 是排列型回溯的核心辅助结构。

### 极简代码（Java）
```java
class Solution {
    List<List<Integer>> ans = new ArrayList<>();
    List<Integer> path = new ArrayList<>();
    boolean[] used;

    public List<List<Integer>> permute(int[] nums) {
        used = new boolean[nums.length];
        dfs(nums);
        return ans;
    }

    void dfs(int[] nums) {
        if (path.size() == nums.length) {
            ans.add(new ArrayList<>(path));
            return;
        }
        for (int i = 0; i < nums.length; i++) {
            if (used[i]) continue;
            used[i] = true;
            path.add(nums[i]);
            dfs(nums);
            path.remove(path.size() - 1);
            used[i] = false;
        }
    }
}
```

### 分析
- 时间复杂度：`O(n * n!)`
- 空间复杂度：`O(n)`，不计结果集
- 易错点：撤销选择时顺序要与加入选择对应。

---

## 22. 爬楼梯（70）——DP 入门题

### 题意
每次爬 1 或 2 阶，求到达第 `n` 阶的方法数。[cite:1]

### 核心思路
到达第 `n` 阶只能从 `n-1` 或 `n-2` 来，所以状态转移为 `f(n)=f(n-1)+f(n-2)`，本质是斐波那契。

### 极简代码（Java）
```java
class Solution {
    public int climbStairs(int n) {
        if (n <= 2) return n;
        int a = 1, b = 2;
        for (int i = 3; i <= n; i++) {
            int c = a + b;
            a = b;
            b = c;
        }
        return b;
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(1)`
- 易错点：这是最典型的“线性 DP + 空间压缩”。

---

## 23. 打家劫舍（198）——线性 DP

### 题意
相邻房屋不能同时偷，求能偷到的最大金额。[cite:1]

### 核心思路
对于每个位置，要么不偷当前房屋，继承前一个最优解；要么偷当前房屋，加上前两个位置的最优解。转移式：`dp[i]=max(dp[i-1], dp[i-2]+nums[i])`。

### 极简代码（Java）
```java
class Solution {
    public int rob(int[] nums) {
        int pre2 = 0, pre1 = 0;
        for (int x : nums) {
            int cur = Math.max(pre1, pre2 + x);
            pre2 = pre1;
            pre1 = cur;
        }
        return pre1;
    }
}
```

### 分析
- 时间复杂度：`O(n)`
- 空间复杂度：`O(1)`
- 易错点：`pre1`、`pre2` 分别对应前一项和前两项状态。

---

## 24. 零钱兑换（322）——完全背包最短方案

### 题意
给定硬币面额数组和总金额，求凑成金额所需最少硬币数，无法凑出返回 `-1`。[cite:1]

### 核心思路
`dp[i]` 表示凑成金额 `i` 的最少硬币数。枚举每个金额时尝试所有硬币：`dp[i]=min(dp[i], dp[i-coin]+1)`。

### 极简代码（Java）
```java
class Solution {
    public int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1);
        dp[0] = 0;
        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (coin <= i) dp[i] = Math.min(dp[i], dp[i - coin] + 1);
            }
        }
        return dp[amount] > amount ? -1 : dp[amount];
    }
}
```

### 分析
- 时间复杂度：`O(amount * n)`
- 空间复杂度：`O(amount)`
- 易错点：初始化为“不可能的大值”，最后再判断是否可达。

---

## 25. 最长递增子序列（300）——经典 DP

### 题意
求数组的最长严格递增子序列长度。[cite:1]

### 核心思路
定义 `dp[i]` 为以 `nums[i]` 结尾的最长递增子序列长度。枚举 `j < i`，若 `nums[j] < nums[i]`，则可以从 `j` 转移到 `i`。

### 极简代码（Java）
```java
class Solution {
    public int lengthOfLIS(int[] nums) {
        int[] dp = new int[nums.length];
        Arrays.fill(dp, 1);
        int ans = 1;
        for (int i = 1; i < nums.length; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[j] < nums[i]) dp[i] = Math.max(dp[i], dp[j] + 1);
            }
            ans = Math.max(ans, dp[i]);
        }
        return ans;
    }
}
```

### 分析
- 时间复杂度：`O(n^2)`
- 空间复杂度：`O(n)`
- 易错点：这是 Hot 100 中非常重要的子序列 DP 母题，二分优化版也要会说。

---

## 高频题型总表

| 题型 | 代表题 | 核心方法 | 面试要求 |
|---|---|---|---|
| 哈希 | 两数之和、字母异位词分组 | `HashMap` / `HashSet` | 必须秒掉 |
| 双指针 | 盛水容器、移动零、三数之和 | 左右收缩 / 快慢覆盖 | 必须秒掉 |
| 滑动窗口 | 无重复最长子串、找到异位词 | 可变窗口 / 定长窗口 | 高频 |
| 链表 | 反转链表、相交链表、回文链表 | 指针操作 + 中点 + 反转 | 高频 |
| 二叉树 | 中序、层序、BST 校验、对称树 | DFS / BFS / 递归边界 | 高频 |
| 图与网格 | 岛屿数量、腐烂橘子 | DFS 连通块 / 多源 BFS | 高频 |
| 回溯 | 括号生成、全排列 | 路径、选择、撤销、剪枝 | 高频 |
| 动态规划 | 最大子数组和、爬楼梯、打家劫舍、零钱兑换、LIS | 状态定义 + 转移方程 | 核心能力 |

## 刷题顺序建议

1. 数组与哈希：1、49、128、283、11、15
2. 滑动窗口：3、438
3. 链表：160、206、234
4. 二叉树：94、102、98、101
5. 图论/搜索：200、994
6. 回溯：22、46
7. 动态规划：53、70、198、322、300

## 面试表达模板

### 看到题先判断什么
- 是否可以用哈希把两层枚举降成一层。
- 是否满足双指针单调收缩条件。
- 是否适合滑动窗口维护一个区间。
- 是否能抽象为 DFS/BFS 连通块或最短扩散。
- 是否需要定义 DP 状态：`dp[i]`、`dp[i][j]`、或“以某位置结尾/截止”的最优值。

### 回答复杂度时怎么说
- 先报时间复杂度，再报空间复杂度。
- 说明是否使用额外哈希、递归栈、队列、结果集。
- 若题解依赖排序，主动指出排序复杂度会并入总复杂度。

## 补充说明

这份文档覆盖了 Hot 100 中最重要、最具代表性的高频题型与母题，适合你当前“Java 后端求职 + 面试刷题提分”的目标。若需要真正“完整 100 题逐题版”，建议继续拆成多份文档，例如：`数组篇.md`、`链表篇.md`、`树图篇.md`、`回溯DP篇.md`，这样更适合复习、背诵和 GitHub 管理。[cite:1]
