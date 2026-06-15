MyBatis 缓存配置详解
MyBatis 缓存机制是优化数据库访问性能的核心手段，通过减少重复SQL查询、降低数据库交互开销，提升系统响应速度。其缓存分为 一级缓存（本地缓存） 和二级缓存（全局缓存），二者作用域、配置方式和使用场景各有不同，以下是完整配置指南及注意事项。
一、一级缓存（默认开启，无需手动配置）
1.1 核心特性
作用域：SqlSession 级别（会话级缓存），仅在当前 SqlSession 内有效，不同 SqlSession 之间的缓存相互独立。
默认行为：自动开启，无需在配置文件中添加任何配置，MyBatis 会自动对同一个 SqlSession 内的重复查询进行缓存。
缓存触发与失效：
命中缓存：同一个 SqlSession 内，执行相同的 SQL（参数、SQL语句完全一致），第一次查询走数据库，后续查询直接从缓存获取。
缓存失效：执行 insert、update、delete 操作（无论是否提交事务），会自动清空当前 SqlSession 的一级缓存；调用 sqlSession.clearCache() 手动清空；关闭 SqlSession（sqlSession.close()），缓存自动销毁。
1.2 示例验证（代码参考）
try (SqlSession session = sqlSessionFactory.openSession()) {
    // 第一次查询：走数据库，结果存入一级缓存
    UserMapper userMapper = session.getMapper(UserMapper.class);
    User user1 = userMapper.selectById(1);
    // 第二次查询：参数、SQL一致，从一级缓存获取，不执行数据库操作
    User user2 = userMapper.selectById(1);
    System.out.println(user1 == user2); // true（内存地址相同，取自缓存）
    // 执行update操作，清空当前SqlSession的一级缓存
    userMapper.updateName(1, "newName");
    // 第三次查询：缓存已失效，重新走数据库
    User user3 = userMapper.selectById(1);
    System.out.println(user1 == user3); // false
}
//  SqlSession关闭，一级缓存销毁
1.3 注意事项
一级缓存无法跨 SqlSession 共享，若多个 SqlSession 执行相同查询，仍会多次访问数据库。
避免在长会话（如web项目中未及时关闭的 SqlSession）中过度依赖一级缓存，可能导致缓存数据与数据库数据不一致（脏数据）。
二、二级缓存（全局缓存，需手动配置）
二级缓存是 Mapper 级别的缓存（全局缓存），作用域覆盖所有 SqlSession，多个 SqlSession 可共享同一个 Mapper 的缓存数据，大幅提升查询性能，需手动配置开启。
2.1 二级缓存开启条件（三步配置）
第一步：开启全局二级缓存（mybatis-config.xml）
在 MyBatis 核心配置文件中，通过 <settings> 标签开启全局二级缓存开关（默认值为 true，可省略，但建议显式配置，提高可读性）。
<configuration>
    <!-- 全局配置 -->
    &lt;settings&gt;
        <!-- 开启二级缓存（全局开关） -->
        <setting name="cacheEnabled" value="true"/>
    </settings>
</configuration>
第二步：在 Mapper 接口/XML 中开启二级缓存
全局开关开启后，需为单个 Mapper 单独开启二级缓存，有两种配置方式（二选一）：
方式1：XML 映射文件配置（推荐，灵活度高）
在对应 Mapper 的 XML 文件中添加 <cache> 标签，即可开启该 Mapper 的二级缓存。
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper"&gt;
    <!-- 开启当前Mapper的二级缓存 -->
    &lt;cache/&gt;
    <!-- 具体SQL映射 -->
    <select id="selectById" resultType="com.example.entity.User">
        select * from user where id = #{id}
    </select>
</mapper>
方式2：注解配置（适用于注解开发）
在 Mapper 接口上添加 @CacheNamespace 注解，开启二级缓存。
// 开启当前Mapper的二级缓存
@CacheNamespace
public interface UserMapper {
    @Select("select * from user where id = #{id}")
    User selectById(Integer id);
}
第三步：确保实体类可序列化（关键）
二级缓存会将数据序列化后存储（默认使用 Java 序列化），因此缓存的实体类（如 User）必须实现 Serializable 接口，否则会抛出序列化异常。
// 实体类实现Serializable接口
public class User implements Serializable {
    private Integer id;
    private String name;
    // getter/setter、toString等方法
}
2.2 二级缓存的特性
作用域：Mapper 级别（namespace 级别），同一个 Mapper 下的所有 SqlSession 共享缓存，不同 Mapper 的缓存相互独立。
缓存存储：默认存储在内存中，可配置第三方缓存（如 Redis、Ehcache）替代默认缓存（后续讲解）。
缓存失效：
当前 Mapper 下执行 insert、update、delete 操作，会自动清空该 Mapper 的二级缓存。
手动清空：通过 sqlSessionFactory.getConfiguration().getCache("com.example.mapper.UserMapper").clear() 清空指定 Mapper 的缓存。
缓存过期：可通过 <cache> 标签配置过期时间，默认无过期时间。
2.3 二级缓存高级配置（<cache> 标签属性）
通过 <cache> 标签的属性，可自定义二级缓存的行为，常用属性如下：
<cache 
    eviction="LRU"       <!-- 缓存回收策略（默认LRU） -->
    flushInterval="60000" <!-- 缓存刷新间隔（毫秒），默认无间隔 -->
    size="1024"           <!-- 缓存最多存储的对象数量，默认1024 -->
    readOnly="false"      <!-- 是否只读（true：只读，性能高；false：可读写，默认） -->
    type="com.example.cache.MyCache"/&gt; <!-- 自定义缓存实现类（如Redis缓存） -->
