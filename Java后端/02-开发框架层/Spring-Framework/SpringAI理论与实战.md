SpringAI理论与实战
第一章 SpringAI核心理论
1.1 什么是SpringAI
SpringAI是Spring官方生态下的子项目，并非独立研发新的大模型，而是一款专注于简化Spring应用与各类AI模型集成的「大模型集成框架」。其核心目标是解决不同AI厂商（如OpenAI、百度文心一言、阿里通义千问等）API差异大、开发繁琐的痛点，为Spring生态提供统一的AI开发抽象，让开发者无需关注大模型底层调用细节，以Spring熟悉的依赖注入、注解驱动方式快速开发AI应用。
类比理解：SpringAI之于大模型，就像Spring Data之于数据库——无需手动编写JDBC代码，通过统一接口即可操作不同数据库；同样，无需手动封装大模型的HTTP API，通过SpringAI的统一接口，即可无缝切换调用不同厂商的大模型，大幅降低AI集成的学习成本和开发成本。
1.2 SpringAI核心优势
统一API抽象：封装了各类大模型的调用逻辑，提供ChatClient（对话交互）、EmbeddingClient（向量生成）等标准化接口，切换OpenAI、文心一言等大模型时，无需修改业务代码，仅需调整依赖和配置即可实现。
无缝集成Spring生态：完全兼容Spring Boot自动配置、依赖注入、AOP等核心特性，符合Spring开发者的使用习惯，无需额外学习新的开发范式，学习成本极低。
丰富的辅助特性：内置Prompt模板、对话记忆、结构化输出、自定义Advisor等功能，解决AI开发中的常见痛点，如硬编码Prompt导致的维护困难、多轮对话上下文丢失、输出格式混乱等问题。
轻量级与多模态支持：采用无侵入式集成设计，可按需引入对应大模型的依赖，不增加项目冗余；同时支持文本、图像、音频等多模态数据处理，覆盖聊天、文生图、语音转换等全场景AI需求。
企业级适配：天生支持微服务、高并发、安全合规等企业级需求，可无缝集成Spring Cloud，实现AI服务的分布式部署、负载均衡，适配金融、大型电商等复杂场景。
1.3 SpringAI核心概念与组件
1.3.1 核心概念
模型（Model）：AI模型是处理和生成信息的算法，通过从大量数据中学习模式，实现预测、文本生成、图像生成等功能。SpringAI支持聊天模型、嵌入模型、文生图模型等多种类型，适配不同应用场景。
提示（Prompt）：引导AI模型产生特定输出的语言输入，是大模型的“输入载体”，包含用户输入、系统指令等内容。Prompt设计是一门专业技能（即提示工程），合理的Prompt能大幅提升模型输出质量。
提示模板（PromptTemplate）：用于解决硬编码Prompt的问题，支持参数化替换、外部文件加载，类似Spring MVC中的“视图”，通过填充占位符动态生成Prompt，提升代码可读性和维护性。
嵌入（Embedding）：将文本、图像等内容转换为浮点数组（向量）的过程，捕捉内容的语义关系，是检索增强生成（RAG）场景的核心支撑，可用于文本相似度计算、语义检索等。
Tokens：AI模型处理文本的基本单位，输入和输出的Tokens数量决定了API调用成本和处理能力，模型存在上下文窗口限制（即单次调用可处理的最大Tokens数）。
1.3.2 核心组件（新手必知）
ChatClient：对话交互核心客户端，是开发者与大模型交互的入口，负责发送Prompt、接收响应，支持同步/异步调用，还支持流式响应，适用于大文本生成场景，避免长时间等待。
Prompt与PromptTemplate：Prompt封装用户输入和系统指令，PromptTemplate提供模板化能力，支持动态填充参数，避免硬编码字符串拼接，常见于多场景复用Prompt的场景。
ConversationMemory：对话记忆组件，负责存储多轮对话的上下文信息，让AI能够“记住”之前的对话内容，实现连续、连贯的多轮交互，解决单轮对话的局限性。
EmbeddingClient：向量生成客户端，用于将文本转换为向量，为RAG场景提供核心支持，原生支持Redis、PGVector、Milvus等17种主流向量数据库，无需手动编写适配代码。
FunctionCalling：让AI模型具备操作外部系统的能力，例如调用数据库查询数据、调用支付接口完成交易，实现“AI思考+工具执行”的闭环，拓展AI应用的边界。
1.4 SpringAI与主流AI框架对比
SpringAI的核心优势在于深耕Java/Spring生态，与其他主流AI框架相比，定位和适用场景各有侧重，具体对比如下表所示，帮助开发者快速选型：
框架
核心定位
适用人群
核心优势
短板
SpringAI
Java/Spring生态的AI集成框架，实现大模型统一调用
Java/Spring技术栈团队、大型企业
无缝集成Spring生态、企业级适配、低学习成本
语言绑定Java、生态较LangChain较新
LangChain
Python生态功能全面的AI开发工具箱
Python技术栈开发者、研究人员
生态完善、组件丰富、灵活性高
版本变化快、存在轻微性能损耗
Dify
低代码/零代码AI应用搭建平台
非技术人员、创业团队
可视化搭建、开箱即用、团队协作便捷
深度定制能力有限、平台绑定性强
LangGraph
复杂AI工作流管理框架
资深工程师、复杂Agent开发场景
支持状态持久化、复杂分支循环
学习曲线陡峭、调试难度高
第二章 SpringAI实战入门
2.1 实战环境准备
2.1.1 环境要求
JDK 17+（SpringAI对JDK版本有最低要求，低于17会出现兼容性问题）
Spring Boot 3.2+（确保与SpringAI版本兼容，推荐使用最新稳定版）
AI模型API Key（本次实战使用OpenAI GPT-3.5，需自行注册OpenAI账号获取API Key）
开发工具：IntelliJ IDEA（推荐）、Maven 3.6+
2.1.2 引入依赖（pom.xml）
创建Spring Boot项目，引入Spring Boot Web依赖、Spring AI OpenAI Starter依赖，Lombok可选（用于简化实体类代码），SpringAI会自动管理相关依赖版本，无需手动指定兼容版本：
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.4</version>
    <relativePath/>
