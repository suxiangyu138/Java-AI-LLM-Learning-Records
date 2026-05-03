# Documentation and Knowledge Management

> Documenting is the key to going further

Hello everyone, I'm Yupi.

Let me share a personal story first. During my sophomore year, I worked on a project with a team. As I contributed more and more code to the project, I gradually took the lead. Eventually, I became so familiar with the project's logic that team members would come to me whenever they encountered problems.

However, due to time constraints, I later left the team after carefully explaining the code to other members.

Guess what happened?

After my departure, team members kept coming to me with questions about the project. Even more surprisingly, a month later, the project was abandoned!

When I later asked why the project failed, a teammate replied frustratedly: "You never wrote any documentation, and many parts of the project were incomprehensible. We couldn't continue working on it."

That's when I realized: while I had been focused on writing code, I failed to document my experience and project knowledge to share with others, making my contributions essentially unmaintainable by anyone else.

Sigh, I was just too inexperienced back then. Since that incident, I've made it a habit to document every project I work on. Even if it's just for my own reference, at least I can understand my own past work.

This brings us to the core topic of this article — **documentation**. Whether for individuals or teams, whether in learning, work, or projects, whether you're a programmer, product manager, or project manager, proper documentation is crucial.

Below, I'll share with you: Why document? How to write good documentation? And how to manage documentation effectively?

## What is Documentation?

Documentation is a medium for recording, storing, and transmitting information.

Our project requirement sheets, system design documents, research reports, meeting minutes, resolved bugs, or even casual notes taken while watching tutorial videos—all of these are forms of documentation.

## Why Document?

From the definition alone, we can already understand the fundamental purposes of documentation:

- **Recording information**: Capturing temporary information
- **Storing information**: Preventing information loss and forgetfulness
- **Transmitting information**: Sharing knowledge with others

Our brains have limited capacity—we can't remember everything. Writing documentation is essentially building our second brain.

Personally, I have a small memory capacity, so I've developed the habit of jotting things down and documenting as I go. Sometimes I even pull out my phone to record good ideas that come to me while walking.

### The Value of Documentation for Projects

For projects (especially large enterprise projects), documentation carries even greater significance.

During the **initial phase**, documentation guides the smooth progression of the entire project. If building a project is like constructing a skyscraper, then documentation is the blueprint. Without blueprints, construction workers would work blindly, making completion uncertain and quality/safety impossible to guarantee.

Similarly, if you don't create documentation during project initiation—lacking systematic plans and execution strategies—team members will lose direction during development, frequently encountering delays, failures, and rework.

> Those who've watched my livestreams know that at the start of every project, I document requirements analysis, design plans, technology selection, etc. Before implementing any feature, I first write the design solution in documentation before coding. While documenting takes extra time initially, it significantly reduces overall project hours.

During the **mid-phase**, documentation's purpose is to **continuously** record and track project status, making information transparent across the team to facilitate correct decisions and timely risk avoidance.

Imagine running a marathon where documentation serves as mile markers—telling everyone how far they've come and what lies ahead. These markers also warn of dangers ahead. Without documentation, the team quickly scatters, working in silos, and one person's risk could drag down the entire team.

During the **final phase**, documentation helps us review, maintain projects, and pass on knowledge.

- **Review and summary**: By reading documentation, we can revisit the project's complete lifecycle from birth to completion, analyzing reasons for success or failure to learn lessons for future projects.
- **Ongoing maintenance**: For projects requiring continuous updates, proper documentation (like bug logs or user manuals) ensures that even if maintainers change, new members can quickly find solutions from documents, preventing "one person leaves, project dies" scenarios.
- **Knowledge transfer**: Well-documented knowledge represents predecessors' valuable experiences, lessons, ideas, and methods—extremely worthwhile for learning and inspiring future team members.

In short, documentation benefits everyone. Every word you write creates value.

---

Now that we understand documentation's importance, how do we do it well?

We can break documentation down into two tasks:

1. Writing good documentation (the prerequisite)
2. Managing documentation effectively

