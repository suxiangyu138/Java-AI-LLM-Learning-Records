# TRAE - AI Learning Hero Mini Program Project

This is a Learning Hero WeChat Mini Program project that helps users learn any topic through gamification. Users input the topic they want to learn, and the AI automatically generates related knowledge Q&A cards, allowing learning through multiple-choice questions.

This is a pure Vibe Coding project, mainly explaining how to use the TRAE AI programming tool to quickly develop WeChat Mini Programs. By describing requirements in natural language, the AI automatically generates complete frontend and backend mini program code, and utilizes TRAE's integrated database and payment services. Suitable for those who want to learn AI development for mini programs or the TRAE tool.

---

Hello everyone, I'm programmer Yupi. Have you ever experienced this: wanting to learn some new technology, but getting a headache as soon as you open the documentation, getting distracted when watching videos, or buying a bunch of courses but always giving up halfway...

Anyway, this happens to me often—I'm a late-stage learning anxiety patient.

![](https://pic.yupi.icu/1/image-20250928214315214.png)

As a programmer, I've long wanted to create a program to cure my learning anxiety, but I kept procrastinating because it seemed troublesome. Now that AI programming capabilities are powerful enough, I've finally taken action! Using TRAE, I completed this "Learning Hero" mini program in just 1 day, making learning as easy and fun as playing a game~

![](https://pic.yupi.icu/1/%E5%B0%8F%E7%A8%8B%E5%BA%8F%E6%BC%94%E7%A4%BA%E6%8B%BC%E5%9B%BE.png)

Next, I'll first take you through the experience of this mini program, then share the tools and techniques used throughout the development process—another **step-by-step AI project development tutorial**.

Bookmark this, and let's get started!

> Recommended video version: https://bilibili.com/video/BV1yMn3zuE7L

## Project Experience

Opening the mini program, you'll see a very simple and vibrant interface. Click "Start Learning," then enter any topic you want to learn, such as Java basics.

![](https://pic.yupi.icu/1/image-20250928215038368.png)

The AI will automatically generate related knowledge Q&A cards based on the topic.

![](https://pic.yupi.icu/1/image-20250928215106136.png)

You just need to click through multiple-choice questions. Don't worry about making mistakes—each question comes with an explanation, so even if you've never heard of the topic before, you can keep learning.

![](https://pic.yupi.icu/1/image-20250928215137518.png)

Besides technical knowledge, you can try more topics, like a vocabulary word, a movie, or even a person.

![](https://pic.yupi.icu/1/%E5%A4%9A%E7%AD%94%E9%A2%98%E6%8B%BC%E5%9B%BE.png)

Originally dull concepts become lively and interesting through Q&A, instantly eliminating learning anxiety~

You can also check your learning history to review or see explanations.

![](https://pic.yupi.icu/1/%E5%AD%A6%E4%B9%A0%E5%8E%86%E5%8F%B2%E6%8B%BC%E5%9B%BE.png)

Browse through it occasionally, and you'll become a learning hero!

## Development Implementation

In the past, this kind of mini program might have taken several days to develop.

But now, using TRAE IDE's AI programming, I'll show you how to create this mini program **without writing a single line of code**.

Steps:
1. Prepare development tools
2. Requirements analysis
3. Solution design
4. Service integration
5. Backend development
6. Frontend development
7. Testing and validation
8. Continuous optimization

### 1. Prepare Development Tools

Since we're using AI for development, tool selection is crucial. This time, I used the AI programming tool [TRAE](https://www.trae.ai/) because its SOLO mode has been incredibly popular lately, and I wanted to try it.

Unlike traditional human-led + AI-assisted programming, SOLO mode lets **AI take the lead and automatically execute development**. You just need an idea, and with AI's help, you can bring it to life.

![](https://pic.yupi.icu/1/image-20250928220322788.png)

Additionally, since we're developing a frontend project, we'll need the [Node.js environment](https://nodejs.org/zh-cn/download). And since it's a WeChat Mini Program, we'll need the [WeChat Developer Tools](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html). Just download and install them from the official websites.

![](https://pic.yupi.icu/1/%E4%B8%8B%E8%BD%BD%E5%B7%A5%E5%85%B7%E6%8B%BC%E5%9B%BE.png)

### 2. Requirements Analysis

With the tools ready, enter TRAE's SOLO mode and open a project folder cleaner than my face.

![](https://pic.yupi.icu/1/image-20250928220647365.png)

First, we need to analyze requirements.

Don't overcomplicate it—just describe your idea to the AI in natural language.

For example, I gave the AI this requirement:

```markdown
You are a professional programmer. Please help me develop the "Learning Hero - AI Q&A Guided Learning" WeChat Mini Program.

Users can set a topic they want to learn (or test), and the AI will generate several interesting knowledge Q&A cards around the topic, guiding users to master knowledge more easily and enjoyably through quiz-style challenges.
```

![](https://pic.yupi.icu/1/image-20250928221154635.png)

The AI quickly generated detailed product requirement documents and technical architecture documents, following standard enterprise development processes.

![](https://pic.yupi.icu/1/image-20250928221128338.png)

The AI seems eager to start coding, but don't rush—**carefully review the requirement documents first**.

The AI's output is good but might not fully match our expectations. So, manually focus on the core features to implement, remove unnecessary additional features, and ensure the core business process (P0 requirements) works first.

![](https://pic.yupi.icu/1/image-20250928221334652.png)

**Don't skip this step—spending an extra minute now can save an hour later!**

Clearly define requirements to prevent the AI from building unnecessary features.

Here's a tip: use TRAE's integrated Figma design tool to get free product UI prototypes.

![](https://pic.yupi.icu/1/image-20250928221519374.png)

You can view specific prototype designs. Just select the prototype you like, click "Add to Conversation," and TRAE will automatically associate the prototype with the AI's dialogue. This cross-product interaction is pretty cool (though those who've learned project development with me probably know how it works).

![](https://pic.yupi.icu/1/image-20250928221618668.png)

But life needs surprises, so I'll let the AI improvise and see what creativity emerges~

### 3. Solution Design

Next, we'll design the solution—a task once reserved for architects earning tens of thousands per month, now handled by our little AI SOLO.

When writing this prompt, pay attention to details and **follow the minimal development principle** to prevent the AI from overcomplicating things.

```markdown
I've manually adjusted the product requirement document, removing many unnecessary features. Please regenerate the technical architecture document based on my revised requirements. Requirements:
1. Do not add any features not mentioned in the requirement document
2. Follow the minimal development principle—focus on functionality, avoid extensions like deployment, monitoring, or rate limiting
3. Follow the frontend-backend separation principle
```

The AI quickly generated a complete technical architecture document, including frontend and backend technologies, interface design, database schema, etc.

![](https://pic.yupi.icu/1/image-20250928221924096.png)

I recommend those who understand the document to leverage their expertise, clarify specific technology choices, and keep the AI's generated code within your control. For example, I specified using Supabase for the database and OpenRouter to connect with Gemini for AI services.

![](https://pic.yupi.icu/1/image-20250928221952548.png)

Huh? What are these?

Don't worry—we'll explain them soon.

![](https://pic.yupi.icu/1/image-20250928222144713.png)

If you don't understand the document, no worries. Imagine yourself as a boss or product manager—your programmer colleague hands you a technical proposal, and you say, "I don't care how it's done, just get this feature live tomorrow!" Let the AI do its thing.

Trust the AI—believe in the power of belief~

### 4. Service Integration

After designing the solution, before diving into development, we need to prepare the project's dependent services.

Where will user data be stored? How will the program connect to AI models?

These are problems we need to solve.

Instead of manually installing services, we can use TRAE's integration capabilities for a plug-and-play setup without reading official documentation.

We'll focus on integrating two services.

![](https://pic.yupi.icu/1/image-20250928222329915.png)

#### Integrate Supabase

[Supabase](https://supabase.com/) is an open-source Backend-as-a-Service (BaaS) platform offering database storage, user authentication, instant APIs, and more, helping developers quickly build and manage program backends.

![](https://pic.yupi.icu/1/image-20250928222457606.png)

Click the connect button, then complete account creation, organization setup, and authorization on the pop-up page.

![](https://pic.yupi.icu/1/image-20250928222520491.png)

Return to TRAE, refresh after the organization appears, click "Create New Project," fill in some configuration details, and click "Create."

![](https://pic.yupi.icu/1/image-20250928222545479.png)

After creating the project, return to TRAE, refresh, and click "Connect"—it's that simple!

![](https://pic.yupi.icu/1/image-20250928222608471.png)

If AI Vibe Coding made backend developers ecstatic, this is a win for frontend developers—simple projects don't even need manual backend setup anymore~

#### Integrate OpenRouter AI Service

TRAE can integrate with various AI services. Here, I chose [OpenRouter](https://openrouter.ai/), which offers the advantage of connecting to multiple major models (e.g., Gemini, GPT, Claude) through a unified API.

![](https://pic.yupi.icu/1/image-20250928222649319.png)

First, register an account on the official site, then create an API key on the API Keys page. Configure and enter the key in TRAE, and the AI service is integrated.

![](https://pic.yupi.icu/1/image-20250928222833273.png)

But note: ensure you have sufficient usage limits, or calls may fail or be rate-limited.

![](https://pic.yupi.icu/1/image-20250928222853247.png)

#### Integrate Stripe Payment Service

You can also integrate the [Stripe payment service](https://docs.stripe.com/), which lets you add payment and subscription features with minimal code.

![](https://pic.yupi.icu/1/image-20250928223013523.png)

Just register an account on the official site—it automatically provides a sandbox testing environment and corresponding API keys. You can create products and set pricing.

![](https://pic.yupi.icu/1/image-20250928223151134.png)

Enter this info into TRAE's configuration, and the AI will generate payment-related code later.

![](https://pic.yupi.icu/1/image-20250928223346093.png)

Due to WeChat Mini Program restrictions, I won't integrate this for now, but it's great for web and app products.

### 5. Backend Development

With preparations complete, we finally enter the exciting development phase.

Note: since **AI has limited context**, to ensure complete project generation and reduce bugs, it's best to proceed step-by-step: first generate backend code, manually verify it, then generate frontend code.

Input the prompt to develop the backend first, ensuring the project runs:

```markdown
Based on the latest product requirement and technical architecture documents, prioritize backend development to ensure the project runs properly.
```

We can use TRAE's prompt optimization feature to refine the prompt.

![](https://pic.yupi.icu/1/%E4%BC%98%E5%8C%96%E6%8F%90%E7%A4%BA%E8%AF%8D.png)

Indeed, it's more precise now.

Click "Execute," and let the AI SOLO begin~ The AI first provides a task plan:

![](https://pic.yupi.icu/1/image-20250928224232897.png)

Then it autonomously operates the terminal to execute commands, writes backend configuration and business logic code, creates database tables, etc. Important operations require our confirmation—very meticulous.

![](https://pic.yupi.icu/1/image-20250928224300188.png)

If you don't understand, let it do its thing~

While waiting, check out [free programming learning paths](https://codefather.cn/). TRAE has built-in notifications, so it'll alert us when tasks complete.

TRAE seems well-trained—it verifies program functionality itself. Since we haven't filled in WeChat login details yet, incomplete API calls are normal.

![](https://pic.yupi.icu/1/image-20250928224350332.png)

After a while, the AI finishes generating not just code but also helpful backend API documentation.

![](https://pic.yupi.icu/1/image-20250928224437599.png)

This is quite useful.

### 6. Frontend Development

Now for frontend development.

Important: don't continue writing prompts in the same conversation.

Why?

Because AI models have limited context, and previous operations have consumed much of it. To prevent context exhaustion or confusion, start a fresh conversation for frontend generation.

![](https://pic.yupi.icu/1/image-20250928224735250.png)

Provide the AI with product requirements, technical architecture, and backend API docs to focus on frontend code.

```markdown
You are a professional programmer. Please help me develop the "Learning Hero - AI Q&A Guided Learning" WeChat Mini Program.

Users can set a topic they want to learn (or test), and the AI will generate several interesting knowledge Q&A cards around the topic, guiding users to master knowledge more easily and enjoyably through quiz-style challenges.

Based on @ProductRequirements @TechnicalArchitecture @BackendAPIDocs, generate complete, runnable WeChat Mini Program frontend code.
Notes:
1. Follow the minimal functionality principle—do not develop any features not mentioned in requirements
2. For images, use placeholder picsum.photos (e.g., picsum.photos/200/300)
```

Execute!

While waiting, check out free interview questions and study paths on [Interview Duck](https://www.mianshiya.com/).

After a while, the AI finishes, SOLO-generating over 20 files at once!

![](https://pic.yupi.icu/1/image-20250928224830751.png)

Though impressive, I'm a bit nervous—will it run properly?

### 7. Testing and Validation

Now for the thrilling testing phase.

First, open WeChat Developer Tools, import the existing project folder, and select "Use Test Account" for debugging.

![](https://pic.yupi.icu/1/image-20250928225118198.png)

After opening the project, click the top-right "Test Account" and follow the [documentation](https://developers.weixin.qq.com/miniprogram/dev/devtools/sandbox.html) to get the test AppID and AppSecret:

![](https://pic.yupi.icu/1/image-20250928225220066.png)

Manually enter these into the backend configuration file; otherwise, WeChat login won't work.

![](https://pic.yupi.icu/1/image-20250928225304614.png)

Now compile and run the project.

And... it crashes!

![](https://pic.yupi.icu/1/image-20250928225522618.png)

Expected, expected... Mini program development is trickier than web development, given WeChat's constantly updating tools and docs.

![](https://pic.yupi.icu/1/image-20250928225618743.png)

No worries—development always has hiccups. The solution is simple: **copy the error message and let the AI fix it!**

For example, I encountered several typical issues:

1) Image path issues: Use TRAE's prompt optimization to guide the AI through bug-fixing steps.

![](https://pic.yupi.icu/1/image-20250928225717687.png)

2) Login failures: Click "Details" in the developer tools' top-right corner, go to "Local Settings," and check "Do not verify valid domain names."

![](https://pic.yupi.icu/1/image-20250928225825654.png)

3) API path issues: Likely due to long context. Have the AI globally fix frontend API call paths and parameters.

![](https://pic.yupi.icu/1/image-20250928225903118.png)

4) Environment configuration issues: Inconsistent environment variable names in code and config files. This is simple—we can manually adjust.

After typing one character, the editor auto-suggests code changes, even supporting multi-line edits.

![](https://pic.yupi.icu/1/image-20250928225955681.png)

This is TRAE's CUE feature, which not only auto-completes code but also predicts future edits—perfect for refactoring.

![](https://pic.yupi.icu/1/%E5%A4%9A%E8%A1%8C%E4%BF%AE%E6%94%B9.gif)

After some fixes, our mini program runs smoothly. Though the UI is still rough, once the core workflow functions and users can operate it, further optimizations are easy.

![](https://pic.yupi.icu/1/image-20250928230045861.png)

### 8. Continuous Optimization

Finally, if you want to launch the mini program, spend some time optimizing.

Remember: before optimizing, use Git to version-control the existing code and commit a baseline version. This lets you revert if optimizations go wrong.

![](https://pic.yupi.icu/1/image-20250928230154447.png)

For example, I had the AI optimize the entire mini program's style with a simple prompt:

```markdown
You are a programmer. Optimize the style of every frontend page and element in the mini program for consistency.

Reference style: Vibrant orange