// .vuepress/config.ts
import { defineConfig } from "vuepress/config";

// .vuepress/extraSideBar.ts
var extraSideBar_default = [
  {
    title: "\u7F16\u7A0B\u5BFC\u822A",
    icon: "/icon/xingqiu.png",
    popoverTitle: "\u5FAE\u4FE1\u626B\u4E00\u626B",
    popoverUrl: "/qrcode-codefather.png",
    popoverDesc: "\u7F16\u7A0B\u5BFC\u822A\uFF1A\u7F16\u7A0B\u5BFC\u822A"
  },
  {
    title: "\u4EA4\u6D41\u7FA4",
    icon: "/icon/weixin.png",
    popoverTitle: '<span style="font-size:0.8rem;font-weight:bold;">\u626B\u7801\u6DFB\u52A0 <span style="color:red;">\u7F16\u7A0B\u5BFC\u822A\u5C0F\u52A9\u624B\u5FAE\u4FE1</span>\uFF0C\u62C9\u4F60\u8FDB\u4E13\u5C5E\u7F16\u7A0B\u5B66\u4E60\u4EA4\u6D41\u7FA4</span>',
    popoverUrl: "/qrcode-codenavhelper.png"
  },
  {
    title: "\u4E0B\u8D44\u6599",
    icon: "/icon/xiazai.png",
    popoverTitle: '<span style="font-size:0.8rem;font-weight:bold;">\u626B\u7801\u5173\u6CE8\u516C\u4F17\u53F7\uFF0C\u56DE\u590D <span style="color:red;">ai</span> \u83B7\u53D6\u6E05\u534E\u5927\u5B66 DeepSeek \u4ECE\u5165\u95E8\u5230\u7CBE\u901A PDF</span>',
    popoverUrl: "/qrcode-mpcoder_yupi.jpg",
    popoverDesc: "\u516C\u4F17\u53F7: \u7A0B\u5E8F\u5458\u9C7C\u76AE"
  },
  {
    title: "\u652F\u6301\u6211",
    icon: "/icon/dianzan.png",
    popoverTitle: ' <span style="font-size:0.8rem;font-weight:bold;">\u9F13\u52B1\u548C\u8D5E\u8D4F\u6211</span>',
    popoverUrl: "/qrcode-thumb.jpg",
    popoverDesc: "\u611F\u8C22\u60A8\u7684\u652F\u6301\uFF0C\u4F5C\u8005\u5934\u53D1++"
  }
];

// .vuepress/footer.ts
var footer_default = {
  friendLinks: [
    {
      label: "\u7AD9\u957F - \u7A0B\u5E8F\u5458\u9C7C\u76AE",
      href: "https://yuyuanweb.feishu.cn/wiki/Abldw5WkjidySxkKxU2cQdAtnah"
    },
    {
      label: "\u9C7C\u9E22\u7F51\u7EDC",
      href: "https://yuyuanweb.com/"
    },
    {
      label: "\u8001\u9C7C\u7B80\u5386",
      href: "https://www.laoyujianli.com/"
    },
    {
      label: "\u9762\u8BD5\u9E2D",
      href: "https://www.mianshiya.com/"
    },
    {
      label: "\u7F16\u7A0B\u5B66\u4E60\u5708",
      href: "https://www.codefather.cn/"
    }
  ],
  copyright: {
    href: "https://beian.miit.gov.cn/",
    name: "\u6CAAICP\u590719026706\u53F7-6"
  }
};

// .vuepress/navbar.ts
var navbar_default = [
  {
    text: "AI \u9879\u76EE",
    items: [
      {
        text: "AI \u6D77\u9F9F\u6C64\u9879\u76EE\u6559\u7A0B",
        link: "/ai-\u6D77\u9F9F\u6C64\u9879\u76EE\u6559\u7A0B/"
      },
      {
        text: "AI + Cursor \u5F00\u53D1\u4E00\u4E2A\u4EB2\u621A\u8BA1\u7B97\u5668",
        link: "/ai-cursor-\u5F00\u53D1\u4E00\u4E2A\u4EB2\u621A\u8BA1\u7B97\u5668/"
      },
      {
        text: "AI + Cursor \u5F00\u53D1\u4E00\u4E2A\u6A21\u62DF\u9762\u8BD5\u7CFB\u7EDF",
        link: "/ai-cursor-\u5F00\u53D1\u4E00\u4E2A\u6A21\u62DF\u9762\u8BD5\u7CFB\u7EDF/"
      },
      {
        text: "AI + Cursor \u5F00\u53D1\u4E00\u4E2A\u80BA\u6D3B\u91CF\u6D4B\u8BD5\u5668",
        link: "/ai-cursor-\u5F00\u53D1\u4E00\u4E2A\u80BA\u6D3B\u91CF\u6D4B\u8BD5\u5668/"
      }
    ]
  },
  {
    text: "Deepseek",
    items: [
      {
        text: "\u5173\u4E8EDeepSeek",
        link: "/Deepseek/#\u5173\u4E8Edeepseek"
      },
      {
        text: "DeepSeek \u4F7F\u7528\u6307\u5357",
        link: "/ai/#deepseek\u4F7F\u7528\u6307\u5357"
      },
      {
        text: "AI \u5E94\u7528\u573A\u666F",
        link: "/ai/#ai\u5E94\u7528\u573A\u666F"
      },
      {
        text: "DeepSeek \u8D44\u6E90\u6C47\u603B",
        link: "/ai/#deepseek\u8D44\u6E90\u6C47\u603B"
      },
      {
        text: "DeepSeek \u6280\u672F\u89E3\u6790",
        link: "/ai/#deepseek\u6280\u672F\u89E3\u6790"
      },
      {
        text: "AI \u884C\u4E1A\u8D44\u8BAF",
        link: "/ai/#AI\u884C\u4E1A\u8D44\u8BAF"
      }
    ]
  },
  {
    text: "\u{1F525}\u7F16\u7A0B\u5B66\u4E60",
    link: "https://www.codefather.cn/"
  },
  {
    text: "AI \u9762\u8BD5\u9898\u5E93",
    link: "https://www.mianshiya.com/?category=ai"
  },
  {
    text: "\u4F5C\u8005",
    link: "/\u4F5C\u8005/"
  }
];

