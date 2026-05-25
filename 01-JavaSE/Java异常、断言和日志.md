Java异常、断言和日志
在Java开发中，异常处理、断言和日志是保障程序稳定性、可调试性和可维护性的三大核心技术。异常处理用于捕获和处理程序运行时的错误，避免程序崩溃；断言用于开发阶段校验程序逻辑的正确性，快速定位问题；日志用于记录程序运行状态、错误信息，方便问题排查和系统监控。三者各司其职、相辅相成，是Java工程师必备的基础技能。本文将全面梳理三者的核心知识点、实战用法及注意事项，帮你快速掌握并灵活运用。
一、Java异常（Exception）
异常是Java程序运行过程中出现的不正常情况（如空指针、数组越界、文件找不到等），会中断程序的正常执行。Java通过“异常类”封装异常信息，通过“异常处理机制”捕获并处理异常，确保程序在出现错误时仍能优雅退出或继续执行，提升程序的健壮性。
1. 异常的分类（核心）
    Java异常体系的顶层是Throwable类，它有两个直接子类：Error（错误）和Exception（异常），二者的区别是学习的重点，也是面试高频考点。
    （1）Error（错误）
    Error是系统级错误，由JVM抛出，程序员无法通过代码捕获和处理，通常是硬件、虚拟机或系统资源耗尽导致的，程序只能终止运行。
    常见案例：OutOfMemoryError（内存溢出）、StackOverflowError（栈溢出）、VirtualMachineError（虚拟机错误）；
    注意：无需处理Error，只需优化程序（如减少内存占用）或升级系统资源。
    （2）Exception（异常）
    Exception是程序级异常，由程序逻辑错误或外部环境异常导致（如输入错误、文件不存在），程序员可以通过代码捕获和处理，避免程序崩溃，是我们重点学习和使用的部分。Exception又分为两大类：
    编译时异常（Checked Exception，受检异常）：编译阶段必须处理的异常，若不处理，程序无法通过编译，强制程序员提前考虑异常场景。 常见案例：IOException（文件读写异常）、SQLException（数据库操作异常）、ClassNotFoundException（类找不到异常）；
    运行时异常（Unchecked Exception，非受检异常）：编译阶段无需处理，运行时才会抛出的异常，通常是程序逻辑错误导致的，可选择性处理。 常见案例：NullPointerException（空指针异常）、ArrayIndexOutOfBoundsException（数组越界异常）、ClassCastException（类型转换异常）、ArithmeticException（算术异常，如除以0）。
2. 异常处理的核心机制（try-catch-finally-throw-throws）
    Java提供了5个关键字实现异常处理，核心逻辑是“捕获异常→处理异常→释放资源”，具体用法如下：
    （1）try-catch：捕获并处理异常
    try块用于包裹可能抛出异常的代码，catch块用于捕获try块中抛出的异常并处理，可多个catch块捕获不同类型的异常（从小到大捕获，避免父类异常覆盖子类异常）。
    // 基本语法
    try {
    // 可能抛出异常的代码（如文件读取、数组操作）
    } catch (异常类型1 异常变量名) {
    // 处理异常类型1的逻辑（如打印错误信息）
    } catch (异常类型2 异常变量名) {
    // 处理异常类型2的逻辑
    } catch (Exception e) { // 父类异常，放在最后，避免覆盖子类异常
    // 处理所有未捕获的异常
    }
    实战案例（处理空指针和算术异常）：
    public class ExceptionTest {
    public static void main(String[] args) {
        int a = 10;
        Integer b = null; // 可能导致空指针异常
        try {
            int result = a / (b - 5); // 可能抛出NullPointerException或ArithmeticException
            System.out.println("计算结果：" + result);
        } catch (NullPointerException e) {
            System.out.println("异常：空指针异常，b的值为null");
            e.printStackTrace(); // 打印异常堆栈信息，方便排查问题
        } catch (ArithmeticException e) {
            System.out.println("异常：算术异常，除数不能为0");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("其他异常：" + e.getMessage()); // 获取异常描述信息
        }
        // 异常处理后，程序继续执行
        System.out.println("程序正常结束");
    }
    }
    （2）finally：释放资源（必执行）
    finally块用于执行“无论是否抛出异常，都必须执行”的代码，核心作用是释放资源（如关闭文件、关闭数据库连接、释放网络资源），避免资源泄露。
    注意：finally块几乎都会执行，唯一不执行的情况是：try块中执行System.exit(0)（强制终止JVM）；
    语法：try-catch-finally 或 try-finally（无catch块，仅释放资源，异常会继续向上抛出）。
    // 实战案例：关闭文件资源
    import java.io.FileInputStream;
    import java.io.IOException;
    public class FinallyTest {
    public static void main(String[] args) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream("test.txt"); // 可能抛出IOException
            // 读取文件操作
        } catch (IOException e) {
            System.out.println("文件读取异常：" + e.getMessage());
        } finally {
            // 无论是否异常，都关闭文件流，释放资源
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("资源已释放");
        }
    }
    }
    （3）throw：主动抛出异常
    throw关键字用于主动抛出一个具体的异常对象，通常用于在方法内部判断逻辑错误时，手动触发异常（如参数校验不通过时）。
    // 实战案例：参数校验，主动抛出异常
    public class ThrowTest {
    // 计算两个数的商，要求除数不能为0，参数不能为null
    public static int divide(Integer a, Integer b) {
        // 主动校验参数，不满足条件则抛出异常
        if (a == null || b == null) {
            throw new NullPointerException("参数a或b不能为null");
        }
        if (b == 0) {
            throw new ArithmeticException("除数b不能为0");
        }
        return a / b;
    }
    public static void main(String[] args) {
        try {
            divide(10, 0);
        } catch (ArithmeticException e) {
            System.out.println("异常：" + e.getMessage()); // 输出：除数b不能为0
        }
    }
    }
    （4）throws：声明异常
    throws关键字用于在方法声明时，声明该方法可能抛出的异常，将异常的处理责任交给调用者（调用者需用try-catch处理，或继续用throws声明），常用于编译时异常（受检异常）。
    // 实战案例：声明异常，交给调用者处理
    import java.io.IOException;
    public class ThrowsTest {
    // 声明该方法可能抛出IOException（编译时异常）
    public static void readFile() throws IOException {
        FileInputStream fis = new FileInputStream("test.txt");
        fis.close();
    }
    public static void main(String[] args) {
        // 调用声明异常的方法，必须处理异常（try-catch或继续throws）
        try {
            readFile();
        } catch (IOException e) {
            System.out.println("文件读取异常：" + e.getMessage());
        }
    }
    }
