03.18 21:59
第14章　分布式缓存Redis实战
在上一章中，我们通过ZooKeeper实现了Netty服务的分布式集群改造，成功解决了集群节点管理、服务注册发现、动态扩缩容等核心协调问题，让Netty HTTP、WebSocket服务摆脱了单机瓶颈，具备了高可用、可水平扩展的能力。但随着分布式集群部署、高并发请求涌入，新的性能与数据一致性问题随之显现：集群内多个Netty节点独立运行，本地缓存无法共享，导致相同业务请求在不同节点重复查询数据库、重复执行业务计算，不仅加大数据库压力，还出现节点间数据不一致的情况；长连接会话、用户状态信息仅存储在单机内存中，客户端切换节点访问后，会话丢失、状态失效；高频请求下，接口响应速度变慢，数据库极易成为性能瓶颈，这些问题直接制约了Netty分布式集群的整体性能与稳定性。
想要解决分布式场景下的数据共享、缓存加速、高并发读写难题，引入高性能分布式缓存组件是行业通用解决方案，而Redis凭借其极高的读写性能、丰富的数据结构、支持持久化与集群部署、易用性强等优势，成为分布式系统的首选缓存中间件。本章将承接ZooKeeper分布式协调的内容，聚焦Redis在Netty分布式通信场景中的实战应用，从核心原理、安装部署、数据结构，到Netty整合Redis实现缓存优化、会话共享、高频数据统计，再到生产环境缓存问题规避，完整搭建“Netty+ZooKeeper+Redis”的分布式核心架构，补齐高性能分布式通信的最后一块拼图，让整套集群架构真正适配高并发、低延迟、数据一致的生产需求。
14.1 分布式缓存引入背景与Redis核心定位
14.1.1 Netty分布式集群的缓存痛点
回顾前序章节的Netty实战，无论是单机服务还是ZooKeeper集群模式，数据存储与读取始终存在三大核心痛点，这也是引入分布式缓存的直接原因：
本地缓存无法跨节点共享，数据不一致：传统本地缓存（如HashMap、ConcurrentHashMap）仅存在于单个Netty节点内存中，集群内其他节点无法访问，相同查询请求在不同节点命中不同缓存，甚至出现数据差异，同时大量重复请求穿透缓存直达数据库，加剧数据库压力。
高频请求导致数据库性能瓶颈：在高并发HTTP接口查询、WebSocket在线状态校验、心跳上报等场景中，大量请求频繁查询数据库，数据库连接数、IO负载飙升，响应延迟急剧增加，严重时会导致数据库宕机，引发整个服务雪崩。
长连接会话与状态无法复用，用户体验差：WebSocket长连接会话、用户登录状态、请求限流计数等信息，存储在单机内存中，客户端通过负载均衡切换节点后，原有会话失效，需要重新连接、重新登录，大幅降低实时通信与接口访问体验。
内存资源浪费，服务重启数据丢失：单机本地缓存占用大量JVM内存，增加GC压力，且服务重启或宕机后，本地缓存数据全部丢失，重启后需要重新加载数据，短时间内大量请求穿透缓存，引发缓存雪崩风险。
14.1.2 Redis核心定位与优势
Redis（Remote Dictionary Server）是一款开源的高性能键值（Key-Value）分布式内存数据库，同时支持数据持久化，既可作为分布式缓存，也可作为轻量级数据库、消息队列使用，是互联网分布式架构中不可或缺的基础组件。针对Netty分布式通信场景，Redis的核心优势完美适配高并发、低延迟需求：
极致读写性能：基于内存操作，读写速度极快，支持每秒10万级读写请求，完全匹配Netty高并发通信的性能要求，大幅降低数据库访问频次。
分布式数据共享：独立部署的中间件模式，集群内所有Netty节点统一访问Redis，实现缓存数据、会话信息、状态数据全局共享，保证跨节点数据一致性。
丰富数据结构：支持String、Hash、List、Set、ZSet、HyperLogLog等多种数据结构，可灵活适配Netty场景下的会话存储、在线人数统计、限流计数、消息队列等多样化需求。
支持数据持久化：提供RDB、AOF两种持久化机制，可将内存数据持久化到磁盘，服务重启后数据不丢失，规避缓存雪崩风险。
高可用与集群扩展：支持主从复制、哨兵模式、集群模式，保证缓存服务高可用，同时支持水平扩展，满足海量数据缓存需求。
轻量级易部署：安装部署简单，资源占用低，测试环境可单机部署，生产环境快速搭建高可用集群，整合成本极低。
14.1.3 Redis在Netty架构中的核心作用
结合本教程Netty实战场景，Redis主要承担三大核心角色，与ZooKeeper形成完美互补：ZooKeeper专注分布式协调，Redis专注分布式缓存与数据共享，二者协同支撑Netty分布式集群稳定运行。
热点数据缓存：缓存数据库高频查询数据、静态配置信息、接口响应结果，大幅减少数据库IO，提升接口与通信响应速度。
全局会话与状态共享：存储WebSocket在线会话、用户登录状态、客户端连接信息，实现跨节点会话复用，客户端切换节点无感知。
高并发管控与数据统计：实现接口限流、防重复提交、在线人数统计、消息计数等功能，依托Redis高性能支撑海量并发管控。
14.2 Redis核心原理与基础特性
14.2.1 Redis部署模式与架构
Redis支持多种部署模式，适配不同场景需求，本教程实战与生产环境推荐采用以下模式，逐步升级：
单机模式：测试环境使用，单节点部署，配置简单、启动快速，适合功能验证与本地调试，本章实战采用此模式。
主从复制模式：一主多从架构，主节点负责写操作，从节点负责读操作，实现读写分离，提升读性能，同时实现数据备份。
哨兵模式：在主从复制基础上，增加哨兵节点监控主从状态，主节点宕机后自动选举新主节点，实现故障自动转移，保证缓存服务高可用，生产环境中小规模集群推荐。
集群模式：多主多从架构，数据分片存储，支持海量数据缓存与高并发读写，适合大规模Netty集群、高并发场景。
14.2.2 Redis核心数据结构（Netty场景适配）
Redis提供多种数据结构，不同结构适配不同业务场景，本章实战重点使用以下四种，完全覆盖Netty通信缓存需求：
1. String（字符串）
最基础、最常用的数据结构，Key-Value一对一存储，Value最大支持512MB，可存储字符串、数字、序列化后的对象。Netty适配场景：缓存接口返回结果、单个用户信息、限流计数、过期会话标识、简单键值对数据。
2. Hash（哈希）
类似Java中的HashMap，存储field-value对，适合存储结构化对象，相比String更节省内存，方便字段单独修改。Netty适配场景：存储WebSocket客户端连接信息、用户详细状态、服务节点配置信息。
3. List（列表）
有序字符串列表，支持左右两端插入、弹出数据，可实现队列、栈功能。Netty适配场景：实现消息队列、缓存历史聊天记录、请求日志暂存。
4. Set（集合）
无序唯一字符串集合，自动去重，支持交集、并集、差集运算。Netty适配场景：存储在线用户ID、客户端唯一标识、IP黑名单，快速统计在线人数。
14.2.3 Redis过期策略与内存淘汰机制
缓存数据并非永久有效，Redis提供完善的过期与淘汰机制，避免内存溢出，适配Netty高频临时缓存需求：
Key过期时间：为缓存Key设置过期时间，到期自动删除，适合会话缓存、临时限流、短期热点数据，避免无效数据占用内存。
内存淘汰策略：当Redis内存达到上限时，自动淘汰部分Key，保证服务稳定运行，生产环境常用策略：volatile-lru（淘汰最近最少使用的过期Key）、allkeys-lru（淘汰所有Key中最近最少使用的），适配Netty高并发缓存场景。
14.2.4 Redis持久化机制
Redis基于内存运行，重启后数据默认丢失，通过持久化机制将数据写入磁盘，重启后自动恢复，保证缓存数据可靠性：
RDB持久化：定时快照，将内存数据全量写入磁盘，恢复速度快，适合数据备份，对性能影响小。
AOF持久化：记录每一条写命令，实时写入磁盘，数据安全性高，适合对数据一致性要求高的场景，可与RDB同时开启。
14.3 Netty整合Redis核心技术方案
14.3.1 Redis客户端选型
Java生态中Redis客户端种类较多，结合Netty异步非阻塞特性，本章选用Jedis与Redisson双客户端方案，兼顾基础缓存与分布式高级功能：
Jedis：轻量级Redis客户端，API简洁，上手快速，适合基础的缓存读写、数据操作，本章基础实战使用。
Redisson：企业级Redis客户端，基于Netty开发，天生适配Netty异步模型，内置分布式锁、分布式限流器、分布式集合等高级功能，生产环境必备，本章高级实战使用。
14.3.2 整合核心思路
Netty整合Redis遵循“全局单例、连接复用、异步优化”原则，避免频繁创建客户端连接导致性能损耗，核心思路如下：
封装Redis连接池工具类，全局维护一个Redis连接实例，实现连接复用，提升性能。
在Netty业务处理器中，注入Redis工具类，实现热点数据缓存、会话共享、限流管控等逻辑。
采用“缓存优先”策略，请求先查询Redis，缓存命中直接返回，缓存未命中再查询数据库，同时将结果写入Redis，后续请求直接命中缓存。
合理设置缓存过期时间，配合内存淘汰机制，避免缓存雪崩、缓存穿透、缓存击穿问题。
14.4 Redis实战环境准备
14.4.1 Redis单机安装与启动
测试环境采用Redis单机部署，Windows与Linux系统均可快速安装，步骤如下：
下载Redis安装包，Windows推荐下载5.x版本，Linux直接通过yum/apt安装。
修改配置文件redis.conf，关闭保护模式（protected-mode no），开启远程访问（bind 0.0.0.0），设置密码（requirepass 123456），方便本地调试。
启动Redis服务，默认端口6379，通过redis-cli客户端连接测试，确认服务正常运行。
14.4.2 核心Maven依赖
引入Jedis、Redisson客户端依赖，同时保持前序Netty、ZooKeeper依赖不变，避免版本冲突，核心依赖如下：
<!-- Redis基础客户端 Jedis -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>3.8.0</version>
</dependency>
<!-- 分布式客户端 Redisson，适配Netty -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.17.7</version>
</dependency>
<!-- 工具类，简化对象序列化 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.83</version>
</dependency>
14.5 Netty+Redis核心实战功能
14.5.1 实战需求总览
本章基于前序第9章Netty HTTP服务、第11章WebSocket服务、第13章ZooKeeper分布式集群，整合Redis实现四大核心功能，全面解决分布式缓存痛点：
热点数据缓存：缓存HTTP接口高频查询数据，实现缓存穿透、击穿防护，降低数据库压力。
WebSocket全局会话共享：存储在线客户端信息，实现跨节点会话复用，切换节点不掉线。
分布式接口限流：基于Redis实现IP限流、接口访问频率限制，防止高并发压垮服务。
在线人数实时统计：基于Redis Set结构，实时统计WebSocket在线人数，数据全局一致。
14.5.2 Redis连接池工具类封装（Jedis）
封装Jedis连接池工具类，实现全局单例、连接复用，避免频繁创建连接，适配Netty高并发场景，核心代码如下：
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
/**
 * Redis连接池工具类，全局单例，实现连接复用
 */
