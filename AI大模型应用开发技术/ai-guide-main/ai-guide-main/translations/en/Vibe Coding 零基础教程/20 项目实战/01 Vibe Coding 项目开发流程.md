# Vibe Coding Project Development Process

> The Complete Vibe Coding Workflow from Idea to Product



Hello, I'm Yupi.

In the quick start phase, we've already created a simple web application. That project was straightforward, and we could complete it by directly conversing with AI. But now, let's tackle a more practical question: How should we approach building a complete, commercial-grade project?

Many beginners make the same mistake when starting with AI for projects: they open an AI coding tool and immediately start chatting with the AI to generate code. For small projects, this approach works fine. However, if you're aiming to build a full-fledged commercial project, this method will lead to problems. Initially, things might go smoothly, but soon you'll find the AI-generated code becoming increasingly messy and not what you intended. After endless modifications and mental battles, you might end up having to start over, wasting a lot of time.

Why does this happen? Because you skipped the most crucial step — **planning**.

In the era of Vibe Coding, although AI can help you write code, good planning remains the key to project success. In fact, planning is even more important now than ever before.

Below, I'll share a proven 5-step development process to help you avoid these pitfalls.



## Why Do We Need a Standard Process?

In traditional programming, if your project planning is unclear, the worst-case scenario is that the code quality might suffer, but at least you know what you're writing.

In Vibe Coding, however, the situation is entirely different. If your project planning is unclear, the AI will "freestyle," generating code that you neither understand nor need.

AI is not magic; it's a highly powerful executor. You tell it what to do, and it does it. If you don't know what you want, the AI naturally can't help you create the ideal product.

Therefore, a clear development process can help you:

1. Clarify project goals and requirements
2. Help the AI understand your intentions
3. Prevent the project from spiraling out of control
4. Improve development efficiency
5. Create truly useful products



## The Complete 5-Step Workflow

This process is derived from my practical experience and incorporates insights from many excellent developers.

It consists of 5 steps:

1. Research
2. Product Requirements Document (PRD)
3. Technical Design Document (Tech Design)
4. AI Agent Instructions (AGENTS.md)
5. Implementation and Iteration (Build)

Does it seem complicated? Don't worry, I'll break it down in the simplest way possible.

![](https://pic.yupi.icu/1/vibecodingworkflow%E5%A4%A7.jpeg)



## Step 1: Research

After understanding the overall process, let's dive into the first step and explain how to approach each step.

Before diving in, clarify: What exactly do I want to do? Why do I want to do it? Has anyone done something similar?

Many beginners skip this step and jump straight into writing requirements. However, my experience shows that spending 30 minutes on research can save you from many detours.

How to do it?

1) Define Your Goal

Start by asking yourself a few questions: What problem do I want to solve? Is this problem worth solving? What do I envision the final product to look like?

For example, if you want to build a budgeting app, think clearly: Is it for personal use or others? What types of transactions will it primarily track? What core features are needed?



2) Research Similar Products

Look into whether there are similar products on the market. How do they approach it? What are their strengths and weaknesses?

You can search for related apps or websites, explore open-source projects, or directly ask AI: "What are some good budgeting apps? What are their features?"

This step is crucial as it helps you avoid reinventing the wheel and provides inspiration and references.



3) Document Your Findings

Record your ideas and research findings in a simple document, such as `RESEARCH.md`. This document doesn't need to be formal; think of it as a diary where you jot down your thoughts and discoveries.

For example, suppose you want to build a budgeting app. After researching several budgeting apps on the market, you find their features too complex, and you only need a simple tool. You then document your thoughts, and `RESEARCH.md` might look like this:

```markdown
# Budgeting App Research

## Goal
Create a simple and easy-to-use personal budgeting app to help me develop a budgeting habit.

## Research Findings
- Existing budgeting apps are too complex
- I only need to quickly record income and expenses
- Monthly statistics would be helpful

## Core Requirements
1. Quickly add income/expense records
2. View records by date
3. View monthly statistics
4. Local data storage
```



