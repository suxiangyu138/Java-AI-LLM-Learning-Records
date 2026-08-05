# 05 IService 与 Service 层封装

> IService 把"业务层样板代码"也消灭了——saveOrUpdate、链式调用、批量操作，Service 层从"复制粘贴"变成"一行声明"

---

## 📚 目录

1. [IService / ServiceImpl 设计](#1-iservice--serviceimpl-设计)
2. [API 全景：查询与写入](#2-api-全景查询与写入)
3. [saveOrUpdate 家族与链式调用](#3-saveorupdate-家族与链式调用)
4. [批量操作：saveBatch / updateBatchById](#4-批量操作savebatch--updatebatchbyid)
5. [与事务的配合](#5-与事务的配合)
6. [何时该写自己的 Service 方法](#6-何时该写自己的-service-方法)

---

## 1. IService / ServiceImpl 设计

**IService 是业务层的"BaseMapper 版"**——一个泛型接口 + 一个基类实现：

```java
// ① Service 接口：继承 IService（泛型 = 实体）
public interface UserService extends IService<User> {
    // 自定义业务方法（MP 通用方法之外的需求）
    void changePassword(Long userId, String newPwd);
}

// ② Service 实现：继承 ServiceImpl（泛型 = Mapper, 实体）
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Override
    public void changePassword(Long userId, String newPwd) {
        // this 自带全套 CRUD（getById/save/updateById/list...）
        // this.baseMapper 可访问 Mapper 自定义方法
    }
}
```

**Service 层获得的能力**（继承即得）：

| 能力 | 说明 |
|------|------|
| 查询 | `getById`、`list()`、`count()`、`getOne`、`listByMap` |
| 写入 | `save`、`updateById`、`removeById` |
| 批量 | `saveBatch`、`updateBatchById`、`removeByIds` |
| 链式 | `lambdaQuery()` / `lambdaUpdate()` 链式调用 |
| 封装 | `saveOrUpdate`、`getByWrapper` 等组合语义 |

> 🎯 **核心要点**：**Controller 层依赖 Service 接口而非 Mapper**——IService 消灭 Service 层样板（save/list/count 直接继承），自定义业务方法再往里加。**分层规范**：Controller → IService → BaseMapper，三层各有职责。

---

## 2. API 全景：查询与写入

| 分类 | 方法 | 说明 |
|------|------|------|
| 单查 | `getById(id)` | 按主键（含逻辑删除过滤） |
| 单查 | `getOne(wrapper)` | 取一条（多条抛异常）；`getOne(w, false)` 取第一条不抛 |
| 列表 | `list()` / `list(wrapper)` | 全量 / 条件 |
| 计数 | `count(wrapper)` | Long |
| 分页 | `page(page, wrapper)` | Page 对象 |
| 保存 | `save(entity)` / `saveBatch(list)` | 单条 / 批量 |
| 更新 | `updateById(e)` / `update(wrapper)` | 同 BaseMapper 语义 |
| 删除 | `removeById(s)` / `remove(wrapper)` | 逻辑/物理由实体决定 |
| 判断 | `exists(wrapper)` | 是否存在（优化 EXISTS 查询） |

```java
// 使用（Controller 层直接依赖接口）
@RestController
public class UserController {
    private final UserService userService;

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping
    public Page<User> page(@RequestParam long current, @RequestParam long size) {
        return userService.page(new Page<>(current, size));
    }

    @PostMapping
    public void save(@RequestBody User user) {
        userService.save(user);
    }
}
```

> 🎯 **核心要点**：IService 的查询方法 = BaseMapper 的"业务化包装"——`getOne` 的抛异常语义、`exists` 的优化、`page` 的 Page 对象，都是业务视角的设计。**Controller 只碰 IService，不碰 Mapper**。

---

## 3. saveOrUpdate 家族与链式调用

**saveOrUpdate：按主键存在与否自动选择 insert/update**：

```java
// 语义：主键为空 → insert；主键非空 → update（查不到则 insert）
boolean ok = userService.saveOrUpdate(user);

// 批量版
userService.saveOrUpdateBatch(list);

// ⚠️ 注意：update 分支走"非 null 更新"语义 —— 想置 null 的字段不更新（见 03 模块）
```

**链式调用（lambdaQuery / lambdaUpdate）**——不用再写 Wrapper 变量：

```java
// 查询链式
List<User> list = userService.lambdaQuery()
        .eq(User::getStatus, 1)
        .like(User::getName, "张")
        .orderByDesc(User::getCreateTime)
        .list();                          // .one()/.count()/.page() 收尾

// 更新链式
boolean updated = userService.lambdaUpdate()
        .eq(User::getId, 1L)              // 定位条件（必须有！）
        .set(User::getName, "新名字")
        .update();

// 删除链式
userService.lambdaUpdate()
        .eq(User::getStatus, 0)
        .remove();
```

> 🎯 **核心要点**：链式调用 = "**Wrapper + Service 方法合一**"——`lambdaQuery()...list()` 一段式完成"条件+执行"，Service 层代码密度再次减半。**更新/删除链式先写定位条件**（防全表）。

---

## 4. 批量操作：saveBatch / updateBatchById

**批量操作的核心价值：复用 SqlSession，减少数据库往返**：

```java
// 循环单条（反模式）：
for (Order o : orders) { orderService.save(o); }        // N 次 insert + N 次提交

// 批量（MP 标准）：
orderService.saveBatch(orders);                          // 内部批量提交
orderService.saveBatch(orders, 500);                     // 自定义批次大小

// 批量更新：
orderService.updateBatchById(orderList);                 // 内部复用 SqlSession
```

**性能对比（实测参考，来源：2025 社区基准）**：

| 方式 | 耗时（1 万条） | 说明 |
|------|:--------------:|------|
| 循环单条 insert | ~8200ms | 每次提交一次 |
| MP saveBatch | ~1500ms | 默认 1000 条一批 |
| 手动拼接批量 SQL | ~900ms | 一条 INSERT 多值 |
| JDBC 批处理 | ~750ms | 最底层方案 |

**saveBatch 的内部原理**：

```text
saveBatch → SqlSession 批量模式（ExecutorType.BATCH）
→ 同一批次内多条 INSERT 预编译后批量执行
→ 默认 1000 条提交一次（可配 batchSize）
注意事项：
① 批量大小参考连接池与 max_allowed_packet（MySQL 大包限制）
② 批量插入无返回值/主键回填受限 —— 需要回填的场景逐条 save
③ saveBatch 的 INSERT 默认不包含 null 字段（同 insert 语义）
```

> 🎯 **核心要点**：**批量写一律 saveBatch/updateBatchById**——提升 5-10 倍是基线；"循环里调 MP 方法"是性能红线（N+1 提交）。超大数据量（百万级）再考虑 JDBC 批处理或分批+多线程。

---

## 5. 与事务的配合

**IService 方法默认无事务**（`@Transactional` 需要自己加）——批量操作尤其需要事务保护：

```java
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>
        implements OrderService {

    @Override
    @Transactional(rollbackFor = Exception.class)   // 批量导入：要么全成要么全回滚
    public void importOrders(List<Order> orders) {
        // ① 批量保存
        saveBatch(orders);
        // ② 更新关联表
        orderItemService.saveBatch(items);
        // ③ 任何一步异常 → 全部回滚（saveBatch 在同一事务内）
    }
}
```

**事务三注意**（衔接 `Spring框架核心/06-声明式事务管理`）：

| 注意 | 说明 |
|------|------|
| 自调用失效 | 同类内部 `this.importOrders()` 不走代理 → 事务失效（拆类） |
| 批量与回滚 | saveBatch 内部是同一个 SqlSession——事务回滚时批量操作一起回滚 |
| 隔离与锁 | 批量导入时目标表用 `@Transactional` + 合理隔离级别（或乐观锁） |

> 🎯 **核心要点**：**"批量 + 事务"是标配**——saveBatch 的性能收益要建立在"要么全成功要么全回滚"的语义上；`rollbackFor = Exception.class` 是工程标准（受检异常默认不回滚，见 Spring 事务模块）。

---

## 6. 何时该写自己的 Service 方法

**IService 覆盖了"单表直来直去"的 80%**，但业务方法仍需自己写：

| 场景 | 写法 | 例子 |
|------|------|------|
| 组合业务逻辑 | 自定义方法（事务内多步操作） | `changePassword`（校验+更新+记录日志） |
| 复杂查询（多表/聚合） | Service 方法 → 调 Mapper 自定义 SQL（XML） | `selectUserWithOrders` |
| 业务规则校验 | 自定义方法（规则写在 Service） | `register`（查重+校验+落库） |
| 跨 Service 协作 | 注入其他 Service | 下单调库存、用户、积分 |

```java
// 自定义业务方法的标准模板
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>
        implements OrderService {

    private final UserService userService;      // 跨 Service 协作

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderRequest req) {
        // ① 业务校验
        User user = userService.getById(req.userId());
        if (user == null) throw new BizException("USER_NOT_FOUND", "用户不存在");
        // ② 主数据落库（继承的 save）
        Order order = new Order(user.getId(), req.amount());
        save(order);
        // ③ 关联操作
        userService.lambdaUpdate().eq(User::getId, user.getId())
                .set(User::getOrderCount, user.getOrderCount() + 1).update();
        return order;
    }
}
```

> 🎯 **核心要点**：**IService 消灭的是"样板"，不是"业务"**——组合逻辑、校验、跨 Service 协作永远要自己写。规范比例：**80% 直接继承方法，20% 自定义方法（事务内组合）**——与 08 模块的"80/20 原则"一脉相承。

---

**下一模块**：[06-插件体系与分页](./06-插件体系与分页.md) / **返回总览**：[00-MyBatisPlus知识体系总览](./00-MyBatisPlus知识体系总览.md)
