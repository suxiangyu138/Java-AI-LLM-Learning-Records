# Java后端开发学习路线与核心知识点

## 一、Java基础语法
基本数据类型：byte(1字节)、short(2字节)、int(4字节)、long(8字节)、float(4字节)、double(8字节)、char(2字节)、boolean(1位) 
变量与常量：变量声明规则、final关键字、作用域（局部/成员/静态） 
运算符：算术运算符(+ - * / % ++ --)、关系运算符(> < == != >= <=)、逻辑运算符(&& || ! ^)、位运算符(& | ^ ~ << >> >>>)、赋值运算符(= += -= *= /= %=) 
流程控制：if-else if-else、switch-case(支持String/enum)、for循环(普通/增强for)、while/dowhile循环、break(跳出当前循环)/continue(跳过本次循环) 
数组：一维数组/二维数组定义、初始化(静态/动态)、遍历(普通for/增强for)、Arrays工具类(排序/查找/填充) 

## 二、面向对象编程(OOP)
类与对象：类的定义(属性+方法)、对象的创建(new关键字)、构造方法(无参/有参、重载、this调用本类构造) 
封装：private修饰符、getter/setter方法、包(package)与访问权限(public/protected/default/private) 
继承：extends关键字、单继承特性、super调用父类构造/方法/属性、方法重写(@Override、两同两小一大原则) 
多态：向上转型(子类转父类)、向下转型(强制类型转换+instanceof判断)、编译时多态(重载)、运行时多态(重写) 
抽象类：abstract修饰、可包含抽象方法(无实现)和非抽象方法、不能实例化、子类必须实现所有抽象方法 
接口：interface定义、default方法(带实现)、static方法(静态)、多实现特性、JDK8前后版本差异(JDK8新增默认方法和静态方法) 
内部类：成员内部类、局部内部类、匿名内部类(常用于回调)、静态内部类(static修饰) 

## 三、异常处理机制
Throwable体系：Error(系统错误，不可控)、Exception(可处理异常) 
Checked Exception：编译器强制处理的异常(IOException、SQLException)，需用try-catch捕获或throws声明抛出 
Unchecked Exception(RuntimeException)：运行时异常(NullPointerException、ArrayIndexOutOfBoundsException、ClassCastException、IllegalArgumentException)，可不处理但建议捕获 
try-catch-finally结构：finally块始终执行(除非System.exit())、多重catch捕获顺序(子类在前父类在后)、try-with-resources(JDK7+)自动关闭资源(实现AutoCloseable接口的类) 

## 四、集合框架
Collection接口：单列集合根接口，子接口List、Set、Queue 
List接口：有序可重复集合，实现类ArrayList(动态数组)、LinkedList(双向链表)、Vector(线程安全，低效)、CopyOnWriteArrayList(并发容器) 
Set接口：无序不可重复集合，实现类HashSet(哈希表)、LinkedHashSet(哈希表+链表，有序)、TreeSet(红黑树，排序) 
Map接口：双列键值对集合，实现类HashMap(哈希表，JDK7数组+链表/JDK8数组+链表+红黑树)、LinkedHashMap(维护插入/访问顺序)、TreeMap(红黑树，按键排序)、Hashtable(线程安全，低效)、ConcurrentHashMap(高效并发，JDK7分段锁/JDK8CAS+Synchronized) 
集合工具类：Collections(排序sort、二分查找binarySearch、反转reverse、同步包装synchronizedXXX) 
迭代器：Iterator(遍历集合，hasNext()/next()/remove())、ListIterator(支持双向遍历和增删改) 

## 五、IO流
流分类：按方向(输入流InputStream/Reader、输出流OutputStream/Writer)、按数据单位(字节流Byte、字符流Char)、按功能(节点流、处理流) 
字节流：FileInputStream/FileOutputStream(文件操作)、BufferedInputStream/BufferedOutputStream(缓冲流，提高性能)、ObjectInputStream/ObjectOutputStream(对象序列化/反序列化，需实现Serializable接口) 
字符流：FileReader/FileWriter(文件操作)、BufferedReader/BufferedWriter(缓冲流，支持readLine()读取一行)、InputStreamReader/OutputStreamWriter(字节流转字符流，指定编码格式) 
NIO(New IO)：Channel(通道)、Buffer(缓冲区)、Selector(选择器，多路复用)、Path接口与Files工具类(替代File类的现代API) 

