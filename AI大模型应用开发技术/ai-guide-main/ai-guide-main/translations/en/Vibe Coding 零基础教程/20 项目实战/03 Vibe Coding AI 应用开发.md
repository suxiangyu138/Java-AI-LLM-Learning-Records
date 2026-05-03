# Vibe Coding AI Application Development

Hello, I'm Yupi.

Today, AI is no longer a distant high-tech concept but a tool that every developer can easily use. By calling AI APIs, you can quickly create various intelligent applications, such as chat assistants, writing assistants, image generators, and more. These projects are not only fun but also highly practical. You can use them directly or showcase them as portfolio projects.

In this article, I will guide you through building 4 popular AI applications: an AI Chat Assistant, a Smart Writing Assistant, an AI Image Generator, and a Speech Recognition App.

Before we start, it's important to note that this tutorial focuses more on guiding you through the thought process and project development workflow. The goal is to help you learn how to develop projects using Vibe Coding. You will need to practice on your own. If you need more comprehensive Vibe Coding tutorials with images and videos, you can check out Yupi's original project实战 section.

## 1. Basics of AI Application Development

Before diving into the projects, let's first understand the basics of AI application development.

An AI application leverages AI capabilities (such as text generation, image generation, speech recognition, etc.) to solve real-world problems. You don't need to train AI models; you just need to call existing AI APIs. It's like driving a car—you don't need to know how the engine works; you just need to know how to drive.

Currently, there are many mainstream AI API services. For text generation, there's OpenAI's GPT-4, Anthropic's Claude, Google's Gemini, and domestic models like Tongyi Qianwen, Wenxin Yiyan, and Zhipu AI. For image generation, there's DALL-E 3, Midjourney, Stable Diffusion, and domestic models like Wenxin Yige. For speech recognition, there's OpenAI's Whisper, Google Speech-to-Text, and iFlytek's speech recognition.

The development process for AI applications is similar to that of regular applications, with the added step of calling AI APIs. The overall workflow is: User Input → Process Input → Call AI API → Process AI Response → Display Results.

Sounds simple, right? Indeed, the barrier to entry for AI application development is now very low.

![](https://pic.yupi.icu/1/aiapp-workflow%E5%A4%A7.jpeg)

When developing AI applications, there are a few things to keep in mind.

First, API Key security: Do not expose your API Key in front-end code, as others can misuse it. Second, cost control: AI APIs charge based on usage, so manage costs to avoid waste. Third, error handling: API calls can fail, so handle errors gracefully and provide user-friendly prompts. Lastly, user experience: AI responses may take a few seconds, so provide loading indicators to let users know processing is underway.

## 2. Project实战 - AI Chat Assistant

The AI Chat Assistant is the most basic yet practical AI application. Through this project, you will learn how to quickly develop a complete AI conversation application using Vibe Coding.

This project aims to implement a complete chat interface where users can input questions, and the AI provides answers. It should support multi-turn conversations, allowing the AI to remember previous content for coherent dialogue. For a better user experience, the AI's responses should be displayed character by character rather than all at once, known as streaming output. Conversation history should be saved locally, so it doesn't disappear on page refresh. Additionally, there should be a clear conversation function to start new topics easily.

![](https://pic.yupi.icu/1/demoweb6.png)

For the tech stack, we'll use React + TypeScript + Vite as the front-end framework and Tailwind CSS for styling. AI capabilities will be implemented by calling large model APIs, conversation history will be saved using LocalStorage, and AI responses will be rendered using react-markdown to support features like code highlighting.

### Development Steps

1) Preparation

The first step in development is preparation. You need to obtain an API Key to call the AI model. Visit the [Zhipu AI Open Platform](https://bigmodel.cn), enter the user console, and click on API Key to get one. Zhipu AI's GLM model performs well and offers free quotas, making it suitable for learning.

2) Write the Requirements Document

With the API Key, you can start writing the requirements document. Create a `PRD.md` file to clearly define what you want to achieve:

```markdown
# AI Chat Assistant PRD

## Core Features
1. Users can input questions, and the AI provides answers
2. Supports multi-turn conversations; the AI remembers previous dialogue
3. AI responses are displayed character by character, not all at once
4. Conversation history is saved locally and persists on page refresh
5. Users can clear conversations to start new topics
6. AI responses support Markdown formatting, including code highlighting

## UI Requirements
- Chat interface similar to WeChat
- User messages on the right, AI messages on the left
- Input box and send button at the bottom
- Clean and modern design style
```

This document clearly outlines the functionality and interface requirements.

3) Write the Technical Design Document

