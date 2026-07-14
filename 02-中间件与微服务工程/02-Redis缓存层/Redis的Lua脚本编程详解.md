Redis的Lua脚本编程详解
Redis从2.6版本开始原生支持Lua脚本，允许开发者将多个Redis命令封装成一段Lua脚本，由Redis服务器原子性执行。Lua是一种轻量级、高效的脚本语言，语法简洁、易上手，且与Redis的交互极为友好。引入Lua脚本后，不仅能减少客户端与Redis服务器的网络往返次数，还能保证复杂逻辑的原子性，解决并发场景下的数据一致性问题，是Redis高级应用开发中的核心技能。
一、Redis Lua脚本核心价值
在Redis日常开发中，单纯的单条命令往往无法满足复杂业务需求，而Lua脚本的出现，恰好弥补了这一短板，其核心价值主要体现在以下3点：
1. 减少网络开销，提升性能
    若一个业务逻辑需要执行多个Redis命令（如“查询+判断+修改”），传统方式需客户端与Redis服务器进行多次网络通信（每次命令一次往返），而将这些命令封装成Lua脚本后，只需一次网络请求即可完成所有操作，大幅减少网络延迟，尤其在高延迟网络环境中，性能提升更为明显。
2. 保证复杂逻辑的原子性
    Redis会将整个Lua脚本作为一个整体执行，执行过程中不会被其他Redis命令打断，天然具备原子性——要么脚本中所有命令全部执行成功，要么全部执行失败，无需额外引入分布式锁等机制，即可规避并发场景下的数据竞争问题（如库存扣减、点赞计数等场景）。
3. 简化开发，增强灵活性
    Lua脚本支持条件判断、循环、函数定义等复杂逻辑，开发者可根据业务需求编写自定义脚本，实现Redis原生命令无法直接完成的功能（如复杂的统计计算、多键关联操作），同时脚本可复用，减少重复开发成本。
    二、Redis Lua脚本基础语法
    Redis中的Lua脚本遵循标准Lua 5.1语法，同时提供了专属的Redis交互API，核心分为“脚本编写规范”“Redis API调用”“脚本执行命令”三部分，以下是基础必备知识点。
    1. 脚本编写基本规范
    脚本中通过redis.call()或redis.pcall()调用Redis命令，两者的区别的是：redis.call()执行失败时会抛出异常，中断脚本执行；redis.pcall()执行失败时会返回错误信息，脚本继续执行。
    脚本中可使用Lua的基本数据类型（字符串、数字、布尔值、表等），与Redis数据类型自动兼容（如Redis的String对应Lua的字符串，Redis的Hash对应Lua的表）。
    脚本执行时，Redis会将脚本运行在一个独立的沙箱环境中，禁止访问外部资源（如文件、网络），确保脚本安全，避免影响Redis服务器稳定。
    脚本中可通过ARGV和KEYS获取外部传入的参数：KEYS[n]获取第n个键参数（推荐用于传递Redis键名），ARGV[n]获取第n个普通参数（用于传递数值、字符串等）。
2. 核心Redis API调用示例
    Lua脚本中调用Redis命令的格式与Redis原生命令一致，只需将命令作为redis.call()的参数传入，以下是常见命令的调用示例：
    设置键值：redis.call('SET', KEYS[1], ARGV[1])（KEYS[1]为键名，ARGV[1]为值）
    获取键值：local val = redis.call('GET', KEYS[1])（将获取到的值赋值给Lua变量val）
    自增操作：redis.call('INCR', KEYS[1])
    哈希操作：redis.call('HSET', KEYS[1], 'name', ARGV[1], 'age', ARGV[2])
    条件判断：结合Lua的if-else语法，实现逻辑判断
