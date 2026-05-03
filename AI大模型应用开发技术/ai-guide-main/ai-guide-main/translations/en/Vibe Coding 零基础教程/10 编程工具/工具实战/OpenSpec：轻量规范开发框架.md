# OpenSpec: Lightweight Specification Development Framework

> Keeping documentation and code always in sync

Hello, I'm programmer Yupi.

In previous articles, we learned about the Spec-kit specification-driven development framework. But you might find Spec-kit's workflow too complex—is there a more lightweight alternative?

In this article, I'll introduce **OpenSpec**, a lightweight specification-driven development framework that's simpler and easier to use than Spec-kit, making it ideal for iterating features on existing projects.

## 1. What is OpenSpec?

[OpenSpec](https://openspec.dev/) is a lightweight specification-driven development framework that's simpler and more user-friendly than Spec-kit.

Its core philosophy is to treat specification documents as part of the codebase. For every feature change:  
**First draft a change proposal** => Implement after confirmation => Archive changes into specification documents after implementation, keeping documentation and code always synchronized.

![](https://pic.yupi.icu/1/%E6%BC%AB%E7%94%BB%E5%9B%BE6%E5%A4%A7.jpeg)

## 2. Quick Start with OpenSpec

Let me walk you through a practical example to quickly get started with OpenSpec.

### 1. Installing OpenSpec

Visit the [OpenSpec official repository](https://github.com/Fission-AI/OpenSpec/) to view documentation.

First ensure your computer has Node.js installed with a compatible version (e.g., here it requires Node.js >= 20.19.0), then globally install the OpenSpec CLI:

```bash
npm install -g @fission-ai/openspec@latest
```

Navigate to your project directory and run the initialization command:

```bash
openspec init
```

During initialization, you'll be asked to select AI tools to integrate (e.g., Claude Code, Cursor, etc.). Here I choose Cursor.

![](https://pic.yupi.icu/1/image-20260116163202471.png)

After running the command, OpenSpec will automatically generate an `openspec/` directory in your project containing:

- `openspec/specs/`: Stores main specification documents recording the project's complete current state
- `openspec/changes/`: Stores change proposals recording modification plans
- ⭐️ `openspec/AGENTS.md`: Guidelines for AI programming assistants to use OpenSpec for specification-driven development, including complete workflows for creating change proposals, writing requirement specifications, validating and archiving changes.
- `openspec/project.md`: Contextual description of the current project (used to record project information)

![](https://pic.yupi.icu/1/image-20260116164308965.png)

Additionally, it will generate corresponding command files based on the AI programming tool you selected, such as for Cursor:

![](https://pic.yupi.icu/1/image-20260116173814973.png)

With these files, we can begin the standardized development process.

### 2. Standardized Development Process

Refer to the [official documentation](https://github.com/Fission-AI/OpenSpec/), mainly divided into 5 steps:

#### 1) Draft Change Proposal

Directly tell the AI in your programming tool to create a change proposal. For example, if I want to add user search functionality:

```markdown
Create an OpenSpec change to add feature: search users by name and email
```

You can also use slash commands in AI programming tools (e.g., Claude Code, Cursor):

```markdown
/openspec:proposal Add feature: search users by name and email
```

![](https://pic.yupi.icu/1/image-20260116171714070.png)

The AI will create an independent directory `openspec/changes/add-user-search/` for this feature, containing:

- `proposal.md`: Describes what to change and why
- `tasks.md`: Task breakdown of implementation steps
- `specs/…/spec.md`: Specific content of requirement changes

![](https://pic.yupi.icu/1/image-20260116171953809.png)

#### 2) Verify & Review

Run the following commands to check if the AI-created change proposal is correct:

```bash
openspec list                        # View all changes
openspec validate add-user-search    # Verify format correctness
openspec show add-user-search        # View detailed content
```

![](https://pic.yupi.icu/1/image-20260116172259055.png)

#### 3) Review Proposal with Team

If improvements are needed, continue the conversation with AI, e.g.:

```markdown
Can you help add more search conditions and limitations?
```

The AI will update the specifications and task list until consensus is reached.

#### 4) Implement Changes

After confirming the specifications, have the AI begin implementation:

```markdown
The specifications are perfect, start generating code
```

Or use slash commands:

```markdown
/openspec:apply add-user-search
```

The AI will implement step-by-step according to the task list in `tasks.md` and mark completion status.

![](https://pic.yupi.icu/1/image-20260116172654371.png)

Generation completes quickly, with the AI's output being very organized—all changes are clear at a glance:

![](https://pic.yupi.icu/1/image-20260116172905921.png)

#### 5) Archive Changes

After all tasks are completed, have the AI archive this change:

```markdown
Please archive this change
```

Or use slash commands:

```markdown
/openspec:archive add-user-search
```

Or run in terminal:

```bash
openspec archive add-user-search --yes
```

![](https://pic.yupi.icu/1/image-20260116172957759.png)

This command will:
- Move the change folder to the `openspec/changes/archive/` archive area
- Automatically merge requirement changes into the `openspec/specs/` main specifications
- Keep documentation and code synchronized

![](https://pic.yupi.icu/1/image-20260116174247313.png)

This way, through the historical records in `openspec/changes/`, you can trace the context of each change at any time.

Additionally, during the development process, it's recommended to regularly run the `openspec validate` verification command to ensure specification integrity.

## 3. Differences Between OpenSpec and Spec-kit

By now, you can probably sense the differences between OpenSpec and Spec-kit.

- Spec-kit requires a complete 7-step process: Establish guidelines → Write requirements → Clarify questions → Finalize solution → Break down tasks → Review → Write code), suitable for large new projects from scratch
- OpenSpec's process is more streamlined: Draft proposal → Review → Implement → Archive → Verify, with faster onboarding, making it ideal for iterating features on existing projects.

If you're iterating features on an existing project, OpenSpec is the better choice. If you're starting a large new project from scratch, Spec-kit's complete process helps lay a solid foundation.

## Final Thoughts

By now, you should have a basic understanding of OpenSpec.

**OpenSpec is more lightweight than Spec-kit and better suited for daily development feature iterations.**

If you find Spec-kit too heavy, try OpenSpec. Its simpler workflow still ensures documentation and code synchronization, making team collaboration smoother.

I recommend trying OpenSpec in your projects to experience the benefits of specification-driven development firsthand.

## Recommended Resources

1) Yupi AI Navigation Site: [Comprehensive AI Resources, Latest AI News, Free AI Tutorials](https://ai.codefather.cn)

2) Programming Navigation Learning Circle: [Learning Paths, Programming Tutorials, Practical Projects, Job Hunting Guide, Q&A](https://www.codefather.cn)

3) Programmer Interview Cheat Sheet: [Internship/Campus Recruitment/Social Recruitment High-Frequency Topics, Enterprise Question Analysis](https://www.mianshiya.com)

4) Programmer Resume Builder: [Professional Templates, Rich Examples, Direct to Interview](https://www.laoyujianli.com)

5) 1-on-1 Mock Interview: [Essential for Internship/Campus Recruitment/Social Recruitment Interviews to Get Offers](https://ai.mianshiya.com)