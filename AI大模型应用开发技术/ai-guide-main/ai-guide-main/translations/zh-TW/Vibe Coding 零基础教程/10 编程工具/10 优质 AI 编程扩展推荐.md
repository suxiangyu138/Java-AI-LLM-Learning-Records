# 優質 AI 程式設計擴充推薦

大家好，我是程式設計師魚皮。給大家分享一些我自己在用的 AI 程式設計擴充，幫你大幅提高 AI 程式設計效率和程式碼品質。

**萬字長文 + 100 多張圖**，絕對乾貨！點個收藏，讓我們開始吧~


## 一、MCP 伺服器類

MCP 的全稱是 Model Context Protocol 模型上下文協議。簡單來說，就是讓 AI 大模型能夠連接外部工具和資料來源的一個開放標準。

打個比方，MCP 就像是 AI 的 USB-C 接口，原本 AI 只能根據訓練資料來回答問題、生成程式碼，但有了這個統一接口，它就能連接各種外部工具，比如打開瀏覽器看網站、搜尋並抓取網頁內容、部署專案到雲端、存取資料庫等等，能力一下子就豐富起來了。

![](https://pic.yupi.icu/1/%E6%BC%AB%E7%94%BB%E5%9B%BE1%E5%A4%A7.jpeg)



### ⭐️ Firecrawl MCP 網頁內容抓取

首先要介紹的是 [Firecrawl MCP](https://www.firecrawl.dev/)，讓 AI 能夠自動抓取和理解網頁內容。

我在開發專案時經常需要從網上獲取參考資料、閱讀官方文件和技術部落格，或者分析競品的功能實現。如果人工來做這件事，需要先打開網站、再手動複製貼上內容，或者自己寫個爬蟲腳本，麻煩得一批。

有了 Firecrawl MCP，這事兒就簡單多了。我直接在 AI 程式設計工具中跟 AI 說：

- 幫我獲取這個網站的內容
- 幫我讀一下這個文件
- 幫我從網上搜尋 XX 相關的資訊

它就能自動把網頁的內容、結構、甚至是動態載入的資料都給我抓下來。

![](https://pic.yupi.icu/1/image-20260116105912027.png)



**如何使用？**

首先你需要在 [Firecrawl 官網](https://www.firecrawl.dev/app/api-keys) 註冊帳號，並建立一個呼叫服務的 API Key。

![](https://pic.yupi.icu/1/image-20260116105955795.png)

然後進入到 AI 程式設計工具中配置一下 MCP 伺服器。這裡我以 Cursor 為例，其他 AI 程式設計工具對接 MCP 的方法可以看各自的官方文件，比如 [Claude Code 接入 MCP 文件](https://docs.anthropic.com/en/docs/claude-code/mcp)。

打開 Cursor 設定，找到 Tools & MCP，點擊 `+ New MCP Server`。

![](https://pic.yupi.icu/1/image-20260116110425690.png)

本質上就是修改 MCP 配置檔案，添加這樣的配置：

```json
{
  "mcpServers": {
    "firecrawl-mcp": {
      "command": "npx",
      "args": ["-y", "firecrawl-mcp"],
      "env": {
        "FIRECRAWL_API_KEY": "你的API密鑰"
      }
    }
  }
}
```

![](https://pic.yupi.icu/1/image-20260116110454499.png)

這段配置的意思是：透過 npx 命令來執行 firecrawl-mcp 這個工具，並且把你的 API 密鑰傳給它。如果你電腦上還沒有安裝 npx，需要先 [到官網安裝 Node.js](https://nodejs.org/zh-cn)，npx 會隨著 Node.js 一起安裝。

配置好之後，看到綠色的成功點點，表示能夠正常使用了。

![](https://pic.yupi.icu/1/image-20260116110558199.png)

除了基礎的網頁抓取，Firecrawl MCP 還支援批次抓取整站內容、遞迴抓取網站的多層連結、失敗自動重試等高級功能。



### Brave Search MCP 隱私搜尋

接下來是 [Brave Search MCP](https://github.com/brave/brave-search-mcp-server)，讓 AI 能夠進行注重隱私保護的網路搜尋。

在開發過程中，我經常需要讓 AI 幫我搜尋最新的技術資料、查找某個庫的使用範例、或者了解某個技術問題的解決方案。傳統的做法是自己去搜尋引擎查，然後把結果複製給 AI，比較麻煩。

有了 Brave Search MCP，我直接跟 AI 說：

- 幫我搜尋一下 React 19 的新特性
- 查一下這個錯誤怎麼解決

它就能透過 Brave 搜尋引擎去找答案。而且 Brave 搜尋不會追蹤你的搜尋記錄，隱私保護做得很好。

![](https://pic.yupi.icu/1/image-20260116111803869.png)



**如何使用？**

首先去 [Brave Search API](https://brave.com/search/api/) 註冊帳號，然後進入 API Key 管理頁面，首先要選擇一個訂閱計劃。必須選擇免費版啊！每月有 2000 次查詢額度，對於個人開發來說夠用了。

![](https://pic.yupi.icu/1/image-20260116110947801.png)

但這裡比較坑的一點是，即使訂閱免費版，也要填寫付款方式，沒有海外銀行卡的朋友可以撤了。

訂閱成功後，建立 API Key：

![](https://pic.yupi.icu/1/image-20260116111311536.png)

拿到 API Key 後，在 Cursor 的 MCP 配置中添加：

```json
{
  "mcpServers": {
    "brave-search": {
      "command": "npx",
      "args": ["-y", "brave-search-mcp"],
      "env": {
        "BRAVE_API_KEY": "你的API密鑰"
      }
    }
  }
}
```

配置好後，AI 就能隨時幫你搜尋最新資訊了。

支援網頁、圖片、影片、新聞等多種類型的內容搜尋，甚至能搜尋本地商家資訊（比如附近的咖啡店）。

![](https://pic.yupi.icu/1/image-20260116111954334.png)

它還帶有 AI 摘要功能，能把搜尋結果自動總結成簡潔的答案。



### ⭐️ Context7 獲取最新文件

[Context7](https://context7.com/) 能幫 AI 獲取到最新的技術文件。

我們都知道，AI 的訓練資料是有截止時間的，比如 GPT-4 的知識可能只更新到 2023 年。這就導致一個問題，當你問 AI 關於某個框架最新版本的用法時，它給出的答案可能是過時的。

Context7 就是來解決這個問題的。它會自動從官方文件網站抓取最新的、特定版本的文件內容，然後提供給 AI。

![](https://pic.yupi.icu/1/image-20260116112229490.png)

這樣一來，AI 給出的程式碼範例和建議就是基於最新文件的，不會去用已經廢棄的寫法，大大提高了專案能正常執行的概率。



**如何使用？**

訪問 [Context7 Dashboard](https://context7.com/dashboard) 註冊帳號並獲取 API Key，個人使用是免費的。

![](https://pic.yupi.icu/1/image-20260116112322940.png)

然後在 MCP 配置中添加：

```json
{
  "mcpServers": {
    "context7": {
      "url": "https://mcp.context7.com/mcp",
      "headers": {
        "CONTEXT7_API_KEY": "你的API密鑰"
      }
    }
  }
}
```

之後你在 AI 程式設計工具中跟 AI 對話時，只要跟技術文件相關，或者主動提一嘴 "use context7"，它就會自動去獲取最新文件來回覆你。

![](https://pic.yupi.icu/1/image-20260116112656483.png)



### Web to MCP 復刻網頁元件

[Web to MCP](https://web-to-mcp.com/) 是一個 Chrome 擴充，搭配 MCP 使用，能把網頁上的任何 UI 元件直接發送給 AI，讓 AI 生成對應的程式碼，用最快的速度抄作業！

![](https://pic.yupi.icu/1/image-20260116113052973.png)

很多時候，我在瀏覽網站時看到一個不錯的 UI 元件，想讓 AI 幫我實現類似的效果。以前的做法是截圖，然後跟 AI 描述：「幫我做一個類似這樣的按鈕，圓角、漸變色、帶陰影……」 既費時又不準確。

有了 Web to MCP，我只需要在網頁上點擊某個想復刻的元素：

![](https://pic.yupi.icu/1/image-20260116113725321.png)

它就會自動捕獲元件的 DOM 結構、CSS 樣式、甚至是互動效果，並且給你一個讓 AI 復刻元件的提示詞。

你只需要把提示詞發送給 AI，AI 會呼叫 MCP 拿到完整的元件資訊，並生成程式碼來復刻元件。

![](https://pic.yupi.icu/1/image-20260116114142631.png)

相比於直接給 AI 模糊的截圖，生成的程式碼更準確了。

![](https://pic.yupi.icu/1/image-20260116114426822.png)



**如何使用？**

1）透過官網或者在 Chrome 應用商店搜尋 Web to MCP 來安裝擴充

![](https://pic.yupi.icu/1/image-20260116113138693.png)

2）用 Google 帳號登入，獲取你的 MCP 配置：

![](https://pic.yupi.icu/1/image-20260116113241575.png)

3）在 AI 程式設計工具的 MCP 配置中添加：

```json
{
  "mcpServers": {
    "web-to-mcp": {
      "url": "https://web-to-mcp.com/mcp/你的唯一ID"
    }
  }
}
```

之後瀏覽網頁時，點擊擴充圖標，選中你想要的元件，就能直接在 AI 程式設計工具裡引用它，並且快速生成風格一致的程式碼了。



### Chrome DevTools MCP 瀏覽器除錯

[Chrome DevTools MCP](https://github.com/ChromeDevTools/chrome-devtools-mcp) 是 Chrome 官方團隊開發的 MCP 伺服器，讓 AI 能夠直接控制 Chrome 瀏覽器進行操作和除錯。

在做前端開發時，我經常需要除錯頁面、查看網路請求、分析效能問題。以前這些都得手動在瀏覽器的開發者工具裡操作，現在有了這個工具，我可以直接讓 AI 幫我做這些事。

比如我跟 AI 說：「幫我分析當前這個網站載入慢的原因」，它就能打開 Chrome DevTools，分析網路請求、查看資源載入時間，然後告訴我哪裡有問題。

![](https://pic.yupi.icu/1/image-20260116115138719.png)

或者我說：「幫我測試一下這個表單提交功能」，它就能自動填寫表單、點擊提交按鈕、查看請求回應。

**如何使用？**

在 MCP 配置中添加：

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

配置好後，AI 就能幫你自動化測試、除錯頁面了。工具會自動連接到你正在執行的 Chrome 瀏覽器，無需額外設定。

這個工具還支援元素定位、網路請求監控、效能分析、頁面截圖等功能，非常適合前端開發和測試。



### EdgeOne Pages MCP 一鍵部署

[EdgeOne Pages MCP](https://github.com/TencentEdgeOne/edgeone-pages-mcp) 是騰訊雲團隊開發的部署工具，能把你的專案一鍵部署到騰訊雲的加速網路，讓別人能訪問你的網站，並且給你的網站提速。

開發完專案後，你一定會想讓別人訪問你的網站。傳統的部署流程很繁瑣，需要人工打包程式碼、上傳程式碼到伺服器、配置網域、設定 HTTPS 安全憑證，一套流程下來得花不少時間。

![](https://pic.yupi.icu/1/%E6%BC%AB%E7%94%BB%E5%9B%BE2%E5%A4%A7.jpeg)

有了 EdgeOne Pages MCP，我直接在 AI 程式設計工具裡跟 AI 說：「幫我部署這個專案」，它就能自動完成打包、上傳、部署的全過程，最後給我一個可以直接訪問的 URL。而且部署到全球加速網路，各地訪問速度都很快。

![](https://pic.yupi.icu/1/1752212029384-16cfba8f-babb-49c0-9d41-3b76ee78eecf.png)



**如何使用？**

首先到 [EdgeOne 控制台](https://console.cloud.tencent.com/edgeone/pages) 開通 Pages 服務：

![](https://pic.yupi.icu/1/1752209404627-3a3193d9-4c94-4f80-ad02-435ff22f16ed.png)

然後獲取 API Token，作為呼叫服務的憑證：

![](https://pic.yupi.icu/1/1752209504079-6741bbd0-438e-48a4-86be-6eddfc3efa83.png)

在 MCP 配置中添加：

```json
{
  "mcpServers": {
    "edgeone-pages-mcp-server": {
      "command": "npx",
      "args": ["edgeone-pages-mcp"],
      "env": {
        "EDGEONE_PAGES_API_TOKEN": "你的API Token"
      }
    }
  }
}
```

配置好後，就能讓 AI 幫你一鍵部署專案了。部署是免費的，支援靜態網站、全棧專案、自動配置 HTTPS 和 CDN 加速等功能，非常適合個人專案和小型應用。

![](https://pic.yupi.icu/1/1752211869664-b74dc4d9-57b1-4bb7-9f02-703112b613eb.png)



### COS MCP 物件儲存

[COS MCP](https://github.com/Tencent/cos-mcp) 能讓 AI 直接操作騰訊雲物件儲存。

物件儲存就是雲端的檔案儲存服務，可以理解為雲端硬碟。

![](https://pic.yupi.icu/1/image-20260116120009365.png)

在團隊協作開發中，我們經常需要讓 AI 參考一些專案規範文件、或者引用一些圖片。以前的做法是把這些檔案放在本地，然後手動上傳給 AI，既不方便，也不利於團隊維護、修改和共享。

有了 COS MCP，我可以說一句話把這些需要共享的檔案存到雲端，然後讓 AI 直接去讀取。

![](https://pic.yupi.icu/1/image-20260116122211103.png)

比如我跟 AI 說：「按照我們團隊 COS 共享的專案規範文件來寫這個功能」，它就能自動從 COS 裡讀取規範文件，然後按照規範來寫程式碼。

![](https://pic.yupi.icu/1/image-20260116122453843.png)



**如何使用？**

1）首先需要開通騰訊雲 COS 物件儲存服務。訪問 [騰訊雲 COS 控制台](https://console.cloud.tencent.com/cos)，建立一個儲存桶（Bucket）：

![](https://pic.yupi.icu/1/image-20260116120711822.png)

2）然後在 "存取管理" > "API 密鑰管理" 中獲取 SecretId 和 SecretKey，注意一定不要洩露這些資訊！

![](https://pic.yupi.icu/1/image-20260116121022222.png)

3）在 MCP 配置中添加：

```json
{
  "mcpServers": {
    "cos-mcp": {
      "command": "npx",
      "args": [
        "cos-mcp",
        "--Region=你的地域",
        "--Bucket=你的儲存桶",
        "--SecretId=你的SecretId",
        "--SecretKey=你的SecretKey"
      ]
    }
  }
}
```

![](https://pic.yupi.icu/1/image-20260116121243549.png)

配置好後，AI 就能讀取和管理你雲端的檔案了，相當於給了 AI 一個網路硬碟。

此外，這個工具還支援圖片搜尋、圖片處理、文件轉換、影片封面生成等功能。

![](https://pic.yupi.icu/1/image-20260116121658701.png)




### GitHub MCP 程式碼倉庫管理

[GitHub MCP](https://github.com/github/github-mcp-server) 是 GitHub 官方開發的 MCP 伺服器，讓 AI 能夠直接操作 GitHub 程式碼倉庫。

程式設計師朋友們對 GitHub 肯定不陌生，這是全球最大的程式碼託管平台，可以用它來儲存程式碼、團隊協作開發。

![](https://pic.yupi.icu/1/image-20260116122621938.png)

在日常開發中，我可能需要搜尋 GitHub 程式碼倉庫、建立 Issue 問題回饋、提交 PR 程式碼合併請求、查看程式碼變更、分析提交歷史等等。以前這些操作都得在 GitHub 網站上手動完成，現在我可以直接讓 AI 幫我做。

比如我跟 AI 說：「我最近在 GitHub 上開源了哪些專案？star 數如何？」

![](https://pic.yupi.icu/1/image-20260116123701822.png)

它就能快速給我在 GitHub 上的專案生成一份資料報告：

![](https://pic.yupi.icu/1/image-20260116123734660.png)

或者我說：「幫我看看最近一週的程式碼變更」，它就能分析 Git 提交記錄，告訴我都改了什麼。

![](https://pic.yupi.icu/1/image-20260116124022290.png)



**如何使用？**

首先需要在 GitHub 獲取到你的 [Access Token](https://github.com/settings/tokens)，作為存取你 GitHub 資源的憑證：

![](https://pic.yupi.icu/1/image-20260116122846165.png)

