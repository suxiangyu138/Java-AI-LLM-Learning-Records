# Java 企业级后端开发的命名规范

在Java企业级后端开发中，规范的命名体系是团队协作、代码可维护性的核心基础。下面从包名、类名、方法名、变量名、常量名等核心维度，结合企业级开发的最佳实践，给出清晰、可落地的命名规范。

---

## 一、核心命名原则（通用）

在具体规范前，先明确几个底层原则，所有命名都需遵守：

- **语义化**：命名必须体现用途，见名知意（如 `UserService` 而非 `Service1`）
- **大小写敏感**：严格遵守大小写规则，Java是大小写敏感语言
- **无特殊字符**：只能包含字母、数字、下划线、美元符（`$`），且不能以数字开头
- **无拼音/混合语**：优先使用英文，禁止拼音（如 `YongHu`）或中英混合（如 `UserYongHu`）
- **避免关键字**：不能使用Java关键字（如 `class`、`int`、`if`）作为命名

---

## 二、分维度详细命名规范

### 1. 包名（Package）

企业级开发的包名是代码结构的骨架，需体现公司/组织、业务模块、功能分层。

**核心规则**：

- **全小写**：所有字母小写，用 `.` 分隔层级
- **反向域名前缀**：以公司/组织的反向域名开头（避免包名冲突），如阿里 `com.alibaba`，个人/中小企业 `com.xxx`
- **分层/模块划分**：反向域名后按"业务模块 + 功能分层"拆分

**标准企业级后端包结构（Spring Boot为例）**：

```
com.xxx（公司/组织）
├── common          # 通用模块（工具类、常量、全局异常等）
│   ├── constant    # 常量定义
│   ├── exception   # 全局异常
│   ├── utils       # 工具类（如 DateUtil、StringUtil）
│   └── enums       # 枚举类
├── user            # 业务模块（用户模块）
│   ├── controller  # 控制层（接收请求、返回响应）
│   ├── service     # 服务层（业务逻辑）
│   │   └── impl    # 服务实现类
│   ├── mapper      # 数据访问层（MyBatis Mapper）
│   ├── entity      # 数据库实体（与表一一对应）
│   ├── dto         # 数据传输对象（入参/出参）
│   └── vo          # 视图对象（返回给前端的展示数据）
├── order           # 业务模块（订单模块）
│   ├── controller
│   ├── service
│   └── ...
└── config          # 配置类（如 MyBatisConfig、RedisConfig）
```

**包名示例**：

- 通用工具类：`com.xxx.common.utils`
- 用户模块控制层：`com.xxx.user.controller`
- 全局常量：`com.xxx.common.constant`

### 2. 类名（Class / Interface / Enum）

| 类型 | 规则 | 示例 |
|------|------|------|
| 类/枚举 | 大驼峰（PascalCase），名词/名词短语 | `UserController`、`OrderService`、`PayStatusEnum` |
| 接口 | 大驼峰，名词/名词短语 | `UserService`（推荐）、`IUserService`（传统） |
| 抽象类 | 前缀 `Abstract` + 大驼峰 | `AbstractBaseService` |

> 接口命名两种方式（团队统一即可）：方式1（推荐）`UserService` + `UserServiceImpl`；方式2（传统）`IUserService` + `UserServiceImpl`

**企业级常用类名后缀**：

| 类型 | 后缀 | 示例 |
|------|------|------|
| 控制层 | `Controller` | `UserController` |
| 服务层接口 | `Service` | `OrderService` |
| 服务层实现类 | `ServiceImpl` | `OrderServiceImpl` |
| 数据访问层 | `Mapper` | `UserMapper` |
| 数据库实体 | `Entity` / `DO` | `UserEntity` / `UserDO` |
| 数据传输对象 | `DTO` | `OrderCreateDTO` |
| 视图对象 | `VO` | `UserInfoVO` |
| 枚举类 | `Enum` | `PayTypeEnum` |
| 配置类 | `Config` | `RedisConfig` |
| 工具类 | `Util` / `Tools` | `DateUtil` |
| 异常类 | `Exception` | `BusinessException` |

### 3. 方法名（Method）

**小驼峰（camelCase）**：动词/动宾短语，体现动作 + 目标。

| 操作类型 | 动词前缀 | 示例 |
|----------|---------|------|
| 获取/查询 | `get` / `find` / `query` | `getUserById`、`findOrderByUserId` |
| 创建/新增 | `create` / `add` / `save` | `createUser`、`saveOrder` |
| 更新 | `update` / `modify` | `updateUserInfo` |
| 删除 | `delete` / `remove` | `deleteOrderById` |
| 校验 | `validate` / `check` | `validateUserToken` |
| 批量操作 | 前缀 `batch` | `batchDeleteUser` |

```java
// 服务层方法示例
public interface UserService {
    UserEntity getUserById(Long id);                      // 根据ID查询用户
    Long createUser(UserCreateDTO dto);                   // 新增用户
    boolean batchUpdateUserStatus(List<Long> ids, Integer status);  // 批量更新
}
```

### 4. 变量名（Variable）

**小驼峰（camelCase）**：名词/名词短语，体现用途，禁止单字母（循环变量 `i`/`j` 除外）。

| 类型 | 规则 | 正例 | 反例 |
|------|------|------|------|
| 成员变量 | 名词短语 | `userId`、`orderAmount` | `a`、`num`、`data` |
| 局部变量 | 名词短语，临时变量需体现临时用途 | `userName`、`tempList` | `name1`、`yongHuId` |
| 布尔变量 | 前缀 `is` / `has` / `can` | `isVip`、`hasPermission`、`canSubmit` | `flag`、`status` |

### 5. 常量名（Constant）

**全大写**：单词间用下划线 `_` 分隔，定义在 `final static` 变量或常量类中。

```java
// 常量类示例（放在 common.constant 包下）
public class OrderConstant {
    public static final Integer ORDER_TIMEOUT_MINUTES = 30;        // 订单超时时间
    public static final String ORDER_STATUS_PENDING = "PENDING";   // 订单状态-待支付
}
```

### 6. 其他补充（企业级高频场景）

| 类型 | 规则 | 示例 |
|------|------|------|
| 参数名 | 小驼峰，同变量名 | `public void updateUser(Long userId, String userName)` |
| 注解名 | 大驼峰 | `@Log`、`@Permission` |
| 模块名/微服务名 | 全小写，短横线分隔 | `user-service`、`order-center` |

---

## 三、反例与正例对比

| 类型 | 反例（错误） | 正例（正确） |
|------|-------------|-------------|
| 包名 | `com.xxx.User.utils` | `com.xxx.common.utils` |
| 类名 | `usercontroller`、`UserServiceImp` | `UserController`、`UserServiceImpl` |
| 方法名 | `getuser`、`update1` | `getUser`、`updateUser` |
| 变量名 | `int a;`、`String name1;` | `int userId;`、`String userName;` |
| 常量名 | `int orderTimeout = 30;` | `final static int ORDER_TIMEOUT = 30;` |

---

## 总结

| 维度 | 规则 |
|------|------|
| **包名** | 反向域名 + 模块 + 分层，全小写，`.` 分隔（如 `com.xxx.user.controller`） |
| **类名** | 大驼峰，结合固定后缀体现职责（如 `XxxController`、`XxxServiceImpl`） |
| **方法/变量** | 小驼峰，方法动宾短语、变量语义化，布尔变量加 `is`/`has`/`can` 前缀 |
| **常量** | 全大写 + 下划线分隔 |

遵循以上规范，能保证企业级后端代码的可读性、一致性，降低团队协作成本，也是大厂/规范团队的通用实践。
