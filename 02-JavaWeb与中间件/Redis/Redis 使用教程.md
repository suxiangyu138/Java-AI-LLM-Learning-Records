Redis 使用教程
一、Redis 是什么
Redis 是基于内存的键值型数据库，读写速度极快，常用于缓存、限流、分布式锁、消息队列、会话存储等场景。
支持多种数据结构，包括字符串、哈希、列表、集合、有序集合等。

------------------------------------------------------------------------------------------------------------
二、安装与启动
Linux 安装步骤
1. 下载并解压 Redis
    wget https://download.redis.io/releases/redis-7.0.10.tar.gz
    tar -zxvf redis-7.0.10.tar.gz
    cd redis-7.0.10
2. 编译
    make
3. 安装到指定目录
    make install PREFIX=/usr/local/redis
4. 后台运行配置
    复制配置文件
    cp redis.conf /usr/local/redis/
    进入配置文件修改
    daemonize yes
    requirepass 你的密码
    bind 0.0.0.0
    port 6379
5. 启动
    /usr/local/redis/bin/redis-server /usr/local/redis/redis.conf
6. 客户端连接
    /usr/local/redis/bin/redis-cli -a 密码
    ------------------------------------------------------------------------------------------------------------ 三、常用配置说明
    bind 绑定可访问的 IP，生产环境不要用 0.0.0.0 除非有安全策略
    port 端口，默认 6379
    requirepass 设置密码，生产环境必须开启
    daemonize 是否后台运行
    logfile 日志文件路径
    dir 数据文件存放目录
    dbfilename 持久化文件名
    maxmemory 最大使用内存
    appendonly 是否开启 AOF 持久化
    ------------------------------------------------------------------------------------------------------------ 四、五种核心数据结构与命令
    1. 字符串 String
    适用场景：缓存、计数器、token 存储
    设置值
    set key value
    设置并指定过期时间（秒）
    set key value ex 10
    获取值
    get key
    数字自增
    incr key
    数字自减
    decr key
    删除
    del key
2. 哈希 Hash
    适用场景：存储对象信息，如用户信息、商品信息
    设置字段
    hset key field value
    批量设置
    hmset key field1 value1 field2 value2
    获取单个字段
    hget key field
    获取所有字段和值
    hgetall key
    获取所有字段
    hkeys key
    获取所有值
    hvals key
    删除字段
    hdel key field
3. 列表 List
    适用场景：队列、栈、消息队列
    从左边加入
    lpush key value1 value2
    从右边加入
    rpush key value1 value2
    从左边弹出
    lpop key
    从右边弹出
    rpop key
    查看列表内容
    lrange key 0 -1
    获取列表长度
    llen key
4. 集合 Set
    适用场景：去重、共同好友、点赞、标签
    添加元素
    sadd key value1 value2
    查看所有元素
    smembers key
    判断是否存在
    sismember key value
    删除元素
    srem key value
    求两个集合交集
    sinter key1 key2
    求并集
    sunion key1 key2
5. 有序集合 ZSet
    适用场景：排行榜、热度排序、延时任务
    添加元素并指定分数
    zadd key score1 value1 score2 value2
    按分数从小到大查看
    zrange key 0 -1
    按分数从大到小查看
    zrevrange key 0 -1
    获取元素分数
    zscore key value
    删除元素
    zrem key value
    ------------------------------------------------------------------------------------------------------------ 五、持久化
    Redis 有两种持久化方式
    1. RDB
    定时将内存数据保存为快照文件
    优点：恢复快、文件小
    缺点：可能丢失最后一次快照后的数据
2. AOF
    记录每一条写命令
    优点：数据安全性高
    缺点：文件大、恢复较慢
    生产环境一般同时开启，保证数据安全。
    ------------------------------------------------------------------------------------------------------------ 六、高级常用功能
    1. 设置过期时间
    expire key 秒数
    查看剩余时间
    ttl key
    取消过期
    persist key
2. 查看所有键
    keys *
    生产环境不建议使用，会阻塞 Redis
3. 判断键是否存在
    exists key
4. 清空当前库
    flushdb
    清空所有库
    flushall
    ------------------------------------------------------------------------------------------------------------ 七、Java 中使用 Redis（Spring Boot）
    1. 引入依赖
    <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
2. 配置文件
    spring:
  redis:
    host: 127.0.0.1
    port: 6379
    password: 你的密码
    database: 0
3. 使用 StringRedisTemplate
    注入 StringRedisTemplate
    直接调用 opsForValue、opsForHash、opsForList 等方法操作对应数据结构
    ------------------------------------------------------------------------------------------------------------ 八、常见使用场景
    1. 缓存：减轻数据库压力
    2. 分布式锁：保证多服务并发安全
    3. 限流：接口访问频率控制
    4. 会话存储：代替 session
    5. 排行榜：有序集合实现
    6. 消息队列：列表结构实现
    7. 去重：集合结构实现
    8. 延时任务：有序集合实现
