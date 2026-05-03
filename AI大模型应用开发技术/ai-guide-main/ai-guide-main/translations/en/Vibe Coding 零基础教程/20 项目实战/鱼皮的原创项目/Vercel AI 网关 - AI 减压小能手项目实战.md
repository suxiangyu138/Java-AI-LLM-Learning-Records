# Vercel AI Gateway - Hands-on Project for AI Stress Relief

This article focuses not on development, but on learning the concept and usage of AI gateways. Through comic-style explanations and the Vibe Coding project, you'll understand how to flexibly switch between different models in AI applications using the Vercel AI Gateway, reducing development costs and maintenance difficulties. Suitable for those who want to learn AI gateway technology and need to quickly integrate multiple AI models.

---

You are Xiao Aba, a newly hired AI application development engineer.

![](https://pic.yupi.icu/1/1761152281564-61073333-da43-4ac2-b6ca-09460e87331a.png)

Your boss says: The company recently wants to build an intelligent customer service system. Xiao Aba, you're new, so this important task falls on you! Thus, heaven will assign great responsibilities to the newcomer~

![](https://pic.yupi.icu/1/1761645372915-7d9df0cf-f44a-4643-8d67-3e7103fb03db.png)

You think to yourself: Isn't this just a matter of calling an API? What's so hard about it?

So you roll up your sleeves and start coding, first integrating OpenAI's GPT model.

Just as you finish, your boss says: We also need to add the Claude model, I heard it performs better in certain scenarios.

So you write a bunch of code to call the Claude model.

Just as you finish that, your boss says: Hmm, I heard the domestic Tongyi Qianwen is also good, let's integrate that too!

![](https://pic.yupi.icu/1/1761645406716-99acb82f-2f1f-4a62-a3c6-076ac17b1e6d.png)

You frown, thinking: Now I have to write code to call this model too, boss, aren't you being a bit excessive...

Your boss seems to hear your thoughts:

- Oh, by the way, calling AI has costs, so we need to handle user authentication
- Oh oh, to prevent malicious users from spamming AI calls, we need to implement rate limiting
- Oh oh oh, AI-generated content might have issues, so we need to validate content safety
- Oh oh oh oh! We need to ensure system stability, so if one model fails, the entire service shouldn't go down
- Oh oh oh oh oh!! This project is definitely going to be a hit, so we need to consider how the AI can handle massive requests
- Oh oh oh oh oh oh!!! We need to be able to monitor AI call counts and costs to reduce costs and increase efficiency
- Oh oh oh oh oh oh oh...

You watch your boss gradually go crazy and start questioning your life: Why is calling an AI API so complicated?

![](https://pic.yupi.icu/1/1761645432489-6e2f67db-40f5-40fe-a6f0-c18ead6e6d8a.png)

⭐️ Video version of this article: [https://bilibili.com/video/BV14NyrBTEeB](https://www.bilibili.com/video/BV14NyrBTEeB)

## What is an AI Gateway?

At this moment, Yupi, known as the "AI Prince," walks over and sees your troubled expression. He smiles and says: What, is it that hard?

You feel a bit annoyed: You're just standing there talking without feeling the pain. With so many requirements, don't I have to write a ton of code?

Yupi: All the scenarios the boss mentioned can be solved with an **AI Gateway**~

You ask curiously: Gateway? What's that?

Yupi: A gateway is like the ticket check at a train station. All passengers must first pass through the ticket check, where the ticket inspector checks your ticket and then guides you to the correct platform.

![](https://pic.yupi.icu/1/1761645506547-26d820e6-cda3-482e-a7e9-dcadcbbb0445.png)

In system architecture, requests from front-end users first pass through the gateway. The gateway uniformly handles user authentication, intercepts malicious requests, controls traffic, monitors and counts requests, etc., and then forwards the requests to the backend server for processing.

![](https://pic.yupi.icu/1/1761645542774-a2da29dc-3d2c-4e1b-adcf-dc5dd59d0881.png)

You nod: Wow, so if I have multiple backend services, I don't need to write these functions separately for each service.

Yupi: Exactly, and if one backend service fails, the gateway can automatically forward requests to other services.

![](https://pic.yupi.icu/1/1761645571947-8cb4a141-7d6f-4add-a82b-7a70716f23e7.png)

You become curious: So what's this AI Gateway you mentioned earlier?

Yupi: Traditional API gateways are usually placed between your application and various backend services; AI Gateways are specifically designed for AI applications, placed between your application and various AI model services (like OpenAI, Tongyi Qianwen, DeepSeek, etc.).

![](https://pic.yupi.icu/1/1761645615469-787296d8-1fb6-4452-b406-c79ef537f193.png)

Your application only needs to send **standard requests** to the AI Gateway, which will automatically handle user authentication, rate limiting, security protection, failover, load balancing, monitoring, and statistics, and then forward the requests to the AI model for processing.

![](https://pic.yupi.icu/1/1761645642401-683e786e-3e06-420a-abce-cd43f7bfa901.png)

If you need to integrate different models, you only need to modify the model name in the standard request, and the AI Gateway will handle the routing for you, without needing to write separate integration code for each model~

![](https://pic.yupi.icu/1/1761645657980-b463d0b2-eecd-4635-99ee-fed9f121bf4e.png)

You cheer: This is amazing! With the AI Gateway, all the problems the boss mentioned can be solved! So what AI Gateway products are available now?

![](https://pic.yupi.icu/1/1761645679096-3ceb73df-a858-4410-abc1-041222b497f4.png)

## AI Gateway Selection

The first AI Gateway many AI enthusiasts encounter might be [OpenRouter](https://openrouter.ai/), which is more like an AI model aggregation platform, supporting integration with hundreds of models through a unified interface.

![](https://pic.yupi.icu/1/1761645719834-eadcde16-711c-474c-9838-5ea5f01be452.png)

Many AI tools support configuring OpenRouter. For ordinary AI users, it allows access to more models and more stable services.

![](https://pic.yupi.icu/1/1761645743475-53493cb1-5be9-4b0b-8b9a-a497ac13d7c7.png)

You ask: Are there any AI Gateway products specifically for developers?

Yupi nods: Of course, there are already many mature AI Gateway products on the market. For example, a quick search online will show the following top ones:

1) [Vercel AI Gateway](https://vercel.com/ai-gateway): This is a very popular new product, with the biggest feature being easy to get started and **zero markup**. If you use your own API Key, the gateway itself doesn't charge extra fees. Suitable for front-end developers to quickly build full-stack AI applications.

2) [Cloudflare AI Gateway](https://www.cloudflare.com/zh-cn/developer-platform/products/ai-gateway/): Cloudflare, as one of the largest CDN service providers globally, its AI Gateway mainly benefits from wide global node coverage and strong security protection.

3) [Kong AI Gateway](https://konghq.com/products/kong-ai-gateway): Kong itself is a mature API gateway, now enhanced specifically for AI scenarios, with comprehensive enterprise-level features.

![](https://pic.yupi.icu/1/image-20251018111819481.png)

4) [Higress AI](https://higress.ai/): An open-source AI Gateway from Alibaba Cloud, supporting unified protocol conversion for over 100 models, providing enterprise-level features like semantic caching, token rate limiting, MCP conversion, suitable for enterprises with complex AI integration needs.

![](https://pic.yupi.icu/1/image-20251019163817976-20251028181254777.png)

You scratch your head: This seems too complicated, how should I get started with AI Gateways?

Yupi: Don't worry, we can start learning with the relatively simple Vercel AI Gateway. Talk is cheap, give me 2 minutes, and I'll show you how to master the usage of Vercel AI Gateway~

## Vercel AI Gateway Hands-on

#### 1. Register and Get API Key

First, go to the [Vercel website](https://vercel.com/) to register an account. Binding a bank card gives you a free $5 usage quota, which is enough for learning and testing.

![](https://pic.yupi.icu/1/1760687990497-90720fbb-0df6-4ede-87b8-64b8702994e9-20251028181254840.png)

Then create an API Key in the console, be careful not to leak it:

![](https://pic.yupi.icu/1/1760688078133-7b91b6f3-2fc4-4bb4-b2c1-d517699f0968-20251028181254879.png)

#### 2. Official Demo

Next, you can follow the official quick start tutorial to create a project and run the AI chat Demo:

![](https://pic.yupi.icu/1/image-20251019160232722.png)

Simply put, there are 4 steps:

1. Create a new project
2. Install AI SDK and AI Gateway dependencies
3. Configure environment variables, fill in API Key configuration information
4. Write example Demo code

#### 3. Stress Relief Project

However, the official Demo is a bit too simple. Let's use AI to build a "Stress Relief Expert" website project, allowing users to chat with an AI specifically designed to relieve stress.

Here, I chose Cursor as the AI development tool, directly letting AI generate complete front-end and back-end code that meets the requirements.

![](https://pic.yupi.icu/1/1761645829262-bd9950ef-6334-410f-ab27-140873cb56a4.png)

Since Vercel AI Gateway is relatively new, AI might not understand its usage, so I directly threw the Vercel AI Gateway official documentation to Cursor, letting it learn the usage through the documentation.

![](https://pic.yupi.icu/1/1761645854330-54b6ab38-7f1f-45a5-ac34-463ee63477a7.png)

The complete prompt is as follows:

```markdown
You are a professional programmer, please help me develop the "Stress Relief Expert" website, where users can relieve stress by chatting with an AI specifically designed to help with stress relief.

## Development Requirements

1. Need to include complete front-end and back-end, back-end uses Node.js
2. Use Vercel's AI Gateway to implement AI capabilities, need to first get as much usage as possible from the official documentation: https://vercel.com/docs/ai-gateway/getting-started
3. Aim to complete core functionality, ensure the project can run normally, no need to output documentation, nor do any extra features
4. The overall website interface uses relaxing light colors, responsive to various device sizes
```

After clicking execute, AI will first call the MCP tool to get information from the webpage. Here I used `Firecrawl MCP`:

![](https://pic.yupi.icu/1/1760690503357-7412db47-4389-49e2-b42c-dc6eabdc283b-20251028181255024.png)

About 6 minutes later, AI completed generating all the code. Unfortunately, AI wasn't very obedient here and still generated a bunch of documentation, taking more time than generating the code itself!

![](https://pic.yupi.icu/1/image-20251019161322770.png)

Then create a `.env` environment variable configuration file in the root directory, fill in the AI Gateway API Key:

![](https://pic.yupi.icu/1/1760689844332-79459841-46e3-4356-9fe7-76062a90464c-20251028181255097.png)

Finally, install dependencies and execute the startup script:

![](https://pic.yupi.icu/1/1760690024471-f89bf1e3-3d9e-4857-8a68-a236ffa0af14-20251028181255125.png)

Visit `localhost:3000` to see the project:

![](https://pic.yupi.icu/1/1760690223277-56f89c2a-b4c5-43d6-a61d-3f744bcd9aef-20251028181255161.png)

Honestly, the effect is really good, giving me a bit more motivation to write a few more words haha:

![](https://pic.yupi.icu/1/1760690245830-e877986b-1bb6-4c6a-96ad-92b738a57fed-20251028181255188.png)

You exclaim: With AI + AI Gateway combo, developing AI projects is so fast!

Yupi nods: Exactly, and throughout the process, we don't need to worry about a model failing, the gateway will handle these issues automatically.

![](https://pic.yupi.icu/1/1761645903883-94c22619-2cac-41a4-87c3-d51ec96586e0.png)

#### 4. More Features

Moreover, Vercel AI Gateway supports many domestic and international models, with different billing standards for each model:

![](https://pic.yupi.icu/1/image-20251019165234650.png)

You can also configure your own model API Key:

![](https://pic.yupi.icu/1/image-20251019162655475.png)

Additionally, it provides **observability** and other enterprise-level features, helping you understand AI call situations and cost analysis:

![](https://pic.yupi.icu/1/1760691345180-9ce7ec43-c651-4c19-a918-90b87034b7fe-20251028181255384.png)

Your eyes light up: Wow, then I'll use it for all my AI projects from now on!

Yupi shakes his head helplessly: Xiao Aba, remember "there is no silver bullet."

If your personal project only needs to call a single AI model, directly calling the API is enough; if your personal or team's small project needs AI Gateway features (like integrating multiple models), then you can choose Vercel AI Gateway; if you're developing enterprise-level applications with high security and stability requirements, choosing more professional gateways like Higress or Kong is more suitable, helping you write a little less crappy code 💩!

![](https://pic.yupi.icu/1/1761645922466-4284b8ac-7928-424e-a0eb-09629e7e66a1.png)

## Ending

A few months later, you refactored the company's intelligent customer service system using the AI Gateway, with excellent results.

You sigh: This is standing on the shoulders of giants! Indeed, don't reinvent the wheel, laziness drives world progress!

![](https://pic.yupi.icu/1/1761645939970-276a3391-2f40-4b85-8d23-d36c5a491f8c.jpeg)

Yupi coos: So this is your excuse for being too lazy to give me a thumbs up???

![](https://pic.yupi.icu/1/1761646182922-0edead80-b8e9-4b8f-a90d-e35fa8712f3b.png)

## Recommended Resources

1) Yupi AI Navigation Website: [Comprehensive AI Resources, Latest AI News, Free AI Tutorials](https://ai.codefather.cn)

2) Programming Navigation Learning Circle: [Learning Paths, Programming Tutorials, Practical Projects, Job Hunting Guide, Q&A](https://www.codefather.cn)

3) Programmer Interview Cheatsheet: [Internship/Campus/Social Recruitment High-Frequency Exam Points, Enterprise Real Questions Analysis](https://www.mianshiya.com)

4) Programmer Resume Writing Tool: [Professional Templates, Rich Example Sentences, Direct to Interview](https://www.laoyujianli.com)

5) 1-on-1 Mock Interview: [Essential for Internship/Campus/Social Recruitment Interviews to Get Offers](https://ai.mianshiya.com)