3. 异常处理的注意事项（实战避坑）
    捕获异常时，遵循“从小到大”原则：先捕获子类异常，再捕获父类异常（如先捕获NullPointerException，再捕获Exception），避免父类异常覆盖子类异常，导致子类异常无法被针对性处理；
    避免“空catch块”：catch块中至少要打印异常信息（如e.printStackTrace()、e.getMessage()），否则异常发生后无法定位问题，难以排查；
    finally块中避免抛出异常：若finally块中抛出异常，会覆盖try/catch块中的异常，导致原异常信息丢失；
    合理使用throw和throws：throw用于主动抛出具体异常，throws用于声明异常，不要滥用throws（将所有异常都抛给调用者，会增加调用者的负担）；
    运行时异常可选择性处理：运行时异常（如空指针）通常是逻辑错误导致的，优先修复逻辑，而非捕获处理；编译时异常必须处理（try-catch或throws）。
    二、Java断言（Assertion）
    断言是Java提供的一种开发阶段的调试工具，用于校验程序中的“假设条件”是否成立（如“参数不为null”“变量值大于0”）。若假设成立，程序正常执行；若假设不成立，抛出AssertionError异常，中断程序运行，帮助开发者快速定位逻辑错误。
    核心作用：在开发阶段验证程序逻辑的正确性，相当于“开发者的自我检查”，上线后通常会关闭断言，避免影响程序运行。
    1. 断言的基本语法
    Java中使用assert关键字实现断言，有两种语法格式：
    // 格式1：仅校验条件，无异常提示信息
    assert 条件表达式;
    // 格式2：校验条件，若不成立，抛出带有提示信息的AssertionError
    assert 条件表达式 : 提示信息;
    说明：条件表达式的结果必须是boolean类型（true/false），若为false，抛出AssertionError；若为true，程序继续执行。
2. 断言的实战案例
    断言常用于参数校验、逻辑校验，适合开发阶段排查问题，示例如下：
    public class AssertTest {
    // 计算矩形面积，假设长和宽都大于0
    public static int calculateArea(int length, int width) {
        // 断言：长和宽必须大于0（开发阶段校验）
        assert length > 0 : "矩形的长必须大于0";
        assert width > 0 : "矩形的宽必须大于0";
        return length * width;
    }
    public static void main(String[] args) {
        // 正常情况：断言成立，程序执行
        int area1 = calculateArea(5, 3);
        System.out.println("矩形面积：" + area1);
        // 异常情况：断言不成立，抛出AssertionError
        int area2 = calculateArea(-2, 3);
        System.out.println("矩形面积：" + area2);
    }
    }
