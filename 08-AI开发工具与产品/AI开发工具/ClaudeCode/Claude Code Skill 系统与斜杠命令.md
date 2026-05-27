# Claude Code Skill 系统与斜杠命令详解（实战版）

> **文档定位**：Claude Code CLI Skill 与 Slash Command 速查
> **核心问题**：Skill 是什么？怎么用？怎么自定义？有哪些内置命令？

---

## 一、Skill 是什么

**Skill**（技能/插件）是 Claude Code 的**功能扩展包**，为特定任务提供专门的指令、工具链和工作流。

```
普通对话：通用 AI 能力，每次要从零开始描述需求
加载 Skill：预设了领域知识 + 工作流 + 工具组合，一步到位
```

### 1.1 Skill 的加载方式

```
方式一：用户输入 /skill-name
  /review → 自动加载代码审查 Skill

方式二：AI 根据任务自动匹配
  用户说"审查这个 PR" → AI 检测到匹配 → 调用 Skill("code-review")

方式三：通过 Skill 工具显式调用
```

---

## 二、可用 Skill 分类

### 2.1 开发类

| Skill | 触发词 | 功能 |
|---|---|---|
| `code-review` | `/review` | 多层级代码审查 |
| `feature-dev` | 开发新功能 | 引导式功能开发 |
| `commit` | `/commit` | 规范提交 |
| `pr` | `/pr` | 创建 PR |
| `simplify` | `/simplify` | 代码简化/优化 |
| `security-review` | 安全审查 | 安全漏洞检查 |
| `init` | `/init` | 初始化 CLAUDE.md |
| `test-browser` | 浏览器测试 | 前端功能测试 |

### 2.2 文档类

| Skill | 触发词 | 功能 |
|---|---|---|
| `pdf` | 操作 PDF | 读取/创建/合并/拆分 PDF |
| `docx` | 操作 Word | 创建/编辑 Word 文档 |
| `pptx` | 操作 PPT | 创建/编辑演示文稿 |
| `xlsx` | 操作 Excel | 创建/编辑电子表格 |

### 2.3 工作流类

| Skill | 触发词 | 功能 |
|---|---|---|
| `brainstorm` | 头脑风暴 | 需求探索和方案讨论 |
| `plan` | 制定计划 | 多步骤任务规划 |
| `debug` | 调试 | 系统化根因分析 |
| `work` | 工作执行 | 高质量功能实现 |
| `compound` | 知识积累 | 沉淀项目经验 |

### 2.4 设计类

| Skill | 触发词 | 功能 |
|---|---|---|
| `frontend-design` | 前端设计 | 高质量 UI 界面 |
| `canvas-design` | 海报设计 | 视觉设计 |
| `algorithmic-art` | 算法艺术 | p5.js 生成艺术 |

### 2.5 运维类

| Skill | 触发词 | 功能 |
|---|---|---|
| `clean-gone-branches` | 清理分支 | 清理远程已删除的本地分支 |
| `sessions` | 会话历史 | 搜索过去对话 |
| `loop` | 循环执行 | 定时/循环命令 |

---

## 三、Skill 工作原理

```
用户触发 /review
    ↓
Claude Code 查找匹配的 Skill 定义
    ↓
加载 Skill 的 System Prompt（扩展指令）
    ↓
Skill 代码接管对话 → 执行专属工作流
    ↓
工作流完成 → 返回标准对话模式
```

---

## 四、内置 Slash Commands（速查）

### 4.1 对话控制

| 命令 | 作用 |
|---|---|
| `/help` | 查看帮助 |
| `/clear` | 清空当前对话 |
| `/compact` | 压缩上下文（释放 Token） |
| `/resume` | 恢复上一个被压缩的对话 |

### 4.2 配置与信息

| 命令 | 作用 |
|---|---|
| `/config` | 配置主题、模型 |
| `/cost` | 查看 Token 消耗 |
| `/context` | 查看当前上下文大小 |
| `/doctor` | 诊断环境问题 |
| `/memory` | 打开记忆管理 |

### 4.3 工作流

| 命令 | 作用 |
|---|---|
| `/init` | 初始化项目 CLAUDE.md |
| `/review` | 代码审查当前分支 |
| `/commit` | 创建 git commit |
| `/pr` | 创建 Pull Request |
| `/simplify` | 简化/重构代码 |
| `/security-review` | 安全审查 |

### 4.4 自动化

| 命令 | 作用 |
|---|---|
| `/loop` | 定时循环执行命令 |
| `/fast` | 切换快速模式 |

---

## 五、常见 Skill 实战

### 5.1 /review — 代码审查

```
用法：/review
或：  "review 这个 PR"

流程：
1. Skill 加载代码审查专用 prompt
2. 读取 git diff
3. 启动多个审查子代理（安全/性能/测试/风格）
4. 汇总发现 + 合并去重
5. 输出审查报告
```

### 5.2 /commit — 智能提交

```
用法：/commit
或：  "commit 我的改动"

流程：
1. 检查 git status + git diff
2. 分析改动内容
3. 根据仓库 commit 风格生成 message
4. 创建 commit（Co-Authored-By 签名）
```

### 5.3 /init — 项目初始化

```
用法：/init

流程：
1. 扫描项目结构
2. 识别技术栈
3. 提取编码规范
4. 生成 CLAUDE.md（含技术栈/结构/规范/命令）
```

### 5.4 /loop — 循环任务

```
用法：/loop 5m /check-ci
     /loop（自定节奏）

功能：定时重复执行指定命令
场景：监控 CI、定期检查部署状态
```

---

## 六、核心要点

1. **Skill 和 Slash Command 的关系？** Skill 是功能包，/command 是触发 Skill 的方式
2. **Skill 怎么加载？** 用户显式触发 /command 或 AI 根据任务自动匹配
3. **Skill 的优势？** 预设工作流和领域知识，避免每次从零开始
4. **能自定义 Skill 吗？** 可以，通过 skill-creator skill 创建
5. **最常用的 Skill？** /review（审查）、/commit（提交）、/init（初始化）、/plan（规划）

---

## 七、极简总结

```
Skill = 预设了工作流的 AI 能力包
命令 = /review /commit /pr /init /plan /loop /fast
加载 = 用户打 /command 或 AI 自动匹配
原理 = Skill 注入自己的 System Prompt + 接管对话
内置 = 30+ Skill 覆盖开发/文档/设计/运维
```
