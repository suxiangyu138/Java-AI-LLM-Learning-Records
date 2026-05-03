# System Architecture Design Practice

> Make Your Product Stand Out

Hello everyone, I'm Yupi. In this article, we'll discuss a topic that sounds sophisticated but is actually not that difficult — **architecture design**. This is a crucial skill for those in the technical field and a core task during the initial preparation phase of product development!

In this article, I'll share my practical experience in product development and cover: What is architecture design? Why is it important? And how to do it well?

Whether you're working on a personal project using Vibe Coding or aiming to create a real product, mastering architecture design methods will make your system more stable and scalable.

## 1. What is Architecture Design?

Imagine we're about to build a skyscraper. What steps would we take?

Would we just ask workers to stack bricks? Or have excavators dig away?

Of course not!

Before construction begins, an architect would draft a blueprint based on the building's purpose, location, and occupancy needs. This blueprint would outline the building's shape, internal layout, and structural details.

After drafting the blueprint, the architect would determine the framework and support structure for each floor, ensuring stability so that the building doesn't collapse in strong winds.

Next, the architect would plan the purpose of each floor based on the building's intended use and requirements, such as having a food court on B1 and luxury clothing stores on the 1st floor. These decisions are made before construction starts, so there's no last-minute change like, "Let's not build this floor!"

Similarly, in software (or website) development, before developers start coding, an experienced architect often drafts an **architecture design diagram** to outline the system's overall structure, scale, and architecture.

The architect also divides the system into modules based on functionality and requirements, such as splitting an e-commerce system into user, product, and order modules. They define the roles and interactions of these modules to ensure the system operates smoothly, avoiding situations where a single failure brings down the entire system.

Some might ask, how is architecture design different from technology selection?

Using the skyscraper analogy, it's easy to distinguish. Architecture design is about planning how to build the skyscraper and arranging each floor, while technology selection involves choosing the specific materials, tools, or methods to complete the construction after the architecture design is done. The two complement each other.

In summary, **architecture design is the process of building a stable, reliable, and scalable system**.

The well-known role of "architect" refers to the person responsible for completing the system's architecture design, ensuring it is stable, reliable, and scalable.

Many might think architects are highly prestigious roles. Personally, I believe that if you can independently design a system's architecture and break down complex systems into modular, scalable components, you are already an architect.

> So, becoming an architect in '97 does have some rationale.

## 2. Why is Architecture Design Important?

From the skyscraper example, you can already sense the importance of architecture design.

If you don't design the building's structure beforehand, it's hard to ensure its safety and stability — a strong wind could collapse the entire floor. Similarly, if you don't design a website's overall architecture (e.g., failing to add a firewall) before development, the entire system might become inaccessible during a network attack. This is the core goal of architecture design: ensuring the system's **stability, reliability, and availability**. In other words, at least make sure the system can run.

> But it's okay, as long as either the system or the person can run.

Another important reason for architecture design is the system's **scalability**. For example, if a newly built shopping mall becomes very popular and we want to add more floors, we must ensure from the start that the building's structure can support the additional weight and allow linear expansion. Not like this:

![](https://pic.yupi.icu/1/image-20230711174331231.png)

Similarly, if a website needs to handle increasing user numbers and new business features, a good architecture design is essential. Avoid situations where the website becomes inaccessible due to increased traffic.

Additionally, good architecture design can enhance system performance and user experience. For example, in a shopping mall, properly arranging elevators and their locations helps users quickly reach their desired stores. Similarly, a well-designed website architecture, such as using fewer layers to achieve the same functionality, can respond to user actions faster.

In short, if you want your website to stand tall like a skyscraper, early-stage architecture design is essential!

## 3. How to Do Architecture Design Well?

There's a big difference between "completing architecture design" and "doing architecture design well." Below, I'll share some key points based on my years of learning experience and the architecture design of Yucongming AI.

### Mastering Methodology

The foundation of good architecture design is having a solid theoretical foundation. Methodology refers to a set of **solutions to specific problems** summarized and generalized by predecessors. It's like using formulas to solve math problems.

There are many methodologies helpful for architecture design. For example, object-oriented design, basic software development principles, 23 classic design patterns, SOA (Service-Oriented Architecture), DDD (Domain-Driven Design), etc. Understanding the ideas behind these methodologies is a **prerequisite** for good architecture design.

For example, one of the five SOLID principles of object-oriented design is the **Single Responsibility Principle**, which states that a class or module should have only one responsibility, focusing on a specific function or task. In other words, each module should do its own job well.

When designing the architecture for Yucongming AI, we first followed the Single Responsibility Principle, splitting the system **by functionality** into user, assistant, conversation, painting, and payment modules. This way, when we wanted to expand conversation-related features, we didn't need to modify the assistant module's code. We could also assign different modules to different team members for development.

![](https://pic.yupi.icu/1/image-20230711192728792.png)

### Learning Classic Architectures

Stand on the shoulders of giants. Before we can independently design an architecture, we can learn from classic architectures summarized and practiced by predecessors.

Here are a few examples:

#### 1) Layered Architecture

First, the most classic layered architecture divides the system into multiple layers, each with specific functions and responsibilities, interacting only with its direct upper and lower layers.

For example, the three-tier architecture commonly used in Java enterprise backend projects divides the system into presentation, business logic, and data access layers.

The presentation layer handles user requests, passing user inputs to the business logic layer for processing and returning data or pages to the user.

The business logic layer handles complex business logic, such as invoking AI capabilities for intelligent conversations, processing the results, and calling the data access layer to store the results in a database. This is the main development part of the system.

The data access layer operates the underlying data sources, such as performing CRUD operations on databases, files, or caches.

![](https://pic.yupi.icu/1/image-20230711194035810.png)

Layered architecture is widely applicable and can serve as the foundational architecture design for most enterprise systems.

Computer networks also use a classic layered architecture. The OSI seven-layer reference model divides computer networks from bottom to top into physical, data link, network, transport, session, presentation, and application layers. Each layer handles specific functions, such as data transmission or routing, and communicates with other layers through interfaces (or protocols).

Our Yucongming AI backend also uses layered architecture, but with an added Manager layer (common logic layer) between the business logic and data access layers to extract **common logic** frequently used by the business logic layer for reuse.

#### 2) Microservices Architecture

The "micro" in microservices contrasts with "monolithic" projects. Microservices architecture involves splitting a large system into multiple small, autonomous services. Each service can be independently deployed, scaled, and maintained without affecting others. Services collaborate through network communication to achieve the full functionality of the original large system.

Our Yucongming AI uses microservices architecture. As mentioned earlier, we first divided the system into modules by functionality.

However, if we only logically divided these modules, all the code would still be deployed in the same project, packaged into a single executable file. This means all modules either run together or crash together, essentially still a monolithic project. A failure in one service could bring down the entire project!

![](https://pic.yupi.icu/1/image-20230711200006355.png)

So, we extracted some critical modules (e.g., the payment module) from the original project and deployed them as separate services, even starting backups to ensure payment business stability. After all, we can't let anything affect revenue, right?

![](https://pic.yupi.icu/1/image-20230711200339275.png)

There are many frameworks for implementing microservices, such as Spring Cloud and Apache Dubbo for Java. However, learning microservices is more about the mindset of **how to reasonably split services**.

Not all projects need to split every function into sub-services. For example, in Yucongming AI, we didn't separate the user and assistant modules because their business logic isn't complex and they have tight dependencies. Splitting them would make maintenance harder.

#### 3) Event-Driven Architecture

In event-driven architecture, modules communicate through events (or messages) using a **publish-subscribe model**.

For example, consider two modules: payment and membership. When a user successfully pays, the payment module sends an event "User XX paid successfully" to the membership module, which then activates the user's membership.

![](https://pic.yupi.icu/1/image-20230711202025430.png)

In practice, event-driven architecture often introduces an **event bus**, acting as a mediator to collect and distribute events.

For example, Yucongming AI's conversation and painting functions use event-driven architecture. When the upstream returns AI responses or generated images, it sends a "success" message to the event bus, which then forwards the message to the corresponding modules for processing. Like this:

![](https://pic.yupi.icu/1/image-20230711201650066.png)

This way, modules are decoupled (they don't affect each other). If I want to add more conversation modules later, I only need to connect the new module to the event bus without affecting other modules.

### Focusing on Needs and Pain Points

Almost all projects we encounter during learning can adopt the mainstream architectures mentioned above. However, how to do architecture design well must be analyzed based on actual needs and pain points.

Take Yucongming AI as an example. As mentioned earlier, we first split the system into modules based on requirements (functionality), then encapsulated some modules as separate services for independent deployment.

But architecture design doesn't end there. Next, we analyzed the pain points of our website, mainly focusing on "security" and "access interoperability."

#### Security

Based on my past failures, a website will inevitably face various attacks after going live! So, we expanded the original layered architecture by adding a high-defense server and Nginx firewall before the presentation layer, and distributed rate limiting and permission verification after the presentation layer.

> Adding centralized distributed rate limiting and permission verification before the presentation layer is also reasonable, depending on actual needs.

The improved architecture looks like this:

![](https://pic.yupi.icu/1/image-20230711203808233.png)

If a user sends frequent malicious requests in a short time, they will be intercepted at the rate-limiting layer, preventing further business logic processing.

#### Access Interoperability

Our AI painting module relies on third-party services for some functionalities, but these services don't support cross-region access, causing network interoperability issues. What can we do?

We had two options:

1) Deploy the entire system in the region where the third-party service is located. Like this:

![](https://pic.yupi.icu/1/image-20230711205545871.png)

2) Set up a proxy between the AI painting module and the third-party service to handle request sending and response. Like this:

![](https://pic.yupi.icu/1/image-20230711210049633.png)

Which one would you choose?

Option 1 is convenient, but the downside is obvious: moving the entire system to another region means slower access for users in the original region.

Option 2's advantage is that only the AI painting module's request address needs to change, while other functionalities (e.g., querying user information) remain unaffected. The downside is the need to set up additional services, increasing implementation costs.

Ultimately, we chose Option 2 and improved the architecture design, accepting higher implementation costs for better user experience.

This example illustrates that **there's no perfect architecture design**. Like technology selection, our goal in architecture design is to find the optimal solution under actual conditions.

### Thinking Ahead

When doing architecture design, we should develop the habit of thinking ahead, not just focusing on the current state but anticipating future system developments and leaving enough **scalability** space.

For example, Yucongming AI started with only 100 beta users, but we designed the system to handle 10,000 or even 100,000 users. So, we used distributed storage middleware instead of local servers to store user login sessions. Although early historical conversation messages only accumulated 50,000, we designed a message expiration mechanism and decoupled the message module to prevent millions or billions of messages from affecting system query performance.

Of course, thinking ahead should have limits. Avoid over-engineering by not considering impossible scenarios or consuming costs far beyond growth.

---

Finally, remember that **architecture design is an ongoing process**. Continuously optimize and iterate based on actual business conditions. Simplify architecture to reduce costs when business profits are low; scale services to handle growth during rapid business expansion.

## Final Thoughts

Architecture design is the foundation of good product development. Good architecture design makes systems more stable, scalable, and performant.

Remember these key points:

1. Master basic methodologies and design patterns for architecture design.
2. Learn classic architectures and stand on the shoulders of giants.
3. Focus on actual needs and pain points, not designing for the sake of design.
4. Think ahead and leave room for expansion.
5. Architecture design is a process of continuous optimization.

In the Vibe Coding era, AI can quickly generate code, but the overall system architecture design still requires your own thinking and planning. With the awareness of architecture design and sufficient accumulation, anyone can become an architect!

## Recommended Resources

1) Yupi AI Navigation Website: [AI Resource Collection, Latest AI News, Free AI Tutorials](https://ai.codefather.cn)

2) Programming Navigation Learning Circle: [Learning Paths, Programming Tutorials, Practical Projects, Job Guides, Q&A](https://www.codefather.cn)

3) Programmer Interview Cheatsheet: [Internship/Campus/Social Recruitment High-Frequency Topics, Enterprise Question Analysis](https://www.mianshiya.com)

4) Programmer Resume Writing Tool: [Professional Templates, Rich Examples, Direct to Interview](https://www.laoyujianli.com)

5) 1-on-1 Mock Interview: [Essential for Internship/Campus/Social Recruitment Interviews to Secure Offers](https://ai.mianshiya.com)