# Vibe Coding FAQs and Solutions

Hello, I'm Yupi, a former Tencent full-stack developer with over 2 million followers as an [AI programming blogger](https://space.bilibili.com/12890453). I'm also the creator of more than 10 self-developed products, including [AI Navigation](https://ai.codefather.cn) and [Programming Navigation](https://www.codefather.cn).

Previously, I systematically explained various techniques and methods of Vibe Coding. In this article, I will summarize the most common issues and solutions encountered in Vibe Coding practices.

You can treat this as a quick reference manual. When you encounter a problem, check here first—you might find the answer. If it's not here, you can also ask questions on my [Programming Navigation](https://www.codefather.cn/) or discuss with other AI enthusiasts on [Yupi AI Navigation](https://ai.codefather.cn/) to find answers.

## Understanding Vibe Coding Concepts

### What’s the Difference Between Vibe Coding and Traditional Programming?

Answer: Traditional programming involves writing code yourself, while Vibe Coding involves describing your requirements in natural language and letting AI write the code for you. Traditional programming requires you to master the syntax and details of a programming language, whereas Vibe Coding focuses more on expressing requirements and guiding AI. Essentially, both are about solving problems, just in different ways. You can think of traditional programming as cooking your own meal and Vibe Coding as ordering takeout—both get you fed, but the process is entirely different.

### Is Vibe Coding Suitable for Everyone?

Answer: Vibe Coding lowers the barrier to entry for programming, allowing more people to participate in development. However, it’s not a silver bullet. For highly complex systems, performance-critical projects, or scenarios requiring deep optimization, traditional programming might be more appropriate. Vibe Coding is best suited for rapid prototyping, personal projects, small to medium-sized applications, and utility software. If you’re a complete beginner looking to learn programming or quickly turn ideas into products, Vibe Coding is a great fit.

### Do I Still Need to Learn Traditional Programming After Learning Vibe Coding?

Answer: Yes, but there’s no rush. Vibe Coding can help you quickly implement functionality, but to truly understand code, debug complex issues, and optimize performance, you’ll need a foundation in programming. It’s recommended to learn programming knowledge while working on projects with Vibe Coding. This approach is more efficient and motivating. You can start by using AI to complete a few projects, gain confidence, and then systematically learn programming. This is much more interesting than diving straight into textbooks.

### Is AI-Generated Code Reliable?

Answer: AI-generated code works, but it’s not always perfect. It may contain bugs, performance issues, or security vulnerabilities. Therefore, you need to test functionality, review code, and continuously optimize. Don’t blindly trust AI—maintain a critical mindset. Just as you’d check your takeout for freshness, you need to vet AI-generated code. However, as AI models improve, the quality of generated code is getting better.

### Will Vibe Coding Replace Programmers?

Answer: It won’t completely replace programmers, but it will change how they work. Just as calculators didn’t replace mathematicians and search engines didn’t replace librarians, AI programming tools will become assistants rather than replacements for programmers. Future programmers will need stronger product thinking, architectural skills, and problem-solving abilities, not just coding skills. Programmers who can effectively use AI will have a competitive edge over those who can’t.

### What Is AI Hallucination?

Answer: AI hallucination refers to AI fabricating content that doesn’t exist, such as inventing non-existent APIs, incorrect function usage, or libraries that don’t exist. This is an inherent issue with AI models because they generate content based on probabilities. When encountering hallucinations, don’t panic—ask the AI to provide documentation links for verification or check official documentation yourself. If the AI persists with errors, try switching models or starting a new conversation to rephrase the problem.

### What Does MVP Mean?

Answer: MVP stands for Minimum Viable Product, which is the simplest version of a product that can still function. In short, it’s about building the most basic usable version first and then improving it over time. For example, an MVP version of a budgeting app might only include features for adding expenses, viewing lists, and calculating totals, with additional features like categorization, charts, and export added later. The benefit of this approach is that it allows for quick validation of ideas, avoids getting bogged down in details early on, and keeps development momentum.

### What Is a Context Window?

Answer: A context window refers to the amount of content an AI model can "remember" at once, typically measured in tokens. For example, Claude Sonnet 4.5 has a context window of 200K tokens, roughly equivalent to 150,000 Chinese characters. The larger the context window, the more code the AI can process and the longer it can retain conversation history. If your project involves a lot of code, choosing a model with a larger context window, such as Gemini 3 Pro, which supports 1M tokens, is more suitable.

## Choosing Vibe Coding Tools

### Which Is Better: Cursor or Windsurf?

Answer: Both are excellent, each with its own strengths. Cursor has a more mature ecosystem, a larger community, more plugins, and better documentation. Windsurf offers innovative features like Cascade mode and has a more modern interface.

If you’re a beginner, start with Cursor because it’s easier to find solutions to problems. If you like experimenting with new tools, try Windsurf. The best approach is to try both and see which suits your workflow better.

### Is the Free Version Enough?

Answer: For learning and small projects, the free version is sufficient. Cursor’s free version offers a certain monthly quota for specific models. However, for daily work or larger projects, the paid version is recommended. Paid versions offer higher quotas, more powerful models, and a better experience—essentially trading money for time.

You can start with the free version and upgrade if you find it useful. Students can make full use of various free resources, which are more than enough for learning purposes.

### How to Choose an AI Model?

Answer: Choose based on task complexity and budget. Use cheaper models (Gemini Flash, DeepSeek) for simple tasks and more powerful models (Claude Opus, GPT-5) for complex tasks. For front-end UI, Gemini 3 Pro performs well. For full-stack projects, Claude Sonnet is more comprehensive. If budget is tight, domestic models (DeepSeek, Tongyi Qianwen, Zhipu GLM) offer good value for money.

If you’re unsure, use Auto mode to let the tool select automatically, or start with a cheaper model and switch to a stronger one if needed.

### What’s the Difference Between Bolt.new and v0.dev?

Answer: Bolt.new is better suited for full-stack applications, generating complete front-end and back-end code that can be run and debugged directly in the browser. v0.dev focuses more on front-end UI components and excels at generating beautiful interfaces. If you want to quickly build a complete application, use Bolt.new. If you’re just generating UI components, use v0.dev. Both are zero-code platforms that require no software installation—just open your browser and start using them, making them ideal for beginners.

### Where Can I Get an API Key?

Answer: Register an account on the official website of the corresponding service, then generate a key in the settings or API section. For example, OpenAI’s key can be obtained at platform.openai.com, Anthropic’s Claude key at console.anthropic.com, and Supabase’s key in the project settings.

After generating an API key, keep it secure and avoid exposing it in public repositories like GitHub. It’s recommended to manage API keys using environment variables or configuration files to avoid hardcoding them in your code.

### How to Access Cursor and Claude in China?

Answer: Cursor can be accessed directly, but accessing Claude’s official website in China may be difficult. You can use domestic models as alternatives, such as DeepSeek, Tongyi Qianwen, and Zhipu GLM, which are already close to international models in programming capabilities, offer faster access, and are cheaper. If you must use Claude, consider using the API or accessing it through services like OpenRouter.

### What AI Modes Does Cursor Offer?

Answer: Cursor 2.0 primarily offers two AI interaction modes:

1. Chat Mode: Dialogue mode, suitable for asking questions, explaining code, and making minor modifications.
2. Agent Mode: The most powerful mode, capable of autonomously planning and executing complex tasks, modifying multiple files simultaneously, and supporting parallel execution of multiple agents.

If you just want to ask questions or modify a single function, Chat Mode is sufficient. For adding new features or modifying multiple files, Agent Mode is more suitable. Agent Mode also supports Plan Mode, which generates a plan for your confirmation before executing changes.

### Should I Choose a Zero-Code Platform or a Code Editor?

Answer: If you’re a complete beginner or just want to quickly create a prototype, use a zero-code platform (Bolt.new, v0.dev). If you’re working on a complex project, need more control, or want to learn programming, use a code editor (Cursor, Claude Code, etc.).

Zero-code platforms are simple and fast but offer limited flexibility. Code editors are more powerful but have a steeper learning curve. It’s recommended to start with a zero-code platform to get a feel for it, then move on to a code editor.

## Vibe Coding Usage Tips

### What to Do If AI-Generated Code Has Errors?

Answer: Copy the complete error message and relevant code to the AI for analysis and fixes. Make sure to include the full error stack, not just a single line. If the AI can’t fix it, try switching to a stronger model or start a new conversation to rephrase the problem. You can also consult documentation or search for solutions in communities or forums. Often, the error message itself contains clues to the solution—learning to read error messages is crucial.

### What If AI Keeps Using the Wrong Tech Stack?

Answer: Clearly state your tech stack at the beginning of each conversation. For example, "I’m using React + TypeScript + Tailwind CSS, please implement using these technologies."

Alternatively, configure a `.cursorrules` file in your project to let the AI automatically know your tech stack.

If the AI still uses the wrong stack, interrupt and correct it: "I’m using React, not Vue, please rewrite using React!"

After emphasizing this multiple times, the AI will remember.

### How to Make AI-Generated Code Match Project Style?

Answer: Provide reference code for the AI to mimic. For example, "Please write the new component in the style of this component," then paste the reference code.

Alternatively, include detailed code style guidelines in the context file, covering naming conventions, component structure, comment formats, etc.

You can also provide a code style document for the AI to follow.

Most importantly, the more specific your prompts, the better—don’t just say "make it look good," specify what "good" means.

### What If AI-Generated Code Has Poor Performance?

Answer: Use tools like Chrome DevTools or Lighthouse to identify performance bottlenecks, then ask the AI to optimize accordingly. For example, "This list renders slowly, please optimize with virtual scrolling," or "This function is slow with large datasets, please optimize the algorithm."

Don’t aim for perfect performance from the start—get the functionality working first, then optimize bottlenecks. In most cases, AI-generated code performs adequately.

### How to Handle AI Hallucinations?

Answer: If the AI invents a non-existent API, ask it to provide documentation links. If it can’t, it’s a hallucination—ask it to use the correct API.

If the AI gets stuck in a loop, cut off the context and start a new conversation.

If the AI insists on an incorrect solution, try switching models or verify with official documentation.

When encountering uncertain content, always verify—don’t blindly trust AI. It’s essential to develop the habit of consulting documentation—it’s a fundamental skill for programmers.

### How to Debug AI-Generated Code?

Answer: Use breakpoint debugging instead of relying solely on console.log.

Set breakpoints in the browser or editor, step through the code, and inspect variable values. This gives you a clearer view of the code’s execution. If you still can’t find the issue, share the error message and code with the AI for analysis. You can also ask the AI to add detailed logs to help you understand the code’s execution flow.

Debugging is a crucial skill for programmers—it’s worth investing time to learn.

### How to Improve Prompt Quality?

Answer: Prompts should be specific, clear, and structured. Don’t say "build a website," say "build a budgeting website with features for adding expenses, viewing lists, and calculating totals, using a blue color scheme and a clean, modern style."

Break prompts into sections: functional requirements, interface requirements, technical requirements. You can also provide reference examples, such as "Interface style inspired by Notion."

Remember, the more detailed your prompts, the more aligned the AI’s output will be with your expectations.

### What If AI-Generated Code Is Too Verbose?

Answer: Ask the AI to refactor the code, extract repetitive parts, and simplify logic. For example, "This code is too long, please refactor it by extracting common functions and reducing repetition," or "Please implement this functionality in a more concise way."

AI generally prioritizes making code functional over making it concise, so you need to explicitly request optimization. However, don’t over-pursue conciseness—readability is more important.

### How to Get AI to Explain Code?

Answer: Asking the AI to explain code helps you understand and learn faster. You can directly ask, "What does this code do? Please explain in detail," or "What’s the logic behind this function? Why was it written this way?" The AI will explain in plain language.

If the explanation is too simple, say, "Please explain in more detail, including the purpose of each step." If it’s too complex, say, "Please explain in simpler terms, I’m a beginner," or even "I’m a dummy"—the results might surprise you.

### How to Handle Outdated AI-Generated Code?

Answer: AI training data may lag, so it sometimes generates code for older versions. Clearly tell the AI to use the latest version, such as "Please use the latest React 19 syntax," or "Please use Next.js 15’s App Router." Always provide the AI with the latest official documentation.

### How to Include Code Examples in Prompts?

Answer: Wrap the code in triple backticks and paste it into the prompt. For example:

````markdown
Please reference the style of this code:

```jsx
Code content
```
````

If the code is long, include only the key parts. You can also use the AI code editor’s `@` feature to let the AI read files from your project, such as "Please reference the style of @src/components/Button.tsx." Providing code examples helps the AI better understand your requirements.

### What If AI Keeps Generating Repetitive Code?

Answer: Remind the AI to extract common functions or components. For example, "This code has a lot of repetition, please extract it into a common function," or "Please create a reusable component." You can also explicitly state in the prompt, "Avoid repetitive code, follow the DRY principle." If the AI still generates repetitive code, manually refactor it or try switching models.

### How to Handle Unsafe AI-Generated Code?

Answer: Ask the AI to review the code for security issues. For example, "Please check this code for security vulnerabilities," or "Add input validation to prevent XSS attacks." You can also use security scanning tools, such as ESLint’s security plugins for front-end code. For sensitive operations (e.g., user authentication, payments), be extra cautious—consult experienced developers or use multiple advanced AI models for cross-validation.

## Vibe Coding Project Development

### What If My Project Code Becomes Messy Halfway Through?

Answer: Refactor promptly—don’t wait until it’s a complete mess. After completing each feature, spend 10–15 minutes organizing the code. Extract repetitive code, split large functions, improve naming, and add comments.

If it’s already messy, let the AI help with refactoring, but proceed incrementally and test each step. For example, "Please refactor this file by extracting common functions," rather than "Refactor the entire project." Take small steps and improve gradually.

Note: Always commit your current code with Git before refactoring! This allows you to revert to the previous version if refactoring goes wrong. Frequent commits are the best way to protect your code.

### How to Deploy an AI-Generated Project?

Answer: Many zero-code platforms (e.g., Bolt.new) support one-click deployment—just press a button to go live.

For manual deployment, use platforms like Vercel or Netlify. Push your code to GitHub, connect the repository on the platform, and it will automatically build and deploy.

If you need a backend, use BaaS services like Supabase. If you need your own server, consider Docker containerized deployment.

In short, deployment isn’t that hard—just follow Yupi’s deployment tutorials step by step.

### What If There’s a Bug After Launch?

Answer: First, use monitoring tools (Sentry, LogRocket) to collect error information. Then, reproduce the issue locally to identify the cause.

After fixing, have the AI write more tests to ensure the issue doesn’t recur. Finally, deploy the fix as soon as possible.

To avoid this, thoroughly test before launch, including functional testing, edge case testing, and testing on different devices. Ask friends to help test—they often catch issues you might miss.

### How to Turn a Toy Project into a Real Product?

Answer: Consider many aspects, such as:

1. Improve error handling to account for various exceptions.
2. Add tests to ensure stability.
3. Optimize performance for faster operation.
4. Enhance security to protect user data.
5. Improve UI for better usability.
6. Write documentation for easier maintenance and expansion.
7. Consider SEO, monitoring, logging, etc.

This is a continuous improvement process—don’t aim for perfection from the start. Improve incrementally, and gradually, it will become a real product.

### How to Evaluate Project Quality?

Answer: Don’t just focus on functionality—quality is equally important. A project with complete functionality but full of bugs is worse than a simple project that’s stable and reliable.

Evaluate based on these aspects:

- Functional completeness (does it meet all requirements?)
- Code quality (is it clear and maintainable?)
- Performance (loading speed, responsiveness)
- Security (are there vulnerabilities?)
- User experience (is it easy to use?)
- Test coverage (are there sufficient tests?)

### What If AI Can’t Solve a Problem?

Answer: First, don’t stick to one AI—try switching models, as different models excel in different areas.

If you have programming experience, check the official documentation—it’s the most authoritative source.

Alternatively, search for