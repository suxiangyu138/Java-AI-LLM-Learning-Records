# 04 - Skills 技能系统与开发

> 🎯 OpenClaw 的独特创新："写文档 = 扩展 AI 能力"。Skill 不是代码插件，而是一个 `SKILL.md` 文件 — LLM 自己读取、理解、执行。ClawHub 已积累 4.4 万个 Skills，按需安装

---

## 目录

1. [Skill 的本质：不是代码，是知识](#1-skill-的本质不是代码是知识)
2. [SKILL.md 开发规范](#2-skillmd-开发规范)
3. [加载优先级与过滤机制](#3-加载优先级与过滤机制)
4. [ClawHub 社区生态](#4-clawhub-社区生态)
5. [自定义 Skill 实战](#5-自定义-skill-实战)
6. [安全与权限控制](#6-安全与权限控制)

---

## 1. Skill 的本质：不是代码，是知识

```text
传统插件/扩展：
  写代码 → 编译 → 加载 → 执行
  问题：需要编程能力、跨平台兼容、安全审计

OpenClaw Skill：
  写 Markdown 文档 → 放入 skills/ 目录 → Agent 自动发现
  原理：LLM 读取 SKILL.md → 理解指令 → 按步骤执行

Skill 摘要以 XML 格式注入 System Prompt：
<available_skills>
  <skill>
    <name>pdf-toolkit</name>
    <description>PDF 文件操作：合并、拆分、提取文本、压缩</description>
    <location>managed/pdf-toolkit/SKILL.md</location>
  </skill>
</available_skills>

Token 成本：约 97 字符/skill ≈ 24 tokens + 字段长度（可量化、可控）
每次只读一个 Skill → 防止上下文爆炸
```

---

## 2. SKILL.md 开发规范

```markdown
# Skill: pdf-toolkit

> PDF 文件操作工具包：合并、拆分、提取文本、压缩

## 触发条件
- 用户要求操作 PDF 文件
- 关键词：PDF、合并、拆分、提取、压缩

## 依赖
- 需要安装 `pdftk` 或 `qpdf`（首次使用时自动检查并提示安装）

## 工作流程

### 1. 合并 PDF
```bash
pdftk file1.pdf file2.pdf cat output merged.pdf
```

### 2. 拆分 PDF
```bash
pdftk input.pdf burst output page_%02d.pdf
```

### 3. 提取文本
```bash
pdftotext input.pdf output.txt
```

### 4. 压缩 PDF
```bash
gs -sDEVICE=pdfwrite -dPDFSETTINGS=/ebook -q -o compressed.pdf input.pdf
```

## 注意事项
- 操作前先备份原文件
- 大文件（>100MB）先提醒用户确认
- 密码保护的 PDF 需要用户提供密码
```

**Skill 结构规范：**

| 章节 | 必须 | 说明 |
|------|:---:|------|
| `# Skill: <name>` | ✅ | 技能名称 |
| `> 一句话描述` | ✅ | Tagline |
| `## 触发条件` | ✅ | LLM 据此判断是否启用此 Skill |
| `## 依赖` | ⬜ | 需要的系统工具/库，Agent 自动检查安装 |
| `## 工作流程` | ✅ | 分步骤的执行指令（LLM 按步骤执行） |
| `## 注意事项` | ⬜ | 安全提示、限制条件 |

---

## 3. 加载优先级与过滤机制

```text
加载优先级（低→高，高优先级覆盖同名 Skill）：

① openclaw-extra/        → 额外配置目录 + 插件
② openclaw-bundled/      → 内置 Skill（50+）
③ openclaw-managed/      → ~/.openclaw/skills（clawhub install）
④ agents-skills-personal/ → ~/.agents/skills（用户个人）
⑤ agents-skills-project/  → 项目级（团队共享）
⑥ openclaw-workspace/     → <workspace>/skills（★ 最高优先级）

过滤机制（shouldIncludeSkill）：
├── enabled 配置
├── 内置白名单
├── OS 兼容性
└── requires.bins / requires.env 运行时资格检查

限制：SkillsLimitsConfig 控制数量/字符/文件大小上限
```

---

## 4. ClawHub 社区生态

```bash
# Skills 市场交互
openclaw skills search "pdf"              # 搜索 Skill
openclaw skills install pdf-toolkit       # 安装
openclaw skills list                      # 列出已安装
openclaw skills update pdf-toolkit        # 更新
openclaw skills uninstall pdf-toolkit     # 卸载

# 内置 Skill 类别（50+）
├── 办公：pdf / excel / markdown / 格式转换
├── 浏览器：网页截图 / 表单填写 / 价格监控
├── 代码：git / docker / npm / 代码审查
├── 金融：股票查询 / 汇率转换 / 财务报表
├── 硬件：摄像头 / 麦克风 / 系统信息
└── 媒体：图片处理 / 视频压缩 / 音频转换
```

---

## 5. 自定义 Skill 实战

```bash
# 示例：创建一个"日报生成"Skill

# ① 创建 Skill 目录
mkdir -p ~/.openclaw/skills/daily-report

# ② 编写 SKILL.md（见上节模板）
cat > ~/.openclaw/skills/daily-report/SKILL.md << 'EOF'
# Skill: daily-report

> 根据当天的工作记录生成日报

## 触发条件
- 用户说"写日报""生成日报""今日总结"
- 每天 18:00 的 Cron 自动触发

## 工作流程
1. 读取 memory/$(date +%Y-%m-%d).md 中的今日记录
2. 按类别归纳：代码/会议/文档/杂项
3. 生成格式化的日报（Markdown 表格）
4. 发送到指定渠道（飞书/邮件/保存本地）
EOF

# ③ 无需重启 — OpenClaw 自动发现
# ④ 测试
openclaw agent "写今天的日报"
```

**自定义 Skill 的典型场景：**

| 场景 | Skill 内容 |
|------|-----------|
| 代码审查机器人 | `## 工作流程：1. git diff 获取变更 2. 逐文件审查 3. 生成 Review 报告` |
| 会议纪要生成 | `## 工作流程：1. 读取录音转文字文件 2. 提取关键决策 3. 生成纪要` |
| 自动部署 | `## 工作流程：1. 检查 CI 状态 2. docker build 3. 推送镜像 4. 通知团队` |

---

## 6. 安全与权限控制

```text
Skill 安全机制：

① 执行沙箱：Pi-embedded 端 "Cell Isolation"
   → 默认在独立 venv 中运行
   → 执行前快照环境变量、动态加载 .ocskill 文件

② Exec Approvals 审批：
   模式：deny / ask / allowlist / full
   敏感命令执行 → Gateway 推送 exec.approval.requested 事件 → Operator 审批

③ 第三方 Skill 审查：
   ⚠️ ClawHub 曾遭遇 "ClawHavoc" 供应链攻击
   安装前检查：是否执行 Shell 命令？是否访问网络？是否修改系统配置？
```

---

> 🎯 **核心要点**：Skill = **SKILL.md（知识文件）+ LLM 理解 + Pi-embedded 执行**。它不是代码插件，而是"写给 AI 看的操作手册"。开发一个 Skill 只需三步：创建目录 → 写 SKILL.md（触发条件 + 工作流程 + 注意事项）→ 放入 skills/ 目录。安装第三方 Skill 前必须审查其中的 Shell 命令和网络访问。

**下一模块**：[05-部署与配置实战](05-部署与配置实战.md) / **返回总览**：[00-总览](00-OpenClaw知识体系总览.md)