## 六、多线程编程
线程创建方式：继承Thread类(run方法)、实现Runnable接口(推荐，避免单继承局限)、实现Callable接口(支持返回值，配合FutureTask)、线程池创建(FixedThreadPool/CachedThreadPool/ScheduledThreadPool) 
线程生命周期：新建(New)、就绪(Runnable)、运行(Running)、阻塞(Blocked)、等待(Waiting)、超时等待(Timed Waiting)、终止(Terminated) 
线程同步：synchronized关键字(修饰方法/代码块)、Lock接口(ReentrantLock实现类，支持公平锁/非公平锁、tryLock尝试获取锁)、volatile关键字(保证可见性和禁止指令重排，不保证原子性) 
线程通信：wait()/notify()/notifyAll()方法(必须在synchronized块中调用)、Condition接口(配合Lock使用，await()/signal()/signalAll()) 
并发工具类：CountDownLatch(计数器，等待多个线程完成)、CyclicBarrier(循环屏障，等待所有线程到达某一点)、Semaphore(信号量，控制并发线程数)、Exchanger(交换两个线程的数据) 

## 七、网络编程
TCP协议：面向连接、可靠传输，Socket类(客户端)和ServerSocket类(服务端)实现，三次握手建立连接，四次挥手断开连接 
UDP协议：无连接、不可靠但高效，DatagramSocket类(发送/接收端)和DatagramPacket类(数据包)实现 
URL处理：URL类解析网络资源，HttpURLConnection类发送HTTP请求(GET/POST) 
NIO网络编程：ServerSocketChannel(服务端通道)、SocketChannel(客户端通道)、Selector(多路复用器，监听多个通道事件) 

## 八、反射与注解
反射机制：Class类(获取类信息：getName()/getMethods()/getFields()等)、Constructor类(创建对象：newInstance())、Method类(调用方法：invoke())、Field类(访问属性：get()/set())，应用场景：框架设计、动态代理 
注解(Annotation)：内置注解(@Override、@Deprecated、@SuppressWarnings)、元注解(@Target指定作用位置、@Retention指定保留阶段SOURCE/CLASS/RUNTIME)、自定义注解(定义@interface)、APT(注解处理器，编译期生成代码) 

## 九、泛型编程
泛型概念：参数化类型，编译期类型安全检查，消除强制类型转换 
泛型使用场景：泛型类(类名<T>)、泛型方法(<T>修饰方法，独立于类泛型)、泛型接口(接口名<T>) 
通配符：? extends T(上限通配符，生产者)、? super T(下限通配符，消费者)、无界通配符<?> 
类型擦除：编译后泛型信息被擦除为原始类型，运行时无法获取泛型实际类型 

## 十、JVM基础
JVM内存结构：程序计数器(记录当前线程执行位置)、虚拟机栈(存储方法栈帧，局部变量表/操作数栈等)、本地方法栈(类似虚拟机栈，为Native方法服务)、堆(对象实例分配区域，GC主要区域)、方法区(类信息/常量/静态变量，JDK8后为元空间Metaspace) 
垃圾收集：判断对象存活算法(引用计数法、可达性分析)、GC算法(标记-清除、标记-整理、复制算法、分代收集)、常见垃圾收集器(Serial、Parallel Scavenge、CMS、G1、ZGC) 
类加载机制：类加载过程(加载、验证、准备、解析、初始化)、类加载器(启动类加载器、扩展类加载器、应用程序类加载器、自定义类加载器，双亲委派模型) 
JIT编译：即时编译器将热点代码编译为机器码，提高执行效率 

