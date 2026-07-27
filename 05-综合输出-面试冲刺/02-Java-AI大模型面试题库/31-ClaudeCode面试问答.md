# Claude Code 必做项目清单 面试问答清单
> 🎯 基于 Claude Code 从入门到全栈工程化再到 AI 深度开发的项目清单，涵盖面试高频问题与完美解答方案。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：Claude Code 和传统代码补全工具（如 TabNine、GitHub Copilot）有什么本质区别？

**面试官意图：** 考察对 AI 辅助编程工具的发展趋势和技术原理的理解。

**完美解答：**

| 维度 | 传统代码补全（TabNine/Copilot） | Claude Code（Agent 模式） |
|------|--------------------------------|-------------------------|
| 工作模式 | 逐行/逐段补全 | 全仓库级代码生成与重构 |
| 上下文感知 | 当前文件 + 附近代码 | 整个项目结构 + 依赖关系 |
| 交互方式 | 打字时被动触发 | 自然语言指令主动执行 |
| 能力边界 | 补全当前输入 | 代码生成、Debug、重构、架构设计、文档生成 |
| 执行动作 | 建议代码（人工复制） | 直接读写文件、执行命令 |
| 多文件操作 | 不支持 | 支持跨文件分析和修改 |

**核心区别总结：**
- 传统工具是"**增强型键盘**"：你写代码，它帮你补全
- Claude Code 是"**协作型工程师**"：你描述需求，它理解项目后自主完成任务
- Claude Code 能连续执行：读取代码 → 分析 → 修改 → 运行 → 调试的完整循环

**延伸追问应对：** 如果面试官问"它在实际工作中能替代多少人工编码"，回答：重复性 CRUD 代码约 70-80% 可由 AI 生成，但是架构决策、关键逻辑设计、安全审查仍需要人工主导。AI 提升的是效率，不是替代。

---

### Q2：你在项目中使用 CLAUDE.md 文件做什么？它和 .cursorrules 有什么区别？

**面试官意图：** 考察 AI 辅助开发的工程化规范和配置能力。

**完美解答：**

**CLAUDE.md 的作用：**

CLAUDE.md 是项目的"上下文说明书"，放在项目根目录，Claude Code 启动时会自动读取。我主要用它来定义：

```markdown
# CLAUDE.md — 项目上下文说明书

## 项目背景
- 这是一个 Spring Boot 3.x + Vue 3 的全栈项目
- 数据库使用 MySQL 8.0，ORM 使用 MyBatis-Plus
- 包结构遵循 DDD 分层：controller → service → repository → entity

## 编码规范
- Controller 层统一使用 Result 包装返回
- Service 层接口必须写 JavaDoc 注释
- 所有参数校验使用 Jakarta Validation 注解
- 异常使用全局 GlobalExceptionHandler 处理

## 常用命令
- `mvn clean test` 运行测试
- `mvn spring-boot:run` 本地启动

## 数据库
- 本地开发环境：localhost:3306/dev_db
- 表名使用小写 + 下划线，如 `user_order`
```

**与 .cursorrules 的对比：**

| 特性 | CLAUDE.md | .cursorrules |
|------|-----------|--------------|
| 适用工具 | Claude Code | Cursor IDE |
| 自动加载 | 是（项目根目录自动读取） | 是 |
| 文件格式 | Markdown | Markdown |
| 核心作用 | 项目上下文 + 规范说明 | 项目上下文 + 规范说明 |
| 共同目标 | 让 AI 理解项目上下文，生成符合项目风格的代码 | 相同 |

**我的最佳实践：**
```
项目根目录/
├── CLAUDE.md        # 通用项目说明（Git 管理）
├── .cursorrules     # 如果同时用 Cursor 也配置一份
└── .claude/
    └── settings.json  # Claude Code 个性化配置
```

---

### Q3：你是怎么用 Claude Code 做代码审查的？它能发现哪些人工容易漏掉的问题？

**面试官意图：** 考察对 AI 辅助代码质量管控的理解和经验。

**完美解答：**