Next, write the technical design document `TECH_DESIGN.md`:

```markdown
# Technical Design

## Tech Stack
- React + TypeScript + Vite
- Tailwind CSS
- Zhipu AI API
- LocalStorage for conversation history
- react-markdown for Markdown rendering

## Data Structure
- Message: role (user or assistant), content, timestamp
- Conversation history stored in LocalStorage

## API Calls
- Use Zhipu AI Chat Completions API
- Enable stream mode for streaming output
- Store API Key in environment variables
```

This document specifies the technologies used, the data structure, and how API calls will be made.

4) Write the AGENTS.md File

Next, create an `AGENTS.md` file to outline development guidelines for the AI:

```markdown
# AI Chat Assistant Development Instructions

## Project Overview
An AI Chat Assistant developed using React + TypeScript, leveraging Zhipu AI API for dialogue functionality.

## Development Guidelines
- Use TypeScript for type safety
- Use Tailwind CSS for styling
- Read API Key from environment variables; do not hardcode
- Provide friendly error messages

## Feature Requirements
- Implement streaming output; AI responses should be displayed character by character
- Support multi-turn conversations; send historical messages to the API
- Save conversation history in LocalStorage
- Support Markdown rendering and code highlighting

## Notes
- Handle API call failures
- Provide clear loading indicators
- Disable the input box during sending to prevent duplicate submissions
```

This file serves as the development guideline for the AI, informing it how the code should be written.

5) Develop with AI Dialogue

With these three documents in place, you can start developing with AI dialogue. Open Cursor and place these three documents in the project root directory.

The first step is to initialize the project:

```
Please initialize a React + TypeScript + Vite project based on the requirements in PRD.md, TECH_DESIGN.md, and AGENTS.md, and install necessary dependencies: Tailwind CSS, react-markdown, react-syntax-highlighter.
```

This prompt tells the AI what project to create and which dependencies to install. The AI will read these three documents and create the project structure, install dependencies, and configure Tailwind CSS accordingly.

The second step is to create data types and API wrappers:

```
Create a types.ts file to define the Message type. Then create an api.ts file to wrap the Zhipu AI API calls, supporting streaming output and reading parameters from environment variables.
```

This step lays the foundation for subsequent development by encapsulating data structures and API calls, making them easier to use later.

The third step is to implement the chat interface:

```
Create a ChatInterface component to implement the chat interface. Requirements:
1. Display the message list at the top, with user messages on the right and AI messages on the left
2. Include an input box and send button at the bottom
3. Use Tailwind CSS to achieve a WeChat-like chat interface style
4. Messages should support Markdown rendering
```

This prompt specifies the layout and styling requirements for the interface. The AI will create a complete chat interface component.

The fourth step is to implement the conversation functionality:

```
Implement the message sending functionality:
1. Add user messages to the message list after input
2. Call the API to get the AI's response
3. Use streaming output to display the AI's response character by character
4. Send historical messages to the API to enable multi-turn conversations
5. Disable the input box during loading and display "Thinking..."
```

This prompt includes all the requirements for the conversation functionality. Streaming output is key, as it makes the AI's response appear character by character like typing, enhancing the user experience. Multi-turn conversations are also important; sending previous dialogue history to the API allows the AI to remember earlier content.

The fifth step is to add data persistence:

```
Use LocalStorage to save conversation history:
1. Automatically save after each conversation update
2. Read historical records on page load
3. Add a "Clear Conversation" button to delete history
```

This ensures that users' previous conversations remain intact even after refreshing the page.

The sixth step is to optimize the user experience:

```
Optimize the user experience:
1. Automatically scroll to the bottom when new messages appear
2. Add a copy button to easily copy the AI's response
3. Enable syntax highlighting for code blocks
4. Display friendly error messages when API calls fail
```