## 十一、数据库基础
SQL语言：DDL(数据定义语言：create/drop/alter table)、DML(数据操纵语言：insert/update/delete)、DQL(数据查询语言：select，含where/group by/having/order by/limit/join等子句)、DCL(数据控制语言：grant/revoke) 
MySQL基础：安装配置、存储引擎(InnoDB支持事务/行级锁/外键，MyISAM不支持事务/表级锁/全文索引)、数据类型(int/varchar/date/datetime/blob/text等)、约束(primary key/foreign key/not null/unique/check) 
数据库设计：三范式(1NF原子性、2NF完全依赖、3NF消除传递依赖)、反范式设计(适当冗余提高查询效率) 
事务特性(ACID)：原子性(Atomicity)、一致性(Consistency)、隔离性(Isolation)、持久性(Durability) 
事务隔离级别：读未提交(Read Uncommitted)、读已提交(Read Committed)、可重复读(Repeatable Read，MySQL默认)、串行化(Serializable) 
锁机制：行级锁(InnoDB支持)、表级锁(MyISAM支持)、共享锁(S锁，读锁)、排他锁(X锁，写锁)、乐观锁(版本号/时间戳控制)、悲观锁(显式加锁) 
索引：B+树索引(聚簇索引/非聚簇索引)、哈希索引、全文索引，索引优化原则(最左前缀匹配、避免索引失效情况) 

## 十二、JDBC编程
JDBC核心组件：DriverManager(管理驱动)、Connection(数据库连接)、Statement(执行SQL，有SQL注入风险)、PreparedStatement(预编译SQL，防止注入)、CallableStatement(调用存储过程)、ResultSet(结果集) 
JDBC操作步骤：加载驱动(Class.forName("com.mysql.cj.jdbc.Driver"))、获取连接(DriverManager.getConnection(url, username, password))、创建Statement、执行SQL(executeQuery查询/excuteUpdate增删改)、处理结果集、关闭资源(try-with-resources自动关闭) 
数据库连接池：Druid(阿里开源，监控强大)、HikariCP(高性能)、C3P0、DBCP，配置参数(initialSize/maxActive/minIdle/maxWait等) 

## 十三、Spring框架核心
Spring IOC(控制反转)：核心容器(ApplicationContext接口及其实现类ClassPathXmlApplicationContext/FileSystemXmlApplicationContext/AnnotationConfigApplicationContext)、Bean的生命周期(实例化→属性赋值→初始化→销毁)、依赖注入(DI，构造器注入/setter注入/字段注入@Autowired)、Bean的作用域(singleton/prototype/request/session/application)、Bean装配方式(XML配置、注解@Component/@Service/@Controller/@Repository、JavaConfig@Configuration/@Bean) 
Spring AOP(面向切面编程)：核心概念(切面Aspect、通知Advice、切入点Pointcut、连接点JoinPoint)、通知类型(前置通知@Before、后置通知@AfterReturning、环绕通知@Around、异常通知@AfterThrowing、最终通知@After)、实现方式(JDK动态代理基于接口、CGLIB动态代理基于类)、应用场景(日志记录、事务管理、权限控制、性能监控) 

## 十四、Spring MVC框架
MVC架构模式：Model(模型层，业务逻辑和数据)、View(视图层，页面展示)、Controller(控制器层，接收请求分发处理) 
核心组件：DispatcherServlet(前端控制器，统一入口)、HandlerMapping(映射处理器，根据URL找到对应的Handler)、HandlerAdapter(适配器，执行Handler)、ViewResolver(视图解析器，解析视图名称) 
请求处理流程：客户端请求→DispatcherServlet→HandlerMapping→HandlerExecutionChain→HandlerAdapter→Handler(Controller方法)→返回ModelAndView→ViewResolver→渲染视图→响应客户端 
常用注解：@Controller(标识控制器)、@RequestMapping(映射URL路径)、@GetMapping/@PostMapping/@PutMapping/@DeleteMapping(细化HTTP方法映射)、@RequestParam(获取请求参数)、@PathVariable(获取路径变量)、@RequestBody(接收JSON请求体)、@ResponseBody(返回JSON响应)、@RestController(组合@Controller和@ResponseBody) 

## 十五、Spring Boot框架
核心特性：自动配置(AutoConfiguration，基于条件注解@Conditional系列)、起步依赖(Starter Dependencies，简化依赖管理)、嵌入式服务器(Tomcat/Jetty/Undertow，无需部署WAR包)、Actuator(应用监控端点) 
配置文件：application.properties/application.yml(yaml语法缩进表示层级)、多环境配置(profiles：application-dev.yml/application-test.yml/application-prod.yml)、配置绑定(@ConfigurationProperties(prefix="")绑定配置文件前缀属性到Bean) 
Starter组件：spring-boot-starter-web(Web开发)、spring-boot-starter-data-jpa(JPA数据访问)、spring-boot-starter-mybatis(MyBatis集成)、spring-boot-starter-redis(Redis缓存)、spring-boot-starter-security(安全认证) 
热部署：devtools依赖实现开发时热更新 

