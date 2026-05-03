# AI Code Editor

> A more professional and powerful Vibe Coding development approach

Hello, I'm Yupi.

In previous articles, we learned how to use no-code platforms and AI agent platforms. If you've already created several projects with these tools, you might start to feel some limitations:

- I want to modify a specific file, but the no-code platform isn't flexible enough...
- As projects grow larger, managing code in the browser becomes cumbersome...
- I want to learn more professional development methods, but I don't want to give up AI assistance...

If you have these thoughts, congratulations! It's time to upgrade to an AI code editor!

AI code editors combine the powerful features of traditional code editors with AI's intelligent assistance, allowing you to develop projects more flexibly and professionally. In this article, I'll explain the mainstream AI code editor Cursor in detail and share more AI code editors worth paying attention to.

## 1. What is an AI Code Editor?

Before understanding AI code editors, let's first grasp a basic concept: what is a code editor?

Simply put, **a code editor is a tool for programmers to write code**, just like Word is a tool for writing documents. Common code editors include VS Code, Sublime Text, Notepad++, etc.

There's also a related concept called **IDE (Integrated Development Environment)**, which is more powerful than a code editor. It not only allows you to write code but also integrates a complete set of development tools like debugging, compiling, and version control. Common IDEs include JetBrains' IntelliJ IDEA, PyCharm, WebStorm, etc.

However, in practice, the line between code editors and IDEs has become increasingly blurred. For example, although VS Code is called an editor, its functionality is almost on par with an IDE through the installation of plugins. So you don't need to worry too much about the distinction between these two concepts; just know that they are tools for writing code.

Now let's clarify two questions:

- What's the difference between an AI code editor and a no-code platform?
- What's the difference between an AI code editor and a traditional code editor?

### AI Code Editor vs. No-Code Platform

No-code platforms are used in the browser, where you generate entire applications through conversations with AI, making them ideal for quickly prototyping and simple projects. AI code editors, on the other hand, need to be downloaded and installed on your computer, allowing you to precisely control each file and line of code, and they come with a complete development toolchain (e.g., code debugging, version control), making them more suitable for complex projects and professional development.

To use an analogy, a no-code platform is like ordering food at a restaurant, where the chef prepares and serves it to you; an AI code editor is like cooking in your own kitchen with a smart assistant by your side.

### AI Code Editor vs. Traditional Code Editor

So what's the difference between an AI code editor and a traditional code editor (e.g., VS Code, JetBrains, Sublime Text)?

In my opinion, the biggest difference is **deep integration with AI**.

A traditional code editor is just a tool for writing code; you need to figure out how to write each line yourself. An AI code editor, however, comes with a powerful AI assistant built-in, which can help you generate code, explain code, modify code, and even automatically execute multi-file tasks.

Specifically, AI code editors have capabilities that traditional editors lack:

- Generate code by describing requirements in natural language
- AI understands the context of the entire project
- Automatically modify multiple files
- Execute commands automatically (e.g., install dependencies)
- Smart code completion (much smarter than traditional completion)

In short, a traditional code editor is "a tool for you to write code," while an AI code editor is "a tool for AI to help you write code." In practice, the efficiency gap can be more than 10 times.

If you have some programming foundation, you'll quickly get the hang of AI code editors.

## 2. Cursor: The Hottest AI Code Editor

Cursor is currently the hottest AI code editor, dubbed "VS Code of the AI era." It's based on VS Code, retaining all its advantages while adding powerful AI features.

