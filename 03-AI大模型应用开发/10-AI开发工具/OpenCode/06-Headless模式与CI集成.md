# 06 - Headless 模式与 CI 集成

> 🎯 OpenCode 的无头模式让 AI 编程 Agent 直接嵌入流水线 — GitHub Actions 自动 Review PR、Cron 定时任务、Webhook 触发。无人类确认环境下的安全强制是关键

---

## 目录

1. [Headless 三种运行方式](#1-headless-三种运行方式)
2. [opencode run 常用参数](#2-opencode-run-常用参数)
3. [GitHub Agent 集成](#3-github-agent-集成)
4. [CI 流水线实战](#4-ci-流水线实战)
5. [无头模式安全要点](#5-无头模式安全要点)

---

## 1. Headless 三种运行方式

```bash
# ① 一次性提示（one-shot）— 最常用
opencode run "summarize the last 5 commits"
opencode run --format json "list every TODO" > todos.json
opencode run --agent plan --model anthropic/claude-haiku-4-5 "audit src/"
opencode run "fix the failing test in UserServiceTest"

# ② 无头服务器（API / Web UI / 远程连接）
opencode serve --port 4096 --hostname 0.0.0.0    # 无头 API 服务
opencode web --port 4096                          # Web UI

# ③ GitHub Agent（PR/Issue 自动化）
opencode github install     # 添加 workflow 文件到仓库
opencode pr 123             # 检出 PR 并开会话
```

---

## 2. opencode run 常用参数

| 参数 | 说明 | 示例 |
|------|------|------|
| `--continue` | 恢复上一个会话 | `opencode run --continue "继续修复"` |
| `--session <id>` | 恢复指定会话 | `opencode run --session abc123 "接着做"` |
| `--fork` | 分支会话（不修改原会话） | `opencode run --fork "换个方案"` |
| `--share` | 发布分享链接 | `opencode run --share "生成文档"` |
| `--format json` | 机器可读输出 | `opencode run --format json "列出 TODO"` |
| `--file <path>` | 附加文件 | `opencode run --file src/main.java "review"` |
| `--agent <name>` | 指定 Agent | `opencode run --agent reviewer "审查变更"` |
| `--model <model>` | 指定模型 | `opencode run --model deepseek/deepseek-chat "..."` |
| `--auto` | 自动批准（**仅限 CI**） | `opencode run --auto "修复构建错误"` |
| `--mini` | 轻量模式 | `opencode run --mini "快速修复"` |
| `--replay` | 交互式回放（v1.16+） | `opencode run --replay` |

---

## 3. GitHub Agent 集成

```bash
# ① 安装 GitHub Agent（添加 workflow 文件）
opencode github install

# ② 触发 PR 审查
opencode pr 123       # 检出 PR → 打开会话 → AI 审查
```

```yaml
# .github/workflows/opencode-review.yml（github install 自动生成）
name: OpenCode PR Review
on:
  pull_request:
    types: [opened, synchronize]

jobs:
  review:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      pull-requests: write
    steps:
      - uses: actions/checkout@v4
      - name: Run OpenCode Review
        run: |
          opencode run --model deepseek/deepseek-chat \
            "审查这个 PR 的变更，输出代码审查报告" \
            --format json > review.json
      - name: Post Review Comment
        run: |
          gh pr comment ${{ github.event.pull_request.number }} \
            --body-file review.md
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## 4. CI 流水线实战

### 4.1 GitHub Actions：自动修复构建错误

```yaml
name: Auto-Fix Build
on:
  workflow_dispatch:
  schedule:
    - cron: "0 3 * * *"          # 每天凌晨 3 点

jobs:
  auto-fix:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - name: Setup Node & Go
        uses: actions/setup-node@v4
        with: { node-version: 22 }

      - name: Run OpenCode Auto-Fix
        run: |
          opencode run --auto --timeout 300 \
            "运行测试，如果失败请修复问题并重新运行，直到测试通过" \
          || echo "AI 无法自动修复，需要人工介入"

      - name: Create PR if changed
        if: success()
        run: |
          git diff --exit-code || git commit -am "fix: AI auto-fix" && git push
```

### 4.2 定时任务：每日代码健康检查

```bash
# Cron 定时执行（Linux）
0 9 * * * cd /path/to/project && opencode run --agent reviewer \
  --model deepseek/deepseek-chat \
  "分析最近的代码变更，输出潜在问题报告" >> ~/opencode-reports/$(date +%Y%m%d).md
```

### 4.3 完整 CI 集成方案

```text
CI 事件触发 → OpenCode 自主完成"思考→调用工具→读结果→再思考"循环：
├── PR 创建 → 自动 Review（github agent）
├── 构建失败 → 自动修复（--auto）
├── 每日 3 点 → 代码健康检查（Cron + reviewer agent）
├── Release 前 → 生成变更日志（changelog agent）
└── 依赖更新 → 自动验证兼容性（test agent）
```

---

## 5. 无头模式安全要点

| 安全要点 | 说明 |
|----------|------|
| **`--auto` 仅限 CI** | 自动批准工具操作 — 绝不在本地交互环境使用 |
| **超时必须设置** | LLM 可能死循环 → `--timeout 300`（5 分钟上限） |
| **安全在工具层强制** | 无人类确认 → 破坏性命令**物理上无法执行**（MCP 服务器分类只读/破坏性） |
| **权限最小化** | CI 用只读 Token、`edit: deny` 的 reviewer Agent |
| **Skills 比模型重要** | 把资深工程师的排查经验写成 markdown 是 ROI 最高的投资 |
| **配置与 CI 解耦** | Agent 配置独立仓库、运行时 clone，避免 CI 配置膨胀 |
| **提供变更上下文** | 给 Agent 最近变更的 diff 做关联分析，提升准确率 |

```json
// 无头模式的安全配置示例
{
  "permissions": {
    "edit": "allow",
    "bash": {
      "allow": ["git *", "npm test", "go test", "mvn test"],
      "deny": ["rm -rf", "sudo", "git push --force"]
    },
    "webfetch": "ask"
  }
}
```

---

> 🎯 **核心要点**：Headless 模式让 AI 编程 Agent 变成 CI 流水线中的"自动修复工" — **PR 自动 Review + 构建失败自动修复 + 每日健康检查**。三个安全铁律：**① `--auto` 只用于 CI ② 必须设置超时（--timeout）③ 安全在工具层强制（破坏性命令物理不可执行），而非仅靠提示词**。

**下一模块**：[07-生产实践与安全](07-生产实践与安全.md) / **返回总览**：[00-总览](00-OpenCode知识体系总览.md)