## 十六、MyBatis框架
ORM思想：对象关系映射，将Java对象与数据库表记录相互转换 
MyBatis核心组件：SqlSessionFactory(会话工厂，构建SqlSession)、SqlSession(会话，执行SQL和事务管理)、Mapper接口(定义数据访问方法)、Mapper XML文件(编写SQL语句，namespace对应Mapper接口全限定名) 
核心配置：mybatis-config.xml(全局配置，数据源/事务管理器/别名/插件等)、mapper.xml(映射文件，<select>/<insert>/<update>/<delete>标签，parameterType/resultType/resultMap) 
动态SQL：<if>条件判断、<choose><when><otherwise>分支选择、<foreach>循环遍历(用于in查询)、<where>智能处理where子句、<set>智能处理set子句 
高级特性：延迟加载(association/collection标签设置fetchType="lazy")、一级缓存(SqlSession级别，默认开启)、二级缓存(Mapper级别，需手动开启cache标签)、插件开发(拦截器Interceptor，实现分页/性能监控等) 

## 十七、Redis数据库
Redis特点：基于内存的Key-Value数据库，支持多种数据结构，持久化机制，高可用集群 
数据结构：String(字符串，set/get/incr/decr)、Hash(哈希，hset/hget/hgetAll)、List(列表，lpush/rpop/lrange)、Set(集合，sadd/smembers/sinter/sunion)、Sorted Set(有序集合，zadd/zrangebyscore/zrank) 
持久化机制：RDB(快照，定期生成数据文件，恢复快但可能丢失数据)、AOF(日志追加，记录所有写操作，数据安全性高但文件大) 
主从复制：主节点(master)负责写，从节点(slave)负责读，实现读写分离和数据备份 
哨兵模式(Sentinel)：监控主从节点健康状态，自动故障转移(主节点宕机后选举新主节点) 
集群模式(Cluster)：分片存储数据(16384个槽位)，高可用，支持水平扩展 
应用场景：缓存(减轻数据库压力)、分布式锁(SET NX EX命令)、计数器(incr实现)、消息队列(lpush+brpop实现简单队列) 

## 十八、Maven项目管理
Maven核心概念：POM(项目对象模型，pom.xml文件)、坐标(groupId/artifactId/version唯一标识一个构件)、依赖管理(dependencies/dependency，scope控制依赖范围compile/test/provided/runtime/system)、仓库(本地仓库/中央仓库/私服Nexus) 
生命周期：clean(清理项目)、default(构建项目，含validate/compile/test/package/install/deploy等阶段)、site(生成项目站点文档) 
常用命令：mvn clean compile(清理并编译)、mvn test(运行测试)、mvn package(打包)、mvn install(安装到本地仓库)、mvn deploy(部署到私服) 
依赖冲突解决：最短路径优先、声明优先、排除依赖(exclusions标签) 

## 十九、Git版本控制
Git基本概念：工作区(Working Directory)、暂存区(Stage/Index)、版本库(Repository，.git目录)、远程仓库(Remote Repository，如GitHub/GitLab) 
基本操作：git init(初始化仓库)、git add(添加文件到暂存区)、git commit(提交暂存区到本地仓库，-m "提交信息")、git status(查看状态)、git log(查看提交历史)、git reflog(查看所有操作记录) 
分支管理：git branch(查看分支)、git checkout -b dev(创建并切换到dev分支)、git merge dev(合并dev分支到当前分支)、git branch -d dev(删除dev分支)、git stash(暂存工作区修改，git stash pop恢复) 
远程协作：git clone(克隆远程仓库)、git remote add origin url、git push origin master(推送本地分支到远程)、git pull origin master(拉取远程分支并合并)、git fetch origin master(拉取远程分支但不合并) 

