# Java异常、断言和日志
异常、断言、日志是Java项目保障健壮性、调试效率、线上问题排查的三大基础配套技术。
- 异常：运行错误捕获处理，防止程序直接崩溃，线上核心容错手段
- 断言：开发期逻辑校验工具，快速发现代码假设漏洞
- 日志：持久化记录运行状态，替代`System.out`，线上唯一排查依据

## 一、Java异常体系（Exception）
### 1. 整体继承结构
顶层父类：`Throwable`，两大分支
1. **Error 系统级错误**
    JVM/硬件/资源故障，代码无法捕获修复，程序只能终止
    典型：`StackOverflowError`栈溢出、`OutOfMemoryError`内存溢出
2. **Exception 程序异常（开发重点）**
    代码/外部环境导致，可捕获处理，分为两类：
    - 受检异常 CheckedException：编译强制处理，不写try-catch/throws直接编译报错
      例：`IOException`文件、`SQLException`数据库、`ClassNotFoundException`
    - 非受检异常 RuntimeException：编译不强制处理，根源是代码逻辑bug
      例：空指针`NullPointerException`、数组越界、除零`ArithmeticException`、类型转换异常

### 2. 五大异常关键字：try / catch / finally / throw / throws
#### （1）try-catch 捕获处理异常
- try：包裹可能报错的代码
- 多catch顺序：**先子类、后父类**，父类Exception必须放最后，否则会覆盖子类异常
```java
public class TryCatchDemo {
    public static void main(String[] args) {
        Integer num = null;
        try {
            int res = 10 / (num - 2);
        } catch (NullPointerException e) {
            System.out.println("空指针：" + e.getMessage());
            e.printStackTrace();
        } catch (ArithmeticException e) {
            System.out.println("除数不能为0");
        } catch (Exception e) {
            System.out.println("其他未知异常");
        }
        System.out.println("程序继续执行");
    }
}
```

#### （2）finally 资源释放，几乎必执行
核心用途：关闭IO流、数据库连接、Socket，防止资源泄漏
唯一不执行场景：try内部执行`System.exit(0)`关闭JVM
```java
import java.io.FileInputStream;
import java.io.IOException;

public class FinallyDemo {
    public static void main(String[] args) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream("test.txt");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("资源释放完成");
        }
    }
}
```

#### （3）throw 手动抛出异常对象
方法内校验参数不合法，主动抛出异常中断执行
```java
public class ThrowDemo {
    public static int div(Integer a, Integer b) {
        if (a == null || b == null) {
            throw new NullPointerException("参数不能为null");
        }
        if (b == 0) {
            throw new ArithmeticException("除数禁止为0");
        }
        return a / b;
    }

    public static void main(String[] args) {
        try {
            div(10, 0);
        } catch (ArithmeticException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

#### （4）throws 方法声明异常
写在方法签名后，告知调用方本方法可能抛出受检异常，异常向上抛，由调用者处理
```java
import java.io.FileInputStream;
import java.io.IOException;

public class ThrowsDemo {
    // 声明受检异常，强制调用者处理
    public static void readFile() throws IOException {
        FileInputStream fis = new FileInputStream("test.txt");
        fis.close();
    }

