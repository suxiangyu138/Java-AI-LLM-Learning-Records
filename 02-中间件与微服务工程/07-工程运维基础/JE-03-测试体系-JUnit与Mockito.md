# 03-测试体系-JUnit与Mockito.md -- Testing: JUnit & Mockito

> **从"不写测试"到"TDD 驱动开发"——构建稳固的测试防线**

---

## 目录

1. [测试金字塔与哲学](#1-测试金字塔与哲学)
2. [JUnit 5 (Jupiter) 深度指南](#2-junit-5-jupiter-深度指南)
3. [Mockito 深度指南](#3-mockito-深度指南)
4. [Spring Boot 集成测试](#4-spring-boot-集成测试)
5. [测试数据管理](#5-测试数据管理)
6. [测试覆盖率](#6-测试覆盖率)
7. [变异测试（Mutation Testing）](#7-变异测试mutation-testing)
8. [TDD 测试驱动开发](#8-tdd-测试驱动开发)
9. [测试命名规范](#9-测试命名规范)
10. [持续测试](#10-持续测试)
11. [面试高频题](#11-面试高频题)

---

## 1. 测试金字塔与哲学

### 1.1 测试金字塔

```
            ╱╲
           ╱  ╲          E2E Tests (端到端测试)
          ╱    ╲         - 模拟真实用户操作
         ╱      ╲        - 最慢、最脆弱、成本最高
        ╱────────╲
       ╱          ╲      Integration Tests (集成测试)
      ╱            ╲     - 测试组件间的交互
     ╱              ╲    - 数据库、外部服务集成
    ╱────────────────╲
   ╱                  ╲  Unit Tests (单元测试)
  ╱                    ╲ - 测试单个组件/方法
 ╱                      ╲- 最快、最可靠、成本最低
╱────────────────────────╲
    越多越好 ←────── 数量/覆盖率
```

**推荐比例 (70/20/10)：**
| 层 | 比例 | 运行时间 | 维护成本 | 定位问题的精度 |
|----|------|---------|---------|-------------|
| Unit | 70% | 毫秒级 | 低 | 高（精确定位） |
| Integration | 20% | 秒级 | 中 | 中 |
| E2E | 10% | 分钟级 | 高 | 低（只能定位到功能） |

### 1.2 FIRST 原则

| 原则 | 说明 | 反例 |
|------|------|------|
| **F**ast (快速) | 测试应该快速运行，鼓励频繁执行 | 单元测试依赖数据库 |
| **I**solated (隔离) | 测试不应相互依赖，可以独立运行 | 测试共享静态变量状态 |
| **R**epeatable (可重复) | 任何环境运行结果一致 | 测试依赖当前时间/随机数 |
| **S**elf-validating (自验证) | 测试自动判断通过/失败，无需人工检查 | 只输出日志不 assert |
| **T**imely (及时) | 测试应该在写业务代码之前或同时编写 | 上线后才补测试 |

### 1.3 测试行为而非实现

```java
// ❌ 错误的做法：测试实现细节
@Test
void saveUser_shouldCallRepository() {
    userService.saveUser(user);
    verify(userRepository).save(user);  // 测试了具体实现
}

// ✅ 正确的做法：测试行为结果
@Test
void saveUser_shouldReturnUserWithId() {
    User saved = userService.saveUser(user);
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
}
```

**为什么要测试行为而不是实现：**
1. 重构时实现会变，但行为保持不变
2. 测试实现细节导致重构时需要大量修改测试
3. 行为测试 catch 更多真正的 bug

---

## 2. JUnit 5 (Jupiter) 深度指南

### 2.1 架构概览

JUnit 5 = JUnit Platform + JUnit Jupiter + JUnit Vintage

```
┌─────────────────────────────────────────────┐
│               IDE / Build Tools              │
│         (IntelliJ, Eclipse, Maven, Gradle)    │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│            JUnit Platform                    │
│  - TestEngine API                           │
│  - Launcher API                             │
│  - Console / IDE Launcher                   │
└──────┬──────────────────┬──────────────────┘
       │                  │
┌──────▼──────┐   ┌──────▼──────┐
│JUnit Jupiter│   │JUnit Vintage│
│ (JUnit 5)   │   │ (JUnit 3/4) │
└─────────────┘   └─────────────┘
```

**Maven 依赖：**

```xml
<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.1</version>
    <scope>test</scope>
</dependency>

<!-- 如果需要 JUnit 4 兼容 -->
<dependency>
    <groupId>org.junit.vintage</groupId>
    <artifactId>junit-vintage-engine</artifactId>
    <version>5.10.1</version>
    <scope>test</scope>
</dependency>
```

**Gradle 依赖：**

```groovy
test {
    useJUnitPlatform()
}

dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
}
```

### 2.2 核心注解

| 注解 | 说明 |
|------|------|
| `@Test` | 标记为测试方法 |
| `@BeforeEach` | 每个测试方法前执行 |
| `@AfterEach` | 每个测试方法后执行 |
| `@BeforeAll` | 所有测试前执行一次（static） |
| `@AfterAll` | 所有测试后执行一次（static） |
| `@DisplayName` | 测试显示名称 |
| `@Disabled` | 禁用测试 |
| `@Nested` | 内嵌测试类 |
| `@Tag` | 测试分类标签 |
| `@TestInstance` | 测试实例生命周期 |

### 2.3 完整示例

```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("用户服务测试")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {

    private UserService userService;

    @BeforeAll
    static void initAll() {
        // 在所有测试前执行 (static)
        System.out.println("=== Starting UserService Tests ===");
    }

    @BeforeEach
    void setUp() {
        // 每个测试方法前执行
        userService = new UserService();
    }

    @Test
    @DisplayName("创建用户成功")
    @Tag("smoke")
    void createUser_shouldSucceed() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");

        // Act
        User user = userService.createUser(request);

        // Assert
        assertAll("用户属性验证",
            () -> assertNotNull(user.getId(), "ID 不能为空"),
            () -> assertEquals("testuser", user.getUsername()),
            () -> assertEquals("test@example.com", user.getEmail()),
            () -> assertNotNull(user.getCreatedAt(), "创建时间不能为空")
        );
    }

    @Test
    @DisplayName("创建重复用户名抛出异常")
    void createUser_duplicateUsername_shouldThrow() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existing");

        // 验证异常
        DuplicateUsernameException exception = assertThrows(
            DuplicateUsernameException.class,
            () -> userService.createUser(request)
        );

        assertEquals("Username already exists: existing", exception.getMessage());
    }

    @AfterEach
    void tearDown() {
        // 每个测试方法后执行
        userService = null;
    }

    @AfterAll
    static void tearDownAll() {
        // 在所有测试后执行 (static)
        System.out.println("=== Completed UserService Tests ===");
    }
}
```

### 2.4 断言 (Assertions)

```java
// 基本断言
assertEquals(42, result);
assertNotEquals("wrong", result);
assertTrue(condition);
assertFalse(condition);
assertNull(object);
assertNotNull(object);
assertSame(expected, actual);    // 引用相等 (==)
assertNotSame(expected, actual);
assertDoesNotThrow(() -> method());

// 聚合断言 - 所有断言都会执行，不会在第一个失败时停止
assertAll("user",
    () -> assertEquals("Alice", user.getName()),
    () -> assertTrue(user.isActive()),
    () -> assertNotNull(user.getEmail())
);

// 异常断言
Throwable exception = assertThrows(IllegalArgumentException.class,
    () -> { throw new IllegalArgumentException("invalid"); });
assertEquals("invalid", exception.getMessage());

// 超时断言
assertTimeout(Duration.ofMillis(100), () -> {
    // 如果超时也会等待执行完毕
    Thread.sleep(50);
});

assertTimeoutPreemptively(Duration.ofMillis(100), () -> {
    // 超时会立即中断（有线程安全风险）
    Thread.sleep(50);
});

// 集合断言
assertIterableEquals(Arrays.asList(1, 2, 3), list);
assertLinesMatch(Arrays.asList("a.*", "b.*"), lines);  // 支持正则匹配
```

### 2.5 假设 (Assumptions)

假设用于跳过特定条件下的测试，而不是标记为失败：

```java
@Test
void testForProductionOnly() {
    assumeTrue("prod".equals(System.getenv("ENV")),
        "Skipping: not in production");
    // ... 只在生产环境执行的测试
}

@Test
void testOnWindows() {
    assumeFalse(System.getProperty("os.name").contains("Linux"));
    // ... 非 Linux 环境的测试
}

@Test
void testWithJava17() {
    assumeThat(Runtime.version().feature(), greaterThanOrEqualTo(17));
    // ... 需要 Java 17+ 的测试
}
```

### 2.6 参数化测试

```java
@ParameterizedTest
@ValueSource(strings = {"racecar", "radar", "level"})
void testPalindrome(String word) {
    assertTrue(StringUtils.isPalindrome(word));
}

@ParameterizedTest
@ValueSource(ints = {1, 2, 3, 4, 5})
void testPositive(int number) {
    assertTrue(number > 0);
}

// CSV 数据源
@ParameterizedTest
@CsvSource({
    "1, 2, 3",
    "4, 5, 9",
    "10, -5, 5"
})
void testAdd(int a, int b, int expected) {
    assertEquals(expected, calculator.add(a, b));
}

// CSV 文件数据源 (classpath:test-data.csv)
@ParameterizedTest
@CsvFileSource(resources = "/test-data.csv", numLinesToSkip = 1)
void testFromCSV(int a, int b, int expected) {
    assertEquals(expected, calculator.add(a, b));
}

// 方法数据源
@ParameterizedTest
@MethodSource("provideUsers")
void testUserCreation(User user) {
    assertNotNull(userService.createUser(user));
}

static Stream<Arguments> provideUsers() {
    return Stream.of(
        Arguments.of(new User("Alice", "alice@test.com")),
        Arguments.of(new User("Bob", "bob@test.com")),
        Arguments.of(new User("Charlie", "charlie@test.com"))
    );
}

// 自定义 ArgumentsProvider
@ParameterizedTest
@ArgumentsSource(CustomArgumentProvider.class)
void testWithCustomProvider(String input, int expected) {
    assertEquals(expected, input.length());
}

static class CustomArgumentProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
            Arguments.of("hello", 5),
            Arguments.of("world", 5)
        );
    }
}

// Null 和 Empty 参数
@ParameterizedTest
@NullSource
@EmptySource
@ValueSource(strings = {" ", "  "})
void testNullOrBlank(String input) {
    assertTrue(input == null || input.trim().isEmpty());
}

// 枚举参数
@ParameterizedTest
@EnumSource(TimeUnit.class)
void testAllTimeUnits(TimeUnit unit) {
    assertNotNull(unit.name());
}

@ParameterizedTest
@EnumSource(value = TimeUnit.class, names = {"DAYS", "HOURS"})
void testSpecificTimeUnits(TimeUnit unit) {
    assertTrue(unit == TimeUnit.DAYS || unit == TimeUnit.HOURS);
}
```

### 2.7 重复测试

```java
@RepeatedTest(value = 10, name = "{displayName} ({currentRepetition}/{totalRepetitions})")
@DisplayName("随机数生成测试")
void testRandomGenerator(RepetitionInfo repetitionInfo) {
    // 验证随机数生成器在多次运行中不会卡顿
    int result = randomGenerator.nextInt(100);
    assertTrue(result >= 0 && result < 100);
}

@RepeatedTest(5)
void testWithRepetitionInfo(RepetitionInfo repetitionInfo) {
    System.out.println("Current: " + repetitionInfo.getCurrentRepetition()
        + "/" + repetitionInfo.getTotalRepetitions());
}
```

### 2.8 动态测试 (TestFactory)

动态测试在运行时生成，不是编译时定义的：

```java
@TestFactory
Collection<DynamicTest> testDynamicCollection() {
    return Arrays.asList(
        DynamicTest.dynamicTest("加法测试",
            () -> assertEquals(3, calculator.add(1, 2))),
        DynamicTest.dynamicTest("减法测试",
            () -> assertEquals(1, calculator.subtract(2, 1)))
    );
}

@TestFactory
Stream<DynamicNode> testDynamicNodes() {
    return Stream.of("apple", "banana", "cherry")
        .map(fruit -> DynamicContainer.dynamicContainer(
            "测试水果: " + fruit,
            Stream.of(
                DynamicTest.dynamicTest("不为空",
                    () -> assertNotNull(fruit)),
                DynamicTest.dynamicTest("长度大于2",
                    () -> assertTrue(fruit.length() > 2))
            )
        ));
}
```

### 2.9 测试生命周期与实例创建

```java
// 默认: PER_METHOD — 每个测试方法创建新的测试类实例
// 每个测试方法前都会执行 @BeforeEach

// 可选: PER_CLASS — 测试类只创建一次实例
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SharedStateTest {
    private int counter = 0;

    @Test
    void first() {
        assertEquals(0, counter++);
    }

    @Test
    void second() {
        assertEquals(1, counter++);  // 共享 counter 状态
    }
}
```

**PER_METHOD vs PER_CLASS：**

| 特性 | PER_METHOD | PER_CLASS |
|------|-----------|-----------|
| 默认 | 是 (JUnit 5 默认) | 否 |
| 隔离性 | 完全隔离 | 共享状态（需要小心） |
| @BeforeAll/@AfterAll | 需要 static | 不需要 static |
| 适用场景 | 大多数测试 | 共享初始化资源 |
| @Nested 测试 | 不支持 @BeforeAll | 支持 |

### 2.10 Extension 模型

Extension 是 JUnit 5 最重要的扩展机制，替代了 JUnit 4 的 Runner 和 Rule。

```java
// 1. 自定义 Extension — 测试实例后处理
public class DatabaseExtension implements TestInstancePostProcessor {
    @Override
    public void postProcessTestInstance(Object testInstance,
                                        ExtensionContext context) {
        // 为测试类注入数据库连接
        Arrays.stream(testInstance.getClass().getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(InjectDatabase.class))
            .forEach(f -> {
                f.setAccessible(true);
                try {
                    f.set(testInstance, new DatabaseConnection("jdbc:h2:mem:test"));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
    }
}

// 2. 自定义 Extension — BeforeEach 回调
public class TimingExtension implements BeforeEachCallback, AfterEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) {
        context.getStore(ExtensionContext.Namespace.create(getClass()))
            .put("startTime", System.currentTimeMillis());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        long startTime = context.getStore(
            ExtensionContext.Namespace.create(getClass())
        ).remove("startTime", long.class);
        long duration = System.currentTimeMillis() - startTime;
        System.out.println(
            context.getDisplayName() + " took " + duration + "ms"
        );
    }
}

// 3. 自定义 ParameterResolver
public class RandomParamResolver implements ParameterResolver {
    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) {
        return parameterContext.getParameter()
            .isAnnotationPresent(RandomValue.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) {
        return ThreadLocalRandom.current().nextInt(1, 100);
    }
}

// 使用自定义 Extension
@ExtendWith({DatabaseExtension.class, TimingExtension.class})
class UserRepositoryTest {

    @InjectDatabase
    private DatabaseConnection connection;

    @Test
    void testConnection(@RandomValue int randomValue) {
        assertTrue(connection.isConnected());
        System.out.println("Random value: " + randomValue);
    }
}
```

### 2.11 条件测试执行

```java
// 操作系统条件
@Test
@EnabledOnOs(OS.WINDOWS)
void testOnlyOnWindows() {
}

@Test
@DisabledOnOs(OS.LINUX)
void testNotOnLinux() {
}

// JRE 版本条件
@Test
@EnabledOnJre(JRE.JAVA_17)
void testOnlyOnJava17() {
}

@Test
@DisabledForJreRange(min = JRE.JAVA_8, max = JRE.JAVA_11)
void testNotOnOldJava() {
}

// 环境变量条件
@Test
@EnabledIfEnvironmentVariable(named = "ENV", matches = "integration")
void testOnlyInIntegration() {
}

@Test
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
void testSkipOnCI() {
}

// 系统属性条件
@Test
@EnabledIfSystemProperty(named = "feature.flag", matches = "true")
void testFeatureFlag() {
}

// 自定义条件
@Test
@EnabledIf("customCondition")
void testCustomCondition() {
}

boolean customCondition() {
    return System.currentTimeMillis() % 2 == 0;
}

// 基于配置的条件
@Test
@EnabledIf("${testing.integration.enabled}")
void testConfigControlled() {
}
```

### 2.12 测试执行顺序

```java
// 按方法名排序
@TestMethodOrder(MethodOrderer.MethodName.class)
class AlphabeticalTest {
    @Test void testB() { /* runs second */ }
    @Test void testA() { /* runs first  */ }
    @Test void testC() { /* runs third  */ }
}

// 按 @Order 注解排序
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderedTest {
    @Test @Order(3) void testC() { }
    @Test @Order(1) void testA() { }
    @Test @Order(2) void testB() { }
}

// 随机排序（暴露测试间的依赖问题）
@TestMethodOrder(MethodOrderer.Random.class)
class RandomOrderTest {
    @Test void testA() { }
    @Test void testB() { }
    @Test void testC() { }
}
```

### 2.13 测试套件 (Suite)

```java
// JUnit 5.8+ 支持的 @Suite
import org.junit.platform.suite.api.*;

@Suite
@SelectPackages("com.company.service")
@IncludeTags("smoke")
@ExcludeTags("slow")
@SelectClasses({UserServiceTest.class, PaymentServiceTest.class})
@IncludeClassNamePatterns(".*Test")
@ExcludeClassNamePatterns(".*IntegrationTest")
@SuiteDisplayName("Smoke Test Suite")
class SmokeTestSuite {
    // 不需要任何代码
}
```

---

## 3. Mockito 深度指南

### 3.1 Mock 创建

```java
// 方式 1: mock() 方法
UserRepository userRepository = mock(UserRepository.class);

// 方式 2: @Mock 注解
@Mock
private UserRepository userRepository;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
}

// 方式 3: MockitoExtension (推荐)
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks  // 自动注入 mock
    private UserService userService;
}

// Mock 默认行为
// - 所有方法返回默认值 (null, 0, false, empty collection)
// - void 方法什么都不做
// - 不会抛出异常
```

### 3.2 Stubbing

```java
// 基本的 when-thenReturn
when(userRepository.findById(1L)).thenReturn(Optional.of(user));
when(userRepository.findById(999L)).thenReturn(Optional.empty());

// 抛出异常
when(userRepository.save(any())).thenThrow(new DataAccessException("DB error"));

// 链式调用: 不同参数返回不同值
when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

// 连续调用: 同一个方法多次调用返回不同值
when(userRepository.findAll())
    .thenReturn(List.of(user1))
    .thenReturn(List.of(user1, user2));

// 使用 thenAnswer: 根据输入计算返回值
when(userRepository.save(any())).thenAnswer(invocation -> {
    User user = invocation.getArgument(0);
    user.setId(ThreadLocalRandom.current().nextLong());
    return user;
});

// void 方法的 stubbing
doThrow(new RuntimeException()).when(userRepository).delete(any());
doNothing().when(userRepository).delete(any());

// doReturn: 可以用于 spy 或 when 不方便的场景
doReturn(Optional.of(user)).when(userRepository).findById(1L);

// 动态响应
when(calculator.add(anyInt(), anyInt())).thenAnswer(inv -> {
    int a = inv.getArgument(0);
    int b = inv.getArgument(1);
    return a + b;
});
```

### 3.3 Argument Matchers

```java
// 精确匹配
when(userRepository.findById(1L)).thenReturn(Optional.of(user));

// 任意匹配
when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
when(userRepository.findByUsername(anyString())).thenReturn(user);
when(userRepository.findByAge(anyInt())).thenReturn(users);
when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));

// 混合使用（必须全部使用 matcher 或全部使用精确值）
when(userRepository.findByNameAndAge(anyString(), anyInt()))
    .thenReturn(users);
// 不能: when(service.find(anyString(), 25)) — 编译错误
// 修复: when(service.find(anyString(), eq(25)))

// 集合匹配器
when(userRepository.findAllById(anyList())).thenReturn(users);
when(userRepository.findAllById(anySet())).thenReturn(users);
when(userRepository.findAllById(anyCollection())).thenReturn(users);

// 自定义 ArgumentMatcher
when(userRepository.findByUsername(argThat(name ->
    name != null && name.length() >= 3
))).thenReturn(user);

// 自定义 Matcher 类
public class ValidEmailMatcher implements ArgumentMatcher<String> {
    @Override
    public boolean matches(String email) {
        return email != null && email.contains("@");
    }
}
when(userRepository.findByEmail(argThat(new ValidEmailMatcher())))
    .thenReturn(user);
```

### 3.4 Verification

```java
// 基本验证
verify(userRepository).save(user);
verify(userRepository, times(1)).save(user);    // 默认就是 times(1)

// 验证调用次数
verify(userRepository, times(3)).save(any());
verify(userRepository, never()).delete(any());
verify(userRepository, atLeast(1)).findById(anyLong());
verify(userRepository, atMost(5)).findAll();
verify(userRepository, atLeastOnce()).save(any());
verify(userRepository, atMostOnce()).deleteAll();

// 验证特定参数
verify(userRepository).findById(1L);
verify(userRepository).findByUsername("admin");

// 验证超时
verify(userRepository, timeout(100)).save(any());
verify(userRepository, timeout(100).times(2)).findById(anyLong());

// 验证无更多交互
verifyNoMoreInteractions(userRepository);
verifyNoInteractions(emailService);  // emailService 完全没有被调用

// 验证调用顺序
InOrder inOrder = inOrder(userRepository, emailService);
inOrder.verify(userRepository).save(user);
inOrder.verify(emailService).sendWelcomeEmail(user);
inOrder.verify(userRepository).findById(user.getId());
```

### 3.5 Spying

```java
// Spy: 部分 mock，保留真实行为但可以 stubbing
List<String> list = new ArrayList<>();
List<String> spyList = spy(list);

// 使用注解
@Spy
private List<String> spyList = new ArrayList<>();

// 保留真实行为
spyList.add("A");
assertEquals(1, spyList.size());  // 真实行为

// Stub 部分方法
doReturn(100).when(spyList).size();  // 注意：spy 要用 doReturn
assertEquals(100, spyList.size());   // stub 行为

// Spy 的注意事项
// 1. 对 spy 对象使用 when().thenReturn() 会调用真实方法
// 2. 推荐使用 doReturn().when() 避免副作用
// 3. spy 的构造器会被调用（如果没有默认构造器）

// 正确: doReturn + when
doReturn(fixedList).when(userRepository).findAll();

// 错误: when + thenReturn (会调用真实方法)
// when(userRepository.findAll()).thenReturn(fixedList);
```

### 3.6 Argument Captor

```java
// 捕获方法调用参数，用于后续验证
@Captor
private ArgumentCaptor<User> userCaptor;

// 或手动创建
ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

@Test
void testArgumentCapture() {
    // 执行
    userService.createUser(request);

    // 验证并捕获参数
    verify(userRepository).save(userCaptor.capture());

    // 验证捕获的参数
    User capturedUser = userCaptor.getValue();
    assertEquals("testuser", capturedUser.getUsername());
    assertNotNull(capturedUser.getCreatedAt());

    // 捕获多次调用
    verify(userRepository, times(2)).save(userCaptor.capture());
    List<User> allUsers = userCaptor.getAllValues();
    assertEquals(2, allUsers.size());
}
```

### 3.7 BDD Style

```java
// Mockito BDD 风格 (Behavior-Driven Development)
import static org.mockito.BDDMockito.*;

@Test
void createUser_shouldReturnUser() {
    // Given
    CreateUserRequest request = new CreateUserRequest();
    request.setUsername("testuser");
    request.setEmail("test@example.com");

    User savedUser = new User();
    savedUser.setId(1L);
    savedUser.setUsername("testuser");

    given(userRepository.save(any())).willReturn(savedUser);

    // When
    User result = userService.createUser(request);

    // Then
    then(result).should().getId();
    then(result).should().getUsername();
    then(userRepository).should(times(1)).save(any());
}
```

### 3.8 Mocking Static Methods

```java
// Mockito 3.4+ 支持 mock 静态方法
@Test
void testStaticMock() {
    try (MockedStatic<IdGenerator> mockedStatic = mockStatic(IdGenerator.class)) {
        // Stub
        mockedStatic.when(IdGenerator::generateId).thenReturn(123L);
        mockedStatic.when(() -> IdGenerator.generateWithPrefix(anyString()))
            .thenReturn("PREFIX_123");

        // 测试
        Long id = IdGenerator.generateId();
        assertEquals(123L, id);

        // 验证
        mockedStatic.verify(IdGenerator::generateId, times(1));
    }
    // try-with-resources 自动释放 mock
}

// 验证特定 scope 内的调用
try (MockedStatic<IdGenerator> mockedStatic = mockStatic(IdGenerator.class,
    Mockito.CALLS_REAL_METHODS)) {  // 保留真实行为但可验证
    Long id = IdGenerator.generateId();
    mockedStatic.verify(IdGenerator::generateId);
}
```

### 3.9 Mocking Constructors

```java
// Mockito 3.5+ 支持 mock 构造器
@Test
void testConstructorMock() {
    try (MockedConstruction<UserService> mockedConstruction =
         mockConstruction(UserService.class)) {

        // 构造函数被 mock，不会真正执行
        UserService userService = new UserService(null);
        // userService 是 mock 对象

        // 验证构造器被调用
        mockedConstruction.constructed().shouldHaveSize(1);
    }
}
```

### 3.10 常见陷阱

```java
// 陷阱 1: Unnecessary Stubbing
// Mockito 默认会检查不必要的 stubbing
@Mock
private UserRepository userRepository;

@Test
void test() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    // 如果 userRepository.findById(1L) 从未被调用，测试会失败
    // 解决方案: @MockitoSettings(strictness = Strictness.LENIENT)
}

// 陷阱 2: Over-mocking
// 不要 mock 值对象、集合、POJO
@Test
void overMocking() {
    // ❌ 不需要 mock
    User user = mock(User.class);
    when(user.getUsername()).thenReturn("test");

    // ✅ 直接使用真实对象
    User user = new User();
    user.setUsername("test");
}

// 陷阱 3: 验证不必要的交互
@Test
void unnecessaryVerification() {
    // ❌
    verify(userRepository).findAll();
    verify(emailService).sendEmail(any());
    verifyNoMoreInteractions(userRepository, emailService);

    // ✅ 只验证关键交互
    verify(emailService).sendEmail(any());
}

// 陷阱 4: Mock 返回值类型方法但不 stub
@Test
void missingStub() {
    when(userRepository.findById(100L))
        .thenReturn(Optional.empty());  // 必须 stub
    // userRepository.findById(100L) 返回 null，导致 NPE
}
```

---

## 4. Spring Boot 集成测试

### 4.1 @SpringBootTest

```java
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
// webEnvironment 选项：
//   MOCK (default): 加载 Mock Servlet 环境，使用 MockMvc
//   RANDOM_PORT: 使用随机端口启动 Embedded Server
//   DEFINED_PORT: 使用配置的端口
//   NONE: 不启动 web 环境
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;  // 只在 MOCK 模式下可用

    @Test
    void getUser_shouldReturnUser() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void createUser_shouldReturnCreated() throws Exception {
        String requestBody = """
            {
                "username": "newuser",
                "email": "new@example.com"
            }
            """;

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"));
    }
}
```

### 4.2 TestRestTemplate 和 WebTestClient

```java
// TestRestTemplate (阻塞式)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UserControllerRestTemplateTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testGetUser() {
        ResponseEntity<User> response = restTemplate
            .getForEntity("/api/users/1", User.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
    }

    @Test
    void testCreateUser() {
        User request = new User("newuser", "new@example.com");

        ResponseEntity<User> response = restTemplate
            .postForEntity("/api/users", request, User.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}

// WebTestClient (响应式，也支持阻塞)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UserControllerWebClientTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    void testGetUser() {
        webClient.get().uri("/api/users/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.username").isEqualTo("testuser")
            .jsonPath("$.email").isEqualTo("test@example.com");
    }
}
```

### 4.3 @DataJpaTest

```java
@DataJpaTest  // 只加载 JPA 相关组件，不会加载整个 Spring 上下文
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
// 默认使用嵌入式内存数据库 (H2)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUsername_shouldReturnUser() {
        // 使用 TestEntityManager 准备数据
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        entityManager.persistAndFlush(user);

        // 测试
        User found = userRepository.findByUsername("testuser");
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexist@example.com");
        assertThat(found).isEmpty();
    }
}
```

### 4.4 Testcontainers

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

```java
// 单测试类中使用
@Testcontainers
class UserRepositoryIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void testWithRealDatabase() {
        User user = new User("testuser", "test@example.com");
        userRepository.save(user);

        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);
    }
}

// 共享容器（适用于多个测试类）
@Testcontainers
abstract class AbstractIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }
}

class UserRepositoryIT extends AbstractIntegrationTest { ... }
class OrderRepositoryIT extends AbstractIntegrationTest { ... }

// 通用 Testcontainers 模块
@Container
static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
    .withExposedPorts(6379);

@Container
static KafkaContainer kafka = new KafkaContainer(
    DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
```

### 4.5 MockMvc 控制器层测试

```java
@WebMvcTest(UserController.class)  // 只加载 Web 层
class UserControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean   // Spring Boot 的 Mock 注解，替换上下文中的 Bean
    private UserService userService;

    @Test
    void getUser_shouldReturnUser() throws Exception {
        // Stub
        User user = new User(1L, "testuser", "test@example.com");
        when(userService.findById(1L)).thenReturn(user);

        // Execute & Assert
        mockMvc.perform(get("/api/users/1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(userService).findById(1L);
    }

    @Test
    void getUser_shouldReturn404() throws Exception {
        when(userService.findById(999L))
            .thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(get("/api/users/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found: 999"));
    }

    @Test
    void createUser_validationError() throws Exception {
        String invalidBody = """
            {
                "username": "",
                "email": "invalid-email"
            }
            """;

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray());
    }
}
```

### 4.6 其他测试切片

```java
// JSON 序列化测试
@JsonTest
class UserJsonTest {

    @Autowired
    private JacksonTester<User> json;

    @Test
    void testSerialize() throws Exception {
        User user = new User(1L, "test", "test@example.com");

        assertThat(json.write(user)).isEqualToJson("""
            {"id":1,"username":"test","email":"test@example.com"}
            """);
    }

    @Test
    void testDeserialize() throws Exception {
        String content = """
            {"id":1,"username":"test","email":"test@example.com"}
            """;

        assertThat(json.parse(content))
            .hasFieldOrPropertyWithValue("username", "test");
    }
}

// Rest Client 测试
@RestClientTest(UserClient.class)
class UserClientTest {

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private UserClient userClient;

    @Test
    void testGetUser() {
        server.expect(requestTo("/api/users/1"))
            .andRespond(withSuccess("""
                {"id":1,"username":"test","email":"test@example.com"}
                """, MediaType.APPLICATION_JSON));

        User user = userClient.getUser(1L);
        assertThat(user.getUsername()).isEqualTo("test");
    }
}
```

---

## 5. 测试数据管理

### 5.1 Builder Pattern

```java
// 使用 Lombok @Builder
@Data
@Builder
public class User {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String avatar;
    private Integer age;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// 测试中创建对象
User user = User.builder()
    .username("testuser")
    .email("test@example.com")
    .age(25)
    .active(true)
    .build();

// 带默认值的 Builder
User user = User.builder().build();
// username = null, active = false, age = null ...
```

### 5.2 Test Fixtures

```java
// 测试固定件（Test Fixture）类
public class UserFixtures {

    public static User.UserBuilder defaultUser() {
        return User.builder()
            .username("default-user")
            .email("default@example.com")
            .age(25)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now());
    }

    public static User activeUser() {
        return defaultUser()
            .username("active-user")
            .active(true)
            .build();
    }

    public static User inactiveUser() {
        return defaultUser()
            .username("inactive-user")
            .active(false)
            .build();
    }

    public static User.UserBuilder admin() {
        return defaultUser()
            .username("admin")
            .email("admin@example.com");
    }

    public static List<User> userList(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> defaultUser()
                .username("user-" + i)
                .email("user" + i + "@example.com")
                .build())
            .collect(Collectors.toList());
    }
}

// 使用
User user = UserFixtures.activeUser();
List<User> users = UserFixtures.userList(5);
```

### 5.3 Object Mother Pattern

```java
// Object Mother: 创建特定测试场景的复杂对象
public class OrderObjectMother {

    public static Order.OrderBuilder standardOrder() {
        return Order.builder()
            .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8))
            .status(OrderStatus.PENDING)
            .totalAmount(new BigDecimal("100.00"))
            .items(List.of(OrderItemObjectMother.standardItem().build()));
    }

    public static Order.OrderBuilder paidOrder() {
        return standardOrder()
            .status(OrderStatus.PAID)
            .paidAt(LocalDateTime.now())
            .paymentMethod(PaymentMethod.WECHAT);
    }

    public static Order.OrderBuilder shippedOrder() {
        return paidOrder()
            .status(OrderStatus.SHIPPED)
            .shippedAt(LocalDateTime.now())
            .trackingNumber("SF1234567890");
    }

    public static Order.OrderBuilder largeOrder() {
        return standardOrder()
            .totalAmount(new BigDecimal("99999.99"))
            .items(IntStream.range(0, 50)
                .mapToObj(i -> OrderItemObjectMother.standardItem().build())
                .toList());
    }

    public static Order.OrderBuilder cancelledOrder() {
        return standardOrder()
            .status(OrderStatus.CANCELLED)
            .cancelledAt(LocalDateTime.now())
            .cancelReason("User requested cancellation");
    }
}
```

### 5.4 数据库隔离策略

```java
// 策略 1: @Transactional — 测试完成后自动回滚
@SpringBootTest
@Transactional  // 默认每个测试方法结束后回滚
class UserServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void test1() {
        userRepository.save(new User("test", "test@test.com"));
        assertThat(userRepository.count()).isEqualTo(1);
        // 测试结束后自动回滚，不影响 test2
    }

    @Test
    void test2() {
        assertThat(userRepository.count()).isEqualTo(0);  // 空的
    }
}

// 策略 2: 截断表
@SpringBootTest
@TestExecutionListeners(
    listeners = {TransactionalTestExecutionListener.class},
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTruncateTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("TRUNCATE TABLE orders");
    }
}

// 策略 3: 使用 @Sql 注解
@SpringBootTest
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserServiceSqlTest {

    @Test
    void testWithPreparedData() {
        // test-data.sql 已经插入了测试数据
        assertThat(userRepository.count()).isGreaterThan(0);
    }
}

// sql/test-data.sql
// INSERT INTO users (id, username, email) VALUES (1, 'alice', 'alice@test.com');
// INSERT INTO users (id, username, email) VALUES (2, 'bob', 'bob@test.com');
```

---

## 6. 测试覆盖率

### 6.1 JaCoCo 配置

```xml
<!-- Maven -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>prepare-package</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVERED_RATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVERED_RATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                            <limit>
                                <counter>CLASS</counter>
                                <value>MISSED_COUNT</value>
                                <maximum>0</maximum>
                            </limit>
                        </limits>
                    </rule>
                    <!-- 排除不需要覆盖率的代码 -->
                    <rule>
                        <element>PACKAGE</element>
                        <includes>
                            <include>com.company.*.dto</include>
                            <include>com.company.*.config</include>
                        </includes>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVERED_RATIO</value>
                                <minimum>0.0</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
                <excludes>
                    <exclude>**/*Application.class</exclude>
                    <exclude>**/*Configuration.class</exclude>
                    <exclude>**/*Dto.class</exclude>
                    <exclude>**/model/**</exclude>
                </excludes>
            </configuration>
        </execution>
    </executions>
</plugin>
```

```bash
# 运行并生成覆盖率报告
mvn clean verify

# 仅生成报告（不执行 check）
mvn jacoco:report

# 跳过覆盖率检查
mvn clean verify -Djacoco.skip=true
```

### 6.2 覆盖率指标解读

| 指标 | 含义 | 目标值 |
|------|------|--------|
| Instruction Coverage | 字节码指令覆盖率 | > 80% |
| Line Coverage | 代码行覆盖率 | > 80% |
| Branch Coverage | 分支覆盖率（if/else, switch） | > 70% |
| Method Coverage | 方法覆盖率 | > 85% |
| Class Coverage | 类覆盖率 | > 95% |
| Cyclomatic Complexity | 圈复杂度 | < 10 |

### 6.3 应该覆盖什么

```java
// ✅ 需要高覆盖率
@Service
public class UserService {

    public User createUser(CreateUserRequest request) {
        // 1. 参数校验 → 测试
        if (request == null || request.getUsername() == null) {
            throw new IllegalArgumentException("Invalid request");
        }

        // 2. 重复检查 → 测试
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException(request.getUsername());
        }

        // 3. 核心业务逻辑 → 测试
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }
}

// ❌ 不需要高覆盖率
@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

// ➡️ 低优先级（有简单的测试即可）
// DTO、POJO、常量类
```

---

## 7. 变异测试（Mutation Testing）

### 7.1 概念

变异测试通过引入小的代码变更（变异体）来测试测试套件的质量。如果测试没有检测到变异，说明测试不够完善。

```xml
<!-- PITest Maven Plugin -->
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.15.0</version>
    <dependencies>
        <dependency>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-junit5-plugin</artifactId>
            <version>1.2.1</version>
        </dependency>
    </dependencies>
    <configuration>
        <targetClasses>
            <param>com.company.service.*</param>
        </targetClasses>
        <targetTests>
            <param>com.company.service.*</param>
        </targetTests>
        <mutationThreshold>80</mutationThreshold>  <!-- 变异覆盖率门禁 -->
        <timeoutConstant>3000</timeoutConstant>
        <threads>4</threads>
    </configuration>
</plugin>
```

```bash
# 运行变异测试
mvn org.pitest:pitest-maven:mutationCoverage

# 报告在 target/pit-reports/
```

### 7.2 变异算子示例

| 算子 | 原始代码 | 变异后 |
|------|---------|--------|
| 条件边界 | `a > b` | `a >= b` |
| 取反 | `a == b` | `a != b` |
| 返回值 | `return 42` | `return 0` |
| 删除调用 | `methodCall()` | (删除) |
| 成员变量赋值 | `field = x` | (删除) |

### 7.3 杀死变异体

```java
// 原始代码
public boolean isValidAge(int age) {
    return age >= 18 && age <= 120;
}

// ❌ 不够好的测试（不能杀死 ">" 变 ">=" 的变异体）
@Test
void testIsValidAge() {
    assertTrue(userService.isValidAge(25));  // 正常值
}

// ✅ 好的测试（能杀死所有常见变异体）
@ParameterizedTest
@CsvSource({
    "17, false",   // 边界以下
    "18, true",    // 下边界
    "19, true",    // 正常值
    "120, true",   // 上边界
    "121, false",  // 边界以上
    "0, false",    // 非法值
    "-1, false"    // 负数
})
void testIsValidAge(int age, boolean expected) {
    assertEquals(expected, userService.isValidAge(age));
}
```

---

## 8. TDD 测试驱动开发

### 8.1 TDD 红绿重构循环

```text
┌─────────────────────┐
│     写一个失败的测试   │  ← Red (写测试，先让它失败)
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│   让测试通过（最快方式） │  ← Green (写最简代码让测试通过)
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│     重构代码          │  ← Refactor (优化代码，保持测试通过)
└─────────┬───────────┘
          │
          └──→ 回到第一步 (下一个功能)
```

### 8.2 TDD 示例

```java
// Step 1: RED — 写一个失败的测试
class FizzBuzzTest {

    @Test
    void shouldReturnFizzForMultipleOf3() {
        assertEquals("Fizz", FizzBuzz.of(3));
    }
}

// Step 2: GREEN — 写最简单的代码让测试通过
class FizzBuzz {
    static String of(int number) {
        if (number == 3) return "Fizz";
        return String.valueOf(number);
    }
}

// Step 3: 添加更多测试，逐步完善
@Test
void shouldReturnBuzzForMultipleOf5() {
    assertEquals("Buzz", FizzBuzz.of(5));
}

@Test
void shouldReturnFizzBuzzForMultipleOf3And5() {
    assertEquals("FizzBuzz", FizzBuzz.of(15));
}

@Test
void shouldReturnNumberOtherwise() {
    assertEquals("1", FizzBuzz.of(1));
    assertEquals("2", FizzBuzz.of(2));
}

// Step 4: REFACTOR — 重构实现
class FizzBuzz {
    static String of(int number) {
        if (number % 15 == 0) return "FizzBuzz";
        if (number % 3 == 0) return "Fizz";
        if (number % 5 == 0) return "Buzz";
        return String.valueOf(number);
    }
}
```

### 8.3 TDD 最佳实践

| 原则 | 说明 |
|------|------|
| 先写测试再写代码 | 测试不是事后补的 |
| 只写刚好让测试通过的代码 | 不做无谓的提前设计 |
| 快速红绿循环 | 每个循环 5-10 分钟 |
| 测试驱动设计 | 好的 API 设计来自可测试性 |
| 保持测试简洁 | 一个测试只测一种行为 |

---

## 9. 测试命名规范

### 9.1 推荐命名方式

```
// 方式 1: methodName_condition_expectedResult (最常用)
@Test
void findById_userExists_returnsUser() { }
@Test
void findById_userNotExists_throwsException() { }

// 方式 2: should_expectedResult_when_condition
@Test
void should_returnUser_when_userExists() { }
@Test
void should_throwException_when_userNotFound() { }

// 方式 3: given_when_then (BDD 风格)
@Test
void given_validUser_when_createUser_then_returnUserWithId() { }
```

### 9.2 测试类命名

```
// 规范: {被测试类名}Test (单元测试)
UserServiceTest
UserRepositoryTest
PaymentServiceTest

// 规范: {被测试类名}IT (集成测试)
UserServiceIT
UserRepositoryIT

// 或者: {被测试类名}IntegrationTest
UserRepositoryIntegrationTest
```

### 9.3 测试方法结构 (AAA Pattern)

```java
@Test
@DisplayName("创建用户时，用户名已存在则抛出异常")
void createUser_duplicateUsername_shouldThrowException() {
    // Arrange — 准备测试数据
    CreateUserRequest request = new CreateUserRequest();
    request.setUsername("existingUser");
    when(userRepository.existsByUsername("existingUser")).thenReturn(true);

    // Act — 执行要测试的方法
    Executable action = () -> userService.createUser(request);

    // Assert — 验证结果
    assertThrows(DuplicateUsernameException.class, action);
    verify(userRepository, never()).save(any());
}
```

---

## 10. 持续测试

### 10.1 IDE 集成

```
IntelliJ IDEA:
- 右键测试类/方法 → Run/Debug
- Ctrl+Shift+F10: 运行上下文最近的测试
- Ctrl+Shift+R: 重新运行上次的测试
- 实时模板: "test" + Tab 生成 @Test 方法

VS Code:
- Test Runner 插件
- 左侧测试面板
```

### 10.2 CI 集成

```yaml
# GitHub Actions
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Run tests
        run: mvn test

      - name: Generate coverage report
        run: mvn jacoco:report

      - name: Publish report
        uses: actions/upload-artifact@v4
        with:
          name: coverage-report
          path: target/site/jacoco/
```

### 10.3 测试分类与过滤

```bash
# Maven: 运行特定 Tag 的测试
mvn test -Dgroups="smoke"
mvn test -Dgroups="smoke,regression"
mvn test -DexcludedGroups="slow,integration"

# Gradle
tasks.withType(Test) {
    useJUnitPlatform {
        includeTags 'smoke'
        excludeTags 'slow'
    }
}
```

---

## 11. 面试高频题

### 基础概念类

**Q: 测试金字塔是什么？为什么重要？**
A: 测试金字塔描述了测试的层次结构：单元测试、集成测试、E2E 测试。建议比例 70/20/10。它重要是因为指导团队把测试资源投放在 ROI 最高的地方——快速、稳定、精确定位的单元测试。

**Q: JUnit 5 的 Extension 模型和 JUnit 4 的 Runner/Rule 有什么区别？**
A: Extension 更灵活统一，一个类可以实现多个扩展接口（BeforeEachCallback, AfterEachCallback, ParameterResolver 等），而 Runner 一个类只能有一个。Extension 通过注册机制可以叠加。

**Q: Mockito 中 @Mock 和 @InjectMocks 的区别？**
A: @Mock 创建 mock 对象；@InjectMocks 创建真实对象并自动注入 @Mock 对象到该对象的属性中（通过构造器、setter 或字段注入）。

### 实战类

**Q: 如何 Mock 静态方法和构造器？**
A: Mockito 3.4+ 支持 `mockStatic()`，3.5+ 支持 `mockConstruction()`。需要用 try-with-resources 限定作用域。

**Q: @SpringBootTest 和 @WebMvcTest 的区别？**
A: @SpringBootTest 加载整个 Spring 上下文，适合集成测试；@WebMvcTest 只加载 Web 层（Controller、Advice、Filter），适合控制器层测试，速度更快。

**Q: 如何测试 Controller 层？有哪些方式？**
A: 三种方式：
1. MockMvc + @WebMvcTest（最快，只测 Web 层）
2. TestRestTemplate + @SpringBootTest(RANDOM_PORT)（集成测试）
3. WebTestClient（支持响应式和阻塞）

### 场景类

**Q: 如何处理测试中的 Flaky Test（不稳定测试）？**
A:
1. 标记为 @Disabled 立即修复
2. 常见原因：依赖外部服务、时间/随机数、测试顺序依赖
3. 使用 Testcontainers 稳定外部依赖
4. 避免 Thread.sleep，使用 Awaitility
5. 隔离测试数据，不要共享状态

**Q: 测试覆盖率很高但线上还是有 bug，可能的原因？**
A:
1. 只测了"快乐路径"，异常路径和边界条件没覆盖
2. Mock 行为与实际不符（Overspecification 或 stubbing 错误）
3. 集成问题没有被单元测试覆盖
4. 测试了实现而不是行为（重构后测试失去意义）
5. 并发/多线程场景没有测试

**Q: 如何为遗留项目（没有测试）补测试，应该优先补哪些？**
A:
1. 先补核心业务逻辑（Service 层）
2. 优先补经常出 Bug 的模块
3. 先补集成测试（验证对外部系统的交互正确）
4. 使用 Characterization Test（写测试捕获当前行为，然后重构）
5. 不要追求 100% 覆盖率，逐步改进

---

## 总结检查清单

- [ ] 测试金字塔比例是否合理 (70/20/10)
- [ ] 测试是否遵循 FIRST 原则
- [ ] JUnit 5 的高级特性是否掌握 (参数化、动态、Extension)
- [ ] Mockito 是否覆盖了所有常用场景
- [ ] Spring Boot 测试切片是否用到合适场景
- [ ] Testcontainers 是否用于集成测试
- [ ] 是否有 Test Fixture 或 Object Mother 简化数据准备
- [ ] JaCoCo 覆盖率门禁是否配置
- [ ] 是否了解变异测试概念
- [ ] TDD 循环是否练习过
- [ ] 测试命名是否规范
- [ ] CI 中是否包含测试环节
