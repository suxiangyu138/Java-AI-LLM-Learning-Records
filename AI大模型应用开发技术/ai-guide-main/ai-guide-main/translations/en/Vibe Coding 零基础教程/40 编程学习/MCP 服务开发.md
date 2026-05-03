# MCP Service Development Step-by-Step Guide

> Developing MCP Services from Scratch

Hello, I'm programmer Yupi.

There's a hot AI-related concept called MCP, short for Model Context Protocol. This is an open standard introduced by Anthropic, aiming to provide a unified and standardized interface for large language models and AI assistants, enabling AI to easily operate external tools and accomplish more complex tasks.

This article will guide you through MCP, explain its core concepts, and take the example of the interview question search MCP service we developed for our product [Interview Duck](https://www.mianshiya.com/) to demonstrate the practical development of MCP server and client!

Open Source Link: https://github.com/yuyuanweb/mcp-mianshiya-server

![MCP Service](https://pic.yupi.icu/1/1744008993525-16f07f5b-e3b4-4f1a-97dd-ea886c8d945d.png)

## 1. Why is MCP So Important?

Previously, if we wanted AI to process our data, we had to rely on pre-trained data or upload data, which was both cumbersome and inefficient. Moreover, even powerful AI models faced data isolation issues and couldn't directly access new data.

Now, MCP solves this problem by breaking the model's dependency on static knowledge bases, giving it stronger dynamic interaction capabilities. It can call search engines, access local files, connect to API services, and even directly operate third-party libraries, just like humans.

More importantly, as long as everyone follows the MCP protocol, AI can seamlessly connect local data, internet resources, development tools, productivity software, and even the entire community ecosystem, achieving true "Internet of Everything." This will significantly enhance AI's collaboration and working capabilities, with immeasurable value.

![MCP Architecture](https://pic.yupi.icu/1/1744006127873-09bedb4a-5c7c-4cd0-9c4d-3620f319eac7.png)

## 2. MCP Overall Architecture

The core of MCP is the "client-server" architecture, where the MCP client host can connect to multiple servers. The client host refers to programs that wish to access data via MCP, such as Claude Desktop, IDE, or AI tools.

![MCP Architecture Diagram](https://pic.yupi.icu/1/1742979138403-f9f03e19-3537-461e-95d5-6f8a9a413c3a.jpeg)

## 3. MCP Development

MCP usage is divided into two modes: STDIO mode (local execution) and SSE mode (remote service).

### MCP Server (Based on stdio Standard Stream)

The stdio-based implementation is the most common MCP client solution, communicating with the MCP server via standard input/output streams. It is particularly suitable for locally deployed MCP servers.

**1. Introduce Dependency**

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
  <version>1.0.0-M6</version>
</dependency>
```

**2. Configure MCP Server**

```yaml
spring:
  application:
    name: mcp-server
  main:
    web-application-type: none # Must disable web application type
    banner-mode: off # Disable banner
  ai:
    mcp:
      server:
        stdio: true # Enable stdio mode
        name: mcp-server # Server name
        version: 0.0.1 # Server version
```

**3. Implement MCP Tool**

`@Tool` is the core annotation in the Spring AI MCP framework for quickly exposing business capabilities as AI tools. Below is an example code snippet:

```java
/**
 * Search Interview Duck interview questions based on search term
 */
@Tool(description = "Search Interview Duck interview questions based on search term")
public String callMianshiya(String searchText) {
    // Execute logic to search questions from the Interview Duck database
    System.out.println("User wants to search: " + searchText);
}
```

**4. Register MCP Tool**

```java
@Bean
public ToolCallbackProvider serverTools(MianshiyaService mianshiyaService) {
    return MethodToolCallbackProvider.builder()
             .toolObjects(mianshiyaService)
             .build();
}
```

**5. Run Server**

```bash
mvn clean package -DskipTests
```

### MCP Client (Based on stdio Standard Stream)

**1. Introduce Dependency**

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-mcp-client-spring-boot-starter</artifactId>
  <version>1.0.0-M6</version>
</dependency>
```

**2. Configure MCP Server**

```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          servers-configuration: classpath:/mcp-servers-config.json
```

The configuration for `mcp-servers-config.json` is as follows:

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

**3. Initialize Chat Client**

```java
@Bean
public ChatClient initChatClient(ChatClient.Builder chatClientBuilder,
                                 ToolCallbackProvider mcpTools) {
    return chatClientBuilder.defaultTools(mcpTools).build();
}
```

**4. Interface Call**

```java
@PostMapping(value = "/ai/answer")
public String generate(@RequestBody AskRequest request) {
    return chatClient.prompt()
            .user(request.getContent())
            .call()
            .content();
}
```

### MCP Server (Based on SSE)

In addition to the stdio-based implementation, Spring AI also provides an MCP solution based on Server-Sent Events (SSE). Compared to the stdio approach, SSE is more suitable for remotely deployed MCP servers.

**1. Introduce Dependency**

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-webflux-spring-boot-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>
```

**2. Configure MCP Server**

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

**3. Run Server**

```bash
mvn clean package -DskipTests
java -jar target/mcp-server-0.0.1-SNAPSHOT.jar
```

## 4. Software Using MCP

In addition to programmatically calling MCP services, the MCP server also supports any intelligent assistant that supports the MCP protocol, such as Claude, Cursor, and Cherry Studio, which can be quickly integrated.

Taking Cherry Studio as an example:

1. Open Cherry Studio's "Settings," click on "MCP Server"

![Cherry Studio Settings](https://pic.yupi.icu/1/1743063238632-2156707f-cfa4-4493-bf3e-68279f3972b9.png)

2. Click "Edit JSON" to add MCP configuration to the configuration file

3. In "Settings => Model Services," select a model and enable the tool function call feature

4. Enter the chat page and check the option to enable MCP service below the input box

![Enable MCP](https://pic.yupi.icu/1/1743063248363-b3a09c97-1bb9-4f97-ab0a-2cee5a641c83.png)

Configuration complete, try searching for interview questions, the effect is great!

![Search Effect](https://pic.yupi.icu/1/1743063251268-145a5d00-4495-49e8-91db-f5536efca436.png)

It even parses interview experiences, returning multiple links to interview questions and answers!

![Interview Experience Parsing](https://pic.yupi.icu/1/1743143537320-1fca3955-3128-42a6-bd32-0cace3bab2ab.png)

Of course, this feature has also been implemented by [Interview Duck Official](https://www.mianshiya.com/) to help everyone review interviews:

![Interview Duck Interview Experience Parsing](https://pic.yupi.icu/1/1744008304237-fdfb2b99-9038-43de-94ed-ca0760afdf40.png)

## 5. Upload MCP Service

Just like developing an app, we can also share the completed MCP service on third-party MCP service platforms. For example, MCP.so can be understood as an app store for MCP services.

![MCP.so](https://pic.yupi.icu/1/1744008425870-c5b7958e-98cc-4a14-a4af-cba3af01fcde.png)

Directly click the submit button on the left side of the avatar, then fill in the MCP service project address and server configuration instance, and click submit.

![Submit MCP](https://pic.yupi.icu/1/1744008547763-70effe90-c0aa-4683-bbc8-060639266529.png)

After submission, it can be searched on the platform:

![Search MCP](https://pic.yupi.icu/1/1744008638998-003207ee-8394-4ded-9ea6-83dbf189477c.png)

---

OK, that's all for this sharing. If you've learned something, remember to like and bookmark it. Also, feel free to share your thoughts and understanding of MCP in the comments~

## Recommended Resources

1) Yupi AI Navigation Website: [AI Resource Collection, Latest AI News, Free AI Tutorials](https://ai.codefather.cn)

2) Programming Navigation Learning Circle: [Learning Paths, Programming Tutorials, Practical Projects, Job Hunting Guide, Q&A](https://www.codefather.cn)

3) Programmer Interview Eight-Part Essay: [Internship/Campus Recruitment/Social Recruitment High-Frequency Points, Enterprise Real Questions Analysis](https://www.mianshiya.com)

4) Programmer Resume Writing Tool: [Professional Templates, Rich Examples, Direct to Interview](https://www.laoyujianli.com)

5) 1-on-1 Mock Interview: [Essential for Internship/Campus Recruitment/Social Recruitment Interviews to Get Offers](https://ai.mianshiya.com)