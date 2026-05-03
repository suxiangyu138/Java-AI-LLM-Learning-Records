# TRAE SOLO: AI-Driven Full-Stack Development Tool

Hi, I'm Yupi.

In previous articles, we explored various AI programming tools. Whether it's Cursor or Claude Code, they all follow a **human-led + AI-assisted** model where you constantly need to converse with, confirm, and modify the AI's output.

But what if you want the AI to take more initiative, planning tasks and executing development automatically? What tool should you use?

In this article, I'll introduce TRAE SOLO, a full-stack development tool that lets AI take the lead in development tasks.

## 1. What is TRAE SOLO?

[TRAE](https://www.trae.ai/) is an AI-native programming tool launched by ByteDance, offering two development modes:

- IDE Mode: Similar to Cursor, maintaining traditional development workflows with human leadership
- SOLO Mode: AI takes the lead to automatically advance development tasks

SOLO Mode means **AI leads the task and executes development automatically**. You just need an idea, and by working with AI, you can bring that idea to life.

![](https://pic.yupi.icu/1/image-20250928220322788.png)

What's the difference between TRAE SOLO and Cursor?

To use an analogy: Using Cursor is like you being the project manager and AI being the programmer—you constantly assign tasks, check results, and provide feedback. With TRAE SOLO, AI is both the project manager and programmer—you just state the goal, and it will plan, develop, and test on its own.

According to the latest 2026 evaluations, TRAE performs exceptionally well among AI programming tools, especially its customizable agent system that allows users to define and configure AI agents with different roles and skills based on project needs, much like assembling a team.

## 2. Core Features of TRAE SOLO

### 1. Automatic Documentation Generation

TRAE SOLO automatically generates:

- Product Requirement Documents (PRD)
- Technical Architecture Documents
- API Documentation
- Test Reports

These documents adhere to enterprise-standard development processes and are highly professional.

### 2. Service Integration Capabilities

TRAE offers powerful integration capabilities, enabling seamless connections to various services:

- Supabase: Database storage and user authentication
- Stripe: Payment functionality
- OpenRouter: AI services
- Figma: Design prototypes

No need to read official documentation—just a few clicks in TRAE to complete integrations.

![](https://pic.yupi.icu/1/image-20250928222329915.png)

### 3. Multi-Task Parallel Execution

TRAE SOLO supports parallel task execution, allowing simultaneous frontend and backend development to significantly improve efficiency.

### 4. Code Change Tools

TRAE provides DiffView, a code change tool that clearly shows what code was modified in each update, making review and rollback easier.

### 5. Plan Mode

Beyond direct execution, TRAE also supports Plan Mode. The AI first generates a detailed execution plan for your approval before proceeding, giving you better control over the development process.

## 3. How to Use TRAE SOLO?

Let me demonstrate TRAE SOLO's workflow with a practical example.

⭐️ Recommended: Watch Yupi's video tutorial for a more detailed explanation: https://www.bilibili.com/video/BV1yMn3zuE7L

### Preparation

First, prepare the development tools:

- Download and install [TRAE](https://www.trae.ai/)
- Install the [Node.js environment](https://nodejs.org/zh-cn/download)
- For mini-program development, also install [WeChat Developer Tools](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)

![](https://pic.yupi.icu/1/%E4%B8%8B%E8%BD%BD%E5%B7%A5%E5%85%B7%E6%8B%BC%E5%9B%BE.png)

### 1. Requirements Analysis

With tools ready, enter TRAE's SOLO Mode and open a blank project folder.

![](https://pic.yupi.icu/1/image-20250928220647365.png)

First, we conduct requirements analysis. Don't overcomplicate it—just describe your idea in natural language to the AI.

For example, I gave the AI this requirement:

```
You are a professional programmer. Please help me develop the "Learning Hero - AI Q&A Guided Learning" WeChat mini-program.

Users can set a topic they want to learn (or test), and the AI will generate several fun knowledge Q&A cards around that topic, guiding users to master knowledge more easily and enjoyably through a quiz-style challenge.
```

![](https://pic.yupi.icu/1/image-20250928221154635.png)

The AI quickly generated detailed product requirement and technical architecture documents, aligning well with enterprise-standard development processes.

![](https://pic.yupi.icu/1/image-20250928221128338.png)

The AI seems eager to start coding, but don't rush—**carefully review the requirement documents first**.

The AI's output is good but may not fully match our expectations. Focus manually on core functionalities, remove unnecessary additional features, and ensure the core business process (P0 requirements) works first.

![](https://pic.yupi.icu/1/image-20250928221334652.png)

**Don't skip this step—spending an extra minute now saves an hour later!** Clearly define requirements to prevent the AI from building unwanted features.

A handy trick: Use TRAE's integrated Figma design tool to access free UI prototypes.

![](https://pic.yupi.icu/1/image-20250928221519374.png)

Click to view specific prototype designs. Just select the one you like, click "Add to Conversation," and TRAE will automatically associate the prototype with the AI dialogue.

![](https://pic.yupi.icu/1/image-20250928221618668.png)

### 2. Solution Design

Next, we move to solution design—traditionally the job of architects earning tens of thousands monthly—now handled by our little AI SOLO.

When writing this prompt, pay attention to details and **follow the minimal development principle** to prevent the AI from overcomplicating simple tasks.

```
I've manually adjusted the product requirement document, removing many unnecessary features. Please regenerate the technical architecture document based on my revised requirements. Requirements:
1. Do not add any features not mentioned in the requirement document
2. Follow the minimal development principle—focus on implementing core functionalities only, avoiding extras like deployment, monitoring, or rate limiting
3. Follow the frontend-backend separation principle
```

The AI quickly generated a complete technical architecture document, covering frontend/backend technologies, API design, database schema, etc.

![](https://pic.yupi.icu/1/image-20250928221924096.png)

For those who understand the document, I recommend leveraging your expertise to specify technical choices, keeping the AI's code within your control. For example, I explicitly chose Supabase for the database and OpenRouter to connect with Gemini for AI services.

![](https://pic.yupi.icu/1/image-20250928221952548.png)

If you don't understand the document, don't worry. Imagine yourself as the boss or product manager—your programmer colleague hands you a technical proposal, and you just say, "I don't care how you do it—this feature goes live tomorrow!" Let the AI do its thing.

Trust the AI—believe in the power of belief.

### 3. Service Integration

After solution design, before formal development begins, we need to prepare project dependencies.

Where to store user data? How to connect the program to AI models? These are problems we need to solve.

Instead of manually setting up services, use TRAE's integration capabilities to connect services without reading official documentation—just a few clicks.

![](https://pic.yupi.icu/1/image-20250928222329915.png)

#### Integrate Supabase

[Supabase](https://supabase.com/) is an open-source Backend-as-a-Service (BaaS) platform offering database storage, user authentication, instant APIs, and more.

![](https://pic.yupi.icu/1/image-20250928222457606.png)

Click the connect button, then complete Supabase account creation, organization setup, and authorization on the pop-up page.

![](https://pic.yupi.icu/1/image-20250928222520491.png)

Back in TRAE, after the organization appears, click to create a new project, fill in configuration details, and hit create.

![](https://pic.yupi.icu/1/image-20250928222545479.png)

After creating the project, return to TRAE, refresh, and click connect—it's that simple.

![](https://pic.yupi.icu/1/image-20250928222608471.png)

If AI Vibe Coding made backend developers ecstatic, this feature is a win for frontend developers—simple projects don't even need manual backend setup.

#### Integrate OpenRouter AI Services

TRAE integrates with various AI services. Here, I chose [OpenRouter](https://openrouter.ai/), which offers a unified API to connect with major models like Gemini, GPT, Claude, etc.

![](https://pic.yupi.icu/1/image-20250928222649319.png)

First, register an account on the official site, then create an API key for AI calls in the API Keys section. Configure and enter the key in TRAE, and the AI service is ready.

![](https://pic.yupi.icu/1/image-20250928222833273.png)

Note: Ensure you have sufficient usage quotas, or calls may fail due to rate limits.

![](https://pic.yupi.icu/1/image-20250928222853247.png)

### 4. Backend Development

With preparations complete, we finally enter the exciting development phase.

Note: Due to **limited AI context**, to generate a complete project with fewer bugs, split the process: First generate backend code, manually verify it, then generate frontend code.

Input a prompt to develop the backend first, ensuring the project runs:

```
Based on the latest product requirement and technical architecture documents, prioritize backend development to ensure the project runs correctly.
```

Use TRAE's prompt optimization feature to refine the prompt with one click.

![](https://pic.yupi.icu/1/%E4%BC%98%E5%8C%96%E6%8F%90%E7%A4%BA%E8%AF%8D.png)

Click execute—let the SOLO begin. The AI first provides a task plan:

![](https://pic.yupi.icu/1/image-20250928224232897.png)

Then it autonomously operates the terminal to execute commands, writes backend configuration and business logic code, creates database tables, etc., even seeking confirmation for critical operations—very meticulous.

![](https://pic.yupi.icu/1/image-20250928224300188.png)

If you don't understand, let it do its thing.

While waiting, check out free programming learning paths at [Programming Navigation](https://codefather.cn/). TRAE has built-in notifications to alert you when tasks complete.

TRAE seems well-trained—it validates whether the program runs correctly.

![](https://pic.yupi.icu/1/image-20250928224350332.png)

After some time, the AI finishes, generating not just code but also neatly organized backend API documentation.

![](https://pic.yupi.icu/1/image-20250928224437599.png)

### 5. Frontend Development

Now for frontend development.

Important: Don't continue writing prompts in the same dialogue.

Why? Because AI models have limited context, and previous operations have consumed much of it. To prevent context exhaustion or confusion, start a fresh dialogue for frontend generation.

![](https://pic.yupi.icu/1/image-20250928224735250.png)

Provide the AI with product requirements, technical architecture, and backend API docs in the prompt to focus on frontend code generation.

```
You are a professional programmer. Please help me develop the "Learning Hero - AI Q&A Guided Learning" WeChat mini-program.

Users can set a topic they want to learn (or test), and the AI will generate several fun knowledge Q&A cards around that topic, guiding users to master knowledge more easily and enjoyably through a quiz-style challenge.

Based on @ProductRequirementDocument @TechnicalArchitectureDocument @BackendAPIDocument, generate complete, runnable WeChat mini-program frontend code.
Notes:
1. Follow the minimal functionality principle—do not develop any features not mentioned in the requirement document
2. For images, use placeholder picsum.photos (e.g., picsum.photos/200/300)
```

Execute!

While waiting, check out free interview questions and study paths at [Interview Duck](https://www.mianshiya.com/).

After some time, the AI finishes, SOLO-generating over 20 files at once!

![](https://pic.yupi.icu/1/image-20250928224830751.png)

It looks impressive, but honestly, I'm a bit nervous—will it actually run?

### 6. Testing and Validation

Now comes the thrilling testing phase.

First, open WeChat Developer Tools, import the project folder, and select the test account for debugging.

![](https://pic.yupi.icu/1/image-20250928225118198.png)

After opening the project, click the "Test Account" button in the top-right corner and follow the [documentation](https://developers.weixin.qq.com/miniprogram/dev/devtools/sandbox.html) to obtain the test AppID and AppSecret:

![](https://pic.yupi.icu/1/image-20250928225220066.png)

Manually enter these into the backend configuration file; otherwise, WeChat login won't work.

![](https://pic.yupi.icu/1/image-20250928225304614.png)

Now compile and run the project.

And... it crashes!

![](https://pic.yupi.icu/1/image-20250928225522618.png)

Expected, expected. Mini-program development is trickier than web development, given WeChat's constantly updating tools and docs.

![](https://pic.yupi.icu/1/image-20250928225618743.png)

No worries—issues are inevitable in development. The solution is simple: **Copy the error message and let the AI fix it!**

Here are some typical issues I encountered:

1) Image path problems: Use TRAE's prompt optimization to guide the AI through step-by-step bug fixes.

![](https://pic.yupi.icu/1/image-20250928225717687.png)

2) Login failures: Click "Details" in the developer tools, go to local settings, and check "Do not verify valid domain names."

![](https://pic.yupi.icu/1/image-20250928225825654.png)

3) API path issues: Likely due to long context. Have the AI globally fix frontend API call paths and parameters.

![](https://pic.yupi.icu/1/image-20250928225903118.png)

4) Environment configuration mismatches: Inconsistent variable names between code and config files. This is simple—we can manually edit.

After typing one character, the editor auto-suggests changes, even supporting multi-line edits.

![](https://pic.yupi.icu/1/image-20250928225955681.png)

This is TRAE's **CUE feature**, which not only auto-completes code but also predicts future edits—perfect for refactoring, boosting efficiency.

![](https://pic.yupi.icu/1/%E5%A4%9A%E8%A1%8C%E4%BF%AE%E6%94%B9.gif)

After some fixes, our mini-program runs. Though the UI is rough, once the core workflow functions and users can operate it, further optimizations are easy.

![](https://pic.yupi.icu/1/image-20250928230045861.png)

### 7. Continuous Optimization

Finally, if you plan to launch the mini-program, spend time optimizing.

Remember: Before optimizing, use Git to version-control the current code and commit a baseline. This lets you roll back if optimizations go wrong.

![](https://pic.yupi.icu/1/image-20250928230154447.png)

For example, I had the AI optimize the mini-program's overall style with a simple prompt:

```
You are a programmer. Please optimize the style of every frontend page and element in the mini-program for visual consistency.

Reference style: Vibrant orange as the primary color, fresh and soothing cartoon style, simple yet elegant, creating a relaxed and pleasant feel.
```

Using TRAE's prompt optimization yields a more detailed plan.

![](https://pic.yupi.icu/1/image-20250928230225960.png)

Adjust as needed or let the AI handle it.

I recommend committing code after each optimization or new feature and occasionally starting fresh AI dialogues to prevent context overload.

The final mini-program you see is my optimized version—not bad, right?

![](https://pic.yupi.icu/1/image-20250928230316272.png)

## 4. Pros and Cons of TRAE SOLO

TRAE SOLO's advantages are clear.

First, **AI-led development** means you don't constantly converse with the AI—it plans and executes tasks automatically. Plus, **strong service integration** enables easy connections to Supabase, Stripe, etc., without reading docs.

It also **auto-generates documentation** aligned with enterprise standards. And the **localized version is fast**, catering to Chinese users.

Of course, there are limitations.

First, **it needs guidance**—clearer requirements yield better results. Also, **generated code may have bugs**, requiring manual testing and fixes.

Additionally, **context management is crucial**. Long dialogues may confuse the AI, necessitating fresh starts.

Pricing-wise, TRAE offers free and paid versions. The free tier has limits; the paid version bills by usage.

## 5. TRAE SOLO Practical Tips

If you want to try TRAE SOLO, here are my suggestions:

1. Define Requirements Clearly

Though the AI can plan autonomously, clearer requirements yield better results. Recommend:

- Specify core functionalities
- Remove unnecessary features
- Provide reference designs (if available)
- Define technical choices (if you understand them)

2. Develop Step-by-Step

Don't generate the entire project at once. Instead:

- Generate backend first, test it
- Then generate frontend, test it
- Finally, optimize styles and details

Commit code after each step for easy rollback.

3. Fix Bugs Promptly

AI-generated code may have bugs—that's normal. Fix issues immediately; don't let them pile up.

Provide complete error messages to the AI—it usually can fix them.

4. Manage Context

Long dialogues reduce AI accuracy. Recommend:

- Start fresh dialogues after major features
- Reference prior documents in new dialogues
- Avoid overloading single dialogues

## Final Thoughts

By now, you should fully understand TRAE SOLO.

**TRAE SOLO represents a major evolution in AI programming tools**, shifting from "integrating AI into tools" to "integrating development tools into AI."

![](https://pic.yupi.icu/1/image-20250928220158367.png)

It feels like development tools are just toys for the AI—it freely operates