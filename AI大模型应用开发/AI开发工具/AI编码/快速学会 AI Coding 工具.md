# 快速学会 AI Coding 工具（2025 主流版）

定位：**后端/前端/全栈开发提效必备，覆盖 Claude Code、Cursor、Copilot、Cline 等主流 AI 编程工具**。

一句话理解：AI Coding 工具 = 你写代码时身边坐着一个 **能读整个项目的 AI 程序员**，可以帮你写代码、改 bug、解释逻辑、重构项目。

***

## 一、核心认知（必读）

### AI 编程工具不是什么

- ❌ 不是银弹，不能替你思考架构
- ❌ 不是 100% 准确（复杂逻辑可能出错）
- ❌ 不是替代开发者，而是**放大器**——放大你的生产力

### AI 编程工具是什么

- ✅ **超级自动补全**：你写一句，它补一段
- ✅ **对话式编程**：用自然语言描述需求，它生成代码
- ✅ **全项目感知**：读懂整个项目上下文（你用的是哪个工具？）
- ✅ **多文件编辑**：一次性改多个文件

### 分类速记

```
AI 编程工具
├── IDE 集成类（内嵌到编辑器）
│   ├── Trae（你现在用的）
│   ├── Cursor（类 VS Code）
│   ├── Windsurf（类 VS Code）
│   └── GitHub Copilot（VS Code / JetBrains 插件）
├── CLI 终端类（命令行操作）
│   ├── Claude Code（Anthropic 出品）
│   ├── Codex CLI（OpenAI 出品）
│   └── Aider（开源）
├── 插件类（VS Code 扩展）
│   ├── Cline / Roo Code
│   ├── Continue.dev（开源）
│   └── Codeium
└── 代码审查类
    └── CodeRabbit / Copilot Code Review
```

> ⚠️ 所有工具的核心能力差别不大，**差异主要在交互方式、上下文理解深度和价格**。

***

## 二、Claude Code（终端版 AI 程序员）

**定位**：Anthropic 官方出的终端 AI 工具，直接在命令行里对话写代码。

### 核心特点

```
- 在终端中运行：npm install -g @anthropic-ai/claude-code
- 直接操控文件系统：能读、写、搜索、执行命令
- 全项目上下文：自动理解整个项目结构
- 无需切换编辑器：纯命令行交互
```

### 基本使用

```bash
# 安装
npm install -g @anthropic-ai/claude-code

# 进入项目目录，启动
cd my-project
claude

# 进入对话模式，直接说需求
> 帮我把 utils.js 里的 fetchData 函数改成 async/await 写法
> 检查这个项目有没有 SQL 注入风险
> 帮我给这个 React 组件写单元测试
```

### 核心能力

```bash
# 1. 读文件、写文件
> 读取 src/main.java 并解释这个类的设计

# 2. 搜索代码库
> 找到项目中所有调用 deleteUser 的地方

# 3. 执行命令
> 运行 npm run test 看有没有失败的用例，修复它们

# 4. Git 操作
> 帮我 review 上一次 commit 的代码
> 生成一个清晰的 commit message

# 5. 多文件重构
> 把 UserService 拆成 UserQueryService 和 UserCommandService，同步更新所有引用
```

### 优缺点

| 优点                         | 缺点                |
| -------------------------- | ----------------- |
| 极度深入理解项目上下文                | 纯命令行，看不到图形界面      |
| 直接操控文件系统                   | 新手可能有门槛           |
| Anthropic Claude 模型（代码能力强） | 按 token 计费，大量使用较贵 |
| 可自动化脚本批量任务                 | 需要 Node.js 环境     |

### 适合谁

- 习惯终端操作的后端开发者
- 需要批量处理项目文件的场景
- 不想切换 IDE 的老手

***

## 三、Cursor（AI 增强版 VS Code）

**定位**：基于 VS Code 魔改，内置 AI 对话、多文件编辑、全项目感知。目前最火的 AI IDE。

### 核心特点

```
- 界面跟 VS Code 几乎一样（无缝迁移）
- Tab 补全：写代码时实时预测下一段代码
- Ctrl+K：选中代码，用自然语言修改
- Ctrl+L / Ctrl+I：打开对话面板，全局聊天
- Composer：同时修改多个文件
- .cursorrules：项目级 AI 行为配置
```

### 核心快捷键