// .vuepress/sidebars/ai.ts
var ai_default = [
  "",
  {
    "title": "\u9C7C\u76AE\u7684 AI \u6307\u5357",
    "collapsable": true,
    "children": [
      "\u9C7C\u76AE\u7684 AI \u6307\u5357/\u9C7C\u76AE\u7684 AI \u6307\u5357 - 0\u3001\u5F00\u7BC7",
      "\u9C7C\u76AE\u7684 AI \u6307\u5357/\u9C7C\u76AE\u7684 AI \u6307\u5357 - 1\u3001AI \u6838\u5FC3\u6982\u5FF5",
      "\u9C7C\u76AE\u7684 AI \u6307\u5357/\u9C7C\u76AE\u7684 AI \u6307\u5357 - 2\u3001AI \u5B9E\u7528\u5DE5\u5177",
      "\u9C7C\u76AE\u7684 AI \u6307\u5357/\u9C7C\u76AE\u7684 AI \u6307\u5357 - 3\u3001AI \u7F16\u7A0B\u6280\u5DE7",
      "\u9C7C\u76AE\u7684 AI \u6307\u5357/\u9C7C\u76AE\u7684 AI \u6307\u5357 - 4\u3001AI \u7F16\u7A0B\u6280\u672F"
    ]
  },
  {
    "title": "AI\u9879\u76EE\u6559\u7A0B",
    "collapsable": true,
    "children": [
      "AI\u9879\u76EE\u6559\u7A0B/AI \u6D77\u9F9F\u6C64\u9879\u76EE\u6559\u7A0B",
      "AI\u9879\u76EE\u6559\u7A0B/AI + Cursor \u5F00\u53D1\u4E00\u4E2A\u4EB2\u621A\u8BA1\u7B97\u5668",
      "AI\u9879\u76EE\u6559\u7A0B/AI + Cursor \u5F00\u53D1\u4E00\u4E2A\u6A21\u62DF\u9762\u8BD5\u7CFB\u7EDF",
      "AI\u9879\u76EE\u6559\u7A0B/AI + Cursor \u5F00\u53D1\u4E00\u4E2A\u80BA\u6D3B\u91CF\u6D4B\u8BD5\u5668"
    ]
  },
  {
    "title": "\u5173\u4E8EDeepSeek",
    "collapsable": true,
    "children": [
      "\u5173\u4E8EDeepSeek/DeepSeek \u521B\u59CB\u56E2\u961F\u4ECB\u7ECD",
      "\u5173\u4E8EDeepSeek/DeepSeek \u53D1\u5C55\u5386\u7A0B",
      "\u5173\u4E8EDeepSeek/\u4EC0\u4E48\u662F DeepSeek"
    ]
  },
  {
    "title": "DeepSeek\u8D44\u6E90\u6C47\u603B",
    "collapsable": true,
    "children": [
      "DeepSeek\u8D44\u6E90\u6C47\u603B/DeepSeek\u5B98\u65B9\u6574\u7406\u7684\u6A21\u578B\u5E94\u7528\u548C\u5DE5\u5177",
      "DeepSeek\u8D44\u6E90\u6C47\u603B/DeepSeek \u5B66\u4E60\u8D44\u6599",
      "DeepSeek\u8D44\u6E90\u6C47\u603B/DeepSeek \u5B98\u65B9\u94FE\u63A5",
      "DeepSeek\u8D44\u6E90\u6C47\u603B/DeepSeek \u5F00\u6E90\u9879\u76EE"
    ]
  },
  {
    "title": "AI\u884C\u4E1A\u8D44\u8BAF",
    "collapsable": true,
    "children": [
      {
        "title": "2025-04",
        "collapsable": true,
        "children": [
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/Cursor \u8FCE\u6765\u4E86\u5F3A\u5927\u7684\u5BF9\u624B\uFF0CAugment Code \u5B9E\u6D4B",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/\u4E00\u5F20\u7167\u7247\u751F\u6210\u8FDE\u8D2F\u5168\u7247\uFF01Runway Gen4 \u6DF1\u591C\u53D1\u5E03\uFF0C\u7EC8\u4E8E\u6345\u7834 AI \u89C6\u9891\u591A\u5E74\u7684\u5929\u82B1\u677F",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/AI \u751F\u6001\u7684 USB \u63A5\u53E3\uFF1F\u963F\u91CC\u3001\u817E\u8BAF\u5168\u9762\u652F\u6301 MCP",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/5 \u5206\u949F\u76F4\u51FA 46 \u9875\u8BBA\u6587\uFF01\u8C37\u6B4C Deep Research \u5B8C\u7206 OpenAI\uFF0C\u6700\u5F3A Gemini 2.5 \u52A0\u6301",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/Shopify \u65B0\u6807\u51C6\uFF1A\u5C06 AI \u878D\u5165\u65E5\u5E38\u5DE5\u4F5C\uFF0C\u5DF2\u662F\u57FA\u672C\u8981\u6C42",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/\u300A\u806A\u660E\u7684\u673A\u5668\uFF0C\u590D\u6742\u7684\u4EBA\u5FC3\u300B\uFF0C\u65AF\u5766\u798F AI \u62A5\u544A 2025 \u5E74\u7248\u7684 12 \u4E2A\u4FE1\u53F7",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/\u5B9E\u6D4B\uFF1A\u963F\u91CC\u4E91\u767E\u70BC\u4E0A\u7EBF\u300C\u5168\u5468\u671F MCP \u670D\u52A1\u300D\uFF0CAI \u5DE5\u5177\u4E00\u7AD9\u5F0F\u6258\u7BA1",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/\u6B63\u5F0F\u6311\u6218\u8C37\u6B4C\uFF01OpenAI \u4E0A\u7EBF ChatGPT \u641C\u7D22\u529F\u80FD",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/Meta \u6DF1\u591C\u5F00\u6E90 Llama 4\uFF01\u9996\u6B21\u91C7\u7528 MoE\uFF0C\u60CA\u4EBA\u5343\u4E07 token \u4E0A\u4E0B\u6587\uFF0C\u7ADE\u6280\u573A\u8D85\u8D8A DeepSeek",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/\u9A6C\u4E91\u73B0\u8EAB\u963F\u91CC\u4E91\u53D1\u58F0\uFF01\u8C08\u79D1\u6280\u4EBA\u5458\u8D23\u4EFB\uFF1A\u8BA9 AI \u66F4\u61C2\u4EBA\u7C7B",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/Gemini 2.5 Pro \u5B9E\u6D4B\uFF1A\u6216\u5C06\u6210\u4E3A\u6700\u5B9E\u7528\u7684\u63A8\u7406\u6A21\u578B",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/\u963F\u91CC\u79D8\u5BC6\u7814\u53D1\u65B0\u6A21\u578B\u5C06\u53D1\u5E03\uFF0C\u5F71\u54CD\u529B\u6307\u6807\u6210\u6700\u91CD\u8981\u8003\u6838",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/\u6709\u53F2\u4EE5\u6765\u6700\u5927\u529B\u5EA6\uFF01\u82F9\u679C\u8FDB\u519B\u533B\u7597\uFF0C\u8BA1\u5212\u660E\u5E74\u63A8\u51FAAI\u533B\u751F - \u534E\u5C14\u8857\u89C1\u95FB",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/\u4E00\u5F20\u7167\u7247\u751F\u6210\u8FDE\u8D2F\u5168\u7247\uFF01Runway Gen-4 \u6DF1\u591C\u53D1\u5E03\uFF0C\u7EC8\u4E8E\u6345\u7834 AI \u89C6\u9891\u591A\u5E74\u7684\u5929\u82B1\u677F",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/\u667A\u8C31\u53D1\u5E03 AutoGLM \u6C89\u601D\uFF1A\u9996\u4E2A\u514D\u8D39\u3001\u5177\u5907\u6DF1\u5EA6\u7814\u7A76\u548C\u64CD\u4F5C\u80FD\u529B\u7684 AI Agent",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/MiniMax Audio \u53D1\u5E03 Speech-02 \u6A21\u578B\uFF0C\u5355\u6B21\u8F93\u5165\u652F\u6301 20 \u4E07\u5B57\u7B26",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/\u4E9A\u9A6C\u900A\u63A8\u51FA Nova Act\uFF1A\u53EF\u64CD\u63A7\u7F51\u9875\u6D4F\u89C8\u5668\u7684 AI \u667A\u80FD\u4F53",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/\u817E\u8BAF\u5143\u5B9D\u8BC6\u56FE\u653E\u5927\u62DB\uFF01\u4E00\u6B21\u4F20 10 \u5F20\u56FE\uFF0C\u670B\u53CB\u5708\u6587\u6848\u3001\u7535\u5B50\u4E66\u91D1\u53E5\u5168\u641E\u5B9A\uFF01",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/\u552E\u4EF7\u8D85 7000 \u5143\uFF0CMeta \u60F3\u7528\u773C\u955C\u53D6\u4EE3 iPhone",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/\u767E\u5EA6\u98DE\u6868\u6846\u67B6 3.0 \u6B63\u5F0F\u7248\u53D1\u5E03",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/OpenAI \u4E0A\u7EBF\u201COpenAI \u5B66\u9662\u201D\uFF0C\u5DF2\u63D0\u4F9B\u6570\u5341\u5C0F\u65F6\u514D\u8D39 AI \u5B66\u4E60\u8D44\u6E90",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-04/xAI \u518D\u66F4\u65B0\uFF0C\u5404\u9879\u80FD\u529B\u5353\u8D8A"
        ]
      },
      {
        "title": "2025-03",
        "collapsable": true,
        "children": [
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u963F\u91CC\u5F00\u6E90\u5168\u65B0\u63A8\u7406\u6A21\u578B QwQ-32B\uFF0C\u4E00\u53F0 Mac \u5C31\u80FD\u5B9E\u73B0\u9876\u7EA7\u63A8\u7406\u80FD\u529B",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u5B9E\u6D4B Manus\uFF1A\u9996\u4E2A\u771F\u5E72\u6D3B AI\uFF0C\u4E2D\u56FD\u9020\uFF08\u9644 50 \u4E2A\u7528\u4F8B + \u62C6\u89E3\uFF09",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u7528\u4E8E\u4E34\u5E8A\u5DE5\u4F5C\u6D41\u7A0B\u7684\u65B0 AI \u52A9\u624B\uFF0C\u5FAE\u8F6F\u63A8\u51FA Microsoft Dragon Copilot",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/Model Context Protocol\uFF0C\u770B\u8FD9\u4E00\u7BC7\u5C31\u591F\u4E86",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u8C37\u6B4C Gemini \u65B0\u589E Canvas \u4E0E\u97F3\u9891\u6982\u89C8\u529F\u80FD\uFF0C\u63D0\u5347\u7528\u6237\u751F\u4EA7\u529B",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u9A6C\u65AF\u514B\u8FDB\u519B AI \u89C6\u9891\uFF0C\u6536\u8D2D\u89C6\u9891\u751F\u6210\u521D\u521B\u516C\u53F8\uFF0C4 \u4EBA 13 \u4E2A\u6708\u6253\u9020\u7C7B Sora \u6A21\u578B",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u767E\u5EA6\u63A8\u51FA\u4E24\u6B3E AI \u5927\u6A21\u578B",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/Claude \u73B0\u5DF2\u652F\u6301\u7F51\u7EDC\u641C\u7D22\u529F\u80FD",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/DeepSeek-V3 \u6A21\u578B\u66F4\u65B0\uFF0C\u5404\u9879\u80FD\u529B\u5168\u9762\u8FDB\u9636",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u817E\u8BAF\u6DF7\u5143 T1 \u6B63\u5F0F\u7248\u53D1\u5E03",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/Ideogram \u6B63\u5F0F\u53D1\u5E03 3.0 \u7248\u672C\u6A21\u578B\uFF1A\u771F\u5B9E\u611F\u4E0E\u521B\u610F\u8868\u73B0\u518D\u7A81\u7834",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u65B0\u63A8\u7406\u6A21\u578B\u6765\u4E86\uFF01\u963F\u91CC Qwen Chat \u5E73\u53F0\u5DF2\u4E0A\u7EBF\u201C\u6DF1\u5EA6\u601D\u8003\u201D\u529F\u80FD\uFF0C\u652F\u6301\u8054\u7F51\u641C\u7D22",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u521A\u521A\uFF0CGPT-4o \u539F\u751F\u56FE\u50CF\u751F\u6210\u4E0A\u7EBF\uFF0CP \u56FE\u3001\u751F\u56FE\u4E5F\u5C31\u4E00\u5634\u7684\u4E8B",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u8C37\u6B4C\u53D1\u5E03 Gemini 2.5 \u4EBA\u5DE5\u667A\u80FD\u6A21\u578B\uFF0C\u5B9E\u73B0\u590D\u6742\u601D\u7EF4",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u8C37\u6B4C\u7EC8\u4E8E\u767B\u9876\u4E00\u6B21\u4E86\uFF01\u6700\u5F3A\u63A8\u7406\u6A21\u578BGemini 2.5 Pro\u5B9E\u6D4B\u4F53\u9A8C\uFF0C\u771F\u7684\u6709\u70B9\u4E1C\u897F",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/DeepSeek\u56DE\u7B54\u73B0\u5728\u80FD\u4E0D\u80FD\u5165\u624B\u9EC4\u91D1 \u5C06\u7EF4\u6301\u9AD8\u4F4D\u9707\u8361",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/DeepSeek\u5B98\u65B9\u8F9F\u8C23\uFF1AR2\u53D1\u5E03\u4E3A\u5047\u6D88\u606F",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u9AD8\u6821\uFF0C\u4E3A\u4F55\u6700\u5FEB\u62E5\u62B1DeepSeek\uFF1F",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u8DE8\u5883\u7535\u5546\u8BD5\u7EC3AI\uFF0CDeepSeek\u53D6\u4EE3\u4E86ChatGPT",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-03/\u4E0D\u88C5\u4E86\uFF01OpenAI\u529B\u4FC3\u7279\u6717\u666E\u5BF9\u4E2D\u56FDAI\u4E0B\u6B7B\u624B\uFF0C\u51FA\u53F0\u201CAI\u51FA\u53E3\u7BA1\u5236\u201D"
        ]
      },
      {
        "title": "2025-02",
        "collapsable": true,
        "children": [
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/Anthropic Claude 3.7 Sonnet \u53D1\u5E03\uFF1A\u63A8\u7406\u80FD\u529B\u518D\u8FDB\u5316\uFF0C\u7F16\u7801\u6548\u7387\u7A81\u7834\u5929\u9645\uFF01",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u963F\u91CC Qwen \u9996\u4E2A\u63A8\u7406\u6A21\u578B\u4EAE\u76F8\uFF01",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u88ABDeepSeek\u523A\u6FC0\u5230\u4E86\uFF1F\u6587\u5FC3\u4E00\u8A00\u3001ChatGPT\u540C\u65F6\u5BA3\u5E03\uFF1A\u514D\u8D39\uFF01 ",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u8FD0\u8425\u5546\u5168\u9762\u63A5\u5165DeepSeek\u610F\u5473\u7740\u4EC0\u4E48\uFF1F",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u96F7\u519B\uFF1A\u94A6\u4F69DeepSeek\u53D6\u5F97\u7684\u6210\u5C31\uFF0C\u6BCF\u4E2A\u4EBA\u53EF\u80FD\u90FD\u8981\u5B66\u4E60AI\u77E5\u8BC6",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u6E05\u534E\u5927\u5B66\uFF1A\u666E\u901A\u4EBA\u5982\u4F55\u6293\u4F4FDeepSeek\u7EA2\u52292025",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u70B9\u8D5E\u6536\u85CF\uFF01DeepSeek\u5728GitHub\u661F\u6807\u91CF\u5DF2\u8D85OpenAI",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u7206\u51B7\uFF0CDeepSeek\u51FA\u5C40\uFF0C\u82F9\u679CAI\u56FD\u884C\u7248\u5C06\u4E0E\u963F\u91CC\u5408\u4F5C",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u770B\u77ED\u5267\u3001\u201C\u4EA4\u670B\u53CB\u201D\uFF0CDeepSeek\u6324\u8FDB\u4E2D\u8001\u5E74\u793E\u4EA4\u5708",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u4E89\u5148\u6050\u540E\u63A5\u5165DeepSeek\u7684\u56FD\u4EA7\u624B\u673A\uFF0C\u5B83\u4EEC\u7684\u81EA\u7814\u5927\u6A21\u578B\u600E\u4E48\u529E\uFF1F",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u4EF7\u503C\u5343\u4E07\u7F8E\u5143\u7684\u201CAI.com\u201D\uFF0C\u4F1A\u662F\u8C01\u7ED9DeepSeek\u516C\u53F8\u4F7F\u7528\u7684\u5462",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u5916\u8D44\u673A\u6784\u770BDeepSeek\uFF1A\u63D0\u632F\u4E2D\u56FD\u80A1\u5E02 \u673A\u4F1A\u85CF\u5728\u8FD9\u4E9B\u9886\u57DF",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u591A\u5BB6\u8F66\u4F01\u63A5\u5165AI\u5927\u6A21\u578BDeepSeek \u667A\u80FD\u6C7D\u8F66\u518D\u8FDB\u4E00\u6B65",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u5982\u4F55\u5229\u7528DeepSeek\u7FFB\u8EAB\uFF1F\u6293\u4F4FAI\u7EA2\u5229\uFF0C\u666E\u901A\u4EBA\u4E5F\u80FD\u9006\u88AD\u76843\u4E2A\u65B9\u5411",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u65E5\u672C\u5982\u4F55\u770B\u5F85Deepseek",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u6DF1\u5EA6\u6C42\u7D22\u6B63\u5728\u7269\u8272\u97E9\u56FD\u4EBA\u5DE5\u667A\u80FD\u4EBA\u624D\uFF0C\u5C55\u5F00\u786E\u4FDD\u4EBA\u624D\u7684\u6218\u4E89",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/DeepSeek\u7684\u201C\u670D\u52A1\u5668\u7E41\u5FD9\u201D\u8BA9\u6240\u6709\u4EBA\u6293\u72C2\uFF0C\u80CC\u540E\u7A76\u7ADF\u662F\u600E\u4E48\u56DE\u4E8B",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/DeepSeek\u88AB\u5C01\u6740\u4E86",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/DeepSeek\u9884\u6D4B\uFF1A\u672A\u676510\u5E74\uFF0C\u5C31\u4E1A\u524D\u666F\u6700\u597D\u768410\u4E2A\u4E13\u4E1A",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/DeepSeek\uFF0C\u7D27\u6025\u58F0\u660E",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/vivo\u4E0E\u8363\u8000\u76F8\u7EE7\u63A5\u5165DeepSeek\uFF1AAI\u6DF1\u5EA6\u878D\u5408\u5F15\u9886\u624B\u673A\u521B\u65B0\u6F6E\u6D41",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/vivo\u5B98\u5BA3\uFF1A\u5C06\u6DF1\u5EA6\u878D\u5408\u6EE1\u8840\u7248DeepSeek",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/\u4E0D\u5728\u573A\u7684DeepSeek\uFF0C\u662F\u5DF4\u9ECEAI\u5CF0\u4F1A\u771F\u6B63\u7684\u4E3B\u89D2",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/DeepSeek\u201C\u670B\u53CB\u5708\u201D\u4E0D\u65AD\u6269\u56F4\uFF1A10\u5BB6\u56FD\u5185\u5916\u4E91\u5382\u5546\u5BA3\u5E03\u63A5\u5165",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/DeepSeek\u201C\u91D1\u878D\u670B\u53CB\u5708\u201D \u4ECE\u201C\u4E89\u5148\u201D\u5230\u201C\u6050\u540E\u201D\uFF0C\u4ECE\u201C\u597D\u7528\u201D\u5230\u201C\u7528\u597D\u201D",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/DeepSeek\u5982\u4F55\u6405\u52A8AI\u4EA7\u4E1A\uFF1F",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/DeepSeek\u5BA3\u5E03\u6DA8\u4EF7\uFF01",
          "AI\u884C\u4E1A\u8D44\u8BAF/2025-02/DeepSeek\u5E26\u98DE\u79D1\u5927\u8BAF\u98DE\uFF1F"
        ]
      }
    ]
  },
  {
    "title": "DeepSeek\u6280\u672F\u89E3\u6790",
    "collapsable": true,
    "children": [
      {
        "title": "DeepSeek \u6A21\u578B\u8BAD\u7EC3",
        "collapsable": true,
        "children": [
          "DeepSeek\u6280\u672F\u89E3\u6790/DeepSeek \u6A21\u578B\u8BAD\u7EC3/DeepSeek-R1\u7684\u56DB\u4E2A\u8BAD\u7EC3\u9636\u6BB5",
          "DeepSeek\u6280\u672F\u89E3\u6790/DeepSeek \u6A21\u578B\u8BAD\u7EC3/DeepSeek-R1\u7684\u8BAD\u7EC3\u6D41\u7A0B\u5F3A\u5316\u5B66\u4E60\uFF08RL\uFF09\u9636\u6BB5\u91C7\u7528\u4E86GRPO\u7B97\u6CD5",
          "DeepSeek\u6280\u672F\u89E3\u6790/DeepSeek \u6A21\u578B\u8BAD\u7EC3/DeepSeek-V3 \u9AD8\u6548\u8BAD\u7EC3\u5173\u952E\u6280\u672F\u5206\u6790",
          "DeepSeek\u6280\u672F\u89E3\u6790/DeepSeek \u6A21\u578B\u8BAD\u7EC3/DeepSeek\u534E\u4E3D\u6587\u98CE\u4ECE\u4F55\u800C\u6765\uFF1F\u4E1A\u5185\u4EBA\u58EB\uFF1A\u8BAD\u7EC3\u6570\u636E\u3001\u8BAD\u7EC3\u7B56\u7565\u548C\u8FED\u4EE3\u4F18\u5316\u7F3A\u4E00\u4E0D\u53EF"
        ]
      },
      {
        "title": "DeepSeek \u6280\u672F\u5206\u6790",
        "collapsable": true,
        "children": [
          "DeepSeek\u6280\u672F\u89E3\u6790/DeepSeek \u6280\u672F\u5206\u6790/DeepSeek\u6700\u5F3A\u4E13\u4E1A\u62C6\u89E3\uFF1A\u6E05\u4EA4\u590D\u6559\u6388\u8D85\u786C\u6838\u89E3\u8BFB",
          "DeepSeek\u6280\u672F\u89E3\u6790/DeepSeek \u6280\u672F\u5206\u6790/DeepSeek\u7684\u4F18\u52BF\u4E0E\u4E0D\u8DB3",
          "DeepSeek\u6280\u672F\u89E3\u6790/DeepSeek \u6280\u672F\u5206\u6790/\u4E00\u6587\u8BE6\u89E3 DeepSeek \u6280\u672F\u67B6\u6784",
          "DeepSeek\u6280\u672F\u89E3\u6790/DeepSeek \u6280\u672F\u5206\u6790/DeepSeek vs. ChatGPT\uFF1A\u8C01\u624D\u662F\u771F\u6B63\u7684\u738B\u8005\uFF1F",
          "DeepSeek\u6280\u672F\u89E3\u6790/DeepSeek \u6280\u672F\u5206\u6790/DeepSeek \u7206\u706B\u903B\u8F91\u3001\u884C\u4E1A\u5F71\u54CD\u53CA\u5BF9\u672A\u6765AI\u53D1\u5C55\u7684\u542F\u793A",
          "DeepSeek\u6280\u672F\u89E3\u6790/DeepSeek \u6280\u672F\u5206\u6790/DeepSeek-R1 \u6280\u672F\u5168\u666F\u89E3\u6790\uFF1A\u4ECE\u539F\u7406\u5230\u5B9E\u8DF5\u7684\u201C\u70BC\u91D1\u672F\u914D\u65B9\u201D",
          "DeepSeek\u6280\u672F\u89E3\u6790/DeepSeek \u6280\u672F\u5206\u6790/DeepSeek\u6280\u672F\u89E3\u8BFB\uFF1A\u4ECEV3\u5230R1\u7684MoE\u67B6\u6784\u521B\u65B0"
        ]
      }
    ]
  },
  {
    "title": "AI\u5E94\u7528\u573A\u666F",
    "collapsable": true,
    "children": [
      {
        "title": "DeepSeek + \u7406\u8D22",
        "collapsable": true,
        "children": [
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u7406\u8D22/DeepSeek\u544A\u8BC9\u6211\uFF1A30\u5C81\u523040\u5C81\uFF0C\u4E00\u822C\u4F1A\u62E5\u6709\u8FD9\u4E48\u591A\u7684\u5B58\u6B3E",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u7406\u8D22/\u7528DeepSeek\u641E\u94B1\uFF0C\u65E5\u8D5A\u767E\u4E07",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u7406\u8D22/\u666E\u901A\u4EBA\u5982\u4F55\u901A\u8FC7\u7092\u80A1\u4E70\u57FA\u91D1\u8D5A\u5230100\u4E07\uFF1F",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u7406\u8D22/\u7528Deepseek\u56DE\u7B54\uFF1A\u5982\u679C\u6709100\u4E07\u95F2\u94B1\uFF0C\u51E0\u5E74\u5185\u4E0D\u7528\uFF0C\u8BE5\u600E\u4E48\u7406\u8D22\uFF1F"
        ]
      },
      {
        "title": "DeepSeek + \u7F16\u7A0B\u5F00\u53D1",
        "collapsable": true,
        "children": [
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u7F16\u7A0B\u5F00\u53D1/3 \u5C0F\u65F6\u505A\u6E38\u620F\uFF0C10 \u5929\u72C2\u8D5A 28 \u4E07\uFF01\u7A0B\u5E8F\u5458\u7528 AI \u8EBA\u8D5A\uFF1F",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u7F16\u7A0B\u5F00\u53D1/\u{1F497}\u7528 DeepSeek \u7ED9\u5BF9\u8C61\u505A\u4E2A\u7F51\u7AD9\uFF0C\u5979\u4E00\u5B9A\u611F\u52A8\u574F\u4E86",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u7F16\u7A0B\u5F00\u53D1/DeepSeek\u88C5\u8FDBVSCode\uFF0C\u7F16\u7A0B\u975E\u5E38\u4E1D\u6ED1\uFF01",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u7F16\u7A0B\u5F00\u53D1/\u6559\u4F60\u7528DeepSeek+Clien\uFF0C\u4ECE0\u52301\u5F00\u53D1\u4E00\u4E2AAPP",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u7F16\u7A0B\u5F00\u53D1/DeepSeek\u63A5\u5165Python\uFF0C\u4E00\u822C\u7535\u8111\u4E5F\u80FD\u98DE\u901F\u8DD1\uFF0C\u786E\u5B9E\u53EF\u4EE5\u5C01\u795E\u4E86\uFF01"
        ]
      },
      {
        "title": "DeepSeek + \u521B\u610F\u8BBE\u8BA1",
        "collapsable": true,
        "children": [
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u521B\u610F\u8BBE\u8BA1/\u548C Deepseek \u8054\u624B\uFF0C\u505A\u4E2A\u54EA\u5412\u7684\u4E7E\u5764\u5708\u89C6\u9891",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u521B\u610F\u8BBE\u8BA1/5 \u4E2A\u4E0D\u5F97\u4E0D\u6536\u85CF\u7684 Deepseek \u738B\u70B8\u7EC4\u5408\uFF01",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u521B\u610F\u8BBE\u8BA1/DeepSeek\u4E00\u53E5\u8BDD\u641E\u5B9A\u4FEE\u56FE\u96BE\u9898",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u521B\u610F\u8BBE\u8BA1/deepseek+\u6570\u5B57\u4EBA\u738B\u70B8\u7EC4\u5408\u4F7F\u7528\u65B9\u6CD5",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u521B\u610F\u8BBE\u8BA1/\u7528 deepseek \u505A AI \u89C6\u9891\uFF0C\u7EDD\u4E86\uFF0C\u548C\u6284\u4F5C\u4E1A\u4E00\u6837\u7B80\u5355\uFF01",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u521B\u610F\u8BBE\u8BA1/\u7EDD\u7EDD\u5B50\uFF01\u7528deepseek\u505AAI\u89C6\u9891\uFF0C\u6DA8\u7C8910W+\uFF08\u9644\u4FDD\u59C6\u7EA7\u6559\u7A0B\uFF09",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u521B\u610F\u8BBE\u8BA1/\u8FD9\u6015\u662F\u5168\u7F51\u6700\u5F3A\u7684 DeepSeek \u56FE\u7247\u6559\u7A0B\u5427\uFF0C\u8D76\u7D27\u6536\u85CF\u4E86\uFF01"
        ]
      },
      {
        "title": "DeepSeek + \u529E\u516C\u6548\u7387",
        "collapsable": true,
        "children": [
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u529E\u516C\u6548\u7387/\u5982\u4F55\u7528DeepSeek\u66F4\u9AD8\u6548\u5730\u5DE5\u4F5C\uFF1A10\u4E2A\u5B9E\u7528\u6280\u5DE7",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u529E\u516C\u6548\u7387/\u624B\u628A\u624B\u6559\u4F60\u5728word\u4E2D\u63A5\u5165deepseek\uFF0C\u79D2\u751F\u6587\u6863\u6750\u6599",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u529E\u516C\u6548\u7387/\u6CD5\u5F8B\u4EBA\u4FDD\u59C6\u7EA7deepseek\u4F7F\u7528\u6307\u5357\uFF08\u9644\u6307\u4EE4\u7248\uFF09",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u529E\u516C\u6548\u7387/DeepSeek R1 + \u4E2A\u4EBA\u77E5\u8BC6\u5E93\uFF0C\u76F4\u63A5\u8D77\u98DE\uFF01",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u529E\u516C\u6548\u7387/DeepSeek\u5D4C\u5165\u5230Excel\uFF0C\u63D0\u534710\u500D\u5DE5\u4F5C\u6548\u7387\uFF0C\u592A\u725B\u4E86\uFF01",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u529E\u516C\u6548\u7387/DeepSeek\u914D\u5408KIMI\uFF0C\u81EA\u52A8\u751F\u6210PPT\uFF0C\u611F\u89C9\u81EA\u5DF1\u8981\u5931\u4E1A\u4E86\uFF01",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u529E\u516C\u6548\u7387/WPS\u91CC\u88C5\u4E0Adeepseek\uFF0C\u7B80\u76F4\u5C31\u662F\u529E\u516C\u795E\u5668",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u529E\u516C\u6548\u7387/\u5229\u7528deepseek\u5EFA\u7ACB\u4E13\u5C5E\u9500\u552E\u77E5\u8BC6\u5E93",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u529E\u516C\u6548\u7387/\u6559\u5E08\u5FC5\u5907DeepSeek\u4F7F\u7528\u6307\u5357\u6765\u4E86\uFF015\u5927\u6559\u5B66\u5E94\u7528\u573A\u666F+\u5B9E\u64CD\u6848\u4F8B+\u9690\u85CF\u7528\u6CD5"
        ]
      },
      {
        "title": "DeepSeek + \u5185\u5BB9\u521B\u4F5C",
        "collapsable": true,
        "children": [
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u5185\u5BB9\u521B\u4F5C/3\u79D2\u8BA9DeepSeek\u5199\u51FA\u7206\u6B3E\u5C0F\u7EA2\u4E66",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u5185\u5BB9\u521B\u4F5C/\u4EBA\u6709\u591A\u5927\u80C6\uFF0C\u5730\u6709\u591A\u5927\u4EA7\uFF1A\u5982\u4F55\u7528DeepSeek\u5199\u957F\u7BC7\u5C0F\u8BF4",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u5185\u5BB9\u521B\u4F5C/\u5982\u4F55\u5229\u7528DeepSeek\u8FDB\u884C\u9AD8\u6548\u5185\u5BB9\u521B\u4F5C",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u5185\u5BB9\u521B\u4F5C/\u7528DeepSeek\u505A\u5C0F\u7EA2\u4E66\u771F\u7684\u592A\u725B\u4E86\uFF01\u8F7B\u8F7B\u677E\u677E\u6253\u9020\u7206\u6B3E\u7B14\u8BB0",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u5185\u5BB9\u521B\u4F5C/\u7528DeepSeek\u5199\u6587\u7AE0\uFF1F\u8FD94\u4E2A\u9A9A\u64CD\u4F5C\u8BA9\u4F60\u8EBA\u5E73\u4E5F\u80FD\u51FA\u7206\u6B3E\uFF01\uFF08\u542B\u63D0\u793A\u8BCD\uFF09",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u5185\u5BB9\u521B\u4F5C/DeepSeek\u4F7F\u7528\u6307\u5357\uFF1A\u63D0\u5347\u516C\u6587\u3001\u65B0\u95FB\u4E0E\u5E7F\u544A\u6587\u6848\u5199\u4F5C\u6548\u7387\u7684\u4E09\u5927\u6280\u5DE7 ",
          "AI\u5E94\u7528\u573A\u666F/DeepSeek + \u5185\u5BB9\u521B\u4F5C/AI\u5199\u5C0F\u8BF4\u600E\u4E48\u5199\uFF1Fdeepseek\u5E2E\u4F60\u5199\u5C0F\u8BF4\u6559\u7A0B"
        ]
      }
    ]
  },
  {
    "title": "DeepSeek\u4F7F\u7528\u6307\u5357",
    "collapsable": true,
    "children": [
      "DeepSeek\u4F7F\u7528\u6307\u5357/\u{1F525}DeepSeek \u5C0F\u767D\u5FEB\u901F\u4E0A\u624B\u6307\u5357",
      "DeepSeek\u4F7F\u7528\u6307\u5357/\u51E0\u4E2A\u6280\u5DE7\uFF0C\u6559\u4F60\u53BB\u9664\u6587\u7AE0\u7684 AI \u5473\uFF01",
      "DeepSeek\u4F7F\u7528\u6307\u5357/DeepSeek \u53D1\u5E03\u65B0\u6A21\u578B V3-0324\uFF0C\u9644\u4F7F\u7528\u6559\u7A0B",
      "DeepSeek\u4F7F\u7528\u6307\u5357/\u6700\u65B0\u6E05\u534E\u5927\u5B66DeepSeek\u4F7F\u7528\u624B\u518C\u7B2C1-5\u7248\uFF0C\u5B98\u65B9\u5B8C\u6574\u7248PDF\u514D\u8D39\u4E0B\u8F7D",
      "DeepSeek\u4F7F\u7528\u6307\u5357/2\u5206\u949F\u5B66\u4F1A DeepSeek API\uFF0C\u7ADF\u7136\u6BD4\u5B98\u65B9\u66F4\u597D\u7528\uFF01",
      "DeepSeek\u4F7F\u7528\u6307\u5357/\u5B8C\u6574\u653B\u7565\uFF1A\u5982\u4F55\u7528\u597DDeepSeek\uFF0C\u4E00\u6587\u6C47\u603B\uFF01",
      "DeepSeek\u4F7F\u7528\u6307\u5357/\u3010\u6C47\u603B\u3011\u6EE1\u8840\u7248 DeepSeek \u7B2C\u4E09\u65B9\u4F7F\u7528\u6E20\u9053",
      "DeepSeek\u4F7F\u7528\u6307\u5357/DeepSeek \u672C\u5730\u90E8\u7F72\u6559\u7A0B",
      "DeepSeek\u4F7F\u7528\u6307\u5357/\u5982\u4F55\u5728iPhone\u4E0A\u7528\u8BED\u97F3\u8C03\u7528Deepseek",
      "DeepSeek\u4F7F\u7528\u6307\u5357/\u666E\u901A\u4EBA\u80FD\u7528DeepSeek\u505A\u4EC0\u4E48\uFF1F20\u4E2A\u5B9E\u7528\u5EFA\u8BAE",
      {
        "title": "DeepSeek \u63D0\u95EE\u6280\u5DE7",
        "collapsable": true,
        "children": [
          "DeepSeek\u4F7F\u7528\u6307\u5357/DeepSeek \u63D0\u95EE\u6280\u5DE7/50\u4E2A\u5E38\u7528\u7684DeepSeek\u6A21\u4EFF\u98CE\u683C\u63D0\u793A\u8BCD\uFF0C\u53BBAI\u5473\u7684\u5927\u6740\u5668",
          "DeepSeek\u4F7F\u7528\u6307\u5357/DeepSeek \u63D0\u95EE\u6280\u5DE7/\u6211\u53D1\u73B0\u4E86 DeepSeek \u53BB AI \u5473\u7684\u6377\u5F84\uFF0C\u592A\u9999\u4E86",
          "DeepSeek\u4F7F\u7528\u6307\u5357/DeepSeek \u63D0\u95EE\u6280\u5DE7/DeepSeek \u63D0\u793A\u8BCD\u57FA\u672C\u6CD5\u5219",
          "DeepSeek\u4F7F\u7528\u6307\u5357/DeepSeek \u63D0\u95EE\u6280\u5DE7/DeepSeek\u4E0D\u597D\u7528\uFF1F\u90A3\u662F\u4F60\u8FD8\u4E0D\u77E5\u9053\u8FD9\u4E9B\u6307\u4EE4\uFF01",
          "DeepSeek\u4F7F\u7528\u6307\u5357/DeepSeek \u63D0\u95EE\u6280\u5DE7/\u5410\u8840\u6574\u7406\uFF01DeepSeek\u795E\u7EA7\u6307\u4EE4\uFF0C\u597D\u7528\u5230\u7206\uFF01",
          "DeepSeek\u4F7F\u7528\u6307\u5357/DeepSeek \u63D0\u95EE\u6280\u5DE7/\u666E\u901A\u4EBA\u4E5F\u80FD\u8F7B\u677E\u638C\u63E1\u7684 20 \u4E2A DeepSeek \u9AD8\u9891\u63D0\u793A\u8BCD\uFF082025\u7248\uFF09"
        ]
      }
    ]
  }
];

