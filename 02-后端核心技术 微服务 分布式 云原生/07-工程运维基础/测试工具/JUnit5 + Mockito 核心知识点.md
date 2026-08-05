# JUnit 5 + Mockito 核心知识点
> JUnit 5 是 Java 单元测试的事实标准，Mockito 是 Java 最流行的 Mock 框架。二者组合构成 Java 后端测试的核心工具链，结合 Spring Boot Test 实现分层自动化测试。

## 目录
1. [概述](#1-概述)
2. [JUnit 5 核心](#2-junit-5-核心)
3. [Mockito 核心](#3-mockito-核心)
4. [分层测试实战](#4-分层测试实战)
5. [Hamcrest Matchers 参考表](#5-hamcrest-matchers-参考表)
6. [常见陷阱与 Mockito 5.x](#6-常见陷阱与-mockito-5x)
7. [测试最佳实践](#7-测试最佳实践)
8. [总结](#8-总结)

---

## 1. 概述

**核心定位：** JUnit 5 写测试结构 + Mockito 模拟外部依赖，Java 测试标配组合。

| 维度 | JUnit 5 | Mockito |
|------|---------|---------|
| 职责 | 测试生命周期、断言、参数化 | 模拟依赖、行为验证、Stub |
| 版本 | 5.10+ (2024-2026) | 5.x (Inline Mock Maker) |
| 核心注解 | `@Test`, `@BeforeEach`, `@ParameterizedTest` | `@Mock`, `@Spy`, `@InjectMocks`, `@Captor` |
| Spring 集成 | `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest` | `MockitoExtension` + `@MockitoBean` |

> 💡 2026 年 AI 生成测试代码已显著降低测试编写成本，GitHub Copilot / 通义灵码 可节约约 40% 的测试编写时间，但人工审查逻辑正确性仍是必要环节。

---

## 2. JUnit 5 核心

### 2.1 基础注解

| 注解 | 说明 |
|------|------|
| `@Test` | 标记测试方法 |
| `@BeforeEach` | 每个测试前执行 |
| `@AfterEach` | 每个测试后执行 |
| `@BeforeAll` | 所有测试前执行一次（必须 static） |
| `@AfterAll` | 所有测试后执行一次（必须 static） |
| `@DisplayName` | 测试可读名称 |
| `@Disabled` | 跳过测试 |
| `@ParameterizedTest` | 参数化测试 |
| `@RepeatedTest(n)` | 重复执行 n 次 |
| `@Nested` | 嵌套测试类（内部类） |
| `@Tag` | 测试标签，用于过滤执行 |
| `@Timeout` | 超时控制 |

### 2.2 断言

```java
// 标准断言
assertEquals(expected, actual);
assertTrue(condition);
assertNotNull(object);
assertThrows(IllegalArgumentException.class, () -> service.doWork(null));

// 分组断言 —— 所有断言都会执行，Collect 所有失败信息
assertAll("user",
    () -> assertEquals("张三", user.getName()),
    () -> assertTrue(user.getAge() > 0),
    () -> assertNotNull(user.getEmail())
);

// 超时断言
assertTimeout(ofSeconds(2), () -> service.longRunningOp());

// 断言集合/行匹配
assertLinesMatch(expectedLines, actualLines);
```

### 2.3 参数化测试

```java
@ParameterizedTest
@CsvSource({
    "1, 2, 3",
    "0, 0, 0",
    "-1, 1, 0"
})
void testAdd(int a, int b, int expected) {
    assertEquals(expected, calculator.add(a, b));
}

@ParameterizedTest
@MethodSource("provideTestData")
void testWithMethod(String input, boolean expected) {
    assertEquals(expected, validator.isValid(input));
}

static Stream<Arguments> provideTestData() {
    return Stream.of(
        Arguments.of("valid@email.com", true),
        Arguments.of("invalid", false),
        Arguments.of("", false),
        Arguments.of(null, false)
    );
}

// ValueSource —— 简单值列表
@ParameterizedTest
@ValueSource(strings = { "racecar", "radar", "level" })
void testPalindrome(String word) {
    assertTrue(StringUtils.isPalindrome(word));
}

// EnumSource —— 枚举遍历
@ParameterizedTest
@EnumSource(OrderStatus.class)
void testStatusTransition(OrderStatus status) {
    assertNotNull(status.canTransitionTo(OrderStatus.CANCELLED));
}
```

### 2.4 测试生命周期

```java
class OrderServiceTest {

    @BeforeAll
    static void initDatabase() {
        // 启动测试数据库容器（如 Testcontainers）
    }

    @BeforeEach
    void setUp() {
        // 每个测试前清理数据、重置 Mock
    }

    @Test
    void createOrder_ValidInput_Success() {
        // Given-When-Then 结构
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
    }

    @AfterAll
    static void shutdown() {
        // 停止测试数据库容器
    }
}
```

### 2.5 JUnit 5 Extension 模型

JUnit 5 用 `Extension` 取代了 JUnit 4 的 `@RunWith` 和自定义 Runner，通过 `@ExtendWith` 注册扩展。

| 扩展接口 | 作用 |
|---------|------|
| `TestInstancePostProcessor` | 测试实例创建后回调，用于注入依赖 |
| `TestExecutionExceptionHandler` | 处理测试执行中抛出的异常 |
| `BeforeEachCallback` / `AfterEachCallback` | 每个测试前后回调 |
| `BeforeAllCallback` / `AfterAllCallback` | 所有测试前后回调 |
| `ParameterResolver` | 解析测试方法参数（如随机数、Mock） |

```java
// 自定义 Extension —— 异常处理
public class IgnoreIOExceptionExtension implements TestExecutionExceptionHandler {
    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable)
            throws Throwable {
        if (throwable instanceof IOException) {
            System.out.println("忽略 IOException: " + throwable.getMessage());
            return;  // 吞掉异常，测试继续
        }
        throw throwable;  // 非 IO 异常继续抛出
    }
}

@ExtendWith(IgnoreIOExceptionExtension.class)
@Test
void testReadFile() {
    // 即使抛出 IOException 也不会失败
    fileService.readFromExternal();
}
```

```java
// 自定义 Extension —— 测试实例后处理
public class LoggingExtension implements TestInstancePostProcessor {
    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        System.out.println(">>> 创建测试实例: " + testInstance.getClass().getSimpleName());
    }
}
```

> 💡 `@ExtendWith(MockitoExtension.class)` 是 Mockito 提供的 Extension，自动初始化 `@Mock`、`@Spy`、`@InjectMocks` 等注解，无需手动调用 `MockitoAnnotations.openMocks(this)`。

### 2.6 @Nested 嵌套测试

`@Nested` 用于在内部类中组织测试，按场景分组，增强可读性。

```java
class UserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    @Nested
    @DisplayName("查询用户")
    class FindUser {

        @Test
        void findById_Exist_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(new User("张三")));
            User user = userService.findById(1L);
            assertEquals("张三", user.getName());
        }

        @Test
        void findById_NotExist_ThrowsException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(UserNotFoundException.class, () -> userService.findById(99L));
        }
    }

    @Nested
    @DisplayName("创建用户")
    class CreateUser {

        @Test
        void createUser_ValidData_Success() {
            User input = new User("李四");
            when(userRepository.save(any())).thenReturn(input);
            User result = userService.createUser(input);
            assertNotNull(result);
            verify(userRepository).save(input);
        }

        @Test
        void createUser_NullName_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> userService.createUser(null));
        }
    }
}
```

> 💡 `@Nested` 内部类可以不使用 `static`，内部类中的 `@BeforeEach` / `@AfterEach` 按外层 -> 内层顺序执行。

### 2.7 测试生命周期与 Spring Context 缓存

```text
Spring 测试框架在整个测试执行期间缓存 ApplicationContext，
相同的配置（@ContextConfiguration 的 locations / classes / initializers 相同）
复用同一个上下文，避免重复启动。

缓存 key = (locations, classes, activeProfiles, propertySourceLocations, ...)
```

```java
@SpringBootTest(classes = TestConfig.class)
class OrderServiceTest1 { /* 启动 Context A */ }

@SpringBootTest(classes = TestConfig.class)
class OrderServiceTest2 { /* 复用 Context A */ }

@SpringBootTest(classes = DifferentConfig.class)
class OrderServiceTest3 { /* 启动 Context B */ }
```

> ⚠️ 使用 `@MockBean` / `@DirtiesContext` 会导致上下文缓存失效，影响测试执行速度。优先使用 `@WebMvcTest` / `@DataJpaTest` 等切片测试替代 `@SpringBootTest` 以减少上下文启动开销。

---

## 3. Mockito 核心

### 3.1 基本用法

```java
// 创建 Mock
@Mock
private UserRepository userRepository;

@InjectMocks  // 自动注入 Mock 依赖
private UserService userService;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
}

@Test
void testFindUser() {
    // Given：设置 Mock 行为
    User mockUser = new User(1L, "张三");
    when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

    // When：执行被测方法
    User result = userService.getUser(1L);

    // Then：验证结果
    assertEquals("张三", result.getName());
    verify(userRepository).findById(1L);       // 验证调用
    verify(userRepository, never()).findAll();  // 验证未调用
}
```

### 3.2 参数匹配器

```java
when(userRepository.findByName(anyString())).thenReturn(mockUser);
when(userRepository.save(any(User.class))).thenReturn(mockUser);

// 自定义匹配器
when(orderService.create(argThat(order ->
    order.getAmount().compareTo(BigDecimal.ZERO) > 0))).thenReturn(true);

// 混合精确值与匹配器（需要 eq() 包装）
when(repository.findByIdAndName(eq(1L), anyString())).thenReturn(user);
```

### 3.3 验证调用

```java
// 精确次数
verify(repository, times(3)).save(any());
verify(repository, atLeastOnce()).findById(any());
verify(repository, atMost(5)).delete(any());

// 从未调用
verify(repository, never()).deleteAll();

// 调用顺序
InOrder inOrder = inOrder(repository);
inOrder.verify(repository).findById(1L);
inOrder.verify(repository).save(any());

// 超时验证 —— 等待异步结果最多 1 秒
verify(repository, timeout(1000)).findById(1L);
verify(repository, timeout(1000).times(2)).save(any());
```

### 3.4 Stub 高级用法

```java
// 链式返回值 —— 多次调用返回不同结果
when(repository.findById(1L))
    .thenReturn(Optional.of(user1))
    .thenReturn(Optional.of(user2))
    .thenThrow(new RuntimeException("数据库异常"));

// 基于参数返回值
when(repository.findById(anyLong())).thenAnswer(invocation -> {
    Long id = invocation.getArgument(0);
    return id > 0 ? Optional.of(new User("用户" + id)) : Optional.empty();
});

// void 方法
doThrow(new RuntimeException()).when(repository).delete(any());

// 真实参数捕获 —— 获取 save 方法保存的实际对象
ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
verify(repository).save(captor.capture());
User saved = captor.getValue();
```

### 3.5 Spy（部分 Mock）

```java
@Spy
private UserService userService;

@Test
void testPartialMock() {
    // 部分方法使用真实实现，部分 Mock
    when(userService.getRealData()).thenCallRealMethod();
    doReturn(mockResult).when(userService).externalCall();

    // 其他方法使用真实逻辑
    String result = userService.process();  // process() 内部调 externalCall()
}
```

### 3.6 @InjectMocks 与 @Captor / ArgumentCaptor 详解

#### @InjectMocks 注入策略

`@InjectMocks` 按以下优先级将 `@Mock` / `@Spy` 注入到被测对象：

| 注入方式 | 说明 | 优先级 |
|---------|------|--------|
| 构造函数注入 | 优先使用构造参数最大的构造函数 | 1 |
| Setter 注入 | 按类型匹配的 setter 方法 | 2 |
| 字段注入 | 按类型/名称匹配的私有字段 | 3 |

```java
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;

    // 构造函数 —— @InjectMocks 优先选择此方式
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
}

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    // 通过构造函数自动注入上面两个 Mock
    @InjectMocks private UserService userService;
}
```

> 💡 Mockito 通过**类型匹配**注入，若有多个同类型 Mock，使用 `@Mock(name = "xxx")` 按字段名匹配。

#### ArgumentCaptor 详例

```java
@Test
void testArgumentCaptor() {
    // 执行
    userService.registerUser("张三", "zhangsan@email.com");

    // 捕获 save 参数
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();

    // 验证属性
    assertEquals("张三", savedUser.getName());
    assertEquals("zhangsan@email.com", savedUser.getEmail());
    assertNotNull(savedUser.getCreatedAt());

    // 捕获多次调用 —— 返回所有捕获值
    ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailService, times(2)).sendEmail(emailCaptor.capture());
    List<String> allEmails = emailCaptor.getAllValues();
    assertEquals(2, allEmails.size());
}
```

### 3.7 BDDMockito 风格

Mockito 提供了 `BDDMockito` 类，使用 `given` / `when` / `then` 风格使测试更贴近 BDD 语义。

```java
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceBDDTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    @Test
    void findUser_ValidId_ReturnsUser() {
        // Given —— 设置 Mock（替代 when().thenReturn()）
        User mockUser = new User(1L, "张三");
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));

        // When —— 执行被测方法
        User result = userService.getUser(1L);

        // Then —— 验证（替代 verify()）
        assertEquals("张三", result.getName());
        then(userRepository).should(times(1)).findById(1L);
        then(userRepository).should(never()).findAll();
    }

    @Test
    void deleteUser_NotExist_ThrowsException() {
        // Given
        willThrow(new UserNotFoundException("用户不存在"))
            .given(userRepository).deleteById(99L);

        // When / Then
        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(99L));
    }
}
```

| BDDMockito | 传统 Mockito |
|-----------|-------------|
| `given(mock.method()).willReturn(x)` | `when(mock.method()).thenReturn(x)` |
| `willThrow(e).given(mock).method()` | `doThrow(e).when(mock).method()` |
| `then(mock).should().method()` | `verify(mock).method()` |
| `then(mock).should(times(n)).method()` | `verify(mock, times(n)).method()` |

### 3.8 doThrow / doReturn / doAnswer 详解

这些方法专用于无法使用 `when().thenReturn()` 的场景，如 void 方法、Spy 对象。

| 方法 | 适用场景 | 示例 |
|------|---------|------|
| `doThrow()` | void 方法抛异常 | `doThrow(new RuntimeException()).when(mock).delete(any())` |
| `doReturn()` | Spy 覆盖某方法返回值 | `doReturn(result).when(spy).compute()` |
| `doAnswer()` | 需根据参数动态返回 | `doAnswer(inv -> inv.getArgument(0) + " processed").when(service).process(any())` |
| `doNothing()` | void 方法静默跳过 | `doNothing().when(mock).sendEmail(any())` |
| `doCallRealMethod()` | Mock 对象调用真实方法 | `doCallRealMethod().when(mock).realMethod()` |

```java
@Test
void testDoFamily() {
    // doThrow —— void 方法抛异常
    doThrow(new DataAccessException("DB down"))
        .when(userRepository).deleteById(anyLong());
    assertThrows(DataAccessException.class, () -> userService.deleteUser(1L));

    // doReturn —— Spy 覆盖特定方法
    UserService spy = spy(userService);
    doReturn("mocked-result").when(spy).externalApiCall();
    assertEquals("mocked-result", spy.processWithExternalCall());

    // doAnswer —— 根据输入返回
    doAnswer(invocation -> {
        String input = invocation.getArgument(0);
        return input.toUpperCase();
    }).when(transformer).transform(anyString());
    assertEquals("HELLO", transformer.transform("hello"));

    // doNothing —— 静默 void 方法
    doNothing().when(emailService).sendWelcomeEmail(any(User.class));
    userService.register(new User("李四"));
    verify(emailService, times(1)).sendWelcomeEmail(any(User.class));
}
```

### 3.9 Verify 进阶

```java
// 次数验证
verify(mock, times(3)).method();        // 精确 3 次
verify(mock, never()).method();         // 0 次
verify(mock, atLeastOnce()).method();   // >= 1 次
verify(mock, atLeast(2)).method();      // >= 2 次
verify(mock, atMost(5)).method();       // <= 5 次
verify(mock, only()).method();          // 仅调用了此方法，无其他交互
verify(mock, timeout(500)).method();    // 500ms 内至少调用 1 次
verify(mock, timeout(1000).times(2)).method(); // 1000ms 内精确调用 2 次

// 零交互验证 —— 确认未与 Mock 发生任何交互
verifyNoInteractions(userRepository);
verifyNoMoreInteractions(userRepository); // 除已 verify 的方法外无其他交互
```

> 💡 `verifyNoMoreInteractions()` 是"严格的验证"，强烈建议单元测试中每个测试方法只验证相关的 Mock 调用，避免过度验证导致维护困难。

### 3.10 @Spy vs @Mock 对比

| 维度 | @Mock | @Spy |
|------|-------|------|
| 行为 | 全部方法返回默认值（null, 0, false） | 方法默认走真实实现 |
| 创建对象 | 无需构造参数，由 Mockito 创建 | 需目标类有默认构造或手动传入实例 |
| 控制粒度 | 所有方法都必须 Stub | 只 Stub 需要 Mock 的方法，其余用真逻辑 |
| 使用场景 | 外部依赖（Repository, Client, Service） | 测试类本身或复杂类（需保留部分逻辑） |
| 风险 | 隐性契约：未 Stub 的方法返回默认值 | 真实逻辑可能产生副作用或依赖外部资源 |
| 性能 | 轻量 | 稍重（需执行真实方法） |

```java
// @Mock —— 所有方法返回默认值
@Mock List<String> mockList;
mockList.add("A");                // 无效果，返回 false
mockList.get(0);                  // 返回 null
assertEquals(0, mockList.size()); // true —— size() 返回 0

// @Spy —— 保留真实行为
@Spy List<String> spyList = new ArrayList<>();
spyList.add("A");                 // 真的添加元素
assertEquals(1, spyList.size());  // true

// Spy 覆盖部分行为
doReturn(100).when(spyList).size();
assertEquals(100, spyList.size());  // 覆盖后返回 Mock 值
```

> ⚠️ Spy 使用 `doReturn()` / `doThrow()` 而非 `when().thenReturn()` 以避免真实方法被提前执行。`when(spy.method()).thenReturn(x)` 会导致 `spy.method()` 先真实执行一次，若方法有副作用则会产生问题。

---

## 4. 分层测试实战

### 4.1 Controller 层测试 —— @WebMvcTest

`@WebMvcTest` 仅加载 Web 层 Bean，不加载完整 ApplicationContext，速度更快。

```java
@WebMvcTest(UserController.class)   // 仅加载 UserController
@ExtendWith(MockitoExtension.class)  // 实际由 @WebMvcTest 自带 MockitoExtension
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean  // Spring Boot 3.4+，替代 @MockBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/users/1 返回用户 JSON")
    void getUserById_Exists_ReturnsOk() throws Exception {
        // Given
        User user = new User(1L, "张三");
        when(userService.findById(1L)).thenReturn(user);

        // When & Then
        mockMvc.perform(get("/api/users/{id}", 1L)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("张三"))
            .andExpect(jsonPath("$.id").value(1));

        verify(userService).findById(1L);
    }

    @Test
    @DisplayName("POST /api/users 创建用户")
    void createUser_ValidBody_ReturnsCreated() throws Exception {
        // Given
        User input = new User("新用户");
        when(userService.createUser(any(User.class))).thenReturn(input);

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"新用户\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /api/users/999 不存在返回 404")
    void getUserById_NotExists_ReturnsNotFound() throws Exception {
        // Given
        when(userService.findById(999L)).thenThrow(new UserNotFoundException("ID 不存在"));

        // When & Then
        mockMvc.perform(get("/api/users/{id}", 999L))
            .andExpect(status().isNotFound());
    }
}
```

| MockMvc 方法 | 说明 |
|-------------|------|
| `perform(get(url))` | 执行 GET 请求 |
| `perform(post(url))` | 执行 POST 请求 |
| `andExpect(status().isOk())` | 验证 HTTP 状态码 |
| `andExpect(jsonPath("$.key").value(v))` | 验证 JSON 响应字段 |
| `andExpect(content().string(containsString("xxx")))` | 验证响应体包含字符串 |
| `andExpect(header().string("Location", containsString("/users/1")))` | 验证响应头 |

### 4.2 Service 层测试 —— @ExtendWith(MockitoExtension.class)

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("注册用户：保存用户并发送欢迎邮件")
    void registerUser_ValidUser_Success() {
        // Given
        User input = new User("王五", "wangwu@email.com");
        User savedUser = new User(1L, "王五", "wangwu@email.com");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // When
        User result = userService.registerUser(input);

        // Then
        assertAll("验证注册结果",
            () -> assertEquals("王五", result.getName()),
            () -> assertEquals("wangwu@email.com", result.getEmail()),
            () -> assertNotNull(result.getId())
        );
        then(userRepository).should(times(1)).save(any(User.class));
        then(emailService).should(times(1)).sendWelcomeEmail(savedUser);
    }

    @Test
    @DisplayName("注册用户：重复邮箱抛异常")
    void registerUser_DuplicateEmail_ThrowsException() {
        // Given
        User input = new User("赵六", "exists@email.com");
        given(userRepository.findByEmail("exists@email.com"))
            .willReturn(Optional.of(new User("existing")));

        // When & Then
        assertThrows(DuplicateEmailException.class,
            () -> userService.registerUser(input));
        then(userRepository).should(never()).save(any());
    }
}
```

### 4.3 Repository 层测试 —— @DataJpaTest

`@DataJpaTest` 仅加载 JPA 相关组件，使用内嵌数据库（默认 H2）进行测试，真实执行 SQL。

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)  // 使用 H2
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("保存并查询用户")
    void saveAndFindUser_Success() {
        // Given
        User user = new User("测试用户", "test@email.com");
        User saved = userRepository.save(user);

        // When
        Optional<User> found = userRepository.findById(saved.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals("测试用户", found.get().getName());
        assertEquals("test@email.com", found.get().getEmail());
    }

    @Test
    @DisplayName("按邮箱查询用户")
    void findByEmail_Exists_ReturnsUser() {
        // Given
        User user = new User("张三", "zhangsan@email.com");
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByEmail("zhangsan@email.com");

        // Then
        assertTrue(found.isPresent());
        assertEquals("张三", found.get().getName());
    }

    @Test
    @DisplayName("级联操作验证")
    void cascadeDelete_RemovesRelatedEntities() {
        // Given
        User user = new User("级联测试");
        user.addOrder(new Order("订单1"));
        userRepository.saveAndFlush(user);

        // When
        userRepository.deleteById(user.getId());

        // Then
        assertTrue(userRepository.findById(user.getId()).isEmpty());
    }
}
```

| 注解 | 作用 |
|------|------|
| `@DataJpaTest` | 仅加载 JPA 组件，开启事务，测试后回滚 |
| `@AutoConfigureTestDatabase` | 配置测试数据库（默认 H2） |
| `@JdbcTest` | 仅加载 JDBC 组件（无 JPA） |
| `@MybatisTest` | MyBatis 切片测试（需额外 starter） |

> 🌐 替代方案：**Testcontainers** 可在集成测试中使用真实 MySQL / PostgreSQL 容器替代 H2 内嵌数据库，适用于需要验证数据库方言特性的场景。

---

## 5. Hamcrest Matchers 参考表

Hamcrest 提供丰富的匹配器，可与 JUnit 5 `assertThat()` 或 Mockito 的 `argThat()` 配合使用。

```java
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
```

| 匹配器 | 示例 | 说明 |
|--------|------|------|
| `is(value)` | `assertThat(result, is(42))` | 精确相等（== 语义） |
| `equalTo(value)` | `assertThat(name, equalTo("张三"))` | equals() 相等 |
| `not(value)` | `assertThat(name, not("李四"))` | 否定 |
| `containsString(str)` | `assertThat(msg, containsString("error"))` | 包含子串 |
| `startsWith(str)` | `assertThat(url, startsWith("https"))` | 前缀匹配 |
| `endsWith(str)` | `assertThat(file, endsWith(".pdf"))` | 后缀匹配 |
| `hasSize(n)` | `assertThat(list, hasSize(3))` | 集合大小为 n |
| `hasItem(item)` | `assertThat(list, hasItem("A"))` | 集合包含元素 |
| `containsInAnyOrder(a, b)` | `assertThat(list, containsInAnyOrder("B", "A"))` | 无序包含所有元素 |
| `greaterThan(n)` | `assertThat(score, greaterThan(60))` | 大于 |
| `lessThanOrEqualTo(n)` | `assertThat(age, lessThanOrEqualTo(150))` | 小于等于 |
| `closeTo(val, delta)` | `assertThat(pi, closeTo(3.14, 0.01))` | 浮点数近似相等 |
| `instanceOf(Class)` | `assertThat(ex, instanceOf(RuntimeException.class))` | 类型匹配 |
| `allOf(m1, m2)` | `assertThat(str, allOf(notNullValue(), containsString("OK")))` | AND 逻辑组合 |
| `anyOf(m1, m2)` | `assertThat(code, anyOf(is(200), is(201)))` | OR 逻辑组合 |

```java
// 组合匹配示例
assertThat(response, allOf(
    hasProperty("status", is(200)),
    hasProperty("body", containsString("success"))
));

assertThat(userList, containsInAnyOrder(
    hasProperty("name", is("张三")),
    hasProperty("name", is("李四"))
));
```

---

## 6. 常见陷阱与 Mockito 5.x

### 6.1 静态方法 Mock（Mockito 5.x + Inline Mock Maker）

传统 Mockito 无法 Mock 静态方法。从 Mockito 3.4.0 开始引入 **Inline Mock Maker**（基于 ByteBuddy + Objenesis 的 Instrumentation 机制），Mockito 5.x 默认启用。

```java
// 需要 mockito-inline 依赖（Mockito 5.x 默认内置）

@Test
void testStaticMethodMock() {
    try (MockedStatic<IdGenerator> mocked = mockStatic(IdGenerator.class)) {
        // Given —— 对静态方法设桩
        mocked.when(IdGenerator::nextId).thenReturn(100L);

        // When
        Long id = orderService.generateOrderId();

        // Then
        assertEquals(100L, id);
        mocked.verify(IdGenerator::nextId, times(1));
    }
    // MockedStatic 在 try-with-resources 结束后自动释放
}
```

### 6.2 Final 类 / 方法 Mock

Mockito 5.x 的 Inline Mock Maker 默认支持 Mock final 类和方法，无需额外配置。

```java
// 不需要 special 设置 —— Mockito 5.x 开箱即用
public final class FinalApiClient {
    public final String call(String input) {
        // ... 真实外部调用
    }
}

@Mock FinalApiClient finalApiClient;

@Test
void testFinalClass() {
    // Mockito 5.x 可以直接 Mock
    given(finalApiClient.call(anyString())).willReturn("mocked-response");
    assertEquals("mocked-response", finalApiClient.call("real-request"));
}
```

### 6.3 构造函数 Mock

```java
// 使用 Mockito 5.x + Inline Mock Maker
@Test
void testConstructorMock() {
    try (MockedConstruction<HeavyDependency> mocked =
             mockConstruction(HeavyDependency.class)) {

        // 构造 HeavyDependency 时返回 Mock 实例
        HeavyDependency mockDependency = new HeavyDependency();
        when(mockDependency.execute()).thenReturn("mocked");

        // 被测类内部 new HeavyDependency()
        String result = service.process();

        assertEquals("mocked", result);
    }
}
```

### 6.4 常见陷阱汇总

| 陷阱 | 说明 | 解决方案 |
|------|------|---------|
| 静态方法未 Mock 返回真实值 | 静态方法调用实际逻辑 | 使用 `MockedStatic` + `try-with-resources` |
| `when().thenReturn()` 在 Spy 上提前执行 | `spy.method()` 在 Stub 前真实执行 | 改用 `doReturn().when(spy)` |
| Mock 参数未使用 `eq()` | 混合精确值与匹配器时报错 | 使用 `eq(1L)` 包裹精确值 |
| `@Mock` 未初始化 | 报 NPE | 加 `@ExtendWith(MockitoExtension.class)` 或 `MockitoAnnotations.openMocks(this)` |
| 过多 Mock 导致测试脆弱 | 每改一行代码测测就崩 | 只 Mock 直接依赖，避免过度细粒度验证 |
| 使用 `@SpringBootTest` 启动整个应用 | 测试慢 | 使用 `@WebMvcTest` / `@DataJpaTest` 等切片测试 |
| `verifyNoMoreInteractions()` 滥用 | 新增方法导致老测试失败 | 只在关键场景使用，每个方法只验证相关 Mock |
| 时间敏感测试依赖 `System.currentTimeMillis()` | 测试结果不稳定 | 使用 `MockedStatic<Instant>` 模拟时间，或 Clock 参数注入 |

---

## 7. 测试最佳实践

### 7.1 命名规范

```text
方法名_测试场景_预期结果

示例：
createOrder_WithInvalidQuantity_ThrowsException
findUser_WhenUserNotExist_ReturnsEmpty
refundOrder_AlreadyRefunded_ReturnsFalse
deleteUser_AdminUser_Success
```

### 7.2 Given-When-Then 模式

```java
@Test
void refundOrder_PaidOrder_Success() {
    // Given（准备数据 + Mock 行为）
    Order paidOrder = OrderFixture.paidOrder();
    when(paymentGateway.refund(any())).thenReturn(true);

    // When（执行被测方法）
    RefundResult result = orderService.refund(paidOrder.getId());

    // Then（验证结果）
    assertEquals(RefundStatus.SUCCESS, result.getStatus());
    verify(paymentGateway).refund(paidOrder.getAmount());
}
```

### 7.3 不要 Mock 什么

- **值对象（VO/DTO/Entity）**：直接 new，不 Mock
- **JDK 核心类**：String、List、Map 等 —— 使用真实实例
- **被测类**：不要 Mock 你正在测试的类（使用 Spy 时谨慎）
- **第三方库的内部 API**：Mock API 的接口/抽象类，而非具体实现内部细节

### 7.4 AI 生成测试

```text
GitHub Copilot / 通义灵码：
选中方法 -> 右键 -> 生成单元测试 -> 审查 + 补充边界用例

AI 优势：快速生成骨架、参数组合、边界值
人工审查要点：
- 业务逻辑是否正确覆盖
- Mock 设置是否合理
- 断言是否足够严格
- 是否存在过度 Mock
```

### 7.5 FIRST 原则

测试应遵循 **FIRST** 五个核心原则：

| 原则 | 含义 | 实践要点 |
|------|------|---------|
| **F**ast（快速） | 测试应快速执行 | 避免数据库/网络调用；使用 Mock / 内嵌数据库；切片测试 |
| **I**ndependent（独立） | 测试之间无依赖 | 不共享可变状态；每个测试独立 setUp；可任意顺序执行 |
| **R**epeatable（可重复） | 任何环境执行结果相同 | Mock 时间/随机数；不依赖外部服务；避免静态可变状态 |
| **S**elf-validating（自验证） | 测试自行判断通过/失败 | 使用断言自动验证；无需人工检查日志或文件 |
| **T**imely（及时） | 测试与生产代码同步 | TDD 理念：先写测试再写实现；随代码变更同步更新测试 |

```java
// 符合 FIRST 原则的测试
@Test
void calculateDiscount_StandardCustomer_AppliesTenPercent() {
    // Fast: 无外部依赖
    // Independent: 只使用方法局部数据
    // Repeatable: 无时间/随机依赖
    Customer customer = new Customer("标准用户", CustomerType.STANDARD);
    BigDecimal discount = pricingService.calculateDiscount(customer);

    // Self-validating: 断言自动判断
    assertEquals(new BigDecimal("0.10"), discount);
}
```

### 7.6 测试金字塔实践

```text
       ╱╲
      ╱ E2E ╲          <-- 少量（~5%）：端到端集成测试，验证关键业务流程
     ╱────────╲
    ╱  服务层   ╲       <-- 适量（~20%）：Service 层测试，@SpringBootTest
   ╱──────────────╲
  ╱   单元测试     ╲    <-- 大量（~75%）：方法级别，@ExtendWith + @Mock
 ╱────────────────────╲
```

> 💡 实际项目中 75% 单元测试 + 20% 服务层集成测试 + 5% E2E 测试为推荐比例。AI 可大幅降低单元测试编写成本，使开发者将更多精力放在关键业务流程的 E2E 测试上。

---

## 8. 总结

- **JUnit 5**：`@Test` + `@ParameterizedTest` + Assertions 体系 + Extension 模型 + `@Nested`
- **Mockito**：`@Mock` + `@Spy` + `@InjectMocks` + `BDDMockito` + ArgumentCaptor + verify 进阶
- **分层测试**：`@WebMvcTest`（Controller）、`@ExtendWith(MockitoExtension.class)`（Service）、`@DataJpaTest`（Repository）
- **Mockito 5.x**：Inline Mock Maker 默认支持 final 类、静态方法、构造函数 Mock
- **原则**：Given-When-Then 结构、FIRST 原则、只 Mock 外部依赖、AI 生成 + 人工审查
