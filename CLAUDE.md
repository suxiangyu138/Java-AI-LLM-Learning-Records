# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Repository Purpose

Personal Java backend + AI learning knowledge base organized as a 5-layer pyramid. All content is **markdown technical documentation** (not runnable code). The primary task is converting raw Chinese learning notes into standard technical documentation format.

## Directory Structure (5-Layer Pyramid)

```
01-底层根基-Java核心底座/     # Foundation: Java core, JUC, JVM, CS fundamentals, DSA
02-中间件与微服务工程/        # Middleware: MySQL, Redis, MQ, ES, Spring, DevOps
03-AI大模型垂直专项/          # AI specialization: RAG, Agent, Embedding, LangChain4j, Spring AI
04-实战项目综合落地/          # Projects: 3 major capstone projects
05-综合输出-面试冲刺/          # Interview: question banks, hand-written code, resume, cheat sheets
06-课程思路体系搭建/          # Curriculum: 84+ course outlines from B站 organized by topic
```

## Common Tasks

### Converting Notes to Standard Markdown

The most frequent task. When asked to rewrite a file:
- Add `# Title` + `> 定位/一句话总结` blockquote at top
- Add numbered `## 目录` with anchor links
- Convert plain text lists to markdown tables (feature comparisons, pros/cons, API references)
- Use ` ```language ` code blocks with language tags
- Use `> ⚠️` for warnings, `> 💡` for tips, `> 🎯` for summaries
- Remove filler/redundant text; keep all technical code examples
- Preserve original file encoding (UTF-8 with Chinese characters)

### Processing Course Outlines (课程思路体系搭建/)

When converting B站 course syllabus `.txt` files to `.md`:
- Strip ALL metadata: UP主 name, followers, BV号, play count, 弹幕, 点赞, "一键三连" ads
- Keep ONLY: course title, chapter structure, lesson numbers and titles
- Format: `### Chapter Name` → `- \`000\` Lesson Title`
- Name files as `NN-EnglishName.md` (e.g., `01-JavaWeb从入门到精通.md`)

### File Naming Conventions

- One topic per file, keep granularity consistent
- Use Chinese for technical topic names, English abbreviations for well-known terms (JVM, Redis, Spring)
- Book outlines: `书名.md`
- Course outlines: `NN-CourseName.md`

## Documentation Standards

All markdown files should follow this structure:
```md
# Title
> One-line positioning summary

## 目录
1. [Section](#anchor)

---

## 1. Section
### 1.1 Subsection
| Header | Header |
|--------|--------|
```

- **Tables preferred** for comparisons, API references, feature lists
- **Code blocks** must specify language (\```java, \```yaml, \```xml, \```bash, \```text)
- **Blockquotes** for key insights: `> ⚠️` (warning), `> 💡` (tip), `> 🎯` (summary)

## Key Patterns

- This is a **documentation repo** — no build, test, or lint commands
- Files contain **Chinese technical content** with English code/API names
- `.txt` files are source materials to be converted to `.md`
- `06-课程思路体系搭建/` is a separate curriculum indexing system (84+ course syllabi)
- Existing `.md` files are the canonical format; `.txt` files in the same directory are source drafts