## Step 2: Product Requirements Document (PRD)

After completing the research, you should have a clear understanding of what you want to build. Next, we need to organize these ideas into a formal document. Clearly define which features to include and which to exclude.

This is the most critical step in the entire process. A well-written PRD helps the AI accurately understand your intentions and generate high-quality code.



### How to Write a High-Quality PRD?

My suggestion is to first write a simple requirement description yourself, then let the AI expand it into a complete PRD.

For example, you could start with:

```
I want to build a budgeting app that can quickly record income and expenses and view monthly statistics.
It should be simple and easy to use, without complex features.
```

Then send this to a large AI model like Gemini or DeepSeek:

```
Please expand this requirement into a complete Product Requirements Document (PRD),
including:
1. Product overview and target users
2. Detailed feature list
3. Feature priorities (MVP and future versions)
4. UI design requirements
5. Tech stack recommendations
6. Non-functional requirements (performance, security, etc.)
```

The AI will generate a structured PRD, which you can then modify and refine based on your needs.



### What Should a PRD Include?

A complete PRD typically includes:

- Product Overview (a brief description of what the product is)
- Target Users (who will use this product)
- Core Features (list all the features to be implemented)
- Feature Priorities (which features are must-haves and which can be added later)
- UI Design (a simple description of what the interface should look like)
- Tech Stack Recommendations
- Code Style and Architecture Patterns
- Constraints and Edge Cases



For example:

```markdown
# Budgeting App PRD

## Product Overview
A simple personal budgeting app to help users quickly record daily income and expenses.

## Target Users
Individuals who need to track their finances but don't want to use complex apps.

## Core Features

### Must-Have (MVP)
1. Add Income/Expense Record
   - Input amount
   - Select type (income/expense)
   - Select category (food, transportation, salary, etc.)
   - Add notes (optional)
   - Select date

2. View Record List
   - Display records in reverse chronological order
   - Show amount, type, category, notes
   - Allow record deletion

3. Monthly Statistics
   - Display total income for the month
   - Display total expenses for the month
   - Display monthly balance

### Future Features
- Data export
- Chart visualization
- Budget setting
- Multi-account management

## UI Design
- Homepage: Display record list and add button
- Add Page: Form input
- Statistics Page: Display monthly data
```



## Step 3: Technical Design Document (Tech Design)

With a clear PRD, we know what to build.

The next step is to determine the technology stack to implement these features and the overall technical architecture.

Create a `TECH_DESIGN.md` file that includes:

- Tech Stack Selection (frontend, backend, database)
- Project Structure (how the code is organized)
- Data Model (what data needs to be stored)
- Key Technical Points (technical challenges to be aware of)

Continuing with the budgeting app example. Based on the PRD requirements, you decide to use React for the frontend due to its mature ecosystem and strong AI support. LocalStorage is sufficient for data storage since it's a personal tool and doesn't require a backend. The technical design document might look like this:

```markdown
# Budgeting App Technical Design

## Tech Stack
- Frontend: React + TypeScript + Vite
- Styling: Tailwind CSS
- Data Storage: LocalStorage
- Deployment: Vercel

## Project Structure

src/
  components/     # Components
  pages/          # Pages
  hooks/          # Custom Hooks
  utils/          # Utility Functions
  types/          # Type Definitions

## Data Model

### Transaction (Transaction Record)
- id: string
- amount: number
- type: 'income' | 'expense'
- category: string
- note: string
- date: string

## Key Technical Points
1. Use LocalStorage for data storage
2. Use React Hooks for state management
3. Use a date library for date handling (date-fns)
```

If you're unfamiliar with tech stack selection, you can ask AI: "What tech stack should I use for a budgeting app?"

The AI will recommend suitable technologies.



## Step 4: AI Agent Instructions (AGENTS.md)

With the PRD and technical design ready, we now need to create a dedicated instruction file for the AI, specifying the rules it should follow in this project.

You can name this file anything, such as `AI_RULES.md`, `INSTRUCTIONS.md`, etc. However, I recommend using `AGENTS.md` as it's an emerging community standard.