```
Ctrl + K          选中代码 → 用自然语言改写
Ctrl + L          打开对话面板（问任何问题）
Ctrl + I          Composer（多文件编辑）
Ctrl + Enter      代码库全局搜索 + 提问
Tab               接受 AI 补全建议
```

### 核心配置 .cursorrules（必懂）

在项目根目录建 `.cursorrules` 文件，告诉 AI 你的项目规则：

```markdown
你是一个 Java 后端开发专家，请遵守以下规则：

1. 使用 Spring Boot 3.x + JDK 17
2. 所有 API 返回统一格式 Result<T>
3. Controller 只做参数校验，业务放 Service
4. 数据库操作用 MyBatis-Plus
5. 异常统一用 GlobalExceptionHandler 处理
6. 不要写注释（除非复杂逻辑）
7. 遵循阿里巴巴 Java 开发规范
```

### 优缺点

| 优点                          | 缺点          |
| --------------------------- | ----------- |
| 界面和 VS Code 几乎一样            | 免费版有次数限制    |
| 支持多种模型（GPT-4o、Claude 3.5 等） | Pro 版 $20/月 |
| Composer 多文件编辑体验好           | 大项目有时响应慢    |
| 插件生态兼容 VS Code              | 不开源         |
| 一键导入 VS Code 配置             | <br />      |

### 适合谁

- 全栈开发者（前端 + 后端）
- VS Code 用户想升级 AI 体验
- 需要多文件同时编辑

***

## 四、GitHub Copilot（最老牌、最普及）

**定位**：GitHub 出品的 AI 补全工具，以插件形式嵌入 VS Code / JetBrains / Neovim。

### 核心特点

```
- 最强 Tab 补全（写一句，直接补一整段）
- 支持 VS Code + JetBrains + Neovim + Xcode
- GitHub Copilot Chat：内嵌对话
- 代码审查功能（Copilot Code Review）
- 企业版支持组织级知识库
```

### 基本使用

```
1. VS Code 安装 GitHub Copilot 插件
2. 登录 GitHub 账号，激活试用/订阅
3. 正常写代码，灰色建议出现时按 Tab 接受

核心体验：你写注释，它生成代码
// 从数据库中查询过去7天注册的用户，按注册时间倒序
// → AI 自动生成完整 SQL + Java/TypeScript 代码
```

### Copilot 独有功能

```
1. Copilot Chat：侧边栏对话
   - /explain   → 解释选中的代码
   - /fix       → 修复选中的代码
   - /tests     → 生成单元测试
   - /doc       → 生成文档注释

2. Copilot Code Review
   - PR 提交时自动审查代码
   - 类似 CodeRabbit，但集成在 GitHub

3. Copilot Workspace
   - 直接在 GitHub 仓库中通过对话完成 Issue 开发
```

### 优缺点

| 优点              | 缺点             |
| --------------- | -------------- |
| 补全体验最流畅         | 多文件编辑弱于 Cursor |
| 生态系统最成熟         | 对话能力一般         |
| GitHub 深度集成     | 依赖 GitHub 账号   |
| 支持 JetBrains 全系 | $10/月（个人）      |
| 企业安全合规好         | 没有免费版          |

### 适合谁

- JetBrains（IDEA）用户
- 习惯写注释驱动的开发者
- 企业团队（安全合规要求高）

***

## 五、Cline / Roo Code（VS Code 插件版 Claude Code）

**定位**：在 VS Code 里实现 Claude Code 的体验，是当前最火的 AI 插件。

### Cline 核心特点

```
- VS Code 插件，安装即用
- 支持多种 API：Claude、GPT、Gemini、DeepSeek 等
- 自动读文件、写文件、执行终端命令
- 每步操作需要你点"确认"（安全可控）
- Plan → Act 模式：先规划，再执行
```

### 基本使用

```bash
# 1. VS Code 插件市场搜索 "Cline" 安装
# 2. 配置 API Key（Claude / OpenAI / DeepSeek 任一）
# 3. 侧边栏打开 Cline，开始对话

# 对话示例：
> 帮我在这个 Spring Boot 项目里加一个用户登录接口
> → AI 自动创建 Controller、Service、DTO，写好完整代码
> → 每一步你都可以看到它在读什么文件、写什么文件
```

### Roo Code vs Cline

```
Roo Code 是 Cline 的 fork（分支），增加了：
  - 更灵活的模型切换
  - 自定义模式（Code / Architect / Debug 等角色）
  - 更好的 DeepSeek 支持
  - .roomodes 项目级模式配置
```

