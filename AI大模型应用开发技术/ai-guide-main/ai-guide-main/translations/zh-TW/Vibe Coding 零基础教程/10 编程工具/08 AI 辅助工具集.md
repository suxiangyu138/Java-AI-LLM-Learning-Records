# AI 輔助工具集

你好，我是魚皮。

在前面的文章中，我們學習了各種 AI 編程工具，包括 AI 零代碼平台、AI 智能體平台、AI 代碼編輯器、命令行工具、IDE 插件。但要真正高效地開發項目，光有 AI 工具還不夠，還需要一些輔助工具來完善整個工作流程。

舉些例子，你可能會遇到這些問題：

- AI 把代碼改出問題了，怎麼還原？
- 項目做完了，怎麼部署上線讓別人訪問？
- 有沒有其他實用的工具可以提升效率？

這篇文章，我會介紹 Vibe Coding 開發中常用的輔助工具，幫你完善整個開發工具鏈。



## 一、Git 版本管理

### 為什麼需要 Git？

在開發過程中，你可能會遇到這些情況：

- 剛才改了代碼，結果改壞了，想恢復到之前的版本
- 想嘗試一個新功能，但又怕影響現有的代碼
- 多人協作時，不知道誰改了什麼

這些問題，Git 都能幫你解決。

**Git 是一個版本控制工具**，可以記錄代碼的每一次修改，隨時回退到任何歷史版本。

💡 如果你想成為一名專業的程序員，那麼必學 Git，這是企業開發的基本功。



### Git 的核心概念

Git 的工作流程很簡單，主要就三步：

1. 在工作區修改代碼
2. 添加代碼到暫存區（用 `git add` 命令）
3. 提交代碼到倉庫（用 `git commit` 命令）

打個比方，修改代碼就像在草稿紙上寫東西，添加到暫存區就像把滿意的內容挑出來，提交到倉庫就像把這些內容正式保存到筆記本裡。

