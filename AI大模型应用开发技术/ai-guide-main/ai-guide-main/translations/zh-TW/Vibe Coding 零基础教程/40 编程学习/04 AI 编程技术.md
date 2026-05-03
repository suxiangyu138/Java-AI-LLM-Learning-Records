# AI 程式設計技術入門指南

> 掌握 AI 開發框架，成為企業招聘的香餑餑



你好，我是程式設計師魚皮。

作為程式設計師，我們不光要會用 AI 工具、能利用 AI 開發專案，還要能夠自主開發 AI 專案，把 AI 的能力接入到自己的專案中。

有句話說得好：**AI 時代，所有的傳統業務都值得利用 AI 重塑一遍。**

所以現在很多公司都在招能夠開發 AI 專案的程式設計師，這也是我們的機會。那麼我們要學習哪些知識和技術，才能成為企業招聘的香餑餑呢？

⭐️ 推薦觀看影片版：https://www.bilibili.com/video/BV1i9Z8YhEja



## 一、AI 開發框架

首先從技術角度出發，我們要學習主流的 AI 開發框架。目前 Java 方向最火的就是 **Spring AI** 和 **LangChain4j**。


### Spring AI

[Spring AI](https://docs.spring.io/spring-ai/reference/getting-started.html) 是 Spring 官方推出的 AI 開發框架，經過 2 年的沉澱，在 2025 年 5 月正式發布了 1.0 版本。

![Spring AI 1.0 發布](https://pic.yupi.icu/1/1747881171718-91ac3eb5-049b-4510-8012-6736c40c9c95.png)

Spring AI 的優勢在於 **更容易和主流 Java 開發框架 Spring 整合**，上手難度較低。它提供了很多現成的方法來幫我們提高開發 AI 應用的效率，比如快速對接大模型、保存會話上下文、對接向量資料庫實現 RAG 等等。

![Spring AI 架構](https://pic.yupi.icu/1/1743563460857-95800757-867c-4e8a-ba7c-dd490d09fcbd.png)

Spring AI 的核心特性包括：

- 大模型呼叫能力：統一介面支援多種主流大模型（OpenAI、Claude、通義千問等）
- 提示工程：提供 Prompt 和 PromptTemplate 類，方便管理提示詞
- 會話記憶：一行程式碼開啟對話記憶，自動處理上下文
- RAG 檢索增強生成：完整的 RAG 流程支援，包括文件載入、向量儲存、檢索優化
- 工具呼叫：通過註解快速定義工具，讓 AI 呼叫外部服務
- MCP 支援：輕鬆接入和開發 MCP 服務

舉個例子，使用 Spring AI 呼叫大模型，只需要幾行程式碼：

```java
// 使用 Spring AI 呼叫大模型
@Bean
public ChatClient chatClient(ChatModel chatModel) {
    return ChatClient.builder(chatModel).build();
}

public String doChat(String message) {
    return chatClient.prompt(message).call().content();
}
```

如果不使用 Spring AI，你就需要自己編寫 HTTP 請求、解析響應，麻煩很多。



### Spring AI Alibaba

[Spring AI Alibaba](https://java2ai.com/) 是阿里巴巴基於 Spring AI 推出的國內版本，專門針對國內的 AI 生態做了優化。

它的優勢在於：

- 更好地支援國內的大模型（通義千問、百度文心一言等）
- 提供了中文文件和技術支援
- 針對國內網路環境做了優化
- 有阿里雲的生態支援

如果你主要使用國內的 AI 服務，Spring AI Alibaba 會是更好的選擇。



### LangChain4j

[LangChain4j](https://docs.langchain4j.dev/intro) 是另一個主流的 Java AI 開發框架，它的特點是 **更靈活，更適合開發複雜的智慧體**。

比如在開發一個智慧文件分析系統時，利用 LangChain4j，智慧體能夠自動讀取文件內容，呼叫搜尋引擎獲取相關背景知識，然後根據任務需求，將文件資訊與外部知識結合，生成分析報告。

LangChain4j 的核心特性包括：

- AI Service：宣告式開發，通過註解快速構建 AI 服務
- 會話記憶：支援多種會話記憶策略和持久化
- 結構化輸出：自動將 AI 輸出轉換為 Java 物件
- RAG 支援：完整的 RAG 流程，支援多種向量資料庫
- 工具呼叫：靈活的工具定義和呼叫機制
- 護軌機制：輸入輸出攔截器，增強安全性

舉個例子，使用 LangChain4j 的 AI Service：

```java
@AiService
public interface AiCodeHelperService {
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String userMessage);
}
```

只需要定義介面和註解，框架會自動生成實現類，非常方便。



### 如何選擇框架？

| 場景          | 推薦框架              | 優勢                     |
| ------------- | --------------------- | ------------------------ |
| Java 企業應用 | Spring AI             | 無縫整合 Spring 生態     |
| 國內 AI 服務  | Spring AI Alibaba     | 更好支援國內大模型       |
| 智慧體開發    | LangChain4j           | 完整 Agent 工具鏈        |
| 複雜工作流    | LangGraph（進階）     | 視覺化編排               |

我的建議是，**兩個都要學，先從 Spring AI 學起，再學 LangChain4j 會更簡單**。很多概念和用法是相通的，學會一個，另一個也能快速上手。



## 二、AI 整合

開發 AI 應用的前提是要有大模型，但是大模型要消耗算力才能運行，算力就是金錢。從哪兒搞來大模型呢？

有 2 種方法：使用 AI 雲服務、或者本地部署大模型。



### AI 雲服務

AI 雲服務就是其他企業為我們部署了 AI 大模型，通過 API 介面的方式提供給我們使用，按量計費。

比如阿里雲百煉、火山引擎、矽基流動、OpenAI 等等。

![AI 雲服務](https://pic.yupi.icu/1/1743563631799-46ff94d5-d51b-4dc5-b6cf-dec28bdcdb39.png)

我們程式設計師需要重點掌握的是：

1. 如何通過 API 接入雲服務？
2. 如何使用 AI 雲服務來建立智慧體和配置參數？
3. 如何選擇合適的雲服務？這就需要關注各家雲服務的計費模式和服務質量
4. 如何更低成本、更穩定地使用雲服務？這就需要我們學習 Prompt 工程和高可用技術



### 本地部署大模型

本地部署大模型對於很多企業來說也是剛需，資料無需上傳至雲端，能夠有效保障資料的安全性和隱私性，尤其適用於醫療、金融等對資料安全極為敏感的行業。

本地部署大模型其實並不難，只需要使用 [Ollama 工具](https://ollama.com/) 就可以一鍵部署各種主流的開源模型。

![Ollama](https://pic.yupi.icu/1/1743563719547-bbed1c54-d1f1-496f-afc2-d755c3538732.png)

唉，但部署大模型的難度不在於技術啊，主要是沒算力啊！不然我也給我們團隊的 [程式設計導航](https://codefather.cn) 和 [面試鴨](https://www.mianshiya.com) 都來一套魚皮大模型了。



## 三、AI 領域業務

企業中的 AI 業務開發，可不僅僅是來個 AI 對話就夠了，我們還要掌握幾種更複雜的業務開發，比如 RAG 知識庫、多模態、MCP 服務、ReAct 智慧體。


### RAG 知識庫

很多公司都有屬於自己的業務知識和文件，會構建自己的問答系統或客服，這就要用到 RAG 檢索增強生成技術。

先通過文字嵌入模型，將企業各種文件轉化為向量，存入向量資料庫；當用戶提問時，系統在向量資料庫檢索相關向量資料，找到最相似文件片段，和問題一起輸入大模型處理。這樣一來，大模型能夠基於企業真實資料作答，更準確貼合實際。

![RAG 流程](https://pic.yupi.icu/1/1743563751814-4123230c-c4b8-458f-bf8b-070c7550dd54.png)

關於 RAG 能學的知識可太多了，比如主流的向量資料庫 Milvus 和 PGVector、文件的抽取 / 轉換 / 載入、索引的構建、查詢策略的優化等等。**這也是 AI 企業面試的重點！**




### 多模態

多模態也是主流的 AI 業務場景，即融合文字、圖像、音頻、影片等多種不同類型的資料模態，從而提高產品使用的易用性，做出更多有創意的功能。

比如做個智慧導購系統，顧客既可以輸入文字描述想要的商品，系統也能識別顧客上傳的商品圖片，甚至可以理解顧客通過語音提出的購物需求。

![多模態](https://pic.yupi.icu/1/1743563981663-8c9f4746-03dc-4b32-8477-ba9a9042922c.png)

想開發多模態應用，我們要學習模態轉換技術，比如文字轉語音（TTS）、語音轉文字（STT）、光學字元識別（OCR）等。不過這些都有現成的工具庫或者雲服務，掌握呼叫方法就行。




### MCP 服務

MCP（Model Context Protocol，模型上下文協議）可以理解為提供給 AI 的各種服務，AI 利用這些服務能夠實現更強大的功能。

![MCP](https://pic.yupi.icu/1/1743563832927-7a2df71f-acc1-47c4-9135-e7d888749dbc.png)

如何在專案中接入別人的 MCP 服務，來增強自己的專案能力；以及如何開發自己的 MCP 服務，讓別人的專案使用，都是必須要學習的。

現在使用 Spring AI 等開發框架就可以開發 MCP 服務，而且甚至有高手做了個 [網站](http://mcpify.ai/)，能夠一句話建立自己的 MCP 服務，這真的是泰褲辣。

![MCP 生成](https://pic.yupi.icu/1/1743563865750-bbd02b74-2a56-49a9-963f-e633c1484fe5.png)



### ReAct 智慧體

ReAct 是一種構建智慧體的開發範式，目的是打造能夠依據推理結果自主採取行動的智慧體。

它的開發過程會涉及到任務規劃、工具呼叫、互動 I/O、異常處理等知識。尤其是工具呼叫，可以通過 Function Call 或 MCP 實現像天氣查詢、文件讀寫、網頁運行、資訊檢索、終端命令執行等功能。

![ReAct 智慧體](https://pic.yupi.icu/1/1743563922663-0096045d-8a99-4202-b30d-df77a341e697.png)

就拿開發影片網站為例，用戶說了 “幫我開發一個 Dilidili 影片網站並部署上線” 的指令時，智慧體首先會深入理解任務內容，通過推理梳理出一系列執行步驟，包括明確需求、設計方案、搭建框架、生成程式碼、部署上線等。接下來，智慧體就會呼叫相應的工具來執行這些行動。

![智慧體工作流](https://pic.yupi.icu/1/1743564028474-638e6414-9a22-4350-80f3-7bf174dd0f77.png)



## 四、AI 工具鏈

最後就是我們開發 AI 專案時可能會用到的一些平台、工具和類庫了。




### 低程式碼平台

比如主流的低程式碼 AI 開發平台 [Dify](https://dify.ai/)，可以讓我們通過拖拉拽的方式構建自己的 AI 智慧體，建立知識庫並匯入自己的文件，搭建複雜的工作流等等。就哪怕你不會寫程式碼，都能用它搞出複雜的 AI 應用。

![Dify](https://pic.yupi.icu/1/1743564064922-03f6365b-a712-47d9-be55-4867b848a269.png)



### 工具庫

還有一些開發 AI 智慧體時會用到的工具庫，比如：

- Apache Tika：功能強大的文件解析器工具庫，支援解析 PDF、Word、Excel、PowerPoint 等各種文件
- Playwright：用於模擬瀏覽器行為的工具庫，AI 需要運行網頁、抓取網頁資料、自動化測試時都能派上用場
- JSON 格式解析庫：GSON 和 Kryo
- HTML 文件解析庫：jsoup

這些類庫基本沒什麼學習成本，要用的時候看文件就好了。



### 部署工具

專案最終是要部署上線的嘛，所以我們還要掌握高效的部署工具。如果是個人學習使用、想快速上線自己的 AI 小應用，可以試試下面這些平台：

- [Vercel](https://vercel.com/)：適合前端應用的部署平台，支援自動構建、線上瀏覽、CDN 分發，而且還免費提供可訪問的域名
- [Sealos](https://sealos.io/)：雲原生應用管理平台，支援 Kubernetes 集群管理
- [Railway](https://railway.com/)：能讓開發人員輕鬆部署 Docker 容器，無需操心伺服器配置與維運

當然，想快速部署服務，Docker 容器化技術也是必須要學習的，就像 APP 的安裝包一樣，能夠輕鬆分發和部署你的應用程式。

![Docker](https://pic.yupi.icu/1/1743564338228-ffc55f7b-7bcd-40df-a10b-4accfb666379.png)



## 五、學習資源推薦

怎麼樣，要學的東西還是挺多的吧。別擔心，我也在持續學習這些內容並且會持續分享給大家。


### 1、AI 學習路線

完整的 AI 大模型應用開發學習路線可以在 [程式設計導航獲取](https://www.codefather.cn/course/1789189862986850306/section/1912024009574629377)：

網址：https://www.codefather.cn/learn

![AI 學習路線](https://pic.yupi.icu/1/image-20250912152042459.png)


### 2、AI 專案實戰

在 [程式設計導航](https://www.codefather.cn) 上，我帶大家做了多套 AI 專案教程，涵蓋了上面提到的幾乎所有技術：

- AI 程式設計助手：LangChain4j 框架入門，實戰對話記憶、結構化輸出、RAG、工具呼叫、MCP、SSE 等
- AI 超級智慧體：Spring AI 框架入門，實戰 AI 戀愛大師應用 + 自主規劃能力的超級智慧體
- AI 零程式碼應用生成平台：LangChain4j AI 智慧體、LangGraph4j 工作流、微服務架構
- AI 答題應用平台：React 跨端小程序、Vue3 AI 應用、分庫分表、SSE 即時推送

這些專案都是企業級的真實專案，做完後可以直接寫進履歷。



### 3、開源專案

我也開源了不少 AI 應用開發專案，分享給大家：

- AI 應用生成平台：https://github.com/liyupi/yu-ai-code-mother
- AI 超級智慧體：https://github.com/liyupi/yu-ai-agent
- AI 文獻閱讀助手：https://github.com/liyupi/literature-assistant
- AI 知識庫：https://github.com/liyupi/ai-guide



### 4、AI 知識庫

在我免費開源的 [AI 知識庫](https://github.com/liyupi/ai-guide) 中，匯總收集了最新最全的 AI 知識，幫助大家更好地適應 AI 時代的到來。

網址：https://ai.codefather.cn

![](https://pic.yupi.icu/1/image-20260109121412266.png)

裡面除了各種教程資料外，也重點給大家分享了很多 AI 工具的具體應用場景，比如接入辦公軟體提升效率、幫你做自媒體、AI 批量製作影片等，希望幫助大家舉一反三，找到新的思路。



## 寫在最後

AI 技術發展日新月異，對程式設計師的要求也在不斷提高。**AI 相關知識不再只是演算法工程師需要了解，而是每個程式設計師都必須掌握的基本技能。**

無論你是前端、後端還是全棧開發者，都需要了解 AI 應用開發的基本概念和實踐方法。

因為未來的軟體開發，AI 將無處不在。

如果你問我 AI 會淘汰程式設計師麼？

我的答案仍然是 “會”。因為程式設計師本身就是需要持續學習和實踐來保持競爭力的，只要大家能夠學會我提到的這些知識，多關注 AI 的前沿資訊，相信 AI 不會搶走我們程式設計師的飯碗，而是成為我們改造世界的槓桿。




## 推薦資源

1）魚皮 AI 導航網站：[AI 資源大全、最新 AI 資訊、免費 AI 教程](https://ai.codefather.cn)

2）程式設計導航學習圈：[學習路線、程式設計教程、實戰專案、求職寶典、交流答疑](https://www.codefather.cn)

3）程式設計師面試八股文：[實習/校招/社招高頻考點、企業真題解析](https://www.mianshiya.com)

4）程式設計師寫履歷神器：[專業模板、豐富例句、直通面試](https://www.laoyujianli.com)

5）1 對 1 模擬面試：[實習/校招/社招面試拿 Offer 必備](https://ai.mianshiya.com)