我使用 Claude Code 进行"AI 初审 + 人工终审"的代码审查流程，效率提升 3-5 倍。

**它能发现的核心问题类型：**

| 问题类型 | AI 审查优势 | 人工容易漏掉的场景 |
|----------|-------------|-------------------|
| 空指针风险 | 跨方法追踪可能的 null 传递 | 深层调用链中的空值场景 |
| 并发安全问题 | 识别缺少同步的共享变量 | 多线程环境下的竞态条件 |
| SQL 注入 | 检测字符串拼接的 SQL | 在复杂业务逻辑中的拼接查询 |
| 资源泄漏 | 检查 IO/DB 连接未关闭 | 异常路径下的资源释放 |
| 事务边界 | 检测事务注解失效场景 | 同 class 内方法调用 AOP 失效 |
| 循环依赖 | Bean 循环依赖检测 | 复杂的多模块依赖 |

**我的审查流程：**
```
1. 提交代码 → 触发 Claude Code 自动审查
2. AI 输出审查报告（问题列表 + 严重级别 + 修复建议）
3. P0/P1 问题自动修复 + 创建修复 PR
4. P2/P3 问题人工审核后批量修复
5. 人工终审：重点关注 AI 标记为"不确定"的问题
```

**实际案例**：Claude Code 在一次审查中发现了一个跨 3 个文件的并发问题：A 类的 `@Async` 方法调用 B 类的 `synchronized` 方法，而这个 `synchronized` 又依赖 C 类的 `ThreadLocal` 变量——在异步线程中 `ThreadLocal` 是拿不到值的。这种问题人工极难发现。

> 💡 **面试技巧**：讲具体的跨文件 Bug 发现案例，比抽象描述有说服力得多。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你怎么用 Claude Code 生成 Spring Boot 的 CRUD 代码？具体是怎么交互的？

**面试官意图：** 考察 AI 辅助开发的实际操作经验，是否真的在项目中用过。

**完美解答：**

**Prompt（一句话需求）：**
```
基于以下 MySQL 用户表，帮我生成 Spring Boot 3 + MyBatis-Plus 的完整 CRUD：
- 包含：Entity、Mapper、Service、Controller、VO、DTO
- 分页查询、新增、修改、删除
- 全局异常处理 + 参数校验
- 统一返回体 Result 包装

CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(200) NOT NULL COMMENT '密码',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `status` tinyint DEFAULT '1' COMMENT '状态 1：正常 0：禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
);
```

**Claude Code 生成的代码结构：**
```
src/main/java/com/example/user/
├── entity/
│   └── SysUser.java                  # 实体类，@TableName("sys_user")
├── mapper/
│   └── SysUserMapper.java            # BaseMapper<SysUser>
├── service/
│   ├── SysUserService.java           # 接口
│   └── impl/SysUserServiceImpl.java  # 实现
├── controller/
│   └── SysUserController.java        # REST 接口
├── dto/
│   ├── SysUserCreateDTO.java         # 新增 DTO（带 @NotBlank 校验）
│   ├── SysUserUpdateDTO.java         # 修改 DTO
│   └── SysUserQueryDTO.java          # 查询 DTO（分页参数）
└── vo/
    └── SysUserVO.java                # 返回体 VO
```

**关键代码示例（Controller）：**
```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class SysUserController {

    private final SysUserService userService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysUserCreateDTO dto) {
        return Result.success(userService.createUser(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, 
                               @Valid @RequestBody SysUserUpdateDTO dto) {
        userService.updateUser(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult<SysUserVO>> page(@Valid SysUserQueryDTO query) {
        return Result.success(userService.pageQuery(query));
    }

    @GetMapping("/{id}")
    public Result<SysUserVO> getById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }
}
```

**我的使用心得：**
- AI 生成基础 CRUD 代码的效率是手写的 10 倍
- 但我一定要审查生成的代码：参数校验是否完整、业务逻辑是否有漏洞、事务边界是否正确
- 固定流程：AI 生成 → 本地运行测试 → 人工审查调整 → 让 AI 批量应用修改