[AGENTS.md](https://agents.md/) is a standardized file format used to guide AI coding tools on how to work. It's like a "work manual" for AI. This standard is promoted by companies like OpenAI, Anthropic, Google, and is currently used in over 80,000 open-source projects. Mainstream AI coding tools like Cursor, Windsurf, Claude Code, Gemini CLI, GitHub Copilot, etc., support automatic reading of AGENTS.md files.

![](https://pic.yupi.icu/1/image-20260109161639565.png)

What should AGENTS.md include?

Generally, it includes project overview, development standards, testing requirements, code style, and notes.

Continuing with the budgeting app example. Based on the previous PRD and technical design, you can create an AGENTS.md file like this:

```markdown
# Budgeting App AI Development Instructions

## Project Overview
This is a simple personal budgeting app built with React + TypeScript.

## Development Standards
- Use TypeScript for type safety
- Use functional components + Hooks
- Use Tailwind CSS for styling
- Store all data in LocalStorage

## Code Style
- Use ESLint and Prettier
- Component names in PascalCase
- Function names in camelCase
- Constants in UPPER_SNAKE_CASE

## Testing Requirements
- Manually test each feature after completion
- Ensure data is correctly stored and retrieved
- Test various edge cases

## Notes
- Keep the code simple, avoid over-engineering
- Prioritize core features
- Ensure mobile compatibility
```

With this file, the AI knows what rules to follow in this project, and the generated code will be more standardized and consistent.



## Step 5: Implementation and Iteration (Build)

The first 4 steps are preparatory work; now we can finally start coding!

But don't write all the code at once; iterate step by step.



### Iterative Strategy

For complex projects, trying to achieve everything at once is unrealistic. I recommend a 3-step strategy:

#### 1. Generate the Basic Framework

First, let the AI generate the basic project framework. Don't worry about functionality yet; just ensure the project can run.

```
Based on the requirements in PRD.md, TECH_DESIGN.md, and AGENTS.md,
initialize the project and create the basic project structure, including:
1. Install necessary dependencies
2. Create directory structure
3. Configure the development environment
4. Create basic routing and page framework
Ensure the project can start normally.
```

For those with programming experience, there's an even simpler method. You can use scaffolding or project templates to quickly generate the basic framework.

Scaffolding is like an automation tool that helps generate the basic project structure and configuration files, such as `create-react-app`, `create-vite`, etc.

Project templates are pre-configured project examples that you can directly copy and use.

Using these tools, you can set up the basic framework in minutes without starting from scratch.



#### 2. Gradually Implement Core Features

Once the framework is set up, you can start implementing specific features. The key here is to **get the core business process working and implement core features** first, rather than aiming for perfection from the start. Break the project into smaller features and implement them one by one.

For example, the budgeting app can be broken down as follows:

1. Implement the data model and storage
2. Implement the add record feature
3. Implement the record list display
4. Implement the delete feature
5. Implement monthly statistics

For each feature, you can converse with the AI like this:

```
I want to implement the add record feature. Based on the requirements in PRD.md and TECH_DESIGN.md,
create an AddTransaction component with form input and submission functionality.
```

After completing each feature, test it: Does the feature work correctly? Are there any bugs? Does the interface meet expectations?

If there are issues, continue conversing with the AI to make modifications.



#### 3. Optimize Implementation Details

Once the core features are implemented, you can refine the details. **Without affecting functionality**, optimize performance, improve user experience, and beautify the interface.

After refining the details, your MVP (Minimum Viable Product) is complete. You can then expand more features based on user feedback and your own ideas.

Additionally, after getting the core features working, it's highly recommended to use Git for code management. Commit your code after completing each feature, so if issues arise later, you can quickly revert to a previous version.

If you're not familiar with Git, check out [Yupi's Git Learning Path](https://www.codefather.cn/course/1789189862986850306/section/1789190804671012866), and you'll get the hang of it quickly.



### Key Techniques to Avoid AI Going Out of Control

In actual development, you might encounter these issues:

- AI outputs irrelevant content
- Implementing a new feature breaks an existing one
- Fixing one bug introduces three more

These issues are common, but there are solutions.

#### 1. Modularize the Project

Since AI's context is limited, as the project's information grows, it might forget previous information, leading to incorrect code generation. Therefore, we should isolate the project's features as much as possible, breaking a large project into smaller modules.

For example, when developing an e-commerce system, you can isolate the product management module. When asking the AI to generate code for adding products, only provide the product table's field design and the business logic rules for adding products, without including unrelated features like payment settlement or user membership as context for the AI.



#### 2. Limit Modification Scope

AI-generated code isn't always controllable, and modifying feature A might inadvertently modify feature B. This issue is easily solved by limiting the modification scope in the prompt:

```
Only modify components/AddTransaction.tsx:
1. Add form validation
2. Keep existing styles and layout
3. Don't modify other files
```



#### 3. Abstract and Reuse

If we want the AI to generate two pages with identical layouts, it might rigidly copy the code from page A to generate page B. This is detrimental to maintaining large projects, as modifying page A might leave page B unchanged. Therefore, we need to proactively tell the AI:

```
Please abstract this page into a reusable component,
allowing other pages to use it by passing different parameters.
```

This also reduces the overall codebase, easing the AI's context memory burden and making the generated results more accurate.



#### 4. Version Control

It's highly recommended to use Git for version control. Commit your code after correctly generating each feature, and manually compare the modified files after each AI-generated code update. If issues arise, you can quickly revert to a previous version, preventing code loss.

```
After completing each feature, commit the code:
git add .
git commit -m "Implemented add record feature"
```

Cursor comes with Git comparison functionality, making it easy to view code changes.



#### 5. Manual Control

Sometimes, the AI might get stuck in a loop due to a lack of key context or its own limitations. For example, repeatedly making the same mistakes or endlessly working on something useless. In such cases, manual intervention is necessary.

Try manually specifying the context, changing the prompt to describe the problem from a different angle, clearing the conversation history and starting over, or even manually modifying part of the code to guide the AI in the right direction.

This is where knowing some programming comes in handy. If you have some programming basics, you can better control and intervene in Vibe Coding. You can understand the AI-generated code, know where the issues lie, manually fix critical parts, and guide the AI in the right direction. This is why I recommend learning some programming basics alongside Vibe Coding.



#### 6. Multi-AI Collaboration

Different AI models excel at different tasks. If a single model can't complete the job properly, you can leverage other models to generate "methods and instructions for teaching the AI."

For example, if GPT-generated code in Cursor has issues, you can:
1. Copy the code and error message to Claude or Gemini
2. Let it analyze the problem and provide modification suggestions
3. Feed the modification suggestions back to GPT to modify the code

This multi-model collaboration approach significantly increases the likelihood of solving problems.



## Case Study - Unclear Requirements Leading to Project Failure

In the project practice section of this Vibe Coding tutorial, I'll share many successful project cases for you to learn from. Here, I'll just give a negative example to show what can go wrong if you don't follow the process.

Xiao Aba wanted to build a to-do list app. He directly opened an AI coding tool and entered:

```
Help me build a to-do list app
```

The AI generated an app that looked decent. But Xiao Aba soon found issues: no categorization, no priority settings, and he didn't like the interface style.

So he continued the conversation:

```
Add categorization
```

The AI added categorization but messed up the layout. Xiao Aba then said:

```
Fix the layout
```

The AI fixed the layout but introduced new bugs. After endless modifications, the code became a mess, and Xiao Aba had to abandon the project.

What went wrong?

First, the initial requirements were unclear, so the AI could only generate code based on its own understanding. Second, each modification was "fixing one problem at a time" without overall planning. Finally, since there was no documentation, the AI had no idea about the project's overall design.

If Xiao Aba had followed the 5-step process from the start, clearly writing the PRD and technical design, these issues wouldn't have occurred.



## Practical Tips for More Accurate AI-Generated Code

After mastering the complete development process