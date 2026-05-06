# LeetCode Solutions

LeetCode 算法题解与 Java 数据结构教程。

## 项目结构

```
src/
├── main/java/com/leetcode/
│   ├── datastructure/          # 数据结构教程
│   │   ├── ArraySum.java
│   │   ├── LinearStructureTutorial.java
│   │   ├── ListGraph.java
│   │   └── QueueTutorial.java
│   └── solution/               # LeetCode 题解（按算法类型分类）
│       ├── array/              # 数组 / 哈希表
│       ├── backtracking/       # 回溯算法
│       ├── dynamicprogramming/ # 动态规划
│       ├── graph/              # 图论
│       ├── linkedlist/         # 链表
│       └── tree/               # 树
└── test/java/com/leetcode/     # 单元测试
    └── solution/
```

## 构建与测试

```bash
mvn clean compile    # 编译
mvn test             # 运行测试
mvn exec:java        # 运行单个 main 方法（需要 maven-exec-plugin）
```

## 技术栈

- Java 24
- Maven 3.x
- JUnit 5 (Jupiter)
