# 07 - 高级类型与 TypeAdapter

> 定位：pydantic 的进阶武器库——无模型校验（TypeAdapter）、按内容分派（判别联合）、配置管理（pydantic-settings）、特殊类型——"模型是常规武器，TypeAdapter 与判别联合是处理'非模型数据'与'多态数据'的专用弹药"

---

## 📚 目录

1. [TypeAdapter：无模型校验](#1-typeadapter无模型校验)
2. [判别联合：按 tag 分派](#2-判别联合按-tag-分派)
3. [pydantic-settings：配置管理](#3-pydantic-settings配置管理)
4. [特殊类型家族](#4-特殊类型家族)
5. [严格模式进阶：Strict 系列](#5-严格模式进阶strict-系列)
6. [私有属性与模型扩展](#6-私有属性与模型扩展)
7. [实战模式](#7-实战模式)
8. [五个常见坑](#8-五个常见坑)
9. [练习 5 题](#9-练习-5-题)

---

## 1. TypeAdapter：无模型校验

```python
from pydantic import TypeAdapter

# 任意类型：不需要定义 BaseModel
ta = TypeAdapter(list[dict[str, int]])
ta.validate_python([{"a": "1"}])        # [{'a': 1}]（字符串转 int）
ta.validate_json('[{"a": "1"}]')        # JSON 直验（Rust 层解析，最快）
```

**TypeAdapter 解决"类型不是模型"的校验需求**：单字段类型、容器类型、任意联合——**"不值得建模型的数据，用一个 TypeAdapter 搞定"**。三个高频场景：**批量校验列表**（一万条记录的 schema 校验，TypeAdapter 编译一次复用）；**单值校验**（`TypeAdapter(int).validate_python("18")`）；**泛化工具**（把"任意类型"作为参数传给函数——**"运行时把类型注解变成校验器"的桥梁**，AI 工具注册场景大量使用，09 篇）。**性能特征**：TypeAdapter 编译一次核心 schema 后可复用——**批量场景比"循环构造模型"快一个量级**（08 篇实测方法论）。

**TypeAdapter 的错误处理**：同样返回 ValidationError（含 loc/type/msg）——`TypeAdapter(list[T]).validate_python` 的错误 loc 带列表下标——**批量场景错误定位依然精确**，不会"一批错一个不知道哪个"。

## 2. 判别联合：按 tag 分派

```python
from typing import Literal, Union
from pydantic import BaseModel, Field

class Add(BaseModel):
    kind: Literal["add"]
    x: int; y: int

class Mul(BaseModel):
    kind: Literal["mul"]
    x: int; y: int

Op = Annotated[Union[Add, Mul], Field(discriminator="kind")]

class Calc(BaseModel):
    ops: list[Op]

Calc.model_validate({"ops": [{"kind": "mul", "x": 2, "y": 3}]})  # 按 kind 直接分派
```

**判别联合（Discriminated Union）解决普通 Union 的三个痛点**：**按内容试错太慢**（普通 Union 逐个尝试）、**错误信息差**（"不匹配 Union 任何成员"，不知道哪个成员差什么字段）、**歧义**（两个成员结构相似时可能选错）。**机制**：通过 `Field(discriminator="tag")` 声明**判别字段**（通常配 Literal），校验时**只看 tag 值一步分派**——快、错误精确（"kind='mul' 但缺 y 字段"）。**生产主线**：**事件/消息/响应体等"多态数据"一律判别联合**——RAG 里的工具调用、LLM 的响应类型、消息队列的事件体都是它的主场（09 篇）。**2.13 增强**：Literal 根类型也可作判别字段类型。

## 3. pydantic-settings：配置管理

```bash
pip install "pydantic-settings"
```

```python
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="APP_",          # 环境变量前缀：APP_DATABASE_URL
        env_file=".env",            # 读取 .env 文件
        env_file_encoding="utf-8",  # 编码显式（Windows 默认非 utf-8）
        extra="forbid",
    )
    database_url: str
    api_key: str
    debug: bool = False

s = Settings()   # 自动合并：.env < 环境变量（环境变量优先）
```

**BaseSettings 在 v2 移到了独立包 pydantic-settings**（pydantic 主包不再带它）——**从 pydantic 导入直接 ImportError**。**能力**：环境变量/`.env` 文件 → 强类型配置对象（**校验在加载时发生**：`APP_DATABASE_URL` 缺失直接报错，快速失败）。**与 pyyaml 的分工**（00 篇）：**yaml 管"结构化配置"（模型参数/嵌套配置，pyyaml 读取 + pydantic 校验），settings 管"部署配置"（环境差异/密钥，环境变量 + .env）**——"**yaml 写业务配置、env 写部署配置、pydantic 校验两者**"。**安全纪律**：**密钥只走环境变量/密钥管理，永不进 yaml 与代码**；settings 对象全局单例（lifespan 启动时加载一次）。

**Settings 的嵌套配置**：`class DB(BaseModel): host: str; port: int` + Settings 里 `db: DB`——环境变量 `APP_DB_HOST` 自动映射嵌套字段（`env_nested_delimiter="__"` 配置分隔符）——"**部署配置也可以分层**"，复杂应用的 Settings 结构能保持可读。

## 4. 特殊类型家族

**pydantic 内置一批开箱即用的领域类型**：`EmailStr`（邮箱格式，需 `pip install "pydantic[email]"`）、`HttpUrl`（URL 校验，含 scheme 要求）、`IPvAnyAddress`（IPv4/IPv6）、`UUID`（自动解析）、`SecretStr`（**密钥字段：repr 与输出自动打码**，`SecretStr("x").get_secret_value()` 取值）、`Json`（JSON 字符串字段）、`PastDatetime/FutureDatetime`（时间语义）、`ConstrainedStr/ConstrainedInt`（约束别名，老写法，新代码用 Annotated）。

**两个高价值认知**：其一，**SecretStr 是"密钥字段"的事实标准**——模型里存密钥就声明 SecretStr，**日志/序列化自动脱敏**（05 篇 field_serializer 之外的天然防线）；其二，**EmailStr 等类型校验在 Rust 层**（email-validator 为可选依赖）——**"字段级语义"用内置类型表达，比手写校验器省事且快**；其三，**ConstrainedStr/ConstrainedInt 是 v1 遗留别名**——老教程读者注意，**新代码一律 Annotated**（03 篇写法）。

## 5. 严格模式进阶：Strict 系列

```python
from pydantic import StrictInt, StrictStr, StrictBool, StrictFloat

class S(BaseModel):
    a: StrictInt       # 必须是真 int，不接受 "18"
    b: StrictStr       # 必须是真 str，不接受 123
```

**Strict 类型是"字段级严格"的类型写法**（等价 `Field(strict=True)`）：与 06 篇 ConfigDict(strict=True) 的区别在**粒度**——**配置级严格影响全模型，Strict 类型只影响单个字段**。**组合姿势**：模型默认宽松（兼容 JSON 字符串输入）+ 关键字段用 Strict 类型（ID/枚举值/金额）——"**全局宽松、局部严格**"。**注意**：StrictBool 严格到"bool 必须是 bool"——`1`/`"true"` 全部拒绝，**用之前想清楚调用方的数据形态**。

## 6. 私有属性与模型扩展

```python
from pydantic import BaseModel, PrivateAttr

class Model(BaseModel):
    name: str
    _cache: dict = PrivateAttr(default_factory=dict)   # 不进序列化/校验

    @property
    def cached(self): return self._cache
```

**PrivateAttr 是"模型内部状态"的容器**：以 `_` 开头的私有属性**不参与校验、不进 model_dump**——**缓存、临时状态、依赖注入**都放这里。**v2.13 新特性**：私有属性的默认工厂（default_factory）**可以接收已校验的模型数据**——如 `_ctx: dict = PrivateAttr(default_factory=lambda m: {...m.model_dump()})`，按模型内容初始化内部状态。**边界**：私有属性是"进程内状态"，**序列化默认不带走，但 model_copy 会复制**（深拷贝场景注意）；"想清楚哪些是数据、哪些是状态"是使用私有属性的心法。

## 7. 实战模式

**模式一：批量数据管道**——`TypeAdapter(list[Record])` 编译一次，逐批 validate_json——**ETL/日志回放/批量导入的骨架**。

**模式二：事件分派**——判别联合 + tag 字段，按 kind 分发到不同处理器——**"解析与分派解耦：先解析成强类型事件，再按类型分派"**。

**模式三：配置三件套**——`Settings（部署配置）+ pydantic 模型（业务配置）+ pyyaml（读取 yaml）`——"**配置加载时校验，缺失快速失败，密钥只进 env**"。

**模式四：类型即接口**——TypeAdapter 把"任意类型"暴露给外部调用方做校验（AI 工具注册，09 篇）——"**类型注解即 API 契约**"。

## 8. 五个常见坑

- **坑一**：判别联合的 tag 字段必须是 Literal/枚举——**tag 字段类型不对直接报 SchemaError**；
- **坑二**：BaseSettings 从 pydantic 导入——**ImportError；from pydantic_settings import BaseSettings**；
- **坑三**：SecretStr 忘了 get_secret_value()——**把 SecretStr 对象当字符串用报类型错**；
- **坑四**：TypeAdapter 每次调用重新构造——**性能白给；模块级/类级构造一次复用**；
- **坑五**：EmailStr 未装 email 依赖——**运行时 ImportError；`pip install "pydantic[email]"`**。

## 9. 练习 5 题

1. TypeAdapter 解决什么问题？三个高频场景？
2. 判别联合相比普通 Union 的三个优势？tag 字段有什么要求？
3. BaseSettings 为什么单独成包？与 pyyaml 的分工？
4. SecretStr 的价值？Strict 系列与 ConfigDict(strict=True) 的区别？
5. PrivateAttr 的定位？2.13 的默认工厂新能力？

> 🎯 **核心要点**：高级武器库 = **TypeAdapter（无模型校验/批量 JSON 直验）+ 判别联合（多态数据按 tag 一步分派）+ pydantic-settings（配置加载即校验）+ SecretStr（密钥天然脱敏）**——"**不值得建模型的用 TypeAdapter，多态的用判别联合，部署配置交给 Settings**"。

---

**下一模块**：[08-性能与生产实践.md](08-性能与生产实践.md) / **返回总览**：[00-pydantic总览.md](00-pydantic总览.md)
