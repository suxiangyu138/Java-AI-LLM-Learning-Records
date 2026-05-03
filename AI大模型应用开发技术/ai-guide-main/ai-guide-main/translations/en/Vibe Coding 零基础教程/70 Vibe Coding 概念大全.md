# Vibe Coding Concept Encyclopedia

> Understand all the core terms of AI programming at once



Hello, I'm Yupi, a former Tencent full-stack developer, an [AI programming blogger](https://space.bilibili.com/12890453) with 2 million followers across platforms, and the creator of over 10 self-developed products like [AI Navigation](https://ai.codefather.cn) and [Programming Navigation](https://www.codefather.cn).

While learning Vibe Coding, you will inevitably encounter various unfamiliar terms and concepts. For example, what is a Token? What is a context window? What is RAG? These concepts may sound lofty, but they are actually not difficult to understand.

This article is your **AI programming terminology dictionary**. I will explain the most common and important concepts in Vibe Coding in the simplest and most understandable language. You can bookmark it and refer to it whenever you encounter an unfamiliar term.



## AI Basic Concepts


### Artificial Intelligence (AI)

Artificial Intelligence (AI) is the technology that enables computers to simulate human intelligence. Simply put, it allows machines to think, learn, and solve problems like humans.

In Vibe Coding, AI is your programming assistant. You tell it what to do, and it writes the code for you. It's like having a 24/7 online programmer friend who is always ready to help.




### Large Language Model (LLM)

Large Language Model (LLM) is an AI system capable of understanding and generating human language. ChatGPT, Claude, Gemini, and DeepSeek are all large language models.

Why are they called "large"? Because these models have an enormous number of parameters, often ranging from billions to trillions. The more parameters, the smarter the model usually is, but it also consumes more computational resources.

You can think of a large language model as a super scholar who has read vast amounts of books and code. It has seen countless programming cases, so it can help you write code, explain code, and fix bugs.

