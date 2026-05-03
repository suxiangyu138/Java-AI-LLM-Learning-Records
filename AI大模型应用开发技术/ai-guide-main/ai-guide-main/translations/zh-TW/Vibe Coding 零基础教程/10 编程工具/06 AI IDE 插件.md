# AI IDE 插件

> 在熟悉的編輯器中加入 AI 能力



你好，我是魚皮。

在前面的文章中，我們學習了 AI 程式碼編輯器和 AI 命令行編程工具。

但如果你有編程基礎，已經習慣用 VS Code / IntelliJ IDEA 等整合開發環境（IDE）了，不想換編輯器，又想 Vibe Coding，怎麼辦呢？

**IDE AI 插件** 就是你要找的答案。

這篇文章，我會介紹最主流的 IDE AI 插件，幫你在熟悉的編輯器中加入 AI 能力。



## 一、為什麼選擇 IDE 插件？

在了解具體插件之前，我們先來搞清楚：IDE 插件和 Cursor 有什麼區別？為什麼要用插件？

Cursor 是一個獨立的編輯器，雖然基於 VS Code，但是一個完整的軟體。而 IDE 插件是安裝在你現有編輯器（VS Code、IntelliJ IDEA 等）上的擴展，不需要換編輯器。

打個比方，Cursor 像買了一輛新車，配好了所有功能；IDE 插件像給你現在的車加裝了新功能，車還是原來那輛。

IDE 插件的優勢很明顯。首先是無需切換編輯器，如果你已經習慣了某個編輯器，配置好了各種插件和快捷鍵，不想重新適應新環境，那用插件是最好的選擇。

而且可以根據需要安裝不同的插件，自由組合，不喜歡某個插件隨時可以卸載換另一個。很多插件是開源免費的，或者可以使用自己的 API Key，成本更可控。

如果你是新手，還沒有固定的編輯器習慣，可能直接用 Cursor 會更簡單。但如果你已經是某個編輯器的老用戶，插件會是更好的選擇。



## 二、Cline 最強大的開源 AI 插件

