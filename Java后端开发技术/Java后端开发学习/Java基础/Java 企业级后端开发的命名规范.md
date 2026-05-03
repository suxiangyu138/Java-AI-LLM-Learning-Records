03.20 16:22
Java 企业级后端开发的命名规范
在Java企业级后端开发中，规范的命名体系是团队协作、代码可维护性的核心基础。
下面我会从包名、类名、方法名、变量名、常量名等核心维度，结合企业级开发的最佳实践，给出清晰、可落地的命名规范。
一、核心命名原则（通用）
在具体规范前，先明确几个底层原则，所有命名都需遵守：
语义化：命名必须体现用途，见名知意（如UserService而非Service1）。
大小写敏感：严格遵守大小写规则，Java是大小写敏感语言。
无特殊字符：只能包含字母、数字、下划线、美元符（$），且不能以数字开头。
无拼音/混合语：优先使用英文，禁止拼音（如YongHu）或中英混合（如UserYongHu）。
避免关键字：不能使用Java关键字（如class、int、if）作为命名。
二、分维度详细命名规范
1. 包名（Package）
企业级开发的包名是代码结构的骨架，需体现公司/组织、业务模块、功能分层，核心规则：
全小写：所有字母小写，用.分隔层级。
反向域名前缀：以公司/组织的反向域名开头（避免包名冲突），如阿里：com.alibaba，个人/中小企业：com.xxx（xxx为公司域名）。
分层/模块划分：反向域名后按“业务模块+功能分层”拆分，常见企业级包结构示例：
# 标准企业级后端包结构（Spring Boot为例）
com.xxx（公司/组织）
├── common          # 通用模块（工具类、常量、全局异常等）
│   ├── constant    # 常量定义
│   ├── exception   # 全局异常
│   ├── utils       # 工具类（如DateUtil、StringUtil）
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
└── config          # 配置类（如MyBatisConfig、RedisConfig）
包名示例：
通用工具类：com.xxx.common.utils
用户模块控制层：com.xxx.user.controller
全局常量：com.xxx.common.constant
2. 类名（Class/Interface/Enum）
类/枚举：大驼峰（PascalCase），名词/名词短语，体现职责。
示例：UserController、OrderService、UserEntity、PayStatusEnum。
接口：
方式1（推荐）：大驼峰，名词/名词短语，如UserService（接口）+UserServiceImpl（实现类）。
方式2（传统）：前缀I+大驼峰，如IUserService+UserServiceImpl（企业中两种都常见，团队统一即可）。
抽象类：前缀Abstract+大驼峰，如AbstractBaseService。
企业级常用类名后缀（固定用法）：
类类型
后缀
示例
控制层
Controller
UserController
服务层接口
Service
OrderService
服务层实现类
ServiceImpl
OrderServiceImpl
数据访问层（Mapper）
Mapper
UserMapper
数据库实体
Entity/DO
UserEntity/UserDO
数据传输对象（入参）
DTO
OrderCreateDTO
视图对象（出参）
VO
UserInfoVO
枚举类
Enum
PayTypeEnum
配置类
Config
RedisConfig
工具类
Util/Tools
DateUtil
异常类
Exception
BusinessException
3. 方法名（Method）
小驼峰（camelCase）：动词/动宾短语，体现动作+目标。
核心规则：
获取/查询：get/find/query（如getUserById、findOrderByUserId）。
创建/新增：create/add/save（如createUser、saveOrder）。
更新：update/modify（如updateUserInfo）。
删除：delete/remove（如deleteOrderById）。
校验：validate/check（如validateUserToken）。
批量操作：前缀batch（如batchDeleteUser）。
示例：
// 服务层方法示例
public interface UserService {
    // 根据ID查询用户
    UserEntity getUserById(Long id);
    // 新增用户
    Long createUser(UserCreateDTO dto);
    // 批量更新用户状态
    boolean batchUpdateUserStatus(List<Long> ids, Integer status);
}
4. 变量名（Variable）
小驼峰（camelCase）：名词/名词短语，体现用途，禁止单字母（循环变量i/j除外）。
核心规则：
成员变量：userId、orderAmount、payStatus。
局部变量：userName、tempList（临时变量需体现临时用途）。
布尔类型变量：前缀is/has/can（如isVip、hasPermission、canSubmit）。
反例（禁止）：a、num、data（无语义）、yongHuId（拼音）。
5. 常量名（Constant）
全大写：单词间用下划线_分隔，定义在final static变量或常量类中。
示例：
// 常量类示例（放在common.constant包下）
public class OrderConstant {
    // 订单超时时间（30分钟）
    public static final Integer ORDER_TIMEOUT_MINUTES = 30;
    // 订单状态-待支付
    public static final String ORDER_STATUS_PENDING = "PENDING";
}
6. 其他补充（企业级高频场景）
参数名：同变量名，小驼峰，语义化（如public void updateUser(Long userId, String userName)）。
注解名：大驼峰（如@Log、@Permission）。
模块名/微服务名：全小写，用短横线分隔（如user-service、order-center）。
三、反例与正例对比（快速避坑）
类型
反例（错误）
正例（正确）
包名
[com.xxx](com.xxx).User.utils
[com.xxx](com.xxx).common.utils
类名
usercontroller、UserServiceImp
UserController、UserServiceImpl
方法名
getuser、update1
getUser、updateUser
变量名
int a; String name1;
int userId; String userName;
常量名
int orderTimeout = 30;
final static int ORDER_TIMEOUT = 30;
总结
包名：反向域名+模块+分层，全小写，.分隔，体现代码结构（如com.xxx.user.controller）。
类名：大驼峰，结合固定后缀体现职责（如XxxController、XxxServiceImpl），接口/抽象类有专属前缀。
方法/变量：小驼峰，方法动宾短语、变量语义化，布尔变量加is/has/can前缀；常量全大写+下划线分隔。
遵循以上规范，能保证企业级后端代码的可读性、一致性，降低团队协作成本，也是大厂/规范团队的通用实践。

