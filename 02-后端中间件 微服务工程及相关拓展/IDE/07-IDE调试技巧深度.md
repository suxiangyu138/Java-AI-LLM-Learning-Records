# 07 - IDE 调试技巧深度

> 🎯 调试能力是区分初级和高级工程师的核心素养。断点调试、条件断点、多线程调试、远程调试——掌握这些，排查 Bug 效率提升 10 倍

---

## 目录

1. [断点高级用法](#1-断点高级用法)
2. [多线程调试](#2-多线程调试)
3. [远程调试](#3-远程调试)
4. [AI 辅助调试](#4-ai-辅助调试)

---

## 1. 断点高级用法

```text
基础断点（人人会）：
  → 点击行号 → 红点 → 运行到此处暂停

条件断点（进阶）：
  → 右键红点 → Condition
  → 输入条件表达式：user.getId() == 12345
  → 只在满足条件时暂停（过滤掉 99.99% 的无关注调用）

日志断点（最实用）：
  → 右键红点 → 取消勾选 "Suspend"
  → 勾选 "Evaluate and log"
  → 输入："处理用户: " + user.getName()
  → 不中断执行，只打印日志 → 打印变量值的最快方式

异常断点：
  → Run → View Breakpoints → Java Exception Breakpoints
  → 选 NullPointerException
  → 任何 NPE 发生立即断点（不管有没有 try-catch）

方法断点：
  → 在方法签名行打断点
  → 进入/退出方法时暂停（适合排查"谁调用了这个方法"）
```

## 2. 多线程调试

```text
IDEA 多线程调试模式：

→ 右键断点 → 选择 "Thread"（默认是 "All"）
  All: 任何线程到断点都暂停（全部线程冻结）
  Thread: 只暂停当前线程，其他线程继续运行 ← 推荐

→ Frames 窗格：查看每个线程的调用栈
→ 切换线程：下拉选择不同线程 → 看各自的执行状态

并发问题排查技巧：
  1. 在两个线程的关键位置各打断点（Thread 模式）
  2. 让一个线程走到断点暂停
  3. 切换到另一个线程继续执行
  4. 观察共享变量的变化 → 找到竞态条件
```

## 3. 远程调试

```bash
# 1. 远程 JVM 启动参数（服务端）
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
     -jar app.jar

# Kubernetes 端口转发（本地连远程）
kubectl port-forward pod/app-xxx 5005:5005

# IDEA: Run → Edit Configurations → Remote JVM Debug
# Host: localhost, Port: 5005
```

```text
远程调试注意事项：
  → suspend=n → 不影响服务正常运行
  → 调试完立即断开（断点会阻塞所有请求！）
  → 生产环境慎用（建议只在测试/预发环境）
```

## 4. AI 辅助调试

```text
Cursor/Copilot 调试技巧：

1. 选中报错堆栈 → Ctrl+K → "分析这个错误，给出修复方案"
   → AI 理解堆栈 → 直接给出修复代码

2. 选中复杂方法 → Ctrl+L → "这段代码有什么 bug？"
   → AI 分析逻辑 → 找到 NPE/边界条件/并发问题

3. 在 Console 选中日志 → Ctrl+L → "这些日志说明什么问题？"
   → AI 分析异常/慢查询/内存警告

4. Agent 模式：程序报错 → 自动读 stacktrace → 修改代码 → 重跑
```

## 核心要点回顾

- 条件断点 = 只在你关心的场景暂停（filter 掉无关数据）
- 日志断点 = 不打 System.out.println 就能看变量值
- 异常断点 = 不猜哪里抛异常，直接断在异常发生处
- Thread 模式 = 多线程调试必备（只冻一个线程）
- AI + 调试 = 选中报错 → 让 AI 分析原因 → 修复

## 参考资料

1. IntelliJ IDEA Debugger 文档
2. Java 远程调试 (JPDA) 文档