[Cline](https://cline.bot/) 是目前功能最強大的開源 AI 編程插件，被稱為 開源版 Cursor。

Cline 最大的優勢是 **跨平台支持**，不僅支持 VS Code，還支持 JetBrains 系列的 IntelliJ IDEA、PyCharm、WebStorm 等多個編輯器。

![](https://pic.yupi.icu/1/image-20260108222935455.png)

它完全開源免費，支持 Claude、GPT、Gemini、DeepSeek 等各種大模型，還可以部署 MCP 服務擴展功能。不僅能對話生成程式碼，還能自主執行命令、修改多個檔案、使用瀏覽器，總之功能非常全面。

下面來演示一下 Cline 的使用流程。



### VS Code 中使用 Cline

比如我想用 Cline 在 VS Code 中創建一個 React 項目。

1）在 VS Code 中打開擴展商店，搜索 "Cline"，點擊安裝。

![](https://pic.yupi.icu/1/image-20260108223139213.png)

2）安裝後，點擊側邊欄的 Cline 圖標，可以直接免費使用，也可以使用你自己的大模型 API Key。

![](https://pic.yupi.icu/1/image-20260108223220642.png)

3）點擊下一步後，Cline 會引導你創建一個帳號，使用 GitHub 或郵箱註冊登錄就好。

4）搞定帳號後，就可以愉快使用了，直接在 Cline 面板中輸入需求：

```
創建一個 React + TypeScript 項目，包含：
- 首頁
- 關於頁面
- 導航欄
- 使用 React Router
```

![](https://pic.yupi.icu/1/image-20260108223531152.png)

5）接下來 Cline 會自動運行命令、安裝必要的依賴、創建各個組件檔案、配置路由、修改樣式。整個過程你只需要確認每一步操作，或者直接讓它全自動執行。

![](https://pic.yupi.icu/1/image-20260108223742056.png)



### JetBrains 中使用 Cline

如果你是 JetBrains IDE 的用戶，在 IDE 中打開 Settings → Plugins，搜索 "Cline"，安裝即可。使用方式和 VS Code 版本完全一樣。

![](https://pic.yupi.icu/1/image-20260108224135571.png)



## 三、AI 編程助手 IDE 插件

除了 Cline，還有一些其他的 AI 編程助手 IDE 插件也值得了解。



### Claude Code 官方擴展

Claude Code 是 Anthropic 推出的 AI 編程助手，原本是獨立的命令行工具。而 [Claude Code VS Code 擴展](https://www.anthropic.com/news/enabling-claude-code-to-work-more-autonomously) 能讓你在程式碼編輯器中直接使用 Claude Code，不用額外打開終端。

這個擴展的優點是提供了圖形界面，你可以通過側邊欄面板和 Claude 對話，能夠靈活輸入文字。

![](https://pic.yupi.icu/1/image-20260116124614180.png)

當 AI 修改程式碼時，你能在編輯器裡實時看到變化，並且自動顯示 diff 對比，讓你清楚地知道 AI 改了哪些地方。

![](https://pic.yupi.icu/1/image-20260116124700221-20260118135011240.png)

我經常用它來重構程式碼、修復 Bug、添加新功能。它還支持多會話並行，也就是說你可以同時讓多個 Claude 代理處理不同的任務，比如一個負責前端，一個負責後端，大大提高開發效率。

![](https://pic.yupi.icu/1/image-20260116124928547.png)



### GitHub Copilot

[GitHub Copilot](https://github.com/features/copilot) 是最成熟的 AI 編程助手，支持 VS Code、JetBrains 全系列、Vim、Neovim 等多個編輯器。

主要功能是程式碼補全，當你寫程式碼時會自動提示下一行要寫什麼。還有 Copilot Chat 功能，可以在側邊欄和 AI 對話。

![](https://pic.yupi.icu/1/image-20260108225417720.png)

它的優點是成熟穩定、程式碼補全質量很高、跨平台支持。最關鍵的是，學生和開源貢獻者可以免費使用。



### JetBrains AI Assistant

[JetBrains AI Assistant](https://www.jetbrains.com/ai-assistant/) 是 JetBrains 官方推出的 AI 編程助手，專門為 JetBrains IDE 優化，剛出的時候魚皮還在阿里雲棲大會現場給大家做過這個的分享哈哈。

![](https://pic.yupi.icu/1/image-20260108230013824.png)

它不僅有程式碼補全，還能生成測試、解釋程式碼、重構程式碼、生成文件等。而且和 IDE 的各種功能深度集成，比如調試、重構、測試、生成提交信息等。

![](https://pic.yupi.icu/1/image-20260108225718180.png)

優勢是官方出品，和 IDE 集成最好，支持多種 AI 模型，功能全面。缺點是需要訂閱 JetBrains 的付費計劃。



### Continue

[Continue](https://www.continue.dev/) 是開源的 AI 編程插件，功能和 Cline 類似但更輕量。支持多種 AI 模型，有程式碼補全、對話、程式碼編輯等功能，界面比較簡潔，上手容易。完全免費，支持 VS Code 和 JetBrains。

![](https://pic.yupi.icu/1/image-20260108230116299.png)



### Amazon Q Developer

[Amazon Q Developer](https://aws.amazon.com/q/developer/)（原名 CodeWhisperer）是亞馬遜推出的 AI 編程助手。

特點是與 AWS 服務深度集成、支持多種 IDE（VS Code、JetBrains 等）、有免費版本、程式碼安全掃描。適合使用 AWS 服務的開發者、需要程式碼安全掃描的團隊。



## 四、IDE 擴展插件

除了 AI 編程助手插件，還有一些實用的 IDE 擴展插件。

這些插件雖然不是 AI 工具，但配合 AI 編程使用，能讓你的開發效率更上一層樓。



### GitLens

GitLens 能讓你更直觀地查看 Git 程式碼的修改歷史，把鼠標放到任意程式碼行上就能看到這行程式碼的作者、提交時間等信息。

![](https://pic.yupi.icu/1/image-20260116125445257.png)



### Office Viewer

Office Viewer 能在編輯器裡直接預覽和編輯各種文件，包括 Markdown、Excel、Word、PDF 等，不用來回切換窗口。

![](https://pic.yupi.icu/1/image-20260116130527681.png)



### ESLint 和 Prettier

ESLint 是程式碼質量檢查工具，Prettier 是程式碼格式化工具。這兩個插件能幫你保持程式碼規範，避免 AI 生成的程式碼出現格式問題。

![](https://pic.yupi.icu/1/image-20260116131356553.png)



### Error Lens

Error Lens 能讓錯誤信息直接高亮顯示在程式碼行尾，一眼就能看到哪裡有問題。

![](https://pic.yupi.icu/1/image-20260116140619858.png)



### Console Ninja

Console Ninja 能讓你在編輯器裡直接看到程式碼的運行結果，不用頻繁切換到瀏覽器控制台。

![](https://pic.yupi.icu/1/image-20260116141109420.png)



### Supermaven

[Supermaven](https://supermaven.com/) 是一個專注於程式碼補全的插件，最大的特點是 100 萬 Token 的上下文窗口，補全速度極快，準確度也很高。

![](https://pic.yupi.icu/1/image-20260108230146505.png)





## 五、怎麼選擇 AI IDE 插件？

- 如果想要最強大的功能（智能體、多文件編輯），選 Cline。它支持 VS Code 和 JetBrains，完全免費，功能接近 Cursor。
- 如果主要需要程式碼補全，選 GitHub Copilot。它最成熟穩定，程式碼補全質量最高，而且跨平台支持。
- 如果你已經訂閱了 JetBrains，直接用 JetBrains AI Assistant，因為它和 IDE 的集成最好。
- 如果想要輕量級的工具，選 Continue。

我現在用 IDE 插件的頻率不是很高，之前主要用 Cline（功能全面 + 免費）、GitHub Copilot（程式碼補全質量高）還有一些國產的 AI 插件，比如智譜 CodeGeex、通義靈碼之類的。



## 寫在最後

到目前為止，魚皮已經把主流的 AI 編程工具介紹完了，建議大家都體驗一下，選擇適合自己的才是最好的。

在下一篇文章中，我會介紹輔助工具集，幫你完善整個開發工具鏈。

加油！




## 推薦資源

1）魚皮 AI 導航網站：[AI 資源大全、最新 AI 資訊、免費 AI 教程](https://ai.codefather.cn)

2）編程導航學習圈：[學習路線、編程教程、實戰項目、求職寶典、交流答疑](https://www.codefather.cn)

3）程序員面試八股文：[實習/校招/社招高頻考點、企業真題解析](https://www.mianshiya.com)

4）程序員寫簡歷神器：[專業模板、豐富例句、直通面試](https://www.laoyujianli.com)

5）1 對 1 模擬面試：[實習/校招/社招面試拿 Offer 必備](https://ai.mianshiya.com)