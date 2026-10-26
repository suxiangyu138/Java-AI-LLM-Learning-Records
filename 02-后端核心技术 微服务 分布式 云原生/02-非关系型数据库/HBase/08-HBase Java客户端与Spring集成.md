# 08-HBase Java 客户端与 Spring 集成

> HBase 的 Java 客户端不是"发个请求就完事"：Connection 是重量级单例，Table 是轻量句柄，BufferedMutator 负责批量，AsyncTable 负责高并发，Filter 决定服务端读多少，CheckAndMutate 提供单行原子条件更新。把这些对象的生命周期用错，比参数调错更常见。

---

## 目录

1. [依赖与版本](#1-依赖与版本)
2. [Connection：进程级单例](#2-connection进程级单例)
3. [核心对象模型](#3-核心对象模型)
4. [写入 API](#4-写入-api)
5. [读取 API](#5-读取-api)
6. [Filter 体系](#6-filter-体系)
7. [条件更新 CheckAndMutate](#7-条件更新-checkandmutate)
8. [计数器与追加](#8-计数器与追加)
9. [异步客户端](#9-异步客户端)
10. [协处理器](#10-协处理器)
11. [Spring Boot 集成](#11-spring-boot-集成)
12. [异常处理与重试](#12-异常处理与重试)
13. [测试与本地验证](#13-测试与本地验证)
14. [生产规范清单](#14-生产规范清单)
15. [小结](#15-小结)

---

## 1. 依赖与版本

Maven 依赖（HBase 3.0.0）：

```xml
<properties>
  <hbase.version>3.0.0</hbase.version>
</properties>

<dependencies>
  <dependency>
    <groupId>org.apache.hbase</groupId>
    <artifactId>hbase-client</artifactId>
    <version>${hbase.version}</version>
  </dependency>
  <dependency>
    <groupId>org.apache.hbase</groupId>
    <artifactId>hbase-common</artifactId>
    <version>${hbase.version}</version>
  </dependency>
</dependencies>
```

版本对齐的三个坑：

```text
① 客户端与服务端大版本要一致
   3.0 客户端连 2.x 服务端不保证兼容

② Hadoop 依赖冲突
   hbase-client 传递依赖 hadoop-common
   如果项目里已有 Hadoop 依赖，用 exclusions 排除后统一版本

③ Guava / Netty 冲突
   HBase 依赖特定版本，Spring Boot 也依赖
   → 用 dependencyManagement 锁定，或排除冲突传递依赖
```

排除示例：

```xml
<dependency>
  <groupId>org.apache.hbase</groupId>
  <artifactId>hbase-client</artifactId>
  <version>${hbase.version}</version>
  <exclusions>
    <exclusion>
      <groupId>org.apache.hadoop</groupId>
      <artifactId>hadoop-common</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

---

## 2. Connection：进程级单例

### 2.1 为什么必须单例

```text
Connection 内部持有：
  RPC 客户端与线程池
  到各 RegionServer 的连接
  meta 路由缓存
  ZooKeeper 连接

创建成本：
  首次创建要连 ZK、读 meta、建线程池（数百毫秒到数秒）
  频繁创建 → 线程与连接泄漏 → RS 拒绝服务

结论：
  一个 JVM 一个 Connection，应用生命周期内复用
```

### 2.2 标准初始化

```java
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

public final class HBaseConnectionFactory {

    private static volatile Connection connection;

    private HBaseConnectionFactory() {
    }

    public static Connection getConnection() {
        if (connection == null) {
            synchronized (HBaseConnectionFactory.class) {
                if (connection == null) {
                    try {
                        Configuration conf = HBaseConfiguration.create();
                        conf.set("hbase.zookeeper.quorum", "zk1,zk2,zk3");
                        conf.set("hbase.zookeeper.property.clientPort", "2181");
                        conf.set("zookeeper.znode.parent", "/hbase");
                        conf.setInt("hbase.client.retries.number", 10);
                        conf.setInt("hbase.client.pause", 200);
                        conf.setInt("hbase.rpc.timeout", 5000);
                        conf.setInt("hbase.client.operation.timeout", 30000);
                        conf.setInt("hbase.client.scanner.timeout.period", 60000);
                        connection = ConnectionFactory.createConnection(conf);
                        Runtime.getRuntime().addShutdownHook(
                                new Thread(HBaseConnectionFactory::close));
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to create HBase connection", e);
                    }
                }
            }
        }
        return connection;
    }

    private static void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException ignored) {
            }
        }
    }
}
```

### 2.3 关键超时

| 参数 | 默认 | 含义 |
|------|------|------|
| `hbase.rpc.timeout` | 60000 | 单次 RPC 超时 |
| `hbase.client.operation.timeout` | 120000 | 单次操作（含重试）超时 |
| `hbase.client.scanner.timeout.period` | 60000 | Scan 两次 next 的最大间隔 |
| `hbase.client.retries.number` | 15 | 重试次数 |
| `hbase.client.pause` | 100 | 重试基础间隔（指数退避） |
| `hbase.client.scanner.caching` | 无 | 每次 Scan 拉取行数 |

> **提示**：`operation.timeout` 必须大于 `rpc.timeout`，否则重试还没完成就被外层超时打断。生产上按 P99 的 3-5 倍设置，并保证业务侧超时小于网关超时。

---

## 3. 核心对象模型

| 对象 | 生命周期 | 线程安全 | 用途 |
|------|---------|:-------:|------|
| `Connection` | 进程级 | 是 | 连接与路由 |
| `Admin` | 短生命周期或长持有 | 是 | 建表、改表、快照 |
| `Table` | 方法内 try-with-resources | 是 | get/put/delete/scan |
| `BufferedMutator` | 长持有 | 是 | 批量异步写 |
| `RegionLocator` | 短生命周期 | 是 | 查 Region 位置 |
| `AsyncConnection` | 进程级 | 是 | 异步 API 入口 |
| `AsyncTable` | 长持有 | 是 | 异步读写 |

`Table` 的正确用法：

```java
try (Table table = connection.getTable(TableName.valueOf("app", "user_profile"))) {
    Result result = table.get(new Get(Bytes.toBytes("u1001")));
}
```

`Table` 是轻量句柄，但每次 `getTable` 仍有对象创建开销。高频场景可以缓存 `Table`，但要注意它可能因 Region 迁移而失效；更稳妥的做法是每次方法内创建，或使用 `BufferedMutator` / `AsyncTable`。

---

## 4. 写入 API

### 4.1 单条 Put

```java
Put put = new Put(Bytes.toBytes("u1001"));
put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"), Bytes.toBytes("张三"));
put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("city"), Bytes.toBytes("合肥"));
table.put(put);
```

### 4.2 批量 Put

```java
List<Put> puts = new ArrayList<>();
for (User user : users) {
    Put put = new Put(Bytes.toBytes(user.getId()));
    put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),
            Bytes.toBytes(user.getName()));
    puts.add(put);
}
table.put(puts);
```

### 4.3 BufferedMutator（推荐）

```java
BufferedMutatorParams params = new BufferedMutatorParams(TableName.valueOf("app", "user_profile"))
        .writeBufferSize(8 * 1024 * 1024)
        .listener((exception, mutator) -> {
            if (exception != null) {
                log.error("BufferedMutator error", exception);
            }
        });

try (BufferedMutator mutator = connection.getBufferedMutator(params)) {
    for (User user : users) {
        Put put = new Put(Bytes.toBytes(user.getId()));
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),
                Bytes.toBytes(user.getName()));
        mutator.mutate(put);
    }
    mutator.flush();
}
```

`BufferedMutator` 的工作方式：

```text
mutate(put) → 放入内存缓冲（按 Region 分组）
缓冲达到 writeBufferSize → 后台批量发送
flush() → 强制发送并等待完成
close() → flush + 释放资源

注意：
  异常通过 listener 异步回调，不会在 mutate 时抛出
  必须在关闭前 flush，否则最后一批可能丢失
```

### 4.4 Delete

```java
Delete delete = new Delete(Bytes.toBytes("u1001"));
delete.addColumn(Bytes.toBytes("info"), Bytes.toBytes("city"));
table.delete(delete);
```

---

## 5. 读取 API

### 5.1 Get

```java
Get get = new Get(Bytes.toBytes("u1001"));
get.addFamily(Bytes.toBytes("info"));
get.setMaxVersions(3);
Result result = table.get(get);

byte[] name = result.getValue(Bytes.toBytes("info"), Bytes.toBytes("name"));
boolean exists = result.containsColumn(Bytes.toBytes("info"), Bytes.toBytes("city"));
```

### 5.2 批量 Get

```java
List<Get> gets = rowKeys.stream()
        .map(key -> new Get(Bytes.toBytes(key)))
        .collect(Collectors.toList());
Result[] results = table.get(gets);
```

批量 get 的优势：

```text
按 Region 自动分组 → 并发发送到多台 RS
单次网络往返 vs N 次
建议每批 ≤ 1000 行，避免单次响应过大
```

### 5.3 Scan

```java
Scan scan = new Scan()
        .withStartRow(Bytes.toBytes("u1001_"))
        .withStopRow(Bytes.toBytes("u1001`"))
        .setCaching(500)
        .setBatch(100)
        .addFamily(Bytes.toBytes("info"))
        .setLimit(1000);

try (ResultScanner scanner = table.getScanner(scan)) {
    for (Result result : scanner) {
        // 处理每一行
    }
}
```

`setCaching` 与 `setBatch` 的区别：

| 参数 | 作用 | 调大 | 调小 |
|------|------|------|------|
| `caching` | 每次 RPC 返回的行数 | RPC 少、内存高 | RPC 多、延迟低 |
| `batch` | 每行返回的列数 | 单行完整 | 防止单行过大 |

### 5.4 Reversed Scan

```java
Scan scan = new Scan()
        .withStartRow(Bytes.toBytes("u1001_"))
        .withStopRow(Bytes.toBytes("u1001_"))
        .setReversed(true);
```

反向扫描需要 RowKey 设计配合（倒序时间戳），否则语义与性能都不理想。

---

## 6. Filter 体系

Filter 在服务端执行，减少网络传输，但不减少磁盘读取。

### 6.1 常用 Filter

```java
// 前缀过滤
Filter prefix = new PrefixFilter(Bytes.toBytes("u1001_"));

// 单列值过滤
Filter valueFilter = new SingleColumnValueFilter(
        Bytes.toBytes("info"),
        Bytes.toBytes("status"),
        CompareOperator.EQUAL,
        Bytes.toBytes("ACTIVE"));

// 分页
Filter page = new PageFilter(100);

// 组合
FilterList list = new FilterList(FilterList.Operator.MUST_PASS_ALL);
list.addFilter(prefix);
list.addFilter(valueFilter);
scan.setFilter(list);
```

### 6.2 Filter 性能注意事项

```text
① Filter 是"读出来再判断"
   → 不能替代 RowKey 设计
   → 对非 RowKey 字段过滤 = 全表扫

② SingleColumnValueFilter 的陷阱
   默认行为：如果该列不存在，整行被过滤掉
   需要 setFilterIfMissing(false) 保留无该列的行的语义

③ 复杂 FilterList 会放大服务端 CPU
   能放到 RowKey 的条件，不要放 Filter

④ PrefixFilter 不如 startRow/stopRow
   startRow/stopRow 在 Region 寻址阶段就缩小范围
   PrefixFilter 是先扫再过滤
```

---

## 7. 条件更新 CheckAndMutate

CheckAndMutate 提供"读-判断-写"的单行原子操作，是 HBase 版本的 CAS：

```java
CheckAndMutate checkAndMutate = CheckAndMutate
        .newBuilder(TableName.valueOf("app", "account"))
        .qualifier(Bytes.toBytes("info"), Bytes.toBytes("status"))
        .ifEquals(Bytes.toBytes("PENDING"))
        .thenPut(new Put(Bytes.toBytes("acc1001"))
                .addColumn(Bytes.toBytes("info"), Bytes.toBytes("status"),
                        Bytes.toBytes("PAID")));

boolean success = table.checkAndMutate(checkAndMutate);
```

适用场景：

| 场景 | 用法 |
|------|------|
| 状态机流转 | 只有 PENDING 才能变 PAID |
| 幂等去重 | 如果不存在则写入（ifNotExists） |
| 乐观锁 | 带版本号比较 |
| 库存扣减 | 条件更新 + 值比较 |

限制：

```text
只保证单行原子
跨行需要业务锁或分布式锁
高并发下失败重试要控制次数与退避
```

---

## 8. 计数器与追加

### 8.1 Increment

```java
Increment increment = new Increment(Bytes.toBytes("stat_20261026"));
increment.addColumn(Bytes.toBytes("d"), Bytes.toBytes("pv"), 1L);
Result result = table.increment(increment);
long pv = Bytes.toLong(result.getValue(Bytes.toBytes("d"), Bytes.toBytes("pv")));
```

### 8.2 Append

```java
Append append = new Append(Bytes.toBytes("log_1001"));
append.addColumn(Bytes.toBytes("d"), Bytes.toBytes("trace"), Bytes.toBytes("step2;"));
table.append(append);
```

两者都是单行原子，但要克制使用：

```text
Increment：
  适合计数（PV/UV、库存）
  单行成为热点时 QPS 有上限（单 Region 串行）
  高并发计数建议先 Redis 累加，再批量落 HBase

Append：
  频繁 append 会导致值越来越大
  读放大、Compaction 压力上升
  日志类场景优先"一行一条"，不要反复 append 同一行
```

---

## 9. 异步客户端

HBase 2.0 起提供 `AsyncConnection` / `AsyncTable`，3.0 继续成熟：

```java
import org.apache.hadoop.hbase.client.AsyncConnection;
import org.apache.hadoop.hbase.client.AsyncTable;
import org.apache.hadoop.hbase.client.ConnectionFactory;

public class AsyncHBaseExample {

    public void write(AsyncConnection connection) {
        AsyncTable<AdvancedScanResultConsumer> table =
                connection.getTable(TableName.valueOf("app", "user_profile"));

        Put put = new Put(Bytes.toBytes("u1001"))
                .addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),
                        Bytes.toBytes("张三"));

        table.put(put).whenComplete((ignored, error) -> {
            if (error != null) {
                // 处理失败
            }
        });
    }
}
```

异步客户端的收益与代价：

```text
收益：
  少量线程支撑高并发（非阻塞 IO）
  天然适配 WebFlux / CompletableFuture 链路

代价：
  编程模型复杂（回调/组合）
  错误处理与超时链路要自己设计
  调试困难

选择：
  网关型高并发写入 → AsyncTable
  普通业务 CRUD → 同步 Table + 线程池
```

---

## 10. 协处理器

协处理器（Coprocessor）把计算推到 RegionServer，分两类：

| 类型 | 类比 | 用途 |
|------|------|------|
| Observer | 触发器 | 写入前后钩子（审计、二级索引维护） |
| Endpoint | 存储过程 | 服务端聚合（SUM/COUNT/自定义计算） |

### 10.1 Observer 示例（简化）

```java
public class AuditObserver extends BaseRegionObserver {

    @Override
    public void postPut(ObserverContext<RegionCoprocessorEnvironment> context,
                        Put put, WALEdit edit, Durability durability) {
        // 写入后触发：记录审计日志或同步索引
    }
}
```

### 10.2 协处理器的风险

```text
① 运行在 RS 进程内，异常会影响整个 RegionServer
② 依赖 HBase 内部 API，升级版本需要重新编译与回归
③ 调试困难，日志分散在各 RS
④ 性能问题会放大到所有请求

使用原则：
  能用客户端逻辑解决，不用协处理器
  必须用时，做好异常隔离、超时、开关（可动态卸载）
```

---

## 11. Spring Boot 集成

Spring for Apache Hadoop 已停止维护，当前推荐"官方客户端 + 自封装 Bean"。

### 11.1 配置类

```java
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@Configuration
@ConfigurationProperties(prefix = "hbase")
public class HBaseConfig {

    private String quorum;
    private String znodeParent = "/hbase";
    private int clientPort = 2181;
    private int rpcTimeout = 5000;
    private int operationTimeout = 30000;

    @Bean(destroyMethod = "close")
    public Connection hbaseConnection() throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", quorum);
        conf.set("hbase.zookeeper.property.clientPort", String.valueOf(clientPort));
        conf.set("zookeeper.znode.parent", znodeParent);
        conf.setInt("hbase.rpc.timeout", rpcTimeout);
        conf.setInt("hbase.client.operation.timeout", operationTimeout);
        return ConnectionFactory.createConnection(conf);
    }
}
```

### 11.2 application.yml

```yaml
hbase:
  quorum: zk1,zk2,zk3
  client-port: 2181
  znode-parent: /hbase
  rpc-timeout: 5000
  operation-timeout: 30000
```

### 11.3 Repository 封装

```java
@Repository
public class UserProfileRepository {

    private final Connection connection;
    private final TableName tableName = TableName.valueOf("app", "user_profile");

    public UserProfileRepository(Connection connection) {
        this.connection = connection;
    }

    public Optional<UserProfile> findById(String userId) {
        Get get = new Get(Bytes.toBytes(userId));
        try (Table table = connection.getTable(tableName)) {
            Result result = table.get(get);
            if (result.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(toUserProfile(result));
        } catch (IOException e) {
            throw new HBaseAccessException("Failed to get user " + userId, e);
        }
    }

    public void save(UserProfile profile) {
        Put put = new Put(Bytes.toBytes(profile.getId()));
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),
                Bytes.toBytes(profile.getName()));
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("city"),
                Bytes.toBytes(profile.getCity()));
        try (Table table = connection.getTable(tableName)) {
            table.put(put);
        } catch (IOException e) {
            throw new HBaseAccessException("Failed to save user " + profile.getId(), e);
        }
    }

    private UserProfile toUserProfile(Result result) {
        String id = Bytes.toString(result.getRow());
        String name = Bytes.toString(result.getValue(
                Bytes.toBytes("info"), Bytes.toBytes("name")));
        String city = Bytes.toString(result.getValue(
                Bytes.toBytes("info"), Bytes.toBytes("city")));
        return new UserProfile(id, name, city);
    }
}
```

### 11.4 健康检查

```java
@Component
public class HBaseHealthIndicator implements HealthIndicator {

    private final Connection connection;

    public HBaseHealthIndicator(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Health health() {
        try (Admin admin = connection.getAdmin()) {
            boolean available = admin.isTableAvailable(
                    TableName.valueOf("app", "user_profile"));
            return available
                    ? Health.up().build()
                    : Health.down().withDetail("table", "unavailable").build();
        } catch (IOException e) {
            return Health.down(e).build();
        }
    }
}
```

---

## 12. 异常处理与重试

### 12.1 常见异常

| 异常 | 含义 | 处理 |
|------|------|------|
| `TableNotFoundException` | 表不存在 | 检查表名与命名空间 |
| `NotServingRegionException` | Region 迁移中/RS 宕机 | 客户端自动重试，持续失败查集群 |
| `RegionTooBusyException` | 单 Region 写入过载 | 退避重试 + 打散 RowKey |
| `RetriesExhaustedException` | 重试耗尽 | 检查集群健康与超时配置 |
| `ScannerTimeoutException` | Scan 两次 next 间隔超时 | 调大 scanner timeout，减小 caching |
| `DoNotRetryIOException` | 不可重试错误 | 不要盲目重试，先定位 |

### 12.2 重试策略

```java
// 客户端内置重试（指数退避）
conf.setInt("hbase.client.retries.number", 10);
conf.setInt("hbase.client.pause", 200);

// 业务层幂等重试
int retries = 3;
for (int i = 0; i < retries; i++) {
    try {
        repository.save(profile);
        break;
    } catch (RetriesExhaustedException e) {
        if (i == retries - 1) {
            throw e;
        }
        Thread.sleep((long) Math.pow(2, i) * 100);
    }
}
```

> **警告**：HBase 的 put 是幂等的（同 RowKey 同列覆盖），但 Increment / Append 不是。对计数类操作重试，必须先做幂等令牌或改用 CheckAndMutate。

---

## 13. 测试与本地验证

### 13.1 本地 MiniCluster

HBase 提供 `HBaseTestingUtil`，可在单元测试里起一个内存集群：

```java
import org.apache.hadoop.hbase.HBaseTestingUtil;

public class HBaseIntegrationTest {

    private static HBaseTestingUtil utility;

    @BeforeAll
    static void start() throws Exception {
        utility = new HBaseTestingUtil();
        utility.startMiniCluster();
    }

    @AfterAll
    static void stop() throws Exception {
        utility.shutdownMiniCluster();
    }
}
```

MiniCluster 的限制：

```text
启动慢（数十秒）
占用内存
依赖本地 Hadoop 环境
→ 适合集成测试，不适合每次单测
```

### 13.2 测试分层

```text
单元测试：Repository 的序列化/反序列化，用 Mock 的 Table
集成测试：MiniCluster 或 Docker 起的单机 HBase
端到端：测试环境真实集群 + 冒烟用例
```

---

## 14. 生产规范清单

```text
① Connection 单例，应用关闭时 close
② Table 用 try-with-resources，不长期缓存
③ 批量写用 BufferedMutator，关闭前 flush
④ 批量读用 List<Get>，每批 ≤ 1000
⑤ Scan 必设 caching/batch/limit，必关 Scanner
⑥ 超时链路：rpc < operation < 业务超时 < 网关超时
⑦ RowKey 与列族名短、语义稳定
⑧ 写入前校验长度与空值，避免超大 Cell
⑨ 计数类操作用幂等设计，不裸重试 Increment
⑩ 所有 HBase 异常转成业务异常，带上下文
⑪ 记录慢操作（> 100ms 的 get/put/scan）
⑫ 连接、请求量、错误率、延迟接入监控
```

---

## 15. 小结

| 问题 | 答案 |
|------|------|
| Connection 能否频繁创建 | 不能，进程级单例 |
| Table 是否线程安全 | 是，但应短生命周期 |
| 批量写用什么 | BufferedMutator |
| 批量读用什么 | List<Get> |
| Scan 三参数 | caching、batch、limit |
| Filter 能否替代索引 | 不能，它是读后判断 |
| 单行 CAS 用什么 | CheckAndMutate |
| 异步客户端适合什么 | 高并发网关型写入 |
| 协处理器的风险 | 影响 RS 进程、依赖内部 API |
| Spring 集成方式 | 官方客户端 + 自封装 Bean |

**下一模块**：[09-HBase生态集成与数据迁移](09-HBase生态集成与数据迁移.md)　**返回总览**：[00-HBase知识体系总览](00-HBase知识体系总览.md)