3. 脚本执行命令
    Redis提供两个核心命令执行Lua脚本，分别是EVAL和EVALSHA，两者功能一致，仅使用方式不同：
    （1）EVAL命令（直接执行脚本）
    语法：EVAL script numkeys key1 key2 ... argv1 argv2 ...
    script：需要执行的Lua脚本字符串；
    numkeys：指定后续参数中“键参数（KEYS）”的数量；
    key1 key2 ...：键参数，对应脚本中的KEYS[1]、KEYS[2]...；
    argv1 argv2 ...：普通参数，对应脚本中的ARGV[1]、ARGV[2]...。
    示例：执行脚本，设置键user:1001的值为zhangsan，并返回该值：
    EVAL "local val = redis.call('SET', KEYS[1], ARGV[1]); return val" 1 user:1001 zhangsan
    （2）EVALSHA命令（通过脚本哈希值执行）
    当脚本较长时，每次使用EVAL命令都会传输完整脚本，增加网络开销。EVALSHA通过脚本的哈希值执行脚本，无需重复传输脚本内容，步骤如下：
    使用SCRIPT LOAD命令将脚本加载到Redis服务器，返回脚本的哈希值；
    使用EVALSHA命令，通过哈希值执行脚本。
    示例：

# 加载脚本，返回哈希值（如"a1b2c3d4..."）
SCRIPT LOAD "local val = redis.call('GET', KEYS[1]); return val"

# 通过哈希值执行脚本
EVALSHA a1b2c3d4... 1 user:1001
（3）辅助脚本命令
SCRIPT EXISTS sha1 sha2 ...：判断指定哈希值的脚本是否已加载到Redis；
SCRIPT FLUSH：清空Redis服务器中所有已加载的脚本；
SCRIPT KILL：终止当前正在执行的Lua脚本（仅适用于脚本执行时间过长的场景，避免阻塞Redis）。
三、Redis Lua脚本实操案例
结合实际业务场景，以下是3个高频Lua脚本案例，覆盖“原子性操作”“复杂逻辑”“分布式锁”等核心场景，可直接复用或修改。
案例1：原子性库存扣减（避免超卖）
业务需求：用户下单时，扣减商品库存，要求库存不能为负，且扣减操作原子性，避免并发超卖。
脚本逻辑：先查询当前库存，若库存大于0，则扣减1，返回扣减后库存；若库存为0，返回失败提示。
-- 脚本内容
local stockKey = KEYS[1]  -- 库存键名，如goods:stock:1001
local stock = tonumber(redis.call('GET', stockKey))  -- 获取当前库存，转为数字
local num = tonumber(ARGV[1])  -- 要扣减的数量，默认为1
if not stock then
    return -1  -- 库存键不存在，返回异常
end
if stock < num then
    return 0  -- 库存不足，扣减失败
end
-- 库存充足，执行扣减
redis.call('DECRBY', stockKey, num)
return tonumber(redis.call('GET', stockKey))  -- 返回扣减后库存
执行命令（扣减商品1001的库存1个）：
EVAL "local stockKey = KEYS[1]; local stock = tonumber(redis.call('GET', stockKey)); local num = tonumber(ARGV[1]); if not stock then return -1 end; if stock < num then return 0 end; redis.call('DECRBY', stockKey, num); return tonumber(redis.call('GET', stockKey))" 1 goods:stock:1001 1
案例2：用户点赞（原子性增加点赞数+记录点赞状态）
业务需求：用户点赞文章，需原子性完成“点赞数+1”和“记录用户点赞状态”，避免重复点赞。
脚本逻辑：先判断用户是否已点赞，若未点赞，则点赞数+1，记录用户点赞状态；若已点赞，返回重复提示。
-- 脚本内容
local likeCountKey = KEYS[1]  -- 文章点赞数键名，如article:like:2001
local likeUserKey = KEYS[2]  -- 文章点赞用户集合键名，如article:like:user:2001
local userId = ARGV[1]  -- 点赞用户ID
-- 判断用户是否已点赞（集合中是否存在该用户ID）
local isLiked = redis.call('SISMEMBER', likeUserKey, userId)
if isLiked == 1 then
    return 2  -- 已点赞，返回重复提示
