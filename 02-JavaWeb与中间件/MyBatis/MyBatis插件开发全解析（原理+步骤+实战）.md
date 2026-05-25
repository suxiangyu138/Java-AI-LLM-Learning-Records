MyBatis插件开发全解析（原理+步骤+实战）
一、MyBatis插件核心认知
1.1 插件本质与作用
MyBatis插件本质是基于拦截器模式和JDK动态代理实现的扩展组件，核心作用是在不修改MyBatis源码的前提下，对SQL执行全流程进行增强，比如实现SQL日志打印、性能监控、数据脱敏、分页处理、参数加密等横切功能。
MyBatis插件并非可拦截任意方法，仅支持对四大核心组件的特定方法进行拦截，这是插件开发的核心前提。
1.2 可拦截的四大核心组件
MyBatis预定义了4个可被插件拦截的核心接口，每个接口对应SQL执行的不同环节，具体如下：
Executor：SQL执行器，负责SQL的最终执行（增删改查、事务提交回滚、缓存维护），可拦截query、update、commit、rollback等方法，是最常用的拦截点之一。
StatementHandler：语句处理器，负责JDBC Statement的创建、参数设置和SQL执行，可拦截prepare、parameterize、query、update等方法，常用于SQL重写（如物理分页）、SQL审计。
ParameterHandler：参数处理器，负责将Java参数设置到JDBC的PreparedStatement中，可拦截setParameters方法，用于参数加密、特殊类型处理。
ResultSetHandler：结果集处理器，负责将JDBC返回的ResultSet映射为Java对象，可拦截handleResultSets、handleOutputParameters方法，用于结果脱敏、自定义类型转换。
1.3 插件核心原理
MyBatis插件的运行依赖三大核心机制，协同完成拦截增强功能：
JDK动态代理：MyBatis通过JDK动态代理为四大核心组件生成代理对象，代理对象会拦截目标方法的调用，优先执行插件的自定义逻辑。
责任链模式：多个插件会按配置顺序层层包装目标对象，形成插件链（InterceptorChain）。调用方法时，请求会从最外层插件依次传递到最内层的真实对象，执行完成后再反向返回，实现多插件协同增强。
注解声明机制：通过@Intercepts和@Signature注解，明确插件要拦截的组件、方法及参数，MyBatis通过解析这些注解构建拦截签名映射，确定是否对目标对象进行代理。
二、MyBatis插件开发核心接口与注解
2.1 核心接口：Interceptor
所有自定义MyBatis插件都必须实现org.apache.ibatis.plugin.Interceptor接口，该接口包含3个核心方法，缺一不可，具体说明如下：
public interface Interceptor {
    // 核心方法：拦截目标方法，编写自定义增强逻辑
    Object intercept(Invocation invocation) throws Throwable;
    // 生成目标对象的代理对象，默认使用Plugin.wrap()实现
    default Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    // 读取插件配置的属性（从MyBatis配置文件中获取）
    default void setProperties(Properties properties) {}
}
各方法详解：
intercept(Invocation invocation)：插件的核心逻辑入口，Invocation对象封装了目标对象、目标方法、方法参数等信息。通过invocation.proceed()可调用原方法（或下一个插件的拦截逻辑），若不调用则会阻断原方法执行。
plugin(Object target)：用于判断是否需要为目标对象生成代理，默认实现调用Plugin.wrap(target, this)，该方法会根据注解声明的拦截签名，为符合条件的目标对象创建代理。
setProperties(Properties properties)：用于接收MyBatis配置文件中为插件配置的属性（如加密密钥、日志级别），实现插件参数的灵活配置。
2.2 核心注解：@Intercepts与@Signature
这两个注解用于声明插件的拦截目标，必须标注在Interceptor实现类上，缺一不可：
@Intercepts：标识该类是一个MyBatis插件，内部包含一个@Signature数组，可声明多个拦截目标（即一个插件可拦截多个组件的多个方法）。
@Signature：定义具体的拦截签名，精准匹配要拦截的方法，包含3个属性：
type：要拦截的核心组件接口（如Executor.class、StatementHandler.class）；
method：要拦截的方法名（字符串，如"query"、"prepare"）；
args：要拦截方法的参数类型数组，需与目标方法的参数类型、顺序完全一致（用于区分重载方法）。
注解使用示例：
@Intercepts({
    // 拦截Executor的query方法
    @Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
    ),
    // 拦截StatementHandler的prepare方法
    @Signature(
        type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class, Integer.class}
    )
})
public class MyCustomPlugin implements Interceptor {
    // 实现接口方法...
}
三、MyBatis插件开发完整步骤（实战）
以开发一个「SQL执行时间监控插件」为例，完整演示插件开发、配置、测试的全流程，该插件可统计每一条SQL的执行耗时并打印日志。
3.1 步骤1：导入依赖（Maven）
确保项目中引入MyBatis核心依赖（若使用Spring Boot，可引入mybatis-spring-boot-starter）：
<!-- MyBatis核心依赖 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.16</version>
</dependency>
<!-- 数据库驱动（根据实际数据库选择） -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.36</version>
    <scope>runtime</scope>
