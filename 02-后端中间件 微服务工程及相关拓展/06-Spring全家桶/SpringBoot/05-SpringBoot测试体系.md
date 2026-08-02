# 05 - Spring Boot 测试体系

> 定位：@SpringBootTest、分层测试切片、MockMvc、Mockito、Testcontainers、测试隔离——Boot 测试全解

## 📚 目录

1. [测试分层与切片](#1-测试分层与切片)
2. [单元测试（Mockito）](#2-单元测试mockito)
3. [Web 层测试（MockMvc）](#3-web-层测试mockmvc)
4. [集成测试（@SpringBootTest）](#4-集成测试springboottest)
5. [Testcontainers 真实环境](#5-testcontainers-真实环境)
6. [测试隔离与性能](#6-测试隔离与性能)

---

## 1. 测试分层与切片

```
Boot 测试金字塔（与通用测试体系呼应）：
  单元测试：Service/Mapper（Mockito mock 依赖）
  Web 层：Controller（MockMvc，mock Service）
  集成测试：@SpringBootTest（真实容器）

⚠️ 切片测试（Boot 特性）：
  @WebMvcTest：只加载 MVC 层（快）
  @DataJpaTest：只加载 JPA 层（快）
  @SpringBootTest：全量加载（慢，集成用）

⚠️ 面试必答：
"Boot 切片测试 = 只加载被测层——
 @WebMvcTest（Controller）、@DataJpaTest（JPA）、
 @SpringBootTest（全量）。切片快、全量准。"
```

---

## 2. 单元测试（Mockito）

```java
// Service 层单元测试：mock 依赖、验证逻辑
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void 用户不存在时抛业务异常() {
        // Given：mock 返回空
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then：断言异常
        assertThrows(BizException.class, () -> userService.getById(1L));
        // 验证异常消息
        BizException e = assertThrows(BizException.class,
                () -> userService.getById(1L));
        assertEquals("用户不存在", e.getMessage());
    }

    @Test
    void 创建用户时校验唯一性并保存() {
        // 行为验证
        verify(userRepository, times(1)).save(any(User.class));
    }
}
```

> 🎯 **要点**：单元测试 = @Mock 依赖 + when 打桩 + verify 验证 + assertThrows 断言。**只测当前类逻辑**（依赖全部 mock）。

---

## 3. Web 层测试（MockMvc）

```java
// Controller 层测试：MockMvc 模拟 HTTP 请求
@WebMvcTest(UserController.class)              // ⚠️ 切片：只加载 MVC
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean                              // ⚠️ mock Service 层
    private UserService userService;

    @Test
    void 查询用户成功返回JSON() throws Exception {
        // Given：mock Service
        when(userService.getById(1L))
            .thenReturn(new UserVO(1L, "张三"));

        // When/Then：模拟 GET 请求 + 断言
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())                        // 200
            .andExpect(jsonPath("$.name").value("张三"))       // JSON 断言
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void 参数校验失败返回400() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))                 // 空名
            .andExpect(status().isBadRequest())               // 校验失败
            .andExpect(jsonPath("$.code").value(400));
    }
}
```

> 🎯 **要点**：MockMvc = 模拟 HTTP + jsonPath 断言（状态码/字段/结构）。`@MockBean` 隔离 Service——**Controller 测试只验证"参数绑定 + 响应格式"**。

---

## 4. 集成测试（@SpringBootTest）

```java
// ⚠️ 集成测试：真实容器 + 真实依赖（推荐 Testcontainers）
@SpringBootTest
@AutoConfigureMockMvc                       // 带 MockMvc
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 创建用户后数据库可查询() throws Exception {
        // ① HTTP 创建
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"李四\",\"age\":30}"))
            .andExpect(status().isCreated());

        // ② 数据库验证（真实持久化）
        User saved = userRepository.findByName("李四").get(0);
        assertEquals("李四", saved.getName());
    }
}
```

```
⚠️ 集成测试要点：
  ✅ 测试真实链路（HTTP → Controller → Service → DB）
  ✅ 数据库用 Testcontainers（真实 MySQL）
  ✅ 事务回滚隔离（@Transactional 测试方法自动回滚）
  ⚠️ 慢（全量上下文）→ 数量控制在 20% 以内
```

---

## 5. Testcontainers 真实环境

```java
// ⚠️ Testcontainers：真实中间件容器（2026 标准）
@SpringBootTest
@Testcontainers
class OrderIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    void 订单流程完整可用() {
        // 真实 MySQL + Redis 环境验证
    }
}
```

> 🎯 **要点**：Testcontainers 解决"测试环境与生产不一致"——容器启动真实 DB/MQ/Redis。`@DynamicPropertySource` 动态注入连接信息是标准姿势。

---

## 6. 测试隔离与性能

### 6.1 隔离策略

```
测试隔离三层面：
  ① 数据隔离：每测试回滚（@Transactional）
  ② 上下文复用：相同配置的测试共享容器（启动加速）
  ③ 并行测试：无共享状态时并行（-Dparallel）

⚠️ 面试必答：
"测试隔离 = 数据回滚 + 上下文缓存 +
 并行化；Boot 测试上下文缓存
 让多个测试类共享同一个容器（启动一次）。"
```

### 6.2 测试性能优化

```
⚠️ 慢测试的三大来源：
  ① @SpringBootTest 全量上下文（用切片替代）
  ② 每次都启动容器（用缓存/单测容器）
  ③ 数据准备慢（用 @Sql 初始化 / Builder 工具）

优化手段：
  ✅ 能用切片不用全量（@WebMvcTest vs @SpringBootTest）
  ✅ 单元测试 mock（不启动容器）
  ✅ 集成测试 Testcontainers 复用
  ✅ CI 并行执行
```

---

> 🎯 **核心要点**：测试体系 = **切片分层**（单元/Web/集成 + @WebMvcTest/@DataJpaTest 切片）+ **Mockito**（mock + verify + 断言）+ **MockMvc**（HTTP 模拟 + jsonPath）+ **@SpringBootTest**（真实链路）+ **Testcontainers**（真实中间件）+ **隔离优化**（回滚/缓存/并行）。"切片优先、真实环境验证、隔离并行"是 Boot 测试三原则。

---

**返回总览**：[00-SpringBoot总览与核心概念](00-SpringBoot总览与核心概念.md) | **上一篇**：[04-SpringBoot数据访问](04-SpringBoot数据访问.md) | **下一篇**：[06-SpringBoot安全与认证](06-SpringBoot安全与认证.md)