> 🎯 面试官想听到的不是"它能生成代码"，而是"你怎么结合 AI 和自己的经验写出更好的代码"。

---

### Q5：你用 Claude Code 做过 RAG 项目开发吗？具体是怎么实现的？

**面试官意图：** 考察 AI 大模型应用开发的技术深度，以及结合 AI 辅助工具的能力。

**完美解答：**

我用 Claude Code 快速搭建了一个全链路 RAG 私有化项目。核心是先生成骨架，再迭代优化。

**第一步：项目骨架生成**
```java
// Claude Code 生成的 RAG 核心链路
@Service
public class RagService {

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final ChatLanguageModel chatModel;

    public String answer(String question) {
        // 1. 问题向量化
        float[] queryVector = embeddingService.embed(question);

        // 2. 检索相似文档
        List<Document> docs = vectorStore.similaritySearch(
            queryVector, 5, 0.6  // Top 5, 阈值 0.6
        );

        // 3. 构建 Prompt
        String context = docs.stream()
            .map(d -> "【来源】" + d.getMetadata("source") + "\n" + d.getContent())
            .collect(Collectors.joining("\n---\n"));

        String prompt = """
            基于以下文档回答问题。如果文档中没有相关信息，请说"知识库中没有相关信息"。
            
            文档：
            %s
            
            问题：%s
            """.formatted(context, question);

        // 4. 调用 LLM
        return chatModel.generate(prompt);
    }
}
```

**第二步：检索优化**
```
使用 Claude Code 逐步添加：
1. 查询重写：LLM 扩展用户短查询
2. HyDE：假设性文档增强检索
3. Rerank：对检索结果重排序
4. 混合检索：向量 + BM25 加权融合
```

**第三步：工程化**
```java
// Claude Code 帮我处理的工程化细节
// 1. 流式返回（SSE）
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamChat(@RequestParam String question) {
    return ragService.streamAnswer(question)
        .map(content -> ServerSentEvent.builder(content).build());
}

// 2. 引用溯源
@GetMapping("/chat")
public Result<ChatResponse> chat(@RequestParam String question) {
    var response = ragService.answer(question);
    return Result.success(new ChatResponse(
        response.getAnswer(),
        response.getCitations().stream()
            .map(c -> new Citation(c.getTitle(), c.getContent(), c.getScore()))
            .collect(Collectors.toList())
    ));
}
```

> 💡 **面试价值**：展示"我用 AI 工具加速了 AI 项目开发"——这本身就是对 AI 技术理解的最好证明。

---

### Q6：你遇到过 Claude Code 生成的代码有 Bug 的情况吗？怎么处理的？

**面试官意图：** 考察对 AI 生成代码的批判性思维和审查能力。

**完美解答：**

**真实案例：** Claude Code 生成了一段 Excel 导出工具类，逻辑看起来完美，但导出的文件在 WPS 中打开时报错。

**定位问题过程：**
```java
// Claude Code 生成的有问题的代码
public void exportExcel(List<UserVO> users, HttpServletResponse response) {
    Workbook workbook = new XSSFWorkbook();  // 使用 XSSFWorkbook
    Sheet sheet = workbook.createSheet("用户数据");
    // ... 填充数据
    workbook.write(response.getOutputStream());
    workbook.close();
}

// 问题：XSSFWorkbook.write() 在自动关闭流之前还没有 flush
// 导致文件不完整
```

**修复方案：**
```java
@PostMapping("/export")
public void exportUsers(@RequestBody List<Long> ids, HttpServletResponse response) {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment;filename=users.xlsx");
    
    try (Workbook workbook = new XSSFWorkbook();
         OutputStream os = response.getOutputStream()) {
        
        Sheet sheet = workbook.createSheet("用户数据");
        // ... 填充数据
        workbook.write(os);
        os.flush();  // 关键：手动 flush
    } catch (IOException e) {
        log.error("导出失败", e);
        throw new BusinessException("导出失败");
    }
}
```

**总结 AI 生成代码常见的坑：**