public class RedisUtil {
    // Redis连接地址与端口
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 6379;
    // Redis密码，无密码则为空
    private static final String PASSWORD = "123456";
    // 连接超时时间
    private static final int TIMEOUT = 5000;
    // 连接池实例
    private static JedisPool jedisPool;
    // 静态代码块初始化连接池
    static {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // 最大连接数
        poolConfig.setMaxTotal(50);
        // 最大空闲连接数
        poolConfig.setMaxIdle(20);
        // 最小空闲连接数
        poolConfig.setMinIdle(5);
        // 获取连接超时等待时间
        poolConfig.setMaxWaitMillis(3000);
        // 初始化连接池
        jedisPool = new JedisPool(poolConfig, HOST, PORT, TIMEOUT, PASSWORD);
        System.out.println("Redis连接池初始化成功，连接地址：" + HOST + ":" + PORT);
    }
    /**
     * 获取Redis连接
     */
    public static Jedis getJedis() {
        return jedisPool.getResource();
    }
    /**
     * 释放连接
     */
    public static void close(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }
}
14.5.3 实战一：HTTP接口热点数据缓存
改造Netty HTTP业务处理器，加入缓存逻辑，实现热点数据缓存，采用“先查缓存，再查数据库”的流程，同时设置缓存过期时间，避免数据不一致，核心代码如下：
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import util.RedisUtil;
import redis.clients.jedis.Jedis;
import com.alibaba.fastjson.JSON;
/**
 * 集成Redis缓存的HTTP业务处理器
 */
