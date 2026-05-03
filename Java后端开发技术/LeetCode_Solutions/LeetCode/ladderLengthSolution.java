package LeetCode;
import java.util.*;

public class ladderLengthSolution {
    public int ladderLength(String beginWord, String endWord, List<String> wordList) {
        // 1. 把单词列表放入 Set，方便 O(1) 查找
        Set<String> wordSet = new HashSet<>(wordList);
        if (!wordSet.contains(endWord)) {
            return 0;
        }

        // 2. BFS 队列：存储当前单词 + 当前路径长度
        Queue<Pair<String, Integer>> queue = new LinkedList<>();
        queue.offer(new Pair<>(beginWord, 1));

        // 3. 已访问集合，避免重复遍历
        Set<String> visited = new HashSet<>();
        visited.add(beginWord);

        int wordLen = beginWord.length();

        // 4. BFS 主循环
        while (!queue.isEmpty()) {
            Pair<String, Integer> curr = queue.poll();
            String word = curr.getKey();
            int level = curr.getValue();

            // 尝试把每一位替换成 a-z
            char[] chars = word.toCharArray();
            for (int i = 0; i < wordLen; i++) {
                char original = chars[i];

                for (char c = 'a'; c <= 'z'; c++) {
                    chars[i] = c;
                    String newWord = new String(chars);

                    // 找到终点，直接返回
                    if (newWord.equals(endWord)) {
                        return level + 1;
                    }

                    // 合法且未访问过
                    if (wordSet.contains(newWord) && !visited.contains(newWord)) {
                        visited.add(newWord);
                        queue.offer(new Pair<>(newWord, level + 1));
                    }
                }
                // 恢复原字符
                chars[i] = original;
            }
        }

        // 没有路径
        return 0;
    }

    // Java 自带的 Pair 在 javafx 里，这里简单实现一个
    static class Pair<K, V> {
        private K key;
        private V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}


class TestMainladderLengthSolution {
    public static void main(String[] args) {
       ladderLengthSolution s = new ladderLengthSolution();
    
    String begin = "hit";
    String end = "cog";
    List<String> list = Arrays.asList("hot","dot","dog","lot","log","cog");
    
    System.out.println(s.ladderLength(begin, end, list)); // 输出 5
    }
}