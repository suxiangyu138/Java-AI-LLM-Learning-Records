# 02 - GitHub Copilot 深度使用

> 🎯 Copilot = 补全 + Chat + Agent + CLI — 最成熟的 AI 编码工具，覆盖从 IDE 到终端的全场景

## 1. 基础定义

GitHub Copilot 是由 GitHub/OpenAI 推出的 AI 编程助手，提供代码补全、对话式编程、代码审查、CLI 辅助等功能。

**核心本质**：嵌入 IDE 的实时 AI 编码伙伴。

## 2. 溯源历史

- 2021.06 — Copilot 技术预览版（基于 Codex）
- 2022.06 — GA 版本，$10/月
- 2023.03 — Copilot Chat Beta
- 2023.12 — Copilot Chat GA，GPT-4 驱动
- 2024.06 — Copilot Workspace（Agent 模式）
- 2025.02 — Copilot Agent Mode GA、Vision 支持

## 3. 底层原理

```text
Copilot 推理流水线：

  ① 上下文收集：
     当前文件(光标前后) + 相邻 Tab 文件 + 最近编辑文件
     + 项目元数据(语言/框架/依赖)
     
  ② Prompt 构建：
     拼接上下文 → 附加 FIM 格式 → 截断到 Token 限制
     
  ③ 模型推理：
     云端 API 调用 → 生成多个候选 → 过滤+排序
     
  ④ 后处理：
     去重 → 语法检查 → 截断到合适边界 → 展示
```

## 4. 对比辨析

**Copilot 各模式对比**：

| 模式 | 触发 | 粒度 | 延迟 |
|------|------|:---:|:---:|
| 行内补全 | 自动 | 行~方法 | <300ms |
| Chat 对话 | Ctrl+I | 任意 | 1-5s |
| Agent 模式 | 手动触发 | 多文件 | 5-30s |
| CLI `/` 命令 | 终端 | 命令级 | <1s |

## 5. 价值与应用

### Java 后端高效 Prompt

```text
// ① 函数签名 → 自动生成实现
// 写：public List<User> filterActiveUsers(List<User> users) {
// Copilot 自动生成：return users.stream()...

// ② 注释驱动开发
// 写：// Convert this List<Order> to Map<Long, Order> keyed by orderId
// Copilot 自动生成转换代码

// ③ 测试生成
// 在测试类中写：@Test public void shouldHandleEmptyList() {
// Copilot 自动生成完整测试
```

### Chat 高频 Prompt

```text
/explain 解释这段代码的逻辑
/fix 修复这个 Bug
/tests 为这个方法生成单元测试
/simplify 简化这段代码
/doc 为这个方法生成 JavaDoc
```

## 6. 性能与代价

- 成本：$10/月个人版，$39/月商业版
- 补全准确率：~35%（Java/TypeScript 最高）
- 延迟：行内 <300ms，Chat 1-3s

## 7. 实操验证

```java
// 测试 Copilot 理解上下文的能力
// 1. 定义一个接口
public interface UserRepository {
    Optional<User> findById(Long id);
}

// 2. 在 Service 中输入
public User getUser(Long id) {
    // Copilot 应该自动补全：return userRepo.findById(id).orElseThrow(...)
}
```

## 8. 核心口诀

> Copilot = 补全(快)+Chat(懂)+Agent(做)。行内补全看速度，Chat 对话看理解，Agent 模式看规划。
