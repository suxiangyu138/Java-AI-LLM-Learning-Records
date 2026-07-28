# 05 - Agent 自主工作闭环

> 🎯 新一代 Codex 的核心价值不是"写代码"，而是"需求→PR 的全流程自动化"。理解这个闭环 = 理解 Agent 的本质

---

## 1. 闭环全景

```text
┌─────────────────────────────────────────────────────┐
│                  Codex Agent 闭环                     │
│                                                      │
│  ① 接收需求 ──→ ② 分析仓库 ──→ ③ 生成 Diff          │
│       ↑                                  ↓           │
│       │                          ④ 沙箱执行测试       │
│       │                                  ↓           │
│       └──────── ⑤ 迭代修复 ←────── 测试失败？         │
│                    ↓ 测试通过                         │
│              ⑥ 生成 PR 提交                           │
└─────────────────────────────────────────────────────┘
```

---

## 2. 六步详解

### Step 1：接收自然语言需求

**输入**：用自然语言描述要做什么
**Codex 做的事**：
- 解析意图（新功能/Bug修复/重构/测试）
- 确认范围和约束
- 提取关键实体（Controller/Service/Entity 名称）

```text
示例需求：
"在 UserController 中添加根据邮箱查询用户的接口。
要求：参数校验（@Valid）、Swagger 文档注解、单元测试覆盖。
参考项目中已有的 OrderController 的风格。"
```

### Step 2：读取关联代码仓库

**Codex 做的事**：
- `git clone` 或直接读取本地项目
- 分析项目结构（Maven/Gradle 的 pom.xml/build.gradle）
- 读取参考文件（OrderController）→ 学习代码风格
- 分析依赖关系（Entity/Mapper 定义）

### Step 3：生成代码变更

**产出**：完整的 git diff，包括：
```text
修改的文件：
  UserController.java    （新增方法）
  UserService.java       （新增方法）
  UserServiceTest.java   （新增测试类）

每个文件的内容：
  → 完整的类/方法定义
  → 与项目风格一致的注解和命名
  → 参数校验、异常处理
  → Swagger 文档注解
```

### Step 4：云端沙箱执行验证

```text
沙箱中的执行流程：
  ① git checkout 到新分支
  ② 应用 Codex 生成的 diff
  ③ mvn compile（编译）
  ④ mvn test（运行测试）
  ⑤ 检查 Swagger 文档是否生成
  
  成功 → 进入 Step 6
  失败 → 进入 Step 5
```

### Step 5：迭代修复

```text
测试失败 → Codex 读取错误信息 → 分析根因 → 修改代码 → 再测试

常见需要迭代的场景：
  → 缺少 import 语句
  → 方法签名不匹配
  → 测试断言错误
  → 依赖注入缺失

Codex 最多迭代 3-5 轮，超过则请求人工介入
```

### Step 6：生成 PR

```text
Codex 自动生成：
  ① git commit message：
     "feat: add email-based user query endpoint
      - Add GET /api/users/by-email endpoint
      - Add email validation
      - Add Swagger documentation
      - Add unit tests (passing)"

  ② PR 描述：
     ## 改动内容
     - 新增根据邮箱查询用户接口
     ## 测试结果
     - 单元测试 ✓ (3/3 passed)
     ## 注意事项
     - 需要数据库添加 email 字段索引
```

---

## 3. 迭代机制详解

### 错误类型与修复策略

| 错误类型 | Codex 修复策略 | 示例 |
|----------|---------------|------|
| 编译错误 | 直接修复语法 | 缺少 import → 添加 |
| 测试失败 | 分析断言→修改逻辑 | 期望值错误→修正 |
| 风格不一致 | 对比现有代码→调整 | 注解格式→统一 |
| 依赖缺失 | 检查 pom.xml→添加 | 缺少依赖→补充 |

### 迭代上限

```text
Codex 内置的保护机制：
  → 最大迭代次数：5 轮（默认）
  → 单次沙箱超时：10 分钟
  → 超过上限 → 输出已完成的部分 + 标注未解决的问题
```

---

## 4. 与传统开发的效率对比

| 任务 | 纯人工 | Codex Agent | 提升 |
|------|:---:|:---:|:---:|
| CRUD 接口开发(含测试) | 2h | 15min | 8× |
| Bug 修复 | 45min | 10min | 4.5× |
| 单元测试补充 | 1h | 5min | 12× |
| 代码重构 | 3h | 30min | 6× |
| 文档/注释补充 | 30min | 2min | 15× |