These small details can significantly enhance the user experience.

### Development Tips

During development, there are a few tips to help you work more efficiently. First, don't ask the AI to implement all features at once. Break it down into smaller steps, testing each step as you go. This makes it easier to locate and fix issues if they arise.

Second, provide sufficient context when conversing with the AI. Clearly tell it what functionality to implement, specific requirements, and guidelines to follow. The clearer the context, the higher the quality of the code generated by the AI.

If you encounter issues with the code, share the complete error message with the AI so it can analyze and fix the problem. Don't just say "the code has an error"; paste the specific error message.

### Extension Ideas

Once the basic version is complete, you can extend its functionality.

For example, add system prompt settings to let the AI play different roles (e.g., programming assistant, writing assistant, psychological counselor); support multiple conversation sessions to handle multiple topics simultaneously; add voice input functionality to converse with the AI via voice; integrate image recognition to allow the AI to answer questions based on images; support exporting conversation history to save important dialogues.

## 3. Project实战 - Smart Writing Assistant

After mastering the basic workflow of AI dialogue, let's build a more professional application—the Smart Writing Assistant. Writing assistants are one of the most practical use cases for AI. Through this project, you will learn how to use prompt engineering to have the AI complete different writing tasks.

This project will support various writing modes, such as article continuation, content rewriting, copywriting generation, email drafting, and more. Users can adjust parameters like creativity level and output length to control the AI's generation results. Additionally, users can optimize text grammar and expression with a single click to make the text more professional. For convenience, multiple versions can be generated at once, allowing users to choose the most satisfactory one. Generated content should be saved to a history log for easy review and management.

![](https://pic.yupi.icu/1/demoweb7.png)

The tech stack is similar to the chat assistant, using React + TypeScript + Vite with Tailwind CSS. The core functionality still relies on calling the Zhipu AI API, with data saved in LocalStorage.

### Development Steps

1) Design Prompt Templates

The first step in development is to design prompt templates. Before writing code, design prompts for different writing tasks. Create a `prompts.md` file to define prompts for various writing modes:

```markdown
# Writing Assistant Prompt Templates

## Article Continuation
Please continue the following article, maintaining consistent style and coherent content:
[User input]

## Content Rewriting
Please rewrite the following content to make it more fluent and professional:
[User input]

## Content Expansion
Please expand the following content by adding more details and examples:
[User input]

## Content Summarization
Please summarize the following content, extracting key points:
[User input]

## Email Drafting
Please write a [formal/friendly] email with the subject: [Subject]

## Copywriting Generation
Please write a [style] marketing copy for [Product Name]
```

Prompts are the core of AI applications. Good prompts lead to better AI-generated results. A good prompt typically includes three elements: a clear task description, sufficient context, and clear output format requirements.

![](https://pic.yupi.icu/1/promptcompare%E5%A4%A7.jpeg)

For example, if you want the AI to write an article, don't just say "help me write an article." Instead, specify the topic, target audience, word count, and content to include. For example:

```markdown
Please write a popular science article about AI programming, targeting beginners, with easy-to-understand language and plenty of examples. The article should be 800-1000 words and cover what AI programming is, why to learn it, and how to get started.
```

This way, the AI knows exactly what to write.

If you're rewriting content, provide context for the AI. For example:

```markdown
This is the opening paragraph of a technical blog. Please rewrite it to be more engaging while maintaining professionalism.
```

This helps the AI understand the direction to take.

Specifying the output format is also important. If you want the AI to summarize an article, clearly state the format:

```markdown
Please summarize this article in the following format:
1. Key points (3-5)
2. Key data (if any)
3. Conclusion (one sentence)
```

This ensures the generated result is structured and meets your needs.

Sometimes, providing examples can yield better results. For instance, if you want the AI to write marketing copy, provide a few examples for it to reference. This approach, known as Few-shot Learning, is effective in many scenarios.

2) Write the Requirements Document

After designing the prompt templates, write the requirements document. Create a `PRD.md` file:

```markdown
# Smart Writing Assistant PRD

## Core Features
1. Split layout with input on the left and generated results on the right
2. Supports 6 writing modes: continuation, rewriting, expansion, summarization, email, and copywriting
3. Allows adjusting creativity level (temperature) and output length
4. Generated content should be displayed via streaming
5. Includes an "Optimize Prompt" button for the AI to refine user descriptions
6. Saves generation history

## UI Requirements
- Top: Large title
- Input Area: Large text box (multi-line)
- Parameter Area: Size selection, style selection
- Button Area: Optimize Prompt, Generate Content
- Display Area: Shows generated content + Copy button
- Sidebar: History log
```

3) Develop with AI Dialogue

With the documents ready, you can start developing with AI dialogue.

First, create the basic interface:

```
Please create the Smart Writing Assistant interface based on PRD.md:
1. Split layout
2. Left: Mode selection dropdown + Input text box + Generate button
3. Right: Display generated content
4. Use Tailwind CSS for a visually appealing interface
```

Second, implement the prompt templates:

```
Create a promptTemplates.ts file to implement prompt functions for the 6 writing modes based on the templates in prompts.md. Each function should take user input and return a complete prompt.
```

This encapsulates the prompt logic, with different writing modes using different prompts.

Third, implement the content generation functionality:

```
Implement the content generation functionality:
1. User selects a mode, inputs content, and clicks Generate
2. Generate the corresponding prompt based on the selected mode
3. Call the Zhipu AI API with streaming output
4. Display the generated content in real-time on the right
5. Support Markdown rendering
```

Fourth, add parameter adjustments:

```
Add an advanced settings panel:
1. Creativity slider (temperature: 0-1)
2. Output length slider (max_tokens: 100-2000)
3. Pass these parameters to the API call
4. Allow collapsing/expanding advanced settings
```

The temperature value affects the creativity of the output.

- 0-0.3: Conservative, suitable for factual content
- 0.4-0.7: Balanced, suitable for most scenarios
- 0.8-1.0: Highly creative, suitable for creative writing

Fifth, add history logging:

```
Implement the history logging functionality:
1. Save each generation to LocalStorage
2. Save content: Prompt, generated result, generation time
3. Display the history log list on the left
4. Clicking a history log allows viewing previous content
5. Support deleting history logs
```

### Prompt Optimization Tips

If you're unsure whether your prompt is good enough, let the AI optimize it for you. For example, you can ask:

```
I want the AI to help me rewrite an article to make it more professional. My current prompt is: "Please rewrite this paragraph." Please optimize this prompt to help the AI generate better results.
```

The AI will provide a more detailed and effective prompt, such as:

```markdown
This is the opening paragraph of a technical blog. Please rewrite it to be more engaging while maintaining professionalism. Requirements:
1. Use more vivid language
2. Maintain technical accuracy
3. Keep the word count around 200
```

### Extension Ideas

Once the basic version is complete, you can extend its functionality. For example, add more writing templates to support different types of writing like essays, reports, and novels; allow custom prompt templates for users to create their own; add multi-language translation functionality for one-click translation; implement batch generation to produce multiple versions for selection; add text comparison functionality to compare differences between versions.

## 4. Project实战 - AI Image Generator

Moving from text generation to image generation, let's build an even cooler AI application. AI image generation is one of the most exciting AI applications, allowing you to generate stunning images with simple text descriptions. This project will teach you how to call image generation APIs.

The core of this project is text-to-image generation. Users input a description, select size and style, and click Generate to get an image. To help users write better prompts, include an "Optimize Prompt" feature where the AI expands simple descriptions into detailed prompts. Generated images should be saved to a history log and support downloading.

![](https://pic.yupi.icu/1/demoweb8.png)

The tech stack is similar to previous projects, primarily calling image generation APIs. Zhipu AI also provides image generation capabilities, and you can use the same API Key. [Visit the official site to get it](https://bigmodel.cn).

### Development Steps

1) Understand Image Generation APIs

The first step in development is to understand image generation APIs. The main parameters for image generation APIs include:

- prompt: Image description (most important)
- size: Image size (e.g., 1024x1024)
- quality: Quality (standard or high)
- style: Style