# JUnit 5 + Mockito 核心知识点

## 一、概述

JUnit 5 是 Java 单元测试的事实标准，Mockito 是 Java 最流行的 Mock 框架。二者组合构成 Java 后端测试的核心工具链。2026 年，AI 生成测试代码已显著降低测试编写成本。

**核心定位：** JUnit 5 写测试结构 + Mockito 模拟外部依赖，Java 测试标配组合。

## 二、JUnit 5 核心

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

### 2.2 断言

```java
// 标准断言
assertEquals(expected, actual);
assertTrue(condition);
assertNotNull(object);
assertThrows(IllegalArgumentException.class, () -> service.doWork(null));

// 分组断言
assertAll("user",
    () -> assertEquals("张三", user.getName()),
    () -> assertTrue(user.getAge() > 0)
);

// 断言集合
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
        Arguments.of("invalid", false)
    );
}
```

### 2.4 测试生命周期

```java
class OrderServiceTest {

    @BeforeAll
    static void initDatabase() {
        // 启动测试数据库容器
    }

    @BeforeEach
    void setUp() {
        // 每个测试前清理数据
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

## 三、Mockito 核心

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
when(orderService.create(argThat(order -> 
    order.getAmount().compareTo(BigDecimal.ZERO) > 0))).thenReturn(true);
```

### 3.3 验证调用

```java
// 精确次数
verify(repository, times(3)).save(any());
verify(repository, atLeastOnce()).findById(any());
verify(repository, atMost(5)).delete(any());

// 调用顺序
InOrder inOrder = inOrder(repository);
inOrder.verify(repository).findById(1L);
inOrder.verify(repository).save(any());
```

### 3.4 Stub 高级用法

```java
// 链式返回值
when(repository.findById(1L))
    .thenReturn(Optional.of(user1))
    .thenReturn(Optional.of(user2))
    .thenThrow(new RuntimeException("数据库异常"));

// void 方法
doThrow(new RuntimeException()).when(repository).delete(any());

// 真实参数捕获
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
    // 部分方法使用真实实现
    doReturn(mockResult).when(userService).externalCall();
    
    // 其他方法使用真实逻辑
    String result = userService.process();  // process() 内部调 externalCall()
}
```

## 四、最佳实践

### 4.1 命名规范

```
方法名_测试场景_预期结果
例：createOrder_WithInvalidQuantity_ThrowsException
```

### 4.2 Given-When-Then 模式

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

### 4.3 不要 Mock 什么

- **值对象（VO/DTO/Entity）**：直接 new，不 Mock
- **JDK 核心类**：String、List 等
- **被测类**：不要 Mock 你正在测试的类
- **第三方库的内部 API**：Mock API 的接口/抽象类

### 4.4 AI 生成测试

```
GitHub Copilot / 通义灵码：
选中方法 → 右键 → 生成单元测试 → 审查 + 补充边界用例
AI 可节省约 40% 测试编写时间，但需人工审查逻辑正确性
```

## 五、总结

- **JUnit 5**：`@Test` + `@ParameterizedTest` + Assertions 体系
- **Mockito**：`@Mock` + `when().thenReturn()` + `verify()`
- **原则**：Given-When-Then 结构、只 Mock 外部依赖、AI 生成 + 人工审查
