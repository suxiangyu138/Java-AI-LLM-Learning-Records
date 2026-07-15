# Java 国际化（i18n）全流程实战与注意事项

> **国际化（i18n = Internationalization）**：程序不修改核心业务代码的前提下，根据不同地区/语言环境，自动切换对应的文本、日期、时间、数字、货币等内容。本文从 JDK 原生 API 讲起，延伸到 Spring Boot 企业级方案，全程贴合实际项目落地场景。

---

## 目录

1. [核心基础概念](#1-核心基础概念)
2. [JDK 原生国际化 API](#2-jdk-原生国际化-api)
3. [核心注意事项与避坑指南](#3-核心注意事项与避坑指南)
4. [Spring Boot 国际化（企业级方案）](#4-spring-boot-国际化企业级方案)
5. [日期、货币、数字本地化](#5-日期货币数字本地化)
6. [总结](#6-总结)

---

## 1. 核心基础概念

### 1.1 核心术语

| 术语 | 全称 | 含义 |
|------|------|------|
| **i18n** | Internationalization | 国际化，程序支持多语言的基础能力 |
| **L10n** | Localization | 本地化，针对特定语言/地区做适配（如中文简体 vs 繁体） |
| **Locale** | — | Java 中表示区域和语言的核心类，是匹配多语言资源的唯一依据 |
| **ResourceBundle** | — | JDK 原生加载多语言配置文件的工具类 |
| **资源文件** | `.properties` | 存储多语言键值对的文件，命名必须遵循固定规范 |

### 1.2 Locale 常用示例

| Locale | 常量 | 说明 |
|--------|------|------|
| `zh_CN` | `Locale.CHINA` | 中文（中国大陆） |
| `zh_TW` | `new Locale("zh", "TW")` | 中文（中国台湾） |
| `en_US` | `Locale.US` | 英文（美国） |
| `ja_JP` | `Locale.JAPAN` | 日文（日本） |

### 1.3 核心设计原则

> **业务代码与语言文本彻底分离**

```text
硬编码 ❌：label.setText("欢迎")
国际化 ✅：label.setText(bundle.getString("welcome"))
```

所有可变文本（界面提示、按钮文字、报错信息）绝不硬编码在 Java 代码中，全部抽离到独立资源文件，通过 key 读取 value。切换语言只需更换资源文件，无需改动代码。

---

## 2. JDK 原生国际化 API

### 2.1 核心类一览

| 类 | 包 | 作用 |
|----|-----|------|
| `Locale` | `java.util` | 定义语言环境（语言 + 地区） |
| `ResourceBundle` | `java.util` | 抽象类，加载国际化资源文件 |
| `PropertyResourceBundle` | `java.util` | `ResourceBundle` 子类，读取 `.properties` 文件 |
| `MessageFormat` | `java.text` | 处理带占位符的国际化文本，支持动态参数替换 |
| `NumberFormat` | `java.text` | 数字/货币区域化格式 |
| `DateFormat` | `java.text` | 日期/时间区域化格式 |

### 2.2 资源文件命名规范（重中之重）

```
格式：baseName_language_country.properties

baseName    — 自定义基础名（如 message、i18n），所有语言文件共用
language    — 小写语言代码（zh、en、ja、ko）
country     — 大写国家/地区代码（CN、US、JP），可省略
```

**命名示例**：

| 文件名 | 用途 |
|--------|------|
| `messages.properties` | **默认资源文件（必选）**，找不到对应 Locale 时加载 |
| `messages_zh_CN.properties` | 中文简体（中国大陆） |
| `messages_en_US.properties` | 英文（美国） |
| `messages_zh_TW.properties` | 中文繁体（中国台湾） |

**关键注意事项**：

| 规则 | 说明 |
|------|------|
| 大小写敏感 | 语言小写、国家大写，不能写错 |
| 必须有默认文件 | 无语言后缀的默认文件，防止 Locale 不匹配时程序报错 |
| 编码限制 | JDK 原生要求 ISO-8859-1，中文需转 Unicode（或 IDE 自动转码） |

### 2.3 资源文件内容编写

> 所有语言文件用相同的 key，对应不同语言的 value，实现 **key 统一、value 差异化**。

**`messages.properties`**（默认，英文）：

```properties
welcome=Welcome
user.info=User Name: {0}, Age: {1}
submit=Submit
error.param=Parameter error
```

**`messages_zh_CN.properties`**（中文简体）：

```properties
welcome=欢迎
user.info=用户名：{0}，年龄：{1}
submit=提交
error.param=参数错误
```

### 2.4 原生 Java 代码完整示例

```java
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class JavaI18nDemo {
    public static void main(String[] args) {
        // 1. 指定 Locale
        Locale localeCN = new Locale("zh", "CN");
        Locale localeUS = Locale.US;

        // 2. 加载对应 Locale 的资源束
        ResourceBundle bundleCN = ResourceBundle.getBundle("messages", localeCN);
        ResourceBundle bundleUS = ResourceBundle.getBundle("messages", localeUS);

        // 3. 读取无占位符的文本
        String welcomeCN = bundleCN.getString("welcome");
        String welcomeUS = bundleUS.getString("welcome");
        System.out.println("中文欢迎：" + welcomeCN);  // 输出：欢迎
        System.out.println("英文欢迎：" + welcomeUS);  // 输出：Welcome

        // 4. 读取带占位符的文本，用 MessageFormat 替换参数
        String userInfoCN = bundleCN.getString("user.info");
        String resultCN = MessageFormat.format(userInfoCN, "张三", 25);
        System.out.println("中文用户信息：" + resultCN);
        // 输出：用户名：张三，年龄：25

        String userInfoUS = bundleUS.getString("user.info");
        String resultUS = MessageFormat.format(userInfoUS, "Tom", 25);
        System.out.println("英文用户信息：" + resultUS);
        // 输出：User Name: Tom, Age: 25

        // 5. 测试默认资源文件（Locale 不匹配时自动降级）
        Locale localeJP = new Locale("ja", "JP");
        ResourceBundle bundleJP = ResourceBundle.getBundle("messages", localeJP);
        System.out.println("日文环境默认文本：" + bundleJP.getString("welcome"));
        // 输出：Welcome（没有日文资源，降级到默认文件）
    }
}
```

### 2.5 Locale 匹配降级机制

```text
请求 messages_ja_JP.properties
    │
    ├── 存在？ → 使用
    └── 不存在？
          │
          ├── messages_ja.properties 存在？ → 使用
          └── 不存在？
                │
                └── messages.properties（默认）→ 兜底
```

---

## 3. 核心注意事项与避坑指南

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| **中文乱码** | JDK 原生默认 ISO-8859-1 编码 | ① `native2ascii` 工具转 Unicode；② IDEA 设置自动转码；③ Spring Boot 直接设 UTF-8 |
| **Locale 匹配不准** | 只传语言不传国家 | 推荐 `new Locale(language, country)` 或常量 `Locale.CHINA` |
| **占位符错误** | 参数个数与 `{0}{1}` 不匹配 | 参数个数必须与占位符一致，否则 `IllegalArgumentException` |
| **key 缺失** | 某语言文件缺少 key | 所有语言文件的 key 必须完全一致，建议用枚举类统一管理 |
| **线程安全** | `MessageFormat` 非线程安全 | 多线程下每次创建新实例，不要定义为静态常量 |
| **区域格式不匹配** | 只切换文本未切换格式 | 日期/货币/数字也需用 `DateFormat`/`NumberFormat` 适配 |
| **全局 Locale 污染** | 随意调 `Locale.setDefault()` | 不建议随意修改全局默认值，影响其他业务 |

### 3.1 key 统一管理推荐写法

```java
public enum I18nKey {
    WELCOME("welcome"),
    USER_INFO("user.info"),
    SUBMIT("submit"),
    ERROR_PARAM("error.param");

    private final String key;

    I18nKey(String key) { this.key = key; }
    public String key() { return key; }
}

// 使用
String text = bundle.getString(I18nKey.WELCOME.key());
```

---

## 4. Spring Boot 国际化（企业级方案）

> 实际企业开发中几乎不会用原生 JDK API。Spring Boot 对国际化做了全自动封装，无需手动 `ResourceBundle`。

### 4.1 application.yml 配置

```yaml
spring:
  messages:
    basename: i18n/messages      # 资源文件基础路径（resources/i18n/ 下）
    encoding: UTF-8              # 彻底解决中文乱码
    cache-duration: 3600s        # 生产环境缓存，提升性能

  mvc:
    locale: zh_CN                # 默认 Locale
    locale-resolver: parameter   # 切换方式：parameter/session/cookie
```

> 📁 资源文件放置位置：`resources/i18n/messages.properties`、`resources/i18n/messages_zh_CN.properties`

### 4.2 LocaleResolver 实现类对比

| 实现类 | 解析方式 | 适用场景 |
|--------|----------|----------|
| `AcceptHeaderLocaleResolver` | 请求头 `Accept-Language`（浏览器默认语言） | 无需手动切换，跟随浏览器设置 |
| `SessionLocaleResolver` | 会话级，切换后整个会话生效 | 登录后选择语言，单次会话内有效 |
| `CookieLocaleResolver` | Cookie 持久化语言偏好 | 记住用户语言选择，下次访问自动适配 |
| `FixedLocaleResolver` | 固定 Locale | 测试/特定场景 |

### 4.3 Controller 中获取国际化文本

```java
@RestController
public class UserController {

    @Autowired
    private MessageSource messageSource;

    @GetMapping("/welcome")
    public String welcome(Locale locale) {
        return messageSource.getMessage("welcome", null, locale);
    }

    @GetMapping("/user-info")
    public String userInfo(@RequestParam String name,
                           @RequestParam int age,
                           Locale locale) {
        return messageSource.getMessage(
            "user.info",
            new Object[]{name, age},  // 占位符参数
            locale
        );
    }
}
```

### 4.4 Thymeleaf 模板获取

```html
<!-- 无参数 -->
<h1 th:text="#{welcome}">Welcome</h1>

<!-- 带参数 -->
<p th:text="#{user.info(${name}, ${age})}">User Info</p>
```

### 4.5 全局异常处理中的国际化

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private MessageSource messageSource;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(
            BusinessException e, Locale locale) {
        String msg = messageSource.getMessage(e.getKey(), null, locale);
        return ResponseEntity.badRequest()
                .body(Map.of("error", msg));
    }
}
```

---

## 5. 日期、货币、数字本地化

> 完整的国际化不仅是文本切换，各类格式也要适配区域。

```java
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocaleFormatDemo {
    public static void main(String[] args) {
        Locale cn = Locale.CHINA;
        Locale us = Locale.US;

        // 日期格式化
        SimpleDateFormat dateCN = new SimpleDateFormat("yyyy-MM-dd", cn);
        SimpleDateFormat dateUS = new SimpleDateFormat("MM/dd/yyyy", us);
        System.out.println("中文日期：" + dateCN.format(new Date()));
        System.out.println("英文日期：" + dateUS.format(new Date()));

        // 货币格式化
        NumberFormat currencyCN = NumberFormat.getCurrencyInstance(cn);
        NumberFormat currencyUS = NumberFormat.getCurrencyInstance(us);
        System.out.println("人民币：" + currencyCN.format(1000));  // ¥1,000.00
        System.out.println("美元：" + currencyUS.format(1000));    // $1,000.00
    }
}
```

### 区域格式差异示例

| 类型 | 中国（zh_CN） | 美国（en_US） |
|------|--------------|--------------|
| 日期 | `2026-07-15` | `07/15/2026` |
| 货币 | `¥1,000.00` | `$1,000.00` |
| 数字 | `1,234.56` | `1,234.56` |
| 千分位 | `,`（逗号） | `,`（逗号） |

---

## 6. 总结

```text
Java 国际化三大核心
├── 文本与代码分离：所有文本抽离到 .properties，通过 key 读取
├── Locale 精准匹配：语言 + 地区严格匹配，默认文件兜底
└── 资源规范命名：baseName_language_country，key 所有文件一致

方案选择
├── 桌面应用 / 简单场景 → JDK 原生 API（ResourceBundle + MessageFormat）
└── Web 项目 / 企业级 → Spring Boot（自动封装 + UTF-8 + 动态切换）
```

| 落地检查项 | 要求 |
|-----------|------|
| 资源文件命名 | `baseName_lang_country.properties`，有默认文件 |
| 编码 | UTF-8（Spring Boot）/ ISO-8859-1（原生，需转 Unicode） |
| key 一致性 | 所有语言文件 key 完全一致 |
| 占位符 | 数字占位 `{0}{1}`，参数顺序正确 |
| 线程安全 | `MessageFormat` 每次新建 |
| 格式本地化 | 日期/货币/数字用 `DateFormat`/`NumberFormat` |

---

> 🎯 **核心总结**：Java 国际化的核心是**文本与代码分离、Locale 精准匹配、资源规范命名**。原生 API 适合基础桌面应用，Spring Boot 方案是 Web 项目的首选——简化配置、解决乱码、支持动态切换。落地时务必遵守命名、编码、key 统一三大规范。
