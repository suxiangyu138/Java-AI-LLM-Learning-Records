# 06-Mock 与依赖隔离

> 单测的核心矛盾：被测单元需要依赖（DB、HTTP、消息队列），而单测必须快且确定。Mockito 5.x 与 Jupiter 原生集成，用 Mock/Stub/Spy 把依赖挡在测试边界之外。

## 📚 目录

1. [测试替身谱系](#1)
2. [Mockito 核心 API](#2)
3. [Mockito 与 Jupiter 集成](#3)
4. [静态方法与构造器 Mock](#4)
5. [验证与匹配器](#5)
6. [隔离边界与反模式](#6)

## 1. 测试替身谱系

| 替身 | 定义 | 典型实现 |
|------|------|---------|
| Dummy | 只占位，从不被调用 | `new User()` 空对象 |
| Fake | 有真实实现的简化版（如内存 HashMap 版 Repository） | 手写 |
| Stub | 预置返回值，不验证调用 | Mockito `when().thenReturn()` |
| Mock | 预置行为 + 验证交互 | Mockito `mock()` |
| Spy | 真实对象 + 选择性覆盖部分方法 | Mockito `spy()` |

> 💡 **Mock 不是万能**：进程内 Fake（内存版依赖）常比 Mock 更贴合真实语义、重构更稳；Mock 优先用于「外部边界」（HTTP/RPC/MQ/时间），Fake 用于「内部协作对象」（Repository 之类）的快速迭代。

## 2. Mockito 核心 API

### 2.1 基础 Mock 与 Stub

```java
UserService service = mock(UserService.class);

// Stub：预置返回值
when(service.findById(1L)).thenReturn(new User(1L, "zhang"));
when(service.findById(anyLong())).thenReturn(null);              // 兜底

// Stub：抛异常
when(service.findByName("")).thenThrow(IllegalArgumentException.class);

// Stub：动态应答
when(service.getScore(anyLong())).thenAnswer(inv -> 100 + inv.getArgument(0));
```

### 2.2 doReturn/doThrow：无返回值方法

```java
doNothing().when(mock).send(any(Message.class));
doThrow(new IOException("连接失败")).when(mock).connect();
```

> ⚠️ **何时用 do- 风格**：`when(mock.voidMethod())` 编译不过（void 无法套用）；以及**避免在 stub 时真实执行一次方法副作用**（`when(spy.realMethod())` 会先跑真实方法）——一律用 do- 风格更安全。

### 2.3 Verify 交互验证

```java
verify(mock).save(any(User.class));                       // 恰好调用 1 次
verify(mock, times(3)).send(any());                       // 恰好 3 次
verify(mock, atLeastOnce()).flush();                      // 至少 1 次
verify(mock, never()).close();                            // 从未调用
verify(mock, timeout(100).times(2)).poll(any());          // 100ms 内至少 2 次（异步场景）
```

### 2.4 参数匹配器

```java
when(service.find(anyLong(), eq("active"))).thenReturn(list);   // 混合：明确值必须用 eq()
verify(mock).process(argThat(u -> u.age() >= 18));              // 自定义谓词
verify(mock, never()).process(isNull());                        // null 校验
```

## 3. Mockito 与 Jupiter 集成

### 3.1 MockitoExtension + 注解注入

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private PaymentClient paymentClient;          // 自动创建 Mock

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private InventoryService inventoryService;    // 未 stub 返回智能空值而非 null

    @InjectMocks
    private OrderService orderService;            // 按类型+名称注入上述 Mock

    @BeforeEach
    void setUp() {
        orderService = new OrderService(paymentClient, inventoryService);  // 或显式构造
    }

    @Test
    void pay_success() {
        when(paymentClient.charge(any())).thenReturn(true);
        assertTrue(orderService.pay(100));
        verify(paymentClient).charge(any());
    }
}
```

### 3.2 严格桩（Strict Stubs）

MockitoExtension 默认开启严格模式：

- **UnnecessaryStubbingException**：定义了 stub 但测试未使用 → 报错提示清理（防止「僵尸 stub」掩盖行为变化）；
- **Argument mismatch 即时报错**：`when(x)` 与调用参数不匹配时立即抛出而非返回 null（默认 mock 行为）。

```java
// 整个类关闭严格模式（个别测试需要宽松桩时）
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LenientTest { }
```

### 3.3 Spy：真实对象 + 定点覆盖

```java
UserService real = new UserService(dao);
UserService spy = spy(real);

doReturn(bigList).when(spy).findRecent(anyInt());   // 只覆盖 findRecent，其余走真实实现
assertEquals(3, spy.count());                        // 真实方法
```

> ⚠️ Spy 的两个坑：`when(spy.realMethod())` 会先执行真实方法（用 `doReturn`）；final 方法/私有方法无法 spy（Mockito 5 内联 mock maker 可 mock final 类）。

## 4. 静态方法与构造器 Mock

Mockito 5.x（inline mock maker，默认）可 mock 静态方法与构造器：

```java
try (MockedStatic<UUID> mocked = mockStatic(UUID.class)) {
    mocked.when(UUID::randomUUID).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    assertEquals("0000...", idGenerator.next());
}

try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class, (m, ctx) -> {
    when(m.send(any(), any())).thenReturn(fakeResponse);
})) {
    new Caller().call();    // Caller 内部 new HttpClient() 被替换
}
```

**规则**：静态 mock 作用域严格限定在 try-with-resources 内，线程局部；**禁止把 `MockedStatic` 声明为字段**。静态方法 mock 是「最后手段」——能用构造函数注入改造的代码优先改造（可测试性设计）。

## 5. 验证与匹配器

### 5.1 进阶验证

```java
InOrder inOrder = inOrder(first, second);                    // 顺序验证
inOrder.verify(first).prepare();
inOrder.verify(second).execute();

verifyNoInteractions(unrelatedMock);                         // 确无交互
verifyNoMoreInteractions(mock);                              // 除已验证外无多余交互（过度使用=脆弱）

ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
verify(service).save(captor.capture());
assertEquals("zhang", captor.getValue().name());             // 捕获参数做深度断言
```

### 5.2 重置与连续性

```java
reset(mock);                              // 清空 stub 与记录（不建议频繁使用，暗示测试设计问题）
when(mock.next()).thenReturn(1, 2, 3);    // 连续调用依次返回
```

## 6. 隔离边界与反模式

### 6.1 该 Mock 什么、不该 Mock 什么

| 该 Mock（外部边界） | 不该 Mock（内部确定逻辑） |
|---------------------|--------------------------|
| HTTP/RPC 客户端 | 被测对象自身的领域方法 |
| 消息生产者/消费者 | 纯函数、值对象 |
| 时间/随机数/ID 生成器 | 你自己写的 Repository（用 Fake 或内存版） |
| 昂贵第三方 SDK | 框架容器（Spring 上下文尽量真实启动） |

### 6.2 Mock 反模式清单

| 反模式 | 症状 | 修正 |
|--------|------|------|
| Mock 泛滥 | 每个依赖都 mock，测试与实现 1:1 耦合 | 引入 Fake/内存版依赖，只 mock 外部边界 |
| 断言实现细节 | verify 内部私有调用链 | 断言行为结果，而非调用序列 |
| 僵尸 stub | 严格模式报 UnnecessaryStubbing | 删除未使用 stub |
| 巨型 when 链 | 一个测试 10+ stub | 用 Fake 或重构成更细的测试 |
| Mock 值对象/POJO | 徒增噪音 | 用真实对象（构造器/Builder 一行搞定） |
| 用 Mock 验证自己 | 对被测对象自身方法打桩 | 说明设计需要拆分 |

### 6.3 依赖隔离综合策略

```java
// 组合示例：外部 HTTP mock + 内存 Fake 仓储 + 临时目录
@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock ExternalApiClient api;              // 外部边界：Mock
    InMemoryOrderRepo repo = new InMemoryOrderRepo();   // 内部依赖：Fake

    @TempDir Path workDir;                    // 文件系统：临时目录

    @Test
    void sync_ok() throws Exception {
        when(api.fetch("2026-08-06")).thenReturn(List.of(new Order("o1")));
        SyncService service = new SyncService(api, repo, workDir);
        service.sync("2026-08-06");
        assertTrue(repo.exists("o1"));
        verify(api, never()).retry(any());
    }
}
```

> 🎯 **核心要点**：Mock 的边界哲学是「**挡外部、不挡内部**」——Mock 是外部边界（网络/时间/ID）的闸门，Fake 是内部协作对象的替身，真实对象是确定逻辑的唯一真相；Mockito 5 的严格桩模式把「测试与行为漂移」提前暴露在编译期，是团队规范化的基础设施。Mock 静态/构造器是战术武器，使用频率接近零才是健康的代码。

---

**上一模块**：[05-扩展模型 Extension 机制](05-扩展模型Extension机制.md) ｜ **下一模块**：[07-Spring 生态集成测试](07-Spring生态集成测试.md) ｜ **返回总览**：[00-知识体系总览](00-JUnit知识体系总览.md)