### 优缺点

| 优点                      | 缺点                          |
| ----------------------- | --------------------------- |
| 在 VS Code 里用（不离开编辑器）    | 每步要确认，略慢                    |
| 支持各种 API（省钱可选 DeepSeek） | 用 Claude API 时 burn token 快 |
| 完全开源免费                  | 需要自己搞 API Key               |
| 操作透明（看得到每一步）            | 新手配 API 有一点点门槛              |

### 适合谁

- 想用 Claude Code 能力但不想离开 VS Code
- 用 DeepSeek 省钱（效果也不错）
- 喜欢透明可控的交互方式

***

## 六、Windsurf（前 Codeium）

**定位**：类 Cursor 的 AI IDE，独特卖点是自动多步操作。

### 核心特点

```
- Cascade：对话式 AI 助手，自动执行多步操作
- Supercomplete：智能补全
- 自动检测并运行终端命令
- 支持多文件同时编辑
```

### 独有亮点

```
Cascade 模式：
你一句话，AI 自动完成多步操作
→ 搜索代码库
→ 读相关文件
→ 写新代码
→ 跑终端命令

比 Cursor 更自动，比 Claude Code 更有图形界面
```

### 优缺点

| 优点             | 缺点              |
| -------------- | --------------- |
| 自动化程度高于 Cursor | 用户群较小           |
| 免费版较慷慨         | 模型选择不如 Cursor 多 |
| Cascade 体验流畅   | 不如 Cursor 成熟    |

***

## 七、Continue.dev（开源、最灵活）

**定位**：开源的 VS Code / JetBrains AI 插件，完全掌控自己的数据和模型。

### 核心特点

```
- 完全开源（GitHub 数万 Star）
- 支持本地模型（Ollama、LM Studio）
- 支持云端模型（Claude、GPT、Gemini 等）
- 可自定义大模型提供商
- 数据完全留在本地
```

### 与众不同的点

```bash
# 可以用本地模型，完全免费 + 完全隐私
1. 安装 Ollama → ollama pull codellama
2. Continue 配置中选 Local Model
3. 全程不联网，代码不离开你电脑
```

### 优缺点

| 优点          | 缺点             |
| ----------- | -------------- |
| 完全开源免费      | 本地模型效果差于云端     |
| 支持本地模型（隐私）  | 需要自己折腾配置       |
| 插件式架构可定制    | 补全体验不如 Copilot |
| @ 上下文引用功能好用 | <br />         |

### 适合谁

- 代码不能外传的安全敏感场景
- 喜欢开源、喜欢折腾
- 想用本地模型省钱的开发者

***

## 八、Aider（命令行 AI 结对编程）

**定位**：终端里最强的 AI 结对编程工具，支持 Git 原生集成。

### 核心特点

```
- 纯命令行，无 GUI
- Git 原生集成：每次改动自动 commit
- 支持地图文件（repo map）：让 AI 理解项目全貌
- 支持几乎所有主流模型
```

### 基本使用

```bash
# 安装
pip install aider-chat

# 使用
aider --model claude-3-5-sonnet-20241022
# 进入对话
> 给 user_service.py 加上分页查询功能
# AI 自动读文件、写文件、git commit

# 最方便：直接让 AI 看 Git 改动
aider --gui  # 浏览器界面（可选）
```

### 优缺点

| 优点            | 缺点        |
| ------------- | --------- |
| Git 自动 commit | 纯终端，无图形界面 |
| 地图文件让 AI 很懂项目 | 不太适合前端可视化 |
| 模型自由度高        | 新手学习曲线    |
| 开源免费（工具本身）    | API 费用自付  |

### 适合谁

- CLI 爱好者
- 后端/数据工程/AI 工程
- 已经习惯 Git 工作流

***

## 九、Trae（你正在用的）

**定位**：字节跳动出品的 AI IDE，国内开发者友好。

### 核心特点

```
- 基于 VS Code 内核
- 内置 AI 对话 + 多文件编辑
- 国内网络友好（不需要代理）
- 支持配置多种模型（DeepSeek 等）
```

### 和你正在看的其他工具对比

```
Trae ≈ Cursor（国内版）≈ Windsurf

相同点：AI 对话、多文件编辑、补全
不同点：Trae 国内体验更好，但成熟度略低于 Cursor
```

***

## 十、横向对比速查表