![](https://pic.yupi.icu/1/image-20260107133923123.png)

Cursor's core advantages:

1. Multiple AI features: Tab code completion, Agent, Chat, inline editing, etc.
2. Multi-model support: Supports multiple models like Claude, GPT, Gemini, Grok, and has an Auto selection feature
3. Context awareness: AI understands the entire project's code, supporting up to 1M Token context (Max mode)
4. Mature ecosystem: Based on VS Code, supports all VS Code plugins

### Cursor's Core AI Features

Cursor's most powerful aspect is its variety of AI features, suitable for different scenarios.

#### 1. Tab Code Completion

This is the most basic feature, similar to GitHub Copilot. When you write code, AI automatically predicts what you want to write, and pressing Tab accepts the suggestion.

![](https://pic.yupi.icu/1/image-20260107134002087.png)

This feature is particularly useful for writing repetitive code, completing function implementations, and quickly generating boilerplate code. The advantage is speed, as it doesn't interrupt your flow; the drawback is that it can only complete code, not make significant modifications.

#### 2. Agent

This is the most powerful feature introduced in Cursor 2.0, and it's the closest to an AI programmer mode. In this mode, AI can modify multiple files simultaneously, create new files automatically, execute commands (e.g., install dependencies), understand the entire project structure, and even use the browser for testing.

You can use it to add new features (requiring modifications to multiple files), refactor code, fix complex bugs, set up project frameworks, and handle various complex tasks.

Cursor has also introduced its own **Composer model**, an AI model optimized specifically for software engineering, generating code four times faster than similar models.

For example:
```
Please add user authentication functionality, including:
- Login page
- Registration page
- Authentication middleware
- User database model
```

Agent will automatically analyze the project, install necessary dependencies, create required files, modify relevant code, and complete the task.

![](https://pic.yupi.icu/1/image-20260107134332952.png)

#### 3. Chat and Inline Editing

Chat is the most commonly used feature. You can select a piece of code and tell AI in natural language what you want to do. You can use it to explain code, modify a function, fix bugs, optimize performance, etc.

![](https://pic.yupi.icu/1/image-20260107134511105.png)

Inline editing allows you to modify code directly, press `Cmd/Ctrl + K` to quickly invoke it, and AI will generate or modify code at the current position.

![](https://pic.yupi.icu/1/image-20260107134532921.png)

For example, you can select a piece of code and tell AI:
- What does this function do?
- Help me optimize its performance
- Add error handling

### How to Use Cursor?

Let me demonstrate Cursor's usage with a complete example.

Suppose I want to create a simulated electronic whiteboard where I can draw anything and export it as an image.

Let's get started!

1) First, go to the [Cursor website](https://cursor.com) and download the version suitable for your system (Windows/Mac/Linux are all supported).

2) Create an empty folder to store the entire project code. It's recommended to use an English name, such as `yupi-drawboard`.

Then open Cursor and open the folder you just created.

![](https://pic.yupi.icu/1/image-20260107142103143.png)

3) Open the Agent panel, select Agent mode and the large model, then enter a prompt to describe your requirements:

```
Help me develop a simulated electronic whiteboard where users can draw and export images
```

![](https://pic.yupi.icu/1/image-20260107142244095.png)

Next, grab a cup of coffee and wait for AI to generate the code. It will automatically create multiple files, and you can see its progress in the Agent panel.

![](https://pic.yupi.icu/1/image-20260107142422465.png)

4) After a few minutes, AI will generate the complete code. You can choose to accept all, reject all, or selectively accept parts of the code:

![](https://pic.yupi.icu/1/image-20260107142737589.png)

5) Since the requirement is relatively simple, you can directly open the `index.html` file in the browser to see the running effect.

![](https://pic.yupi.icu/1/image-20260107142906255.png)

Not bad, right? Guess what I drew~

6) If you need to modify details or fix bugs, you can directly chat with AI.

Of course, if you have some programming foundation, you can select a piece of code and use inline editing with `Cmd/Ctrl + K`.

![](https://pic.yupi.icu/1/image-20260107142631274.png)

When you write new code, AI will automatically suggest completions (Tab completion feature), and you can press Tab to accept the suggestion.

This is the basic usage of Cursor. Additionally, Cursor supports online search, browser usage, voice input, MCP plugins, and more, which you can explore gradually.

### Pros and Cons of Cursor

Cursor is currently the most comprehensive AI code editor I've used, with its various AI modes covering all scenarios from code completion to agents.

Since it's based on VS Code, all VS Code plugins, themes, and shortcuts work. If you've used VS Code before, you'll get the hang of it quickly.

Moreover, you can freely switch between mainstream models like Claude, GPT, Gemini, and even use your own large models and API keys.

![](https://pic.yupi.icu/1/1764145641557-9b0b777f-f9ca-4cb4-8566-437dbd4b2cbb.png)

Additionally, Cursor has a large user base, making it easier to find solutions when encountering problems. There are also many tutorials and videos online, and I've been following Cursor's development closely, creating several tutorials myself.

However, Cursor's biggest drawback is its price. The premium plans are quite expensive, and each plan has usage limits.

When you exceed the monthly usage limit, you can choose to add pay-as-you-go usage (charged by API rate) or upgrade to a higher tier. Different models have different API costs, and your model choice will affect usage speed.

For detailed pricing information and usage calculations, it's recommended to check the [Cursor official pricing documentation](https://cursor.com/cn/docs/account/pricing), which has the latest and most accurate information.

If you're serious about learning Vibe Coding, I recommend subscribing to the Pro version. The efficiency boost you get for $20 is worth it for most people. If you're a heavy user, using Agent extensively every day, consider Pro Plus or Ultra.

As a Cursor enthusiast like me, even with a premium subscription, I have to bear the high bills...

![](https://pic.yupi.icu/1/yupicursorbilling.png)

Additionally, Agent mode may take longer to handle complex tasks. Although AI assists, some programming foundation is still required, and complete beginners might find it a bit complex.

For more Cursor usage tips and money-saving methods, check out the [Advanced Elective: Tips & Tricks] section of this tutorial series, where there are detailed explanations.

## 3. Other Mainstream AI Code Editors

Besides Cursor, there are other AI code editors worth knowing about.

### Windsurf

[Windsurf](https://codeium.com/windsurf) is an AI code editor launched by Codeium, with the biggest advantage being completely free. After using up the free quota, Windsurf remains free to use, making it particularly suitable for students and developers with limited budgets.

It also offers Cascade Agent mode (similar to Cursor's Agent), where you describe requirements in natural language, and AI automatically creates and modifies multiple files. The biggest advantage is that it's completely free.

![](https://pic.yupi.icu/1/image-20260107135727334.png)

### Antigravity

[Antigravity](https://antigravity.google) is a smart agent development platform launched by Google, with an interface similar to VS Code. It adopts an Agent-first design, where AI agents can autonomously plan, execute, and verify complex tasks.

It integrates large models like Gemini, supports 1M Token context, and is suitable for developers who want to try Google's AI ecosystem and need ultra-long context for large projects.

![](https://pic.yupi.icu/1/image-20260107135834072.png)

### Kiro

[Kiro](https://kiro.dev) is an AI IDE launched by Amazon, emphasizing "specification-driven development." You write the requirements first, and then AI implements them.

It integrates deeply with AWS, allowing direct deployment to AWS services, making it suitable for developers using AWS, teams needing standardized development processes, and enterprise application scenarios.

![](https://pic.yupi.icu/1/image-20260107135922286.png)

### TRAE

[TRAE](https://www.trae.ai) is an AI-native programming tool launched by ByteDance. It has two development modes:

- IDE mode is similar to Cursor, retaining traditional development workflows
- SOLO mode lets AI take the lead, automatically advancing development tasks

TRAE's SOLO mode is its biggest highlight, distinguishing it from traditional human-led + AI-assisted programming. In SOLO mode, AI leads tasks and automatically executes development. You just need an idea, and AI will automatically generate product requirement documents, technical architecture documents, then autonomously write code, install dependencies, test, and verify until the project is ready to run.

Moreover, TRAE supports integrating third-party services like Supabase databases, Stripe payments, OpenRouter AI services, etc., allowing seamless integration without reading documentation. It helps you quickly develop commercial-grade products with complete front-end and back-end.

![](https://pic.yupi.icu/1/image-20250928222329915.png)

The domestic version of TRAE is very suitable for Chinese users, with fast access speeds. I've used TRAE SOLO to develop WeChat mini-programs; interested users can check out the [practical video](https://www.bilibili.com/video/BV1yMn3zuE7L).

### Zed

[Zed](https://zed.dev) is a high-performance code editor written in Rust, created by the Atom founding team. It comes with an AI assistant, supports multiple AI models, and enables real-time collaborative editing.

Its advantages are extremely fast startup speed and low resource consumption. It's suitable for developers who pursue ultimate performance, have average computer configurations, and need team collaboration.

![](https://pic.yupi.icu/1/windows-multibuffer.jpeg)

## 4. How to Choose an AI Code Editor?

By now, you might be asking: With so many AI code editors, which one should I choose?

Actually, the choice is simple, mainly depending on your needs and budget.

**Cursor is the top recommendation.** As of now, Cursor remains the most powerful and mature AI code editor, and I personally use it as my main tool for project development.

As mentioned earlier, its advantages include:

- Comprehensive features: Tab, Agent, Chat, inline editing, etc.
- Frequent updates: Often introduces new features
- Strong context understanding: Supports up to 1M Token
- Multi-model support: Freely switch between Claude, GPT, Gemini, etc., and connect to new models promptly
- Mature ecosystem: Based on VS Code, all plugins work
- Active community: Easy to find solutions when encountering problems

If your budget allows (over $20 per month), Cursor is the first choice. If your budget is limited, Windsurf is a great free option; although its features are relatively simple, it's sufficient for learning.

## 5. Practical Tips for AI Code Editors

Whether you choose Cursor or another AI code editor, the following practical tips can help improve your efficiency.

### 1. Leverage Context

The most powerful aspect of AI code editors is their ability to understand the context of the entire project. Make full use of this:

- Create a `README.md` in the project root directory to describe the overall architecture
- Use `.cursorrules` (or other rule files supported by the editor) to tell AI your coding standards
- When chatting, reference relevant files (`@filename`)

For example:
```
Refer to the code style of @src/components/ImageUploader.vue to create a Card component
```

![](https://pic.yupi.icu/1/image-20260107140926938.png)

### 2. Implement Step by Step

Don't propose overly complex requirements at once; break them down:

❌ Bad approach:
```
Create a complete e-commerce website
```

✅ Recommended approach:
```
Step 1: Create a product list page
Step 2: Add a product detail page
Step 3: Add a shopping cart feature
Step 4: Add a checkout feature
```

### 3. Utilize Shortcuts

Proficiency with shortcuts can significantly boost efficiency.

Take Cursor as an example; try these shortcuts to reduce mouse usage and operate faster.

Chat-related:
- `Cmd/Ctrl + L`: Toggle sidebar (unless bound to a mode)
- `Cmd/Ctrl + I`: Toggle sidebar (unless bound to a mode)
- `Cmd/Ctrl + K`: Open inline editing, insert AI-generated code at the current position
- `Tab`: Accept suggestion

Code editing:
- `Cmd/Ctrl + Shift + L`: Add selected content to chat
- `Alt + ↑/↓`: Move current line
- `Cmd/Ctrl + /`: Comment/uncomment

File operations:
- `Cmd/Ctrl + Shift + F`: Global search

For the latest default keyboard shortcuts, refer to the [official documentation](https://cursor.com/cn/docs/configuration/kbd):

![](https://pic.yupi.icu/1/image-20260104192219087.png)

### 4. Code Review

AI-generated code isn't always perfect; develop a habit of reviewing it.

Check if the code logic is correct, if there are security risks, if performance is reasonable, and if the code style is consistent.

Of course, you can also ask AI to review it:

```
Please review this code and point out potential issues
```

### 5. Save Important Versions

Git is currently the most popular distributed version control system (VCS), an indispensable tool for team collaboration development. It saves and manages all update records of files, distinguishing them with **version numbers**. This supports restoring edited documents to their pre-modification state, comparing differences between versions, preventing old versions from overwriting new ones, and more.

Before making major changes, remember to commit to Git:

```bash
git add .
git commit -m "Version before adding user authentication functionality"
```

This way, if AI messes up, you can always revert.

💡 If you want to learn Git and GitHub in depth, check out my [Git & GitHub Beginner's Learning Path](https://www.codefather.cn/course/1789189862986850306