# Claude Code Hooks 与自动化详解

> **核心摘要**：Hook（钩子）是 Claude Code 的事件驱动自动化机制。在工具调用前后、用户输入提交时等关键节点自动执行脚本，实现校验、通知、格式化等自动化操作。

---

## 1. Hook 是什么

Hook 是 Claude Code 的事件驱动自动化机制。在工具调用前后等关键节点，自动执行你定义的脚本。

```
Claude Code 事件 → 匹配 Hook 规则 → 执行自定义脚本 → 返回结果/阻止操作
```

## 2. Hook 事件类型

| 事件 | 触发时机 | 用途 |
|------|---------|------|
| `preToolUse` | 工具调用之前 | 校验、确认、阻止危险操作 |
| `postToolUse` | 工具调用成功后 | 通知、日志、副作用处理 |
| `preUserPromptSubmit` | 用户提交输入前 | 输入预处理、上下文注入 |
| `postSessionStart` | 会话启动时 | 环境初始化 |
| `stop` | Agent 停止时 | 清理、收尾 |

## 3. Hook 配置结构

```json
{
  "hooks": {
    "preToolUse": [
      {
        "matcher": "Bash(git push*)",
        "command": "python3 .claude/hooks/pre_push_check.py",
        "timeout": 10000
      }
    ],
    "postToolUse": [
      {
        "matcher": "Write*",
        "command": "echo '文件已被修改'"
      }
    ]
  }
}
```

### Matcher 匹配器语法

| Matcher | 含义 |
|---------|------|
| `*` | 匹配所有工具 |
| `Bash*` | 匹配所有 Bash 调用 |
| `Bash(git push*)` | 匹配 git push 开头的命令 |
| `Write(*.java)` | 匹配写入 Java 文件 |

## 4. 实战示例

### 阻止危险命令

```json
{
  "hooks": {
    "preToolUse": [
      {
        "matcher": "Bash(rm -rf *)",
        "command": "echo 'DANGER: 递归删除被阻止' && exit 1"
      }
    ]
  }
}
```

### 自动格式化代码

```json
{
  "hooks": {
    "postToolUse": [
      {
        "matcher": "Write(*.java)",
        "command": "mvn spotless:apply"
      }
    ]
  }
}
```

### 注入上下文

```json
{
  "hooks": {
    "preUserPromptSubmit": [
      {
        "matcher": "",
        "command": "echo '[时间: $(date)] [分支: $(git branch --show-current)]'"
      }
    ]
  }
}
```

## 5. Hook 环境变量

| 环境变量 | 说明 |
|---------|------|
| `CLAUDE_TOOL_NAME` | 被调用的工具名 |
| `CLAUDE_TOOL_INPUT` | 工具调用参数（JSON） |
| `CLAUDE_TOOL_FILE_PATH` | 操作的文件路径 |
| `CLAUDE_SESSION_ID` | 当前会话 ID |
| `CLAUDE_PROJECT_DIR` | 项目根目录 |

## 6. Hook 返回值

| 退出码 | 含义 |
|--------|------|
| `0` | 允许继续执行 |
| 非 `0` | 阻止操作（preToolUse）/ 标记失败（postToolUse） |

## 核心要点回顾

- Hook = 事件触发 → 执行脚本 → 返回结果
- preToolUse = 工具调用前（校验、阻止）
- postToolUse = 工具调用后（格式化、通知、日志）
- matcher 语法：`*` 通配，支持工具名和参数匹配
- exit 非 0 = 阻止操作
- 可用环境变量获取工具名、参数、文件路径等信息
- Hook 与 Git hook 互补：Git hook 管 Git 操作，Claude Code hook 管 AI 工具行为

## 参考资料

1. Anthropic 官方文档 - Claude Code Hooks
2. Claude Code 配置参考 - Hook Matcher 语法