| 维度    | Claude Code | Cursor | Copilot | Cline   | Windsurf | Continue | Aider   | Trae |
| ----- | ----------- | ------ | ------- | ------- | -------- | -------- | ------- | ---- |
| 类型    | CLI         | IDE    | 插件      | 插件      | IDE      | 插件       | CLI     | IDE  |
| 价格    | 按量付费        | $20/月  | $10/月   | 免费+API费 | $15/月    | 免费       | 免费+API费 | 免费   |
| 多文件编辑 | ✅           | ✅      | ❌       | ✅       | ✅        | ❌        | ✅       | ✅    |
| 补全体验  | ❌           | ✅      | ✅✅✅     | ❌       | ✅        | ✅        | ❌       | ✅    |
| 开源    | ❌           | ❌      | ❌       | ✅       | ❌        | ✅        | ✅       | ❌    |
| 本地模型  | ❌           | ❌      | ❌       | ✅       | ❌        | ✅        | ✅       | ✅    |
| 学习门槛  | 中           | 低      | 低       | 中       | 低        | 中        | 高       | 低    |
| 终端操控  | ✅✅✅         | ✅      | ❌       | ✅       | ✅        | ❌        | ✅✅✅     | ✅    |

***

## 十一、选型建议（快速决策）

### 按你的角色选

```
Java/后端开发者 → Copilot（IDEA插件）+ Cursor（辅助）
全栈开发者     → Cursor 或 Windsurf
CLI 爱好者     → Claude Code + Aider
省钱党         → Continue.dev + DeepSeek API
数据敏感       → Continue.dev + 本地 Ollama
企业团队       → GitHub Copilot Enterprise
国内用户       → Trae / Cursor / Cline + DeepSeek
```

### 按使用场景选

```
日常写代码补全          → Copilot（无敌）
理解大型项目、改 bug   → Cursor / Claude Code
多文件重构              → Cursor Composer / Claude Code
自动化批量任务          → Claude Code / Cline
写单元测试              → 任意（Copilot Chat 最方便）
代码审查                → Copilot Code Review / CodeRabbit
```

***

## 十二、省钱攻略

```
1. 不订阅，只用 API：
   - Cline + DeepSeek API（¥2/百万 token，极省）
   - Aider + DeepSeek API
   - Continue.dev + DeepSeek API

2. 一工具多用：
   - Cursor 既有补全又有对话，一个订阅覆盖多个需求
   - 不需要同时订阅 Copilot + Cursor

3. 本地模型（完全免费）：
   - Ollama + Codestral / Qwen2.5-Coder
   - 效果不如云端，但简单任务足够

4. 免费方案：
   - Trae（免费）
   - Continue.dev（永远免费）
   - Cline（开源免费 + 自备API）
   - GitHub Copilot Free（每月有限额）
```

***

## 十三、快速上手路线

```
第1天：装好 Trae 或 Cursor，体验 AI 对话 + Tab 补全
第2天：学会用 .cursorrules 配置项目规则
第3天：用 AI 完成一次多文件重构（拆 Service、加接口等）
第4天：试试 Cline（VS Code插件），体验透明可控的 AI 操作
第5天：装 Claude Code，在终端完成一个完整功能开发
第6天：配置 Continue.dev + DeepSeek，享受免费 AI 编程
第7天：形成自己的工作流组合，不再纠结工具选择
```

***

## 附赠：避坑提醒

1. ⚠️ **AI 生成的代码一定要 review**：它可能引入安全漏洞或不合理设计
2. ⚠️ **不要把密钥/密码发给 AI**：API 调用会经过第三方服务器
3. ⚠️ **复杂业务逻辑不要让 AI 从头写**：让它理解你的思路再辅助
4. ⚠️ **.cursorrules 写得好，效率翻倍**：花 10 分钟写好规则，省几百分钟调试
5. ⚠️ **不要一个工具没学会就换下一个**：先精通一个，再拓展
6. ⚠️ **免费版有限额**：Cursor 免费 2000 次/月，Copilot Free 2000 次/月
7. ⚠️ **API 模式注意费用**：Claude API 全项目上下文会消耗大量 token，DeepSeek 便宜很多
8. ✅ **最佳实践**：AI 写代码 → 你 review → Git commit → AI 写测试 → 跑 CI → 上线

***

> **总结**：工具是放大器，不是替代品。**你越强，AI 让你更强。你不懂的东西，AI 也帮不了你。**
