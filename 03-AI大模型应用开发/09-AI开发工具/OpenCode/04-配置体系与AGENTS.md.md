# 04 - 配置体系与 AGENTS.md

> 🎯 OpenCode 的配置灵魂是 AGENTS.md — 每次会话自动读取的"项目行为准则"。配合 opencode.json 的全局/项目双层配置，构成完整的规则体系

---

## 目录

1. [配置体系总览](#1-配置体系总览)
2. [AGENTS.md：项目行为准则](#2-agentsmd项目行为准则)
3. [opencode.json 详解](#3-opencodejson-详解)
4. [全局 vs 项目配置](#4-全局-vs-项目配置)
5. [从 Claude Code 迁移](#5-从-claude-code-迁移)

---

## 1. 配置体系总览

```text
OpenCode 配置体系（两层）：

① 全局配置（所有项目生效）
   ├── ~/.config/opencode/opencode.json    （Linux/macOS）
   ├── %APPDATA%\opencode\config.json      （Windows）
   ├── ~/.config/opencode/agents/*.md      （全局 Agent）
   ├── ~/.config/opencode/skills/*/SKILL.md（全局 Skills）
   └── ~/.config/opencode/plugins/*.ts     （全局插件）

② 项目配置（当前项目生效，建议提交 Git）
   ├── AGENTS.md                            （★ 项目行为准则）
   ├── .opencode/opencode.json              （项目级配置）
   ├── .opencode/agents/*.md                （项目 Agent）
   ├── .opencode/skills/*/SKILL.md          （项目 Skills）
   ├── .opencode/plugins/*.ts               （项目插件）
   └── .opencode/commands/*.md              （项目命令）

优先级：项目配置 > 全局配置
```

---

## 2. AGENTS.md：项目行为准则

### 2.1 生成与作用

```bash
# 在项目目录运行 /init → 自动生成 AGENTS.md
opencode            # 进入 TUI
/init               # 自动分析项目结构 → 生成 AGENTS.md
```

**AGENTS.md 是每次会话自动读取的"house rules"** — 告诉 AI 这个项目的规则、约定、禁止事项。应提交到 Git 供团队共享。

### 2.2 完整示例

```markdown
# AGENTS.md

## 项目信息
- 项目：用户订单系统（Spring Boot 4 + Java 21）
- 构建：./mvnw clean package
- 测试：./mvnw test
- 运行：./mvnw spring-boot:run

## 代码约定
- 使用 4 空格缩进（非 2 空格）
- 类名使用 PascalCase，方法/变量使用 camelCase
- 所有 API 必须返回统一响应格式：Result<T>
- 使用 MyBatis-Plus 而非 JPA
- 配置项必须放在 application.yml，禁止硬编码

## 目录规范
- src/main/java/com/example/order/
  - controller/   # REST 接口层
  - service/      # 业务逻辑层
  - mapper/       # MyBatis Mapper
  - model/        # 实体/DTO

## 禁止事项
- ❌ 不要修改 src/main/resources/mapper/ 下的 XML 映射文件
- ❌ 不要直接 push 到 main 分支
- ❌ 不要引入新的第三方依赖（需先讨论）
- ❌ 不要删除历史测试用例

## 测试要求
- 新增功能必须配套单元测试
- 测试类命名：XxxServiceTest
- 测试覆盖率目标：service 层 ≥ 80%
```

### 2.3 附加规则文件

```json
// opencode.json 中可添加额外规则文件
{
  "instructions": [
    ".opencode/rules/security.md",   // 安全规则
    ".opencode/rules/api.md"         // API 规范
  ]
}
```

---

## 3. opencode.json 详解

```json
{
  // ===== 模型配置 =====
  "model": "anthropic/claude-sonnet-4-5",
  "models": {
    "reviewer": "anthropic/claude-haiku-4-5",
    "cheap": "deepseek/deepseek-chat"
  },

  // ===== 指令/规则 =====
  "instructions": [".opencode/rules/extra.md"],

  // ===== MCP 服务器 =====
  "mcp": {
    "target-system": {
      "type": "remote",
      "url": "http://localhost:8080/sse",
      "enabled": true
    },
    "context7": {
      "type": "local",
      "command": ["npx", "mcp-query-docs"],
      "enabled": true
    }
  },

  // ===== 插件 =====
  "plugin": [
    "@omagents/omagents",
    "./plugins/custom.ts"
  ],

  // ===== 工具权限（安全）=====
  "permissions": {
    "edit": "allow",          // allow | ask | deny
    "bash": "ask",            // ★ bash 默认 ask 更安全
    "webfetch": "ask"
  },

  // ===== 其他 =====
  "theme": "dark",
  "vim": true,                // Vim 键位
  "autoupdate": true,
  "experimental": {}
}
```

---

## 4. 全局 vs 项目配置

| 配置项 | 全局（~/.config/opencode/） | 项目（.opencode/） |
|--------|:---:|:---:|
| opencode.json | ✅ | ✅（覆盖全局） |
| AGENTS.md | ❌（无） | ✅（项目特有） |
| agents/ | ✅ | ✅ |
| skills/ | ✅ | ✅ |
| plugins/ | ✅ | ✅ |
| commands/ | ✅ | ✅ |
| 建议放什么 | 通用模型/通用 Skills/通用 Agent | 项目规则/项目工具/项目专用 Agent |

**最佳实践：**
```text
全局配置（个人）：
  → 默认模型（sonnet）
  → 常用 Skills（deep-research、refactor）
  → 常用插件（omagents）

项目配置（团队）：
  → AGENTS.md（项目规范）
  → 项目专属 Agent（reviewer 用项目知识）
  → 项目专属 MCP（内部系统）
```

---

## 5. 从 Claude Code 迁移

```text
AGENTS.md 兼容：
  ├── 无 AGENTS.md 时 → OpenCode 回退读取 CLAUDE.md
  └── 可用环境变量关闭：OPENCODE_DISABLE_CLAUDE_CODE=1

Skills 兼容：
  ├── OpenCode 读取 ~/.claude/skills/（Claude Code 的 Skills 直接用）
  └── 也读取 ~/.agents/skills/（Codex + OpenCode 共享路径）

桥接插件：
  └── @sjawhar/opencode-claude-bridge
      → 把 Claude Code 的 agents/commands/skills/MCP 全部桥接进 OpenCode

迁移步骤：
  ① 保留 AGENTS.md（OpenCode 自动兼容 CLAUDE.md 命名也可）
  ② 保留 ~/.claude/skills/（OpenCode 自动读取）
  ③ 修改 opencode.json 的模型（换成非 Claude 模型解锁中立性）
  ④ 可选：安装 claude-bridge 插件过渡
```

---

> 🎯 **核心要点**：配置体系一句话 — **"AGENTS.md 管规则、opencode.json 管行为"**。AGENTS.md 是最重要的配置（构建命令/代码约定/禁止事项/测试要求），团队共享必须提交 Git。从 Claude Code 迁移几乎零成本：CLAUDE.md 自动兼容、~/.claude/skills 自动读取。

**下一模块**：[05-五大扩展点详解](05-五大扩展点详解.md) / **返回总览**：[00-总览](00-OpenCode知识体系总览.md)