public class RedisHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    // 缓存前缀
    private static final String CACHE_PREFIX = "netty:http:data:";
    // 缓存过期时间，单位秒，10分钟
    private static final int EXPIRE_TIME = 600;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 获取请求路径，作为缓存Key
        String uri = request.uri();
        String cacheKey = CACHE_PREFIX + uri;
        Jedis jedis = null;
        try {
            jedis = RedisUtil.getJedis();
            // 1. 先查询Redis缓存
            String cacheData = jedis.get(cacheKey);
            if (cacheData != null) {
                // 缓存命中，直接返回缓存数据
                HttpResponseUtil.writeResponse(ctx, request, HttpResponseStatus.OK, cacheData);
                System.out.println("缓存命中，Key：" + cacheKey);
                return;
            }
            // 2. 缓存未命中，模拟查询数据库
            String dbData = "从数据库查询的热点数据：" + uri + "，缓存时间：" + System.currentTimeMillis();
            // 3. 将数据写入Redis，设置过期时间
            jedis.setex(cacheKey, EXPIRE_TIME, dbData);
            // 4. 返回数据
            HttpResponseUtil.writeResponse(ctx, request, HttpResponseStatus.OK, dbData);
            System.out.println("缓存未命中，查询数据库，Key：" + cacheKey);
        } finally {
            RedisUtil.close(jedis);
        }
    }
}
14.5.4 实战二：WebSocket全局会话共享
改造WebSocket业务处理器，将客户端连接信息、在线状态存入Redis Hash结构，实现全局会话共享，客户端断开连接时清理缓存，核心代码如下：
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import util.RedisUtil;
import redis.clients.jedis.Jedis;
import java.util.HashMap;
import java.util.Map;
/**
 * 集成Redis会话共享的WebSocket业务处理器
 */