// .vuepress/sidebar.ts
var sidebar_default = {
  "/AI/": ai_default,
  "/AI\u9879\u76EE\u6559\u7A0B/": ai_default,
  "/": "auto"
};

// .vuepress/config.ts
var author = "\u7A0B\u5E8F\u5458\u9C7C\u76AE";
var domain = "https://ai.codefather.cn";
var tags = [
  "ai",
  "deepseek",
  "AI \u8D44\u8BAF",
  "\u4EBA\u5DE5\u667A\u80FD",
  "AI \u884C\u4E1A\u8D8B\u52BF",
  "AI \u6280\u672F",
  "AI \u65B0\u95FB",
  "AI \u52A8\u6001",
  "AI \u5E02\u573A\u5206\u6790",
  "AI \u6A21\u578B",
  "AI \u72EC\u5BB6\u5206\u6790",
  "AI \u6DF1\u5EA6\u89E3\u8BFB"
];
var config_default = defineConfig({
  title: "\u9C7C\u76AE AI \u77E5\u8BC6\u5E93",
  description: "\u9C7C\u76AE AI \u77E5\u8BC6\u5E93 - \u514D\u8D39 DeepSeek \u6559\u7A0B\uFF5C\u5DE5\u5177\u7AD9\uFF5C\u8D44\u6E90\u5E93\uFF0C\u662F\u4E00\u7AD9\u5F0F\u5F00\u6E90\u514D\u8D39\u7684\u4EBA\u5DE5\u667A\u80FD\u77E5\u8BC6\u5206\u4EAB\u5E73\u53F0\uFF0C\u6C47\u96C6 Deepseek\u3001GPT \u7B49\u70ED\u95E8 AI \u5DE5\u5177\u4ECB\u7ECD\u3001\u4F7F\u7528\u6307\u5357\u3001\u6280\u5DE7\u5206\u4EAB\u3001\u5E94\u7528\u573A\u666F\u3001AI \u53D8\u73B0\u3001\u884C\u4E1A\u8D44\u8BAF\u3001\u6559\u7A0B\u8D44\u6E90\u6C47\u603B\uFF0C\u63D0\u4F9B\u7CFB\u7EDF\u5316\u7684 AI \u6559\u7A0B\u3001\u7CBE\u9009 AI \u8D44\u6E90\uFF0C\u52A9\u4F60\u5FEB\u901F\u638C\u63E1 AI \u6280\u672F\uFF0C\u6210\u4E3A AI \u4E13\u5BB6\uFF01",
  head: [
    ["link", { rel: "icon", href: "/favicon.ico" }],
    [
      "meta",
      {
        name: "keywords",
        content: "ai, deepseek, AI \u8D44\u8BAF\uFF0C\u4EBA\u5DE5\u667A\u80FD\uFF0CAI \u884C\u4E1A\u8D8B\u52BF\uFF0CAI \u6280\u672F\uFF0CAI \u65B0\u95FB\uFF0CAI \u52A8\u6001\uFF0CAI \u5E02\u573A\u5206\u6790\uFF0CAI \u6A21\u578B\uFF0CAI \u72EC\u5BB6\u5206\u6790\uFF0CAI \u6DF1\u5EA6\u89E3\u8BFB"
      }
    ],
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
      `
    ]
  ],
  permalink: "/:slug",
  extraWatchFiles: [".vuepress/*.ts", ".vuepress/sidebars/*.ts"],
  markdown: {
    lineNumbers: true,
    extractHeaders: ["h2", "h3", "h4", "h5", "h6"]
  },
  plugins: [
    ["@vuepress/back-to-top"],
    [
      "@vuepress/google-analytics",
      {
        ga: "GTM-WVS9HM6W"
      }
    ],
    ["@vuepress/medium-zoom"],
    [
      "seo",
      {
        siteTitle: (_, $site) => $site.title + " - \u514D\u8D39 DeepSeek \u6559\u7A0B\uFF5C\u5DE5\u5177\u7AD9\uFF5C\u8D44\u6E90\u5E93",
        title: ($page) => $page.title + " - \u514D\u8D39 DeepSeek \u6559\u7A0B\uFF5C\u5DE5\u5177\u7AD9\uFF5C\u8D44\u6E90\u5E93",
        description: ($page) => $page.frontmatter.description || $page.description,
        author: (_, $site) => $site.themeConfig.author || author,
        tags: ($page) => $page.frontmatter.tags || tags,
        type: ($page) => "article",
        url: (_, $site, path) => ($site.themeConfig.domain || domain || "") + path,
        image: ($page, $site) => $page.frontmatter.image && ($site.themeConfig.domain && !$page.frontmatter.image.startsWith("http") || "") + $page.frontmatter.image,
        publishedAt: ($page) => $page.frontmatter.date && new Date($page.frontmatter.date),
        modifiedAt: ($page) => $page.lastUpdated && new Date($page.lastUpdated)
      }
    ],
    [
      "sitemap",
      {
        hostname: domain
      }
    ],
    ["vuepress-plugin-baidu-autopush"],
    ["vuepress-plugin-tags"],
    [
      "vuepress-plugin-code-copy",
      {
        successText: "\u4EE3\u7801\u5DF2\u590D\u5236"
      }
    ],
    [
      "feed",
      {
        canonical_base: domain,
        count: 1e4,
        posts_directories: []
      }
    ],
    ["img-lazy"]
  ],
  themeConfig: {
    logo: "/logo.png",
    nav: navbar_default,
    sidebar: sidebar_default,
    lastUpdated: "\u6700\u8FD1\u66F4\u65B0",
    repo: "liyupi/ai-guide",
    docsBranch: "master",
    editLinks: true,
    editLinkText: "\u5B8C\u5584\u9875\u9762",
    footer: footer_default,
    extraSideBar: extraSideBar_default
  }
});
export {
  config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsiLnZ1ZXByZXNzL2NvbmZpZy50cyIsICIudnVlcHJlc3MvZXh0cmFTaWRlQmFyLnRzIiwgIi52dWVwcmVzcy9mb290ZXIudHMiLCAiLnZ1ZXByZXNzL25hdmJhci50cyIsICIudnVlcHJlc3Mvc2lkZWJhcnMvYWkudHMiLCAiLnZ1ZXByZXNzL3NpZGViYXIudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImltcG9ydCB7IGRlZmluZUNvbmZpZyB9IGZyb20gXCJ2dWVwcmVzcy9jb25maWdcIjtcbmltcG9ydCBleHRyYVNpZGVCYXIgZnJvbSBcIi4vZXh0cmFTaWRlQmFyXCI7XG5pbXBvcnQgZm9vdGVyIGZyb20gXCIuL2Zvb3RlclwiO1xuaW1wb3J0IG5hdmJhciBmcm9tIFwiLi9uYXZiYXJcIjtcbmltcG9ydCBzaWRlYmFyIGZyb20gXCIuL3NpZGViYXJcIjtcblxuY29uc3QgYXV0aG9yID0gXCJcdTdBMEJcdTVFOEZcdTU0NThcdTlDN0NcdTc2QUVcIjtcbmNvbnN0IGRvbWFpbiA9IFwiaHR0cHM6Ly9haS5jb2RlZmF0aGVyLmNuXCI7XG5jb25zdCB0YWdzID0gW1xuICBcImFpXCIsXG4gIFwiZGVlcHNlZWtcIixcbiAgXCJBSSBcdThENDRcdThCQUZcIixcbiAgXCJcdTRFQkFcdTVERTVcdTY2N0FcdTgwRkRcIixcbiAgXCJBSSBcdTg4NENcdTRFMUFcdThEOEJcdTUyQkZcIixcbiAgXCJBSSBcdTYyODBcdTY3MkZcIixcbiAgXCJBSSBcdTY1QjBcdTk1RkJcIixcbiAgXCJBSSBcdTUyQThcdTYwMDFcIixcbiAgXCJBSSBcdTVFMDJcdTU3M0FcdTUyMDZcdTY3OTBcIixcbiAgXCJBSSBcdTZBMjFcdTU3OEJcIixcbiAgXCJBSSBcdTcyRUNcdTVCQjZcdTUyMDZcdTY3OTBcIixcbiAgXCJBSSBcdTZERjFcdTVFQTZcdTg5RTNcdThCRkJcIixcbl07XG5cbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZyh7XG4gIHRpdGxlOiBcIlx1OUM3Q1x1NzZBRSBBSSBcdTc3RTVcdThCQzZcdTVFOTNcIixcbiAgZGVzY3JpcHRpb246XG4gICAgXCJcdTlDN0NcdTc2QUUgQUkgXHU3N0U1XHU4QkM2XHU1RTkzIC0gXHU1MTREXHU4RDM5IERlZXBTZWVrIFx1NjU1OVx1N0EwQlx1RkY1Q1x1NURFNVx1NTE3N1x1N0FEOVx1RkY1Q1x1OEQ0NFx1NkU5MFx1NUU5M1x1RkYwQ1x1NjYyRlx1NEUwMFx1N0FEOVx1NUYwRlx1NUYwMFx1NkU5MFx1NTE0RFx1OEQzOVx1NzY4NFx1NEVCQVx1NURFNVx1NjY3QVx1ODBGRFx1NzdFNVx1OEJDNlx1NTIwNlx1NEVBQlx1NUU3M1x1NTNGMFx1RkYwQ1x1NkM0N1x1OTZDNiBEZWVwc2Vla1x1MzAwMUdQVCBcdTdCNDlcdTcwRURcdTk1RTggQUkgXHU1REU1XHU1MTc3XHU0RUNCXHU3RUNEXHUzMDAxXHU0RjdGXHU3NTI4XHU2MzA3XHU1MzU3XHUzMDAxXHU2MjgwXHU1REU3XHU1MjA2XHU0RUFCXHUzMDAxXHU1RTk0XHU3NTI4XHU1NzNBXHU2NjZGXHUzMDAxQUkgXHU1M0Q4XHU3M0IwXHUzMDAxXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGXHUzMDAxXHU2NTU5XHU3QTBCXHU4RDQ0XHU2RTkwXHU2QzQ3XHU2MDNCXHVGRjBDXHU2M0QwXHU0RjlCXHU3Q0ZCXHU3RURGXHU1MzE2XHU3Njg0IEFJIFx1NjU1OVx1N0EwQlx1MzAwMVx1N0NCRVx1OTAwOSBBSSBcdThENDRcdTZFOTBcdUZGMENcdTUyQTlcdTRGNjBcdTVGRUJcdTkwMUZcdTYzOENcdTYzRTEgQUkgXHU2MjgwXHU2NzJGXHVGRjBDXHU2MjEwXHU0RTNBIEFJIFx1NEUxM1x1NUJCNlx1RkYwMVwiLFxuICBoZWFkOiBbXG4gICAgLy8gXHU3QUQ5XHU3MEI5XHU1NkZFXHU2ODA3XG4gICAgW1wibGlua1wiLCB7IHJlbDogXCJpY29uXCIsIGhyZWY6IFwiL2Zhdmljb24uaWNvXCIgfV0sXG4gICAgLy8gU0VPXG4gICAgW1xuICAgICAgXCJtZXRhXCIsXG4gICAgICB7XG4gICAgICAgIG5hbWU6IFwia2V5d29yZHNcIixcbiAgICAgICAgY29udGVudDpcbiAgICAgICAgICBcImFpLCBkZWVwc2VlaywgQUkgXHU4RDQ0XHU4QkFGXHVGRjBDXHU0RUJBXHU1REU1XHU2NjdBXHU4MEZEXHVGRjBDQUkgXHU4ODRDXHU0RTFBXHU4RDhCXHU1MkJGXHVGRjBDQUkgXHU2MjgwXHU2NzJGXHVGRjBDQUkgXHU2NUIwXHU5NUZCXHVGRjBDQUkgXHU1MkE4XHU2MDAxXHVGRjBDQUkgXHU1RTAyXHU1NzNBXHU1MjA2XHU2NzkwXHVGRjBDQUkgXHU2QTIxXHU1NzhCXHVGRjBDQUkgXHU3MkVDXHU1QkI2XHU1MjA2XHU2NzkwXHVGRjBDQUkgXHU2REYxXHU1RUE2XHU4OUUzXHU4QkZCXCIsXG4gICAgICB9LFxuICAgIF0sXG4gICAgLy8gXHU3NjdFXHU1RUE2XHU3RURGXHU4QkExXG4gICAgW1xuICAgICAgXCJzY3JpcHRcIixcbiAgICAgIHt9LFxuICAgICAgYFxuICAgICAgICB2YXIgX2htdCA9IF9obXQgfHwgW107XG4gICAgICAgIChmdW5jdGlvbigpIHtcbiAgICAgICAgICB2YXIgaG0gPSBkb2N1bWVudC5jcmVhdGVFbGVtZW50KFwic2NyaXB0XCIpO1xuICAgICAgICAgIGhtLnNyYyA9IFwiaHR0cHM6Ly9obS5iYWlkdS5jb20vaG0uanM/Njk5OGQ2Mzg1NjJiY2VlZjMwYmUyOTc3NjdlOTFkNjRcIjtcbiAgICAgICAgICB2YXIgcyA9IGRvY3VtZW50LmdldEVsZW1lbnRzQnlUYWdOYW1lKFwic2NyaXB0XCIpWzBdOyBcbiAgICAgICAgICBzLnBhcmVudE5vZGUuaW5zZXJ0QmVmb3JlKGhtLCBzKTtcbiAgICAgICAgfSkoKTtcbiAgICAgIGAsXG4gICAgXSxcbiAgXSxcbiAgcGVybWFsaW5rOiBcIi86c2x1Z1wiLFxuXG4gIC8vIFx1NzZEMVx1NTQyQ1x1NjU4N1x1NEVGNlx1NTNEOFx1NTMxNlx1RkYwQ1x1NzBFRFx1NjZGNFx1NjVCMFxuICBleHRyYVdhdGNoRmlsZXM6IFtcIi52dWVwcmVzcy8qLnRzXCIsIFwiLnZ1ZXByZXNzL3NpZGViYXJzLyoudHNcIl0sXG4gIG1hcmtkb3duOiB7XG4gICAgLy8gXHU1RjAwXHU1NDJGXHU0RUUzXHU3ODAxXHU1NzU3XHU3Njg0XHU4ODRDXHU1M0Y3XG4gICAgbGluZU51bWJlcnM6IHRydWUsXG4gICAgLy8gXHU2NTJGXHU2MzAxIDQgXHU3RUE3XHU0RUU1XHU0RTBBXHU3Njg0XHU2ODA3XHU5ODk4XHU2RTMyXHU2N0QzXG4gICAgZXh0cmFjdEhlYWRlcnM6IFtcImgyXCIsIFwiaDNcIiwgXCJoNFwiLCBcImg1XCIsIFwiaDZcIl0sXG4gIH0sXG4gIC8vIEB0cy1pZ25vcmVcbiAgcGx1Z2luczogW1xuICAgIFtcIkB2dWVwcmVzcy9iYWNrLXRvLXRvcFwiXSxcbiAgICAvLyBHb29nbGUgXHU1MjA2XHU2NzkwXG4gICAgW1xuICAgICAgXCJAdnVlcHJlc3MvZ29vZ2xlLWFuYWx5dGljc1wiLFxuICAgICAge1xuICAgICAgICBnYTogXCJHVE0tV1ZTOUhNNldcIiwgLy8gXHU4ODY1XHU1MTQ1XHU4MUVBXHU1REYxXHU3Njg0XHU4QzM3XHU2QjRDXHU1MjA2XHU2NzkwIElEXHVGRjBDXHU2QkQ0XHU1OTgyIFVBLTAwMDAwMDAwLTBcbiAgICAgIH0sXG4gICAgXSxcbiAgICBbXCJAdnVlcHJlc3MvbWVkaXVtLXpvb21cIl0sXG4gICAgLy8gaHR0cHM6Ly9naXRodWIuY29tL2xvcmlzbGVpdmEvdnVlcHJlc3MtcGx1Z2luLXNlb1xuICAgIFtcbiAgICAgIFwic2VvXCIsXG4gICAgICB7XG4gICAgICAgIHNpdGVUaXRsZTogKF8sICRzaXRlKSA9PiAkc2l0ZS50aXRsZSArIFwiIC0gXHU1MTREXHU4RDM5IERlZXBTZWVrIFx1NjU1OVx1N0EwQlx1RkY1Q1x1NURFNVx1NTE3N1x1N0FEOVx1RkY1Q1x1OEQ0NFx1NkU5MFx1NUU5M1wiLFxuICAgICAgICB0aXRsZTogKCRwYWdlKSA9PiAkcGFnZS50aXRsZSArIFwiIC0gXHU1MTREXHU4RDM5IERlZXBTZWVrIFx1NjU1OVx1N0EwQlx1RkY1Q1x1NURFNVx1NTE3N1x1N0FEOVx1RkY1Q1x1OEQ0NFx1NkU5MFx1NUU5M1wiLFxuICAgICAgICBkZXNjcmlwdGlvbjogKCRwYWdlKSA9PiAkcGFnZS5mcm9udG1hdHRlci5kZXNjcmlwdGlvbiB8fCAkcGFnZS5kZXNjcmlwdGlvbixcbiAgICAgICAgYXV0aG9yOiAoXywgJHNpdGUpID0+ICRzaXRlLnRoZW1lQ29uZmlnLmF1dGhvciB8fCBhdXRob3IsXG4gICAgICAgIHRhZ3M6ICgkcGFnZSkgPT4gJHBhZ2UuZnJvbnRtYXR0ZXIudGFncyB8fCB0YWdzLFxuICAgICAgICB0eXBlOiAoJHBhZ2UpID0+IFwiYXJ0aWNsZVwiLFxuICAgICAgICB1cmw6IChfLCAkc2l0ZSwgcGF0aCkgPT4gKCRzaXRlLnRoZW1lQ29uZmlnLmRvbWFpbiB8fCBkb21haW4gfHwgXCJcIikgKyBwYXRoLFxuICAgICAgICBpbWFnZTogKCRwYWdlLCAkc2l0ZSkgPT5cbiAgICAgICAgICAkcGFnZS5mcm9udG1hdHRlci5pbWFnZSAmJlxuICAgICAgICAgICgoJHNpdGUudGhlbWVDb25maWcuZG9tYWluICYmICEkcGFnZS5mcm9udG1hdHRlci5pbWFnZS5zdGFydHNXaXRoKFwiaHR0cFwiKSkgfHwgXCJcIikgKyAkcGFnZS5mcm9udG1hdHRlci5pbWFnZSxcbiAgICAgICAgcHVibGlzaGVkQXQ6ICgkcGFnZSkgPT4gJHBhZ2UuZnJvbnRtYXR0ZXIuZGF0ZSAmJiBuZXcgRGF0ZSgkcGFnZS5mcm9udG1hdHRlci5kYXRlKSxcbiAgICAgICAgbW9kaWZpZWRBdDogKCRwYWdlKSA9PiAkcGFnZS5sYXN0VXBkYXRlZCAmJiBuZXcgRGF0ZSgkcGFnZS5sYXN0VXBkYXRlZCksXG4gICAgICB9LFxuICAgIF0sXG4gICAgLy8gaHR0cHM6Ly9naXRodWIuY29tL2Vrb2VyeWFudG8vdnVlcHJlc3MtcGx1Z2luLXNpdGVtYXBcbiAgICBbXG4gICAgICBcInNpdGVtYXBcIixcbiAgICAgIHtcbiAgICAgICAgaG9zdG5hbWU6IGRvbWFpbixcbiAgICAgIH0sXG4gICAgXSxcbiAgICAvLyBodHRwczovL2dpdGh1Yi5jb20vSU9yaWVucy92dWVwcmVzcy1wbHVnaW4tYmFpZHUtYXV0b3B1c2hcbiAgICBbXCJ2dWVwcmVzcy1wbHVnaW4tYmFpZHUtYXV0b3B1c2hcIl0sXG4gICAgLy8gaHR0cHM6Ly9naXRodWIuY29tL3pxOTkyOTkvdnVlcHJlc3MtcGx1Z2luL3RyZWUvbWFzdGVyL3Z1ZXByZXNzLXBsdWdpbi10YWdzXG4gICAgW1widnVlcHJlc3MtcGx1Z2luLXRhZ3NcIl0sXG4gICAgLy8gaHR0cHM6Ly9naXRodWIuY29tL3puaWNob2xhc2Jyb3duL3Z1ZXByZXNzLXBsdWdpbi1jb2RlLWNvcHlcbiAgICBbXG4gICAgICBcInZ1ZXByZXNzLXBsdWdpbi1jb2RlLWNvcHlcIixcbiAgICAgIHtcbiAgICAgICAgc3VjY2Vzc1RleHQ6IFwiXHU0RUUzXHU3ODAxXHU1REYyXHU1OTBEXHU1MjM2XCIsXG4gICAgICB9LFxuICAgIF0sXG4gICAgLy8gaHR0cHM6Ly9naXRodWIuY29tL3dlYm1hc3RlcmlzaC92dWVwcmVzcy1wbHVnaW4tZmVlZFxuICAgIFtcbiAgICAgIFwiZmVlZFwiLFxuICAgICAge1xuICAgICAgICBjYW5vbmljYWxfYmFzZTogZG9tYWluLFxuICAgICAgICBjb3VudDogMTAwMDAsXG4gICAgICAgIC8vIFx1OTcwMFx1ODk4MVx1ODFFQVx1NTJBOFx1NjNBOFx1OTAwMVx1NzY4NFx1NjU4N1x1Njg2M1x1NzZFRVx1NUY1NVxuICAgICAgICBwb3N0c19kaXJlY3RvcmllczogW10sXG4gICAgICB9LFxuICAgIF0sXG4gICAgLy8gaHR0cHM6Ly9naXRodWIuY29tL3RvbGtpbmcvdnVlcHJlc3MtcGx1Z2luLWltZy1sYXp5XG4gICAgW1wiaW1nLWxhenlcIl0sXG4gIF0sXG4gIC8vIFx1NEUzQlx1OTg5OFx1OTE0RFx1N0Y2RVxuICB0aGVtZUNvbmZpZzoge1xuICAgIGxvZ286IFwiL2xvZ28ucG5nXCIsXG4gICAgbmF2OiBuYXZiYXIsXG4gICAgc2lkZWJhcixcbiAgICBsYXN0VXBkYXRlZDogXCJcdTY3MDBcdThGRDFcdTY2RjRcdTY1QjBcIixcblxuICAgIC8vIEdpdEh1YiBcdTRFRDNcdTVFOTNcdTRGNERcdTdGNkVcbiAgICByZXBvOiBcImxpeXVwaS9haS1ndWlkZVwiLFxuICAgIGRvY3NCcmFuY2g6IFwibWFzdGVyXCIsXG5cbiAgICAvLyBcdTdGMTZcdThGOTFcdTk0RkVcdTYzQTVcbiAgICBlZGl0TGlua3M6IHRydWUsXG4gICAgZWRpdExpbmtUZXh0OiBcIlx1NUI4Q1x1NTU4NFx1OTg3NVx1OTc2MlwiLFxuXG4gICAgLy8gQHRzLWlnbm9yZVxuICAgIC8vIFx1NUU5NVx1OTBFOFx1NzI0OFx1Njc0M1x1NEZFMVx1NjA2RlxuICAgIGZvb3RlcixcbiAgICAvLyBcdTk4OURcdTU5MTZcdTUzRjNcdTRGQTdcdThGQjlcdTY4MEZcbiAgICBleHRyYVNpZGVCYXIsXG4gIH0sXG59KTtcbiIsICIvKipcbiAqIFx1OTg5RFx1NTkxNlx1NTNGM1x1NEZBN1x1OEZCOVx1NjgwRlxuICovXG5leHBvcnQgZGVmYXVsdCBbXG4gIHtcbiAgICB0aXRsZTogXCJcdTdGMTZcdTdBMEJcdTVCRkNcdTgyMkFcIixcbiAgICBpY29uOiBcIi9pY29uL3hpbmdxaXUucG5nXCIsXG4gICAgcG9wb3ZlclRpdGxlOiBcIlx1NUZBRVx1NEZFMVx1NjI2Qlx1NEUwMFx1NjI2QlwiLFxuICAgIHBvcG92ZXJVcmw6IFwiL3FyY29kZS1jb2RlZmF0aGVyLnBuZ1wiLFxuICAgIHBvcG92ZXJEZXNjOiBcIlx1N0YxNlx1N0EwQlx1NUJGQ1x1ODIyQVx1RkYxQVx1N0YxNlx1N0EwQlx1NUJGQ1x1ODIyQVwiLFxuICB9LFxuICAvLyB7XG4gIC8vICAgdGl0bGU6IFwiXHU3RjE2XHU3QTBCXHU1QkZDXHU4MjJBXCIsXG4gIC8vICAgaWNvbjogXCIvaWNvbi94aW5ncWl1LnBuZ1wiLFxuICAvLyAgIHBvcG92ZXJUaXRsZTpcbiAgLy8gICAgICc8c3BhbiBzdHlsZT1cImZvbnQtc2l6ZTowLjhyZW07Zm9udC13ZWlnaHQ6Ym9sZDtcIj48c3BhbiBzdHlsZT1cImNvbG9yOnJlZDtcIj5cdTRGRERcdTU5QzZcdTdFQTdcdTVCOUVcdTYyMThcdTk4NzlcdTc2RUVcdTY1NTlcdTdBMEI8L3NwYW4+XHUzMDAxXHU3RjE2XHU3QTBCXHU1QjY2XHU0RTYwXHU2MzA3XHU1MzU3XHUzMDAxXHU1QjY2XHU0RTYwXHU4RDQ0XHU2RTkwXHUzMDAxXHU2QzQyXHU4MDRDXHU2MzA3XHU1MzU3XHUzMDAxXHU2MjgwXHU2NzJGXHU1MjA2XHU0RUFCXHUzMDAxXHU3RjE2XHU3QTBCXHU0RUE0XHU2RDQxPC9zcGFuPicsXG4gIC8vICAgcG9wb3ZlclVybDpcbiAgLy8gICAgIFwiL3FyY29kZS1jb2RlbmF2LnBuZ1wiLFxuICAvLyAgIHBvcG92ZXJEZXNjOiBcIlx1N0YxNlx1N0EwQlx1NUJGQ1x1ODIyQVx1RkYxQVx1N0YxNlx1N0EwQlx1NUJGQ1x1ODIyQVwiLFxuICAvLyB9LFxuICB7XG4gICAgdGl0bGU6IFwiXHU0RUE0XHU2RDQxXHU3RkE0XCIsXG4gICAgaWNvbjogXCIvaWNvbi93ZWl4aW4ucG5nXCIsXG4gICAgcG9wb3ZlclRpdGxlOlxuICAgICAgJzxzcGFuIHN0eWxlPVwiZm9udC1zaXplOjAuOHJlbTtmb250LXdlaWdodDpib2xkO1wiPlx1NjI2Qlx1NzgwMVx1NkRGQlx1NTJBMCA8c3BhbiBzdHlsZT1cImNvbG9yOnJlZDtcIj5cdTdGMTZcdTdBMEJcdTVCRkNcdTgyMkFcdTVDMEZcdTUyQTlcdTYyNEJcdTVGQUVcdTRGRTE8L3NwYW4+XHVGRjBDXHU2MkM5XHU0RjYwXHU4RkRCXHU0RTEzXHU1QzVFXHU3RjE2XHU3QTBCXHU1QjY2XHU0RTYwXHU0RUE0XHU2RDQxXHU3RkE0PC9zcGFuPicsXG4gICAgcG9wb3ZlclVybDogXCIvcXJjb2RlLWNvZGVuYXZoZWxwZXIucG5nXCIsXG4gIH0sXG4gIHtcbiAgICB0aXRsZTogXCJcdTRFMEJcdThENDRcdTY1OTlcIixcbiAgICBpY29uOiBcIi9pY29uL3hpYXphaS5wbmdcIixcbiAgICBwb3BvdmVyVGl0bGU6XG4gICAgICAnPHNwYW4gc3R5bGU9XCJmb250LXNpemU6MC44cmVtO2ZvbnQtd2VpZ2h0OmJvbGQ7XCI+XHU2MjZCXHU3ODAxXHU1MTczXHU2Q0U4XHU1MTZDXHU0RjE3XHU1M0Y3XHVGRjBDXHU1NkRFXHU1OTBEIDxzcGFuIHN0eWxlPVwiY29sb3I6cmVkO1wiPmFpPC9zcGFuPiBcdTgzQjdcdTUzRDZcdTZFMDVcdTUzNEVcdTU5MjdcdTVCNjYgRGVlcFNlZWsgXHU0RUNFXHU1MTY1XHU5NUU4XHU1MjMwXHU3Q0JFXHU5MDFBIFBERjwvc3Bhbj4nLFxuICAgIHBvcG92ZXJVcmw6IFwiL3FyY29kZS1tcGNvZGVyX3l1cGkuanBnXCIsXG4gICAgcG9wb3ZlckRlc2M6IFwiXHU1MTZDXHU0RjE3XHU1M0Y3OiBcdTdBMEJcdTVFOEZcdTU0NThcdTlDN0NcdTc2QUVcIixcbiAgfSxcbiAge1xuICAgIHRpdGxlOiBcIlx1NjUyRlx1NjMwMVx1NjIxMVwiLFxuICAgIGljb246IFwiL2ljb24vZGlhbnphbi5wbmdcIixcbiAgICBwb3BvdmVyVGl0bGU6ICcgPHNwYW4gc3R5bGU9XCJmb250LXNpemU6MC44cmVtO2ZvbnQtd2VpZ2h0OmJvbGQ7XCI+XHU5RjEzXHU1MkIxXHU1NDhDXHU4RDVFXHU4RDRGXHU2MjExPC9zcGFuPicsXG4gICAgcG9wb3ZlclVybDogXCIvcXJjb2RlLXRodW1iLmpwZ1wiLFxuICAgIHBvcG92ZXJEZXNjOiBcIlx1NjExRlx1OEMyMlx1NjBBOFx1NzY4NFx1NjUyRlx1NjMwMVx1RkYwQ1x1NEY1Q1x1ODAwNVx1NTkzNFx1NTNEMSsrXCIsXG4gIH0sXG5dO1xuIiwgIi8qKlxuICogXHU1RTk1XHU5MEU4XHU3MjQ4XHU2NzQzXHU0RkUxXHU2MDZGXG4gKi9cbmV4cG9ydCBkZWZhdWx0IHtcbiAgZnJpZW5kTGlua3M6IFtcbiAgICB7XG4gICAgICBsYWJlbDogXCJcdTdBRDlcdTk1N0YgLSBcdTdBMEJcdTVFOEZcdTU0NThcdTlDN0NcdTc2QUVcIixcbiAgICAgIC8vIGljb246IFwiL2ljb24vdXNlci5zdmdcIixcbiAgICAgIGhyZWY6IFwiaHR0cHM6Ly95dXl1YW53ZWIuZmVpc2h1LmNuL3dpa2kvQWJsZHc1V2tqaWR5U3hrS3hVMmNRZEF0bmFoXCIsXG4gICAgfSxcbiAgICB7XG4gICAgICBsYWJlbDogXCJcdTlDN0NcdTlFMjJcdTdGNTFcdTdFRENcIixcbiAgICAgIGhyZWY6IFwiaHR0cHM6Ly95dXl1YW53ZWIuY29tL1wiLFxuICAgIH0sXG4gICAge1xuICAgICAgbGFiZWw6IFwiXHU4MDAxXHU5QzdDXHU3QjgwXHU1Mzg2XCIsXG4gICAgICBocmVmOiBcImh0dHBzOi8vd3d3Lmxhb3l1amlhbmxpLmNvbS9cIixcbiAgICB9LFxuICAgIHtcbiAgICAgIGxhYmVsOiBcIlx1OTc2Mlx1OEJENVx1OUUyRFwiLFxuICAgICAgaHJlZjogXCJodHRwczovL3d3dy5taWFuc2hpeWEuY29tL1wiLFxuICAgIH0sXG4gICAge1xuICAgICAgbGFiZWw6IFwiXHU3RjE2XHU3QTBCXHU1QjY2XHU0RTYwXHU1NzA4XCIsXG4gICAgICBocmVmOiBcImh0dHBzOi8vd3d3LmNvZGVmYXRoZXIuY24vXCIsXG4gICAgfSxcbiAgXSxcbiAgY29weXJpZ2h0OiB7XG4gICAgaHJlZjogXCJodHRwczovL2JlaWFuLm1paXQuZ292LmNuL1wiLFxuICAgIG5hbWU6IFwiXHU2Q0FBSUNQXHU1OTA3MTkwMjY3MDZcdTUzRjctNlwiLFxuICB9LFxufTtcbiIsICJpbXBvcnQgeyBOYXZJdGVtIH0gZnJvbSBcInZ1ZXByZXNzL2NvbmZpZ1wiO1xuXG5leHBvcnQgZGVmYXVsdCBbXG4gIHtcbiAgICB0ZXh0OiBcIkFJIFx1OTg3OVx1NzZFRVwiLFxuICAgIGl0ZW1zOiBbXG4gICAgICB7XG4gICAgICAgIHRleHQ6IFwiQUkgXHU2RDc3XHU5RjlGXHU2QzY0XHU5ODc5XHU3NkVFXHU2NTU5XHU3QTBCXCIsXG4gICAgICAgIGxpbms6IFwiL2FpLVx1NkQ3N1x1OUY5Rlx1NkM2NFx1OTg3OVx1NzZFRVx1NjU1OVx1N0EwQi9cIixcbiAgICAgIH0sXG4gICAgICB7XG4gICAgICAgIHRleHQ6IFwiQUkgKyBDdXJzb3IgXHU1RjAwXHU1M0QxXHU0RTAwXHU0RTJBXHU0RUIyXHU2MjFBXHU4QkExXHU3Qjk3XHU1NjY4XCIsXG4gICAgICAgIGxpbms6IFwiL2FpLWN1cnNvci1cdTVGMDBcdTUzRDFcdTRFMDBcdTRFMkFcdTRFQjJcdTYyMUFcdThCQTFcdTdCOTdcdTU2NjgvXCIsXG4gICAgICB9LFxuICAgICAge1xuICAgICAgICB0ZXh0OiBcIkFJICsgQ3Vyc29yIFx1NUYwMFx1NTNEMVx1NEUwMFx1NEUyQVx1NkEyMVx1NjJERlx1OTc2Mlx1OEJENVx1N0NGQlx1N0VERlwiLFxuICAgICAgICBsaW5rOiBcIi9haS1jdXJzb3ItXHU1RjAwXHU1M0QxXHU0RTAwXHU0RTJBXHU2QTIxXHU2MkRGXHU5NzYyXHU4QkQ1XHU3Q0ZCXHU3RURGL1wiLFxuICAgICAgfSxcbiAgICAgIHtcbiAgICAgICAgdGV4dDogXCJBSSArIEN1cnNvciBcdTVGMDBcdTUzRDFcdTRFMDBcdTRFMkFcdTgwQkFcdTZEM0JcdTkxQ0ZcdTZENEJcdThCRDVcdTU2NjhcIixcbiAgICAgICAgbGluazogXCIvYWktY3Vyc29yLVx1NUYwMFx1NTNEMVx1NEUwMFx1NEUyQVx1ODBCQVx1NkQzQlx1OTFDRlx1NkQ0Qlx1OEJENVx1NTY2OC9cIixcbiAgICAgIH0sXG4gICAgXSxcbiAgfSxcbiAge1xuICAgIHRleHQ6IFwiRGVlcHNlZWtcIixcbiAgICBpdGVtczogW1xuICAgICAge1xuICAgICAgICB0ZXh0OiBcIlx1NTE3M1x1NEU4RURlZXBTZWVrXCIsXG4gICAgICAgIGxpbms6IFwiL0RlZXBzZWVrLyNcdTUxNzNcdTRFOEVkZWVwc2Vla1wiLFxuICAgICAgfSxcbiAgICAgIHtcbiAgICAgICAgdGV4dDogXCJEZWVwU2VlayBcdTRGN0ZcdTc1MjhcdTYzMDdcdTUzNTdcIixcbiAgICAgICAgbGluazogXCIvYWkvI2RlZXBzZWVrXHU0RjdGXHU3NTI4XHU2MzA3XHU1MzU3XCIsXG4gICAgICB9LFxuICAgICAge1xuICAgICAgICB0ZXh0OiBcIkFJIFx1NUU5NFx1NzUyOFx1NTczQVx1NjY2RlwiLFxuICAgICAgICBsaW5rOiBcIi9haS8jYWlcdTVFOTRcdTc1MjhcdTU3M0FcdTY2NkZcIixcbiAgICAgIH0sXG4gICAgICB7XG4gICAgICAgIHRleHQ6IFwiRGVlcFNlZWsgXHU4RDQ0XHU2RTkwXHU2QzQ3XHU2MDNCXCIsXG4gICAgICAgIGxpbms6IFwiL2FpLyNkZWVwc2Vla1x1OEQ0NFx1NkU5MFx1NkM0N1x1NjAzQlwiLFxuICAgICAgfSxcbiAgICAgIHtcbiAgICAgICAgdGV4dDogXCJEZWVwU2VlayBcdTYyODBcdTY3MkZcdTg5RTNcdTY3OTBcIixcbiAgICAgICAgbGluazogXCIvYWkvI2RlZXBzZWVrXHU2MjgwXHU2NzJGXHU4OUUzXHU2NzkwXCIsXG4gICAgICB9LFxuICAgICAge1xuICAgICAgICB0ZXh0OiBcIkFJIFx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRlwiLFxuICAgICAgICBsaW5rOiBcIi9haS8jQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUZcIixcbiAgICAgIH0sXG4gICAgXSxcbiAgfSxcbiAge1xuICAgIHRleHQ6IFwiXHVEODNEXHVERDI1XHU3RjE2XHU3QTBCXHU1QjY2XHU0RTYwXCIsXG4gICAgbGluazogXCJodHRwczovL3d3dy5jb2RlZmF0aGVyLmNuL1wiLFxuICB9LFxuICB7XG4gICAgdGV4dDogXCJBSSBcdTk3NjJcdThCRDVcdTk4OThcdTVFOTNcIixcbiAgICBsaW5rOiBcImh0dHBzOi8vd3d3Lm1pYW5zaGl5YS5jb20vP2NhdGVnb3J5PWFpXCIsXG4gIH0sXG4gIHtcbiAgICB0ZXh0OiBcIlx1NEY1Q1x1ODAwNVwiLFxuICAgIGxpbms6IFwiL1x1NEY1Q1x1ODAwNS9cIixcbiAgfSxcbl0gYXMgTmF2SXRlbVtdO1xuIiwgIlxuZXhwb3J0IGRlZmF1bHQgW1xuICBcIlwiLFxuICB7XG4gICAgXCJ0aXRsZVwiOiBcIlx1OUM3Q1x1NzZBRVx1NzY4NCBBSSBcdTYzMDdcdTUzNTdcIixcbiAgICBcImNvbGxhcHNhYmxlXCI6IHRydWUsXG4gICAgXCJjaGlsZHJlblwiOiBbXG4gICAgICBcIlx1OUM3Q1x1NzZBRVx1NzY4NCBBSSBcdTYzMDdcdTUzNTcvXHU5QzdDXHU3NkFFXHU3Njg0IEFJIFx1NjMwN1x1NTM1NyAtIDBcdTMwMDFcdTVGMDBcdTdCQzdcIixcbiAgICAgIFwiXHU5QzdDXHU3NkFFXHU3Njg0IEFJIFx1NjMwN1x1NTM1Ny9cdTlDN0NcdTc2QUVcdTc2ODQgQUkgXHU2MzA3XHU1MzU3IC0gMVx1MzAwMUFJIFx1NjgzOFx1NUZDM1x1Njk4Mlx1NUZGNVwiLFxuICAgICAgXCJcdTlDN0NcdTc2QUVcdTc2ODQgQUkgXHU2MzA3XHU1MzU3L1x1OUM3Q1x1NzZBRVx1NzY4NCBBSSBcdTYzMDdcdTUzNTcgLSAyXHUzMDAxQUkgXHU1QjlFXHU3NTI4XHU1REU1XHU1MTc3XCIsXG4gICAgICBcIlx1OUM3Q1x1NzZBRVx1NzY4NCBBSSBcdTYzMDdcdTUzNTcvXHU5QzdDXHU3NkFFXHU3Njg0IEFJIFx1NjMwN1x1NTM1NyAtIDNcdTMwMDFBSSBcdTdGMTZcdTdBMEJcdTYyODBcdTVERTdcIixcbiAgICAgIFwiXHU5QzdDXHU3NkFFXHU3Njg0IEFJIFx1NjMwN1x1NTM1Ny9cdTlDN0NcdTc2QUVcdTc2ODQgQUkgXHU2MzA3XHU1MzU3IC0gNFx1MzAwMUFJIFx1N0YxNlx1N0EwQlx1NjI4MFx1NjcyRlwiXG4gICAgXVxuICB9LFxuICB7XG4gICAgXCJ0aXRsZVwiOiBcIkFJXHU5ODc5XHU3NkVFXHU2NTU5XHU3QTBCXCIsXG4gICAgXCJjb2xsYXBzYWJsZVwiOiB0cnVlLFxuICAgIFwiY2hpbGRyZW5cIjogW1xuICAgICAgXCJBSVx1OTg3OVx1NzZFRVx1NjU1OVx1N0EwQi9BSSBcdTZENzdcdTlGOUZcdTZDNjRcdTk4NzlcdTc2RUVcdTY1NTlcdTdBMEJcIixcbiAgICAgIFwiQUlcdTk4NzlcdTc2RUVcdTY1NTlcdTdBMEIvQUkgKyBDdXJzb3IgXHU1RjAwXHU1M0QxXHU0RTAwXHU0RTJBXHU0RUIyXHU2MjFBXHU4QkExXHU3Qjk3XHU1NjY4XCIsXG4gICAgICBcIkFJXHU5ODc5XHU3NkVFXHU2NTU5XHU3QTBCL0FJICsgQ3Vyc29yIFx1NUYwMFx1NTNEMVx1NEUwMFx1NEUyQVx1NkEyMVx1NjJERlx1OTc2Mlx1OEJENVx1N0NGQlx1N0VERlwiLFxuICAgICAgXCJBSVx1OTg3OVx1NzZFRVx1NjU1OVx1N0EwQi9BSSArIEN1cnNvciBcdTVGMDBcdTUzRDFcdTRFMDBcdTRFMkFcdTgwQkFcdTZEM0JcdTkxQ0ZcdTZENEJcdThCRDVcdTU2NjhcIlxuICAgIF1cbiAgfSxcbiAge1xuICAgIFwidGl0bGVcIjogXCJcdTUxNzNcdTRFOEVEZWVwU2Vla1wiLFxuICAgIFwiY29sbGFwc2FibGVcIjogdHJ1ZSxcbiAgICBcImNoaWxkcmVuXCI6IFtcbiAgICAgIFwiXHU1MTczXHU0RThFRGVlcFNlZWsvRGVlcFNlZWsgXHU1MjFCXHU1OUNCXHU1NkUyXHU5NjFGXHU0RUNCXHU3RUNEXCIsXG4gICAgICBcIlx1NTE3M1x1NEU4RURlZXBTZWVrL0RlZXBTZWVrIFx1NTNEMVx1NUM1NVx1NTM4Nlx1N0EwQlwiLFxuICAgICAgXCJcdTUxNzNcdTRFOEVEZWVwU2Vlay9cdTRFQzBcdTRFNDhcdTY2MkYgRGVlcFNlZWtcIlxuICAgIF1cbiAgfSxcbiAge1xuICAgIFwidGl0bGVcIjogXCJEZWVwU2Vla1x1OEQ0NFx1NkU5MFx1NkM0N1x1NjAzQlwiLFxuICAgIFwiY29sbGFwc2FibGVcIjogdHJ1ZSxcbiAgICBcImNoaWxkcmVuXCI6IFtcbiAgICAgIFwiRGVlcFNlZWtcdThENDRcdTZFOTBcdTZDNDdcdTYwM0IvRGVlcFNlZWtcdTVCOThcdTY1QjlcdTY1NzRcdTc0MDZcdTc2ODRcdTZBMjFcdTU3OEJcdTVFOTRcdTc1MjhcdTU0OENcdTVERTVcdTUxNzdcIixcbiAgICAgIFwiRGVlcFNlZWtcdThENDRcdTZFOTBcdTZDNDdcdTYwM0IvRGVlcFNlZWsgXHU1QjY2XHU0RTYwXHU4RDQ0XHU2NTk5XCIsXG4gICAgICBcIkRlZXBTZWVrXHU4RDQ0XHU2RTkwXHU2QzQ3XHU2MDNCL0RlZXBTZWVrIFx1NUI5OFx1NjVCOVx1OTRGRVx1NjNBNVwiLFxuICAgICAgXCJEZWVwU2Vla1x1OEQ0NFx1NkU5MFx1NkM0N1x1NjAzQi9EZWVwU2VlayBcdTVGMDBcdTZFOTBcdTk4NzlcdTc2RUVcIlxuICAgIF1cbiAgfSxcbiAge1xuICAgIFwidGl0bGVcIjogXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRlwiLFxuICAgIFwiY29sbGFwc2FibGVcIjogdHJ1ZSxcbiAgICBcImNoaWxkcmVuXCI6IFtcbiAgICAgIHtcbiAgICAgICAgXCJ0aXRsZVwiOiBcIjIwMjUtMDRcIixcbiAgICAgICAgXCJjb2xsYXBzYWJsZVwiOiB0cnVlLFxuICAgICAgICBcImNoaWxkcmVuXCI6IFtcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDQvQ3Vyc29yIFx1OEZDRVx1Njc2NVx1NEU4Nlx1NUYzQVx1NTkyN1x1NzY4NFx1NUJGOVx1NjI0Qlx1RkYwQ0F1Z21lbnQgQ29kZSBcdTVCOUVcdTZENEJcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDQvXHU0RTAwXHU1RjIwXHU3MTY3XHU3MjQ3XHU3NTFGXHU2MjEwXHU4RkRFXHU4RDJGXHU1MTY4XHU3MjQ3XHVGRjAxUnVud2F5IEdlbjQgXHU2REYxXHU1OTFDXHU1M0QxXHU1RTAzXHVGRjBDXHU3RUM4XHU0RThFXHU2MzQ1XHU3ODM0IEFJIFx1ODlDNlx1OTg5MVx1NTkxQVx1NUU3NFx1NzY4NFx1NTkyOVx1ODJCMVx1Njc3RlwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wNC9BSSBcdTc1MUZcdTYwMDFcdTc2ODQgVVNCIFx1NjNBNVx1NTNFM1x1RkYxRlx1OTYzRlx1OTFDQ1x1MzAwMVx1ODE3RVx1OEJBRlx1NTE2OFx1OTc2Mlx1NjUyRlx1NjMwMSBNQ1BcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDQvNSBcdTUyMDZcdTk0OUZcdTc2RjRcdTUxRkEgNDYgXHU5ODc1XHU4QkJBXHU2NTg3XHVGRjAxXHU4QzM3XHU2QjRDIERlZXAgUmVzZWFyY2ggXHU1QjhDXHU3MjA2IE9wZW5BSVx1RkYwQ1x1NjcwMFx1NUYzQSBHZW1pbmkgMi41IFx1NTJBMFx1NjMwMVwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wNC9TaG9waWZ5IFx1NjVCMFx1NjgwN1x1NTFDNlx1RkYxQVx1NUMwNiBBSSBcdTg3OERcdTUxNjVcdTY1RTVcdTVFMzhcdTVERTVcdTRGNUNcdUZGMENcdTVERjJcdTY2MkZcdTU3RkFcdTY3MkNcdTg5ODFcdTZDNDJcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDQvXHUzMDBBXHU4MDZBXHU2NjBFXHU3Njg0XHU2NzNBXHU1NjY4XHVGRjBDXHU1OTBEXHU2NzQyXHU3Njg0XHU0RUJBXHU1RkMzXHUzMDBCXHVGRjBDXHU2NUFGXHU1NzY2XHU3OThGIEFJIFx1NjJBNVx1NTQ0QSAyMDI1IFx1NUU3NFx1NzI0OFx1NzY4NCAxMiBcdTRFMkFcdTRGRTFcdTUzRjdcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDQvXHU1QjlFXHU2RDRCXHVGRjFBXHU5NjNGXHU5MUNDXHU0RTkxXHU3NjdFXHU3MEJDXHU0RTBBXHU3RUJGXHUzMDBDXHU1MTY4XHU1NDY4XHU2NzFGIE1DUCBcdTY3MERcdTUyQTFcdTMwMERcdUZGMENBSSBcdTVERTVcdTUxNzdcdTRFMDBcdTdBRDlcdTVGMEZcdTYyNThcdTdCQTFcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDQvXHU2QjYzXHU1RjBGXHU2MzExXHU2MjE4XHU4QzM3XHU2QjRDXHVGRjAxT3BlbkFJIFx1NEUwQVx1N0VCRiBDaGF0R1BUIFx1NjQxQ1x1N0QyMlx1NTI5Rlx1ODBGRFwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wNC9NZXRhIFx1NkRGMVx1NTkxQ1x1NUYwMFx1NkU5MCBMbGFtYSA0XHVGRjAxXHU5OTk2XHU2QjIxXHU5MUM3XHU3NTI4IE1vRVx1RkYwQ1x1NjBDQVx1NEVCQVx1NTM0M1x1NEUwNyB0b2tlbiBcdTRFMEFcdTRFMEJcdTY1ODdcdUZGMENcdTdBREVcdTYyODBcdTU3M0FcdThEODVcdThEOEEgRGVlcFNlZWtcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDQvXHU5QTZDXHU0RTkxXHU3M0IwXHU4RUFCXHU5NjNGXHU5MUNDXHU0RTkxXHU1M0QxXHU1OEYwXHVGRjAxXHU4QzA4XHU3OUQxXHU2MjgwXHU0RUJBXHU1NDU4XHU4RDIzXHU0RUZCXHVGRjFBXHU4QkE5IEFJIFx1NjZGNFx1NjFDMlx1NEVCQVx1N0M3QlwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wNC9HZW1pbmkgMi41IFBybyBcdTVCOUVcdTZENEJcdUZGMUFcdTYyMTZcdTVDMDZcdTYyMTBcdTRFM0FcdTY3MDBcdTVCOUVcdTc1MjhcdTc2ODRcdTYzQThcdTc0MDZcdTZBMjFcdTU3OEJcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDQvXHU5NjNGXHU5MUNDXHU3OUQ4XHU1QkM2XHU3ODE0XHU1M0QxXHU2NUIwXHU2QTIxXHU1NzhCXHU1QzA2XHU1M0QxXHU1RTAzXHVGRjBDXHU1RjcxXHU1NENEXHU1MjlCXHU2MzA3XHU2ODA3XHU2MjEwXHU2NzAwXHU5MUNEXHU4OTgxXHU4MDAzXHU2ODM4XCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTA0L1x1NjcwOVx1NTNGMlx1NEVFNVx1Njc2NVx1NjcwMFx1NTkyN1x1NTI5Qlx1NUVBNlx1RkYwMVx1ODJGOVx1Njc5Q1x1OEZEQlx1NTE5Qlx1NTMzQlx1NzU5N1x1RkYwQ1x1OEJBMVx1NTIxMlx1NjYwRVx1NUU3NFx1NjNBOFx1NTFGQUFJXHU1MzNCXHU3NTFGIC0gXHU1MzRFXHU1QzE0XHU4ODU3XHU4OUMxXHU5NUZCXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTA0L1x1NEUwMFx1NUYyMFx1NzE2N1x1NzI0N1x1NzUxRlx1NjIxMFx1OEZERVx1OEQyRlx1NTE2OFx1NzI0N1x1RkYwMVJ1bndheSBHZW4tNCBcdTZERjFcdTU5MUNcdTUzRDFcdTVFMDNcdUZGMENcdTdFQzhcdTRFOEVcdTYzNDVcdTc4MzQgQUkgXHU4OUM2XHU5ODkxXHU1OTFBXHU1RTc0XHU3Njg0XHU1OTI5XHU4MkIxXHU2NzdGXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTA0L1x1NjY3QVx1OEMzMVx1NTNEMVx1NUUwMyBBdXRvR0xNIFx1NkM4OVx1NjAxRFx1RkYxQVx1OTk5Nlx1NEUyQVx1NTE0RFx1OEQzOVx1MzAwMVx1NTE3N1x1NTkwN1x1NkRGMVx1NUVBNlx1NzgxNFx1N0E3Nlx1NTQ4Q1x1NjRDRFx1NEY1Q1x1ODBGRFx1NTI5Qlx1NzY4NCBBSSBBZ2VudFwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wNC9NaW5pTWF4IEF1ZGlvIFx1NTNEMVx1NUUwMyBTcGVlY2gtMDIgXHU2QTIxXHU1NzhCXHVGRjBDXHU1MzU1XHU2QjIxXHU4RjkzXHU1MTY1XHU2NTJGXHU2MzAxIDIwIFx1NEUwN1x1NUI1N1x1N0IyNlwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wNC9cdTRFOUFcdTlBNkNcdTkwMEFcdTYzQThcdTUxRkEgTm92YSBBY3RcdUZGMUFcdTUzRUZcdTY0Q0RcdTYzQTdcdTdGNTFcdTk4NzVcdTZENEZcdTg5QzhcdTU2NjhcdTc2ODQgQUkgXHU2NjdBXHU4MEZEXHU0RjUzXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTA0L1x1ODE3RVx1OEJBRlx1NTE0M1x1NUI5RFx1OEJDNlx1NTZGRVx1NjUzRVx1NTkyN1x1NjJEQlx1RkYwMVx1NEUwMFx1NkIyMVx1NEYyMCAxMCBcdTVGMjBcdTU2RkVcdUZGMENcdTY3MEJcdTUzQ0JcdTU3MDhcdTY1ODdcdTY4NDhcdTMwMDFcdTc1MzVcdTVCNTBcdTRFNjZcdTkxRDFcdTUzRTVcdTUxNjhcdTY0MUVcdTVCOUFcdUZGMDFcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDQvXHU1NTJFXHU0RUY3XHU4RDg1IDcwMDAgXHU1MTQzXHVGRjBDTWV0YSBcdTYwRjNcdTc1MjhcdTc3M0NcdTk1NUNcdTUzRDZcdTRFRTMgaVBob25lXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTA0L1x1NzY3RVx1NUVBNlx1OThERVx1Njg2OFx1Njg0Nlx1NjdCNiAzLjAgXHU2QjYzXHU1RjBGXHU3MjQ4XHU1M0QxXHU1RTAzXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTA0L09wZW5BSSBcdTRFMEFcdTdFQkZcdTIwMUNPcGVuQUkgXHU1QjY2XHU5NjYyXHUyMDFEXHVGRjBDXHU1REYyXHU2M0QwXHU0RjlCXHU2NTcwXHU1MzQxXHU1QzBGXHU2NUY2XHU1MTREXHU4RDM5IEFJIFx1NUI2Nlx1NEU2MFx1OEQ0NFx1NkU5MFwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wNC94QUkgXHU1MThEXHU2NkY0XHU2NUIwXHVGRjBDXHU1NDA0XHU5ODc5XHU4MEZEXHU1MjlCXHU1MzUzXHU4RDhBXCJcbiAgICAgICAgXVxuICAgICAgfSxcbiAgICAgIHtcbiAgICAgICAgXCJ0aXRsZVwiOiBcIjIwMjUtMDNcIixcbiAgICAgICAgXCJjb2xsYXBzYWJsZVwiOiB0cnVlLFxuICAgICAgICBcImNoaWxkcmVuXCI6IFtcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDMvXHU5NjNGXHU5MUNDXHU1RjAwXHU2RTkwXHU1MTY4XHU2NUIwXHU2M0E4XHU3NDA2XHU2QTIxXHU1NzhCIFF3US0zMkJcdUZGMENcdTRFMDBcdTUzRjAgTWFjIFx1NUMzMVx1ODBGRFx1NUI5RVx1NzNCMFx1OTg3Nlx1N0VBN1x1NjNBOFx1NzQwNlx1ODBGRFx1NTI5QlwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMy9cdTVCOUVcdTZENEIgTWFudXNcdUZGMUFcdTk5OTZcdTRFMkFcdTc3MUZcdTVFNzJcdTZEM0IgQUlcdUZGMENcdTRFMkRcdTU2RkRcdTkwMjBcdUZGMDhcdTk2NDQgNTAgXHU0RTJBXHU3NTI4XHU0RjhCICsgXHU2MkM2XHU4OUUzXHVGRjA5XCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAzL1x1NzUyOFx1NEU4RVx1NEUzNFx1NUU4QVx1NURFNVx1NEY1Q1x1NkQ0MVx1N0EwQlx1NzY4NFx1NjVCMCBBSSBcdTUyQTlcdTYyNEJcdUZGMENcdTVGQUVcdThGNkZcdTYzQThcdTUxRkEgTWljcm9zb2Z0IERyYWdvbiBDb3BpbG90XCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAzL01vZGVsIENvbnRleHQgUHJvdG9jb2xcdUZGMENcdTc3MEJcdThGRDlcdTRFMDBcdTdCQzdcdTVDMzFcdTU5MUZcdTRFODZcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDMvXHU4QzM3XHU2QjRDIEdlbWluaSBcdTY1QjBcdTU4OUUgQ2FudmFzIFx1NEUwRVx1OTdGM1x1OTg5MVx1Njk4Mlx1ODlDOFx1NTI5Rlx1ODBGRFx1RkYwQ1x1NjNEMFx1NTM0N1x1NzUyOFx1NjIzN1x1NzUxRlx1NEVBN1x1NTI5QlwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMy9cdTlBNkNcdTY1QUZcdTUxNEJcdThGREJcdTUxOUIgQUkgXHU4OUM2XHU5ODkxXHVGRjBDXHU2NTM2XHU4RDJEXHU4OUM2XHU5ODkxXHU3NTFGXHU2MjEwXHU1MjFEXHU1MjFCXHU1MTZDXHU1M0Y4XHVGRjBDNCBcdTRFQkEgMTMgXHU0RTJBXHU2NzA4XHU2MjUzXHU5MDIwXHU3QzdCIFNvcmEgXHU2QTIxXHU1NzhCXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAzL1x1NzY3RVx1NUVBNlx1NjNBOFx1NTFGQVx1NEUyNFx1NkIzRSBBSSBcdTU5MjdcdTZBMjFcdTU3OEJcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDMvQ2xhdWRlIFx1NzNCMFx1NURGMlx1NjUyRlx1NjMwMVx1N0Y1MVx1N0VEQ1x1NjQxQ1x1N0QyMlx1NTI5Rlx1ODBGRFwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMy9EZWVwU2Vlay1WMyBcdTZBMjFcdTU3OEJcdTY2RjRcdTY1QjBcdUZGMENcdTU0MDRcdTk4NzlcdTgwRkRcdTUyOUJcdTUxNjhcdTk3NjJcdThGREJcdTk2MzZcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDMvXHU4MTdFXHU4QkFGXHU2REY3XHU1MTQzIFQxIFx1NkI2M1x1NUYwRlx1NzI0OFx1NTNEMVx1NUUwM1wiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMy9JZGVvZ3JhbSBcdTZCNjNcdTVGMEZcdTUzRDFcdTVFMDMgMy4wIFx1NzI0OFx1NjcyQ1x1NkEyMVx1NTc4Qlx1RkYxQVx1NzcxRlx1NUI5RVx1NjExRlx1NEUwRVx1NTIxQlx1NjEwRlx1ODg2OFx1NzNCMFx1NTE4RFx1N0E4MVx1NzgzNFwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMy9cdTY1QjBcdTYzQThcdTc0MDZcdTZBMjFcdTU3OEJcdTY3NjVcdTRFODZcdUZGMDFcdTk2M0ZcdTkxQ0MgUXdlbiBDaGF0IFx1NUU3M1x1NTNGMFx1NURGMlx1NEUwQVx1N0VCRlx1MjAxQ1x1NkRGMVx1NUVBNlx1NjAxRFx1ODAwM1x1MjAxRFx1NTI5Rlx1ODBGRFx1RkYwQ1x1NjUyRlx1NjMwMVx1ODA1NFx1N0Y1MVx1NjQxQ1x1N0QyMlwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMy9cdTUyMUFcdTUyMUFcdUZGMENHUFQtNG8gXHU1MzlGXHU3NTFGXHU1NkZFXHU1MENGXHU3NTFGXHU2MjEwXHU0RTBBXHU3RUJGXHVGRjBDUCBcdTU2RkVcdTMwMDFcdTc1MUZcdTU2RkVcdTRFNUZcdTVDMzFcdTRFMDBcdTU2MzRcdTc2ODRcdTRFOEJcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDMvXHU4QzM3XHU2QjRDXHU1M0QxXHU1RTAzIEdlbWluaSAyLjUgXHU0RUJBXHU1REU1XHU2NjdBXHU4MEZEXHU2QTIxXHU1NzhCXHVGRjBDXHU1QjlFXHU3M0IwXHU1OTBEXHU2NzQyXHU2MDFEXHU3RUY0XCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAzL1x1OEMzN1x1NkI0Q1x1N0VDOFx1NEU4RVx1NzY3Qlx1OTg3Nlx1NEUwMFx1NkIyMVx1NEU4Nlx1RkYwMVx1NjcwMFx1NUYzQVx1NjNBOFx1NzQwNlx1NkEyMVx1NTc4QkdlbWluaSAyLjUgUHJvXHU1QjlFXHU2RDRCXHU0RjUzXHU5QThDXHVGRjBDXHU3NzFGXHU3Njg0XHU2NzA5XHU3MEI5XHU0RTFDXHU4OTdGXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAzL0RlZXBTZWVrXHU1NkRFXHU3QjU0XHU3M0IwXHU1NzI4XHU4MEZEXHU0RTBEXHU4MEZEXHU1MTY1XHU2MjRCXHU5RUM0XHU5MUQxIFx1NUMwNlx1N0VGNFx1NjMwMVx1OUFEOFx1NEY0RFx1OTcwN1x1ODM2MVwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMy9EZWVwU2Vla1x1NUI5OFx1NjVCOVx1OEY5Rlx1OEMyM1x1RkYxQVIyXHU1M0QxXHU1RTAzXHU0RTNBXHU1MDQ3XHU2RDg4XHU2MDZGXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAzL1x1OUFEOFx1NjgyMVx1RkYwQ1x1NEUzQVx1NEY1NVx1NjcwMFx1NUZFQlx1NjJFNVx1NjJCMURlZXBTZWVrXHVGRjFGXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAzL1x1OERFOFx1NTg4M1x1NzUzNVx1NTU0Nlx1OEJENVx1N0VDM0FJXHVGRjBDRGVlcFNlZWtcdTUzRDZcdTRFRTNcdTRFODZDaGF0R1BUXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAzL1x1NEUwRFx1ODhDNVx1NEU4Nlx1RkYwMU9wZW5BSVx1NTI5Qlx1NEZDM1x1NzI3OVx1NjcxN1x1NjY2RVx1NUJGOVx1NEUyRFx1NTZGREFJXHU0RTBCXHU2QjdCXHU2MjRCXHVGRjBDXHU1MUZBXHU1M0YwXHUyMDFDQUlcdTUxRkFcdTUzRTNcdTdCQTFcdTUyMzZcdTIwMURcIlxuICAgICAgICBdXG4gICAgICB9LFxuICAgICAge1xuICAgICAgICBcInRpdGxlXCI6IFwiMjAyNS0wMlwiLFxuICAgICAgICBcImNvbGxhcHNhYmxlXCI6IHRydWUsXG4gICAgICAgIFwiY2hpbGRyZW5cIjogW1xuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMi9BbnRocm9waWMgQ2xhdWRlIDMuNyBTb25uZXQgXHU1M0QxXHU1RTAzXHVGRjFBXHU2M0E4XHU3NDA2XHU4MEZEXHU1MjlCXHU1MThEXHU4RkRCXHU1MzE2XHVGRjBDXHU3RjE2XHU3ODAxXHU2NTQ4XHU3Mzg3XHU3QTgxXHU3ODM0XHU1OTI5XHU5NjQ1XHVGRjAxXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAyL1x1OTYzRlx1OTFDQyBRd2VuIFx1OTk5Nlx1NEUyQVx1NjNBOFx1NzQwNlx1NkEyMVx1NTc4Qlx1NEVBRVx1NzZGOFx1RkYwMVwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMi9cdTg4QUJEZWVwU2Vla1x1NTIzQVx1NkZDMFx1NTIzMFx1NEU4Nlx1RkYxRlx1NjU4N1x1NUZDM1x1NEUwMFx1OEEwMFx1MzAwMUNoYXRHUFRcdTU0MENcdTY1RjZcdTVCQTNcdTVFMDNcdUZGMUFcdTUxNERcdThEMzlcdUZGMDEgXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAyL1x1OEZEMFx1ODQyNVx1NTU0Nlx1NTE2OFx1OTc2Mlx1NjNBNVx1NTE2NURlZXBTZWVrXHU2MTBGXHU1NDczXHU3NzQwXHU0RUMwXHU0RTQ4XHVGRjFGXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAyL1x1OTZGN1x1NTE5Qlx1RkYxQVx1OTRBNlx1NEY2OURlZXBTZWVrXHU1M0Q2XHU1Rjk3XHU3Njg0XHU2MjEwXHU1QzMxXHVGRjBDXHU2QkNGXHU0RTJBXHU0RUJBXHU1M0VGXHU4MEZEXHU5MEZEXHU4OTgxXHU1QjY2XHU0RTYwQUlcdTc3RTVcdThCQzZcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDIvXHU2RTA1XHU1MzRFXHU1OTI3XHU1QjY2XHVGRjFBXHU2NjZFXHU5MDFBXHU0RUJBXHU1OTgyXHU0RjU1XHU2MjkzXHU0RjRGRGVlcFNlZWtcdTdFQTJcdTUyMjkyMDI1XCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAyL1x1NzBCOVx1OEQ1RVx1NjUzNlx1ODVDRlx1RkYwMURlZXBTZWVrXHU1NzI4R2l0SHViXHU2NjFGXHU2ODA3XHU5MUNGXHU1REYyXHU4RDg1T3BlbkFJXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAyL1x1NzIwNlx1NTFCN1x1RkYwQ0RlZXBTZWVrXHU1MUZBXHU1QzQwXHVGRjBDXHU4MkY5XHU2NzlDQUlcdTU2RkRcdTg4NENcdTcyNDhcdTVDMDZcdTRFMEVcdTk2M0ZcdTkxQ0NcdTU0MDhcdTRGNUNcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDIvXHU3NzBCXHU3N0VEXHU1MjY3XHUzMDAxXHUyMDFDXHU0RUE0XHU2NzBCXHU1M0NCXHUyMDFEXHVGRjBDRGVlcFNlZWtcdTYzMjRcdThGREJcdTRFMkRcdTgwMDFcdTVFNzRcdTc5M0VcdTRFQTRcdTU3MDhcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDIvXHU0RTg5XHU1MTQ4XHU2MDUwXHU1NDBFXHU2M0E1XHU1MTY1RGVlcFNlZWtcdTc2ODRcdTU2RkRcdTRFQTdcdTYyNEJcdTY3M0FcdUZGMENcdTVCODNcdTRFRUNcdTc2ODRcdTgxRUFcdTc4MTRcdTU5MjdcdTZBMjFcdTU3OEJcdTYwMEVcdTRFNDhcdTUyOUVcdUZGMUZcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDIvXHU0RUY3XHU1MDNDXHU1MzQzXHU0RTA3XHU3RjhFXHU1MTQzXHU3Njg0XHUyMDFDQUkuY29tXHUyMDFEXHVGRjBDXHU0RjFBXHU2NjJGXHU4QzAxXHU3RUQ5RGVlcFNlZWtcdTUxNkNcdTUzRjhcdTRGN0ZcdTc1MjhcdTc2ODRcdTU0NjJcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDIvXHU1OTE2XHU4RDQ0XHU2NzNBXHU2Nzg0XHU3NzBCRGVlcFNlZWtcdUZGMUFcdTYzRDBcdTYzMkZcdTRFMkRcdTU2RkRcdTgwQTFcdTVFMDIgXHU2NzNBXHU0RjFBXHU4NUNGXHU1NzI4XHU4RkQ5XHU0RTlCXHU5ODg2XHU1N0RGXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAyL1x1NTkxQVx1NUJCNlx1OEY2Nlx1NEYwMVx1NjNBNVx1NTE2NUFJXHU1OTI3XHU2QTIxXHU1NzhCRGVlcFNlZWsgXHU2NjdBXHU4MEZEXHU2QzdEXHU4RjY2XHU1MThEXHU4RkRCXHU0RTAwXHU2QjY1XCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAyL1x1NTk4Mlx1NEY1NVx1NTIyOVx1NzUyOERlZXBTZWVrXHU3RkZCXHU4RUFCXHVGRjFGXHU2MjkzXHU0RjRGQUlcdTdFQTJcdTUyMjlcdUZGMENcdTY2NkVcdTkwMUFcdTRFQkFcdTRFNUZcdTgwRkRcdTkwMDZcdTg4QURcdTc2ODQzXHU0RTJBXHU2NUI5XHU1NDExXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAyL1x1NjVFNVx1NjcyQ1x1NTk4Mlx1NEY1NVx1NzcwQlx1NUY4NURlZXBzZWVrXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAyL1x1NkRGMVx1NUVBNlx1NkM0Mlx1N0QyMlx1NkI2M1x1NTcyOFx1NzI2OVx1ODI3Mlx1OTdFOVx1NTZGRFx1NEVCQVx1NURFNVx1NjY3QVx1ODBGRFx1NEVCQVx1NjI0RFx1RkYwQ1x1NUM1NVx1NUYwMFx1Nzg2RVx1NEZERFx1NEVCQVx1NjI0RFx1NzY4NFx1NjIxOFx1NEU4OVwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMi9EZWVwU2Vla1x1NzY4NFx1MjAxQ1x1NjcwRFx1NTJBMVx1NTY2OFx1N0U0MVx1NUZEOVx1MjAxRFx1OEJBOVx1NjI0MFx1NjcwOVx1NEVCQVx1NjI5M1x1NzJDMlx1RkYwQ1x1ODBDQ1x1NTQwRVx1N0E3Nlx1N0FERlx1NjYyRlx1NjAwRVx1NEU0OFx1NTZERVx1NEU4QlwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMi9EZWVwU2Vla1x1ODhBQlx1NUMwMVx1Njc0MFx1NEU4NlwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMi9EZWVwU2Vla1x1OTg4NFx1NkQ0Qlx1RkYxQVx1NjcyQVx1Njc2NTEwXHU1RTc0XHVGRjBDXHU1QzMxXHU0RTFBXHU1MjREXHU2NjZGXHU2NzAwXHU1OTdEXHU3Njg0MTBcdTRFMkFcdTRFMTNcdTRFMUFcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDIvRGVlcFNlZWtcdUZGMENcdTdEMjdcdTYwMjVcdTU4RjBcdTY2MEVcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDIvdml2b1x1NEUwRVx1ODM2M1x1ODAwMFx1NzZGOFx1N0VFN1x1NjNBNVx1NTE2NURlZXBTZWVrXHVGRjFBQUlcdTZERjFcdTVFQTZcdTg3OERcdTU0MDhcdTVGMTVcdTk4ODZcdTYyNEJcdTY3M0FcdTUyMUJcdTY1QjBcdTZGNkVcdTZENDFcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDIvdml2b1x1NUI5OFx1NUJBM1x1RkYxQVx1NUMwNlx1NkRGMVx1NUVBNlx1ODc4RFx1NTQwOFx1NkVFMVx1ODg0MFx1NzI0OERlZXBTZWVrXCIsXG4gICAgICAgICAgXCJBSVx1ODg0Q1x1NEUxQVx1OEQ0NFx1OEJBRi8yMDI1LTAyL1x1NEUwRFx1NTcyOFx1NTczQVx1NzY4NERlZXBTZWVrXHVGRjBDXHU2NjJGXHU1REY0XHU5RUNFQUlcdTVDRjBcdTRGMUFcdTc3MUZcdTZCNjNcdTc2ODRcdTRFM0JcdTg5RDJcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDIvRGVlcFNlZWtcdTIwMUNcdTY3MEJcdTUzQ0JcdTU3MDhcdTIwMURcdTRFMERcdTY1QURcdTYyNjlcdTU2RjRcdUZGMUExMFx1NUJCNlx1NTZGRFx1NTE4NVx1NTkxNlx1NEU5MVx1NTM4Mlx1NTU0Nlx1NUJBM1x1NUUwM1x1NjNBNVx1NTE2NVwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMi9EZWVwU2Vla1x1MjAxQ1x1OTFEMVx1ODc4RFx1NjcwQlx1NTNDQlx1NTcwOFx1MjAxRCBcdTRFQ0VcdTIwMUNcdTRFODlcdTUxNDhcdTIwMURcdTUyMzBcdTIwMUNcdTYwNTBcdTU0MEVcdTIwMURcdUZGMENcdTRFQ0VcdTIwMUNcdTU5N0RcdTc1MjhcdTIwMURcdTUyMzBcdTIwMUNcdTc1MjhcdTU5N0RcdTIwMURcIixcbiAgICAgICAgICBcIkFJXHU4ODRDXHU0RTFBXHU4RDQ0XHU4QkFGLzIwMjUtMDIvRGVlcFNlZWtcdTU5ODJcdTRGNTVcdTY0MDVcdTUyQThBSVx1NEVBN1x1NEUxQVx1RkYxRlwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMi9EZWVwU2Vla1x1NUJBM1x1NUUwM1x1NkRBOFx1NEVGN1x1RkYwMVwiLFxuICAgICAgICAgIFwiQUlcdTg4NENcdTRFMUFcdThENDRcdThCQUYvMjAyNS0wMi9EZWVwU2Vla1x1NUUyNlx1OThERVx1NzlEMVx1NTkyN1x1OEJBRlx1OThERVx1RkYxRlwiXG4gICAgICAgIF1cbiAgICAgIH1cbiAgICBdXG4gIH0sXG4gIHtcbiAgICBcInRpdGxlXCI6IFwiRGVlcFNlZWtcdTYyODBcdTY3MkZcdTg5RTNcdTY3OTBcIixcbiAgICBcImNvbGxhcHNhYmxlXCI6IHRydWUsXG4gICAgXCJjaGlsZHJlblwiOiBbXG4gICAgICB7XG4gICAgICAgIFwidGl0bGVcIjogXCJEZWVwU2VlayBcdTZBMjFcdTU3OEJcdThCQURcdTdFQzNcIixcbiAgICAgICAgXCJjb2xsYXBzYWJsZVwiOiB0cnVlLFxuICAgICAgICBcImNoaWxkcmVuXCI6IFtcbiAgICAgICAgICBcIkRlZXBTZWVrXHU2MjgwXHU2NzJGXHU4OUUzXHU2NzkwL0RlZXBTZWVrIFx1NkEyMVx1NTc4Qlx1OEJBRFx1N0VDMy9EZWVwU2Vlay1SMVx1NzY4NFx1NTZEQlx1NEUyQVx1OEJBRFx1N0VDM1x1OTYzNlx1NkJCNVwiLFxuICAgICAgICAgIFwiRGVlcFNlZWtcdTYyODBcdTY3MkZcdTg5RTNcdTY3OTAvRGVlcFNlZWsgXHU2QTIxXHU1NzhCXHU4QkFEXHU3RUMzL0RlZXBTZWVrLVIxXHU3Njg0XHU4QkFEXHU3RUMzXHU2RDQxXHU3QTBCXHU1RjNBXHU1MzE2XHU1QjY2XHU0RTYwXHVGRjA4UkxcdUZGMDlcdTk2MzZcdTZCQjVcdTkxQzdcdTc1MjhcdTRFODZHUlBPXHU3Qjk3XHU2Q0Q1XCIsXG4gICAgICAgICAgXCJEZWVwU2Vla1x1NjI4MFx1NjcyRlx1ODlFM1x1Njc5MC9EZWVwU2VlayBcdTZBMjFcdTU3OEJcdThCQURcdTdFQzMvRGVlcFNlZWstVjMgXHU5QUQ4XHU2NTQ4XHU4QkFEXHU3RUMzXHU1MTczXHU5NTJFXHU2MjgwXHU2NzJGXHU1MjA2XHU2NzkwXCIsXG4gICAgICAgICAgXCJEZWVwU2Vla1x1NjI4MFx1NjcyRlx1ODlFM1x1Njc5MC9EZWVwU2VlayBcdTZBMjFcdTU3OEJcdThCQURcdTdFQzMvRGVlcFNlZWtcdTUzNEVcdTRFM0RcdTY1ODdcdTk4Q0VcdTRFQ0VcdTRGNTVcdTgwMENcdTY3NjVcdUZGMUZcdTRFMUFcdTUxODVcdTRFQkFcdTU4RUJcdUZGMUFcdThCQURcdTdFQzNcdTY1NzBcdTYzNkVcdTMwMDFcdThCQURcdTdFQzNcdTdCNTZcdTc1NjVcdTU0OENcdThGRURcdTRFRTNcdTRGMThcdTUzMTZcdTdGM0FcdTRFMDBcdTRFMERcdTUzRUZcIlxuICAgICAgICBdXG4gICAgICB9LFxuICAgICAge1xuICAgICAgICBcInRpdGxlXCI6IFwiRGVlcFNlZWsgXHU2MjgwXHU2NzJGXHU1MjA2XHU2NzkwXCIsXG4gICAgICAgIFwiY29sbGFwc2FibGVcIjogdHJ1ZSxcbiAgICAgICAgXCJjaGlsZHJlblwiOiBbXG4gICAgICAgICAgXCJEZWVwU2Vla1x1NjI4MFx1NjcyRlx1ODlFM1x1Njc5MC9EZWVwU2VlayBcdTYyODBcdTY3MkZcdTUyMDZcdTY3OTAvRGVlcFNlZWtcdTY3MDBcdTVGM0FcdTRFMTNcdTRFMUFcdTYyQzZcdTg5RTNcdUZGMUFcdTZFMDVcdTRFQTRcdTU5MERcdTY1NTlcdTYzODhcdThEODVcdTc4NkNcdTY4MzhcdTg5RTNcdThCRkJcIixcbiAgICAgICAgICBcIkRlZXBTZWVrXHU2MjgwXHU2NzJGXHU4OUUzXHU2NzkwL0RlZXBTZWVrIFx1NjI4MFx1NjcyRlx1NTIwNlx1Njc5MC9EZWVwU2Vla1x1NzY4NFx1NEYxOFx1NTJCRlx1NEUwRVx1NEUwRFx1OERCM1wiLFxuICAgICAgICAgIFwiRGVlcFNlZWtcdTYyODBcdTY3MkZcdTg5RTNcdTY3OTAvRGVlcFNlZWsgXHU2MjgwXHU2NzJGXHU1MjA2XHU2NzkwL1x1NEUwMFx1NjU4N1x1OEJFNlx1ODlFMyBEZWVwU2VlayBcdTYyODBcdTY3MkZcdTY3QjZcdTY3ODRcIixcbiAgICAgICAgICBcIkRlZXBTZWVrXHU2MjgwXHU2NzJGXHU4OUUzXHU2NzkwL0RlZXBTZWVrIFx1NjI4MFx1NjcyRlx1NTIwNlx1Njc5MC9EZWVwU2VlayB2cy4gQ2hhdEdQVFx1RkYxQVx1OEMwMVx1NjI0RFx1NjYyRlx1NzcxRlx1NkI2M1x1NzY4NFx1NzM4Qlx1ODAwNVx1RkYxRlwiLFxuICAgICAgICAgIFwiRGVlcFNlZWtcdTYyODBcdTY3MkZcdTg5RTNcdTY3OTAvRGVlcFNlZWsgXHU2MjgwXHU2NzJGXHU1MjA2XHU2NzkwL0RlZXBTZWVrIFx1NzIwNlx1NzA2Qlx1OTAzQlx1OEY5MVx1MzAwMVx1ODg0Q1x1NEUxQVx1NUY3MVx1NTRDRFx1NTNDQVx1NUJGOVx1NjcyQVx1Njc2NUFJXHU1M0QxXHU1QzU1XHU3Njg0XHU1NDJGXHU3OTNBXCIsXG4gICAgICAgICAgXCJEZWVwU2Vla1x1NjI4MFx1NjcyRlx1ODlFM1x1Njc5MC9EZWVwU2VlayBcdTYyODBcdTY3MkZcdTUyMDZcdTY3OTAvRGVlcFNlZWstUjEgXHU2MjgwXHU2NzJGXHU1MTY4XHU2NjZGXHU4OUUzXHU2NzkwXHVGRjFBXHU0RUNFXHU1MzlGXHU3NDA2XHU1MjMwXHU1QjlFXHU4REY1XHU3Njg0XHUyMDFDXHU3MEJDXHU5MUQxXHU2NzJGXHU5MTREXHU2NUI5XHUyMDFEXCIsXG4gICAgICAgICAgXCJEZWVwU2Vla1x1NjI4MFx1NjcyRlx1ODlFM1x1Njc5MC9EZWVwU2VlayBcdTYyODBcdTY3MkZcdTUyMDZcdTY3OTAvRGVlcFNlZWtcdTYyODBcdTY3MkZcdTg5RTNcdThCRkJcdUZGMUFcdTRFQ0VWM1x1NTIzMFIxXHU3Njg0TW9FXHU2N0I2XHU2Nzg0XHU1MjFCXHU2NUIwXCJcbiAgICAgICAgXVxuICAgICAgfVxuICAgIF1cbiAgfSxcbiAge1xuICAgIFwidGl0bGVcIjogXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2RlwiLFxuICAgIFwiY29sbGFwc2FibGVcIjogdHJ1ZSxcbiAgICBcImNoaWxkcmVuXCI6IFtcbiAgICAgIHtcbiAgICAgICAgXCJ0aXRsZVwiOiBcIkRlZXBTZWVrICsgXHU3NDA2XHU4RDIyXCIsXG4gICAgICAgIFwiY29sbGFwc2FibGVcIjogdHJ1ZSxcbiAgICAgICAgXCJjaGlsZHJlblwiOiBbXG4gICAgICAgICAgXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2Ri9EZWVwU2VlayArIFx1NzQwNlx1OEQyMi9EZWVwU2Vla1x1NTQ0QVx1OEJDOVx1NjIxMVx1RkYxQTMwXHU1QzgxXHU1MjMwNDBcdTVDODFcdUZGMENcdTRFMDBcdTgyMkNcdTRGMUFcdTYyRTVcdTY3MDlcdThGRDlcdTRFNDhcdTU5MUFcdTc2ODRcdTVCNThcdTZCM0VcIixcbiAgICAgICAgICBcIkFJXHU1RTk0XHU3NTI4XHU1NzNBXHU2NjZGL0RlZXBTZWVrICsgXHU3NDA2XHU4RDIyL1x1NzUyOERlZXBTZWVrXHU2NDFFXHU5NEIxXHVGRjBDXHU2NUU1XHU4RDVBXHU3NjdFXHU0RTA3XCIsXG4gICAgICAgICAgXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2Ri9EZWVwU2VlayArIFx1NzQwNlx1OEQyMi9cdTY2NkVcdTkwMUFcdTRFQkFcdTU5ODJcdTRGNTVcdTkwMUFcdThGQzdcdTcwOTJcdTgwQTFcdTRFNzBcdTU3RkFcdTkxRDFcdThENUFcdTUyMzAxMDBcdTRFMDdcdUZGMUZcIixcbiAgICAgICAgICBcIkFJXHU1RTk0XHU3NTI4XHU1NzNBXHU2NjZGL0RlZXBTZWVrICsgXHU3NDA2XHU4RDIyL1x1NzUyOERlZXBzZWVrXHU1NkRFXHU3QjU0XHVGRjFBXHU1OTgyXHU2NzlDXHU2NzA5MTAwXHU0RTA3XHU5NUYyXHU5NEIxXHVGRjBDXHU1MUUwXHU1RTc0XHU1MTg1XHU0RTBEXHU3NTI4XHVGRjBDXHU4QkU1XHU2MDBFXHU0RTQ4XHU3NDA2XHU4RDIyXHVGRjFGXCJcbiAgICAgICAgXVxuICAgICAgfSxcbiAgICAgIHtcbiAgICAgICAgXCJ0aXRsZVwiOiBcIkRlZXBTZWVrICsgXHU3RjE2XHU3QTBCXHU1RjAwXHU1M0QxXCIsXG4gICAgICAgIFwiY29sbGFwc2FibGVcIjogdHJ1ZSxcbiAgICAgICAgXCJjaGlsZHJlblwiOiBbXG4gICAgICAgICAgXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2Ri9EZWVwU2VlayArIFx1N0YxNlx1N0EwQlx1NUYwMFx1NTNEMS8zIFx1NUMwRlx1NjVGNlx1NTA1QVx1NkUzOFx1NjIwRlx1RkYwQzEwIFx1NTkyOVx1NzJDMlx1OEQ1QSAyOCBcdTRFMDdcdUZGMDFcdTdBMEJcdTVFOEZcdTU0NThcdTc1MjggQUkgXHU4RUJBXHU4RDVBXHVGRjFGXCIsXG4gICAgICAgICAgXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2Ri9EZWVwU2VlayArIFx1N0YxNlx1N0EwQlx1NUYwMFx1NTNEMS9cdUQ4M0RcdURDOTdcdTc1MjggRGVlcFNlZWsgXHU3RUQ5XHU1QkY5XHU4QzYxXHU1MDVBXHU0RTJBXHU3RjUxXHU3QUQ5XHVGRjBDXHU1OTc5XHU0RTAwXHU1QjlBXHU2MTFGXHU1MkE4XHU1NzRGXHU0RTg2XCIsXG4gICAgICAgICAgXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2Ri9EZWVwU2VlayArIFx1N0YxNlx1N0EwQlx1NUYwMFx1NTNEMS9EZWVwU2Vla1x1ODhDNVx1OEZEQlZTQ29kZVx1RkYwQ1x1N0YxNlx1N0EwQlx1OTc1RVx1NUUzOFx1NEUxRFx1NkVEMVx1RkYwMVwiLFxuICAgICAgICAgIFwiQUlcdTVFOTRcdTc1MjhcdTU3M0FcdTY2NkYvRGVlcFNlZWsgKyBcdTdGMTZcdTdBMEJcdTVGMDBcdTUzRDEvXHU2NTU5XHU0RjYwXHU3NTI4RGVlcFNlZWsrQ2xpZW5cdUZGMENcdTRFQ0UwXHU1MjMwMVx1NUYwMFx1NTNEMVx1NEUwMFx1NEUyQUFQUFwiLFxuICAgICAgICAgIFwiQUlcdTVFOTRcdTc1MjhcdTU3M0FcdTY2NkYvRGVlcFNlZWsgKyBcdTdGMTZcdTdBMEJcdTVGMDBcdTUzRDEvRGVlcFNlZWtcdTYzQTVcdTUxNjVQeXRob25cdUZGMENcdTRFMDBcdTgyMkNcdTc1MzVcdTgxMTFcdTRFNUZcdTgwRkRcdTk4REVcdTkwMUZcdThERDFcdUZGMENcdTc4NkVcdTVCOUVcdTUzRUZcdTRFRTVcdTVDMDFcdTc5NUVcdTRFODZcdUZGMDFcIlxuICAgICAgICBdXG4gICAgICB9LFxuICAgICAge1xuICAgICAgICBcInRpdGxlXCI6IFwiRGVlcFNlZWsgKyBcdTUyMUJcdTYxMEZcdThCQkVcdThCQTFcIixcbiAgICAgICAgXCJjb2xsYXBzYWJsZVwiOiB0cnVlLFxuICAgICAgICBcImNoaWxkcmVuXCI6IFtcbiAgICAgICAgICBcIkFJXHU1RTk0XHU3NTI4XHU1NzNBXHU2NjZGL0RlZXBTZWVrICsgXHU1MjFCXHU2MTBGXHU4QkJFXHU4QkExL1x1NTQ4QyBEZWVwc2VlayBcdTgwNTRcdTYyNEJcdUZGMENcdTUwNUFcdTRFMkFcdTU0RUFcdTU0MTJcdTc2ODRcdTRFN0VcdTU3NjRcdTU3MDhcdTg5QzZcdTk4OTFcIixcbiAgICAgICAgICBcIkFJXHU1RTk0XHU3NTI4XHU1NzNBXHU2NjZGL0RlZXBTZWVrICsgXHU1MjFCXHU2MTBGXHU4QkJFXHU4QkExLzUgXHU0RTJBXHU0RTBEXHU1Rjk3XHU0RTBEXHU2NTM2XHU4NUNGXHU3Njg0IERlZXBzZWVrIFx1NzM4Qlx1NzBCOFx1N0VDNFx1NTQwOFx1RkYwMVwiLFxuICAgICAgICAgIFwiQUlcdTVFOTRcdTc1MjhcdTU3M0FcdTY2NkYvRGVlcFNlZWsgKyBcdTUyMUJcdTYxMEZcdThCQkVcdThCQTEvRGVlcFNlZWtcdTRFMDBcdTUzRTVcdThCRERcdTY0MUVcdTVCOUFcdTRGRUVcdTU2RkVcdTk2QkVcdTk4OThcIixcbiAgICAgICAgICBcIkFJXHU1RTk0XHU3NTI4XHU1NzNBXHU2NjZGL0RlZXBTZWVrICsgXHU1MjFCXHU2MTBGXHU4QkJFXHU4QkExL2RlZXBzZWVrK1x1NjU3MFx1NUI1N1x1NEVCQVx1NzM4Qlx1NzBCOFx1N0VDNFx1NTQwOFx1NEY3Rlx1NzUyOFx1NjVCOVx1NkNENVwiLFxuICAgICAgICAgIFwiQUlcdTVFOTRcdTc1MjhcdTU3M0FcdTY2NkYvRGVlcFNlZWsgKyBcdTUyMUJcdTYxMEZcdThCQkVcdThCQTEvXHU3NTI4IGRlZXBzZWVrIFx1NTA1QSBBSSBcdTg5QzZcdTk4OTFcdUZGMENcdTdFRERcdTRFODZcdUZGMENcdTU0OENcdTYyODRcdTRGNUNcdTRFMUFcdTRFMDBcdTY4MzdcdTdCODBcdTUzNTVcdUZGMDFcIixcbiAgICAgICAgICBcIkFJXHU1RTk0XHU3NTI4XHU1NzNBXHU2NjZGL0RlZXBTZWVrICsgXHU1MjFCXHU2MTBGXHU4QkJFXHU4QkExL1x1N0VERFx1N0VERFx1NUI1MFx1RkYwMVx1NzUyOGRlZXBzZWVrXHU1MDVBQUlcdTg5QzZcdTk4OTFcdUZGMENcdTZEQThcdTdDODkxMFcrXHVGRjA4XHU5NjQ0XHU0RkREXHU1OUM2XHU3RUE3XHU2NTU5XHU3QTBCXHVGRjA5XCIsXG4gICAgICAgICAgXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2Ri9EZWVwU2VlayArIFx1NTIxQlx1NjEwRlx1OEJCRVx1OEJBMS9cdThGRDlcdTYwMTVcdTY2MkZcdTUxNjhcdTdGNTFcdTY3MDBcdTVGM0FcdTc2ODQgRGVlcFNlZWsgXHU1NkZFXHU3MjQ3XHU2NTU5XHU3QTBCXHU1NDI3XHVGRjBDXHU4RDc2XHU3RDI3XHU2NTM2XHU4NUNGXHU0RTg2XHVGRjAxXCJcbiAgICAgICAgXVxuICAgICAgfSxcbiAgICAgIHtcbiAgICAgICAgXCJ0aXRsZVwiOiBcIkRlZXBTZWVrICsgXHU1MjlFXHU1MTZDXHU2NTQ4XHU3Mzg3XCIsXG4gICAgICAgIFwiY29sbGFwc2FibGVcIjogdHJ1ZSxcbiAgICAgICAgXCJjaGlsZHJlblwiOiBbXG4gICAgICAgICAgXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2Ri9EZWVwU2VlayArIFx1NTI5RVx1NTE2Q1x1NjU0OFx1NzM4Ny9cdTU5ODJcdTRGNTVcdTc1MjhEZWVwU2Vla1x1NjZGNFx1OUFEOFx1NjU0OFx1NTczMFx1NURFNVx1NEY1Q1x1RkYxQTEwXHU0RTJBXHU1QjlFXHU3NTI4XHU2MjgwXHU1REU3XCIsXG4gICAgICAgICAgXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2Ri9EZWVwU2VlayArIFx1NTI5RVx1NTE2Q1x1NjU0OFx1NzM4Ny9cdTYyNEJcdTYyOEFcdTYyNEJcdTY1NTlcdTRGNjBcdTU3Mjh3b3JkXHU0RTJEXHU2M0E1XHU1MTY1ZGVlcHNlZWtcdUZGMENcdTc5RDJcdTc1MUZcdTY1ODdcdTY4NjNcdTY3NTBcdTY1OTlcIixcbiAgICAgICAgICBcIkFJXHU1RTk0XHU3NTI4XHU1NzNBXHU2NjZGL0RlZXBTZWVrICsgXHU1MjlFXHU1MTZDXHU2NTQ4XHU3Mzg3L1x1NkNENVx1NUY4Qlx1NEVCQVx1NEZERFx1NTlDNlx1N0VBN2RlZXBzZWVrXHU0RjdGXHU3NTI4XHU2MzA3XHU1MzU3XHVGRjA4XHU5NjQ0XHU2MzA3XHU0RUU0XHU3MjQ4XHVGRjA5XCIsXG4gICAgICAgICAgXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2Ri9EZWVwU2VlayArIFx1NTI5RVx1NTE2Q1x1NjU0OFx1NzM4Ny9EZWVwU2VlayBSMSArIFx1NEUyQVx1NEVCQVx1NzdFNVx1OEJDNlx1NUU5M1x1RkYwQ1x1NzZGNFx1NjNBNVx1OEQ3N1x1OThERVx1RkYwMVwiLFxuICAgICAgICAgIFwiQUlcdTVFOTRcdTc1MjhcdTU3M0FcdTY2NkYvRGVlcFNlZWsgKyBcdTUyOUVcdTUxNkNcdTY1NDhcdTczODcvRGVlcFNlZWtcdTVENENcdTUxNjVcdTUyMzBFeGNlbFx1RkYwQ1x1NjNEMFx1NTM0NzEwXHU1MDBEXHU1REU1XHU0RjVDXHU2NTQ4XHU3Mzg3XHVGRjBDXHU1OTJBXHU3MjVCXHU0RTg2XHVGRjAxXCIsXG4gICAgICAgICAgXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2Ri9EZWVwU2VlayArIFx1NTI5RVx1NTE2Q1x1NjU0OFx1NzM4Ny9EZWVwU2Vla1x1OTE0RFx1NTQwOEtJTUlcdUZGMENcdTgxRUFcdTUyQThcdTc1MUZcdTYyMTBQUFRcdUZGMENcdTYxMUZcdTg5QzlcdTgxRUFcdTVERjFcdTg5ODFcdTU5MzFcdTRFMUFcdTRFODZcdUZGMDFcIixcbiAgICAgICAgICBcIkFJXHU1RTk0XHU3NTI4XHU1NzNBXHU2NjZGL0RlZXBTZWVrICsgXHU1MjlFXHU1MTZDXHU2NTQ4XHU3Mzg3L1dQU1x1OTFDQ1x1ODhDNVx1NEUwQWRlZXBzZWVrXHVGRjBDXHU3QjgwXHU3NkY0XHU1QzMxXHU2NjJGXHU1MjlFXHU1MTZDXHU3OTVFXHU1NjY4XCIsXG4gICAgICAgICAgXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2Ri9EZWVwU2VlayArIFx1NTI5RVx1NTE2Q1x1NjU0OFx1NzM4Ny9cdTUyMjlcdTc1MjhkZWVwc2Vla1x1NUVGQVx1N0FDQlx1NEUxM1x1NUM1RVx1OTUwMFx1NTUyRVx1NzdFNVx1OEJDNlx1NUU5M1wiLFxuICAgICAgICAgIFwiQUlcdTVFOTRcdTc1MjhcdTU3M0FcdTY2NkYvRGVlcFNlZWsgKyBcdTUyOUVcdTUxNkNcdTY1NDhcdTczODcvXHU2NTU5XHU1RTA4XHU1RkM1XHU1OTA3RGVlcFNlZWtcdTRGN0ZcdTc1MjhcdTYzMDdcdTUzNTdcdTY3NjVcdTRFODZcdUZGMDE1XHU1OTI3XHU2NTU5XHU1QjY2XHU1RTk0XHU3NTI4XHU1NzNBXHU2NjZGK1x1NUI5RVx1NjRDRFx1Njg0OFx1NEY4QitcdTk2OTBcdTg1Q0ZcdTc1MjhcdTZDRDVcIlxuICAgICAgICBdXG4gICAgICB9LFxuICAgICAge1xuICAgICAgICBcInRpdGxlXCI6IFwiRGVlcFNlZWsgKyBcdTUxODVcdTVCQjlcdTUyMUJcdTRGNUNcIixcbiAgICAgICAgXCJjb2xsYXBzYWJsZVwiOiB0cnVlLFxuICAgICAgICBcImNoaWxkcmVuXCI6IFtcbiAgICAgICAgICBcIkFJXHU1RTk0XHU3NTI4XHU1NzNBXHU2NjZGL0RlZXBTZWVrICsgXHU1MTg1XHU1QkI5XHU1MjFCXHU0RjVDLzNcdTc5RDJcdThCQTlEZWVwU2Vla1x1NTE5OVx1NTFGQVx1NzIwNlx1NkIzRVx1NUMwRlx1N0VBMlx1NEU2NlwiLFxuICAgICAgICAgIFwiQUlcdTVFOTRcdTc1MjhcdTU3M0FcdTY2NkYvRGVlcFNlZWsgKyBcdTUxODVcdTVCQjlcdTUyMUJcdTRGNUMvXHU0RUJBXHU2NzA5XHU1OTFBXHU1OTI3XHU4MEM2XHVGRjBDXHU1NzMwXHU2NzA5XHU1OTFBXHU1OTI3XHU0RUE3XHVGRjFBXHU1OTgyXHU0RjU1XHU3NTI4RGVlcFNlZWtcdTUxOTlcdTk1N0ZcdTdCQzdcdTVDMEZcdThCRjRcIixcbiAgICAgICAgICBcIkFJXHU1RTk0XHU3NTI4XHU1NzNBXHU2NjZGL0RlZXBTZWVrICsgXHU1MTg1XHU1QkI5XHU1MjFCXHU0RjVDL1x1NTk4Mlx1NEY1NVx1NTIyOVx1NzUyOERlZXBTZWVrXHU4RkRCXHU4ODRDXHU5QUQ4XHU2NTQ4XHU1MTg1XHU1QkI5XHU1MjFCXHU0RjVDXCIsXG4gICAgICAgICAgXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2Ri9EZWVwU2VlayArIFx1NTE4NVx1NUJCOVx1NTIxQlx1NEY1Qy9cdTc1MjhEZWVwU2Vla1x1NTA1QVx1NUMwRlx1N0VBMlx1NEU2Nlx1NzcxRlx1NzY4NFx1NTkyQVx1NzI1Qlx1NEU4Nlx1RkYwMVx1OEY3Qlx1OEY3Qlx1Njc3RVx1Njc3RVx1NjI1M1x1OTAyMFx1NzIwNlx1NkIzRVx1N0IxNFx1OEJCMFwiLFxuICAgICAgICAgIFwiQUlcdTVFOTRcdTc1MjhcdTU3M0FcdTY2NkYvRGVlcFNlZWsgKyBcdTUxODVcdTVCQjlcdTUyMUJcdTRGNUMvXHU3NTI4RGVlcFNlZWtcdTUxOTlcdTY1ODdcdTdBRTBcdUZGMUZcdThGRDk0XHU0RTJBXHU5QTlBXHU2NENEXHU0RjVDXHU4QkE5XHU0RjYwXHU4RUJBXHU1RTczXHU0RTVGXHU4MEZEXHU1MUZBXHU3MjA2XHU2QjNFXHVGRjAxXHVGRjA4XHU1NDJCXHU2M0QwXHU3OTNBXHU4QkNEXHVGRjA5XCIsXG4gICAgICAgICAgXCJBSVx1NUU5NFx1NzUyOFx1NTczQVx1NjY2Ri9EZWVwU2VlayArIFx1NTE4NVx1NUJCOVx1NTIxQlx1NEY1Qy9EZWVwU2Vla1x1NEY3Rlx1NzUyOFx1NjMwN1x1NTM1N1x1RkYxQVx1NjNEMFx1NTM0N1x1NTE2Q1x1NjU4N1x1MzAwMVx1NjVCMFx1OTVGQlx1NEUwRVx1NUU3Rlx1NTQ0QVx1NjU4N1x1Njg0OFx1NTE5OVx1NEY1Q1x1NjU0OFx1NzM4N1x1NzY4NFx1NEUwOVx1NTkyN1x1NjI4MFx1NURFNyBcIixcbiAgICAgICAgICBcIkFJXHU1RTk0XHU3NTI4XHU1NzNBXHU2NjZGL0RlZXBTZWVrICsgXHU1MTg1XHU1QkI5XHU1MjFCXHU0RjVDL0FJXHU1MTk5XHU1QzBGXHU4QkY0XHU2MDBFXHU0RTQ4XHU1MTk5XHVGRjFGZGVlcHNlZWtcdTVFMkVcdTRGNjBcdTUxOTlcdTVDMEZcdThCRjRcdTY1NTlcdTdBMEJcIlxuICAgICAgICBdXG4gICAgICB9XG4gICAgXVxuICB9LFxuICB7XG4gICAgXCJ0aXRsZVwiOiBcIkRlZXBTZWVrXHU0RjdGXHU3NTI4XHU2MzA3XHU1MzU3XCIsXG4gICAgXCJjb2xsYXBzYWJsZVwiOiB0cnVlLFxuICAgIFwiY2hpbGRyZW5cIjogW1xuICAgICAgXCJEZWVwU2Vla1x1NEY3Rlx1NzUyOFx1NjMwN1x1NTM1Ny9cdUQ4M0RcdUREMjVEZWVwU2VlayBcdTVDMEZcdTc2N0RcdTVGRUJcdTkwMUZcdTRFMEFcdTYyNEJcdTYzMDdcdTUzNTdcIixcbiAgICAgIFwiRGVlcFNlZWtcdTRGN0ZcdTc1MjhcdTYzMDdcdTUzNTcvXHU1MUUwXHU0RTJBXHU2MjgwXHU1REU3XHVGRjBDXHU2NTU5XHU0RjYwXHU1M0JCXHU5NjY0XHU2NTg3XHU3QUUwXHU3Njg0IEFJIFx1NTQ3M1x1RkYwMVwiLFxuICAgICAgXCJEZWVwU2Vla1x1NEY3Rlx1NzUyOFx1NjMwN1x1NTM1Ny9EZWVwU2VlayBcdTUzRDFcdTVFMDNcdTY1QjBcdTZBMjFcdTU3OEIgVjMtMDMyNFx1RkYwQ1x1OTY0NFx1NEY3Rlx1NzUyOFx1NjU1OVx1N0EwQlwiLFxuICAgICAgXCJEZWVwU2Vla1x1NEY3Rlx1NzUyOFx1NjMwN1x1NTM1Ny9cdTY3MDBcdTY1QjBcdTZFMDVcdTUzNEVcdTU5MjdcdTVCNjZEZWVwU2Vla1x1NEY3Rlx1NzUyOFx1NjI0Qlx1NTE4Q1x1N0IyQzEtNVx1NzI0OFx1RkYwQ1x1NUI5OFx1NjVCOVx1NUI4Q1x1NjU3NFx1NzI0OFBERlx1NTE0RFx1OEQzOVx1NEUwQlx1OEY3RFwiLFxuICAgICAgXCJEZWVwU2Vla1x1NEY3Rlx1NzUyOFx1NjMwN1x1NTM1Ny8yXHU1MjA2XHU5NDlGXHU1QjY2XHU0RjFBIERlZXBTZWVrIEFQSVx1RkYwQ1x1N0FERlx1NzEzNlx1NkJENFx1NUI5OFx1NjVCOVx1NjZGNFx1NTk3RFx1NzUyOFx1RkYwMVwiLFxuICAgICAgXCJEZWVwU2Vla1x1NEY3Rlx1NzUyOFx1NjMwN1x1NTM1Ny9cdTVCOENcdTY1NzRcdTY1M0JcdTc1NjVcdUZGMUFcdTU5ODJcdTRGNTVcdTc1MjhcdTU5N0REZWVwU2Vla1x1RkYwQ1x1NEUwMFx1NjU4N1x1NkM0N1x1NjAzQlx1RkYwMVwiLFxuICAgICAgXCJEZWVwU2Vla1x1NEY3Rlx1NzUyOFx1NjMwN1x1NTM1Ny9cdTMwMTBcdTZDNDdcdTYwM0JcdTMwMTFcdTZFRTFcdTg4NDBcdTcyNDggRGVlcFNlZWsgXHU3QjJDXHU0RTA5XHU2NUI5XHU0RjdGXHU3NTI4XHU2RTIwXHU5MDUzXCIsXG4gICAgICBcIkRlZXBTZWVrXHU0RjdGXHU3NTI4XHU2MzA3XHU1MzU3L0RlZXBTZWVrIFx1NjcyQ1x1NTczMFx1OTBFOFx1N0Y3Mlx1NjU1OVx1N0EwQlwiLFxuICAgICAgXCJEZWVwU2Vla1x1NEY3Rlx1NzUyOFx1NjMwN1x1NTM1Ny9cdTU5ODJcdTRGNTVcdTU3MjhpUGhvbmVcdTRFMEFcdTc1MjhcdThCRURcdTk3RjNcdThDMDNcdTc1MjhEZWVwc2Vla1wiLFxuICAgICAgXCJEZWVwU2Vla1x1NEY3Rlx1NzUyOFx1NjMwN1x1NTM1Ny9cdTY2NkVcdTkwMUFcdTRFQkFcdTgwRkRcdTc1MjhEZWVwU2Vla1x1NTA1QVx1NEVDMFx1NEU0OFx1RkYxRjIwXHU0RTJBXHU1QjlFXHU3NTI4XHU1RUZBXHU4QkFFXCIsXG4gICAgICB7XG4gICAgICAgIFwidGl0bGVcIjogXCJEZWVwU2VlayBcdTYzRDBcdTk1RUVcdTYyODBcdTVERTdcIixcbiAgICAgICAgXCJjb2xsYXBzYWJsZVwiOiB0cnVlLFxuICAgICAgICBcImNoaWxkcmVuXCI6IFtcbiAgICAgICAgICBcIkRlZXBTZWVrXHU0RjdGXHU3NTI4XHU2MzA3XHU1MzU3L0RlZXBTZWVrIFx1NjNEMFx1OTVFRVx1NjI4MFx1NURFNy81MFx1NEUyQVx1NUUzOFx1NzUyOFx1NzY4NERlZXBTZWVrXHU2QTIxXHU0RUZGXHU5OENFXHU2ODNDXHU2M0QwXHU3OTNBXHU4QkNEXHVGRjBDXHU1M0JCQUlcdTU0NzNcdTc2ODRcdTU5MjdcdTY3NDBcdTU2NjhcIixcbiAgICAgICAgICBcIkRlZXBTZWVrXHU0RjdGXHU3NTI4XHU2MzA3XHU1MzU3L0RlZXBTZWVrIFx1NjNEMFx1OTVFRVx1NjI4MFx1NURFNy9cdTYyMTFcdTUzRDFcdTczQjBcdTRFODYgRGVlcFNlZWsgXHU1M0JCIEFJIFx1NTQ3M1x1NzY4NFx1NjM3N1x1NUY4NFx1RkYwQ1x1NTkyQVx1OTk5OVx1NEU4NlwiLFxuICAgICAgICAgIFwiRGVlcFNlZWtcdTRGN0ZcdTc1MjhcdTYzMDdcdTUzNTcvRGVlcFNlZWsgXHU2M0QwXHU5NUVFXHU2MjgwXHU1REU3L0RlZXBTZWVrIFx1NjNEMFx1NzkzQVx1OEJDRFx1NTdGQVx1NjcyQ1x1NkNENVx1NTIxOVwiLFxuICAgICAgICAgIFwiRGVlcFNlZWtcdTRGN0ZcdTc1MjhcdTYzMDdcdTUzNTcvRGVlcFNlZWsgXHU2M0QwXHU5NUVFXHU2MjgwXHU1REU3L0RlZXBTZWVrXHU0RTBEXHU1OTdEXHU3NTI4XHVGRjFGXHU5MEEzXHU2NjJGXHU0RjYwXHU4RkQ4XHU0RTBEXHU3N0U1XHU5MDUzXHU4RkQ5XHU0RTlCXHU2MzA3XHU0RUU0XHVGRjAxXCIsXG4gICAgICAgICAgXCJEZWVwU2Vla1x1NEY3Rlx1NzUyOFx1NjMwN1x1NTM1Ny9EZWVwU2VlayBcdTYzRDBcdTk1RUVcdTYyODBcdTVERTcvXHU1NDEwXHU4ODQwXHU2NTc0XHU3NDA2XHVGRjAxRGVlcFNlZWtcdTc5NUVcdTdFQTdcdTYzMDdcdTRFRTRcdUZGMENcdTU5N0RcdTc1MjhcdTUyMzBcdTcyMDZcdUZGMDFcIixcbiAgICAgICAgICBcIkRlZXBTZWVrXHU0RjdGXHU3NTI4XHU2MzA3XHU1MzU3L0RlZXBTZWVrIFx1NjNEMFx1OTVFRVx1NjI4MFx1NURFNy9cdTY2NkVcdTkwMUFcdTRFQkFcdTRFNUZcdTgwRkRcdThGN0JcdTY3N0VcdTYzOENcdTYzRTFcdTc2ODQgMjAgXHU0RTJBIERlZXBTZWVrIFx1OUFEOFx1OTg5MVx1NjNEMFx1NzkzQVx1OEJDRFx1RkYwODIwMjVcdTcyNDhcdUZGMDlcIlxuICAgICAgICBdXG4gICAgICB9XG4gICAgXVxuICB9XG5dXG4gICIsICJpbXBvcnQgeyBTaWRlYmFyQ29uZmlnNE11bHRpcGxlIH0gZnJvbSBcInZ1ZXByZXNzL2NvbmZpZ1wiO1xuaW1wb3J0IEFJIGZyb20gXCIuL3NpZGViYXJzL2FpXCI7XG4vLyBAdHMtaWdub3JlXG5leHBvcnQgZGVmYXVsdCB7XG4gIFwiL0FJL1wiOiBBSSxcbiAgXCIvQUlcdTk4NzlcdTc2RUVcdTY1NTlcdTdBMEIvXCI6IEFJLFxuICAvLyBcdTk2NERcdTdFQTdcdUZGMENcdTlFRDhcdThCQTRcdTY4MzlcdTYzNkVcdTY1ODdcdTdBRTBcdTY4MDdcdTk4OThcdTZFMzJcdTY3RDNcdTRGQTdcdThGQjlcdTY4MEZcbiAgXCIvXCI6IFwiYXV0b1wiLFxufSBhcyBTaWRlYmFyQ29uZmlnNE11bHRpcGxlO1xuIl0sCiAgIm1hcHBpbmdzIjogIjtBQUFBOzs7QUNHQSxJQUFPLHVCQUFRO0FBQUEsRUFDYjtBQUFBLElBQ0UsT0FBTztBQUFBLElBQ1AsTUFBTTtBQUFBLElBQ04sY0FBYztBQUFBLElBQ2QsWUFBWTtBQUFBLElBQ1osYUFBYTtBQUFBO0FBQUEsRUFXZjtBQUFBLElBQ0UsT0FBTztBQUFBLElBQ1AsTUFBTTtBQUFBLElBQ04sY0FDRTtBQUFBLElBQ0YsWUFBWTtBQUFBO0FBQUEsRUFFZDtBQUFBLElBQ0UsT0FBTztBQUFBLElBQ1AsTUFBTTtBQUFBLElBQ04sY0FDRTtBQUFBLElBQ0YsWUFBWTtBQUFBLElBQ1osYUFBYTtBQUFBO0FBQUEsRUFFZjtBQUFBLElBQ0UsT0FBTztBQUFBLElBQ1AsTUFBTTtBQUFBLElBQ04sY0FBYztBQUFBLElBQ2QsWUFBWTtBQUFBLElBQ1osYUFBYTtBQUFBO0FBQUE7OztBQ3JDakIsSUFBTyxpQkFBUTtBQUFBLEVBQ2IsYUFBYTtBQUFBLElBQ1g7QUFBQSxNQUNFLE9BQU87QUFBQSxNQUVQLE1BQU07QUFBQTtBQUFBLElBRVI7QUFBQSxNQUNFLE9BQU87QUFBQSxNQUNQLE1BQU07QUFBQTtBQUFBLElBRVI7QUFBQSxNQUNFLE9BQU87QUFBQSxNQUNQLE1BQU07QUFBQTtBQUFBLElBRVI7QUFBQSxNQUNFLE9BQU87QUFBQSxNQUNQLE1BQU07QUFBQTtBQUFBLElBRVI7QUFBQSxNQUNFLE9BQU87QUFBQSxNQUNQLE1BQU07QUFBQTtBQUFBO0FBQUEsRUFHVixXQUFXO0FBQUEsSUFDVCxNQUFNO0FBQUEsSUFDTixNQUFNO0FBQUE7QUFBQTs7O0FDM0JWLElBQU8saUJBQVE7QUFBQSxFQUNiO0FBQUEsSUFDRSxNQUFNO0FBQUEsSUFDTixPQUFPO0FBQUEsTUFDTDtBQUFBLFFBQ0UsTUFBTTtBQUFBLFFBQ04sTUFBTTtBQUFBO0FBQUEsTUFFUjtBQUFBLFFBQ0UsTUFBTTtBQUFBLFFBQ04sTUFBTTtBQUFBO0FBQUEsTUFFUjtBQUFBLFFBQ0UsTUFBTTtBQUFBLFFBQ04sTUFBTTtBQUFBO0FBQUEsTUFFUjtBQUFBLFFBQ0UsTUFBTTtBQUFBLFFBQ04sTUFBTTtBQUFBO0FBQUE7QUFBQTtBQUFBLEVBSVo7QUFBQSxJQUNFLE1BQU07QUFBQSxJQUNOLE9BQU87QUFBQSxNQUNMO0FBQUEsUUFDRSxNQUFNO0FBQUEsUUFDTixNQUFNO0FBQUE7QUFBQSxNQUVSO0FBQUEsUUFDRSxNQUFNO0FBQUEsUUFDTixNQUFNO0FBQUE7QUFBQSxNQUVSO0FBQUEsUUFDRSxNQUFNO0FBQUEsUUFDTixNQUFNO0FBQUE7QUFBQSxNQUVSO0FBQUEsUUFDRSxNQUFNO0FBQUEsUUFDTixNQUFNO0FBQUE7QUFBQSxNQUVSO0FBQUEsUUFDRSxNQUFNO0FBQUEsUUFDTixNQUFNO0FBQUE7QUFBQSxNQUVSO0FBQUEsUUFDRSxNQUFNO0FBQUEsUUFDTixNQUFNO0FBQUE7QUFBQTtBQUFBO0FBQUEsRUFJWjtBQUFBLElBQ0UsTUFBTTtBQUFBLElBQ04sTUFBTTtBQUFBO0FBQUEsRUFFUjtBQUFBLElBQ0UsTUFBTTtBQUFBLElBQ04sTUFBTTtBQUFBO0FBQUEsRUFFUjtBQUFBLElBQ0UsTUFBTTtBQUFBLElBQ04sTUFBTTtBQUFBO0FBQUE7OztBQzlEVixJQUFPLGFBQVE7QUFBQSxFQUNiO0FBQUEsRUFDQTtBQUFBLElBQ0UsU0FBUztBQUFBLElBQ1QsZUFBZTtBQUFBLElBQ2YsWUFBWTtBQUFBLE1BQ1Y7QUFBQSxNQUNBO0FBQUEsTUFDQTtBQUFBLE1BQ0E7QUFBQSxNQUNBO0FBQUE7QUFBQTtBQUFBLEVBR0o7QUFBQSxJQUNFLFNBQVM7QUFBQSxJQUNULGVBQWU7QUFBQSxJQUNmLFlBQVk7QUFBQSxNQUNWO0FBQUEsTUFDQTtBQUFBLE1BQ0E7QUFBQSxNQUNBO0FBQUE7QUFBQTtBQUFBLEVBR0o7QUFBQSxJQUNFLFNBQVM7QUFBQSxJQUNULGVBQWU7QUFBQSxJQUNmLFlBQVk7QUFBQSxNQUNWO0FBQUEsTUFDQTtBQUFBLE1BQ0E7QUFBQTtBQUFBO0FBQUEsRUFHSjtBQUFBLElBQ0UsU0FBUztBQUFBLElBQ1QsZUFBZTtBQUFBLElBQ2YsWUFBWTtBQUFBLE1BQ1Y7QUFBQSxNQUNBO0FBQUEsTUFDQTtBQUFBLE1BQ0E7QUFBQTtBQUFBO0FBQUEsRUFHSjtBQUFBLElBQ0UsU0FBUztBQUFBLElBQ1QsZUFBZTtBQUFBLElBQ2YsWUFBWTtBQUFBLE1BQ1Y7QUFBQSxRQUNFLFNBQVM7QUFBQSxRQUNULGVBQWU7QUFBQSxRQUNmLFlBQVk7QUFBQSxVQUNWO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUE7QUFBQTtBQUFBLE1BR0o7QUFBQSxRQUNFLFNBQVM7QUFBQSxRQUNULGVBQWU7QUFBQSxRQUNmLFlBQVk7QUFBQSxVQUNWO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBO0FBQUE7QUFBQSxNQUdKO0FBQUEsUUFDRSxTQUFTO0FBQUEsUUFDVCxlQUFlO0FBQUEsUUFDZixZQUFZO0FBQUEsVUFDVjtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBO0FBQUE7QUFBQTtBQUFBO0FBQUEsRUFLUjtBQUFBLElBQ0UsU0FBUztBQUFBLElBQ1QsZUFBZTtBQUFBLElBQ2YsWUFBWTtBQUFBLE1BQ1Y7QUFBQSxRQUNFLFNBQVM7QUFBQSxRQUNULGVBQWU7QUFBQSxRQUNmLFlBQVk7QUFBQSxVQUNWO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUE7QUFBQTtBQUFBLE1BR0o7QUFBQSxRQUNFLFNBQVM7QUFBQSxRQUNULGVBQWU7QUFBQSxRQUNmLFlBQVk7QUFBQSxVQUNWO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUE7QUFBQTtBQUFBO0FBQUE7QUFBQSxFQUtSO0FBQUEsSUFDRSxTQUFTO0FBQUEsSUFDVCxlQUFlO0FBQUEsSUFDZixZQUFZO0FBQUEsTUFDVjtBQUFBLFFBQ0UsU0FBUztBQUFBLFFBQ1QsZUFBZTtBQUFBLFFBQ2YsWUFBWTtBQUFBLFVBQ1Y7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQTtBQUFBO0FBQUEsTUFHSjtBQUFBLFFBQ0UsU0FBUztBQUFBLFFBQ1QsZUFBZTtBQUFBLFFBQ2YsWUFBWTtBQUFBLFVBQ1Y7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUE7QUFBQTtBQUFBLE1BR0o7QUFBQSxRQUNFLFNBQVM7QUFBQSxRQUNULGVBQWU7QUFBQSxRQUNmLFlBQVk7QUFBQSxVQUNWO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUE7QUFBQTtBQUFBLE1BR0o7QUFBQSxRQUNFLFNBQVM7QUFBQSxRQUNULGVBQWU7QUFBQSxRQUNmLFlBQVk7QUFBQSxVQUNWO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQTtBQUFBO0FBQUEsTUFHSjtBQUFBLFFBQ0UsU0FBUztBQUFBLFFBQ1QsZUFBZTtBQUFBLFFBQ2YsWUFBWTtBQUFBLFVBQ1Y7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQTtBQUFBO0FBQUE7QUFBQTtBQUFBLEVBS1I7QUFBQSxJQUNFLFNBQVM7QUFBQSxJQUNULGVBQWU7QUFBQSxJQUNmLFlBQVk7QUFBQSxNQUNWO0FBQUEsTUFDQTtBQUFBLE1BQ0E7QUFBQSxNQUNBO0FBQUEsTUFDQTtBQUFBLE1BQ0E7QUFBQSxNQUNBO0FBQUEsTUFDQTtBQUFBLE1BQ0E7QUFBQSxNQUNBO0FBQUEsTUFDQTtBQUFBLFFBQ0UsU0FBUztBQUFBLFFBQ1QsZUFBZTtBQUFBLFFBQ2YsWUFBWTtBQUFBLFVBQ1Y7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBLFVBQ0E7QUFBQSxVQUNBO0FBQUEsVUFDQTtBQUFBO0FBQUE7QUFBQTtBQUFBO0FBQUE7OztBQzlQVixJQUFPLGtCQUFRO0FBQUEsRUFDYixRQUFRO0FBQUEsRUFDUixnQ0FBWTtBQUFBLEVBRVosS0FBSztBQUFBOzs7QUxEUCxJQUFNLFNBQVM7QUFDZixJQUFNLFNBQVM7QUFDZixJQUFNLE9BQU87QUFBQSxFQUNYO0FBQUEsRUFDQTtBQUFBLEVBQ0E7QUFBQSxFQUNBO0FBQUEsRUFDQTtBQUFBLEVBQ0E7QUFBQSxFQUNBO0FBQUEsRUFDQTtBQUFBLEVBQ0E7QUFBQSxFQUNBO0FBQUEsRUFDQTtBQUFBLEVBQ0E7QUFBQTtBQUdGLElBQU8saUJBQVEsYUFBYTtBQUFBLEVBQzFCLE9BQU87QUFBQSxFQUNQLGFBQ0U7QUFBQSxFQUNGLE1BQU07QUFBQSxJQUVKLENBQUMsUUFBUSxFQUFFLEtBQUssUUFBUSxNQUFNO0FBQUEsSUFFOUI7QUFBQSxNQUNFO0FBQUEsTUFDQTtBQUFBLFFBQ0UsTUFBTTtBQUFBLFFBQ04sU0FDRTtBQUFBO0FBQUE7QUFBQSxJQUlOO0FBQUEsTUFDRTtBQUFBLE1BQ0E7QUFBQSxNQUNBO0FBQUE7QUFBQTtBQUFBO0FBQUE7QUFBQTtBQUFBO0FBQUE7QUFBQTtBQUFBO0FBQUE7QUFBQSxFQVdKLFdBQVc7QUFBQSxFQUdYLGlCQUFpQixDQUFDLGtCQUFrQjtBQUFBLEVBQ3BDLFVBQVU7QUFBQSxJQUVSLGFBQWE7QUFBQSxJQUViLGdCQUFnQixDQUFDLE1BQU0sTUFBTSxNQUFNLE1BQU07QUFBQTtBQUFBLEVBRzNDLFNBQVM7QUFBQSxJQUNQLENBQUM7QUFBQSxJQUVEO0FBQUEsTUFDRTtBQUFBLE1BQ0E7QUFBQSxRQUNFLElBQUk7QUFBQTtBQUFBO0FBQUEsSUFHUixDQUFDO0FBQUEsSUFFRDtBQUFBLE1BQ0U7QUFBQSxNQUNBO0FBQUEsUUFDRSxXQUFXLENBQUMsR0FBRyxVQUFVLE1BQU0sUUFBUTtBQUFBLFFBQ3ZDLE9BQU8sQ0FBQyxVQUFVLE1BQU0sUUFBUTtBQUFBLFFBQ2hDLGFBQWEsQ0FBQyxVQUFVLE1BQU0sWUFBWSxlQUFlLE1BQU07QUFBQSxRQUMvRCxRQUFRLENBQUMsR0FBRyxVQUFVLE1BQU0sWUFBWSxVQUFVO0FBQUEsUUFDbEQsTUFBTSxDQUFDLFVBQVUsTUFBTSxZQUFZLFFBQVE7QUFBQSxRQUMzQyxNQUFNLENBQUMsVUFBVTtBQUFBLFFBQ2pCLEtBQUssQ0FBQyxHQUFHLE9BQU8sU0FBVSxPQUFNLFlBQVksVUFBVSxVQUFVLE1BQU07QUFBQSxRQUN0RSxPQUFPLENBQUMsT0FBTyxVQUNiLE1BQU0sWUFBWSxTQUNoQixPQUFNLFlBQVksVUFBVSxDQUFDLE1BQU0sWUFBWSxNQUFNLFdBQVcsV0FBWSxNQUFNLE1BQU0sWUFBWTtBQUFBLFFBQ3hHLGFBQWEsQ0FBQyxVQUFVLE1BQU0sWUFBWSxRQUFRLElBQUksS0FBSyxNQUFNLFlBQVk7QUFBQSxRQUM3RSxZQUFZLENBQUMsVUFBVSxNQUFNLGVBQWUsSUFBSSxLQUFLLE1BQU07QUFBQTtBQUFBO0FBQUEsSUFJL0Q7QUFBQSxNQUNFO0FBQUEsTUFDQTtBQUFBLFFBQ0UsVUFBVTtBQUFBO0FBQUE7QUFBQSxJQUlkLENBQUM7QUFBQSxJQUVELENBQUM7QUFBQSxJQUVEO0FBQUEsTUFDRTtBQUFBLE1BQ0E7QUFBQSxRQUNFLGFBQWE7QUFBQTtBQUFBO0FBQUEsSUFJakI7QUFBQSxNQUNFO0FBQUEsTUFDQTtBQUFBLFFBQ0UsZ0JBQWdCO0FBQUEsUUFDaEIsT0FBTztBQUFBLFFBRVAsbUJBQW1CO0FBQUE7QUFBQTtBQUFBLElBSXZCLENBQUM7QUFBQTtBQUFBLEVBR0gsYUFBYTtBQUFBLElBQ1gsTUFBTTtBQUFBLElBQ04sS0FBSztBQUFBLElBQ0w7QUFBQSxJQUNBLGFBQWE7QUFBQSxJQUdiLE1BQU07QUFBQSxJQUNOLFlBQVk7QUFBQSxJQUdaLFdBQVc7QUFBQSxJQUNYLGNBQWM7QUFBQSxJQUlkO0FBQUEsSUFFQTtBQUFBO0FBQUE7IiwKICAibmFtZXMiOiBbXQp9Cg==