## 二十、Linux操作系统基础
Linux常用命令：文件操作(ls/cd/pwd/touch/mkdir/rm/cp/mv/cat/less/tail/head)、目录操作(cd /root进入根目录，cd ..返回上级目录)、权限管理(chmod 755 file修改权限，chown user:group file修改所有者)、进程管理(ps aux查看进程，kill -9 pid杀死进程，top实时查看系统资源)、网络管理(ifconfig查看IP，ping测试连通性，netstat查看端口占用)、压缩解压(tar -zxvf file.tar.gz解压，tar -zcvf file.tar.gz dir压缩) 
Shell脚本：#!/bin/bash指定解释器，变量定义(name="value"，引用name或{name})，条件判断(if [ a -eq b ]; then ... fi)，循环(for i in 1 2 3; do ... done)，函数定义(function name() { ... })，执行方式(bash script.sh或./script.sh需加执行权限) 
系统服务管理：systemctl start/stop/restart/status service_name(如systemctl start tomcat) 

## 二十一、RESTful API设计
REST核心思想：资源为中心，HTTP方法语义化(GET获取资源、POST创建资源、PUT更新资源、DELETE删除资源、PATCH部分更新资源) 
API设计规范：URL命名使用名词复数(/users、/users/{id})，避免动词；状态码规范(200成功、201创建成功、400请求错误、401未授权、403禁止访问、404资源不存在、500服务器错误)；请求/响应格式统一使用JSON；版本控制(URL路径/v1/users或请求头Accept: application/vnd.company.v1+json) 
HATEOAS：超媒体作为应用状态的引擎，响应中包含相关资源的链接，使API自描述 

## 二十二、单元测试
JUnit5核心组件：@Test(测试方法)、@BeforeEach(每个测试前执行)、@AfterEach(每个测试后执行)、@BeforeAll(所有测试前执行，静态方法)、@AfterAll(所有测试后执行，静态方法)、@DisplayName(测试显示名称)、断言(Assertions.assertEquals/assertTrue/assertNotNull等) 
Mock测试：Mockito框架(@Mock模拟依赖对象、@InjectMocks注入依赖到被测试对象、when(mock.method()).thenReturn(result)设置模拟行为、verify(mock).method()验证方法调用次数) 
测试覆盖率：Jacoco插件生成覆盖率报告，目标覆盖率一般要求70%以上 

## 二十三、设计模式(23种经典设计模式)
创建型模式：单例模式(饿汉式/懒汉式/双重检查锁/静态内部类/枚举，确保一个类只有一个实例)、工厂方法模式(定义一个创建对象的接口，由子类决定实例化哪个类)、抽象工厂模式(创建一系列相关或依赖对象的家族，而无需指定具体类)、建造者模式(将复杂对象的构建与表示分离，允许按步骤创建对象)、原型模式(通过复制现有实例创建新实例，实现Cloneable接口) 
结构型模式：适配器模式(将一个类的接口转换成客户期望的另一个接口)、装饰器模式(动态地给对象添加额外职责，不改变其结构)、代理模式(为其他对象提供一种代理以控制对这个对象的访问，静态代理/动态代理/JDK代理/CGLIB代理)、外观模式(为子系统中的一组接口提供一个一致的界面)、桥接模式(将抽象部分与实现部分分离，使它们都可以独立变化)、组合模式(将对象组合成树形结构以表示"部分-整体"的层次结构)、享元模式(运用共享技术有效地支持大量细粒度的对象) 
行为型模式：观察者模式(定义对象间一对多的依赖关系，当一个对象改变状态时，所有依赖它的对象都会收到通知)、策略模式(定义一系列算法，把它们一个个封装起来，并使它们可互相替换)、模板方法模式(定义一个操作中的算法的骨架，而将一些步骤延迟到子类中)、迭代器模式(提供一种方法顺序访问一个聚合对象中的各个元素，而又不暴露其内部表示)、责任链模式(使多个对象都有机会处理请求，从而避免请求的发送者和接收者耦合在一起)、命令模式(将一个请求封装为一个对象，从而使你可用不同的请求对客户进行参数化)、备忘录模式(在不破坏封装性的前提下，捕获一个对象的内部状态，并在该对象之外保存这个状态)、状态模式(允许对象在其内部状态改变时改变它的行为)、访问者模式(表示一个作用于某对象结构中的各元素的操作，它使你可以在不改变各元素的类的前提下定义作用于这些元素的新操作)、中介者模式(用一个中介对象来封装一系列的对象交互)、解释器模式(给定一个语言，定义它的文法的一种表示，并定义一个解释器，这个解释器使用该表示来解释语言中的句子) 