常用缓存回收策略（eviction）
LRU（Least Recently Used）：最近最少使用，默认策略，移除最长时间未被使用的缓存对象。
FIFO（First In First Out）：先进先出，按缓存添加顺序移除最早添加的对象。
SOFT（软引用）：基于JVM软引用，内存不足时移除缓存对象。
WEAK（弱引用）：基于JVM弱引用，只要GC执行，就会移除缓存对象。
三、第三方缓存集成（以Redis为例）
MyBatis 默认二级缓存为内存缓存，存在重启丢失、分布式环境下缓存不共享的问题，实际开发中常集成 Redis 作为第三方缓存，实现分布式缓存共享。
3.1 依赖导入（Maven）
<!-- MyBatis Redis缓存依赖 -->
<dependency>
    <groupId>org.mybatis.caches</groupId>
    <artifactId>mybatis-redis</artifactId>
    <version>1.0.0-beta2</version&gt;
&lt;/dependency&gt;
<!-- Redis依赖 -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>3.8.0</version>
</dependency>
3.2 配置Redis缓存（mybatis-config.xml）
<configuration>
    <settings>
        <setting name="cacheEnabled" value="true"/&gt; <!-- 开启全局二级缓存 -->
    </settings>
   <!-- 配置Redis缓存 -->
    <typeAliases>
        <typeAlias type="org.mybatis.caches.redis.RedisCache" alias="redisCache"/>
    </typeAliases>
</configuration>
3.3 在Mapper中使用Redis缓存
<mapper namespace="com.example.mapper.UserMapper"&gt;
    <!-- 使用Redis作为当前Mapper的二级缓存 -->
    <cache type="redisCache"/&gt;
    <!-- SQL映射 -->
    <select id="selectById" resultType="com.example.entity.User">
        select * from user where id = #{id}
    </select>
</mapper>
3.4 Redis缓存配置（可选，自定义Redis连接）
默认使用本地Redis（127.0.0.1:6379），若Redis地址、端口、密码不同，可在 resources 目录下创建 redis.properties 文件配置：
redis.host=192.168.1.100
redis.port=6379
redis.password=123456
redis.timeout=3000
redis.database=0
四、缓存使用注意事项（重点）
数据一致性：缓存会导致数据“读写分离”，若数据库数据被手动修改（非通过MyBatis操作），缓存数据会与数据库不一致，需谨慎使用（适合读多写少的场景，如商品详情、字典数据）。
禁止缓存的场景：高频更新的数据（如订单、库存）不建议使用二级缓存，避免缓存失效频繁，反而增加性能开销；查询结果不固定（如带随机函数、当前时间的SQL），无需缓存。
单个SQL禁用缓存：若某个SQL无需使用缓存（无论一级还是二级），可在SQL上添加 useCache="false"（XML）或 @Options(useCache = false)（注解）。
分布式环境：默认二级缓存无法跨节点共享，必须集成Redis、Ehcache等分布式缓存，否则会出现节点间缓存不一致的问题。
序列化问题：二级缓存（无论默认还是第三方）都需要实体类实现Serializable接口，否则会抛出序列化异常。
五、总结
1. 一级缓存：SqlSession 级，默认开启，无需配置，适合单会话内重复查询，注意避免长会话导致的脏数据。
2. 二级缓存：Mapper 级，需手动开启（全局开关+Mapper配置），适合多会话共享的读多写少场景，需实现实体类序列化。
3. 第三方缓存：分布式环境必用，推荐集成Redis，解决默认缓存重启丢失、跨节点不共享的问题。
4. 核心原则：缓存是“空间换时间”，需根据业务场景选择是否使用，优先保证数据一致性，再追求性能优化。
