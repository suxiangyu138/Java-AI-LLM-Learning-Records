# Claude Code Hooks 与自动化详解（实战版）

> **文档定位**：Claude Code CLI Hook 系统深度解析
> **核心问题**：如何让 Claude Code 在特定事件触发时自动执行脚本？

---

## 一、Hook 是什么

Hook（钩子）是 Claude Code 的**事件驱动自动化机制**。在工具调用前后、用户输入提交时等关键节点，自动执行你定义的脚本。

```
Claude Code 事件 → 匹配 Hook 规则 → 执行自定义脚本 → 返回结果/阻止操作
```

---

## 二、Hook 事件类型

| 事件 | 触发时机 | 用途 |
|---|---|---|
| `preToolUse` | 工具调用**之前** | 校验、确认、阻止危险操作 |
| `postToolUse` | 工具调用**成功后** | 通知、日志、副作用处理 |
| `preUserPromptSubmit` | 用户提交输入**前** | 输入预处理、上下文注入 |
| `postSessionStart` | 会话启动时 | 环境初始化 |
| `notification` | 收到通知时 | 自定义通知处理 |
| `stop` | Agent 停止时 | 清理、收尾 |
| `subagentStart` | 子代理启动时 | 子代理初始化 |
| `subagentStop` | 子代理停止时 | 子代理清理 |

---

## 三、Hook 配置结构

### 3.1 在 settings.json 中定义

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
        "command": "echo '文件已被修改' | terminal-notifier -title 'Claude Code'"
      }
    ],
    "preUserPromptSubmit": [
      {
        "matcher": "",
        "command": "cat CLAUDE.md"
      }
    ]
  }
}
```

### 3.2 Matcher（匹配器）

| Matcher 语法 | 含义 |
|---|---|
| `*` | 匹配所有工具 |
| `Bash*` | 匹配所有 Bash 调用 |
| `Bash(git push*)` | 匹配 git push 开头的命令 |
| `Write(*.java)` | 匹配写入 Java 文件 |
| `Read(*test*)` | 匹配读取含 test 的文件 |

---

## 四、实战 Hook 示例

### 4.1 preToolUse：阻止危险命令

```json
{
  "hooks": {
    "preToolUse": [
      {
        "matcher": "Bash(rm -rf *)",
        "command": "echo 'DANGER: 递归删除被阻止' && exit 1"
      },
      {
        "matcher": "Bash(git push --force *)",
        "command": "echo 'WARNING: force push 被阻止' && exit 1"
      }
    ]
  }
}
```

### 4.2 preToolUse：敏感文件保护

```json
{
  "hooks": {
    "preToolUse": [
      {
        "matcher": "Read(*.env*)",
        "command": "echo 'WARNING: 读取 .env 文件可能泄露密钥'"
      },
      {
        "matcher": "Write(*.env)",
        "command": "echo 'ERROR: 禁止写入 .env 文件' && exit 1"
      }
    ]
  }
}
```

### 4.3 postToolUse：自动格式化

```json
{
  "hooks": {
    "postToolUse": [
      {
        "matcher": "Write(*.java)",
        "command": "mvn spotless:apply"
      },
      {
        "matcher": "Write(*.tsx)",
        "command": "npx prettier --write ${CLAUDE_TOOL_FILE_PATH}"
      }
    ]
  }
}
```

### 4.4 preUserPromptSubmit：注入上下文

```json
{
  "hooks": {
    "preUserPromptSubmit": [
      {
        "matcher": "",
        "command": "echo '[系统时间: $(date)] [分支: $(git branch --show-current)]'"
      }
    ]
  }
}
```

### 4.5 postSessionStart：环境检查

```json
{
  "hooks": {
    "postSessionStart": [
      {
        "matcher": "",
        "command": "node .claude/hooks/check_env.js"
      }
    ]
  }
}
```

```javascript
// .claude/hooks/check_env.js
const { execSync } = require('child_process');

const checks = [
  { name: 'Node.js', cmd: 'node --version' },
  { name: 'Java', cmd: 'java --version' },
  { name: 'Maven', cmd: 'mvn --version' },
  { name: 'Docker', cmd: 'docker --version' },
];

checks.forEach(({ name, cmd }) => {
  try {
    const version = execSync(cmd, { encoding: 'utf8' }).split('\n')[0];
    console.log(`✅ ${name}: ${version}`);
  } catch {
    console.log(`❌ ${name}: 未安装或不可用`);
  }
});
```

---

## 五、Hook 环境变量

Hook 脚本运行时可以访问以下环境变量：

| 环境变量 | 说明 |
|---|---|
| `CLAUDE_TOOL_NAME` | 被调用的工具名 |
| `CLAUDE_TOOL_INPUT` | 工具调用参数（JSON） |
| `CLAUDE_TOOL_FILE_PATH` | 操作的文件路径（Write/Read/Edit） |
| `CLAUDE_SESSION_ID` | 当前会话 ID |
| `CLAUDE_PROJECT_DIR` | 项目根目录 |

```bash
#!/bin/bash
# preToolUse hook 示例
echo "工具: $CLAUDE_TOOL_NAME"
echo "参数: $CLAUDE_TOOL_INPUT"
echo "文件: $CLAUDE_TOOL_FILE_PATH"
```

---

## 六、Hook 返回值

| 退出码 | 含义 |
|---|---|
| `0` | 允许继续执行 |
| `非 0` | 阻止操作（preToolUse）/ 标记失败（postToolUse） |

Hook 的 stdout 内容会显示给用户，stderr 会记录到日志。

---

## 七、项目级 Hook 脚本

```
项目/
├── .claude/
│   ├── settings.json          # Hook 配置
│   └── hooks/
│       ├── pre_commit_check.sh
│       ├── auto_format.sh
│       └── check_env.sh
```

```json
// settings.json
{
  "hooks": {
    "preToolUse": [
      {
        "matcher": "Bash(git commit*)",
        "command": "bash .claude/hooks/pre_commit_check.sh"
      }
    ],
    "postToolUse": [
      {
        "matcher": "Edit(*.java)",
        "command": "bash .claude/hooks/auto_format.sh"
      }
    ]
  }
}
```

---

## 八、面试核心要点

1. **Hook 是什么？** 事件驱动的自动化，在工具调用前后等时刻自动执行脚本
2. **有几个事件类型？** preToolUse / postToolUse / preUserPromptSubmit 等
3. **怎么阻止危险操作？** preToolUse hook 中 exit 非 0 即可阻止
4. **Hook 能拿到什么信息？** 环境变量（工具名、参数、文件路径、会话ID）
5. **和 Git hook 什么关系？** 互补，Git hook 管 Git 操作，Claude Code hook 管 AI 工具行为

---

## 九、极简总结

```
Hook = 事件触发 → 执行脚本 → 返回结果
preToolUse = 工具调用前（校验、阻止）
postToolUse = 工具调用后（格式化、通知、日志）
matcher = 匹配规则（* 通配，支持工具名和参数）
exit 非 0 = 阻止操作
环境变量 = CLAUDE_TOOL_NAME / CLAUDE_TOOL_INPUT / CLAUDE_TOOL_FILE_PATH
```
