# MCP 服務開發保姆級教程

> 從零開始開發 MCP 服務



你好，我是程式設計師魚皮。

有個 AI 相關的概念特別火，叫 MCP，全稱模型上下文協議（Model Context Protocol）。這是由 Anthropic 推出的一項開放標準，目標是為大型語言模型和 AI 助手提供一個統一、標準化的接口，使 AI 能夠輕鬆操作外部工具並完成更複雜的任務。

這篇文章，就帶大家速通 MCP，了解其核心概念，並且以我們給自己產品 [面試鴨](https://www.mianshiya.com/) 開發的面試搜題 MCP 服務為例，帶大家實戰 MCP 服務端和客戶端的開發！

開源指路：https://github.com/yuyuanweb/mcp-mianshiya-server

![MCP 服務](https://pic.yupi.icu/1/1744008993525-16f07f5b-e3b4-4f1a-97dd-ea886c8d945d.png)



## 一、MCP 為啥如此重要？

以前，如果想讓 AI 處理我們的數據，基本只能靠預訓練數據或者上傳數據，既麻煩又低效。而且，就算是強大的 AI 模型，也會有數據隔離的問題，無法直接訪問新數據。

現在，MCP 解決了這個問題，它突破了模型對靜態知識庫的依賴，使其具備更強的動態交互能力，能夠像人類一樣調用搜索引擎、訪問本地文件、連接 API 服務，甚至直接操作第三方庫。

更重要的是，只要大家都遵循 MCP 這套協議，AI 就能無縫連接本地數據、互聯網資源、開發工具、生產力軟件，甚至整個社區生態，實現真正的 "萬物互聯"，這將極大提升 AI 的協作和工作能力，價值不可估量。

![MCP 架構](https://pic.yupi.icu/1/1744006127873-09bedb4a-5c7c-4cd0-9c4d-3620f319eac7.png)



## 二、MCP 總體架構

MCP 的核心是 "客戶端 - 服務器" 架構，其中 MCP 客戶端主機可以連接到多個服務器。客戶端主機是指希望通過 MCP 訪問數據的程序，比如 Claude Desktop、IDE 或 AI 工具。

![MCP 架構圖](https://pic.yupi.icu/1/1742979138403-f9f03e19-3537-461e-95d5-6f8a9a413c3a.jpeg)



## 三、MCP 開發

MCP 的使用分為兩種模式，STDIO 模式（本地運行）和 SSE 模式（遠程服務）。



### MCP 服務端（基於 stdio 標準流）

基於 stdio 的實現是最常見的 MCP 客戶端方案，它通過標準輸入輸出流與 MCP 服務器進行通信。特別適用於本地部署的 MCP 服務器。

**1、引入依賴**

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
  <version>1.0.0-M6</version>
</dependency>
```

**2、配置 MCP 服務端**

```yaml
spring:
  application:
    name: mcp-server
  main:
    web-application-type: none # 必須禁用web應用類型
    banner-mode: off # 禁用banner
  ai:
    mcp:
      server:
        stdio: true # 啟用stdio模式
        name: mcp-server # 服務器名稱
        version: 0.0.1 # 服務器版本
```

**3、實現 MCP 工具**

`@Tool` 是 Spring AI MCP 框架中用於快速暴露業務能力為 AI 工具的核心註解。下面是一段示例代碼：

```java
/**
 * 根據搜索詞搜索面試鴨面試題目
 */
@Tool(description = "根據搜索詞搜索面試鴨面試題目")
public String callMianshiya(String searchText) {
    // 執行從面試鴨數據庫中搜索題目的邏輯
    System.out.println("用戶要搜索：" + searchText);
}
```

**4、註冊 MCP 工具**

```java
@Bean
public ToolCallbackProvider serverTools(MianshiyaService mianshiyaService) {
    return MethodToolCallbackProvider.builder()
             .toolObjects(mianshiyaService)
             .build();
}
```

**5、運行服務端**

```bash
mvn clean package -DskipTests
```



### MCP 客戶端（基於 stdio 標準流）

**1、引入依賴**

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-mcp-client-spring-boot-starter</artifactId>
  <version>1.0.0-M6</version>
</dependency>
```

**2、配置 MCP 服務器**

```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          servers-configuration: classpath:/mcp-servers-config.json
```

其中 `mcp-servers-config.json` 的配置如下：

```json
{
  "mcpServers": {
    "mianshiyaServer": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-Dspring.main.web-application-type=none",
        "-Dlogging.pattern.console=",
        "-jar",
        "/yourPath/mcp-server-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

**3、初始化聊天客戶端**

```java
@Bean
public ChatClient initChatClient(ChatClient.Builder chatClientBuilder,
                                 ToolCallbackProvider mcpTools) {
    return chatClientBuilder.defaultTools(mcpTools).build();
}
```

**4、接口調用**

```java
@PostMapping(value = "/ai/answer")
public String generate(@RequestBody AskRequest request) {
    return chatClient.prompt()
            .user(request.getContent())
            .call()
            .content();
}
```



### MCP 服務端（基於 SSE）

除了基於 stdio 的實現外，Spring AI 還提供了基於 Server-Sent Events (SSE) 的 MCP 方案。相較於 stdio 方式，SSE 更適用於遠程部署的 MCP 服務器。

**1、引入依賴**

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-webflux-spring-boot-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>
```

**2、配置 MCP 服務端**

```yaml
server:
  port: 8090
spring:
  ai:
    mcp:
      server:
        name: mcp-server
        version: 0.0.1
```

**3、運行服務端**

```bash
mvn clean package -DskipTests
java -jar target/mcp-server-0.0.1-SNAPSHOT.jar
```



## 四、軟件使用 MCP

除了利用程序去調用 MCP 服務外，MCP 服務端還支持任意支持 MCP 協議的智能體助手，比如 Claude、Cursor 以及 Cherry Studio 等，都可以快速接入。

以 Cherry Studio 為例：

1、打開 Cherry Studio 的 "設置"，點擊 "MCP 服務器"

![Cherry Studio 設置](https://pic.yupi.icu/1/1743063238632-2156707f-cfa4-4493-bf3e-68279f3972b9.png)

2、點擊 "編輯 JSON"，將 MCP 配置添加到配置文件中

3、在 "設置 => 模型服務" 裡選擇一個模型，勾選工具函數調用功能

4、進入聊天頁面，在輸入框下面勾選開啟 MCP 服務

![開啟 MCP](https://pic.yupi.icu/1/1743063248363-b3a09c97-1bb9-4f97-ab0a-2cee5a641c83.png)

配置完成，嘗試搜索下面試題目，效果不戳！

![搜索效果](https://pic.yupi.icu/1/1743063251268-145a5d00-4495-49e8-91db-f5536efca436.png)

甚至還進行面經解析，返回多個面試題目與答案的鏈接！

![面經解析](https://pic.yupi.icu/1/1743143537320-1fca3955-3128-42a6-bd32-0cace3bab2ab.png)

當然這個功能我們 [面試鴨官方](https://www.mianshiya.com/) 也實現了，幫助大家面試覆盤：

![面試鴨面經解析](https://pic.yupi.icu/1/1744008304237-fdfb2b99-9038-43de-94ed-ca0760afdf40.png)



## 五、上傳 MCP 服務

和開發一個 APP 一樣，我們也可以把做好的 MCP 服務分享到第三方 MCP 服務平台。比如 MCP.so，可以理解為 MCP 服務的應用市場。

![MCP.so](https://pic.yupi.icu/1/1744008425870-c5b7958e-98cc-4a14-a4af-cba3af01fcde.png)

直接點擊頭像左側的提交按鈕，然後填寫 MCP 服務的項目地址、以及服務器配置實例，點擊提交即可。

![提交 MCP](https://pic.yupi.icu/1/1744008547763-70effe90-c0aa-4683-bbc8-060639266529.png)

提交完成後就可以在平台搜索到了：

![搜索 MCP](https://pic.yupi.icu/1/1744008638998-003207ee-8394-4ded-9ea6-83dbf189477c.png)



---



OK 就分享到這裡，學會的話記得點讚收藏哦。也歡迎大家在評論區交流你對 MCP 的看法和理解~




## 推薦資源

1）魚皮 AI 導航網站：[AI 資源大全、最新 AI 資訊、免費 AI 教程](https://ai.codefather.cn)

2）編程導航學習圈：[學習路線、編程教程、實戰項目、求職寶典、交流答疑](https://www.codefather.cn)

3）程式設計師面試八股文：[實習/校招/社招高頻考點、企業真題解析](https://www.mianshiya.com)

4）程式設計師寫簡歷神器：[專業模板、豐富例句、直通面試](https://www.laoyujianli.com)

5）1 對 1 模擬面試：[實習/校招/社招面試拿 Offer 必備](https://ai.mianshiya.com)