public class RedisWebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    // 在线用户Hash缓存Key
    private static final String ONLINE_USER_KEY = "netty:websocket:online:users";
    // 在线用户集合Key，用于统计人数
    private static final String ONLINE_USER_SET_KEY = "netty:websocket:online:set";
    /**
     * 客户端上线，存入Redis
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String clientId = ctx.channel().remoteAddress().toString();
        Jedis jedis = RedisUtil.getJedis();
        try {
            // 存储客户端信息
            Map<String, String> userInfo = new HashMap<&gt();
            userInfo.put("clientId", clientId);
            userInfo.put("onlineTime", String.valueOf(System.currentTimeMillis()));
            userInfo.put("channelId", ctx.channel().id().asShortText());
            // Hash结构存储会话信息
            jedis.hset(ONLINE_USER_KEY, clientId, userInfo.toString());
            // Set结构存储在线用户，用于统计
            jedis.sadd(ONLINE_USER_SET_KEY, clientId);
            System.out.println("客户端上线，会话已存入Redis：" + clientId + "，当前在线人数：" + jedis.scard(ONLINE_USER_SET_KEY));
        } finally {
            RedisUtil.close(jedis);
        }
    }
    /**
     * 客户端下线，清理Redis缓存
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String clientId = ctx.channel().remoteAddress().toString();
        Jedis jedis = RedisUtil.getJedis();
        try {
            // 清理会话信息
            jedis.hdel(ONLINE_USER_KEY, clientId);
            jedis.srem(ONLINE_USER_SET_KEY, clientId);
            System.out.println("客户端下线，会话已清理：" + clientId + "，当前在线人数：" + jedis.scard(ONLINE_USER_SET_KEY));
        } finally {
            RedisUtil.close(jedis);
        }
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        String clientMsg = msg.text();
        String clientId = ctx.channel().remoteAddress().toString();
        System.out.println("收到客户端消息：" + clientId + " -> " + clientMsg);
        // 从Redis获取在线用户信息，实现跨节点消息推送
        ctx.channel().writeAndFlush(new TextWebSocketFrame("[缓存共享] 服务端收到：" + clientMsg));
    }
}
14.5.5 实战三：分布式接口限流
基于Redis String结构实现IP接口限流，限制单个IP单位时间内访问次数，防止恶意高频请求，核心逻辑在HTTP处理器中增加限流校验：
/**
 * Redis分布式限流逻辑
 * @param ip 客户端IP
 * @return true：允许访问，false：限流
 */
