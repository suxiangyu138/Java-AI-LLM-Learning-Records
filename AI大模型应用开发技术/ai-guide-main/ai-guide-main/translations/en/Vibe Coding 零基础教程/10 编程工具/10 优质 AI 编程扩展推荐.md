# Top AI Programming Extensions Recommendations

Hello everyone, I'm programmer Yupi. Today I'll share some AI programming extensions I personally use to significantly improve AI programming efficiency and code quality.

**10,000-word guide + 100+ images**, pure干货! Bookmark this and let's get started~

## 1. MCP Server Category

MCP stands for Model Context Protocol. Simply put, it's an open standard that allows AI models to connect with external tools and data sources.

Think of MCP as a USB-C port for AI. Originally, AI could only answer questions or generate code based on its training data. But with this universal interface, it can now connect to various external tools - like opening browsers to view websites, searching and scraping web content, deploying projects to the cloud, accessing databases, etc. Its capabilities instantly become much richer.

![](https://pic.yupi.icu/1/%E6%BC%AB%E7%94%BB%E5%9B%BE1%E5%A4%A7.jpeg)

### ⭐️ Firecrawl MCP for Web Content Scraping

First is [Firecrawl MCP](https://www.firecrawl.dev/), which enables AI to automatically scrape and understand web content.

When developing projects, I often need to gather reference materials from the web, read official documentation and tech blogs, or analyze competitors' feature implementations. Doing this manually requires opening websites, copying and pasting content, or writing custom scraping scripts - a huge hassle.

With Firecrawl MCP, it becomes much simpler. I just tell my AI programming tool:
- Get me the content from this website
- Read this document for me
- Search online for information about XX

It automatically retrieves the webpage's content, structure, even dynamically loaded data.

![](https://pic.yupi.icu/1/image-20260116105912027.png)

**How to use?**

First register on [Firecrawl's website](https://www.firecrawl.dev/app/api-keys) and create an API Key for service calls.

![](https://pic.yupi.icu/1/image-20260116105955795.png)

Then configure the MCP server in your AI programming tool. Using Cursor as an example (other tools have similar MCP integration methods - check their official docs like [Claude Code MCP integration](https://docs.anthropic.com/en/docs/claude-code/mcp)).

Open Cursor settings, find Tools & MCP, click `+ New MCP Server`.

![](https://pic.yupi.icu/1/image-20260116110425690.png)

Essentially you're modifying the MCP config file to add:

```json
{
  "mcpServers": {
    "firecrawl-mcp": {
      "command": "npx",
      "args": ["-y", "firecrawl-mcp"],
      "env": {
        "FIRECRAWL_API_KEY": "Your_API_Key"
      }
    }
  }
}
```

![](https://pic.yupi.icu/1/image-20260116110454499.png)

This configuration means: Run the firecrawl-mcp tool via npx command and pass your API key to it. If you don't have npx installed, first [install Node.js](https://nodejs.org/zh-cn) - npx comes bundled with it.

After configuration, green success dots indicate it's ready to use.

![](https://pic.yupi.icu/1/image-20260116110558199.png)

Beyond basic web scraping, Firecrawl MCP also supports batch site scraping, recursive multi-layer link scraping, automatic retries on failure, and other advanced features.

### Brave Search MCP for Private Searching

Next is [Brave Search MCP](https://github.com/brave/brave-search-mcp-server), enabling AI to perform privacy-focused web searches.

During development, I often need AI to search for the latest technical materials, find library usage examples, or troubleshoot technical issues. The traditional approach requires manually searching and copying results to AI - quite tedious.

With Brave Search MCP, I simply tell AI:
- Search for React 19's new features
- Look up how to fix this error

It uses Brave Search to find answers. Brave doesn't track search history, offering excellent privacy protection.

![](https://pic.yupi.icu/1/image-20260116111803869.png)

**How to use?**

First register at [Brave Search API](https://brave.com/search/api/), then go to API Key management. You must select the free plan! 2000 monthly queries are enough for personal development.

![](https://pic.yupi.icu/1/image-20260116110947801.png)

One catch: Even the free plan requires payment method setup. Those without overseas bank cards may need to skip.

After subscription, create an API Key:

![](https://pic.yupi.icu/1/image-20260116111311536.png)

With the API Key, add to Cursor's MCP config:

```json
{
  "mcpServers": {
    "brave-search": {
      "command": "npx",
      "args": ["-y", "brave-search-mcp"],
      "env": {
        "BRAVE_API_KEY": "Your_API_Key"
      }
    }
  }
}
```

Once configured, AI can search for latest information anytime.

Supports web, image, video, news searches, even local business info (like nearby coffee shops).

![](https://pic.yupi.icu/1/image-20260116111954334.png)

It also features AI summarization, automatically condensing search results into concise answers.

### ⭐️ Context7 for Latest Documentation

[Context7](https://context7.com/) helps AI access the latest technical documentation.

We know AI training data has cutoff dates - GPT-4's knowledge might only be current until 2023. This creates problems when asking about latest framework versions - answers may be outdated.

Context7 solves this by automatically scraping latest version-specific docs from official sites for AI.

![](https://pic.yupi.icu/1/image-20260116112229490.png)

Thus, AI's code examples and suggestions are based on current docs, avoiding deprecated practices, significantly improving project success rates.

**How to use?**

Visit [Context7 Dashboard](https://context7.com/dashboard) to register and get an API Key (free for personal use).

![](https://pic.yupi.icu/1/image-20260116112322940.png)

Add to MCP config:

```json
{
  "mcpServers": {
    "context7": {
      "url": "https://mcp.context7.com/mcp",
      "headers": {
        "CONTEXT7_API_KEY": "Your_API_Key"
      }
    }
  }
}
```

When discussing technical docs with AI or mentioning "use context7", it automatically fetches latest documentation.

![](https://pic.yupi.icu/1/image-20260116112656483.png)

### Web to MCP for UI Component Replication

[Web to MCP](https://web-to-mcp.com/) is a Chrome extension that works with MCP to send any webpage UI component directly to AI for code generation - the fastest way to "copy homework"!

![](https://pic.yupi.icu/1/image-20260116113052973.png)

Often when browsing sites, I see nice UI components and want AI to recreate similar effects. Previously, I'd screenshot and describe: "Make a button like this - rounded corners, gradient, with shadow..." Time-consuming and imprecise.

With Web to MCP, I just click the element I want to replicate:

![](https://pic.yupi.icu/1/image-20260116113725321.png)

It automatically captures the component's DOM structure, CSS styles, even interactions, providing a prompt for AI replication.

Just send the prompt to AI, which uses MCP to get complete component info and generates replication code.

![](https://pic.yupi.icu/1/image-20260116114142631.png)

Compared to vague screenshots, the generated code is much more accurate.

![](https://pic.yupi.icu/1/image-20260116114426822.png)

**How to use?**

1) Install extension via website or Chrome Web Store search

![](https://pic.yupi.icu/1/image-20260116113138693.png)

2) Log in with Google account to get your MCP config:

![](https://pic.yupi.icu/1/image-20260116113241575.png)

3) Add to AI tool's MCP config:

```json
{
  "mcpServers": {
    "web-to-mcp": {
      "url": "https://web-to-mcp.com/mcp/your_unique_ID"
    }
  }
}
```

When browsing, click the extension icon, select desired components, and directly reference them in your AI tool to quickly generate style-consistent code.

### Chrome DevTools MCP for Browser Debugging

[Chrome DevTools MCP](https://github.com/ChromeDevTools/chrome-devtools-mcp) by Chrome's team lets AI directly control Chrome for operations and debugging.

For frontend development, I often need to debug pages, inspect network requests, analyze performance. Previously manual in browser DevTools, now AI can do it.

For example: "Why is this site loading slowly?" AI opens DevTools, analyzes network requests, checks resource load times, then identifies issues.

![](https://pic.yupi.icu/1/image-20260116115138719.png)

Or: "Test this form submission" - it auto-fills forms, clicks submit, checks responses.

**How to use?**

Add to MCP config:

```json
{
  "mcpServers": {
    "chrome-devtools": {
      "command": "npx",
      "args": ["-y", "chrome-devtools-mcp@latest"]
    }
  }
}
```

After configuration, AI can automate testing and debugging. The tool auto-connects to your running Chrome browser without extra setup.

Also supports element locating, network monitoring, performance analysis, screenshots - ideal for frontend development and testing.

### EdgeOne Pages MCP for One-Click Deployment

[EdgeOne Pages MCP](https://github.com/TencentEdgeOne/edgeone-pages-mcp) by Tencent Cloud deploys projects to Tencent's accelerated network for public access and speed boosts.

After development, you'll want others to access your site. Traditional deployment is cumbersome - manually packaging code, uploading to servers, configuring domains, setting HTTPS certificates - takes significant time.

![](https://pic.yupi.icu/1/%E6%BC%AB%E7%94%BB%E5%9B%BE2%E5%A4%A7.jpeg)

With EdgeOne Pages MCP, I just tell AI: "Deploy this project" - it handles packaging, uploading, deployment, providing a live URL. Deployed to global accelerated network for fast worldwide access.

![](https://pic.yupi.icu/1/1752212029384-16cfba8f-babb-49c0-9d41-3b76ee78eecf.png)

**How to use?**

First enable Pages service at [EdgeOne Console](https://console.cloud.tencent.com/edgeone/pages):

![](https://pic.yupi.icu/1/1752209404627-3a3193d9-4c94-4f80-ad02-435ff22f16ed.png)

Get API Token as service credential:

![](https://pic.yupi.icu/1/1752209504079-6741bbd0-438e-48a4-86be-6eddfc3efa83.png)

Add to MCP config:

```json
{
  "mcpServers": {
    "edgeone-pages-mcp-server": {
      "command": "npx",
      "args": ["edgeone-pages-mcp"],
      "env": {
        "EDGEONE_PAGES_API_TOKEN": "Your_API_Token"
      }
    }
  }
}
```

After configuration, AI can one-click deploy projects. Free deployment supports static sites, full-stack projects, auto HTTPS and CDN - perfect for personal projects and small apps.

![](https://pic.yupi.icu/1/1752211869664-b74dc4d9-57b1-4bb7-9f02-703112b613eb.png)

### COS MCP for Object Storage

[COS MCP](https://github.com/Tencent/cos-mcp) lets AI directly operate Tencent Cloud Object Storage.

Object storage is cloud file storage - think cloud drives.

![](https://pic.yupi.icu/1/image-20260116120009365.png)

For team collaboration, we often need AI to reference project specs or images. Previously storing files locally and manually uploading to AI was inconvenient and hard to maintain/share/modify across teams.

With COS MCP, I can store shared files in the cloud and have AI directly access them.

![](https://pic.yupi.icu/1/image-20260116122211103.png)

For example: "Write this feature according to our team's COS-shared project specs" - AI reads specs from COS and codes accordingly.

![](https://pic.yupi.icu/1/image-20260116122453843.png)

**How to use?**

1) First enable Tencent Cloud COS service at [COS Console](https://console.cloud.tencent.com/cos), create a Bucket:

![](https://pic.yupi.icu/1/image-20260116120711822.png)

2) Get SecretId and SecretKey at "Access Management" > "API Key Management" - keep these secure!

![](https://pic.yupi.icu/1/image-20260116121022222.png)

3) Add to MCP config:

```json
{
  "mcpServers": {
    "cos-mcp": {
      "command": "npx",
      "args": [
        "cos-mcp",
        "--Region=your_region",
        "--Bucket=your_bucket",
        "--SecretId=your_SecretId",
        "--SecretKey=your_SecretKey"
      ]
    }
  }
}
```

![](https://pic.yupi.icu/1/image-20260116121243549.png)

After configuration, AI can read/manage your cloud files - essentially giving AI a cloud drive.

Also supports image search, processing, document conversion, video thumbnail generation.

![](https://pic.yupi.icu/1/image-20260116121658701.png)

### GitHub MCP for Repository Management

[GitHub MCP](https://github.com/github/github-mcp-server) by GitHub lets AI directly operate GitHub repositories.

Programmers know GitHub - the largest code hosting platform for storing code and collaborative development.

![](https://pic.yupi.icu/1/image-20260116122621938.png)

Daily development may require searching repos, creating issues, submitting PRs, reviewing changes, analyzing commit history. Previously manual on GitHub website, now AI can do it.

For example: "What projects have I recently open-sourced on GitHub? How many stars?"

![](https://pic.yupi.icu/1/image-20260116123701822.png)

It quickly generates a data report:

![](https://pic.yupi.icu/1/image-20260116123734660.png)

Or: "Show recent week's code changes" - analyzes Git history to show modifications.

![](https://pic.yupi.icu/1/image-20260116124022290.png)

**How to use?**

First get your [Access Token](https://github.com/settings/tokens) as credential:

![](https://pic.yupi.icu/1/image-20260116122846165.png)

Add to MCP config:

```json
{
  "mcpServers": {
    "github": {
      "url": "https://api.githubcopilot.com/mcp/",
      "headers": {
        "Authorization": "Bearer your_GitHub_token"
      }
    }
  }
}
```

Also supports code analysis, CI/CD monitoring, security scanning - nearly anything you can do on GitHub, AI can do.

![](https://pic.yupi.icu/1/image-20260116123148068.png)

## 2. IDE Extension Plugins

Now let's discuss IDE extensions.

IDEs are integrated development environments - essentially code editors like VS Code, JetBrains IDEA. Plugins enhance editor capabilities for better development experience.

Many GUI-based AI programming tools (like Cursor) are VS Code-based, thus supporting VS Code plugins. Below I'll mainly share VS Code plugins that work out-of-the-box.

![](https://pic.yupi.icu/1/image-20260116124127539.png)

### Claude Code Official Extension

Claude Code by Anthropic was originally a standalone CLI tool. The [Claude Code VS Code Extension](https://www.anthropic.com/news/enabling-claude-code-to-work-more-autonomously) lets you use it directly in editors without separate terminals.

Search "Claude Code" in VS Code or Cursor's extension marketplace to install:

![](https://pic.yupi.icu/1/image-20260116124255886.png)

This extension provides GUI - converse with Claude via sidebar panel with flexible text input.

![](https://pic.yupi.icu/1/image-20260116124614180.png)

When AI modifies code, changes appear in real-time with diff comparisons showing exactly what changed.

![](https://pic.yupi.icu/1/image-20260116124700221.png)

I often use it for refactoring, bug fixes, new features. Supports parallel multi-sessions - multiple Claude agents handling different tasks (e.g. one for frontend, one for backend) for efficiency.

![](https://pic.yupi.icu/1/image-20260116124928547.png)

Similar plugins include Cline, GitHub Copilot - choose based on preference.

### GitLens for Git Visualization

[GitLens](https://www.gitkraken.com/gitlens) provides intuitive Git change history viewing.

![](https://pic.yupi.icu/1/image-20260116125249627.png)

Git manages code versions - recording every change: who, when, why.

With these records, when bugs appear I can quickly find the "culprit".

![](https://pic.yupi.icu/1/%E6%BC%AB%E7%94%BB%E5%9B%BE3%E5%A4%A7.jpeg)

But Git is CLI-only - viewing history requires manual commands in uncomfortable formats.

![](https://pic.yupi.icu/1/image-20260116125146713.png)

After installing GitLens from editor marketplace, hovering over any code line shows author, related PRs, etc.

![](https://pic.yupi.icu/1/image-20260116125445257.png)

The Git management panel clearly displays project commit history.

![](https://pic.yupi.icu/1/image-20260116125736701.png)

Also features AI capabilities - auto-generating commit messages, explaining changes, changelogs, and AI explanations of modification purposes.

![](https://pic.yupi.icu/1/image-20260116130129466.png)

### ⭐️ Office Viewer for Document Preview