# Practical Guide to Technology Selection

> Choose the right technology, achieve twice the result with half the effort

Hello everyone, I'm Yupi. Today I'll be sharing about **technology selection**, a must-know topic for friends in technical roles and a core task during the preliminary preparation stage when developing products.

In this article, I'll combine my practical experience in product development to share: What is technology selection? What does technology selection involve? Why do we need technology selection? And how to do technology selection well?

Whether you're using Vibe Coding for personal projects or aiming to build a real product, mastering technology selection methods can save you from many detours.

## 1. What is Technology Selection?

The term "technology selection" might sound sophisticated, but it essentially means "choosing technologies" - selecting which technologies to use for project implementation. For example, using HTML for web development or C++ for Windows desktop applications.

To draw an analogy: if developing a project is like leading troops into battle, technology selection would be choosing weapons before the battle. You need to comprehensively select the most suitable weapons based on your own troops, the enemy's forces, and their battle formations to achieve victory at minimal cost.

Note that technology selection isn't just done during the initial project planning and design phases. Whenever we add new features to a project, we may need to select new technologies; as your project expands, you might also need to upgrade the original technology stack.

## 2. What Does Technology Selection Involve?

"Which technologies to use for project implementation" sounds simple. As mentioned above, using HTML for web development can be summarized with one technical term.

But in reality, this is just the surface level. Technology selection isn't easy and involves many layers and details.

From simple to complex, technology selection includes:

1) Which category of technology to choose?
   Like programming languages, development frameworks, data storage, caching, queues, etc.

2) Which specific technology to choose?
   Like choosing Java, Go, or C++ for programming language? Spring or Play for development framework? Redis or Memcached for caching?

3) Which version of the technology to choose?
   Like Java 8 or 11? Vue 2 or Vue 3? Redis 5 or 6? Different versions of the same technology often have significant differences.

4) Which specific features of the technology to use?
   Like Spring's AOP aspect or Redis's GEO advanced data structures.

From this perspective, technology selection is somewhat complicated. Generally, the larger the project scale, the more cautious technology selection tends to be, and the longer the cycle.

I remember when I was building a BI visualization project from scratch at Tencent, just the technology selection took several weeks, with in-depth and comprehensive comparisons of mainstream data storage technologies both domestically and internationally.

Since technology selection can consume so much time and energy, why do we still need to do it?

## 3. Why Do Technology Selection?

I believe many friends who haven't worked yet have never systematically done technology selection.

This is normal because during the learning phase, everyone follows online tutorials for projects. What technologies to use, which versions, even what code to write - all are pre-planned by the instructor.