![](https://pic.yupi.icu/1/1745384872015-f84a47fc-0925-4797-9dfc-e4dfae01a3fa.png)



### Model Parameters

Parameters are the "knowledge points" learned by the model during training, stored in the model in numerical form. The more parameters, the richer the knowledge the model can remember, and usually, the smarter it is.

For example:

- GPT-4 has about 1.8 trillion parameters
- Claude 3.5 Sonnet's parameter count is undisclosed but estimated to be in the hundreds of billions
- DeepSeek-V3 has 671 billion parameters

The number of parameters affects the model's capabilities and operational costs. Generally, models with more parameters are more expensive but also more effective.




### Training and Inference

Training is the process of teaching an AI model from large amounts of data. This process requires massive computational resources and time, typically handled by AI companies. You don't need to train the model yourself.

Inference is the process of using the learned knowledge to answer questions or generate content after the model has been trained. When you chat with ChatGPT, you are performing inference.

To put it simply: training is like a student going to school to study, while inference is like a student taking an exam. Our daily use of AI tools relies on inference capabilities.




### Fine-tuning

Fine-tuning involves continuing to train an existing model with domain-specific data to improve its performance in a particular field.

For example, you can fine-tune a model with extensive medical data to make it a medical expert. Or fine-tune it with your company's codebase to make it more familiar with your project's style.

For regular users, fine-tuning is costly and usually unnecessary. Using pre-trained models is sufficient.



## Token and Billing


### Token

Token is the basic unit of text processing for AI models. You can simply think of it as a "word chunk."

In English, one Token is roughly equivalent to a word or part of a word. In Chinese, one character is usually 1-2 Tokens.

Why is Token important? Because AI services typically charge based on Tokens. Both the text you input and the text AI outputs consume Tokens. The more Tokens used, the more money you spend.

For example:

- "Hello World" is about 2 Tokens
- “你好世界” is about 4-6 Tokens

![](https://pic.yupi.icu/1/image-20260112112612434.png)



### Input Token and Output Token

AI services usually calculate input and output Tokens separately:

- Input Token: The content you send to the AI (prompts, code, files, etc.)
- Output Token: The content returned by the AI (answers, generated code, etc.)

Generally, output Tokens are more expensive than input Tokens because generating content consumes more computational power than understanding content.

Money-saving tip: Write clear and concise prompts to help the AI understand your needs in one go, reducing the need for repeated conversations.



### Context Window

Context Window refers to the maximum amount of content an AI model can "remember" at once, measured in Tokens.

Different models have different context window sizes:

- GPT-4o: 128K Tokens (about 100,000 Chinese characters)
- Claude 3.5 Sonnet: 200K Tokens (about 150,000 Chinese characters)
- Gemini 2.0 Pro: 2M Tokens (about 1.5 million Chinese characters)

The larger the context window, the more code the AI can handle, and the longer the conversation history it can remember. If your project involves a lot of code, choosing a model with a larger context window is more suitable.

However, note that a larger context window also means more Tokens consumed per request, leading to higher costs.



## Prompt-Related Concepts


### Prompt

Prompt is the instruction or question you give to the AI. In Vibe Coding, the prompt is your natural language description of the requirements.

The quality of the prompt directly determines the quality of the AI's output. A good prompt should:

- Be specific and unambiguous
- Include necessary background information
- Specify the desired output format

For example, "Make a website" is a vague prompt, while "Create an accounting website using React, with features to add expenses, view lists, and calculate totals, with a blue-themed interface" is a good prompt.

In AI conversations, messages are typically divided into three roles:

- System Prompt: Sets the AI's role and behavior rules, invisible to the user
- User Prompt: The message you send to the AI
- Assistant Prompt: The message the AI replies with

Understanding these three roles helps you better structure conversations. For example, when debugging, you can simulate previous conversation history in the prompt to help the AI better understand the context.



### System Prompt

System Prompt is the instruction set before the conversation starts, used to define the AI's role, behavior, and limitations.

For example, you can set a system prompt: "You are a senior React development expert. Please answer questions in a concise and clear coding style."

The system prompt remains effective throughout the conversation and is an important way to customize AI behavior.

![](https://pic.yupi.icu/1/1745462990451-6f2b5727-d47b-436c-9da2-50dac64fb790.png)



### Prompt Engineering

Prompt Engineering is the technique of designing and optimizing prompts to help the AI better understand your intent and generate more expected results.

This is one of the core skills in Vibe Coding. A good prompt engineer can generate higher-quality code with fewer conversation rounds.



### Zero-shot Prompt

Zero-shot Prompt refers to giving the AI a task without providing any examples.

For example: "Please translate this English text into Chinese."

The AI will complete the task based on its training knowledge. For simple tasks, zero-shot prompts are usually sufficient.



### Few-shot Prompt

Few-shot Prompt involves providing a few examples in the prompt to help the AI learn the desired format or style.

For example:

```
Please translate in the following format:
English: Hello → Chinese: 你好
English: Thank you → Chinese: 谢谢
English: Good morning → Chinese:
```

By providing examples, the AI can more accurately understand your needs and produce more consistent results.



### Chain-of-Thought Prompt

Chain-of-Thought Prompt involves making the AI think step by step rather than directly providing the answer. This is particularly effective for complex reasoning tasks.

You can add "Please think step by step" or "Let's think step by step" to the prompt, and the AI will show its reasoning process, often leading to more accurate answers.

In programming, chain-of-thought prompts help the AI better understand complex requirements and generate more reasonable code structures.

![](https://pic.yupi.icu/1/chainofthought.png)



### Markdown Language

Markdown is a lightweight markup language for text formatting using simple symbols. For example, `#` for headings, `**text**` for bold, and `-` for lists.

In Vibe Coding, Markdown is very important because:

- AI-generated responses are usually in Markdown format
- Project documentation (e.g., README) is written in Markdown
- Rule files are also in Markdown format

Learning Markdown helps you communicate better with AI and write more standardized project documentation.




## AI Programming Models


### Vibe Coding

Vibe Coding is a concept proposed by computer scientist Andrej Karpathy in February 2025. It describes a new way of programming: through natural language conversations with AI, letting the AI write code for you, while you only need to describe requirements, test results, and guide directions.

The core idea of Vibe Coding is that you don't need to master programming syntax; you just need to clearly express your ideas. The AI is responsible for turning your ideas into executable code.

It's like ordering takeout: you tell the platform what you want to eat, and the restaurant prepares it and delivers it to you. You don't need to know how to cook, but you need to know what you want to eat.



### Agentic Coding

Agentic Coding refers to making AI work like an autonomous "agent," capable of planning tasks, executing operations, and verifying results, rather than just passively answering questions.

In Cursor's Agent mode, AI can:

- Automatically read and analyze multiple files
- Plan implementation strategies
- Execute code modifications
- Run tests and verify results
- Automatically fix issues

This is more powerful than traditional Q&A-style AI because it can autonomously complete complex multi-step tasks.

![](https://pic.yupi.icu/1/agent-in-cursor.png)



### Multi-Agent Collaboration

Multi-Agent Collaboration refers to multiple AI agents working together to complete complex tasks.

For example, one agent is responsible for designing the architecture, one for writing frontend code, one for backend code, and one for code review. They collaborate like a team.

In recent years, multi-agent systems have become an important trend in AI programming, capable of handling more complex projects.

![](https://pic.yupi.icu/1/image-20260112112834637.png)



### Agent Orchestration

Orchestration is the process of coordinating and managing multiple AI agents or AI tasks to ensure they work in the correct order and manner.

Like a conductor in an orchestra, the orchestrator decides which agent does what at what time, how information is passed, and how results are aggregated.

![](https://pic.yupi.icu/1/image-20260112112854174.png)



### Agent Loop

Agent Loop is the core working mechanism of AI agents, describing how agents continuously operate to complete tasks.

A typical Agent Loop includes:

1. Perception: Acquire current environmental information (read files, check errors, etc.)
2. Thinking: Analyze the situation and decide the next action
3. Action: Execute specific operations (write code, run commands, etc.)
4. Observation: Check the results of the action
5. Loop: Decide whether to continue based on the results

This loop continues until the task is completed or a termination condition is met. Understanding the Agent Loop helps you better use tools like Cursor Agent.




### ReAct Reasoning and Acting

ReAct (Reasoning and Acting) is a technical framework that allows AI agents to alternate between reasoning and acting.

Traditional AI either only thinks or only acts. ReAct enables AI to:

- First reason: Think about the current situation and make a plan
- Then act: Execute specific operations
- Observe results: See how the action worked
- Continue reasoning: Adjust the strategy based on the results

This "think-act-observe" loop allows AI to more reliably complete complex tasks and is one of the core technologies of modern AI programming tools.

![](https://pic.yupi.icu/1/react-agent.png)



### Tool Use

Tool Use (Function Calling) is the technology that allows AI to use external tools and functions. AI itself can only generate text, but through tool use, it can:

- Read and write files
- Execute command-line commands
- Search the web
- Call APIs
- Operate databases

![](https://pic.yupi.icu/1/1746590338968-0240c12b-2956-47f4-b8ff-5b5f831221f6.png)

The workflow of tool use consists of 4 steps:

1. Identify needs: AI determines that the current task requires tool use
2. Select tool: Choose the appropriate tool from available tools
3. Execute call: Call the tool with the correct parameters
4. Integrate results: Incorporate the tool's returned results into the response

![](https://pic.yupi.icu/1/%E5%B7%A5%E5%85%B7%E8%B0%83%E7%94%A8%E6%B5%81%E7%A8%8B.png)

Note that the AI model itself does not directly execute tools but generates instructions like "I want to call this tool with these parameters," which are then executed by external programs and returned to the AI.

In Vibe Coding, tool use allows AI to move from "just talking" to "taking action." For example, Cursor's Agent mode uses tool use to read files, modify code, and run commands.




### Agent Skills

Agent Skills is an open standard launched by Anthropic in October 2025 to extend AI agents' professional capabilities in specific domains.

Simply put, a Skill is a folder containing a `SKILL.md` file, which can include instructions, script code, reference materials, etc. When AI encounters relevant tasks, it automatically loads the corresponding Skill to enhance its capabilities.

![](https://pic.yupi.icu/1/agent%2520skills.jpeg)

You can think of a Skill as a "new employee onboarding guide" for AI. For example:

- A PDF processing Skill teaches AI how to fill out PDF forms
- A project deployment Skill contains your team's unique deployment processes and scripts
- A code review Skill defines your project's code standards and checklists

The core design of Skills is **progressive disclosure**: AI only loads relevant content when needed, avoiding the need to cram all information into the context at once, saving Tokens while maintaining flexibility.

![](https://pic.yupi.icu/1/agent%20skills%20bundling.jpeg)

💡 Want to discover more useful Agent Skills? Visit [Yupi AI Navigation - Skills Encyclopedia](https://ai.codefather.cn/skills) for continuously updated high-quality skills to unleash AI's execution potential.




### A2A (Agent-to-Agent)

A2A (Agent-to-Agent) refers to the protocol or method for AI agents to communicate and collaborate with each other, forming the foundation of multi-agent systems.

Just as humans need language to communicate, AI agents need standardized ways to exchange information, assign tasks, and report results.

A2A protocols enable different AI agents to form teams and collaborate to complete complex tasks.

![](https://pic.yupi.icu/1/a2a-agent.png)



### BMAD Agile AI Development Method

[BMAD-METHOD](https://github.com/bmad-code-org/BMAD-METHOD) (Breakthrough Method of Agile AI-Driven Development) is a systematic framework for AI agent development, aiming to structure and make reusable the otherwise chaotic AI programming process.

BMAD organizes the development process using **role-based agents**, with each agent playing a specific role:

- Analyst Agent: Creates project briefs, including market analysis and user profiles
- PM Agent: Transforms briefs into detailed product requirement documents (PRD)
- Architect Agent: Designs technical implementation plans and system architectures

BMAD agents are divided into two types:

- Simple Agents: Single-file, self-contained, suitable for focused tasks like code review and document generation
- Expert Agents: Possess cross-session persistent memory, equipped with dedicated folders for resources, suitable for complex multi-step workflows

Each agent has standardized components: persona (role, identity, communication style, principles), capability list, interaction menu, and optional key actions.

BMAD has garnered tens of thousands of stars on GitHub, indicating that this structured AI development approach is gaining recognition among developers.

![](https://pic.yupi.icu/1/image-20260201143945594.png)




### Browser Use

Browser Use is the technical capability that allows AI agents to automatically control web browsers. Through Browser Use, AI can browse web pages, click buttons, fill forms, and extract data like a human.

Browser Use's typical applications include:

- Automated research: Let AI search and organize information across multiple websites
- Data collection: Extract structured data from web pages
- Form filling: Automate tedious online form submissions
- Cross-platform operations: Complete multi-step tasks across different websites

A notable open-source project is [Browser-Use](https://github.com/browser-use/browser-use), which supports controlling browsers via Python calls to various large models. Additionally, mainstream AI programming tools like Cursor and Claude Code also have built-in Browser Use capabilities, enabling automatic browser previews, test executions, and more during development.

![](https://pic.yupi.icu/1/image-20251030220841383.png)

A key advantage of Browser Use is that AI can leverage your existing browser sessions and login states, eliminating the need to develop API integrations for each website. This means AI can access websites without public APIs, greatly expanding the scope of automation.




### Computer Use

Computer Use is an AI capability launched by Anthropic in 2024, enabling Claude to operate the entire computer desktop like a human.

Unlike Browser Use, which is limited to browsers, Computer Use can operate any desktop application, such as:

- Viewing screenshots and understanding interface elements
- Moving the mouse cursor and clicking buttons
- Typing with the keyboard
- Executing command-line operations

Computer Use operates through a continuous feedback loop:

1. Screenshot analysis: AI captures and analyzes the current screen
2. Decision planning: Determines the next action based on task goals
3. Action execution: Sends mouse/keyboard inputs
4. Observation: Checks the action's effect and adjusts strategy

💡 For safety, Computer Use typically runs in virtual machines or containers, avoiding direct control of your actual computer.

Computer Use represents a significant leap from "only generating text" to "being able to operate software," fundamentally changing human-computer interaction.

Based on Computer Use technology, Anthropic launched [Claude Cowork](https://claude.com/product/cowork) in 2026, a desktop AI assistant that can directly access files and folders on your computer, helping you organize download directories, extract data from screenshots