## 二十四、微服务架构基础
微服务概念：将单一应用程序划分成一组小的服务，每个服务运行在自己的进程中，服务间采用轻量级通信机制(通常是HTTP RESTful API)，独立部署、独立扩展、独立维护 
微服务优势：松耦合、独立部署、技术异构、弹性伸缩、容错能力强 
微服务挑战：分布式复杂性、数据一致性、服务治理、运维复杂度高 
常见微服务框架：Spring Cloud(Netflix OSS/Eureka/Ribbon/Hystrix/Feign/Zuul/Gateway)、Dubbo(阿里巴巴开源RPC框架)、gRPC(Google开源高性能RPC框架) 

## 二十五、Spring Cloud核心组件
服务注册与发现：Eureka(AP特性，自我保护机制)、Consul(CP特性，支持多数据中心)、Nacos(阿里开源，同时支持CP和AP，可作为配置中心) 
负载均衡：Ribbon(客户端负载均衡，轮询/随机/权重等策略)、Spring Cloud LoadBalancer(Ribbon替代品) 
服务调用：OpenFeign(声明式REST客户端，整合Ribbon和Hystrix)、RestTemplate(同步HTTP客户端) 
熔断器：Hystrix(Netflix开源，熔断机制、降级处理、舱壁模式)、Resilience4j(轻量级熔断器，支持限流、重试等) 
API网关：Zuul(第一代网关，同步阻塞)、Gateway(第二代网关，异步非阻塞，基于WebFlux) 
配置中心：Config Server(集中管理配置文件，支持Git/SVN存储)、Nacos Config(动态配置更新) 
链路追踪：Sleuth(生成追踪ID)+Zipkin(收集和展示追踪数据)、SkyWalking(国产APM工具，功能更全面) 

## 二十六、Docker容器技术
Docker核心概念：镜像(Image，只读模板，包含运行环境和应用)、容器(Container，镜像的运行实例，可创建/启动/停止/删除)、仓库(Repository，存储镜像，如Docker Hub/私有仓库) 
常用命令：docker images(查看本地镜像)、docker ps(查看运行中的容器)、docker run -d -p 8080:8080 --name myapp image_name(后台运行容器并映射端口)、docker stop/start/restart container_id(停止/启动/重启容器)、docker rm container_id(删除容器)、docker rmi image_id(删除镜像)、docker exec -it container_id /bin/bash(进入容器内部) 
Dockerfile：构建镜像的脚本文件，常用指令FROM(基础镜像)、MAINTAINER(作者)、RUN(执行命令)、COPY(复制文件)、ADD(复制并解压)、ENV(环境变量)、EXPOSE(暴露端口)、CMD(容器启动时执行的命令)、ENTRYPOINT(入口点命令) 
Docker Compose：编排多容器应用，docker-compose.yml定义服务、网络、卷，命令docker-compose up -d启动所有服务 

## 二十七、消息队列
消息队列作用：异步处理(提高系统响应速度)、应用解耦(降低系统间依赖)、流量削峰(应对突发流量)、日志处理、消息通讯 
RabbitMQ：基于AMQP协议的消息中间件，交换机类型(direct/fanout/topic/headers)、队列持久化、消息确认机制(生产者确认/消费者ACK)、死信队列 
Kafka：高吞吐量的分布式发布订阅消息系统，主题(Topic)、分区(Partition)、副本(Replica)、消费者组(Consumer Group)、ISR(In-Sync Replicas)、零拷贝技术、适用场景(日志采集、大数据流处理) 
RocketMQ：阿里开源的消息中间件，支持事务消息、顺序消息、延迟消息、批量消息，可靠性高 