3. 断言的启用与关闭
    Java默认情况下，断言是关闭的（即使断言条件不成立，程序也不会抛出异常），需要手动启用断言，两种启用方式：
    命令行启用：运行Java程序时，添加参数-ea（enable assertions），示例：java -ea AssertTest；
    IDE中启用（以IDEA为例）：Run → Edit Configurations → 在VM options中输入-ea → 点击Apply，运行程序即可启用断言。
    关闭断言：无需添加-ea参数，默认关闭；若已启用，可添加-da（disable assertions）参数关闭。
4. 断言的注意事项（实战避坑）
    断言仅用于开发阶段：上线后的程序必须关闭断言，因为断言会影响程序性能，且AssertionError是Error的子类，无法通过try-catch捕获，会导致程序崩溃；
    不要用断言替代异常处理：断言用于“开发阶段的逻辑校验”，异常用于“运行时的错误处理”，例如参数校验，开发阶段用断言，上线后用异常处理（如throw抛出异常）；
    断言条件不能有副作用：断言的条件表达式不能包含修改程序状态的代码（如assert (i++ > 0)），因为关闭断言后，该代码不会执行，导致程序逻辑异常；
    不要用断言校验用户输入：用户输入的合法性校验，必须用异常处理，不能用断言（因为上线后断言关闭，校验会失效）。
    三、Java日志（Logging）
    日志是Java程序运行过程中，对程序状态、操作行为、错误信息的持久化记录，核心作用是“排查问题、监控系统、追踪用户行为”。与System.out.println()相比，日志更灵活（可控制输出级别、输出位置）、更规范（支持分级、格式化），是企业开发中必备的工具。
    Java中常用的日志框架：JDK自带的java.util.logging（简单易用，适合小型项目）、Log4j（经典日志框架）、Logback（Log4j的升级版本，性能更优，推荐使用）、SLF4J（日志门面，统一日志接口，可切换不同日志实现）。
    1. 日志的核心概念（通用）
    （1）日志级别（从低到高，可控制输出）
    所有日志框架都有统一的日志级别，用于控制日志的输出范围（级别越高，输出的日志越少），常用级别如下（以Logback为例）：
    TRACE：最详细的日志，用于追踪程序的每一步执行（开发阶段调试，上线后关闭）；
    DEBUG：调试日志，用于记录程序的关键执行步骤（开发阶段常用，上线后可关闭）；
    INFO：信息日志，用于记录程序的正常运行状态（如“程序启动成功”“用户登录成功”，上线后保留）；
    WARN：警告日志，用于记录潜在的风险（如“参数不合法，但不影响程序执行”，上线后保留）；
    ERROR：错误日志，用于记录程序运行时的错误（如“数据库连接失败”“空指针异常”，必须保留，用于排查问题）；
    FATAL：致命日志，用于记录导致程序崩溃的严重错误（如“内存溢出”，极少出现，必须保留）。
    （2）日志输出位置
    日志可输出到多个位置，满足不同需求：
    控制台（Console）：开发阶段常用，方便实时查看日志；
    文件（File）：上线后常用，将日志写入文件，持久化保存，便于后续排查问题；
    数据库（Database）：大型项目常用，将日志存入数据库，便于日志分析、统计和监控。
