# OpenCode: Hands-on Review of an Open-Source Free AI Command Line Tool

Hello everyone, I'm programmer Yupi.

Claude Code has long been recognized as the undisputed Top 1 AI programming command line tool, enjoying an almost god-like status in the AI and programmer communities.

![](https://pic.yupi.icu/1/happy-new-year-claude-coders-v0-o2quvbl99lag1.png)

But this damn thing isn't very friendly to Chinese users...

First, to use Claude Code, you must have special network access + an official account, otherwise you'll just see red errors.

![](https://pic.yupi.icu/1/cannotuseclaude.png)

Moreover, in September 2025, Anthropic suddenly announced **a complete ban on Chinese-controlled enterprises using Claude services** for some inexplicable reason. This ban not only includes mainland Chinese companies but also overseas companies with over 50% Chinese ownership!

Anthropic even specifically named China, calling us an **adversarial nation**!

![](https://pic.yupi.icu/1/image-20250905164631315.png)

The world has suffered under Claude Code's dominance for too long!

But recently, many programmer friends around me have started switching from Claude Code to another tool - the suddenly popular open-source project OpenCode.

![](https://pic.yupi.icu/1/image-20260107174223010.png)

In just half a year, this project has skyrocketed to 52k stars on GitHub!

To put this in perspective - that's more stars than all my dozens of open-source projects on GitHub combined! So jealous...

![](https://pic.yupi.icu/1/opencodestarhistory.png)

What exactly is OpenCode? Why is it so popular?



## What is OpenCode?

[OpenCode](https://opencode.ai/) is a 100% open-source AI programming command line tool that can be used in **terminals, IDEs, and even desktop applications**.

![](https://pic.yupi.icu/1/screenshot.png)

You might ask: How is this different from Claude Code?

Why not try it and see?

Next, I'll walk you through a hands-on demo - from installation and configuration to actual coding, all in one go~



## Getting Started with OpenCode from Scratch

### 1. Installing OpenCode

Go directly to the OpenCode official website and copy this command:

![](https://pic.yupi.icu/1/image-20260107174407894.png)

Here's the command:

```bash
curl -fsSL https://opencode.ai/install | bash
```

Execute it in your terminal to complete the installation.

After installation, enter `opencode` to launch the program, and you're ready to use it happily~

![](https://pic.yupi.icu/1/image-20260107174646918.png)

Let's start with the classic Hello World - the AI successfully responded.

![](https://pic.yupi.icu/1/image-20260107174755331.png)

Congratulations, you've now mastered 70% of OpenCode.

### 2. Selecting Modes and Models

OpenCode supports 2 modes. The default is Build mode for application development and code generation.

Press Tab to switch to Plan mode for generating execution plans.

![](https://pic.yupi.icu/1/image-20260107174952823.png)

Press `Ctrl + p` to open the command palette with dozens of built-in commands. Let's try switching the LLM first:

![](https://pic.yupi.icu/1/image-20260107175255527.png)

By default, it offers 4 free models:

![](https://pic.yupi.icu/1/image-20260107175409282.png)

Wow, even the latest GLM-4.7 from Zhipu is free? Did I waste money on my Coding Plan subscription?

![](https://pic.yupi.icu/1/image-20260107175513490.png)

Besides free models, OpenCode supports a huge number of AI models for you to choose freely:

![](https://pic.yupi.icu/1/image-20260107175614359.png)

After selecting a model, just configure your API Key:

![](https://pic.yupi.icu/1/image-20260107175657296.png)

If you previously had a **Claude Pro/Max subscription account**, you can log in directly and seamlessly migrate from Claude Code.

![](https://pic.yupi.icu/1/image-20260107175745963.png)

### 3. Quick Commands

OpenCode supports slash commands. Type `/` to see many operations like viewing model lists, checking Agents, managing MCP, switching themes, etc.:

![](https://pic.yupi.icu/1/image-20260107175926346.png)

It supports dozens of different themes, all quite aesthetically pleasing. This shows OpenCode really cares about user experience:

![](https://pic.yupi.icu/1/image-20260107180108430.png)

Type `@` to quickly associate directory files and add context for the AI:

![](https://pic.yupi.icu/1/image-20260107182710150.png)

### 4. Interactive Experience

Compared to Claude Code, OpenCode really maximizes the command line interactive experience. I'd say it's more like a desktop app disguised as a command line tool.

You can click any message to pop up an action box where you can recall messages and AI replies, copy them, or start a new dialog based on the current conversation.

![](https://pic.yupi.icu/1/image-20260107180609525.png)

You can scroll up/down to switch menus and directly click to proceed to the next step.

Press `Ctrl + p` to open the command palette and enable the sidebar:

![](https://pic.yupi.icu/1/image-20260107181100523.png)

Then the interface becomes like this. You call this a command line?

![](https://pic.yupi.icu/1/image-20260107181218259.png)

### 5. LSP Support

If you're observant, you've noticed the `LSP` in the right sidebar. What's this? Some perverted thing?

LSP (Language Server Protocol) is a communication protocol developed by Microsoft to enable communication between code editors and language servers.

Simply put, **LSP is the technology that helps editors understand code.**

For example, when you type `console.` in VS Code, it automatically suggests `log`. You can jump to definitions by clicking function names, and incorrect code gets red underlines. These editor features all rely on LSP.

OpenCode's LSP support means the AI truly understands your code structure rather than treating code as plain text for blind guessing, making modifications more precise.

For instance, when I ask the AI to introduce the most valuable code in my AI quiz platform project, LSP comes into play. It helps the AI quickly locate where a piece of code is called and what variables it references, instead of having the AI dumbly search through text globally.

![](https://pic.yupi.icu/1/image-20260107181807464.png)

### 6. Returning to Previous Sessions

If you accidentally close OpenCode, don't worry. Open the command palette and select "Switch session":

![](https://pic.yupi.icu/1/image-20260107183241477.png)

You can return to your previous chat:

![](https://pic.yupi.icu/1/image-20260107183320692.png)

## Desktop Version of OpenCode

Even with all these user experience improvements in OpenCode, I suspect most of you still dislike the black terminal box.

No problem - OpenCode also offers a desktop app version! Supporting macOS, Windows, and Linux across all platforms. They're really going all out to compete with Claude Code...

> Link: https://opencode.ai/download

![](https://pic.yupi.icu/1/image-20260107182151987.png)

But when I installed and opened it with great enthusiasm, it errored out!

![](https://pic.yupi.icu/1/image-20260107183123854.png)

After some troubleshooting, I found it was because I had a proxy enabled. After disabling it, it ran normally.

![](https://pic.yupi.icu/1/image-20260107183605119.png)

But having gotten used to Cursor, this interaction experience feels a bit perfunctory. I don't recommend using it.

## OpenCode's Extensibility

So far, I think OpenCode completely crushes Claude Code in frontend user experience. Moreover, OpenCode is fully compatible with Claude Code's Skills system!

Skills are capability extension packages prepared for the AI. You can think of them as onboarding documents for new colleagues, containing task execution methods, tool usage instructions, template materials, etc.

For example, you can create a `Company Code Style Skill` documenting code styles, naming conventions, comment requirements, etc. Afterward, Claude Code will automatically follow these standards when generating code without needing repeated explanations.

According to official documentation, OpenCode automatically searches for Skills in these locations:

- `.opencode/skill/<name>/SKILL.md` (project directory)
- `~/.config/opencode/skill/<name>/SKILL.md` (user directory)
- `.claude/skills/<name>/SKILL.md` (Claude Code compatible)
- `~/.claude/skills/<name>/SKILL.md` (Claude Code compatible)

This means if you've previously created custom Skills for Claude Code, you can use them directly with OpenCode! Another seamless migration.

## Oh My OpenCode Supercharged Plugin

If you think OpenCode isn't powerful enough, try the open-source OpenCode enhancement plugin `Oh My OpenCode`, already with 10k stars.

> Project address: https://github.com/code-yeongyu/oh-my-opencode

![](https://pic.yupi.icu/1/image-20260107184457429.png)

How amazing is this plugin? Check out user reviews:

> "It made me cancel my Cursor subscription."
> 
> "Knocked out 8000 eslint warnings with Oh My Opencode, just in a day"

The core feature of Oh My OpenCode is introducing an agent orchestration system called **Sisyphus**.

I looked it up:

> Sisyphus is a king in Greek mythology punished by the gods for deceiving them and challenging authority. His punishment was endlessly pushing a boulder up a mountain, only for it to roll back down upon reaching the top, symbolizing futile, never-ending tasks and representing a spirit of rebellion against absurd fate.

This system can:

1. Schedule multiple AI models in parallel: e.g., have GPT debug while Gemini writes frontend code
2. Automatic task management: Won't stop until tasks are completed, persevering like Sisyphus pushing the boulder
3. Intelligent code review: Automatically detects and cleans redundant AI-generated comments
4. Deep LSP integration: Provides IDE-level features like renaming and jumping to definitions

In short, Sisyphus is an AI supervisor that can command multiple AI models simultaneously while ensuring they complete tasks.

![](https://pic.yupi.icu/1/omo.png)

Although the official docs say installation can be done with one command, I recommend first installing bun, then using npx to install, otherwise errors may occur.

```bash
npm install bun -g
npx oh-my-opencode install
```

During installation, it may ask if you have subscriptions to certain models. I just kept selecting "No":

![](https://pic.yupi.icu/1/image-20260107185251337.png)

After installation completes, enter OpenCode again. Then just include the `ultrawork` (or `ulw`) cheat code in your prompts to activate all enhanced features - automatic scheduling of multiple AI models working simultaneously, deep codebase exploration, and relentless execution.

Let's test this out and see if OpenCode can really kick Claude Code to the curb when it comes to project capabilities?

## Hands-on Project - Building an AI Health Assistant with OpenCode

Recently, Ant Group's `Ant Afu` AI health assistant went viral, with ads featuring host He Jiong appearing everywhere from subway stations to office building TVs.

![](https://pic.yupi.icu/1/mayiafuad.jpeg)

Although I haven't used it yet, I heard it can provide AI preliminary diagnoses by scanning skin or reports, along with intelligent answers to medical questions and treatment suggestions.

Let's build a similar health assistant website!

Before there was Ant Afu, now there's Yupi Akun.

![](https://pic.yupi.icu/1/image-20260107194117758.png)

First, let's analyze: we're building a full-stack project including frontend + backend, with the backend needing to call AI models to generate content.

Here I chose to use **Vercel AI Gateway** to implement AI capabilities - a simple and easy-to-use AI gateway.

![](https://pic.yupi.icu/1/1760687990497-90720fbb-0df6-4ede-87b8-64b8702994e9-20251028181254840.png)

What's an AI gateway?

Simply put, it's like a train station ticket gate. Your application's requests first pass through the gateway, which handles authentication, rate limiting, monitoring, and other complex operations before forwarding requests to AI models.

![](https://pic.yupi.icu/1/1761645642401-683e786e-3e06-420a-abce-cd43f7bfa901.png)

Moreover, Vercel AI Gateway supports integration with over 500 large models and has free quotas, making it perfect for learning and small projects.

> Link: https://vercel.com/ai-gateway

1) First, you need to register/login to Vercel, then create an API Key in the console. Be careful not to leak it:

![](https://pic.yupi.icu/1/1760688078133-7b91b6f3-2fc4-4bb4-b2c1-d517699f0968-20251028181254879.png)

2) Launch OpenCode, switch the model to the highly capable and free GLM-4.7, then input this prompt:

```markdown
You are a professional programmer. Please help me develop the "Daily Health Assistant" website where users can chat with AI to record and manage daily health status.

## Development Requirements

1. Must include complete frontend and backend, backend using Node.js
2. Implement AI capabilities using Vercel's AI Gateway, first check official docs for usage: https://vercel.com/docs/ai-gateway/getting-started
3. Focus on completing core functionality to ensure the project runs properly
4. Overall website interface should adopt a fresh green health style, responsive across devices
5. AI should proactively ask about user health status like sleep, exercise, diet, etc.
```

After sending, OpenCode will automatically use web scraping tools to read Vercel AI Gateway's official docs and learn the latest usage:

![](https://pic.yupi.icu/1/image-20260107190151933.png)

In about 5 minutes, the AI completed all code generation and automatically installed dependencies.

![](https://pic.yupi.icu/1/image-20260107190629349.png)

3) I directly provided the Vercel API Key I obtained earlier to the AI to help launch the project:

![](https://pic.yupi.icu/1/image-20260107190751628.png)

4) After successfully launching the project, open a browser to `localhost:3000` to test it.

But it errored! Couldn't call the AI.

![](https://pic.yupi.icu/1/image-20260107191838608.png)

Perhaps the AI misunderstood the Vercel AI Gateway documentation, leading to incorrect AI calling code. So I input the docs to the AI again for another attempt:

![](https://pic.yupi.icu/1/image-20260107191719979.png)

It errored again. Even though I provided the API Key, the system still reported "Missing API Key".

So I called the AI again, telling it "I already gave you this key earlier".

![](https://pic.yupi.icu/1/image-20260107192718301.png)

After about 5 rounds of errors and fixes, it still wouldn't work properly! I'm exhausted...

![](https://pic.yupi.icu/1/image-20260107193542108.png)

Then I had a mischievous idea: Since we're comparing to Claude Code, why not try using Claude Code to fix this problem OpenCode couldn't solve?

![](https://pic.yupi.icu/1/image-20260107193829543.png)

Let's try! Input prompt:

```markdown
Currently the project's backend AI functionality isn't working
Please refer to https://vercel.com/docs/ai-gateway/getting-started documentation
Help fix the backend to ensure the project runs properly
```

![](https://pic.yupi.icu/1/image-20260107193701784.png)

Claude Code successfully fixed the issue, and it finally worked normally:

![](https://pic.yupi.icu/1/image-20260107194915666.png)

💡 Note: If you encounter AI call network timeout issues, have the AI change the calling baseURL to https://ai-gateway.vercel.sh/v1

Previously, similar tasks using Claude Code/Cursor + GLM took under 10 minutes to complete. This time it took about 20 minutes with back-and-forth before working properly.

This makes me doubt OpenCode's capabilities. And it feels like the GLM model became dumber in OpenCode - or is that just my imagination...

No way - everyone's raving about OpenCode. I must be using it wrong!

![](https://pic.yupi.icu/1/image-20260107195050357.png)

### Ultrawork Mode

Remember the `ultrawork` (or `ulw`) cheat code mentioned earlier? Let's try it!

![](https://pic.yupi.icu/1/image-20260107195327425.png)

Entering battle mode:

![](https://pic.yupi.icu/1/image-20260107195346575.png)

You can view sub-agent operation details. First press `Ctrl + x`, then arrow keys to check different agents.

When background tasks complete, there's a notification. Here the "Research Vercel AI SDK Conversation Mode" task is done.

![](https://pic.yupi.icu/1/image-20260107195605772.png)

But guess what? After waiting nearly 10 minutes, the task still wasn't finished...

Looking at this task list - does it really need to be this complex? It even pulled in databases?

![](https://pic.yupi.icu/1/image-20260107200237753.png)

I've lost patience waiting. Just end it!

Apparently, this not-too-complex work doesn't leverage multi-agent advantages well. It's like needing to print one sheet of paper but mobilizing the entire company - some researching paper types, some studying printer status, some exploring optimal printing postures.

## Final Thoughts

After these simple tests, I'm keeping OpenCode under observation for now.

The frontend is indeed excellent, but backend capabilities seem behind Claude Code.

If I just want frontend convenience, why not use Cursor?

![](https://