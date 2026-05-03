# AI-Assisted Toolset

Hello, I'm Yupi.

In previous articles, we explored various AI programming tools, including AI no-code platforms, AI agent platforms, AI code editors, command-line tools, and IDE plugins. However, to truly develop projects efficiently, AI tools alone are not enough. You also need some auxiliary tools to perfect the entire workflow.

Here are some examples of issues you might encounter:

- AI messed up the code—how do you revert it?
- The project is done—how do you deploy it so others can access it?
- Are there other practical tools to boost efficiency?

In this article, I’ll introduce the auxiliary tools commonly used in Vibe Coding development to help you complete your development toolchain.

## 1. Git Version Control

### Why Do You Need Git?

During development, you might encounter situations like:

- You just modified the code, but it broke—how do you revert to the previous version?
- You want to try a new feature but are afraid it might affect the existing code.
- In team collaboration, you’re unsure who changed what.

Git can solve all these problems.

**Git is a version control tool** that records every change to your code, allowing you to revert to any historical version at any time.

💡 If you want to become a professional programmer, learning Git is essential—it’s a fundamental skill in enterprise development.

### Core Concepts of Git

Git’s workflow is simple, consisting of three main steps:

1. Modify code in the working directory.
2. Add code to the staging area (using the `git add` command).
3. Commit code to the repository (using the `git commit` command).

Think of it like writing on a draft paper: adding to the staging area is like selecting the satisfactory content, and committing to the repository is like formally saving that content in a notebook.

![](https://pic.yupi.icu/1/gitworkflow%E5%A4%A7.jpeg)

### How to Use Git?

There are two ways to use Git: **GUI tools** and **command-line**.

For beginners, I strongly recommend starting with GUI tools. Many mainstream development tools (like Cursor and VS Code) come with built-in Git functionality. You can commit and pull code with just a few clicks—no need to memorize commands.

When I first started with Git, I didn’t search for tutorials online. I just watched others submit projects by clicking a few times in the editor and found it magical. Then I followed suit and used tools like [GitHub Desktop](https://desktop.github.com/) for simple operations, searching for solutions when I encountered issues.

![GitHub Desktop APP](https://pic.yupi.icu/1/screenshot-windows-dark.png)

Once you’re comfortable, you can learn command-line operations. Although command-line seems complex, it’s more flexible and powerful, and many advanced features are only available through the command-line.

Here are some of the most commonly used commands. Mastering these will handle 90% of your daily development tasks.

```bash
# Clone a project
git clone https://github.com/liyupi/ai-guide.git

# Check status
git status

# Add files
git add .

# Commit
git commit -m "Added new feature"

# Push to remote
git push

# Pull latest code
git pull
```

No need to memorize them—just look them up when needed, or ask AI directly.

I recommend installing Git on your computer regardless of whether you plan to learn it. [Download it directly from the official website](https://git-scm.com/). Some software or tools may depend on Git, and not having it installed could cause issues later.

### Practical Usage Scenario

Let me demonstrate Git usage with a practical example. Suppose you’ve created a project in Cursor and want to manage its versions with Git.

1) First, initialize Git in the project’s root directory:

```bash
git init
```

2) Then add all files and commit the first version:

```bash
git add .
git commit -m "Initial version"
```

3) Continue developing, make some changes, and commit again:

```bash
git add .
git commit -m "Added user login feature"
```

4) If a modification causes issues and you want to revert to a previous version:

```bash
git log  # View history, find the commit hash you want to revert to
git reset --hard commit_hash
```

Git records every change, allowing you to revert anytime.

### Git and AI Tools Integration

Many AI tools now come with built-in Git functionality. For example, Cursor allows you to commit code directly in the editor:

![](https://pic.yupi.icu/1/image-20260109110826513.png)

You can also let AI automatically execute Git commands—just tell it, “Help me manage the project with Git.”

Additionally, AI can generate commit messages for you—everything is Vibe Coding!

I recommend developing a habit: **Commit every time you complete a feature.**

This way, even if AI messes up the code, you can always revert to a previous version. With Git, you can confidently let AI modify your code—you can always roll back.

### Learning Recommendations

Git is powerful, but for Vibe Coding, mastering the above usage is sufficient. If you want to dive deeper into Git and GitHub, check out Yupi’s [Git & GitHub Learning Path](https://www.codefather.cn/course/1789189862986850306/section/1789190804671012866). This learning path covers all core Git knowledge from beginner to advanced levels and is completely free.

## 2. Deployment Hosting Platforms

After completing your project with AI, it runs well locally. But if you want others to access it, you need to deploy it to a server.

Traditional deployment is complex: renting a server, configuring the environment, uploading code, setting up a domain, etc. But now, there are many free deployment platforms that let you go live in minutes.

### Vercel

[Vercel](https://vercel.com) is currently the most popular frontend deployment platform, especially suitable for React, Next.js, Vue, and static websites.

Its advantages include being completely free for personal projects, extremely fast deployment (usually 1–2 minutes), automatic HTTPS and CDN configuration, and deep integration with GitHub, enabling automatic deployment when you push code.

Using Vercel is super simple.

1) First, visit the [Vercel website](https://vercel.com) to register an account. It’s recommended to use your GitHub account for registration and login.

2) Create a project, bind it to GitHub, select the hosted project, and click the "Deploy" button:

![](https://pic.yupi.icu/1/image-20260109111323983.png)

3) Wait 1–2 minutes, and your project is live!

After successful deployment, Vercel automatically assigns you a domain, like `your-project.vercel.app`. You can also bind your own domain.

Every time you push code to GitHub, Vercel automatically redeploys—no manual intervention needed.

💡 For a detailed process, check out Yupi’s video tutorial: https://www.bilibili.com/video/BV1TV4y1j76t

### Netlify

[Netlify](https://www.netlify.com/) is similar to Vercel but offers more comprehensive features. It supports more frameworks and static site generators, includes form handling, serverless functions, larger free quotas, and supports A/B testing and analytics. Its usage is similar to Vercel, so I won’t elaborate further.

### EdgeOne Pages (Domestic Deployment Platform)

[EdgeOne Pages](https://pages.edgeone.ai) is a full-stack edge development platform launched by Tencent Cloud, based on Tencent Cloud’s EdgeOne infrastructure, providing a serverless deployment experience from frontend pages to dynamic APIs.

EdgeOne is Tencent Cloud’s edge security acceleration platform, essentially combining “network acceleration + security protection.” It leverages Tencent’s global network nodes to serve your website closer to users, ensuring faster loading speeds. It also integrates web protection capabilities to filter and block malicious traffic at the edge, safeguarding your website.

EdgeOne Pages, built on this robust infrastructure, offers advantages like fast domestic access speeds, deep integration with Tencent Cloud services, support for edge functions, and free quotas. It’s more suitable for domestic developers.

### GitHub Pages

[GitHub Pages](https://pages.github.com/) is GitHub’s free static website hosting service. Its advantages include being completely free, offering unlimited traffic, and seamless integration with GitHub.

Using it is incredibly simple. After creating a repository on GitHub and uploading your website files, enable GitHub Pages directly in the repository settings:

![](https://pic.yupi.icu/1/image-20260109111917547.png)

Then, you can access it via `username.github.io/repo-name`. It’s suitable for personal homepages, project documentation, and simple static websites.

### How to Choose?

- If your project is Next.js, choose Vercel (official recommendation).
- For other frontend frameworks or static websites, both Vercel and Netlify are good options.
- For domestic users seeking faster access speeds, choose EdgeOne Pages.
- For simple static pages, GitHub Pages is the easiest.

I mainly use Vercel + EdgeOne Pages because they’re fast and offer a great experience. For domestic projects, EdgeOne Pages indeed provides much faster access speeds.

### Cloudflare CDN

If you want your website to load even faster, you can use [Cloudflare](https://www.cloudflare.com/zh-cn/)’s free CDN service.

CDN (Content Delivery Network) caches your website content on servers worldwide, automatically serving users from the nearest server, significantly improving load times.

![](https://pic.yupi.icu/1/1763643073516-5248d56c-bf7d-4537-b8f8-681a104626d9.png)

Cloudflare’s advantages include:

- Completely free (for personal websites).
- Global CDN acceleration, covering 200+ cities.
- Automatic HTTPS certificates.
- DDoS protection and web firewall.
- Free DNS service.

Using it is simple: register a Cloudflare account, add your domain, and change your domain’s DNS servers to those provided by Cloudflare. Cloudflare will automatically accelerate and protect your website.

You can also use Cloudflare’s Pages deployment capability to upload your code and let it handle deployment with a free domain—even more convenient!

![](https://pic.yupi.icu/1/1763643412558-4d499b46-5e16-4f83-9df7-06a85175df35.png)

If your website is deployed on Vercel or Netlify, they already have CDN acceleration, so no additional Cloudflare configuration is needed. But if you’re deploying on your own rented server, strongly consider using Cloudflare for acceleration.

### More Deployment Methods

Yupi has shared multiple video tutorials on quickly deploying projects:

- [Vercel Project Deployment Tutorial](https://www.bilibili.com/video/BV1TV4y1j76t)
- [Cloud Editor + Vercel + Object Storage + Intranet Penetration: 4 Deployment Methods](https://www.bilibili.com/video/BV1UZ4y197i1)
- [Nginx + Baota: 2 Deployment Methods for Personal Blogs](https://www.bilibili.com/video/BV1rU4y1J785)
- [WordPress Personal Blog Setup](https://www.bilibili.com/video/BV14q4y1R7XM)
- [4 Mainstream Frontend/Backend Project Deployments](https://www.codefather.cn/course/1790943469757837313/section/1791075571845345281?contentType=video&tabKey=videoList)

Additionally, Yupi has guided over 20 projects on [Programming Navigation](https://codefather.cn/), covering almost every deployment method. If you want to become a professional programmer, it’s recommended to learn these.

- [AI No-Code Application Generation Platform Project](https://www.codefather.cn/course/1948291549923344386): 1Panel + Nginx Frontend + Java Backend (JAR package)
- [Code Generator Sharing Platform Project](https://www.codefather.cn/course/1790980795074654209): Baota Panel + Nginx Frontend + Java Project Manager (JAR package) Backend Deployment
- [AI Quiz Application Platform Project](https://www.code-nav.cn/course/1790274408835506178): Vercel Frontend + Docker Backend + Cloud Hosting Serverless Platform Deployment
- [AI Super Agent Project](https://www.codefather.cn/course/1915010091721236482): Docker Frontend + Docker Backend + Cloud Hosting Serverless Platform Deployment
- [OJ Online Judge Project](https://www.codefather.cn/course/1790980707917017089): Docker Compose Backend Microservices Deployment

Mastering these deployment methods will handle most deployment needs.

## 3. MCP Services - Extending AI Capabilities

MCP (Model Context Protocol) is an open standard that allows AI tools to connect to various external tools and data sources.

Simply put, MCP is like equipping AI with various “plugins,” enabling it to do more. For example, the File System MCP allows AI to read and write files, GitHub MCP lets AI operate GitHub repositories, Database MCP enables AI to query databases, and Browser MCP allows AI to browse the web.

![](https://pic.yupi.icu/1/mcp.png)

Almost all mainstream AI programming tools now support MCP, including Cursor, Claude Code, Cline, Windsurf, Gemini CLI, Kiro, etc. You can use various MCP services in these tools to greatly expand AI’s capabilities.

Let me use Cursor as an example to demonstrate how to configure and use MCP.

### Using MCP in Cursor

Let me demonstrate how to configure and use MCP in Cursor with a practical example.

For instance, suppose I want Cursor to know the current accurate time.

1) Search for the MCP tool you need on the MCP Encyclopedia website and obtain the MCP configuration information for later use:

![](https://pic.yupi.icu/1/image-20260109113038258.png)

Since this MCP tool requires the `uvx` command for installation, we need to install the uv tool first. Refer to the [official installation documentation](https://docs.astral.sh/uv/getting-started/installation/), select your operating system, and execute a single command to complete the installation.

![](https://pic.yupi.icu/1/image-20260109113308798.png)

After installation, execute the `uvx` command, and you should see the following output, indicating successful installation:

![](https://pic.yupi.icu/1/image-20260109113427041.png)

2) Open Cursor settings, find the MCP configuration option, and click to add MCP:

![](https://pic.yupi.icu/1/image-20260109113809834.png)

3) Cursor manages MCP through JSON files. Add the previously copied MCP server configuration:

```json
{
  "mcpServers": {
    "time": {
      "command": "uvx",
      "args": [
        "mcp-server-time",
        "--local-timezone=America/New_York"
      ]
    }
  }
}
```

![](https://pic.yupi.icu/1/image-20260109112647904.png)

4) After saving, you’ll notice the MCP tool is successfully enabled. Now AI can fetch the latest time.

![](https://pic.yupi.icu/1/image-20260109113524465.png)

5) You can ask AI: What time is it now?

AI will call MCP to give you the most accurate time.

![](https://pic.yupi.icu/1/image-20260109113631840.png)

### Recommended MCP Services

Besides the time MCP demonstrated earlier, there are many practical MCP services to enhance AI programming efficiency.

**Web-related**:
- Firecrawl MCP: Allows AI to automatically scrape and understand web content.
- Brave Search MCP: Privacy-focused web search.
- Context7: Fetches the latest technical documentation.
- Web to MCP: Replicates web components.
- Chrome DevTools MCP: Browser debugging.

**Code Hosting**:
- GitHub MCP: Operates GitHub code repositories.

**Deployment-related**:
- EdgeOne Pages MCP: One-click deployment to Tencent Cloud.

**File Storage**:
- COS MCP: Operates Tencent Cloud Object Storage.

These MCP services significantly expand AI’s capabilities, transforming it from a mere code generator into a versatile assistant capable of web searching, file operations, and project deployment.

![](https://pic.yupi.icu/1/image-20260116122211103.png)

If you want to learn more about MCP services, visit:

- ⭐️ [Yupi AI Navigation - MCP Encyclopedia](https://ai.codefather.cn/mcp): Continuously updated with quality MCP services to reshape your AI workflow.
- [mcp.so](https://mcp.so/): MCP server marketplace, offering various MCP services.
- [GitHub awesome-mcp-servers](https://github.com/punkpeye/awesome-mcp-servers): Community-maintained list of MCP servers.

These websites continuously update the latest MCP services—recommended to bookmark.

## 4. Standardized Development Tools

Beyond version control, deployment platforms, and MCP, some standardized development tools can enhance project quality.

### Spec-kit and OpenSpec

If you want AI to follow a standardized development process, consider **Specification-Driven Development** (SDD) tools.

The core idea of these tools is to first write requirements into specification documents, then have AI strictly adhere to these specifications when generating code, ensuring code quality and alignment with requirements.

![](https://pic.yupi.icu/1/image-20260116161926017.png)

1) Spec-kit: A specification-driven development framework launched by GitHub, suitable for starting large new projects from scratch.

![](https://pic.yupi.icu/1/%E6%BC%AB%E7%94%BB%E5%9B%BE4%E5%A4%A7.jpeg)

2) OpenSpec: A lightweight specification-driven development framework, suitable for iterating features on existing projects.

![](https://pic.yupi.icu/1/%E6%BC%AB%E7%94%BB%E5%9B%BE6%E5%A4%A7.jpeg)

For more detailed usage of these tools, refer to the “Tool Practice” section of this Vibe Coding tutorial.

### Agent Skills

Agent Skills is an AI skill system launched by Anthropic, allowing complex task instructions, scripts, and resources to be packaged into a skill, enabling AI to quickly learn new capabilities.

![](https://pic.yupi.icu/1/%25E6%25BC%25AB%25E7%2594%25BB%25E5%259B%25BE7%25E5%25A4%25