</parent&gt;
&lt;dependencies&gt;
    <!-- Spring Boot Web 依赖：提供HTTP接口能力，用于测试AI交互接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    &lt;/dependency&gt;
    <!-- Spring AI OpenAI 依赖：对接OpenAI大模型，自动配置ChatClient等核心组件 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        <version>1.0.0-M1</version><!-- 推荐使用最新稳定版本，可前往Spring官网查询更新 -->
    </dependency&gt;
    <!-- Lombok 依赖（可选）：简化实体类get/set、构造方法等冗余代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional&gt;true&lt;/optional&gt;
    &lt;/dependency&gt;
    <!-- 测试依赖：用于编写单元测试，验证AI服务功能正确性 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
2.1.3 配置大模型信息（application.yml）
在配置文件中填写OpenAI的API Key、模型名称等参数，SpringAI会自动配置ChatClient实例，无需手动创建。若需切换为百度文心一言、阿里通义千问等其他大模型，只需替换依赖和对应配置项即可，无需修改业务代码：
spring:
  ai:
    openai:
      api-key: 你的OpenAI API Key # 替换为自己的API Key
      chat:
        model: gpt-3.5-turbo # 模型名称，可选gpt-4等
        temperature: 0.7 # 随机性，0-1之间，值越小越严谨
        max-tokens: 1024 # 单次响应最大Tokens数，不超过模型上下文窗口限制
