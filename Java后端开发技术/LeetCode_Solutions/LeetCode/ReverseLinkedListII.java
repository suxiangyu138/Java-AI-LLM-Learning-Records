package LeetCode;

// 完整类名，与文件名保持一致
public class ReverseLinkedListII {
    // 链表节点定义（本地运行必须包含）
    public static class ListNode {
        int val;
        ListNode next;
        ListNode() {}
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    // LeetCode 提交的核心逻辑
    public static class Solution {
        public ListNode reverseBetween(ListNode head, int left, int right) {
            ListNode dummy = new ListNode(-1);
            dummy.next = head;

            ListNode pre = dummy;
            for (int i = 1; i < left; i++) {
                pre = pre.next;
            }

            ListNode cur = pre.next;
            ListNode next;

            for (int i = 0; i < right - left; i++) {
                next = cur.next;
                cur.next = next.next;
                next.next = pre.next;
                pre.next = next;
            }

            return dummy.next;
        }
    }

    // 辅助方法：将数组转为链表（方便测试）
    private static ListNode arrayToList(int[] arr) {
        if (arr == null || arr.length == 0) return null;
        ListNode head = new ListNode(arr[0]);
        ListNode cur = head;
        for (int i = 1; i < arr.length; i++) {
            cur.next = new ListNode(arr[i]);
            cur = cur.next;
        }
        return head;
    }

    // 辅助方法：打印链表（验证结果）
    private static void printList(ListNode head) {
        ListNode cur = head;
        while (cur != null) {
            System.out.print(cur.val);
            if (cur.next != null) {
                System.out.print(" -> ");
            }
            cur = cur.next;
        }
        System.out.println();
    }

    // 主函数：测试用例执行
    public static void main(String[] args) {
        Solution solution = new Solution();

        // 测试用例1：题目示例（left=2, right=4）
        int[] arr1 = {1, 2, 3, 4, 5};
        ListNode head1 = arrayToList(arr1);
        System.out.println("测试用例1 - 原链表：");
        printList(head1);
        ListNode res1 = solution.reverseBetween(head1, 2, 4);
        System.out.println("反转后（left=2, right=4）：");
        printList(res1);
        System.out.println("------------------------");

        // 测试用例2：单节点链表（left=1, right=1）
        int[] arr2 = {5};
        ListNode head2 = arrayToList(arr2);
        System.out.println("测试用例2 - 原链表：");
        printList(head2);
        ListNode res2 = solution.reverseBetween(head2, 1, 1);
        System.out.println("反转后（left=1, right=1）：");
        printList(res2);
        System.out.println("------------------------");

        // 测试用例3：反转整个链表（left=1, right=3）
        int[] arr3 = {3, 5, 7};
        ListNode head3 = arrayToList(arr3);
        System.out.println("测试用例3 - 原链表：");
        printList(head3);
        ListNode res3 = solution.reverseBetween(head3, 1, 3);
        System.out.println("反转后（left=1, right=3）：");
        printList(res3);
    }
}