## 二十八、搜索引擎Elasticsearch
Elasticsearch特点：分布式、RESTful风格的搜索和分析引擎，基于Lucene，支持全文检索、结构化搜索、分析 
核心概念：索引(Index，相当于数据库)、文档(Document，相当于表中的一条记录)、类型(Type，ES 7.x后移除)、映射(Mapping，定义字段类型和分词器)、分片(Shard，索引拆分后的片段)、副本(Replica，分片的拷贝) 
常用API：索引操作(PUT /index、GET /index/mapping)、文档操作(PUT /index/doc/id、GET /index/doc/id、DELETE /index/doc/id)、搜索API(GET /index/_search，DSL查询语法match/match_phrase/term/range/bool/filter等) 
中文分词：IK分词器(ik_max_word/ik_smart两种模式) 
倒排索引：将文档中的词映射到包含该词的文档，提高搜索效率 

## 二十九、安全框架
Shiro：Apache开源安全框架，核心组件Subject(当前用户)、SecurityManager(安全管理器)、Realm(数据源，认证授权)、Authentication(认证)、Authorization(授权)、Cryptography(加密) 
Spring Security：Spring生态的安全框架，基于过滤器链实现，支持认证(Authentication Manager)、授权(Authorization Manager)、防护攻击(CSRF/XSS)、OAuth2/OpenID Connect单点登录 
JWT(JSON Web Token)：无状态的身份验证令牌，结构(header.payload.signature)，适用于前后端分离架构 

## 三十、性能优化
JVM调优：调整堆内存大小(-Xms初始堆/-Xmx最大堆)、新生代比例(-XX:NewRatio)、Survivor区比例(-XX:SurvivorRatio)、选择合适的垃圾收集器(-XX:+UseG1GC)、分析GC日志(-XX:+PrintGCDetails) 
SQL优化：添加合适的索引、避免SELECT *、优化WHERE子句(避免在索引列上使用函数)、减少子查询、使用JOIN代替子查询、分页查询优化(LIMIT offset过大时使用主键过滤) 
缓存优化：合理设置缓存过期时间、缓存穿透(布隆过滤器)、缓存击穿(互斥锁)、缓存雪崩(过期时间随机分散)、多级缓存(本地缓存Caffeine+分布式缓存Redis) 
并发优化：减少锁竞争(减小锁粒度、使用读写锁、无锁数据结构)、线程池合理配置(核心线程数、最大线程数、队列容量)、避免死锁(按顺序获取锁、设置超时时间) 
应用性能分析：使用Profiler工具(Async Profiler/Arthas)定位性能瓶颈，分析CPU/内存/IO消耗 

## 三十一、日志系统
SLF4J：日志门面，统一日志API，适配不同日志实现 
Logback：SLF4J的原生实现，性能优于Log4j，支持自动重新加载配置、过滤器、归档策略 
Log4j2：Apache开源，性能优秀，支持异步日志、插件化架构、配置热更新 
日志规范：使用正确的日志级别(ERROR/WARN/INFO/DEBUG/TRACE)、日志内容包含上下文信息(用户ID/请求ID/时间戳)、避免敏感信息泄露(密码/身份证号等) 
ELK Stack：Elasticsearch(存储日志)+Logstash(收集解析日志)+Kibana(可视化展示日志) 

## 三十二、持续集成/持续部署(CI/CD)
CI/CD概念：持续集成(频繁合并代码到主干并自动化测试)、持续交付(代码随时可部署到生产环境)、持续部署(自动部署到生产环境) 
Jenkins：开源CI/CD工具，流水线(Pipeline)配置，插件丰富，支持构建、测试、部署全流程 
GitHub Actions：GitHub提供的CI/CD服务，通过yaml文件定义工作流(workflow) 
GitLab CI/CD：GitLab内置的CI/CD功能，.gitlab-ci.yml配置文件 
Docker镜像构建与推送：在CI流程中自动构建Docker镜像并推送到镜像仓库 

## 三十三、常用工具与插件
IDE：IntelliJ IDEA(主流Java IDE，社区版免费/旗舰版付费)、Eclipse(老牌IDE，插件丰富) 
数据库连接工具：Navicat、DataGrip(JetBrains出品)、DBeaver(开源通用数据库工具) 
Postman/Insomnia：API接口测试工具，支持HTTP/HTTPS请求调试、环境变量管理 
Swagger/OpenAPI：API文档自动生成工具，在线调试接口，注解方式(@ApiOperation/@ApiParam等) 
Lombok：通过注解减少样板代码(@Data/@Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor等) 
MapStruct：对象映射工具，编译期生成映射代码，性能优于BeanUtils 