| 问题类型 | AI 出错率 | 人工检查要点 |
|----------|-----------|-------------|
| 资源未关闭 | 中 | try-with-resources 覆盖了吗？ |
| 异常处理不完整 | 中 | 所有异常路径都有处理吗？ |
| 性能问题 | 高 | 循环内有没有不必要的数据操作？ |
| 并发安全 | 高 | 共享变量有无同步？ |
| 业务逻辑漏边界 | 中 | 空值、越界、重复操作等边界场景 |
| 版本兼容问题 | 中 | API 在项目使用版本中是否支持？ |

> 🎯 **核心心态**：AI 是强大的"初稿生成器"，但不是"最终代码交付者"。每次 AI 生成后，我会做三件事：看日志是否完整、测边界是否覆盖、想性能是否优化。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：你怎么用 Claude Code 做架构设计？它能在多大程度上辅助架构决策？

**面试官意图：** 考察在架构设计层面使用 AI 的深度和方法论。

**完美解答：**

我总结了一套"AI 辅助架构设计四步法"：

**第一步：痛点分析 + 方案建议**
```
我向 Claude Code 描述业务场景和痛点：
"我现在有一个单体电商项目，团队 10 人，日均订单 5000 单。
问题是：每次发布都需要全量部署，某个模块出问题影响全部。
我想拆成微服务，帮我分析方案。"

Claude Code 输出：
1. 业务模块拆分建议：用户、商品、订单、支付、库存
2. 每个模块的职责边界
3. 拆分优先级：先拆订单和支付（变更最频繁）
4. 不建议一步到位，推荐"绞杀者模式"
```

**第二步：接口契约定义**
```java
// Claude Code 生成的微服务间接口定义
// 订单服务 - 提供给外部调用的 Feign 接口
@FeignClient(name = "order-service", path = "/internal/orders")
public interface OrderInternalApi {
    
    @PostMapping("/create")
    OrderDTO createOrder(@Valid @RequestBody CreateOrderRequest request);
    
    @GetMapping("/{orderId}")
    OrderDTO getOrder(@PathVariable String orderId);
    
    @PostMapping("/{orderId}/pay")
    void markPaid(@PathVariable String orderId, @RequestParam BigDecimal amount);
}
```

**第三步：数据流设计**
```text
Claude Code 帮我画出：
用户下单流程：
1. API 网关 → 订单服务 → 调用库存服务预占库存
2. 订单服务 → 发送"订单创建"事件到 MQ
3. 支付服务 → 监听支付回调 → 调用订单服务确认支付
4. 订单服务 → 支付成功 → 发送"支付成功"事件
5. 通知服务 → 发送短信/邮件
```

**第四步：非功能指标审查**
```text
Claude Code 的架构建议中补充了：
- 每个服务的 QPS 预估和资源需求
- 数据一致性方案（最终一致性 vs 强一致性）
- 容错和降级策略
- 监控指标定义
```

**但 AI 做不了的决策：**
1. 团队能力匹配：如果团队不熟悉微服务，是否应该拆分？
2. 业务优先级：哪个模块优先拆分，取决于业务目标
3. 成本考量：微服务带来的运维成本是否值得？
4. 技术债务：现有代码的质量是否支撑拆分？

> 💡 **我的结论**：AI 是优秀的"架构参谋"——能提供方案选项、分析利弊、生成代码框架，但最终的架构决策必须由人根据业务上下文来做。

---

### Q8：Claude Code 在自动化测试方面能做什么？生成测试的质量如何？

**面试官意图：** 考察对 AI 生成测试代码的理解和实际使用经验。

**完美解答：**

我用 Claude Code 辅助测试开发，主要集中在三个层次：