2.2 实战案例：基础AI对话功能开发
本次实战围绕3个核心场景展开，从简单到复杂，逐步掌握SpringAI的基础用法，涵盖单次对话、Prompt模板、多轮对话（对话记忆），每个案例提供完整代码和测试方法。
2.2.1 案例1：单次对话（无上下文）
最基础的AI交互场景，通过ChatClient发送单次Prompt，获取大模型响应，无需保存对话上下文，类似“一问一答”模式，适用于简单的咨询、查询场景。
步骤1：编写Service层代码
创建对话服务类，注入ChatClient实例，提供单次对话方法，直接调用ChatClient的prompt方法发送请求，获取响应内容：
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.ChatClient;
import org.springframework.stereotype.Service;
/**
 * SpringAI 基础对话服务（单次对话）
     */
    @Service
    @RequiredArgsConstructor // 自动注入依赖，无需手动写@Autowired
    public class SimpleChatService {
    // 注入SpringAI自动配置的ChatClient实例
    private final ChatClient chatClient;
    /**
     * 单次对话方法
     * @param message 用户输入的消息
     * @return 大模型的响应内容
     */
    public String singleChat(String message) {
        // 发送Prompt并获取响应，content()方法提取响应文本
        return chatClient.prompt(message).call().content();
    }
    }
    步骤2：编写Controller层代码
    创建接口，接收前端用户输入，调用Service层方法，返回大模型响应，用于测试交互功能：
    import lombok.RequiredArgsConstructor;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RequestParam;
    import org.springframework.web.bind.annotation.RestController;
    /**
 * 对话接口控制器
     */
    @RestController
    @RequiredArgsConstructor
    public class ChatController {
    private final SimpleChatService simpleChatService;
    // 单次对话接口，通过请求参数接收用户消息
    @GetMapping("/ai/single-chat")
    public String singleChat(@RequestParam String message) {
        return simpleChatService.singleChat(message);
    }
    }
    步骤3：测试接口
    启动Spring Boot项目，通过浏览器或Postman访问接口，例如：http://localhost:8080/ai/single-chat?message=介绍SpringAI，即可获取大模型的响应结果，实现单次对话功能。
    2.2.2 案例2：Prompt模板（参数化交互）
    实际开发中，经常需要复用Prompt格式（如固定提问模板、系统指令），此时可使用PromptTemplate实现参数化替换，避免硬编码字符串拼接，提升代码可维护性。
    步骤1：编写Service层代码
    创建Prompt模板服务，使用PromptTemplate构建模板，通过Map填充参数，生成动态Prompt，再调用ChatClient发送请求：
    import lombok.RequiredArgsConstructor;
    import org.springframework.ai.chat.ChatClient;
    import org.springframework.ai.chat.prompt.Prompt;
    import org.springframework.ai.chat.prompt.PromptTemplate;
    import org.springframework.stereotype.Service;
    import java.util.HashMap;
    import java.util.Map;
    /**
 * SpringAI Prompt模板服务
     */
    @Service
    @RequiredArgsConstructor
    public class PromptTemplateService {
    private final ChatClient chatClient;
    /**
     * 基于Prompt模板的对话方法
     * @param user需求 用户的具体需求
     * @param format 期望的输出格式（如markdown、json）
     * @return 大模型的响应内容
     */
    public String templateChat(String user需求, String format) {
        // 1. 定义Prompt模板，使用{占位符}表示参数
        String template = "请根据用户需求：{user需求}，按照{format}格式输出内容，要求简洁明了，重点突出。";
        // 2. 创建PromptTemplate实例，绑定模板
        PromptTemplate promptTemplate = new PromptTemplate(template);
        // 3. 填充模板参数
        Map<String, Object> params = new HashMap<>();
        params.put("user需求", user需求);
        params.put("format", format);
        // 4. 生成Prompt对象
        Prompt prompt = promptTemplate.create(params);
        // 5. 发送Prompt并获取响应
        return chatClient.prompt(prompt).call().content();
    }
    }
    步骤2：添加接口
    在ChatController中添加Prompt模板对话接口：
    @RestController
    @RequiredArgsConstructor
    public class ChatController {
    // 新增：注入PromptTemplateService
    private final PromptTemplateService promptTemplateService;
    private final SimpleChatService simpleChatService;
    // 原有单次对话接口...
    // Prompt模板对话接口
    @GetMapping("/ai/template-chat")
    public String templateChat(
            @RequestParam String user需求,
            @RequestParam String format
    ) {
        return promptTemplateService.templateChat(user需求, format);
    }
    }
    步骤3：测试接口
    访问接口：http://localhost:8080/ai/template-chat?user需求=介绍SpringAI核心组件&format=markdown，即可获取符合指定格式的响应，验证Prompt模板的参数化功能。
    2.2.3 案例3：多轮对话（对话记忆）
    单次对话无法保存上下文，多轮对话场景（如智能客服）需要让AI“记住”之前的对话内容，此时可使用ConversationMemory组件实现上下文管理，SpringAI提供了InMemoryConversationMemory（内存级对话记忆），适用于简单场景。
    步骤1：编写Service层代码
    创建多轮对话服务，注入ConversationMemory，每次对话时将用户消息和AI响应存入记忆，实现连续交互：
    import lombok.RequiredArgsConstructor;
    import org.springframework.ai.chat.ChatClient;
    import org.springframework.ai.chat.Conversation;
    import org.springframework.ai.chat.memory.InMemoryConversationMemory;
    import org.springframework.stereotype.Service;
    /**
 * SpringAI 多轮对话服务（带对话记忆）
     */
    @Service
    @RequiredArgsConstructor
    public class MultiRoundChatService {
    private final ChatClient chatClient;
    /**
     * 多轮对话方法，通过conversationId区分不同对话会话
     * @param conversationId 对话会话ID（用于隔离不同用户的对话记忆）
     * @param message 用户输入的消息
     * @return 大模型的响应内容
     */
    public String multiRoundChat(String conversationId, String message) {
        // 1. 创建对话记忆（内存级，重启项目后丢失，生产环境可替换为持久化记忆）
        InMemoryConversationMemory conversationMemory = new InMemoryConversationMemory();
        // 2. 创建对话对象，绑定会话ID和记忆
        Conversation conversation = new Conversation(conversationId, conversationMemory);
        // 3. 发送用户消息，自动关联上下文
        conversation.addUserMessage(message);
        var response = chatClient.prompt(conversation).call();
        // 4. 将AI响应存入对话记忆
        conversation.addAssistantMessage(response.getResult().getOutput().getContent());
        // 5. 返回响应内容
        return response.getResult().getOutput().getContent();
    }
    }
    步骤2：添加接口
    在ChatController中添加多轮对话接口：
    @RestController
    @RequiredArgsConstructor
    public class ChatController {
    // 新增：注入MultiRoundChatService
    private final MultiRoundChatService multiRoundChatService;
    private final PromptTemplateService promptTemplateService;
    private final SimpleChatService simpleChatService;
    // 原有接口...
    // 多轮对话接口
    @GetMapping("/ai/multi-round-chat")
    public String multiRoundChat(
            @RequestParam String conversationId,
            @RequestParam String message
    ) {
        return multiRoundChatService.multiRoundChat(conversationId, message);
    }
    }
    步骤3：测试接口
    1. 第一次访问：http://localhost:8080/ai/multi-round-chat?conversationId=test001&message=介绍SpringAI，获取响应；
    2. 第二次访问：http://localhost:8080/ai/multi-round-chat?conversationId=test001&message=它的核心组件有哪些，AI会基于上一轮的对话内容，继续补充核心组件信息，实现多轮连贯交互。
    第三章 实战进阶：RAG场景开发（检索增强生成）
    3.1 RAG场景核心原理
    RAG（Retrieval-Augmented Generation，检索增强生成）是解决大模型“知识过时”“幻觉”问题的核心方案，其原理是：将企业私有文档（如知识库、手册）转换为向量，存储到向量数据库中；用户提问时，先从向量数据库中检索与问题最相关的文档片段，再将检索结果与用户问题结合，发送给大模型，让大模型基于检索到的准确信息生成响应，确保回答的准确性和时效性。
    SpringAI原生支持RAG场景开发，整合了EmbeddingClient（向量生成）、向量数据库集成、Prompt模板等组件，简化RAG场景的开发流程。
    3.2 RAG实战：文档问答系统
    本次实战以“SpringAI文档问答”为例，使用Redis作为向量数据库，实现用户提问时，从本地文档中检索相关内容，再生成响应。
    3.2.1 环境准备
    安装Redis（需支持向量存储，推荐Redis 7.0+）；
    引入Redis向量数据库依赖和文档处理依赖，在pom.xml中添加：
    <!-- Spring AI Redis 向量数据库依赖 -->
    <dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-redis-store</artifactId>
    <version>1.0.0-M1</version>
    </dependency>
    <!-- 文档处理依赖（用于解析本地文档，如txt、pdf） -->
    <dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-document-reader-tika</artifactId>
    <version>1.0.0-M1</version>
    </dependency>
    3.2.2 配置Redis向量数据库（application.yml）
    spring:

  # Redis 配置
  redis:
    host: localhost
    port: 6379
    password: 你的Redis密码（无密码可省略）
  ai:
    openai:

      # 原有OpenAI配置...
    embedding:
      openai:
        model: text-embedding-ada-002 # OpenAI的嵌入模型
    vectorstore:
      redis:
        index-name: springai_doc_index # 向量索引名称