![](https://pic.yupi.icu/1/image-20230331134500590-20230706153049690-20230706153117049-20230706163222417.png)

But actually, it's not that technology selection wasn't done - the instructor did it for you. When instructors choose which technologies to use for projects, they're essentially doing technology selection, considering factors like market demand and technology popularity.

So why do technology selection?

The answer is simple: to **better** develop and maintain projects.

"Better" here could mean improved efficiency, cost savings, enhanced development experience, increased project scalability, etc.

Imagine when developing projects in a corporate team: if the leader (or architect) chooses a technology only they're familiar with that others don't know at all, will the project progress quickly? If choosing a cheap, poorly-made cloud database to save small costs, will the project perform well?

Before developing a **complete project**, if we thoughtlessly decide on a technology and start coding, we might encounter problems later. It's like Yupi choosing cheap tires for his electric bike - it works fine at first, but one day it blows out halfway, leaving him stranded...

Here's a personal example from my university days. I led a team to develop a campus forum website using React for frontend. Initially smooth, I quickly built dozens of pages. But one day when needing to implement post page state preservation, I discovered React doesn't have built-in keep-alive (component state caching) like Vue Router. It took a long time to find a similar component, which was full of bugs!

Moreover, the more invasive a technology is to project code (like development frameworks), the higher the switching cost later. In my example above, after building dozens of pages, switching frameworks would be very troublesome. Sometimes introducing new components or libraries might conflict with existing dependencies, causing the project to fail. Then you must choose between switching old technologies or spending more effort finding new ones that don't conflict. Worse, to maintain compatibility with outdated frameworks, you might avoid any new technologies, having to develop everything yourself... One wrong step leads to many more!

These are all problems from not doing technology selection or doing it poorly, highlighting its necessity.

Looking back, it was indeed due to lack of experience. If I had considered this initially, confirming all potentially needed technologies and choosing appropriate ones, I could have reduced later risks and saved much time.

## 4. How to Do Technology Selection Well?

Understanding technology selection's importance, let's discuss how to do it well.

Next, I'll combine my team's experience to share key points of technology selection from several aspects.

### 4.1 Define Context

First, clarify: there's no perfect technology. Our goal is selecting the **optimal solution** for **specific scenarios** under **limited conditions**.

Key points:

#### 1) Limited Conditions
Refers to team size, members' skills, time costs, monetary costs, etc.

For example, if everyone only knows Java and the project needs quick delivery, prioritize Java-related technologies. Don't make everyone learn Go just because it's high-performance.

If the company lacks funds/resources, rather than paid services (like cloud databases), consider self-hosting on a server.

If the company has resources but lacks manpower, don't self-host services like databases - use third-party cloud services.

A common approach is starting from human resources - see what technologies the team knows. For urgent needs, prioritize familiar technologies for quick initial delivery, then research better architectures for optimization. If the team has mature experience with a technology and experts, prioritize it. For example, Alibaba teams prioritize Java; ByteDance prefers Go.

From resource perspective, consider if team resources suit the technology. For example, startups with limited funds might use MySQL instead of Elasticsearch for search functionality, sacrificing flexibility for cost savings. If servers only have 4GB RAM, choose open-source technologies with memory usage below this threshold.

#### 2) Specific Scenarios
Technology selection must revolve around **specific business and requirements**, fitting reality rather than using technology for its own sake.

Consider:

1. What functionalities to implement? For a cloud storage system, focus on file storage technologies; for a chat system, prioritize real-time communication technologies.
2. What's the business scale? For many users/large data, use distributed databases or sharding; for high concurrency, use Nginx or LVS for load balancing.
3. What are the core workflows and key data structures? For management systems, mainstream relational databases like MySQL suffice; for analytics, choose OLAP databases like ClickHouse.
4. Which performance metrics matter most? For log collection prioritizing high throughput, use Kafka; for low latency and message accuracy, choose RabbitMQ.

Limited conditions + specific scenarios = context, meaning the team/project's actual situation.

#### 3) Optimal Solution
Often, technology selection is like algorithm design - no absolute best, but trade-offs between time, space, stability, availability, performance, etc.

Different contexts yield different optimal solutions. This is what makes technology selection both interesting and challenging!

> CAP theory is similar: in distributed systems, you can't simultaneously achieve Consistency, Availability, and Partition Tolerance - you must balance based on reality.

### 4.2 Thorough Research
After clarifying context, conduct thorough research using search engines, GitHub, articles, videos, etc., to explore potential technologies.

Research includes:
- What is this technology? What's its purpose?
- What are its pros and cons?
- What's its usage cost?
- What scenarios is it best for?
- Its background and reputation, etc.

Many websites help with technology selection, like:
- Technology Radar: https://www.thoughtworks.com/zh-cn/radar
- Framework Performance Comparisons: https://www.techempower.com/benchmarks

But since AI chatbots became popular, I directly ask AI (GPT or Yucongming) for technology selection information.

For example: "You're a computer programming expert. I'm building an XX system with XX functionalities under XX constraints. List required technologies, recommending multiple options with introductions, pros/cons, and suitable scenarios for optimal selection."

During research, explore widely without limiting to specific technologies.

Record all findings in documents (tables/lists) for easy comparison and final selection, like:

![](https://pic.yupi.icu/1/image-20230706180955933.png)

In large companies/projects, technology selection requires solid justifications for approval.

### 4.3 Compare and Select
With sufficient information, select the most suitable technology based on context + research.

Beyond context, prioritize: well-known, company-backed, actively maintained, high-activity, open-source, well-documented, popular technologies with good ecosystems.

For example, famous frontend framework Vue and Java backend framework Spring Boot are known for power, ease-of-use, abundant learning resources, making them mainstream with high demand.

Avoid poorly-documented, obscure technologies! If issues arise later without solutions online, the entire project might stall!

### 4.4 Minimal Demo Validation
Before finalizing technology, validate its applicability to your project - don't just decide!

Recommend creating minimal demos for quick validation. For example, for Vue Router, create a demo with button-click page navigation to `/about`.

This bridges theory to practice.

Thanks to AI, you can now ask GPT to generate demo code for validation:

![](https://pic.yupi.icu/1/image-20230706182008022.png)

For legacy systems, especially check compatibility between new technologies and existing dependencies. Minimal demos help prevent version conflicts.

### 4.5 Continuous Learning
Beyond above methods, experience accumulation is crucial for technology selection. For example, a decade-experienced e-commerce architect can immediately identify suitable technologies for new e-commerce systems.

Two suggestions:
1) Continuous Documentation
   Record every technology related to your learning/work direction. Understand its purpose for future reference when needed.

2) Expand Horizons
   Don't rely on one technology or limit yourself to your field. Especially during self-learning/small projects, occasionally try new technologies to broaden selection scope.

For example, though my work is Java backend, I often do frontend projects using various technologies like Vue, React, Svelte.

## 5. Technology Selection Practice

After methodology, let's share our team's technology selection practice for Yucongming AI.

Following above steps, first define context.

### 5.1 Define Context

#### 1) Limited Conditions
Our small team is most familiar with Java Spring Boot (backend) + React (frontend), so these frameworks were basically decided.

Limited funds prevented purchasing many servers/third-party services, so for most non-core services, we used Java Tomcat single-server deployment with BaoTa Linux for server management.

#### 2) Specific Scenarios
Four key questions:

1. What functionalities? Core P0 features: AI chat, AI assistant, content moderation. AI chat/moderation could use third-party GPT services.
2. Business scale? Initial website with low concurrency (QPS ≤3), no load balancing needed yet.
3. Core workflows/data structures? Main flow: user sends message => AI replies => store history. Involves conversation/message storage, suitable for relational databases like MySQL.
4. Key performance metrics? Having been attacked before, we prioritized security/availability. For concurrency control, used Redis + Redisson for distributed rate limiting on message frequency.

#### 3) Optimal Solution
One example: use costly third-party cloud databases or self-host?

In school, I'd self-host - learning projects just need to run, and self-hosting builds Linux skills. Most importantly, saving money for lunch!

But for user-facing online projects, I prefer cloud databases - setup, maintenance, optimization are handled; just use provided credentials without worrying about downtime. The extra cost significantly saves our "already limited" development/operations resources.

This is our current optimal database selection.

### 5.2 Thorough Research
As mentioned, most features use mainstream Spring Boot + React - our team's expertise with good ecosystems, requiring little additional research.

Instead, we spent more time researching **content moderation**.

Why?

Because for AIGC products, content moderation is crucial - and costly!

To balance quality and cost, we focused on major domestic cloud services (BAT).

Through official docs and customer service, we created this comparison table:

> Data for reference only; check official sources

![](https://pic.yupi.icu/1/image-20230706195734818.png)

### 5.3 Compare and Select
With the table, we could select content moderation technology based on actual needs.

For example, our chat allows 1000-2000 characters per input with QPS ≤10, making Provider A or B reasonable choices.

> We can split long inputs into paragraphs, sending multiple requests to bypass character limits.

### 5.4 Minimal Demo Validation
After selecting, implementation was straightforward. For third-party moderation services, official docs provided ready example code for local execution.

![](https://pic.yupi.icu/1/image-20230706200934291.png)

### 5.5 Continuous Learning
Regarding continuous learning, our team constantly monitors AI-related technologies, recording new findings in shared knowledge bases for future research when needed.

---

Technology selection is key to good products. Right choices bring efficiency; wrong ones may trap projects.

Remember:
1. Base selection on team reality and project needs
2. Research thoroughly, compare widely
3. Prioritize well-known, ecosystem-rich technologies
4. Validate with minimal demos
5. Continuously accumulate experience

In the Vibe Coding era, AI helps quickly understand technologies and generate demo code. But selecting optimal technologies based on context still requires your judgment and experience.

Choose wisely for smoother product development! 💪

## Recommended Resources

1) Yupi AI Navigation: [AI Resources, Latest News, Free Tutorials](https://ai.codefather.cn)  
2) Coding Navigation Learning Circle: [Learning Paths, Tutorials, Projects, Career Guides, Q&A](https://www.codefather.cn)  
3) Programmer Interview Handbook: [Internship/Campus/Social Recruitment FAQs, Company Questions](https://www.mianshiya.com)  
4) Resume Builder for Programmers: [Professional Templates, Rich Examples, Interview Prep](https://www.laoyujianli.com)  
5) 1-on-1 Mock Interviews: [Essential for Internship/Campus/Social Recruitment Offers](https://ai.mianshiya.com)