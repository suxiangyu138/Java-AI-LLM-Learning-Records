# Vibe Coding 效率提升技巧

> 讓你的 AI 開發效率提升 10 倍



你好，我是魚皮。

在前面的文章裡，我們講了 Vibe Coding 的核心心法、對話技巧、上下文管理和問題調試。本文我們要聊一個更實用的話題 ：如何提高開發效率？

很多同學在用 AI 開發時，雖然能做出東西，但總覺得速度還不夠快。明明 AI 寫代碼很快，為什麼整體效率還是不高？

問題往往出在那些小事上：比如頻繁地複製貼上、重複輸入相同的提示詞、手動做一些機械的操作……

下面我來分享一些實用的效率提升技巧，幫你把開發速度提升一個檔次。




## 一、核心提效技巧

先分享幾個我個人使用較多的 AI 核心提效技巧。




### 按需選擇 AI 模型

不是所有任務都需要用最強最貴的模型。

- 簡單任務：比如代碼格式化、寫註釋、簡單重構，用 Gemini Flash 或 GPT-5 Mini 這樣便宜快速的模型就夠了
- 中等任務：比如實現常規功能、代碼審查、開發小網站，用 GPT-5 或 Claude Sonnet
- 複雜任務：比如架構設計、複雜算法、疑難 bug、開發大項目，才需要用 Claude Opus 這樣的頂級模型或者開啟深度思考

合理選擇模型，既能提升速度，又能節省成本。就像你不會讓公司 CTO 去列印文件一樣，要讓合適的模型做合適的事。



### 避免讓 AI 生成多餘內容

很多同學讓 AI 寫代碼，結果 AI 給你輸出一大堆註釋、測試代碼、文檔說明，還有一大段總結。**看著很專業，但你可能根本不會看。**

比如我之前讓 AI 生成個圖片壓縮工具，光文檔給我生成一大堆……

