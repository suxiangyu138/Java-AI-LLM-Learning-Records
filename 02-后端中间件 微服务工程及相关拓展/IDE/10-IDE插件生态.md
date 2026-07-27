# 10 - IDE 插件生态

> 🎯 好的插件 = 效率翻倍。IDEA/Cursor/VS Code 的插件生态是三大 IDE 的核心竞争力。本章推荐 Java + AI 开发的必备插件清单

---

## 目录

1. [IDEA 必备插件](#1-idea-必备插件)
2. [VS Code 必备插件](#2-vs-code-必备插件)
3. [AI 编程插件对比](#3-ai-编程插件对比)

---

## 1. IDEA 必备插件

| 插件 | 用途 | 推荐度 |
|------|------|:---:|
| **Lombok** | 消灭 getter/setter 样板代码 | ⭐⭐⭐⭐⭐ |
| **MyBatisX** | MyBatis XML ↔ Java 跳转 | ⭐⭐⭐⭐⭐ |
| **Maven Helper** | 依赖冲突分析 | ⭐⭐⭐⭐⭐ |
| **Alibaba Java Coding Guidelines** | 实时代码规范检查 | ⭐⭐⭐⭐ |
| **SonarLint** | 代码质量/安全扫描 | ⭐⭐⭐⭐ |
| **GsonFormat** | JSON → Java Bean 一键生成 | ⭐⭐⭐⭐ |
| **PlantUML** | 代码 → UML 图 | ⭐⭐⭐ |
| **Key Promoter X** | 统计操作 → 推荐快捷键 | ⭐⭐⭐ |
| **Rainbow Brackets** | 彩虹括号（嵌套一目了然） | ⭐⭐⭐⭐ |
| **GitToolBox** | Git 状态增强（行内 blame） | ⭐⭐⭐⭐ |
| **JPA Buddy** | JPA Entity 可视化编辑 | ⭐⭐⭐⭐ |

```text
AI 编程插件（IDEA）：
├── GitHub Copilot → 代码补全 + Chat
├── 通义灵码 → 阿里免费，中文友好
├── CodeGPT → 多模型切换
└── Codiumate → 测试生成 + 代码解释
```

## 2. VS Code 必备插件

| 插件 | 用途 | 推荐度 |
|------|------|:---:|
| **GitHub Copilot** | AI 代码补全 | ⭐⭐⭐⭐⭐ |
| **Cline** | AI Agent 自主编程 | ⭐⭐⭐⭐⭐ |
| **Continue** | 本地模型编程助手 | ⭐⭐⭐⭐ |
| **Extension Pack for Java** | Java 全家桶 | ⭐⭐⭐⭐⭐ |
| **Spring Boot Tools** | Spring 支持 | ⭐⭐⭐⭐⭐ |
| **Docker** | Docker 管理 | ⭐⭐⭐⭐⭐ |
| **Remote - SSH** | 远程开发 | ⭐⭐⭐⭐⭐ |
| **Prettier** | 代码格式化 | ⭐⭐⭐⭐ |
| **Thunder Client** | HTTP Client (轻量 Postman) | ⭐⭐⭐⭐ |
| **GitLens** | Git 增强 | ⭐⭐⭐⭐⭐ |
| **Error Lens** | 行内显示错误 | ⭐⭐⭐⭐ |

## 3. AI 编程插件对比

| 插件 | IDE | 价格 | 模型 | 特色 |
|------|-----|:---:|------|------|
| **GitHub Copilot** | IDEA/VS Code | $10/月 | GPT-4o | 最成熟，生态最广 |
| **Cline** | VS Code | 免费(自带Key) | Claude/GPT/本地 | Agent 模式，开源 |
| **Continue** | IDEA/VS Code | 免费 | 本地+云端 | 数据不出本机 |
| **通义灵码** | IDEA/VS Code | **免费** | 通义千问 | 中文最佳 |
| **Codeium** | 全平台 | 免费 | 自研 | 免费版 Copilot |
| **Cursor** | 独立 IDE | $20/月 | GPT/Claude | Agent+Composer 最佳 |

```text
选型建议：
├── 个人开发 → Codeium(免费) 或 通义灵码(中文)
├── 专业开发 → GitHub Copilot($10/月)
├── Agent 开发 → Cursor($20/月) 或 Cline(免费)
├── 企业安全 → Continue + 本地模型
└── 组合推荐 → Copilot(补全) + Cursor(Agent) = 最强组合
```

## 核心要点回顾

- IDEA 必备：Lombok + MyBatisX + Maven Helper + SonarLint
- VS Code 必备：Java Extension Pack + Docker + Remote SSH
- AI 编程：Copilot(补全) + Cursor(Agent) 是 2025 最佳组合
- 中文开发者：通义灵码免费且中文友好
- GitLens + Error Lens = VS Code 必装（提升明显）

## 参考资料

1. JetBrains Marketplace — plugins.jetbrains.com
2. VS Code Marketplace — marketplace.visualstudio.com