    public static void main(String[] args) {
        try {
            readFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### 3. 异常开发避坑规范
1. 禁止空catch块，至少打印堆栈/记录日志，否则异常无声消失
2. 多catch严格从小到大排序，父类Exception后置
3. finally中尽量不抛出新异常，会覆盖原始异常信息
4. RuntimeException优先修复代码逻辑，不要无脑捕获；Checked异常必须处理
5. 不要滥用throws，层层上抛会导致顶层代码臃肿

## 二、断言 Assert
断言是**开发期调试工具**，校验代码预设条件，条件为false抛出`AssertionError`（Error子类，无法捕获）。上线后关闭，不影响性能。

### 1. 两种语法
```java
// 格式1：无提示信息
assert 布尔表达式;

// 格式2：失败携带提示文案
assert 布尔表达式 : "错误提示信息";
```

### 2. 代码示例
```java
public class AssertDemo {
    public static int getArea(int w, int h) {
        // 校验长宽正数，仅开发阶段生效
        assert w > 0 : "宽度必须大于0";
        assert h > 0 : "高度必须大于0";
        return w * h;
    }

    public static void main(String[] args) {
        getArea(-1, 5);
    }
}
```

### 3. 启用/关闭断言
- 启用：运行参数加 `-ea`（enable assertions）
  命令行：`java -ea AssertDemo`
  IDEA：Run Configurations → VM options 填写 `-ea`
- 关闭：默认关闭，或添加 `-da`

### 4. 使用限制（高频考点）
1. 上线必须关闭，AssertionError属于Error，会直接宕机
2. 不能替代异常处理：用户输入、参数校验线上必须用throw抛异常
3. 断言表达式禁止带副作用代码（i++、赋值等），关闭断言时代码不执行，逻辑错乱
4. 不能用于校验外部用户输入，仅校验开发者自身代码逻辑假设

## 三、Java日志体系
`System.out`仅适用于本地简单测试；企业统一使用日志框架，支持分级、持久化、滚动文件、脱敏。

### 1. 主流框架分工
- SLF4J：日志门面（统一接口），隔离业务代码与底层实现
- Logback：SLF4J官方实现，性能最优，项目首选
- Log4j2：老牌高性能实现
- JUL：JDK自带日志，小型简单项目使用

### 2. 日志级别（从低到高）
级别配置后，仅输出**大于等于配置级别的日志**
1. TRACE：详细追踪，开发调试，线上关闭
2. DEBUG：关键步骤调试，测试环境开启
3. INFO：正常业务运行记录，线上基础保留级别
4. WARN：潜在风险，不阻断流程，需要关注
5. ERROR：业务异常、报错，必须记录堆栈，线上必开
6. FATAL：致命错误，系统无法继续运行

### 3. Logback 完整使用示例
#### Maven依赖
```xml
<!-- 日志门面 -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.7</version>
</dependency>
<!-- logback实现 -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.8</version>
</dependency>
```

#### resources/logback.xml 基础配置
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%level] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <!-- 按日期滚动文件 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/app-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%level] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <!-- 全局默认日志级别 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
    <!-- 指定包单独调试级别 -->
    <logger name="com.example" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
    </logger>
</configuration>
```

#### 代码打印日志
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogDemo {
    private static final Logger logger = LoggerFactory.getLogger(LogDemo.class);

    public static void main(String[] args) {
        logger.trace("追踪日志");
        logger.debug("用户id：{}", 10001);
        logger.info("服务启动成功");
        logger.warn("参数为空，存在风险");
        try {
            int i = 1 / 0;
        } catch (Exception e) {
            // 打印完整异常堆栈
            logger.error("计算出错", e);
        }
    }
}
```

### 4. 日志编码规范
1. 使用占位符`{}`，禁止字符串拼接，级别不匹配时不执行拼接，提升性能
2. 日志禁止打印手机号、身份证、密码等敏感信息
3. 线上使用INFO级别，关闭DEBUG/TRACE，减少磁盘占用
4. 捕获异常后必须使用`logger.error(msg, e)`打印完整堆栈，只打印getMessage无法定位代码行
5. 使用滚动日志，设置过期清理，防止单个日志文件超大

## 四、异常 / 断言 / 日志 对比汇总
| 技术 | 使用阶段 | 核心作用 | 能否捕获 | 线上是否启用 |
|------|----------|----------|----------|--------------|
| 异常 | 开发+线上 | 容错，避免程序崩溃 | Exception可捕获，Error不可 | 必须全程使用 |
| 断言 | 仅开发调试 | 校验代码内部逻辑假设 | AssertionError无法捕获 | 必须关闭 |
| 日志 | 开发+线上 | 记录运行信息、异常堆栈，问题排查 | 无捕获概念，仅记录 | 全程开启 |

### 协同开发规范
1. 开发期：断言校验内部逻辑 + 日志打印调试信息
2. 上线后：断言关闭，依靠异常捕获容错 + 日志记录所有报错
3. 标准流程：try-catch捕获异常 → error级别日志打印堆栈 → 根据业务判断重试/返回友好提示

## 五、面试核心背诵总结
1. **异常分类**
Throwable分Error和Exception；Exception分为受检异常（编译强制处理）、运行时异常（代码逻辑错误）。
2. **finally特点**
资源释放专用，绝大多数场景都会执行，仅`System.exit()`会跳过。
3. **断言核心要点**
开发调试工具，-ea启用，上线关闭；不能替代异常、不能包含副作用代码。
4. **日志分级与规范**
TRACE<DEBUG<INFO<WARN<ERROR<FATAL；推荐SLF4J+Logback，使用占位符，异常打印完整堆栈。
5. **三者配合逻辑**
断言提前在开发发现逻辑漏洞；异常保证线上程序不崩溃；日志留存报错信息用于事后定位修复。