![](https://pic.yupi.icu/1/gitworkflow%E5%A4%A7.jpeg)



### 怎麼使用 Git？

有 2 種使用 Git 的方式：**可視化工具** 和 **命令行**。

對於新手來說，我強烈推薦先用可視化工具。現在很多主流的開發工具（比如 Cursor、VS Code）都內置了 Git 功能，點幾下滑鼠就能完成代碼的提交和拉取，完全不需要記命令。

我自己剛開始接觸 Git 的時候就是這樣，完全沒有上網搜教程，就是看別人提交項目的時候在編輯器上點幾下就搞定了，覺得很神奇。然後我就有樣學樣地用起了這個工具。而且很長一段時間我都是用 [GitHub Desktop](https://desktop.github.com/) 來傻瓜式操作，遇到問題了再上網搜解決方案。

![GitHub Desktop APP](https://pic.yupi.icu/1/screenshot-windows-dark.png)

等你熟練了之後，可以再學習命令行操作。命令行雖然看起來複雜，但其實更靈活、更強大，而且很多高級功能只能通過命令行實現。

下面是幾個最常用的命令，學會這幾個命令，就能應付 90% 的日常開發了。

```bash
# 克隆項目
git clone https://github.com/liyupi/ai-guide.git

# 查看狀態
git status

# 添加文件
git add .

# 提交
git commit -m "添加了新功能"

# 推送到遠端
git push

# 拉取最新代碼
git pull
```

不用刻意去背，用到的時候查一下就行，或者直接問 AI。

這裡我建議大家無論是否要學習 Git，都可以先把它安裝到你的電腦中，[直接去官網安裝](https://git-scm.com/) 就好。有可能一些軟件或工具會依賴 Git，不裝的話後面會出現一些問題。



### 實際使用場景

讓我用一個實際例子來演示 Git 的用法。比如你用 Cursor 做了一個項目，想用 Git 管理版本。

1）首先在項目的根目錄執行命令來初始化 Git：

```bash
git init
```

2）然後添加所有文件並提交第一個版本：

```bash
git add .
git commit -m "初始版本"
```

3）接下來繼續開發，修改了一些代碼。修改完成後再次提交：

```bash
git add .
git commit -m "添加了用戶登錄功能"
```

4）如果某次修改出了問題，想回退到之前的版本，可以這樣：

```bash
git log  # 查看歷史，找到想回退的版本號
git reset --hard 版本號
```

Git 會幫你記錄每一次修改，隨時可以回退。



### Git 和 AI 工具的配合

現在很多 AI 工具都內置了 Git 功能。比如 Cursor 可以在編輯器裡直接提交代碼：

![](https://pic.yupi.icu/1/image-20260109110826513.png)

也可以讓 AI 幫你自動執行 Git 命令，就跟它說 “幫我用 Git 來管理項目” 就可以了。

此外，AI 還能幫你生成提交信息，一切皆可 Vibe Coding！

我建議大家養成一個習慣：**每完成一個功能，就提交一次**。

這樣即使 AI 改壞了代碼，也能隨時恢復到之前的版本。有了 Git，你就可以放心大膽地讓 AI 改代碼了，反正隨時能回退。



### 學習建議

Git 的功能很強大，但對於 Vibe Coding 來說，掌握上面這些用法就夠用了。如果想深入學習 Git 和 GitHub，可以看看魚皮寫的 [Git & GitHub 學習路線](https://www.codefather.cn/course/1789189862986850306/section/1789190804671012866)。這個學習路線從零基礎到精通，涵蓋了 Git 的所有核心知識點，而且完全免費。



## 二、部署託管平台

你用 AI 做好了項目，雖然在本地運行得很好，但如果想讓別人也能訪問，就需要把項目部署到伺服器上。

傳統的部署方式很複雜：要租伺服器、配置環境、上傳代碼、配置域名等等。但現在有很多免費的部署平台，可以讓你幾分鐘就把項目上線。



### Vercel

[Vercel](https://vercel.com) 是目前最流行的前端部署平台，特別適合 React、Next.js、Vue、靜態網站等前端項目。

優點是個人項目完全免費，部署速度極快（一般 1 ~ 2 分鐘），自動配置 HTTPS 和 CDN 加速；還和 GitHub 深度集成，能夠在你推送代碼後自動部署。

使用 Vercel 超級簡單。

1）首先訪問 [Vercel 官網](https://vercel.com) 註冊帳號，建議使用 GitHub 帳號註冊登錄。

2）創建項目，可以直接綁定 GitHub 並選擇已經託管的項目，點擊 "Deploy" 部署按鈕：

![](https://pic.yupi.icu/1/image-20260109111323983.png)

3）等待 1 ~ 2 分鐘，項目就上線了！

部署成功後 Vercel 會自動給你一個域名，比如 `your-project.vercel.app`，你也可以綁定自己的域名。

而且每次你往 GitHub 推送代碼，Vercel 都會自動重新部署，完全不用手動操作。

💡 具體過程可以看魚皮的視頻教程：https://www.bilibili.com/video/BV1TV4y1j76t



### Netlify

[Netlify](https://www.netlify.com/) 和 Vercel 類似，但功能更全面一些。支持更多的框架和靜態站點生成器，有表單處理、無伺服器函數等功能，免費額度更大、還支持 A/B 測試和分析。使用方式和 Vercel 類似，不過多介紹了。



### EdgeOne Pages 國產部署平台

[EdgeOne Pages](https://pages.edgeone.ai) 是騰訊雲推出的邊緣全棧開發平台，它基於騰訊雲 EdgeOne 基礎設施，提供從前端頁面到動態 API 的無伺服器部署體驗。


EdgeOne 是騰訊雲的邊緣安全加速平台，簡單來說就是 “網絡加速 + 安全防護” 的組合。它利用騰訊遍布全球的網絡節點，讓你的網站在離用戶更近的地方提供服務，加載速度更快。同時還集成了 Web 防護能力，可以在邊緣就過濾攔截惡意流量，保護你的網站安全。

EdgeOne Pages 基於這個強大的基礎設施，優點是國內訪問速度快，和騰訊雲服務深度集成，支持邊緣函數，有免費額度。更適合國產開發者寶寶們~



### GitHub Pages

[GitHub Pages](https://pages.github.com/) 是 GitHub 提供的免費靜態網站託管服務。優點是完全免費、無限流量、和 GitHub 無縫集成。

使用方法巨簡單，在 GitHub 創建倉庫並上傳網站文件後，直接在倉庫設置中啟用 GitHub Pages：

![](https://pic.yupi.icu/1/image-20260109111917547.png)

然後就能通過 `username.github.io/repo-name` 訪問了。適合個人主頁、項目文檔、簡單的靜態網站。



### 如何選擇？

- 如果你的項目是 Next.js，選 Vercel（官方推薦）
- 如果是其他前端框架或靜態網站，Vercel 和 Netlify 都可以
- 如果是國內用戶，想要更快的訪問速度，選 EdgeOne Pages
- 如果只是簡單的靜態頁面，GitHub Pages 最簡單

我自己主要用 Vercel + EdgeOne Pages，因為它速度快、體驗好。國內項目會用 EdgeOne Pages，訪問速度確實快很多。



### Cloudflare CDN

如果你想讓網站訪問速度更快，還可以使用 [Cloudflare](https://www.cloudflare.com/zh-cn/) 的免費 CDN 服務。

CDN（內容分發網絡）就是把你的網站內容緩存到全球各地的伺服器上，用戶訪問時會自動選擇離他最近的伺服器，大大提高加載速度。

![](https://pic.yupi.icu/1/1763643073516-5248d56c-bf7d-4537-b8f8-681a104626d9.png)

Cloudflare 的優勢是：

- 完全免費（個人網站）
- 全球 CDN 加速，覆蓋 200+ 個城市
- 自動 HTTPS 證書
- DDoS 防護和 Web 防火牆
- 免費的 DNS 服務

使用方法很簡單，註冊 Cloudflare 帳號，添加你的域名，然後把域名的 DNS 伺服器改成 Cloudflare 提供的地址就行。Cloudflare 會自動幫你加速和保護網站。

也可以直接使用 Cloudflare 提供的 Pages 頁面部署能力，直接上傳自己的代碼，交給它一鍵部署並白票免費的域名，更方便~

![](https://pic.yupi.icu/1/1763643412558-4d499b46-5e16-4f83-9df7-06a85175df35.png)

如果你的網站部署在 Vercel 或 Netlify 上，它們本身就有 CDN 加速，不需要額外配置 Cloudflare。但如果你是自己租伺服器部署，強烈建議用 Cloudflare 加速。 



### 更多部署方式

魚皮分享過多種快速部署上線項目的視頻教程：

- [Vercel 項目部署教程](https://www.bilibili.com/video/BV1TV4y1j76t)
- [雲端編輯器 + Vercel + 對象存儲 + 內網穿透 4 種方式部署](https://www.bilibili.com/video/BV1UZ4y197i1)
- [Nginx + 寶塔 2 種方式部署個人博客](https://www.bilibili.com/video/BV1rU4y1J785)
- [WordPress 搭建個人博客](https://www.bilibili.com/video/BV14q4y1R7XM)
- [4 種主流前後端項目部署](https://www.codefather.cn/course/1790943469757837313/section/1791075571845345281?contentType=video&tabKey=videoList)

此外，魚皮在 [編程導航](https://codefather.cn/) 帶大家做過 20 多套項目了，幾乎每種部署方式都給大家講解過，如果你想成為一名專業的程序員，建議學習一下。

- [AI 零代碼應用生成平台項目](https://www.codefather.cn/course/1948291549923344386)：1Panel 面板 + Nginx 前端 + Java 後端（jar 包）
- [代碼生成器共享平台項目](https://www.codefather.cn/course/1790980795074654209)：寶塔面板 + Nginx 前端 + Java 項目管理器（jar 包）後端部署
- [AI 答題應用平台項目](https://www.code-nav.cn/course/1790274408835506178)： Vercel 前端 + Docker 後端 + 雲託管 Serverless 平台部署
- [AI 超級智能體項目](https://www.codefather.cn/course/1915010091721236482)：Docker 前端 + Docker 後端 + 雲託管 Serverless 平台部署
- [OJ 在線判題項目](https://www.codefather.cn/course/1790980707917017089)：Docker Compose 後端微服務部署

基本上學會這幾種部署方式，能夠應對絕大多數部署需求了。



## 三、MCP 服務 - 擴展 AI 能力

MCP（Model Context Protocol） 是一個開放標準，可以讓 AI 工具連接到各種外部工具和數據源。

簡單來說，MCP 就像給 AI 裝上了各種 "插件"，讓它能做更多事情。比如文件系統 MCP 讓 AI 可以讀寫文件，GitHub MCP 讓 AI 可以操作 GitHub 倉庫，數據庫 MCP 讓 AI 可以查詢數據庫，瀏覽器 MCP 讓 AI 可以瀏覽網頁。

![](https://pic.yupi.icu/1/mcp.png)

現在幾乎所有主流 AI 編程工具都支持 MCP，包括 Cursor、Claude Code、Cline、Windsurf、Gemini CLI、Kiro 等。你可以在這些工具中使用各種 MCP 服務，大大擴展 AI 的能力。

下面我用 Cursor 作為例子，演示如何配置和使用 MCP。



### 在 Cursor 中使用 MCP

讓我用一個實際例子來演示如何在 Cursor 中配置和使用 MCP。

比如我希望讓 Cursor 能夠得知當前準確的時間。

1）利用 MCP 大全網站搜索你需要的 MCP 工具，並獲取到 MCP 配置信息，後面會用到：

![](https://pic.yupi.icu/1/image-20260109113038258.png)

由於這個 MCP 工具需要用到 `uvx` 命令來安裝，我們需要先安裝 uv 工具。參考 [官方安裝文檔](https://docs.astral.sh/uv/getting-started/installation/)，選擇操作系統並執行一行命令，就能完成安裝了。

![](https://pic.yupi.icu/1/image-20260109113308798.png)

安裝後，執行 `uvx` 命令，應該會看到下圖的輸出，表示安裝成功：

![](https://pic.yupi.icu/1/image-20260109113427041.png)



2）打開 Cursor 設置，找到 MCP 配置選項，點擊添加 MCP：

![](https://pic.yupi.icu/1/image-20260109113809834.png)



3）Cursor 是通過 JSON 文件來管理 MCP 的，添加前面複製的 MCP 伺服器配置：

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



4）保存後，會發現 MCP 工具已成功開啟，現在 AI 就可以獲取到最新時間了。

![](https://pic.yupi.icu/1/image-20260109113524465.png)



5）你可以問 AI：現在幾點鐘？

AI 就能通過調用 MCP，給你最準確的時間。

![](https://pic.yupi.icu/1/image-20260109113631840.png)



### 常用 MCP 服務推薦

除了前面演示的時間 MCP，還有很多實用的提升 AI 編程效率的 MCP 服務。

**網頁相關**：
- Firecrawl MCP：讓 AI 能夠自動抓取和理解網頁內容
- Brave Search MCP：注重隱私保護的網絡搜索
- Context7：獲取最新的技術文檔
- Web to MCP：復刻網頁組件
- Chrome DevTools MCP：瀏覽器調試

**代碼託管**：
- GitHub MCP：操作 GitHub 代碼倉庫

**部署相關**：
- EdgeOne Pages MCP：一鍵部署項目到騰訊雲

**文件存儲**：
- COS MCP：操作騰訊雲對象存儲

這些 MCP 服務能讓 AI 的能力邊界大大擴展，從單純的代碼生成變成能夠聯網搜索、操作文件、部署項目的全能助手。

![](https://pic.yupi.icu/1/image-20260116122211103.png)

如果你想了解更多 MCP 服務，可以訪問：

- ⭐️ [魚皮 AI 導航 - MCP 大全](https://ai.codefather.cn/mcp)：持續更新優質 MCP 服務，幫你重塑 AI 工作流
- [mcp.so](https://mcp.so/)：MCP 伺服器市場，可以找到各種 MCP 服務
- [GitHub awesome-mcp-servers](https://github.com/punkpeye/awesome-mcp-servers)：社區維護的 MCP 伺服器列表

這些網站會持續更新最新的 MCP 服務，建議收藏。



## 四、規範化開發工具

除了版本管理、部署平台和 MCP，還有一些規範化開發工具能幫你提升項目質量。

### Spec-kit 和 OpenSpec

如果你想讓 AI 按照規範化的流程來開發項目，可以了解一下 **規範驅動開發**（SDD）工具。

這些工具的核心理念是：先把需求寫成規範文檔，然後讓