**1. 单元测试生成（效果最好）**
```java
// Claude Code 生成的 Service 层单元测试
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;
    
    @Mock
    private ProductService productService;
    
    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_ShouldSucceed_WhenStockEnough() {
        // Arrange
        Long userId = 1L;
        Long productId = 1L;
        Integer quantity = 2;
        
        Product product = new Product();
        product.setId(productId);
        product.setStock(10);
        product.setPrice(new BigDecimal("99.99"));
        
        when(productService.getById(productId)).thenReturn(product);
        when(orderMapper.insert(any(Order.class))).thenReturn(1);
        
        // Act
        Order result = orderService.createOrder(userId, productId, quantity);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(productService).reduceStock(productId, quantity);
    }

    @Test
    void createOrder_ShouldThrowException_WhenStockInsufficient() {
        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setStock(0);  // 库存为 0
        
        when(productService.getById(1L)).thenReturn(product);
        
        // Act & Assert
        assertThrows(InsufficientStockException.class, () -> {
            orderService.createOrder(1L, 1L, 1);
        });
    }
}
```

**2. 接口测试（覆盖率好）**
```java
// Claude Code 生成的 Controller 层集成测试
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createOrder_WithValidRequest_ShouldReturn201() throws Exception {
        String requestJson = """
            {
                "productId": 1,
                "quantity": 2,
                "addressId": 10
            }
            """;
        
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderId").isNotEmpty());
    }
}
```

**3. 边界情况（需要人工补充）**
```text
AI 生成测试的覆盖情况：
✅ 正常流程（Happy Path）
✅ 常见异常（参数非法、数据不存在）
❌ 边界组合（多个边界条件同时出现）
❌ 并发场景（多线程竞态条件）
❌ 外部依赖异常（网络超时、第三方 API 降级）
```

> ⚠️ **经验之谈**：AI 生成的单元测试覆盖率可达 80%，但极端边界和并发场景还是需要人工补充。我的习惯是让 AI 生成"第一版测试"，我再补"AI 想不到的测试"。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：你怎么用 Claude Code 排查一个诡异的线上 Bug？描述一个实际案例。

**面试官意图：** 考察利用 AI 工具辅助问题定位的实战能力。

**完美解答：**

**真实案例：一个偶尔出现的"订单超时但未收到回调"问题**

**背景**：订单支付成功后，支付网关回调我们的通知接口，偶尔出现"订单状态未更新"的问题，但没有任何错误日志。

**排查过程：**

**第一步：用 Claude Code 分析整个回调处理流程**
```
我贴出支付回调的完整代码，让 Claude Code 审查。
它发现了一个线程安全问题：
```

```java
// 问题代码（Claude Code 发现）
@Component
public class PaymentCallbackService {
    
    @Autowired
    private OrderMapper orderMapper;
    
    // 问题：没有 @Transactional，也没有防重入机制
    public void handleCallback(String orderId, String status) {
        // 1. 查询当前订单状态
        Order order = orderMapper.selectById(orderId);
        
        // 2. 如果已经处理过，跳过（并发问题：两个线程同时通过 check）
        if (order.getStatus() == OrderStatus.PAID) {
            return;  
        }
        
        // 3. 更新状态
        orderMapper.updateStatus(orderId, OrderStatus.PAID);
        
        // 4. 发送通知
        notificationService.send(order.getUserId(), "支付成功");
        // 问题：如果第 4 步抛异常，第 3 步已执行导致状态更新了但通知没发
    }
}
```

**Claude Code 的修复建议：**
```java
@Transactional(rollbackFor = Exception.class)
public void handleCallback(String orderId, String status) {
    // 1. 使用数据库层面的乐观锁防止重复处理
    int updated = orderMapper.updateStatusIfNotPaid(orderId, OrderStatus.PAID);
    if (updated == 0) {
        log.info("订单 {} 已处理，跳过", orderId);
        return;
    }
    
    // 2. 事务内部操作：事务回滚时通知也不发送
    notificationService.send(orderId, "支付成功");
}

// Mapper 中的乐观锁 SQL
@Update("UPDATE `order` SET status = 'PAID', update_time = NOW() " +
        "WHERE id = #{orderId} AND status != 'PAID'")
int updateStatusIfNotPaid(@Param("orderId") String orderId);
```

> 🎯 **排查效率**：如果人工看这段代码 + 复现 + 定位，至少需要半天。用 Claude Code 审查 + 修复建议，30 分钟搞定。关键是我要把"上下文"给够——堆栈、代码、现象描述，缺一不可。

