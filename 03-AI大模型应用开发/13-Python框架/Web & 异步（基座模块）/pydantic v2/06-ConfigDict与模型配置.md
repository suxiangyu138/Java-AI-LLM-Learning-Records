# 06 - ConfigDict 与模型配置

> 定位：模型行为的"全局开关"——额外字段、ORM 模式、严格模式、冻结、别名——"字段级的事写在注解里，模型级的事写在 model_config 里——先想清楚这个模型是什么角色，再配 config"

---

## 📚 目录

1. [model_config：模型级配置](#1-model_config模型级配置)
2. [extra：额外字段策略](#2-extra额外字段策略)
3. [from_attributes：ORM 与任意对象](#3-from_attributesorm-与任意对象)
4. [strict：严格模式](#4-strict严格模式)
5. [frozen 与不可变性](#5-frozen-与不可变性)
6. [别名体系：populate_by_name 与 alias](#6-别名体系populate_by_name-与-alias)
7. [配置继承与常用组合](#7-配置继承与常用组合)
8. [五个常见坑](#8-五个常见坑)
9. [练习 5 题](#9-练习-5-题)

---

## 1. model_config：模型级配置

**model_config 是 v2 取代 v1 `class Config` 的配置入口**——一个 ConfigDict 实例声明模型的全局行为：

```python
from pydantic import BaseModel, ConfigDict

class User(BaseModel):
    model_config = ConfigDict(
        extra="forbid",        # 多余字段：拒绝
        from_attributes=True,  # 允许从任意对象构造
        strict=False,          # 默认宽松模式（智能转换）
        frozen=True,           # 冻结：实例不可改
        populate_by_name=True, # 字段名与别名都接受
    )
    name: str
    age: int = 18
```

**配置分组记忆**：**输入侧**（extra/from_attributes/populate_by_name）、**行为侧**（strict/frozen/validate_assignment）、**输出侧**（by_alias/ser_json_timedelta 等）、**性能侧**（revalidate_instances/arbitrary_types_allowed）。**配置作用域**：ConfigDict 只作用于所在模型（继承除外）——**父模型配置不自动下传给嵌套子模型**（各模型自己配）。**"先想角色再配 config"**：这个模型是"请求体 DTO"（extra=forbid 严进）、"内部实体"（宽松）、"配置对象"（frozen 保护）——角色决定配置。

## 2. extra：额外字段策略

**extra 三值**：`"ignore"`（默认——多余字段静默丢弃）、`"allow"`（收进 `model_extra` 字典）、`"forbid"`（**报错拒绝**）。

**选型主线**：**对外边界（API 请求体/LLM 输出解析）用 forbid**——"静默丢弃 = 隐患"：前端多传的字段一旦改名成了新字段，ignore 模式会悄悄吞掉（业务无感但数据契约失真），forbid 直接 422 让调用方知道；**内部数据流（数据库行/内部消息）用 ignore 或 allow**——数据库表加列、消息加字段是常态，forbid 会让老代码炸；**扩展性场景（插件/自定义元数据）用 allow**——`model_extra` 保留未知字段，配合 `model_extra=None`（ConfigDict(model_extra=None)）限制只允许特定 extra 键。

**决策心法**：**"这个模型是给别人传的，还是自己内部用的？"**——别人传的 forbid、自己用的 ignore/allow。

## 3. from_attributes：ORM 与任意对象

```python
class UserORM:
    def __init__(self, name, age): self.name, self.age = name, age

class User(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    name: str
    age: int

u = User.model_validate(UserORM("x", 18))   # 从 ORM 对象/任意带属性的对象构造
```

**from_attributes=True 让 model_validate 接受"任意对象"**（按属性名取值）——**v1 的 orm_mode 迁移目标**。场景：**SQLAlchemy 模型 → pydantic DTO**（查询结果直接转响应模型）、dataclass/NamedTuple → 模型。**注意**：`from_attributes` 是**递归生效的**——嵌套子模型也要各自开（或顶层模型里子模型同样配置）。

**from_attributes 与 dataclass**：Python dataclass/NamedTuple 实例同样适用（属性名匹配）——"**任何带属性的对象都能当数据源**"；**注意嵌套 ORM 关系**（延迟加载对象）——访问属性可能触发额外查询，转 DTO 前先确保关联已加载。**配合 ORM 的经典姿势**：**读（ORM → DTO）用 from_attributes，写（DTO → ORM）用 model_dump(exclude_unset=True)**——只导出客户端显式传过的字段，避免默认值覆盖数据库存量（FastAPI 的 PATCH 语义，10 篇）。

## 4. strict：严格模式

**strict=False（默认）**：pydantic 执行**智能转换**——"18"→18、"true"→True、dict→模型、"1.5"→1.5……**宽容但可能不符合预期**（"18" 变 18 有时正是 bug 源头）。**strict=True**：**关闭所有转换，类型必须精确匹配**——str 必须是 str、int 必须是 int（bool 不算 int）。

**字段级严格**：`Field(strict=True)` / `Annotated[int, Strict()]` / `ConfigDict(strict=True)`（模型级）——**三档粒度**。**选型主线**：**默认宽松（生态兼容性：JSON 进来全是字符串）**；**货币/ID/关键业务字段单字段开 strict**（"订单号 '123' 不该自动变 123"）；**全模型严格适合"协议严格、转换由上层负责"的服务**。**心法**：宽松是"信任转换器"，严格是"信任调用方"——**边界层（外部输入）宽松 + 关键字段严格**是 2026 生产惯例；**strict 只影响当前模型，嵌套子模型各自决定**——别假设父级严格子级也严格。

## 5. frozen 与不可变性

**frozen=True**：模型实例不可修改——`user.name = "y"` 抛错；同时**模型获得 __hash__（可作 dict 键/set 成员）**。相关配置：`validate_assignment=True`（**赋值时重新校验**——"改字段也要过校验器"）、`revalidate_instances="never"`（嵌套实例默认不重复校验）。

**选型主线**：**配置对象/共享常量用 frozen**（防误改）；**DTO 用默认可变**（方便 model_copy 之外的小改动——其实生产里 DTO 也推荐"不可变更新"风格，05 篇 model_copy）；**需要"赋值即校验"的强约束模型用 validate_assignment**（金融/交易场景）——注意 validate_assignment 有性能代价（每次赋值过校验），高吞吐场景慎用。

## 6. 别名体系：populate_by_name 与 alias

```python
from pydantic import BaseModel, Field, ConfigDict

class Item(BaseModel):
    model_config = ConfigDict(populate_by_name=True, by_alias=True)
    item_id: int = Field(alias="id")     # 对外叫 id

Item(id=1)               # 用别名构造
Item(item_id=1)          # populate_by_name=True：字段名也接受
item.model_dump()        # by_alias=True：输出用别名（默认 False）
```

**别名的三个问题**：**为什么用**——外部契约（前端 camelCase、第三方 snake_case、数据库列名）与内部命名解耦；**怎么配**——`Field(alias=...)`（静态别名）或 `AliasChoices`（多别名优先序）；**三个开关**——`populate_by_name`（构造时接受字段名，**生产几乎必开**——否则用户要么记别名要么被别名困住）、`by_alias`（序列化时用别名输出）、`serialization_alias`（输入输出别名分离，极少用）。**alias 与 JSON Schema**：别名会出现在 OpenAPI 文档里——**"对外契约 = 别名，内部代码 = 字段名"**是 alias 的价值陈述。

## 7. 配置继承与常用组合

**继承规则**：子类继承父类 model_config，**子类可覆盖单项**（`model_config = ConfigDict(**父类config, **新配置)` 或直接覆盖 ConfigDict 字段）。**生产常用组合**（抄作业模板）：

```python
class DTO(BaseModel):
    """对外 DTO 基类：严进 + 别名 + 可赋值校验"""
    model_config = ConfigDict(
        extra="forbid", populate_by_name=True,
        by_alias=True, validate_assignment=True,
    )

class Cfg(BaseModel):
    """配置对象基类：冻结保护"""
    model_config = ConfigDict(frozen=True, extra="forbid")
```

**"DTO 严进 + 配置冻结 + 实体宽松"三个模板覆盖 90% 项目**——先抄模板，再按角色微调。

**配置命名规范**：模型名用后缀表角色——`OrderIn`（输入 DTO）/`OrderOut`（输出 DTO）/`Order`（实体）——**"名字即角色声明"**，团队读代码零成本；ConfigDict 也建议放在类第一行（一眼看到行为开关）。

## 8. 五个常见坑

- **坑一**：extra 忘了配——默认 ignore 静默吞字段，**边界模型必须显式写 forbid**（不写就是默认宽松）；
- **坑二**：from_attributes 只开了顶层——**嵌套子模型也要各自配**（或子模型继承配置）；
- **坑三**：strict=True 后 JSON 输入全炸——**JSON 里全是字符串，宽松模型配 strict 字段**（用 Field(strict=True) 单字段严格）；
- **坑四**：frozen 模型 + validate_assignment 混用——frozen 下赋值直接拒绝，validate_assignment 是"可变但校验"，**两者互斥的意图别混**；
- **坑五**：populate_by_name 没开，调用方只传字段名——**ValueError: field not in model**；生产 DTO 一律开。

## 9. 练习 5 题

1. model_config 的配置分哪四组？"角色决定配置"指什么？
2. extra 三值各适用什么场景？为什么边界模型要 forbid？
3. from_attributes 解决什么问题？读/写 ORM 的经典姿势？
4. strict 的三档粒度？"边界宽松 + 关键字段严格"怎么理解？
5. alias 的三个开关是什么？"对外契约 = 别名"指什么？

> 🎯 **核心要点**：ConfigDict = **模型的全局行为开关**——"别人传的 forbid、自己用的 ignore、配置对象 frozen、对外契约别名、关键字段严格"；**"DTO 严进 + 配置冻结 + 实体宽松"三个模板覆盖 90% 项目**。

---

**下一模块**：[07-高级类型与TypeAdapter.md](07-高级类型与TypeAdapter.md) / **返回总览**：[00-pydantic总览.md](00-pydantic总览.md)