## 三十四、面试高频考点
Java基础：HashMap底层实现原理(JDK7与JDK8区别)、ConcurrentHashMap如何实现线程安全、ArrayList与LinkedList区别、String/StringBuffer/StringBuilder区别及底层实现、==与equals的区别、hashCode与equals的关系、Java内存模型(JMM)、volatile关键字作用、synchronized锁升级过程(偏向锁→轻量级锁→重量级锁) 
多线程：线程池参数含义及工作原理、线程安全的集合类有哪些、CountDownLatch/CyclicBarrier/Semaphore区别、ThreadLocal原理及内存泄漏问题、synchronized与Lock的区别、CAS原理及ABA问题 
JVM：类加载过程、双亲委派模型及打破双亲委派的方式、垃圾收集算法及垃圾收集器对比、JVM调优参数、OOM异常类型及排查思路 
Spring：IOC容器初始化过程、Bean的生命周期、AOP实现原理、事务传播机制及隔离级别、Spring如何解决循环依赖(三级缓存) 
Spring MVC：请求处理流程、DispatcherServlet作用、常用注解含义及使用场景 
Spring Boot：自动配置原理(@EnableAutoConfiguration/@Import/@Conditional)、Starter工作原理、配置文件优先级、Actuator端点作用 
MyBatis：#{}与${}的区别、动态SQL标签使用场景、延迟加载原理、一级缓存与二级缓存区别、插件原理(拦截器) 
MySQL：索引类型及数据结构(B+树)、索引失效场景、事务隔离级别及解决的问题、MVCC原理、锁机制(行锁/表锁/间隙锁)、慢查询优化方法 
Redis：数据类型及应用场景、持久化机制区别、缓存穿透/击穿/雪崩解决方案、分布式锁实现、Redis集群方案对比(主从/哨兵/Cluster) 
微服务：CAP理论、服务注册发现原理、熔断降级区别、网关作用及常见网关对比(Zuul/Gateway)、分布式事务解决方案(2PC/TCC/SAGA/本地消息表/最大努力通知) 

## 三十五、学习资源推荐
书籍：《Java核心技术卷I》《深入理解Java虚拟机》(周志明)、《Effective Java》(Joshua Bloch)、《Head First设计模式》、《Spring实战》(第四版)、《MyBatis从入门到精通》、《MySQL必知必会》、《Redis设计与实现》 
视频课程：尚硅谷Java全套教程、黑马程序员JavaEE就业班、动力节点Spring全家桶、尚硅谷SpringBoot+MyBatis+SpringCloud全套教程 
官方文档：Oracle Java Documentation、Spring Framework Reference Documentation、MyBatis官方文档、MySQL官方文档、Redis官方文档 
技术社区：掘金、思否(SegmentFault)、CSDN、博客园、GitHub、Stack Overflow 
实践项目：电商系统(含用户/商品/订单/支付模块)、博客系统(含文章/评论/分类模块)、秒杀系统(高并发场景练习)、外卖管理系统(多角色权限控制) 

## 三十六、学习计划建议
第一阶段(1-2个月)：夯实Java基础(OOP、集合、IO、多线程)、熟悉MySQL基本操作和JDBC编程、掌握Maven/Git/Linux基础命令 
第二阶段(2-3个月)：深入学习Spring框架(IOC/AOP)、Spring MVC、Spring Boot自动配置原理、MyBatis框架使用和源码初步理解、Redis基础和常用数据结构应用 
第三阶段(2-3个月)：学习微服务架构理念、Spring Cloud核心组件(Eureka/Ribbon/Hystrix/Feign/Gateway)、Docker容器基础、消息队列RabbitMQ/Kafka入门、Elasticsearch搜索引擎基础 
第四阶段(长期)：深入研究源码(Spring/MyBatis/JDK)、性能优化实战、分布式系统设计、高可用架构搭建、参与开源项目贡献、关注新技术发展趋势(Kubernetes/Serverless/AI编程助手等) 
