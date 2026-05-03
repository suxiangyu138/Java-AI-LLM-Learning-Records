# Vibe Coding Context Management Techniques

> Making AI Truly Understand Your Project

Hello, I'm Yupi.

In the previous two articles, we discussed the core principles and conversational techniques of Vibe Coding. Today, we're diving into a more fundamental yet equally important topic — context management.

You might have encountered situations where, at the beginning of a conversation with AI, it appears intelligent and generates code that meets your requirements. However, after chatting for a while, it starts to "forget" — forgetting the tech stack you mentioned earlier, the design style of the project, or even starting to implement features using entirely different approaches.

This isn't because the AI has become less intelligent; it's a memory issue. Below, I'll teach you how to apply **context engineering** to equip AI with a reliable "memory patch."

## 1. What is Context Engineering?

Before diving into specific methods, we need to understand what context is.

### Context is AI's Working Memory

Imagine collaborating with a new colleague on a project. If you have to explain the project's purpose, the tech stack, and the design guidelines from scratch every time, efficiency would plummet. However, if you have a shared project document that the colleague can quickly review, collaboration becomes much smoother.

Context is this "project document." It contains all the background information AI needs to know:

- What your project is about
- The tech stack being used
- Design guidelines
- Features already implemented
- Current tasks

With this information, AI can provide accurate and consistent answers.

### The Importance of Context

Many people focus on crafting good prompts, but **context might be more important than prompts**.

A good prompt helps AI understand your current needs, but good context helps AI understand your entire project. The former is a "point," while the latter is a "plane."

For example, if you simply say, "Help me write a button," AI might use plain HTML or React, and the style would be its own decision.

But if you provide complete context — "The project uses React, Tailwind CSS, with a minimalist modern design style and a primary color of blue" — AI can generate a button that perfectly aligns with your project's style. This is the power of context.

### The Three Levels of Context

Context can be divided into three levels:

1. **Project-level context**: Basic information about the entire project, such as the tech stack, design guidelines, and directory structure.

2. **Feature-level context**: Information about the feature currently being developed, such as its purpose and dependencies.

3. **Conversation-level context**: Temporary information from the current conversation, such as previously discussed issues or generated code snippets.

