# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Repository Purpose

Personal Java backend + AI learning knowledge base. **1,460+ markdown technical documents** organized as a **6-layer pyramid**. All content is documentation (not runnable code). The primary task is creating and maintaining high-quality modular knowledge systems.

---

## Directory Structure (6-Layer Pyramid)

```
01-底层根基-Java核心底座/               # Java core, JUC, JVM, CS fundamentals, Vim
02-后端中间件 微服务工程及相关拓展/     # Middleware: MySQL, Redis, MQ, ES, Spring, DevOps, tool systems
03-AI大模型应用开发/                    # AI: DeepSeek, RAG, Agent, Embedding, LangChain4j, Spring AI
04-实战项目综合落地/                    # Projects: FlavorDash, SuGuangMall, LingShu
05-综合输出-面试冲刺/                    # Interview: 152 Q&A, hand-written code, resume, cheat sheets
06-课程思路体系搭建/                    # Curriculum: 84+ B站 course outlines organized by topic
```

### Active Knowledge Systems (2026.07)

| System | Location | Files | Focus |
|--------|---------|:---:|------|
| DeepSeek (V4 Pro) | `03-AI.../DeepSeek/` | 10 | Full evolution V2→V4 Pro |
| Java 后端名词剖析 | `02-.../Java后端开发名词剖析/` | 11 | 300+ concepts, 10 domains |
| Postman | `02-.../Postman/` | 8 | API testing full workflow |
| JMeter | `02-.../Jmeter/` | 8 | Performance testing: Sampler→CI/CD |
| Ubuntu | `02-.../Ubuntu/` | 7 | CLI→Java env→systemd→Shell |
| CentOS | `02-.../CentOS/` | 5 | dnf→SELinux→firewalld→Production |
| 项目全流程 | `02-.../项目从开始开发到上线全流程/` | 6 | Requirements→Launch→Ops |
| Vim | `01-.../Vim/` | 4 | Basic→Advanced→Plugins |
| 文件后缀名 | `01-.../不同文件的后缀名/` | 5 | 100+ file formats |

---

## Core Task: Creating Knowledge Systems

When user points to an empty directory (via `& 'path'`), build a comprehensive multi-file knowledge system.

### Overview File Requirements (`00-xxx总览.md`)

```md
# Title
> One-line positioning

## 📚 目录
1. [知识体系导图](#1)
2. [模块导航](#2)
3. [学习路线推荐](#3)
4. [核心概念速查](#4)

## 1. 知识体系导图
(ASCII tree showing module hierarchy)

## 2. 模块导航
| 序号 | 模块 | 核心内容 | 适合人群 |
## 3. 学习路线推荐
(2-3 paths: beginner/intermediate/advanced)
## 4. 核心概念速查
(quick reference table)
```

### Sub-module Requirements (`01-xxx.md`)

```md
# Title
> Positioning

## 📚 目录
1. [Section Name](#anchor)

## 1. Section Name
### 1.1 Subsection
(content with tables, code blocks)

> 🎯 **核心要点**：(key takeaway)

---

**下一模块**：[link] / **返回总览**：[link]
```

---

## Documentation Standards

- `> 一句话定位` blockquote under `# Title`
- **Tables preferred** for comparisons, API references, feature lists
- **Code blocks** MUST specify language: ` ```java ` ` ```yaml ` ` ```bash ` ` ```text ` ` ```json `
- Blockquotes: `> ⚠️` warning, `> 💡` tip, `> 🎯` summary
- Chinese content with English technical terms
- Anchor links in TOC must match section headers exactly
- Cross-references at file end: `**下一模块：**` or `**返回总览：**`

---

## File Naming

- `00-` prefix for overview files
- `01-`, `02-`... for sequential modules
- Chinese for topic names, English for well-known terms (JVM, Redis, Spring)
- Course outlines: `NN-EnglishName.md`

---

## Key Patterns

- **Documentation repo** — no build, test, or lint commands
- **Chinese technical content** with English code/API names
- `.txt` files are raw source materials to convert to `.md`
- `06-课程思路体系搭建/` is a curriculum indexing system (84+ course syllabi)
- Existing `.md` files are canonical; `.txt` files in same directory are source drafts
- When building new systems, prefer **fewer but richer files** over many thin ones
- When user says "太少了"/"不对", it means deepen the content or fix structure