---

### Q10：你的项目中有用 Claude Code 做 CI/CD 自动化吗？怎么配置的？

**面试官意图：** 考察 DevOps 自动化的工程化水平。

**完美解答：**

我让 Claude Code 生成了完整的 GitHub Actions 流水线，实现"提交代码 → 自动构建 → 测试 → 部署"的全自动化。

```yaml
# .github/workflows/deploy.yml
# Claude Code 生成的 CI/CD 配置文件

name: Build, Test and Deploy

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test
          MYSQL_DATABASE: test_db
        ports:
          - 3306:3306

    steps:
      - uses: actions/checkout@v4
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      
      - name: Build & Test
        run: mvn clean verify -Pci
      
      - name: Upload Test Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: target/surefire-reports/
      
      - name: Build Docker Image
        if: github.ref == 'refs/heads/main'
        run: |
          docker build -t app:${{ github.sha }} .
          docker tag app:${{ github.sha }} app:latest
      
      - name: Deploy to Server
        if: github.ref == 'refs/heads/main'
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_KEY }}
          script: |
            cd /opt/app
            git pull origin main
            docker-compose pull app
            docker-compose up -d --force-recreate app
            docker system prune -f
```

**效果：**
- 每次 push 自动触发构建和测试（10 分钟完成）
- 测试通过后自动部署到预发布环境
- 部署失败自动回滚
- 开发者从提交代码到上线，只需一次 review，其他全自动化

---

### Q11：Claude Code 在处理"祖传代码"时表现怎么样？你用它重构过老旧项目吗？

**面试官意图：** 考察在遗留系统维护场景中使用 AI 工具的实战经验。

**完美解答：**

**真实场景**：接手了一个 3 年没维护的 Spring Boot 项目，Java 8 + XML 配置 + 零注释。

**我用 Claude Code 做的重构工作：**

**1. 代码分析 + 文档生成**
```
让 Claude Code 扫描整个项目，输出了：
- 项目架构概览（模块、依赖、数据流）
- 核心业务逻辑梳理
- 潜在问题清单（弃用 API、安全隐患、性能瓶颈）
```

**2. 自动化升级（Java 8 → 17）**
```java
// 重构前（XML 配置）
// UserService.java
public class UserService {
    private UserMapper userMapper;
    public void setUserMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
}

// applicationContext.xml
<bean id="userService" class="com.xx.UserService">
    <property name="userMapper" ref="userMapper"/>
</bean>

// Claude Code 自动重构后
@RequiredArgsConstructor
@Service
public class UserService {
    private final UserMapper userMapper;  // 构造器注入
}
```

**3. 批量消除坏味道**
```text
Claude Code 识别并修复了：
- 30+ 处 try-catch 空 catch 块 → 加日志或抛异常
- 15 处魔法数字 → 提取为常量
- 8 处超长方法（>200 行）→ 拆分为小方法
- 5 处循环内数据库查询 → 批量查询优化
```

**4. 测试补充**
```java
// 为原来零测试的核心逻辑补充了 40+ 个测试用例
// 确保重构前后行为一致
```

> ⚠️ **经验之谈**：重构老项目最关键的是"不改行为只改结构"。我会让 Claude Code 先生成测试（Characterization Test）锁定现有行为，再动手重构，确保回归测试通过。

---

### Q12：Claude Code 在处理多语言混编项目时表现如何？怎么用它做跨语言衔接？

**面试官意图：** 考察在复杂多语言技术栈项目中使用 AI 辅助工具的经验。

**完美解答：**

多语言混编项目（如 Java 后端 + Python AI 服务 + 前端 Vue）是 AI 时代的常见架构，Claude Code 在这方面有独特优势。

**1. 跨语言代码理解**
```
在项目根目录运行:
Claude Code 会自动识别项目中的多语言文件，理解它们之间的调用关系。

比如项目中 Java 通过 HTTP 调用 Python 的 AI 服务，
Claude Code 能同时分析两边的代码，识别接口不匹配。

实际案例：Java 端发送的 JSON 字段是 camelCase，
Python 端接收时用的是 snake_case，导致字段映射失败。
Claude Code 在跨文件审查中发现了这个问题。
```

