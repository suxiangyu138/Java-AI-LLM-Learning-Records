import { defineConfig } from "vuepress/config";
import extraSideBar from "./extraSideBar";
import footer from "./footer";
import navbar from "./navbar";
import sidebar from "./sidebar";

const author = "程序员鱼皮";
const domain = "https://ai.codefather.cn";
const tags = [
  "ai",
  "deepseek",
  "AI 资讯",
  "人工智能",
  "AI 行业趋势",
  "AI 技术",
  "AI 新闻",
  "AI 动态",
  "AI 市场分析",
  "AI 模型",
  "AI 独家分析",
  "AI 深度解读",
];

export default defineConfig({
  title: "鱼皮 AI 知识库",
  description:
    "鱼皮 AI 知识库 - 免费 DeepSeek 教程｜工具站｜资源库，是一站式开源免费的人工智能知识分享平台，汇集 Deepseek、GPT 等热门 AI 工具介绍、使用指南、技巧分享、应用场景、AI 变现、行业资讯、教程资源汇总，提供系统化的 AI 教程、精选 AI 资源，助你快速掌握 AI 技术，成为 AI 专家！",
  head: [
    // 站点图标
    ["link", { rel: "icon", href: "/favicon.ico" }],
    // SEO
    [
      "meta",
      {
        name: "keywords",
        content:
          "ai, deepseek, AI 资讯，人工智能，AI 行业趋势，AI 技术，AI 新闻，AI 动态，AI 市场分析，AI 模型，AI 独家分析，AI 深度解读",
      },
    ],
    // 百度统计
    [
      "script",
      {},
      `
        var _hmt = _hmt || [];
        (function() {
          var hm = document.createElement("script");
          hm.src = "https://hm.baidu.com/hm.js?6998d638562bceef30be297767e91d64";
          var s = document.getElementsByTagName("script")[0]; 
          s.parentNode.insertBefore(hm, s);
        })();
      `,
    ],
  ],
  permalink: "/:slug",

  // 监听文件变化，热更新
  extraWatchFiles: [".vuepress/*.ts", ".vuepress/sidebars/*.ts"],
  markdown: {
    // 开启代码块的行号
    lineNumbers: true,
    // 支持 4 级以上的标题渲染
    extractHeaders: ["h2", "h3", "h4", "h5", "h6"],
  },
  // @ts-ignore
  plugins: [
    ["@vuepress/back-to-top"],
    // Google 分析
    [
      "@vuepress/google-analytics",
      {
        ga: "GTM-WVS9HM6W", // 补充自己的谷歌分析 ID，比如 UA-00000000-0
      },
    ],
    ["@vuepress/medium-zoom"],
    // https://github.com/lorisleiva/vuepress-plugin-seo
    [
      "seo",
      {
        siteTitle: (_, $site) => $site.title + " - 免费 DeepSeek 教程｜工具站｜资源库",
        title: ($page) => $page.title + " - 免费 DeepSeek 教程｜工具站｜资源库",
        description: ($page) => $page.frontmatter.description || $page.description,
        author: (_, $site) => $site.themeConfig.author || author,
        tags: ($page) => $page.frontmatter.tags || tags,
        type: ($page) => "article",
        url: (_, $site, path) => ($site.themeConfig.domain || domain || "") + path,
        image: ($page, $site) =>
          $page.frontmatter.image &&
          (($site.themeConfig.domain && !$page.frontmatter.image.startsWith("http")) || "") + $page.frontmatter.image,
        publishedAt: ($page) => $page.frontmatter.date && new Date($page.frontmatter.date),
        modifiedAt: ($page) => $page.lastUpdated && new Date($page.lastUpdated),
      },
    ],
    // https://github.com/ekoeryanto/vuepress-plugin-sitemap
    [
      "sitemap",
      {
        hostname: domain,
      },
    ],
    // https://github.com/IOriens/vuepress-plugin-baidu-autopush
    ["vuepress-plugin-baidu-autopush"],
    // https://github.com/zq99299/vuepress-plugin/tree/master/vuepress-plugin-tags
    ["vuepress-plugin-tags"],
    // https://github.com/znicholasbrown/vuepress-plugin-code-copy
    [
      "vuepress-plugin-code-copy",
      {
        successText: "代码已复制",
      },
    ],
    // https://github.com/webmasterish/vuepress-plugin-feed
    [
      "feed",
      {
        canonical_base: domain,
        count: 10000,
        // 需要自动推送的文档目录
        posts_directories: [],
      },
    ],
    // https://github.com/tolking/vuepress-plugin-img-lazy
    ["img-lazy"],
  ],
  // 主题配置
  themeConfig: {
    logo: "/logo.png",
    nav: navbar,
    sidebar,
    lastUpdated: "最近更新",

    // GitHub 仓库位置
    repo: "liyupi/ai-guide",
    docsBranch: "master",

    // 编辑链接
    editLinks: true,
    editLinkText: "完善页面",

    // @ts-ignore
    // 底部版权信息
    footer,
    // 额外右侧边栏
    extraSideBar,
  },
});