2. 常用日志框架实战（Logback，推荐）
    Logback是目前最流行的日志框架，性能优于Log4j，配置简单，支持多种输出方式，是企业开发的首选。以下是Logback的基础使用步骤：
    （1）导入依赖（Maven项目）
    <!-- Logback核心依赖 -->
    <dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.8</version>
    </dependency>
    <!-- SLF4J日志门面（统一接口） -->
    <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.7</version>
    </dependency>
    （2）配置Logback（核心）
    在项目的src/main/resources目录下，创建Logback配置文件logback.xml，配置日志级别、输出位置、日志格式，示例配置如下（基础版）：
    <?xml version="1.0" encoding="UTF-8"?>
    <configuration>
    <!-- 1. 控制台输出配置 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- 日志格式：时间 日志级别 类名 日志信息 -->
            %d{yyyy-MM-dd HH:mm:ss} [%level] %logger{36} - %msg%n
        </encoder>
    </appender>
    <!-- 2. 文件输出配置（按日期滚动，避免单个文件过大） -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <!-- 日志文件路径及名称 -->
        logs/csdn.log<!-- 滚动策略：按日期滚动，每天一个文件 -->
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/csdn.%d{yyyy-MM-dd}.log</fileNamePattern>
            <!-- 日志文件保留7天 -->
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder>
            %d{yyyy-MM-dd HH:mm:ss} [%level] %logger{36} - %msg%n
        </encoder>
    </appender>
    <!-- 3. 全局日志级别配置（默认INFO级别） -->
    <root level="INFO">
        <!-- 关联控制台和文件输出 -->
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
    <!-- 4. 单独配置某个包的日志级别（如com.example包，日志级别为DEBUG） -->
    <logger name="com.example" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </logger>
    </configuration>
    （3）在代码中使用Logback
    通过SLF4J的LoggerFactory获取日志对象，然后调用对应级别的日志方法（debug、info、warn、error等）：
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    public class LogbackTest {
    // 获取日志对象（参数为当前类的class）
    private static final Logger logger = LoggerFactory.getLogger(LogbackTest.class);
    public static void main(String[] args) {
        // 不同级别的日志输出
        logger.trace("TRACE级日志：程序追踪信息");
        logger.debug("DEBUG级日志：调试信息，当前用户ID：{}", 1001); // 支持占位符
        logger.info("INFO级日志：程序启动成功");
        logger.warn("WARN级日志：参数异常，当前参数：{}", null);
        try {
            int a = 10 / 0;
        } catch (ArithmeticException e) {
            // 输出错误日志，包含异常堆栈信息
            logger.error("ERROR级日志：计算异常", e);
        }
        logger.fatal("FATAL级日志：致命错误，程序即将崩溃");
    }
    }
3. 日志使用的注意事项（实战避坑）
    选择合适的日志级别：开发阶段可用DEBUG/TRACE，上线后切换为INFO及以上级别，避免过多日志占用磁盘空间、影响程序性能；
    避免输出敏感信息：日志中不要输出密码、手机号、身份证号等敏感信息，防止信息泄露；
    使用占位符输出日志：避免使用字符串拼接（如logger.info("用户ID：" + userId)），改用占位符（logger.info("用户ID：{}", userId)），提升性能（未达到日志级别时，不会执行字符串拼接）；
    合理配置日志文件：使用滚动日志（按日期/大小滚动），设置日志保留时间，避免单个日志文件过大，便于排查和清理；
    统一日志框架：项目中只使用一种日志框架，避免多种日志框架冲突（推荐使用SLF4J+Logback，统一接口，便于后续切换实现）；
    不要用日志替代异常处理：日志用于记录错误信息，异常处理用于捕获和处理错误，二者结合使用（如捕获异常后，输出error日志，再进行后续处理）。
    四、异常、断言和日志的区别与联系
    1. 核心区别
    技术
    核心作用
    使用阶段
    是否可捕获
    核心目标
    异常
    处理程序运行时的错误，避免程序崩溃
    开发+上线
    可捕获（Exception）/不可捕获（Error）
    保证程序健壮性，优雅处理错误
    断言
    校验开发阶段的逻辑假设，快速定位问题
    仅开发阶段
    不可捕获（AssertionError是Error）
    验证程序逻辑正确性，辅助调试
    日志
    记录程序运行状态、错误信息，便于排查
    开发+上线
    无“捕获”概念，仅记录
    监控系统、追踪问题、分析行为
2. 核心联系
    三者协同保障程序稳定性：异常处理避免程序崩溃，断言在开发阶段提前发现逻辑错误，日志在运行阶段记录错误信息，便于后续排查；
    异常与日志结合使用：捕获异常后，通过日志输出异常信息（如堆栈信息），方便开发者定位异常原因；
    断言与异常互补：开发阶段用断言校验逻辑，上线后用异常处理替代断言，确保参数校验、逻辑校验不失效。
    五、总结
    1. 异常是程序运行时的错误，分为Error（不可处理）和Exception（可处理），核心通过try-catch-finally-throw-throws实现处理，重点是“优雅处理错误，避免程序崩溃”；
    2. 断言是开发阶段的调试工具，用于校验逻辑假设，启用后条件不成立会抛出AssertionError，上线后必须关闭，重点是“辅助调试，快速定位逻辑错误”；
    3. 日志是程序运行的记录工具，通过不同级别控制输出，可输出到控制台、文件等位置，重点是“记录信息，便于排查问题和监控系统”；
    4. 实战中，三者协同使用：开发阶段用断言+日志调试，上线后用异常+日志保障程序稳定，避免滥用断言、空catch块、敏感日志等常见误区；
    5. 面试重点：异常的分类、异常处理机制、断言的作用与启用方式、日志级别与使用规范，需结合实战案例记忆，掌握三者的区别与联系。
