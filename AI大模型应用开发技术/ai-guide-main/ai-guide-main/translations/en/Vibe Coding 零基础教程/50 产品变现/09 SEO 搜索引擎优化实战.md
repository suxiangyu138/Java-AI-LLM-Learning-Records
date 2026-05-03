# SEO Search Engine Optimization Practical Guide

> Let more people discover your product

Hello everyone, I'm programmer Yupi. Our team's programmer interview question bank website [Mianshiya](https://www.mianshiya.com/) was indexed and recommended by major search engines like Baidu within less than half a month of its launch!

The effect is obvious. When users search for "Mianshiya," the first thing they see is our own website, thereby increasing traffic to the site:

![](https://pic.yupi.icu/1/image-20240815102430905.png)

Getting search engines to index your website faster is actually a deep subject, and there's a professional term for it: SEO. For individual website owners, search engine traffic is crucial. Everyone wants their website to be seen by more people, right? Even without considering profits, having a website with high traffic can be a great talking point when writing resumes and during interviews. So, I recommend that programmer friends should have a basic understanding of SEO.

In this article, I'll use my Mianshiya website as an example to share some practical SEO tips to help your website get indexed by search engines faster. Additionally, I'll introduce a new concept in the AI era — GEO (Generative Engine Optimization) — to help your product be discovered by more people in the AI search era.

Whether you're using Vibe Coding for personal projects or aiming to create a real product, mastering SEO and GEO methods can help you gain more traffic.

⭐️ You can also watch the video explanation here: https://www.bilibili.com/video/BV1tz421i7Q1

## Yupi's SEO Insights

### 1. What is SEO?

SEO stands for Search Engine Optimization, which makes it easier for search engines to index and display your website. This allows more people to discover your site through search engines like Baidu and Google, thereby increasing website traffic and visibility.

Before learning how to optimize for SEO, let's briefly understand the SEO process: how do search engines discover your website and make it searchable for users?

### 2. The SEO Process

The SEO process can be divided into four main stages: crawling, indexing, ranking, and indexing. Below, I'll explain these four steps in detail.

#### 1. Crawling

Crawling is the first step in the SEO process. Search engines send out crawler programs (commonly known as spiders) that crawl the internet, visit various websites, and capture webpage content. These spiders follow links from one page to another, attempting to traverse the entire website.

#### 2. Indexing

After crawling, search engines analyze the webpage content and decide whether to include the page in their database. Only indexed pages can appear in user search results, so ensuring your pages are indexed is a crucial step in SEO. Some websites may have many links and content, but if search engine spiders don't like or index them, users won't find them even if they search specifically for your site.

#### 3. Ranking

Ranking refers to the process where search engines organize and categorize the indexed webpage content, creating a massive index library. This process is similar to tagging each webpage so that when users search, the search engine can quickly find pages related to the search terms.

For example, our Mianshiya website includes: Java interview questions, front-end interview questions, C++ interview questions. These terms could be set as indexes. If users search for content containing these terms, they might find our website.

#### 4. Ranking

With so many websites and indexes, how can we ensure users find our website first? This involves the final step of SEO — ranking.

When users enter keywords into a search engine, the search engine uses its algorithm to select the most relevant pages from the index library and **sort them based on relevance, weight, and website quality** to determine which pages appear on the first few pages of search results.

This is the SEO process. The SEO optimization tips I'm about to share are all centered around these processes.

### 3. How to Optimize for SEO?

#### 1. Keyword Optimization
Keywords are the terms users enter into search engines. You can increase your website's indexing and improve its ranking in relevant searches by setting keywords (Keywords) and descriptions (Description) in the HTML head information of your website.

Keyword selection needs to be precise and strongly related to your website content. Avoid keyword stuffing, as it can be flagged as cheating by search engines.

For example, for an interview question bank website, you can set the following keywords and description:

```html
<meta name="keywords" content="programmer interview,Java interview questions,programmer job search,computer">
<meta name="description" content="For programmer interview practice, come to Mianshiya, a free interview question bank website for programmers. Massive high-frequency Java interview questions to help you prepare for technical interviews.">
```

#### 2. Website Structure Optimization
Website structure optimization can be divided into two points: overall page structure optimization and content structure optimization for each page.

For the entire website, the nesting hierarchy of pages should be as flat as possible, minimizing page levels to reduce the difficulty for crawlers to crawl.

For example, which of the following two website structures do you think is easier for crawlers to fully access?

![](https://pic.yupi.icu/1/image-20240815112845394.png)

The answer is obvious. For important pages you want to be discovered by search engines faster, you should minimize the path to reach the page and appropriately increase the number of entry points to that page.

For each page, there should be a clear hierarchical structure. Use reasonable heading tags (such as `<h1>` for first-level headings) to make the page content easier to index.

#### 3. Friendly Links

When I first started building personal websites in college, I used friendly links to increase website weight (though the effect was limited). The method is simple: add links to other websites on your site, and have other websites add links to your site. This mutual recommendation makes it easier to improve rankings in search engines.

The principle behind friendly links is simple. Many search engines rank websites based on weight. How is weight calculated? A simple algorithm (Page Rank) assigns each website a number of votes. Each friendly link from another website to yours counts as a vote for your site. Websites with more votes gain higher weight and ranking. Friendly links are like mutual voting, which is better than having no votes at all.

![](https://pic.yupi.icu/1/image-20240815113641780.png)

Of course, this mutual recommendation method should be used cautiously. Excessive link exchanges can lead to weight dispersion.

#### 4. Sitemap File
A Sitemap is a file that lists all the pages on your website, usually placed in the root directory or specified via the robots.txt file. It helps search engines quickly understand your website's structure and crawl the pages you want indexed first.

It's like giving the crawler a map, so it doesn't get lost and doesn't miss important pages on your site.

For simpler websites, a static, unchanging Sitemap is sufficient. For example:

![](https://pic.yupi.icu/1/image-20240815114215436.png)

For websites with continuously updated content, there's a more advanced approach: using programs to automatically generate dynamic Sitemaps. For example, generating a Sitemap file for daily new questions helps crawlers discover the latest content faster.

Additionally, some search engines support actively uploading Sitemap files, which can further shorten the time it takes for your site to be discovered and indexed.

#### 5. SSR Server-Side Rendering

Note: SSR here is not the one we talk about when playing gacha games!

SSR (Server-Side Rendering) is one of the most effective SEO techniques. It involves generating **complete HTML pages** on the server and sending them directly to the browser. Compared to traditional front-end AJAX dynamic data rendering, SSR makes it easier for search engines to crawl complete page content, thereby improving SEO.

For example, if it's a front-end website that dynamically requests data, the crawler might see incomplete webpage content, as shown below:

![](https://pic.yupi.icu/1/image-20240815114606517.png)

This is because the browser pulls the webpage from the server, then loads JS scripts, and finally sends requests to fetch data.

If SSR is used, the server completes the data request and assembles the data into the page before returning it to the front-end. The crawler sees more complete webpage content, as shown below:

![](https://pic.yupi.icu/1/image-20240815114910820.png)

While SSR is effective, it also increases server pressure and usually involves higher development costs. For example, our Mianshiya website was developed using the Next.js framework, and we encountered many pitfalls during development.

Oh, by the way, using PHP to develop SSR websites is very convenient, which might be one reason why PHP was so popular in the past.

#### 6. SSG Static Site Generation

Similar to SSR, SSG is another major SEO optimization tool. It involves pre-generating **static HTML files** for all pages during website construction and deploying them directly to the server. When users visit the site, they directly receive the pre-generated HTML files. Unlike SSR, there's no need for the server to temporarily request data.

This method not only significantly improves page loading speed but also allows search engines to index all pages faster and more completely. Many blog site generators (like Hugo, VuePress, Hexo) package written articles into static HTML files before deploying them to the server.

Of course, SSG is not a silver bullet. It's suitable for websites with relatively fixed content and low update frequency, such as personal blogs. Static websites are essentially a form of cache. If webpage content changes frequently, updating these files frequently can also incur significant costs.

So, we can think of a more advanced strategy: combining SSR + SSG! Use static generation for relatively fixed content pages, SSR for content-changing pages, and pure client-side rendering for pages that don't need SEO.

#### 7. Spend Money

Note: The above methods do not guarantee absolute effectiveness but increase the probability of search engine indexing and ranking optimization. SEO strategies require continuous adjustment and long-term validation.

If there's no SEO-savvy technical staff in your team and you want your website to be recommended by search engines quickly, then you can only "spend money." It's simple and brutal: buy ads to make your webpage appear in the top few search results. Many companies do this, but for individual website owners without revenue, it's better to stick to the methods recommended above.

## GEO in the AI Era: Generative Engine Optimization

With the popularity of AI conversational tools like ChatGPT, more users are starting to use AI to search for information instead of traditional search engines. This has given rise to a new concept: **GEO (Generative Engine Optimization)**.

### What is GEO?

GEO refers to optimizing your content to make it easier for AI search engines (like ChatGPT, Perplexity, Gemini) to understand and reference. When users ask AI questions, the AI crawls relevant content from the internet and generates answers. If your website content is referenced by AI, it can gain more exposure.

### Differences Between GEO and SEO

- SEO: Optimizing websites to make them easier for traditional search engines (like Baidu, Google) to index and rank.
- GEO: Optimizing content to make it easier for AI search engines to understand and reference.

### How to Excel at GEO?

1) Structured Content

AI prefers well-structured content. Use headings, lists, tables, etc., to organize content, making it easier for AI to extract key information.

2) Provide Authoritative Information

AI tends to reference authoritative and accurate information. Ensure your content is data-backed and cited, enhancing its credibility.

3) Use Natural Language

AI has strong natural language understanding capabilities. Write in plain, easy-to-understand language, avoiding overly specialized jargon, making it easier for AI to understand your content.

4) Answer User Questions

AI searches are typically conducted in a Q&A format. Directly answer potential user questions in your content, such as "How to do XX" or "What is XX," making it easier for AI to reference.

5) Keep Content Updated

AI prefers to reference the latest content. Regularly update your website content to maintain information timeliness.

6) Provide Complete Context

AI needs to understand the context of content to reference it accurately. Ensure each article has a complete background introduction and detailed explanations, avoiding confusion for readers (and AI).

## Final Thoughts

In the Vibe Coding era, creating products with AI is easy. However, getting more people to discover your product still requires mastering SEO and GEO methods.

Keep pushing forward, and let more people discover your product!

## Recommended Resources

1) Yupi's AI Navigation Website: [Comprehensive AI Resources, Latest AI News, Free AI Tutorials](https://ai.codefather.cn)

2) Programming Navigation Learning Circle: [Learning Paths, Programming Tutorials, Practical Projects, Job Search Guides, Q&A](https://www.codefather.cn)

3) Programmer Interview Question Bank: [Internship/Campus Recruitment/Social Recruitment High-Frequency Exam Points, Enterprise Real Questions Analysis](https://www.mianshiya.com)

4) Programmer Resume Writing Tool: [Professional Templates, Rich Examples, Direct to Interviews](https://www.laoyujianli.com)

5) 1-on-1 Mock Interviews: [Essential for Internship/Campus Recruitment/Social Recruitment Interviews to Secure Offers](https://ai.mianshiya.com)