Let's explore each.

## How to Write Good Documentation?

To write good documentation, we first need to understand "what makes documentation good," then learn the tools and methods for writing it.

### What Makes Documentation Good?

My personal criteria for evaluating documentation quality:

1) **Human-readable and easy to understand**

First, your documentation is for **people** to read. If it's personal study notes, ensure you can understand it later. If shared within a team, ensure others can comprehend it. If one sentence could explain something but you use twenty, even with documentation, people might still bother you directly.

2) **Well-structured and easy to navigate**

Good documentation should let readers quickly scan from top to bottom and understand: what you're writing, what you want to convey, what they can gain from it, and where to find needed content.

Like this article, I use multi-level headings to structure it, allowing you to quickly locate interesting sections via the table of contents.

3) **Complete and precise content**

Good documentation resembles a project module—it should be complete and cohesive, enabling readers to solve problems using just this document.

For example, a "Java Bug Solution" document should include: the bug's cause, troubleshooting process, solution, and lessons learned—not just presenting the bug without resolution.

Additionally, terminology and phrasing must be precise and unambiguous!

For instance, when our YuCongMing AI initially estimated costs, we wrote: "Several servers, cumulative cost around ten thousand yuan." "Several" and "around" are vague terms that could lead to miscalculations and losses. Later, we changed it to specifics like: "1 server (4C 8G), ¥1000/month" for finer cost control.

Other standards for good documentation might include consistent style and formatting for user-facing product docs, enhancing reader experience and team credibility, making users more willing to adopt the product.

### Writing Tools

"A craftsman must sharpen his tools first." Before discussing writing methods, let's share some tools.

Years ago, Word was the go-to for documentation, but it has many flaws—manual formatting adjustments, compatibility issues causing layout problems on different computers, etc.

Now I strongly recommend learning **Markdown** (a lightweight markup language), enabling consistent formatting with simple syntax.

For example, use `## Heading 2` for second-level headings, `> Quote` for quoted text, etc.

Many editors now support Markdown, like VS Code and JetBrains IDEs. For the best local Markdown editor, I recommend Typora, which I've used for years.

Diagrams often aid understanding—flowcharts, architecture diagrams, etc. Use online tools like Draw.io or classic software like Visio. For mind maps, try XMind. For generating images, consider Midjourney (or our YuCongMing AI).

For publishing documentation on platforms, paste Markdown into mdnice.com to automatically generate beautifully formatted articles.

Additionally, with web technology advancements, online documentation platforms have become powerful. For team collaboration and real-time sharing, consider YuQue, Tencent Docs, or Lark Docs.

### Methods

#### "Copy"

First, to write good documentation, start by "copying."

Or rather, as scholars say: "learn from others."

If you don't know how to write certain documents—project docs, design specs, user manuals—what to do?

Simple: find excellent **examples** online to emulate.

This applies beyond documentation—content creation, product development, thesis writing, craftsmanship. To excel at anything, first observe how others do it.

- Building a website? Study existing sites
- Making videos? Analyze viral content

It's straightforward—many things have been done before. Even without tutorials, careful imitation can succeed.

With open-source culture thriving, many projects and documents are publicly available on GitHub. When writing documentation, simply copy a renowned project's README.md, keep its outline, replace content with yours, and you have a standard document—that easy.

After reading and writing enough documentation, you'll naturally develop your own (or your team's) methodology.

#### Streamline Writing

Many dislike documenting because they lack ideas, don't know where to start, or what to write.

I used to stare at blank documents too...

But with more writing experience, I developed a method for quick content creation—what I call "streamlined writing."

What's a stream? Getting breakfast in a cafeteria: queue → buy buns → get soy milk → find seating—a DNA-embedded routine.

With a clear writing process, documenting becomes as easy as buying breakfast.

How?

1) **Outline first**: Structure your article and write headings based on the topic

For this article, I first wrote: "What is documentation? Why document? How to write good docs? How to manage docs?"—establishing the framework before