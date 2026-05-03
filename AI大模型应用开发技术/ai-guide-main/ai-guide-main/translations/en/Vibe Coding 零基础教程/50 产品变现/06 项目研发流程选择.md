# Project Development Process Selection

> Fast or Stable?

Hello everyone, I'm Yupi.

I wonder if you like spicy sticks? Anyway, I'm a big fan.

When eating spicy sticks, I often ponder: how are these delicious spicy sticks made?

To satisfy my curiosity, I searched online and was quite surprised!

I found that some spicy sticks are produced using quite "advanced" methods—not entirely manual labor, but through a **production assembly line**. After preparing the raw materials, machines sequentially handle mixing, stirring, cutting, seasoning, frying, cooling, and packaging, while workers only need to supervise.

![](https://pic.yupi.icu/1/6f6e8ad610e6418e9556fb0fb95afda1.jpeg)

With such a clear set of procedures, the entire spicy stick production process becomes orderly. Workers (or machines) only need to focus on their specific tasks step by step, improving overall efficiency. Moreover, if each step is thoroughly validated, the quality and safety of the spicy sticks will also be higher.

This highlights the importance of **standardized processes**.

Returning to today's topic, if we want to develop and launch a product, we also need a standardized development process.

A development process refers to a **series of work steps and methods** designed to efficiently complete product development. Team members only need to follow these steps and methods sequentially to successfully deliver a high-quality product.

So, what kind of development process should a team adopt for collaborative development? Should the process have more steps or fewer?

This article will combine my experience working in large companies and leading small teams to share: the standard development process in large companies, agile development for small teams, and the development process choice of our Yucongming AI team. I hope this helps broaden your perspective and improve your team's development efficiency and project quality.

## Standard Development Process in Large Companies

This is the focus of this article.

In large companies, hundreds or even thousands of people may collaborate on a single project. To ensure stable progress, the development process in large companies is typically **comprehensive and complex**, involving many steps, each with strict requirements.

I’ve divided the large-company development process into several phases, summarized in a mind map for clarity:

![Large-Company Development Process Mind Map](https://pic.yupi.icu/1/3334dda0-2177-4816-bc91-94e7d3b77bde.png)

Using spicy stick production as an analogy, the large-company development process is like the **standardized assembly line** for producing spicy sticks mentioned earlier.

> Note: The phases above are not strictly sequential; there may be overlaps. For example, technical research and technology selection should ideally be considered during the design phase.

### Requirements Phase

The goal of the requirements phase is to clarify: Why are we building this product? What kind of product are we building? What features should the product have?

Now, let’s assume spicy sticks are our product.

Since the requirements phase is the **initial** step in the development process, I’ll break it down into four sub-phases for detailed explanation.

#### Requirement Generation

Why are we making spicy sticks? Perhaps market research showed that people love spicy sticks and that producing them could be profitable. Maybe we stumbled upon an idea to improve the flavor. Or perhaps our resources are well-suited for spicy stick production.

The most important task in this phase is to collect and organize as many requirements as possible, leaving no potential opportunity unexplored.

Sometimes, a seemingly trivial idea can turn into a highly meaningful requirement.

#### Requirement Review

After gathering many requirements, we need to review them to determine their feasibility, cost-benefit ratio, risks, and alignment with our product vision—rather than trying to do everything.

For example, if we plan to produce a new flavor of spicy sticks, we must first research whether the new flavor will be popular and whether it can be produced.

#### Requirement Analysis

After reviewing the requirements, we need to further analyze them, breaking them down into **specific** product features or tasks.

For instance, to produce a new flavor, we must determine the spiciness level, develop a new recipe, ensure safety, and design new packaging.

#### Scheduling

After clarifying the requirements through analysis, we need to schedule them.

In this phase, we create a production plan, prioritizing and allocating time for each requirement based on its importance and resource needs.

For example, producing spicy sticks might involve spending one month developing the recipe, three days procuring raw materials, three days producing the sticks, one day verifying safety, and three days designing new packaging—all concurrently.

### Design Phase

The goal of the design phase is to figure out in advance: How will we build the product? How will the features be implemented?

For spicy stick production, the design phase is about "finalizing the recipe."

First, **architecture design** is like determining the main ingredients and production techniques, ensuring that future new flavors can be developed on this foundation rather than starting from scratch each time (scalability).

**High-level design** is akin to conceptualizing the spicy sticks as a whole—such as the type of chili, the ratio of chili powder to flour, and the size of the sticks.

**Detailed design** refines the high-level design, delving into the **specifics** of each ingredient and step. For example, add 3 grams of chili powder, then 100 grams of flour, and stir three times (this is made up).

After completing these designs, we share them with the team for discussion to ensure everyone’s **plans are aligned** and ideas are consistent. We don’t want customers to receive spicy sticks with different recipes after launch.

During the design phase, not only product managers and engineers but also testers are involved, working on **test case design** in advance. This defines quality standards, such as a specific spiciness range (xx ~ xx) or stick length (10 cm).

### Development Preparation

In the development preparation phase, we focus on setting up the foundational conditions for the project.

For example, before officially producing spicy sticks, we conduct **technical research** to assess the feasibility of the new recipe. Then, we perform **technology selection**, choosing the tools or machines for mixing, cutting, etc.

After selecting the technology, we **request resources**, ensuring the team has the budget for advanced tools, raw materials, and labor.

In large companies, resource requests are often stringent. For instance, at Tencent, requesting a single server for project deployment required approvals from multiple layers, including leadership and operations.

Once resources are secured, we prepare the **environment**—finding a clean, well-equipped space for machines and workers. In development, this means setting up the development environment, installing necessary software, etc.

Next, we perform **project initialization** and **dependency installation**, positioning machines and workers, powering up, starting machines, installing programs, etc., to get everything ready. In development, we might use scaffolding tools to generate clean initial code and package managers to install dependencies, allowing us to start coding happily!

### Development Phase

The development phase is where programmers shine, with the goal of writing code to complete the project.

This phase can be divided into five sub-phases: local development, remote development, code optimization, unit testing, and integration testing.

First, each developer writes code on their own machine (local development) or collaborates on a shared machine (remote development). After completing basic features, they optimize the code—adding error handling for robustness or multithreading for performance. Then, they write unit tests to verify the code runs as expected. Finally, programmers collaborate on integration testing, calling each other’s code to validate full functionality.

In spicy stick terms, this phase involves machines and workers cooperating to mix, cut, season, and inspect the sticks. Since I’ve never worked in spicy stick production, I don’t know if they include taste-testing. If the flavor is off, they might tweak the machines (fixing bugs) or add secret ingredients (code optimization).

### Testing and Validation

After development, we must test and validate. The goal of this phase is to ensure the product functions correctly and eliminate bugs!

You might ask: Didn’t programmers already write unit tests during development?

However, unit tests are just a basic safeguard, ensuring individual code works. When multiple pieces of code interact, **integration testing** is needed to ensure they work together.

Beyond unit and integration testing, common tests include system testing and acceptance testing.

**System testing** follows integration testing, examining the entire system’s functionality and performance on a larger scale.

**Acceptance testing** is typically the final phase, confirming the system meets actual requirements and user expectations.

Another concept is **product experience**, which involves evaluating the product from a real user’s perspective. Strictly speaking, this isn’t limited to the testing phase but should run through the entire development process to identify and address issues early.

In spicy stick terms, after production, not only do we taste-test, but professional tasters also evaluate the recipe and process for issues.

### Submission Phase

The goal of the submission phase is to push the tested local code to the remote repository and merge it into the main branch, preparing for launch.

In practice, since projects are maintained by multiple people, **code reviews** are used to reduce conflicts and poor code. Before merging into the main branch, you must submit a Merge Request (MR), which is reviewed by the project lead (usually your manager or colleague). Only after approval can the code be merged.

This ensures all code is reviewed by at least two people, improving quality and reducing the chance of bugs post-launch.

In spicy stick terms, code reviews are like food safety inspections. If issues are found in the recipe or process, it’s sent back for rework.

### Release Phase

After "overcoming 81 hardships," our project is finally ready for release!

First, we package the spicy sticks for sale. Instead of a full-scale launch, we start with a small group of customers for feedback. If the response is positive, we proceed with a full launch.

In development, we **build and package** the code, then conduct a **pre-release** (or canary release) for a subset of users. If all goes well, we **officially launch**.

### Post-Release

At this point, the development process isn’t over—we’ve only completed one release cycle.

After launch, we continue collecting user feedback, analyzing it to iteratively improve the product.

Documentation is also crucial. We record all issues, materials, and knowledge from the process to help the team improve.

---

How does that sound? Does the large-company development process seem complex?

In reality, after the first launch, every new feature requires going through the entire process again! Sometimes, just the requirement review phase takes 1–2 weeks of discussion!

While this process ensures quality, it may not suit small teams, especially startups where time is money.

Next, let’s discuss agile development, which is better suited for small teams.

## Agile Development for Small Teams

If the large-company process emphasizes **standardization and stability**, agile development focuses on **team collaboration and rapid iteration**.

In agile development, many steps from the large-company process can be simplified or skipped, offering great flexibility.

For example, if we decide to build a product in the morning, the team might discuss requirements at noon, confirm core features for launch, and start coding in the afternoon. The goal is to deliver core functionality as quickly as possible, then continuously discuss new requirements, develop, launch, and improve.

More time is spent on development—speed is the priority!

![Agile Development Process](https://pic.yupi.icu/1/image-20230712185656368.png)

If we used agile development for spicy sticks, it might look like this:

The team notices a region loves spicy sticks, so they manually produce a few packs quickly for taste-testing. Based on feedback, they refine the recipe and packaging. After success, they invest in machines and factories for mass production.

Note: Agile development is a software methodology with various approaches like Scrum, Kanban, and Extreme Programming. Understanding the basics and applicability is sufficient.

## Choosing a Development Process

After learning about these two processes, how should we choose for our projects?

First, remember: **there’s no one-size-fits-all solution**. Each approach has strengths, and the choice depends on the situation.

Agile development prioritizes speed and flexibility but requires high team coordination and carries greater risks. Weeks of development might yield a useless product. In contrast, the large-company process consumes more upfront resources but delivers higher quality and stability.

So, for our Yucongming AI team, do we choose speed or stability?

The answer: Why choose? We’ll take both!

![](https://pic.yupi.icu/1/image-20230712184257231.png)

First, let’s analyze our situation:

1. We’re a small team without large-company resources or infrastructure (e.g., automated build pipelines).
2. We’re building AI products and need to launch quickly to capture the market.
3. Our team works closely in a small 40-square-meter office, enabling tight collaboration.

Given this, we lean toward agile development. However, we retain the design and submission phases from the large-company process and implement robust monitoring for continuous improvement.

The full process:

![](https://pic.yupi.icu/1/image-20230712190738396.png)

We aim for speed while ensuring scalability and code quality. Thus, we spend more time on design, adopting a generalized architecture (e.g., microservices, with a separate payment center) to support future growth.

For collaboration, we use Git with a simplified Git Flow model. Each developer works on their branch, and I review their code before merging into the main branch, ensuring stability.

Code review interface:

![Code Review](https://pic.yupi.icu/1/image-20230712193216012.png)

You might ask: Why skip the testing phase? Don’t you test your product?

Ideally, products should be tested by dedicated testers. But in reality, we don’t have any!

Our workaround:

1. Frontend and backend developers self-test their features. Before launch, I meet with the team to verify functionality.
2. Issues are endless, and systems evolve. We launch quickly and recruit beta testers for feedback.

This lets us focus on development while maintaining quality and improvement opportunities.

---

There’s no absolute right or wrong in choosing a development process. The key is tailoring it to your team and project.

Remember these points:

1. Large-company processes emphasize standardization and stability, suited for big teams.
2. Agile development focuses on rapid iteration, suited for small teams.
3. Combine the strengths of both to create your ideal process.
4. Keep core steps (e.g., design, code reviews) and simplify the rest.
5. Continuously optimize and improve the process.

In the Vibe Coding era, AI can accelerate development, but a good process ensures efficient collaboration and quality.

Find the right process for your team, and let your projects run fast and stable!

## Recommended Resources

1) Yupi AI Navigation: [AI Resources, Latest AI News, Free AI Tutorials](https://ai.codefather.cn)  
2) Programming Navigation Learning Circle: [Learning Paths, Tutorials, Projects, Job Guides, Q&A](https://www.codefather.cn)  
3) Programmer Interview Guide: [Internship/Campus/Job FAQs, Company Questions](https://www.mianshiya.com)  
4) Resume Builder for Programmers: [Professional Templates, Examples, Interview Prep](https://www.laoyujianli.com)  
5) 1-on-1 Mock Interviews: [Essential for Internship/Campus/Job Offers](https://ai.mianshiya.com)