</dependency>
3.2 步骤2：编写自定义插件类
实现Interceptor接口，添加注解声明拦截目标，编写SQL执行时间监控逻辑：
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import java.util.Properties;
/**
 * MyBatis插件：SQL执行时间监控
     */
    @Intercepts({
    // 拦截Executor的query方法（查询操作）
    @Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
    ),
    // 拦截Executor的update方法（增删改操作）
    @Signature(
        type = Executor.class,
        method = "update",
        args = {MappedStatement.class, Object.class}
    )
    })
    public class SqlExecutionTimePlugin implements Interceptor {
    // 从配置文件中读取的耗时阈值（单位：ms），用于标记慢SQL
    private Long slowSqlThreshold;
    /**
     * 核心拦截逻辑：统计SQL执行时间
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 获取SQL相关信息
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        String mapperMethod = mappedStatement.getId(); // Mapper接口方法全路径（如：com.example.mapper.UserMapper.selectById）
        SqlCommandType sqlType = mappedStatement.getSqlCommandType(); // SQL类型（SELECT/INSERT/UPDATE/DELETE）
        // 2. 记录SQL执行开始时间
        long startTime = System.currentTimeMillis();
        try {
            // 3. 执行原始SQL方法（放行，继续执行后续逻辑）
            return invocation.proceed();
        } finally {
            // 4. 计算执行耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            // 5. 打印日志（区分普通SQL和慢SQL）
            if (duration >= slowSqlThreshold) {
                System.out.printf("[慢SQL警告] 类型：%s | 方法：%s | 执行耗时：%d ms%n", sqlType, mapperMethod, duration);
            } else {
                System.out.printf("[SQL执行日志] 类型：%s | 方法：%s | 执行耗时：%d ms%n", sqlType, mapperMethod, duration);
            }
        }
    }
    /**
     * 生成代理对象（使用默认实现，无需修改）
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    /**
     * 读取插件配置属性（从MyBatis配置文件中获取）
     */
    @Override
    public void setProperties(Properties properties) {
        // 读取配置的慢SQL阈值，默认值为500ms
        this.slowSqlThreshold = Long.parseLong(properties.getProperty("slowSqlThreshold", "500"));
        System.out.println("SQL执行时间监控插件初始化：慢SQL阈值=" + slowSqlThreshold + "ms");
    }
    }
    3.3 步骤3：注册插件到MyBatis
    插件编写完成后，需注册到MyBatis中才能生效，支持两种注册方式，根据项目类型选择：
    方式1：XML配置（传统MyBatis项目）
    在mybatis-config.xml的<plugins>标签中配置插件，可同时配置插件属性：
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-config.dtd">
    <configuration>
    <!-- 插件配置 -->
    <plugins>
        <plugin interceptor="com.example.plugin.SqlExecutionTimePlugin">
    <!-- 配置插件属性：慢SQL阈值（单位：ms） -->
           <property name="slowSqlThreshold" value="300"/>
        </plugin>
        <!-- 可配置多个插件，按顺序形成插件链 -->
       <!-- <plugin interceptor="com.example.plugin.AnotherPlugin"/> -->
    </plugins>
    <!-- 其他配置（环境、mapper扫描等） -->
    <environments default="development">
        <!-- 环境配置... -->
    </environments>
    <mappers>
        <mapper resource="mapper/UserMapper.xml"/>
    </mappers>
    </configuration>
    方式2：Java配置（Spring Boot项目）
    通过配置类注入SqlSessionFactory，将插件添加到MyBatis中：
    import org.mybatis.spring.SqlSessionFactoryBean;
    import org.mybatis.spring.annotation.MapperScan;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
    import javax.sql.DataSource;
    import java.io.IOException;
    @Configuration
    @MapperScan("com.example.mapper") // 扫描Mapper接口
    public class MyBatisConfig {
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws IOException {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        // 配置Mapper.xml路径（可选，若Mapper接口与XML同路径可省略）
        factoryBean.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/*.xml")
        );
        // 注册自定义插件
        factoryBean.setPlugins(new SqlExecutionTimePlugin());
        try {
            return factoryBean.getObject();
        } catch (Exception e) {
            throw new RuntimeException("初始化SqlSessionFactory失败", e);
        }
    }
    }
    3.4 步骤4：测试插件效果
    编写Mapper接口和测试代码，调用SQL方法，查看控制台日志是否打印SQL执行时间：
    // 1. Mapper接口
    public interface UserMapper {
    @Select("select * from user where id = #{id}")
    User selectById(Long id);
    }
    // 2. 测试代码
    public class MyBatisPluginTest {
    public static void main(String[] args) throws IOException {
        // 读取MyBatis配置文件
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        // 获取SqlSession，调用Mapper方法
        try (SqlSession session = sqlSessionFactory.openSession()) {
            UserMapper userMapper = session.getMapper(UserMapper.class);
            User user = userMapper.selectById(1L);
            System.out.println("查询结果：" + user);
        }
    }
    }
    控制台输出示例：
    SQL执行时间监控插件初始化：慢SQL阈值=300ms
    [SQL执行日志] 类型：SELECT | 方法：com.example.mapper.UserMapper.selectById | 执行耗时：56 ms
    查询结果：User(id=1, name="张三", age=25)
    四、插件开发关键注意事项
    拦截签名必须精准匹配：@Signature注解的args参数必须与目标方法的参数类型、顺序完全一致（如Executor的query方法有多个重载，需明确匹配参数列表），否则插件无法拦截目标方法。
    必须调用invocation.proceed()：在intercept方法中，若未调用invocation.proceed()，会阻断原方法执行（如SQL无法执行、事务无法提交），除非有明确的阻断需求。
    避免过度拦截：尽量只拦截必要的组件和方法，避免拦截所有四大组件或核心方法，否则会影响MyBatis执行性能。
    多插件执行顺序：多个插件按配置顺序形成插件链，最外层插件先执行前置逻辑，最后执行后置逻辑；内层插件后执行前置逻辑，先执行后置逻辑，配置时需注意顺序合理性。
    线程安全问题：插件实例是单例的，若插件中存在成员变量（如示例中的slowSqlThreshold），需确保其线程安全，避免多线程环境下出现数据异常。
    避免修改核心对象：不要随意修改Invocation中的目标对象、方法或参数，若需修改SQL，建议通过MetaObject操作BoundSql，避免直接修改核心对象导致MyBatis运行异常。
    五、常见插件场景实战扩展
    5.1 场景1：数据脱敏插件（拦截ResultSetHandler）
    拦截ResultSetHandler的handleResultSets方法，对返回结果中的敏感数据（如手机号、身份证号）进行脱敏处理，示例核心逻辑：
    @Intercepts({
    @Signature(
        type = ResultSetHandler.class,
        method = "handleResultSets",
        args = {Statement.class}
    )
    })
    public class DataDesensitizationPlugin implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 执行原方法，获取返回结果
        Object result = invocation.proceed();
        // 对结果进行脱敏处理（如手机号脱敏：138****1234）
        if (result instanceof List) {
            List<?> list = (List<?>) result;
            for (Object obj : list) {
                desensitize(obj); // 自定义脱敏方法
            }
        }
        return result;
    }
    // 自定义脱敏逻辑
    private void desensitize(Object obj) {
        // 利用反射获取对象字段，对敏感字段进行脱敏
        // 示例：手机号脱敏
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals("phone")) {
                field.setAccessible(true);
                try {
                    String phone = (String) field.get(obj);
                    if (phone != null && phone.length() == 11) {
                        field.set(obj, phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // 其他方法实现...
    }
    5.2 场景2：SQL分页插件（拦截StatementHandler）
    拦截StatementHandler的prepare方法，通过修改SQL语句实现物理分页（如MySQL的LIMIT、Oracle的ROWNUM），核心逻辑是获取原始SQL，拼接分页语句，再设置回BoundSql中。
    六、总结
    MyBatis插件开发的核心是「拦截器接口+注解声明+动态代理」，只需遵循“实现Interceptor接口→声明拦截目标→注册插件”的三步流程，即可实现对SQL执行全流程的增强。开发时需注意拦截签名的精准性、多插件顺序、线程安全等问题，避免影响MyBatis的正常运行。
    常见的插件场景包括性能监控、数据脱敏、分页处理、参数加密等，合理使用插件可大幅提升MyBatis的灵活性和可扩展性，减少重复代码开发。实际开发中，也可参考PageHelper、MyBatis-Plus等成熟插件的实现思路，优化自定义插件的性能和稳定性。