end
-- 未点赞，执行点赞操作
redis.call('INCR', likeCountKey)  -- 点赞数+1
redis.call('SADD', likeUserKey, userId)  -- 记录用户点赞状态
return 1  -- 点赞成功
执行命令（用户1001点赞文章2001）：
EVAL "local likeCountKey = KEYS[1]; local likeUserKey = KEYS[2]; local userId = ARGV[1]; local isLiked = redis.call('SISMEMBER', likeUserKey, userId); if isLiked == 1 then return 2 end; redis.call('INCR', likeCountKey); redis.call('SADD', likeUserKey, userId); return 1" 2 article:like:2001 article:like:user:2001 1001
案例3：分布式锁释放（原子性释放，避免锁泄露）
业务需求：基于Redis实现分布式锁，释放锁时需原子性判断“锁是否属于当前线程”，避免误释放其他线程的锁，防止锁泄露。
脚本逻辑：先判断锁的value是否为当前线程的标识，若是，则删除锁；若不是，返回释放失败。
-- 脚本内容
local lockKey = KEYS[1]  -- 锁的键名，如lock:order:3001
local lockValue = ARGV[1]  -- 当前线程的锁标识（如UUID）
-- 获取锁的当前value
local currentValue = redis.call('GET', lockKey)
if currentValue == lockValue then
    -- 锁属于当前线程，释放锁
    redis.call('DEL', lockKey)
    return 1  -- 释放成功
end
return 0  -- 锁不属于当前线程，释放失败
执行命令（释放锁，锁标识为uuid-123456）：
EVAL "local lockKey = KEYS[1]; local lockValue = ARGV[1]; local currentValue = redis.call('GET', lockKey); if currentValue == lockValue then redis.call('DEL', lockKey); return 1 end; return 0" 1 lock:order:3001 uuid-123456
四、Redis Lua脚本注意事项
Lua脚本虽强大，但使用不当会导致Redis性能下降、服务阻塞等问题，以下是开发和部署过程中必须注意的5点：
1. 避免脚本执行时间过长
    Redis是单线程模型，Lua脚本执行过程中会阻塞所有其他Redis命令，若脚本执行时间过长（如超过100ms），会严重影响Redis的吞吐量和响应速度。建议脚本逻辑尽量简洁，避免复杂循环（如遍历大量数据），若需处理大量数据，可分批次执行。
2. 谨慎使用全局变量
    Lua脚本中若使用全局变量（未加local关键字），会被所有脚本共享，可能导致变量污染，引发逻辑错误。建议所有变量都添加local关键字，定义为局部变量。
3. 避免在脚本中使用耗时操作
    禁止在Lua脚本中执行耗时操作（如睡眠、循环等待），这类操作会阻塞Redis线程，导致整个服务不可用。同时，脚本中不能访问外部资源（文件、网络），确保脚本的安全性和高效性。
4. 合理处理脚本错误
    根据业务需求选择redis.call()或redis.pcall()：若脚本执行失败后需要中断整个逻辑，使用redis.call()；若需要忽略错误、继续执行后续逻辑，使用redis.pcall()。同时，脚本中可添加错误处理逻辑，返回清晰的错误提示，便于排查问题。
5. 脚本复用与版本管理
    对于常用的Lua脚本，建议统一管理（如存储在代码仓库中），通过SCRIPT LOAD和EVALSHA命令实现复用，减少网络开销。同时，脚本修改后需重新加载，避免使用旧版本脚本导致逻辑错误。
    五、总结
    Redis Lua脚本编程是提升Redis应用性能和安全性的关键手段，其核心优势在于“原子性”和“减少网络开销”，能够完美解决并发场景下的数据一致性问题，简化复杂业务逻辑的开发。掌握Lua脚本的基础语法、Redis API调用方式，结合实际业务场景编写脚本，同时规避常见注意事项，就能充分发挥Redis的强大能力。
    在实际开发中，Lua脚本常用于库存扣减、分布式锁、点赞统计、复杂统计计算等场景，是Redis高级开发的必备技能。建议多结合实操案例练习，熟练掌握脚本的编写、执行和优化技巧，让Redis更好地支撑业务需求。
