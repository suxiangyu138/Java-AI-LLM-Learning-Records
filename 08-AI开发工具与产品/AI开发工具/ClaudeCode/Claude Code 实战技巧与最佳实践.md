# Claude Code 实战技巧与最佳实践（实战版）

> **文档定位**：Claude Code CLI 高效使用指南
> **核心问题**：如何用 Claude Code 事半功倍？有哪些隐藏技巧？生产环境怎么用？

---

## 一、高效交互技巧

### 1.1 给 Claude Code 搭好"舞台"

```
✅ 好的做法：
  - 项目根目录放 CLAUDE.md（技术栈/规范/命令）
  - .gitignore 排除 build/ 和 node_modules/（减少噪音）
  - 目录结构清晰，命名规范

❌ 避免的做法：
  - 塞入无关文件导致上下文浪费
  - 项目无 README 或 CLAUDE.md
  - 文件和目录命名混乱
```

### 1.2 描述需求的高效方式

```
❌ 太模糊："帮我改一下登录功能"
✅ 好的："在 UserController 的 login 方法中，增加手机号验证码登录方式，
       复用现有的 JWT token 生成逻辑。验证码存在 Redis，key 格式为 sms:login:{phone}"

技巧：
1. 指定文件和位置（文件路径 + 方法名）
2. 明确边界（复用哪些、新增哪些）
3. 给出约束（性能要求、格式要求）
```

### 1.3 利用管道模式

```bash
# 分析错误日志
cat error.log | claude -p "分析这些错误，按原因分类，给出修复优先级"

# 审查 staged 改动
git diff --cached | claude -p "review 这些改动，重点关注安全问题"

# 生成 commit message
git diff --cached | claude -p "为这些改动生成一个 conventional commit 格式的 message"
```

---

## 二、项目配置最佳实践

### 2.1 CLAUDE.md 模板

```markdown
# 项目名：xxx

## 技术栈
- 后端：Spring Boot 3.2 + MyBatis Plus 3.5 + MySQL 8.0 + Redis 7.0
- 部署：Docker Compose

## 项目结构
```
src/main/java/com/xxx/
├── controller/    # REST API
├── service/       # 业务
├── mapper/        # 数据访问
└── entity/        # 实体
```

## 编码规范
- 使用阿里巴巴 Java 开发规范
- 类名大驼峰，方法名小驼峰
- SQL 字段下划线，Java 属性驼峰

## 常用命令
- mvn spring-boot:run    # 启动
- mvn test               # 测试
- docker-compose up -d   # 启动中间件

## 约束
- 不要提交 application-prod.yml
- 数据库密码从环境变量读取
- Redis key 前缀统一为 myapp:
```

### 2.2 .claude/settings.json 模板

```json
{
  "permissions": {
    "allow": [
      "Read(*)",
      "Grep(*)",
      "Glob(*)",
      "Bash(git *)",
      "Bash(ls *)",
      "Bash(mvn *)",
      "Bash(docker *)",
      "Bash(npm *)"
    ],
    "deny": [
      "Bash(rm -rf *)",
      "Bash(git push --force *)",
      "Bash(docker rm -f *)",
      "Bash(curl *)"
    ]
  },
  "hooks": {
    "preToolUse": [
      {
        "matcher": "Bash(git push*)",
        "command": "echo '⚠️ 推送需确认'"
      }
    ]
  }
}
```

---

## 三、上下文管理策略

### 3.1 何时用 /compact

```
触发条件：
  - 对话很长，Token 接近上限
  - 之前的步骤已完成，不再需要细节
  - AI 响应变慢或开始截断

/compact 做了什么：
  - 压缩旧的对话历史为摘要
  - 保留最近的完整上下文
  - 释放 Token 空间继续工作
```

### 3.2 何时用 Agent（子代理）

```
✅ 适合用 Agent：
  - 探索性搜索（Agent Explore）
  - 独立审查任务（Agent code-review）
  - 并行研究任务（多个 Agent 同时跑）
  - 需要隔离上下文的任务

❌ 直接用工具更好：
  - 已知文件路径的直接 Read
  - 精确搜索的直接 Grep
  - 简单操作不要过度使用 Agent
```

---

## 四、Git 工作流集成

### 4.1 日常开发流程

```bash
# 1. 开始开发前
git status                        # 确认当前状态

# 2. 让 Claude Code 实现功能
"在 src/service/UserService.java 中增加 findByPhone 方法"

# 3. 查看改动
git diff                          # Claude Code 自动执行

# 4. 创建提交
/commit                           # 自动生成规范 message

# 5. 推送
"push 到远程"
```

### 4.2 PR 工作流

```bash
# 1. 创建 PR
/pr                                # 自动生成标题和描述

# 2. 审查 PR
/review                            # 多层级代码审查

# 3. 处理审查意见
"修复 review 中指出的 NPE 问题"
```

---

## 五、多文件重构技巧

```
情景：将一个模块从 A 包迁移到 B 包，涉及 50+ 文件

1. "列出 src/.../oldpackage/ 下所有文件"
2. "把 OldService 移到 newpackage，并更新所有 import"
3. "搜索所有引用 OldService 的地方"（Grep）
4. "逐一更新这些引用"（批量 Edit）

关键：分步来，每步验证，不要一步到位
```

---

## 六、调试与排错

```
1. "这个报错是什么意思？"（贴错误日志）
2. "搜索项目中使用 @Deprecated 方法的地方"
3. "检查 SQL Mapper XML 中是否有语法错误"
4. "跑一下单元测试，告诉我哪些失败了"
5. "对比 master 分支，看看最近的改动引入了什么"
```

---

## 七、安全最佳实践

```json
// 必须 deny 的命令
{
  "permissions": {
    "deny": [
      "Bash(rm -rf *)",            // 递归删除
      "Bash(git push --force *)",  // 强制推送
      "Bash(git reset --hard *)",  // 强制重置
      "Bash(docker rm -f *)",     // 强制删除容器
      "Bash(curl *)",             // 外发请求
      "Bash(wget *)",             // 下载文件
      "Bash(chmod 777 *)"         // 宽松权限
    ]
  }
}
```

---

## 八、效率量化

```
使用 Claude Code 做一次完整的代码审查 + 修复：

人工：
  - 审查代码：30 分钟
  - 定位问题：15 分钟
  - 修复代码：20 分钟
  - 验证测试：15 分钟
  总计：80 分钟

Claude Code：
  - 运行 /review：2 分钟
  - 确认发现：5 分钟
  - 自动修复：5 分钟
  - 验证测试：5 分钟
  总计：17 分钟（节省 ~80%）
```

---

## 九、核心要点

1. **最好的实践？** 项目根目录有好的 CLAUDE.md + 描述需求时指定文件和方法
2. **什么时候用 Agent？** 探索性搜索、独立并行任务、需要上下文隔离
3. **什么时候 /compact？** 对话很长、Token 接近上限、AI 响应变慢
4. **必须 deny 的命令？** rm -rf、git push --force、chmod 777、curl
5. **Git 工作流？** /commit 提交 → /pr 创建 PR → /review 审查

---

## 十、极简总结

```
好 CLAUDE.md = Claude Code 效率翻倍
描述需求 = 指定文件 + 明确边界 + 给出约束
管道 = cat file | claude -p "分析"
权限 = allow 读+搜索+git，deny rm+force push
提效 = 审查/提交/PR 用 /command 一条龙
节省 = 代码审查 -80%，重构 -60%
```