public static boolean limitAccess(String ip) {
    String limitKey = "netty:http:limit:" + ip;
    Jedis jedis = RedisUtil.getJedis();
    try {
        // 10秒内最多访问5次
        int maxCount = 5;
        int expire = 10;
        Long count = jedis.incr(limitKey);
        if (count == 1) {
            // 第一次访问，设置过期时间
            jedis.expire(limitKey, expire);
        }
        if (count > maxCount) {
            System.out.println("IP限流，IP：" + ip + "，访问次数：" + count);
            return false;
        }
        return true;
    } finally {
        RedisUtil.close(jedis);
    }
}
14.6 缓存问题规避与生产优化
14.6.1 缓存三大问题及解决方案
Redis缓存使用过程中，极易出现三大经典问题，针对Netty高并发场景，解决方案如下：
缓存穿透：查询不存在的数据，缓存始终未命中，请求直达数据库。解决方案：缓存空值、布隆过滤器过滤无效请求。
缓存击穿：热点Key过期，大量请求同时穿透缓存。解决方案：设置热点Key永不过期、加分布式锁、过期时间随机化。
缓存雪崩：大量Key同时过期，或Redis宕机，大量请求压垮数据库。解决方案：过期时间错开、Redis集群高可用、多级缓存、熔断降级。
14.6.2 Netty整合Redis生产优化
连接池参数调优：根据Netty并发量调整最大连接数、空闲连接数，避免连接不足或浪费。
异步操作优化：使用Redisson异步API，避免Redis同步操作阻塞Netty工作线程，影响IO性能。
合理设置过期时间：热点数据长过期，临时数据短过期，避免大量Key同时过期。
开启Redis持久化：生产环境同时开启RDB+AOF，保证数据恢复，避免重启数据丢失。
集群部署：生产环境采用Redis哨兵或集群模式，避免单机缓存故障导致整个服务雪崩。
14.7 功能测试与验证
启动Redis单机服务，确认连接正常，启动整合Redis后的Netty HTTP、WebSocket服务。
测试HTTP接口，第一次访问缓存未命中，查询数据库，后续访问缓存命中，响应速度大幅提升。
启动多个Netty节点，模拟客户端切换节点访问WebSocket，会话正常复用，在线人数统计准确。
高频刷新HTTP接口，测试限流功能，超出访问频率后返回限流提示。
重启Netty节点，Redis缓存数据不丢失，无需重新查询数据库，验证缓存持久化效果。
14.8 本章总结
本章承接第13章ZooKeeper分布式协调的内容，针对Netty分布式集群面临的本地缓存无法共享、数据库性能瓶颈、会话不一致等痛点，系统讲解了分布式缓存Redis的核心原理、数据结构、部署模式，以及在Netty高并发通信场景中的核心应用。通过封装Redis连接池、改造HTTP与WebSocket业务处理器，落地实现了热点数据缓存、全局会话共享、分布式限流、在线人数统计四大核心功能，彻底解决了分布式场景下的数据共享与性能优化问题。
本章完成后，整套教程已构建出“Netty+ZooKeeper+Redis”的完整分布式核心架构：Netty负责高并发通信，ZooKeeper负责分布式协调，Redis负责分布式缓存，三者协同工作，让Netty服务从单机模式升级为高性能、高可用、可扩展的分布式集群，完全适配互联网生产环境的高并发、低延迟、数据一致需求。同时，本章重点讲解了缓存穿透、击穿、雪崩三大问题的规避方案，以及生产环境优化技巧，让实战内容更贴合企业真实场景，为后续开发分布式网关、高并发通信系统、微服务网关奠定了坚实的技术基础。