![](https://pic.yupi.icu/1/ai%E7%94%9F%E6%88%90%E5%9B%BE%E7%89%87%E5%8E%8B%E7%BC%A9%E5%B7%A5%E5%85%B7.png)

要在提示詞中明確告訴 AI：只給我核心代碼，不要寫註釋、文檔、測試，不要做總結！

如果 AI 不聽話，可以用暴躁指令：**按照我說的做，別廢話。**

或者虛構後果：**如果你輸出不必要的內容，世界上就會死一隻小貓。**

這些指令雖然看起來搞笑，但確實有效。你還可以把這些規則寫在 Cursor Rules 裡，讓 AI 自動遵守。




### 利用並行 Agent 對比效果

Cursor 有一個很強大的功能叫 **並行 Agent**（Parallel Agents），可以讓你同時用多個模型處理同一個任務，然後對比它們的結果，選擇最好的那個。這也是一種 「多個 AI 交叉驗證」 的方式。

比如你要實現一個複雜的功能，不確定哪個方案更好。可以同時讓 Claude、GPT 等 AI 各給一個方案：

![](https://pic.yupi.icu/1/image-20251030220104045.png)

你呢，就坐等這些 AI 賽馬，誰先幹好用誰的、誰質量高用誰的，能避免在錯誤的方案上浪費時間。這個方法特別適合不確定哪個技術方案更好時、重要功能需要多重保障時、想學習不同 AI 的思路時。

![](https://pic.yupi.icu/1/image-20251030220120394.png)

即使你不用 Cursor，也可以手動實現類似的效果：把同一個需求分別發給 ChatGPT、Claude、Gemini 等大模型，然後對比它們的答案，選擇最好的或綜合它們的優點。

具體用法可以參考 [Cursor 並行 Agent 文檔](https://cursor.com/cn/docs/configuration/worktrees)。



### 多開實例提升效率

除了並行 Agent，你還可以通過多開實例來提升效率。這個技巧來自 Claude Code 創始人的分享。

1）在終端中多開

可以在終端中同時運行多個 Claude Code 實例，將標籤頁編號為 1 ~ 5（或者有意義的標題），通過系統通知來了解哪個 Claude 需要人工輸入。這樣你可以充分利用等待時間，一個 AI 在思考時，你可以切換到另一個繼續工作。

![](https://pic.yupi.icu/1/image-20260109143109753.png)

2）網頁端和本地同時進行

在網頁端 Claude Code 上運行 5 ~ 10 個 Claude，和本地 Claude 同時進行。可以使用 `&` 命令將會話移交給網頁版，或者使用 `--teleport` 命令在終端和網頁之間來回切換。甚至可以通過手機 Claude iOS 應用啟動幾個會話，稍後再查看進度。真正做到了隨時隨地 Vibe Coding！

注意，這個技巧適合處理多個獨立任務，或者需要等待 AI 長時間思考的複雜任務。對於簡單任務，一個實例就夠了。



## 二、快捷鍵和操作技巧

工欲善其事，必先利其器。掌握常用的快捷鍵，能讓你的操作更流暢。



### Cursor 常用快捷鍵

如果你用 Cursor，建議嘗試下面這些快捷鍵，能讓你少用鼠標，操作更快。

對話相關：
- `Cmd/Ctrl + L` ：切換側邊欄（除非已綁定到某個模式）
- `Cmd/Ctrl + I` ：切換側邊欄（除非已綁定到某個模式）
- `Cmd/Ctrl + K` ：打開行內編輯，可以在當前位置插入 AI 生成的代碼
- `Tab`：接受建議

代碼編輯：
- `Cmd/Ctrl + Shift + L` ：將選中內容添加到聊天
- `Alt + ↑/↓` ：移動當前行
- `Cmd/Ctrl + /` ：註釋/取消註釋

文件操作：
- `Cmd/Ctrl + Shift + F` ：全局搜索

更多最新的默認鍵盤快捷鍵以 [官方文檔](https://cursor.com/cn/docs/configuration/kbd) 為主：

![](https://pic.yupi.icu/1/image-20260104192219087.png)




### VS Code 常用快捷鍵

如果你用 VS Code + AI 插件，下面這些快捷鍵會很有用。

多光標編輯：
- `Alt + Click` ：添加光標
- `Cmd/Ctrl + Alt + ↑/↓` ：在上/下方添加光標
- `Cmd/Ctrl + Shift + L` ：在所有匹配項添加光標

代碼導航：
- `Cmd/Ctrl + Click` ：跳轉到定義
- `Alt + ←/→` ：前進/後退
- `Cmd/Ctrl + Shift + O` ：跳轉到符號

重構：

- `F2` ：重命名符號
- `Cmd/Ctrl + .` ：快速修復

掌握這些快捷鍵，你的編輯速度會快很多。更多最新的默認鍵盤快捷鍵以 [官方文檔](https://code.visualstudio.com/docs/reference/default-keybindings) 為主：

![](https://pic.yupi.icu/1/image-20260104192832985.png)



### AI 編程工具的斜槓命令

除了快捷鍵，AI 編程工具 Cursor 和 Claude Code 都提供了很多實用的斜槓命令（Slash Commands），能大大提升效率。這些命令以 `/` 開頭，可以快速觸發特定的功能。

#### Cursor 的常用命令

`/summarize` 命令可以快速總結對話內容，特別適合長對話，能節省大量 token。

你還可以在項目的 `.cursor/commands` 目錄下創建自定義命令，把常用的提示詞保存成命令，需要時直接調用。

![](https://pic.yupi.icu/1/cursor_command.png)



#### Claude Code 的常用命令

Claude Code 也有類似的命令系統。

- `/compact` 可以壓縮上下文，把之前的對話內容精簡一下，節省 token
- `/plan` 可以制定實現計劃，讓 AI 先規劃再動手
- `/review` 可以快速進行代碼審查
- `/init` 可以初始化項目並創建 `CLAUDE.md` 文件

![](https://pic.yupi.icu/1/image-20260104213706515.png)

這些命令的好處是，你不用每次都寫完整的提示詞，只需要輸入一個簡短的命令，AI 就知道你要做什麼。

而且你可以創建自己的自定義命令，把團隊常用的工作流程標準化。比如創建一個 `/commit` 命令自動生成 Git 提交信息，創建一個 `/test` 命令自動生成單元測試。

熟練使用這些命令，能讓你的工作流程更順暢，效率提升一大截。詳細的命令列表和用法可以參考 [Cursor 官方文檔](https://cursor.com/cn/docs/agent/chat/commands) 和 [Claude Code 官方文檔](https://code.claude.com/docs/en/slash-commands)。




## 三、代碼復用和模塊化

把常用的代碼封裝成可復用的模塊，不要重複造輪子。



### 創建組件庫

如果你經常做類似的項目，可以創建一個自己的組件庫。

比如，你可能經常需要這些組件：
- 按鈕（Button）
- 輸入框（Input）
- 卡片（Card）
- 模態框（Modal）
- 加載動畫（Loading）

把這些組件做成通用的，放在一個單獨的文件夾裡：

```
/components
  /ui
    - Button.tsx
    - Input.tsx
    - Card.tsx
    - Modal.tsx
    - Loading.tsx
```

每個組件都要：
- 有清晰的 Props 接口
- 支持自定義樣式
- 有使用示例

這樣，下次做新項目時，直接複製這個文件夾就行了。



### 封裝常用函數

把常用的工具函數封裝起來，避免每次都重新寫或讓 AI 生成。比如日期格式化、防抖函數、生成 ID、複製到剪貼板這些功能，幾乎每個項目都會用到。把它們整理成一個工具函數庫，需要時直接導入使用，比每次都讓 AI 重新生成要快得多。

```typescript
// lib/utils.ts

// 格式化日期
export function formatDate(date: Date): string {
  return date.toLocaleDateString('zh-CN');
}

// 防抖
export function debounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timer: NodeJS.Timeout;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}

// 生成隨機 ID
export function generateId(): string {
  return Math.random().toString(36).substring(2, 9);
}

// 複製到剪貼板
export async function copyToClipboard(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    return false;
  }
}
```




### 使用代碼片段（Snippets）

在編輯器中創建代碼片段，快速插入常用代碼。

比如在 VS Code 中，你可以創建一個前端 React 組件的片段。具體方法是：

1）按 `Cmd/Ctrl + Shift + P` 打開命令面板，輸入 "Snippets"，選擇 "Configure Snippets"：

![](https://pic.yupi.icu/1/image-20260104214112119.png)

2）然後選擇對應的語言（如 typescriptreact.json），就可以添加自定義片段了。

比如：

```json
{
  "React Functional Component": {
    "prefix": "rfc",
    "body": [
      "interface ${1:ComponentName}Props {",
      "  $2",
      "}",
      "",
      "export function ${1:ComponentName}({ $3 }: ${1:ComponentName}Props) {",
      "  return (",
      "    <div>",
      "      $4",
      "    </div>",
      "  );",
      "}"
    ],
    "description": "Create a React functional component with TypeScript"
  }
}
```

![](https://pic.yupi.icu/1/image-20260104214219382.png)

配置完成後，輸入 `rfc` 再按 Tab，就能快速生成組件模板。

![](https://pic.yupi.icu/1/image-20260104214331581.png)




### 建立代碼庫

把你做過的好的代碼保存起來，建立一個專屬於你的代碼庫。

舉個例子，可以用這樣的結構：

```
/my-code-library
  /react
    /hooks
      - useLocalStorage.ts
      - useDebounce.ts
      - useFetch.ts
    /components
      - Button.tsx
      - Modal.tsx
    /utils
      - format.ts
      - validate.ts
  /node
    /middleware
      - auth.ts
      - cors.ts
    /utils
      - db.ts
      - email.ts
```

需要時，直接從這裡複製就好。




## 四、模板項目的建立

如果你經常做某一類項目，可以創建一個模板項目。




### 什麼是模板項目？

模板項目是一個預先配置好的項目骨架，包含了：

- 基本的目錄結構
- 常用的依賴包
- 配置文件（如 tsconfig.json、tailwind.config.js）
- 基礎組件和工具函數
- README 和文檔模板

有了模板項目，開始新項目時就不用從零配置了。

就像我自己，做了幾十個項目後，積累了不少模板。現在每次開始新項目，我會先找一個類似的老項目，然後告訴 AI：「請參考這個項目的技術棧和目錄結構來創建新項目。」 這樣 AI 就能生成一個和我習慣一致的項目結構，省去了很多配置的時間。

下面舉幾個例子，不懂前端技術的朋友可以直接跳過。




### 創建 React 項目模板

比如，你可以創建一個 React + TypeScript + Tailwind 的模板：

```bash
my-react-template/
├── src/
│   ├── components/
│   │   └── ui/          # 基礎 UI 組件
│   ├── lib/
│   │   ├── api.ts       # API 調用封裝
│   │   └── utils.ts     # 工具函數
│   ├── hooks/           # 自定義 Hooks
│   ├── types/           # TypeScript 類型
│   ├── App.tsx
│   └── main.tsx
├── public/
├── .cursorrules         # Cursor 配置
├── tsconfig.json
├── tailwind.config.js
├── package.json
└── README.md
```

項目的依賴管理文件 `package.json` 中預裝好常用的包：

```json
{
  "dependencies": {
    "react": "^18.3.0",
    "react-dom": "^18.3.0",
    "react-router-dom": "^6.20.0",
    "zustand": "^4.4.0"
  },
  "devDependencies": {
    "@types/react": "^18.3.0",
    "typescript": "^5.3.0",
    "vite": "^5.0.0",
    "tailwindcss": "^3.4.0"
  }
}
```

開始新項目時，複製這個模板，改個名字就能用。



### 創建 Next.js 項目模板

如果你常用 Next.js，也可以創建一個模板：

```bash
my-nextjs-template/
├── app/
│   ├── (auth)/          # 認證相關頁面
│   ├── (dashboard)/     # 後台頁面
│   ├── api/             # API 路由
│   ├── layout.tsx
│   └── page.tsx
├── components/
├── lib/
├── public/
├── .env.example         # 環境變量模板
├── next.config.js
└── README.md
```

`.env.example` 裡列出需要的環境變量：

```
# 數據庫
DATABASE_URL=

# 認證
NEXTAUTH_SECRET=
NEXTAUTH_URL=

# API Keys
OPENAI_API_KEY=
```

這樣新項目開始時，就知道需要配置哪些環境變量。



### 使用 GitHub 模板倉庫

可以把你的模板項目放在 GitHub 上，設置為 `Template repository` 模板倉庫。

![](https://pic.yupi.icu/1/image-20260104215020646.png)

這樣創建新項目時，點擊 `Use this template` 就能快速復刻項目模板了：

![](https://pic.yupi.icu/1/image-20260104215101657.png)

除了自己創建模板，你還可以使用別人的模板。在 GitHub 上搜索 "react template"、"nextjs starter" 等關鍵詞，能找到很多優秀的模板項目。優先選擇 Star 數多、更新活躍的項目。

![](https://pic.yupi.icu/1/image-20260104215329685.png)

然後點擊 "Use this template" 就能基於它創建自己的項目。這樣能站在巨人的肩膀上，節省大量配置時間。



## 五、工作流自動化

把重複的操作自動化，節省時間和精力。

下面這些技巧比較專業，主要適合有編程基礎的同學。如果你是完全零基礎，可以先跳過這部分，等有需要時再回來看。



### 使用 npm scripts

npm scripts 是 Node.js 前端項目中定義和運行腳本命令的方式。簡單來說，就是把常用的命令保存在配置文件裡，需要時用一個簡短的命令就能執行。比如啟動項目、構建項目、運行測試等，都可以定義成 npm script。

可以在 `package.json` 中定義常用的腳本：

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext ts,tsx",
    "lint:fix": "eslint . --ext ts,tsx --fix",
    "format": "prettier --write \"src/**/*.{ts,tsx}\"",
    "type-check": "tsc