03.22 11:07
Redis核心命令大全
Redis命令是操作Redis的核心，所有与Redis的交互（如数据读写、配置修改、集群管理）都通过命令完成。本文按Redis核心数据类型分类，整理最常用、最实用的命令，每个命令包含语法、说明、实战示例，适配Web应用开发场景，同时补充命令使用注意事项，帮助开发者快速上手、避免踩坑。
说明：本文基于Redis 7.x/8.x稳定版本，所有命令均经过实际测试；命令区分大小写（推荐使用大写，规范且易区分），参数用 [] 表示可选，<> 表示必选。
一、通用命令（所有数据类型通用）
通用命令用于操作Redis的键（Key）、查看服务状态、管理配置等，是所有操作的基础，高频使用且适用所有数据类型。
KEYS <pattern> 说明：查询所有匹配指定模式的键，支持通配符 *（匹配任意字符）、?（匹配单个字符）、[]（匹配指定范围内的字符）。 注意：生产环境慎用！若Redis中键数量过多，会阻塞主线程，导致服务卡顿。 示例： # 查询所有键 127.0.0.1:6379> KEYS * 1) "product:info:1001" 2) "hot:search:rank" # 查询以product开头的键 127.0.0.1:6379> KEYS product:* 1) "product:info:1001" # 查询product:info:100x（x为单个数字） 127.0.0.1:6379> KEYS product:info:100? 1) "product:info:1001" 2) "product:info:1002"
EXISTS <key> [key ...] 说明：判断指定键是否存在，返回存在的键的数量（单个键返回1表示存在，0表示不存在）。 示例： # 判断单个键是否存在 127.0.0.1:6379> EXISTS product:info:1001 (integer) 1 # 判断多个键是否存在 127.0.0.1:6379> EXISTS product:info:1001 user:info:100 (integer) 1
DEL <key> [key ...] 说明：删除指定的一个或多个键，返回删除成功的键的数量（不存在的键会被忽略）。 示例： # 删除单个键 127.0.0.1:6379> DEL product:info:1001 (integer) 1 # 删除多个键 127.0.0.1:6379> DEL product:info:1002 user:info:100 (integer) 1
EXPIRE <key> <seconds> 说明：给指定键设置过期时间（单位：秒），返回1表示设置成功，0表示键不存在或无法设置。 补充：PEXPIRE <key> <milliseconds>（设置过期时间，单位：毫秒）。 示例： # 给product:info:1001设置30分钟过期时间 127.0.0.1:6379> EXPIRE product:info:1001 1800 (integer) 1 # 给user:info:100设置1000毫秒过期时间 127.0.0.1:6379> PEXPIRE user:info:100 1000 (integer) 1
TTL <key> 说明：查看指定键的剩余过期时间（单位：秒），返回值含义： - 正数：剩余过期时间（秒）； - 0：键已过期（即将被删除）； - -1：键无过期时间（永不过期）； - -2：键不存在。 补充：PTTL <key>（查看剩余过期时间，单位：毫秒）。 示例： # 查看product:info:1001的剩余过期时间 127.0.0.1:6379> TTL product:info:1001 (integer) 1756 # 查看永不过期的键 127.0.0.1:6379> TTL hot:search:rank (integer) -1
PERSIST <key> 说明：移除指定键的过期时间，使其永不过期，返回1表示成功，0表示键不存在或无过期时间。 示例： # 移除product:info:1001的过期时间 127.0.0.1:6379> PERSIST product:info:1001 (integer) 1 127.0.0.1:6379> TTL product:info:1001 (integer) -1
TYPE <key> 说明：查看指定键对应的值的数据类型，返回值：string、hash、list、set、zset等。 示例： 127.0.0.1:6379> TYPE product:info:1001 string 127.0.0.1:6379> TYPE hot:search:rank zset
FLUSHDB 说明：清空当前数据库（默认是0号数据库）的所有键，谨慎使用！ 补充：FLUSHALL（清空所有数据库的所有键，生产环境严禁使用）。 示例： # 清空当前数据库 127.0.0.1:6379> FLUSHDB OK
PING 说明：测试Redis服务是否正常运行，返回PONG表示服务正常，否则表示服务异常。 示例： 127.0.0.1:6379> PING PONG
SELECT <dbid> 说明：切换Redis数据库（Redis默认有16个数据库，编号0-15），返回OK表示切换成功。 示例： # 切换到1号数据库 127.0.0.1:6379> SELECT 1 OK 127.0.0.1:6379[1]>
二、String类型命令（最基础、最常用）
String是Redis最基础的数据类型，可存储文本、二进制数据（如图片、序列化对象），最大存储容量为512MB，常用于缓存、计数器、存储简单字符串等场景。
SET <key> <value> [EX <seconds>] [PX <milliseconds>] [NX|XX] 说明：设置键值对，可选参数： - EX <seconds>：设置过期时间（秒），等价于EXPIRE； - PX <milliseconds>：设置过期时间（毫秒）； - NX：只有键不存在时，才设置（用于分布式锁场景）； - XX：只有键存在时，才设置（用于更新场景）。 示例： # 基础设置键值对 127.0.0.1:6379> SET name "Tom" OK # 设置键值对并设置30秒过期 127.0.0.1:6379> SET age 20 EX 30 OK # 只有键不存在时才设置 127.0.0.1:6379> SET name "Jerry" NX (nil) # name已存在，设置失败 # 只有键存在时才更新 127.0.0.1:6379> SET name "Jerry" XX OK
GET <key> 说明：获取指定键的值，若键不存在，返回nil。 示例： 127.0.0.1:6379> GET name "Jerry" 127.0.0.1:6379> GET non_exist_key (nil)
SETNX <key> <value> 说明：等价于SET <key> <value> NX，只有键不存在时才设置，返回1表示成功，0表示失败（分布式锁核心命令）。 示例：127.0.0.1:6379> SETNX lock:order:1001 "1" (integer) 1 # 设置成功，获取锁 127.0.0.1:6379> SETNX lock:order:1001 "1" (integer) 0 # 锁已存在，设置失败
SETEX <key> <seconds> <value> 说明：等价于SET + EXPIRE，设置键值对并指定过期时间（秒），返回OK表示成功。 示例： 127.0.0.1:6379> SETEX user:token:100 86400 "abc123xyz" OK
INCR <key> 说明：将键对应的值（必须是数字）原子递增1，返回递增后的值；若键不存在，先初始化为0再递增。 补充：INCRBY <key> <increment>（原子递增指定数值）、DECR <key>（原子递减1）、DECRBY <key> <decrement>（原子递减指定数值）。 示例（计数器场景）：# 文章阅读量递增 127.0.0.1:6379> INCR article:read:1001 (integer) 1 127.0.0.1:6379> INCR article:read:1001 (integer) 2 # 递增5 127.0.0.1:6379> INCRBY article:read:1001 5 (integer) 7 # 递减1 127.0.0.1:6379> DECR article:read:1001 (integer) 6 # 递减2 127.0.0.1:6379> DECRBY article:read:1001 2 (integer) 4
APPEND <key> <value> 说明：将指定字符串追加到键对应的值末尾，返回追加后字符串的长度；若键不存在，等价于SET <key> <value>。 示例： 127.0.0.1:6379> SET msg "Hello" OK 127.0.0.1:6379> APPEND msg " Redis" (integer) 11 127.0.0.1:6379> GET msg "Hello Redis"
STRLEN <key> 说明：获取键对应的值的字符串长度，若键不存在，返回0。 示例： 127.0.0.1:6379> STRLEN msg (integer) 11 127.0.0.1:6379> STRLEN non_exist_key (integer) 0
GETSET <key> <value> 说明：设置键的新值，并返回键的旧值；若键不存在，返回nil。 示例： 127.0.0.1:6379> GETSET name "Mike" "Jerry" # 返回旧值 127.0.0.1:6379> GET name "Mike" # 新值生效
三、Hash类型命令（适合存储对象）
Hash类型是键值对的集合（类似Java中的Map、Python中的字典），适合存储对象（如用户信息、商品信息），可单独操作对象的某个字段，节省内存且操作高效。
HSET <key> <field> <value> [field value ...] 说明：给Hash类型的键设置一个或多个字段-值对，返回设置成功的字段数量（若字段已存在，会覆盖旧值）。 示例（存储用户信息）： # 设置单个字段 127.0.0.1:6379> HSET user:info:100 username "zhangsan" (integer) 1 # 设置多个字段 127.0.0.1:6379> HSET user:info:100 age 25 gender "male" (integer) 2
HGET <key> <field> 说明：获取Hash类型键中指定字段的值，若键或字段不存在，返回nil。 示例： 127.0.0.1:6379> HGET user:info:100 username "zhangsan" 127.0.0.1:6379> HGET user:info:100 address (nil)
HGETALL &lt;key&gt; 说明：获取Hash类型键中所有的字段-值对，返回一个列表（字段和值交替出现）。 示例： 127.0.0.1:6379> HGETALL user:info:100 1) "username" 2) "zhangsan" 3) "age" 4) "25" 5) "gender" 6) "male"
HDEL <key> <field> [field ...] 说明：删除Hash类型键中指定的一个或多个字段，返回删除成功的字段数量。 示例： 127.0.0.1:6379> HDEL user:info:100 gender (integer) 1 127.0.0.1:6379> HGETALL user:info:100 1) "username" 2) "zhangsan" 3) "age" 4) "25"
HEXISTS <key> <field> 说明：判断Hash类型键中指定字段是否存在，返回1表示存在，0表示不存在。 示例： 127.0.0.1:6379> HEXISTS user:info:100 username (integer) 1 127.0.0.1:6379> HEXISTS user:info:100 gender (integer) 0
HKEYS <key> 说明：获取Hash类型键中所有的字段，返回字段列表。 示例： 127.0.0.1:6379> HKEYS user:info:100 1) "username" 2) "age"
HVALS <key> 说明：获取Hash类型键中所有的字段值，返回值列表。 示例： 127.0.0.1:6379> HVALS user:info:100 1) "zhangsan" 2) "25"
HLEN <key> 说明：获取Hash类型键中字段的数量，若键不存在，返回0。 示例： 127.0.0.1:6379> HLEN user:info:100 (integer) 2 127.0.0.1:6379> HLEN non_exist_key (integer) 0
HINCRBY <key> <field> <increment> 说明：将Hash类型键中指定字段的值（必须是数字）原子递增指定数值，返回递增后的值；若字段不存在，先初始化为0再递增。 补充：HINCRBYFLOAT <key> <field> <increment>（递增浮点数）。 示例： # 用户积分递增10 127.0.0.1:6379> HINCRBY user:info:100 score 10 (integer) 10 # 再递增5 127.0.0.1:6379> HINCRBY user:info:100 score 5 (integer) 15
四、List类型命令（有序可重复，适合队列/栈）
List类型是有序、可重复的字符串列表，底层是双向链表，支持从两端插入、删除数据，适合实现消息队列、栈、时间线等场景。
LPUSH <key> <value> [value ...] 说明：从List的左侧（头部）插入一个或多个值，返回插入后List的长度。 补充：RPUSH <key> <value> [value ...]（从List的右侧（尾部）插入值）。 示例： # 从左侧插入值 127.0.0.1:6379> LPUSH msg:queue "msg1" "msg2" (integer) 2 # 从右侧插入值 127.0.0.1:6379&gt; RPUSH msg:queue "msg3" (integer) 3
LPOP <key> 说明：从List的左侧（头部）弹出一个值（删除并返回），若List为空，返回nil。 补充：RPOP <key>（从List的右侧（尾部）弹出值）。 示例： 127.0.0.1:6379> LPOP msg:queue "msg2" 127.0.0.1:6379> RPOP msg:queue "msg3" 127.0.0.1:6379> LPOP msg:queue "msg1" 127.0.0.1:6379> LPOP msg:queue (nil)
LRANGE <key> <start> <stop> 说明：获取List中从start到stop的所有元素（索引从0开始，stop为-1表示获取所有元素）。 示例： 127.0.0.1:6379> RPUSH list:test 1 2 3 4 5 (integer) 5 # 获取所有元素 127.0.0.1:6379> LRANGE list:test 0 -1 1) "1" 2) "2" 3) "3" 4) "4" 5) "5" # 获取索引1到3的元素 127.0.0.1:6379> LRANGE list:test 1 3 1) "2" 2) "3" 3) "4"
LLEN <key> 说明：获取List的长度（元素个数），若List不存在，返回0。 示例： 127.0.0.1:6379> LLEN list:test (integer) 5 127.0.0.1:6379> LLEN non_exist_list (integer) 0
BRPOP <key> [key ...] <timeout> 说明：阻塞式从List的右侧弹出值，若List为空，会阻塞指定时间（timeout，单位：秒）；若timeout为0，会一直阻塞直到有值可弹出（消息队列核心命令）。 补充：BLPOP <key> [key ...] <timeout>（阻塞式从左侧弹出值）。 示例（消息队列场景）： # 阻塞等待消息，超时时间10秒 127.0.0.1:6379> BRPOP msg:queue 10 # 若10秒内有消息插入，返回消息；否则返回(nil) (10.00s)
LREM <key> <count> <value> 说明：删除List中指定的value，count参数含义： - count > 0：从左侧开始删除，删除count个匹配的value； - count < 0：从右侧开始删除，删除count的绝对值个匹配的value； - count = 0：删除所有匹配的value。 示例： 127.0.0.1:6379> RPUSH list:test 2 2 3 2 4 (integer) 5 # 删除左侧2个值为2的元素 127.0.0.1:6379> LREM list:test 2 2 (integer) 2 127.0.0.1:6379> LRANGE list:test 0 -1 1) "3" 2) "2" 3) "4"
五、Set类型命令（无序不重复，适合标签/好友列表）
Set类型是无序、不重复的字符串集合，支持交集、并集、差集等操作，适合存储标签、好友列表、去重等场景。
SADD <key> <member> [member ...] 说明：向Set中添加一个或多个成员，返回添加成功的成员数量（重复的成员会被忽略）。 示例（存储用户标签）： 127.0.0.1:6379> SADD user:tag:100 "java" "redis" "mysql" (integer) 3 # 添加重复成员（会被忽略） 127.0.0.1:6379> SADD user:tag:100 "java" (integer) 0
SREM <key> <member> [member ...] 说明：从Set中删除一个或多个成员，返回删除成功的成员数量（不存在的成员会被忽略）。 示例： 127.0.0.1:6379> SREM user:tag:100 "mysql" (integer) 1
SMEMBERS <key> 说明：获取Set中的所有成员，返回成员列表（无序）。 示例： 127.0.0.1:6379> SMEMBERS user:tag:100 1) "java" 2) "redis"
SISMEMBER <key> <member> 说明：判断指定成员是否在Set中，返回1表示存在，0表示不存在。 示例（判断用户是否有某个标签）： 127.0.0.1:6379> SISMEMBER user:tag:100 "java" (integer) 1 127.0.0.1:6379> SISMEMBER user:tag:100 "mysql" (integer) 0
SCARD <key> 说明：获取Set中的成员数量，若Set不存在，返回0。 示例： 127.0.0.1:6379> SCARD user:tag:100 (integer) 2
SINTER <key1> [key2 ...] 说明：求多个Set的交集（返回同时存在于所有Set中的成员）。 示例（求两个用户的共同标签）： 127.0.0.1:6379> SADD user:tag:101 "redis" "python" "mongodb" (integer) 3 127.0.0.1:6379> SINTER user:tag:100 user:tag:101 1) "redis"
SUNION <key1> [key2 ...] 说明：求多个Set的并集（返回所有Set中的成员，去重）。 示例： 127.0.0.1:6379> SUNION user:tag:100 user:tag:101 1) "java" 2) "redis" 3) "python" 4) "mongodb"
SDIFF <key1> [key2 ...] 说明：求两个Set的差集（返回存在于key1中，但不存在于其他key中的成员）。 示例： 127.0.0.1:6379> SDIFF user:tag:100 user:tag:101 1) "java"
六、Sorted Set类型命令（有序不重复，适合排行榜）
Sorted Set（有序集合）与Set类似，成员不重复，但每个成员关联一个分数（score），Redis会根据分数自动对成员排序，适合实现排行榜、优先级队列等场景。
ZADD &lt;key&gt; &lt;score&gt; &lt;member&gt; [score member ...] 说明：向Sorted Set中添加一个或多个成员及对应的分数，返回添加成功的成员数量（重复成员会更新分数）。 示例（热搜排行榜）： 127.0.0.1:6379> ZADD hot:search:rank 100 "Redis" 80 "Java" 90 "MySQL" (integer) 3 # 更新"Java"的分数 127.0.0.1:6379> ZADD hot:search:rank 85 "Java" (integer) 0
ZREM <key> <member> [member ...] 说明：从Sorted Set中删除一个或多个成员，返回删除成功的成员数量。 示例： 127.0.0.1:6379> ZREM hot:search:rank "MySQL" (integer) 1
ZRANGE <key> <start> <stop> [WITHSCORES] 说明：按分数升序排列，获取从start到stop的成员（索引从0开始，stop为-1表示所有成员）；加上WITHSCORES，会同时返回成员和对应的分数。 补充：ZREVRANGE <key> <start> <stop> [WITHSCORES]（按分数降序排列，排行榜核心命令）。 示例（获取热搜Top2）： # 按分数降序，获取前2个成员（带分数） 127.0.0.1:6379> ZREVRANGE hot:search:rank 0 1 WITHSCORES 1) "Redis" 2) "100" 3) "Java" 4) "85" # 按分数升序，获取所有成员 127.0.0.1:6379> ZRANGE hot:search:rank 0 -1 1) "Java" 2) "Redis"
ZSCORE <key> <member> 说明：获取Sorted Set中指定成员的分数，若成员不存在，返回nil。 示例： 127.0.0.1:6379> ZSCORE hot:search:rank "Redis" "100"
ZINCRBY <key> <increment> <member> 说明：将Sorted Set中指定成员的分数原子递增指定数值，返回递增后的分数；若成员不存在，先初始化为0再递增（排行榜点击量递增核心命令）。 示例： # "Redis"点击量递增5 127.0.0.1:6379> ZINCRBY hot:search:rank 5 "Redis" "105"
ZRANK <key> <member> 说明：按分数升序排列，获取指定成员的排名（索引从0开始）；若成员不存在，返回nil。 补充：ZREVRANK <key> <member>（按分数降序排列，获取排名，排行榜核心命令）。 示例： # 获取"Redis"的降序排名（0为第一名） 127.0.0.1:6379> ZREVRANK hot:search:rank "Redis" (integer) 0 # 获取"Java"的降序排名 127.0.0.1:6379> ZREVRANK hot:search:rank "Java" (integer) 1
ZCARD <key> 说明：获取Sorted Set中的成员数量，若键不存在，返回0。 示例： 127.0.0.1:6379> ZCARD hot:search:rank (integer) 2
七、Redis命令使用注意事项
避免阻塞主线程：Redis主线程为单线程，避免使用KEYS、FLUSHALL、FLUSHDB、HGETALL（大Hash）、SMEMBERS（大Set）等命令，这些命令会遍历所有数据，导致服务卡顿。替代方案：用SCAN（遍历键）、HSCAN（遍历Hash）、SSCAN（遍历Set）、ZSCAN（遍历Sorted Set），分批获取数据。
原子性命令优先：高并发场景（如秒杀、计数器），优先使用Redis原子性命令（INCR、DECR、SETNX、ZINCRBY等），避免使用Lua脚本或事务，减少复杂度和阻塞风险。
键设计规范：遵循“业务模块:数据类型:唯一标识”格式（如user:info:100、article:read:1001），避免键冲突、键过长（影响性能），便于后续维护和排查问题。
过期时间合理设置：根据数据更新频率设置过期时间，避免过期时间过长（缓存脏数据）或过短（增加缓存穿透概率）；核心业务数据可设置“永不过期+主动更新”。
生产环境禁用危险命令：通过Redis配置文件（redis.conf）禁用FLUSHALL、FLUSHDB、KEYS等危险命令，避免误操作导致数据丢失。
批量操作优化：批量操作（如批量设置Hash字段、批量添加Set成员），优先使用支持批量参数的命令（如HSET、SADD），避免循环调用单个命令，减少网络开销。
八、总结
本文整理了Redis最核心、最常用的命令，按数据类型分类，每个命令都搭配实战示例，贴合Web应用开发场景（缓存、计数器、消息队列、排行榜等）。掌握这些命令，就能满足日常开发中90%以上的Redis操作需求。
使用Redis命令的核心原则是：优先使用原子性命令、避免阻塞主线程、规范键设计、合理设置过期时间。后续可根据业务需求，深入学习Redis高级命令（如Lua脚本、事务、集群相关命令），进一步提升Redis使用效率。

