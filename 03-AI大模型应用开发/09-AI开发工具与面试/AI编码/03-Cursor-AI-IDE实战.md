# 03 - Cursor AI IDE 实战

> 🎯 Cursor = VS Code 内核 + AI 原生 — Agent 模式自主编辑多文件、Composer 对话式重构、`.cursorrules` 项目级配置

## 1. 基础定义

Cursor 是基于 VS Code 内核、内置深度 AI 集成的代码编辑器。核心创新是将 AI 从"辅助补全"升级为"自主 Agent"。

**核心本质**：AI-native IDE = 编辑器 + AI Agent + 代码库索引。

**区分边界**：
- Cursor：AI 原生的完整 IDE（修改多文件、运行终端）
- Copilot：VS Code 插件（补全为主、Chat 辅助）
- Claude Code：终端 Agent CLI（无 GUI、纯命令行）

## 2. 溯源历史

- 2023.03 — Cursor 首次发布（基于 GPT-4）
- 2024.04 — Cursor Agent 模式 Beta
- 2024.09 — Composer 模式（对话式多文件编辑）
- 2025 — `.cursorrules` + MCP 支持

## 3. 底层原理

```text
Cursor 的 Repo 级索引：

  ① 代码库向量化：所有代码文件 → Embedding → 向量索引
  ② 实时检索：每次 AI 请求，检索最相关的代码片段
  ③ Agent 循环：思考→编辑→读文件→验证→重复

  Cursor Tab（补全）：
    不仅看当前文件 → 还检索相关代码 → 更高的准确率
```

## 4. 对比辨析

| 维度 | Cursor | GitHub Copilot | Claude Code |
|------|:---:|:---:|:---:|
| 定位 | AI-native IDE | VS Code 插件 | Terminal Agent |
| 多文件编辑 | ✅ Agent 自主 | ⚠️ 有限 | ✅ 自主 |
| 终端操作 | ✅ 内置 | ❌ | ✅ |
| 代码库索引 | ✅ 向量检索 | ✅ 语义检索 | ✅ grep+rg |
| GUI | ✅ | ✅ | ❌ |
| 学习曲线 | 低（VS Code 用户） | 极低 | 中 |

## 5. 价值与应用

### .cursorrules 项目配置

```text
// .cursorrules — 放在项目根目录
你是 Java 后端开发专家。
技术栈：Spring Boot 3 + MyBatis Plus + Redis + Kafka。
编码规范：
- 使用 Lombok（@Data/@Slf4j）
- 异常处理用全局 @RestControllerAdvice
- Controller→Service→Mapper 三层
- 写单元测试用 JUnit5 + Mockito
- 每个方法写 JavaDoc
```

### Java 高频操作

```bash
# Ctrl+K — 编辑选中代码
"把这个 for 循环改成 Stream API"
"给这个方法加 @Transactional 和异常处理"
"生成这个接口的 MockMvc 测试"

# Ctrl+L — 对话模式
"这个类有没有线程安全问题？"
"帮我重构这个 Service，拆成两个类"

# Agent 模式
"帮我创建一个 UserController，包含 CRUD 接口"
"给项目加全局异常处理和参数校验"
```

## 6. 性能与代价

- 成本：Pro $20/月（500次高级请求/月）
- 索引构建：首次 1-5 分钟（取决于项目大小）
- 补全延迟：<200ms（本地模型）+ <1s（云端模型）

## 7. 实操验证

```text
Agent 模式的完整验证：
  ① 创建 Spring Boot 项目 → Agent 生成完整 pom.xml + 启动类
  ② 添加 User CRUD → Agent 生成 Controller/Service/Mapper/Entity
  ③ 加单元测试 → Agent 生成完整测试类含 Mock
  ④ 运行验证 → mvn test 通过
  
  人工只需 Code Review + 微调
```

## 8. 核心口诀

> Cursor = VS Code 的身体 + AI 的大脑。Tab 补全、Chat 理解、Agent 执行，三层递进。