**2. 统一接口定义生成**
```java
// Claude Code 帮我生成了两端的代码，保持接口一致

// Java 端（发送方）
public class AiServiceClient {
    
    public AiResponse chat(String question) {
        // Claude Code 根据 Python 端的 API 定义，自动生成 Java 调用代码
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(pythonServiceUrl + "/api/v1/chat"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                // 字段名与 Python 端完全一致
                """
                {"question": "%s", "session_id": "%s"}
                """.formatted(question, sessionId)
            ))
            .build();
        return httpClient.send(request, BodyHandlers.ofString(/*...*/));
    }
}

// Python 端（接收方）— Claude Code 同时生成的对应代码
@app.post("/api/v1/chat")
async def chat(request: ChatRequest):
    # 字段名与 Java 端完全对应
    question = request.question
    session_id = request.session_id
    # ...
```

**3. 跨语言重构**
```
场景：需要将某个 Python 数据处理模块迁移到 Java。
Claude Code 的做法：
1. 先读取完整的 Python 模块（包括所有边界逻辑）
2. 理解业务语义（不只是语法翻译）
3. 生成同业务的 Java 实现
4. 自动生成两端测试，验证输出结果一致
```

**4. Docker Compose 多服务编排**
```yaml
# Claude Code 生成的多语言服务编排
services:
  java-backend:
    build: ./backend
    ports:
      - "8080:8080"
    
  python-ai-service:
    build: ./ai-service
    ports:
      - "8000:8000"
    volumes:
      - ./models:/app/models
    deploy:
      resources:
        reservations:
          devices:
            - capabilities: [gpu]
```

> 🎯 **面试亮点**：多语言混编能力是高级场景。能讲清楚"用 AI 打通 Java 和 Python 的接口一致性"，说明你有架构级视野，不只是单语言开发者。

---

## 💎 面试加分金句

1. "Claude Code 不是替代程序员，而是让我从重复编码中解放出来，把精力放在架构设计和代码审查上——这才是程序员真正的价值。"
2. "我用 AI 辅助开发的核心流程是：AI 生成初稿 → 我审查逻辑 → AI 批量修改 → 我验证结果。AI 负责效率，我负责质量。"
3. "最大的收获不是代码写得快了，而是 AI 帮我发现了很多我以前注意不到的边界问题和安全隐患。"
4. "我在项目里配置了 CLAUDE.md，定义了编码规范和项目上下文，这样 AI 生成的代码风格天然和团队一致，省了 review 时间。"
5. "AI 辅助编程让一个人能完成以前一个 3 人小团队的工作，前提是这个人要有足够的技术功底来判断 AI 输出的正确性。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| AI 生成代码的版权问题怎么看？ | AI 辅助生成视为辅助工具，核心逻辑和架构决策属于开发者 |
| 有没有安全风险？ | 需要人工审查 SQL 注入、权限校验、敏感信息泄露等安全问题 |
| 团队怎么推广 AI 辅助开发？ | 先让 1-2 人试点出最佳实践，再输出团队规范推广 |
| 最不适合 AI 做的任务？ | 复杂的业务逻辑决策、安全关键代码、需要深度领域知识的逻辑 |
| 你怎么评估 AI 生成的代码质量？ | 跑测试 + 代码审查 + 性能基准对比，三个维度综合评估 |

## 🔗 关联知识点

- [Cursor必做项目清单-面试问答](./Cursor必做项目清单-面试问答.md) — Cursor IDE 的 AI 辅助开发对比
- [三大核心项目-面试问答](./三大核心项目-面试问答.md) — Claude Code 辅助开发秒杀/RAG/Agent 项目
- [前端必做实战项目-面试问答](./前端必做实战项目-面试问答.md) — Claude Code 也能辅助前端代码生成
- 软件工程 — AI 辅助开发是软件工程效率提升的新范式