![](https://pic.yupi.icu/1/contextlevel%E5%A4%A7.jpeg)

Managing these three levels of context ensures that AI stays "in the zone."

## 2. AI's Short-Term Memory

Let's start with the basics — how to manage AI's short-term memory.

### What is a Context Window?

AI has a context window, which can be thought of as its short-term memory capacity. This window is limited, typically ranging from a few thousand to tens of thousands of tokens (roughly equivalent to a few thousand to tens of thousands of words).

Each message in your conversation occupies space in this window. The longer the conversation, the fuller the window becomes. When the window is full, earlier parts of the conversation are "forgotten."

This is why AI seems to forget — it's not that it truly forgets, but the earlier information has been pushed out of the window.

### One Conversation, One Task

The simplest way to manage this is: **one conversation, one task.**

Don't discuss login functionality, payment functionality, and performance optimization all in one conversation. This will confuse the context and make it harder for AI to keep track.

The correct approach is:

- Start a new conversation for login functionality
- Once completed and tested, start a new conversation for payment functionality
- If performance issues arise, start a new conversation specifically for optimization

Each conversation focuses on one task, keeping the context clear.

Of course, if the tasks are simple, it's fine to handle them in one conversation — flexibility is key.

### Regularly Compress Context

If a task requires a long conversation, you can periodically compress the context.

The approach is: halfway through the conversation, ask AI to summarize the progress so far.

Some AI coding tools have built-in commands for summarizing context, which you can use directly:

![](https://pic.yupi.icu/1/image-20260104180238760.png)

Alternatively, you can manually input a prompt to summarize:

```markdown
Please summarize what we've done so far, including:
1) Features completed
2) Technical solutions used
3) Remaining issues to resolve
```

You can then use this summary to start a new conversation and continue your work. This effectively compresses the long conversation into a concise summary.

### Leverage Recaps

At the start of a new conversation, ask AI to briefly recap previous content.

For example:

```markdown
We previously created a login form using React Hook Form and Zod validation. Now, I want to redirect to the homepage after successful login.
```

This helps AI quickly recall previous work and provide coherent answers.

## 3. AI's Long-Term Memory

Beyond short-term memory in conversations, you also need to establish long-term memory for your project. A typical approach is **providing project documentation**.

### README.md: The Project's ID

`README.md` is the most important document for a project. It should include:

1. Project overview: What the project does and the problem it solves

2. Tech stack: Technologies, frameworks, and libraries used

3. Directory structure: Purpose of major files and folders

4. Development guidelines: Code style, naming conventions, etc.

5. How to run: Commands for installing dependencies and starting the project

A good `README.md` should allow anyone (including AI) to quickly understand the project's basics.

For example:

````markdown
# My Blog System

A minimalist personal blog system supporting Markdown writing and code highlighting.

## Tech Stack

- Frontend: Next.js 14 (App Router) + TypeScript + Tailwind CSS
- Backend: Supabase (PostgreSQL + Auth)
- Deployment: Vercel

## Directory Structure

- `/app` - Next.js pages and routes
- `/components` - Reusable components
- `/lib` - Utility functions and configurations
- `/public` - Static resources

## Development Guidelines

- Use functional components, not class components
- All components must have TypeScript types
- Styles use Tailwind CSS, no custom CSS
- API calls use functions in `/lib/api.ts`

## How to Run

```bash
npm install
npm run dev
```
````

Yupi's open-source projects' `README.md` files generally follow this structured format, such as the [AI Zero-Code App Generator Project](https://github.com/liyupi/yu-ai-code-mother), for reference.

At the start of each new conversation, paste the contents of `README.md` to AI, allowing it to quickly understand your project.

### TODO.md: The Project's Task List

`TODO.md` records the project's to-do items and progress. It should include:

1. Completed features: What has been done

2. Features in progress: What is currently being worked on

3. Features to be developed: What is next

4. Known issues: Bugs or areas needing optimization

For example:

```markdown
# Development Progress

## Completed ✅

- [x] User registration and login
- [x] Article list page
- [x] Article detail page
- [x] Markdown rendering

## In Progress 🚧

- [ ] Article editing functionality
  - [x] Editor interface
  - [ ] Save draft
  - [ ] Publish article

## To Do 📋

- [ ] Comment functionality
- [ ] Search functionality
- [ ] Tag system

## Known Issues 🐛

- Mobile navigation bar displays incorrectly on some devices
- Code highlighting lacks contrast in dark mode
```

`TODO.md` helps both you and AI clearly understand the project's progress, avoiding redundant work or missed features.

### Update Documentation Promptly

The biggest issue with documentation is that it becomes outdated. Therefore, every time you complete a feature or make significant changes, promptly update `README.md` and `TODO.md`.

You can ask AI to help with updates:

```markdown
We just completed the article editing feature. Please update TODO.md to mark "Article editing functionality" as completed.
```

AI naturally understands the importance of this, so it might automatically generate these documents for us when we generate code.

## 4. Context Strategies for AI Coding Tools

Different AI tools handle context differently, so it's important to understand their characteristics.

### Cursor's .cursorrules

Cursor supports creating a `.cursorrules` file in the project root as a system prompt.

You can write in this file:

```
This is a Next.js blog project.

Tech stack:
- Next.js 16 (App Router)
- TypeScript
- Tailwind CSS
- Supabase

Code guidelines:
- Use functional components
- All components must have TypeScript type definitions
- Styles use only Tailwind CSS
- Avoid using the `any` type

Design style:
- Minimalist, modern
- Primary color: #3B82F6 (blue)
- Border radius: 8px
- Shadows: subtle

Always follow these guidelines.
```

This way, Cursor will automatically reference these rules when generating code.

💡 Note: As AI coding tools evolve, these rules may change, so it's best to consult the [official documentation](https://cursor.com/cn/docs/context/rules).

![](https://pic.yupi.icu/1/image-20260104181209687.png)

### Claude Code's CLAUDE.md

Claude Code reads the `CLAUDE.md` file in the project root as context.

You can include more detailed information in this file:

```markdown
# Project Context

## Project Overview
A personal blog system supporting Markdown writing.

## Tech Stack
- Next.js 16 + TypeScript + Tailwind CSS
- Supabase (database + authentication)

## Key Decisions
1. Why Supabase: Simple, sufficient free tier, built-in authentication
2. Why App Router: It's the future direction of Next.js
3. Why not Redux: The project is simple; React Context is sufficient

## Known Issues
- Mobile navigation bar needs optimization
- Code highlighting theme needs adjustment

## Next Steps
- Implement comment functionality
- Add search
```

This file serves as a project manual for AI.

### General Strategy: Context Folder

If your tool doesn't support specific context files, create a `/docs` folder and place all documentation inside:

```
/docs
  - README.md (project overview)
  - TECH_STACK.md (detailed tech stack)
  - DESIGN.md (design guidelines)
  - API.md (API documentation)
  - TODO.md (task list)
```

At the start of each new conversation, paste the relevant document content to AI.

## 5. Fixing Context Breaks

Even with good context management, AI might still "lose track." Here's how to fix it.

### Recognizing Context Breaks

Context breaks usually manifest as:

- AI suddenly uses a different tech stack (e.g., you're using React, but it generates Vue code)
- AI forgets previously discussed design plans
- AI-generated code style differs from earlier work
- AI repeats questions you've already answered

When you notice these signs, fix them promptly.

### Fix Method 1: Re-provide Context

The simplest method is to re-provide the context.

```markdown
Wait, our project uses React and TypeScript, not Vue.
Here's our tech stack: [paste the tech stack section from README.md]. Please regenerate the code using the correct tech stack.
```

### Fix Method 2: Reference Previous Content

If AI forgets previously discussed content, reference it.

```markdown
Remember we decided to use Context API for state management? Please continue with this approach; don't switch to Redux.
```

### Fix Method 3: Start a New Conversation

If the context is too chaotic to fix (e.g., repeatedly fixing the same bug without success), the best approach is to start a new conversation.

In the new conversation, provide the complete context and continue working. Although this means starting over, it ensures the quality of subsequent conversations.

### Fix Method 4: Back on Track Prompt

Sometimes, you can use a clear prompt to get AI back on track.

```markdown
Please pause. Our current goal is to implement login functionality using React Hook Form and Supabase Auth.
Please confirm you understand this goal, and then we'll proceed.
```

This gives AI a chance to reset. However, based on Yupi's testing, this prompt doesn't always work.

## 6. Best Practices for Context Management

Based on my experience and community insights, here are some best practices for context management.

### 1. Establish Documentation at Project Start

Don't wait until halfway through the project to start writing documentation. Create `README.md` and `TODO.md` from day one and keep them updated.

This not only helps AI but also helps you clarify your thoughts.

### 2. Use Tools' Native Context Mechanisms

If your tool supports specific context files (e.g., `.cursorrules`), prioritize using these mechanisms — they are the most efficient.

### 3. Keep Context Concise

More context isn't always better. Too much information can confuse AI.

Provide only the most important and relevant information. If a piece of information isn't useful for the current task, don't include it — it wastes tokens.

### Practice 4: Use Hierarchical Structures

Organize context into different levels and files rather than cramming everything into one file.

For example:
- `README.md` for project overview and basic information
- `TECH_STACK.md` for detailed tech stack explanations
- `DESIGN.md` for design guidelines
- Each feature module has its own documentation

### Practice 5: Regularly Review and Update

Every week or after completing a major feature, review your documentation to check for outdated content and update it promptly.

You can ask AI to help with this:

```markdown
Please review my README.md and check for inconsistencies with the current code.
```

### 6. Enhance Code Context with Comments

Add meaningful comments to your code, explaining "why" rather than just "what."

❌ Bad comment:

```typescript
// Get user data
const user = await getUser(id);
```

✅ Good comment:

```typescript
// Fetch user data from Supabase
// Note: Sensitive information (e.g., passwords) is excluded; only public fields are returned
const user = await getUser(id);
```

Good comments help AI understand the intent behind the code, enabling better decision-making during modifications.

## 7. Practical Example: Building a Complete Context System

Let's use a real-world example to demonstrate how to build a complete context system for a project.

Suppose you're building an online note-taking app.

### Step 1: Create README.md

```markdown
# Online Note App

A minimalist online note-taking app supporting Markdown editing and real-time saving.

## Tech Stack

- Frontend: React 18 + TypeScript + Vite
- UI Library: Tailwind CSS + Headless UI
- Editor: CodeMirror 6
- Backend: Supabase (PostgreSQL + Realtime)
- Deployment: Vercel

## Core Features

1. User registration and login
2. Create, edit, delete notes
3. Markdown live preview
4. Auto-save notes
5. Note search

## Directory Structure

- `/src/components` - React components
- `/src/pages` - Page components
- `/src/lib` - Utility functions and API
- `/src/hooks` - Custom Hooks
- `/src/types` - TypeScript type definitions

## Development Guidelines

- Use functional components + Hooks
- All components must have TypeScript types
- Styles use only Tailwind CSS
- API calls use functions in `/src/lib/api`
- State management uses Zustand

## Design Style

- Minimalist, professional
- Primary color: #6366F1 (Indigo)
- Border radius: 6px
- Font: Inter
```

### Step 2: Create TODO.md

```markdown
# Development Progress

## Completed ✅

- [x] Project initialization
- [x] Supabase configuration
- [x] User authentication (registration/login)
- [x] Note list page

## In Progress 🚧

- [ ] Note editor
  - [x] CodeMirror integration
  - [x] Markdown syntax highlighting
  - [ ] Live preview
  - [ ] Auto-save

## To Do 📋

- [ ] Note search
- [ ] Note categorization/tags
- [ ] Export functionality
- [ ] Dark mode

## Known Issues 🐛

- Editor performance is poor on mobile
- Long notes load slowly
```

### Step 3: Create Rules File

```
Project: Online Note App

Tech stack:
- React 18 + TypeScript + Vite
- Tailwind CSS + Headless UI
- CodeMirror 6
- Supabase
- Zustand (state management)

Code guidelines:
- Use functional components
- All components must have TypeScript type definitions
- Props type naming: ComponentName + Props (e.g., EditorProps)
- Styles use only Tailwind CSS
- Avoid using `any` type
- API calls must include error handling

Design guidelines:
- Primary color: #6366F1
- Border radius: rounded-md (6px)
- Spacing: Use Tailwind's standard spacing (4, 8, 12, 16...)
- Buttons: px-4 py-2, darken on hover
- Input fields: border-gray-300, border-indigo-500 on focus

Naming conventions:
- Component files: PascalCase (e.g., NoteEditor.tsx)
- Utility functions: camelCase (e.g., formatDate.ts)
- Constants: UPPER_SNAKE_CASE (e.g., API_URL)

Always follow these guidelines.
```

### Step 4: Add Context Comments in Code

```typescript
// src/lib/api/notes.ts

/**
 * Note API function collection
 * 
 * Technical choices:
 * - Use Supabase Client instead of raw SQL
 * - All functions return { data, error } format
 * - Errors are handled here; no exceptions are thrown
 */

import { supabase } from './supabase';
import type { Note } from '@/types';
```

With this complete context system, whenever you start a new conversation, simply paste the relevant documentation to AI, and it will quickly get up to speed, providing accurate answers.

## Final Thoughts

Context engineering is one of the most overlooked yet crucial aspects of Vibe Coding. Many people focus on "how to ask" but neglect "how to make AI remember."

Let me summarize the key points of this article:

1. Context is AI's working memory: It determines whether AI can understand your project.

2. Manage three levels: Project-level, feature-level, and conversation-level context all matter.

3. Build a documentation system: `README.md`, `TODO.md`, and context files are essential.

4. Leverage tool features: Different tools have