3.2.3 编写核心代码
步骤1：文档加载与向量入库
创建文档处理服务，加载本地SpringAI相关文档（如txt文件），转换为向量后存入Redis向量数据库：
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.RedisVectorStore;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.List;
/**
 * 文档处理与向量入库服务
     */
    @Service
    @RequiredArgsConstructor
    public class DocumentService {
    private final EmbeddingClient embeddingClient;
    private final RedisVectorStore redisVectorStore;
    /**
     * 加载本地文档并入库（仅需执行一次）
     * @param filePath 文档路径（如：D:/springai_doc.txt）
     */
    public void loadDocumentToVectorStore(String filePath) {
        // 1. 读取本地文档（支持txt、pdf等格式）
        TikaDocumentReader reader = new TikaDocumentReader(new File(filePath));
        List<Document> documents = reader.get();
        // 2. 将文档转换为向量并存入Redis
        redisVectorStore.add(documents);
        System.out.println("文档加载完成，共入库 " + documents.size() + " 个文档片段");
    }
    }
    步骤2：RAG问答服务
    创建RAG问答服务，实现“用户提问→检索相关文档→生成响应”的完整流程：
    import lombok.RequiredArgsConstructor;
    import org.springframework.ai.chat.ChatClient;
    import org.springframework.ai.chat.prompt.Prompt;
    import org.springframework.ai.chat.prompt.PromptTemplate;
    import org.springframework.ai.vectorstore.VectorStore;
    import org.springframework.stereotype.Service;
    import java.util.HashMap;
    import java.util.Map;
    /**
 * RAG 文档问答服务
     */
    @Service
    @RequiredArgsConstructor
    public class RagQAService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    /**
     * RAG问答方法
     * @param question 用户提问
     * @return 基于文档检索的准确响应
     */
    public String ragQA(String question) {
        // 1. 从向量数据库中检索与问题最相关的3个文档片段
        var retrievedDocs = vectorStore.similaritySearch(question, 3);
        // 2. 构建Prompt模板，结合检索到的文档和用户问题
        String template = "请基于以下参考文档，回答用户的问题：\n" +
                "参考文档：{retrievedDocs}\n" +
                "用户问题：{question}\n" +
                "要求：1. 仅基于参考文档回答，不添加文档中没有的信息；2. 回答简洁明了，重点突出；3. 若文档中没有相关信息，直接回复'未找到相关答案'。";
        // 3. 填充模板参数
        Map<String, Object> params = new HashMap<>();
        params.put("retrievedDocs", retrievedDocs);
        params.put("question", question);
        // 4. 生成Prompt并发送请求
        PromptTemplate promptTemplate = new PromptTemplate(template);
        Prompt prompt = promptTemplate.create(params);
        return chatClient.prompt(prompt).call().content();
    }
    }
    步骤3：添加接口测试
    在ChatController中添加文档加载和RAG问答接口：
    @RestController
    @RequiredArgsConstructor
    public class ChatController {
    // 新增：注入DocumentService和RagQAService
    private final DocumentService documentService;
    private final RagQAService ragQAService;
    // 原有服务注入...
    // 文档加载接口（仅需调用一次，加载文档入库）
    @GetMapping("/ai/load-document")
    public String loadDocument(@RequestParam String filePath) {
        documentService.loadDocumentToVectorStore(filePath);
        return "文档加载成功！";
    }
    // RAG问答接口
    @GetMapping("/ai/rag-qa")
    public String ragQA(@RequestParam String question) {
        return ragQAService.ragQA(question);
    }
    }
    3.2.4 测试RAG功能
    1. 准备本地文档（如springai_doc.txt），写入SpringAI相关知识点（如核心组件、优势等）；
    2. 访问接口加载文档：http://localhost:8080/ai/load-document?filePath=D:/springai_doc.txt，提示加载成功；
    3. 访问RAG问答接口：http://localhost:8080/ai/rag-qa?question=SpringAI的核心组件有哪些，AI会基于加载的文档内容生成准确响应，验证RAG功能的有效性。
    第四章 实战总结与进阶方向
    4.1 实战总结
    本次实战从SpringAI核心理论出发，逐步完成了基础环境搭建、单次对话、Prompt模板、多轮对话、RAG文档问答等场景的开发，核心要点如下：
    SpringAI的核心价值是“统一大模型调用接口”，无缝集成Spring生态，降低Java开发者集成AI的门槛；
    核心组件ChatClient、PromptTemplate、ConversationMemory、EmbeddingClient是实现基础AI交互和RAG场景的关键；
    实战中，通过配置文件即可完成大模型和向量数据库的集成，无需手动编写复杂的调用代码；
    RAG场景是解决大模型“幻觉”问题的核心方案，SpringAI提供了完整的组件支持，简化了文档加载、向量入库、检索问答的全流程。
    4.2 进阶方向
    多模型切换与动态配置：实现基于配置中心的大模型动态切换，支持根据业务场景自动选择合适的大模型（如文本对话用GPT-3.5，文生图用Stable Diffusion）；
    对话记忆持久化：将InMemoryConversationMemory替换为Redis、MySQL等持久化存储，实现对话记忆的长期保存，适配生产环境；
    函数调用实战：开发基于FunctionCalling的场景，如让AI调用数据库查询接口、天气接口，实现“AI思考+工具执行”的闭环；
    可观察性与监控：集成Spring Boot Actuator，实现AI调用的日志记录、性能监控、异常告警，保障生产环境的稳定性；
    多模态场景开发：探索文生图、语音转文字、文字转语音等多模态交互，拓展AI应用的边界。
    SpringAI作为Spring生态的重要延伸，正在快速迭代完善，其“以开发者为中心”的设计理念，让Java开发者能够快速入局AI开发，无需跨越技术鸿沟。后续可结合具体业务场景，深入探索SpringAI的高级特性，构建更强大的AI应用。
