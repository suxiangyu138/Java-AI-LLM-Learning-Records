03.21 14:09
SpringBoot整合缓存（入门实战，避坑版）
缓存是后端开发中提升系统性能的核心手段，尤其在数据访问频繁、查询耗时的场景（如数据库查询），通过缓存热点数据，减少数据库访问次数，大幅提升接口响应速度。
SpringBoot对缓存提供了完善的自动配置支持，整合过程简单，无需手动编写复杂缓存逻辑，本文聚焦新手最常用的Spring Cache + Caffeine（本地缓存，轻量高效），从依赖引入、配置、实战使用到注意事项，全程实操，避开新手常见坑。
前置准备：1. 已搭建SpringBoot基础项目（2.7.x版本最佳）；2. 已整合MyBatis（或JDBC），具备基础数据访问能力；3. 了解简单的CRUD逻辑（本文延续前文User表实战）。
一、核心认知（新手必懂）
Spring Cache：Spring提供的缓存抽象框架，统一缓存API，无需关心底层缓存实现（可切换Caffeine、Redis等），通过注解就能实现缓存功能，简化开发。
默认缓存：SpringBoot 2.7.x默认整合Caffeine缓存（本地缓存，轻量、高效，适合单机部署），无需额外部署第三方缓存服务（如Redis），新手优先掌握。
核心注解：缓存的使用全靠注解，无需编写缓存操作代码，重点掌握5个核心注解（后文实战详解）。
避坑提醒：新手初期优先使用本地缓存（Caffeine），无需急于整合Redis（分布式缓存）；若直接整合Redis，需额外部署Redis服务，增加入门难度，容易踩部署、配置坑。
二、第一步：引入缓存依赖（核心步骤）
SpringBoot提供了缓存起步依赖，直接在pom.xml中添加，无需手动管理版本（SpringBoot父依赖统一管理），核心依赖有两个：Spring Cache核心依赖 + Caffeine实现依赖。
<!-- Spring Cache核心依赖（提供缓存抽象和注解） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<!-- Caffeine缓存实现（SpringBoot 2.7.x默认整合，必加） -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
<!-- 可选：若已整合MyBatis，无需重复添加；若未整合，需添加JDBC/MyBatis依赖（用于实战测试） -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.2.2</version>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.30</version>
    <scope>runtime</scope>
</dependency>
注意：无需指定Caffeine版本，SpringBoot父依赖会自动匹配兼容版本（与SpringBoot 2.7.x匹配），手动指定版本易导致冲突。
三、第二步：缓存核心配置（必配，避坑关键）
引入依赖后，需在配置文件（application.properties/yml）中配置Caffeine缓存参数，同时开启缓存注解支持，无需编写Java配置类（新手友好）。
3.1 配置文件配置（application.properties）
# 1. 开启Spring Cache注解支持（核心，必须配置）
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=600s
# 2. 数据库连接配置（实战测试用，与前文一致）
spring.datasource.url=jdbc:mysql://localhost:3306/springboot_db?serverTimezone=Asia/Shanghai&useSSL=false&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
# 3. MyBatis配置（实战测试用）
mybatis.mapper-locations=classpath:mapper/*.xml
mybatis.type-aliases-package=com.example.springboot.data.entity
mybatis.configuration.map-underscore-to-camel-case=true
3.2 配置参数说明（避坑重点）
spring.cache.type=caffeine：指定缓存类型为Caffeine，SpringBoot会自动识别并使用Caffeine缓存，不可省略。
spring.cache.caffeine.spec：Caffeine核心参数，配置缓存规则：
maximumSize=1000：缓存的最大容量（最多缓存1000条数据，超过后会淘汰不常用数据）；
expireAfterWrite=600s：缓存有效期（数据写入后，600秒（10分钟）后自动失效，避免缓存数据与数据库不一致）；
可选补充：expireAfterAccess=300s（数据300秒未被访问则失效），可根据需求组合配置。
避坑提醒：不要遗漏spring.cache.type=caffeine，否则SpringBoot会使用默认的SimpleCacheManager，缓存效果差且不支持过期时间配置。
3.3 启动类开启缓存（关键一步）
在SpringBoot启动类上添加@EnableCaching注解，开启缓存注解支持，否则所有缓存注解（如@Cacheable）都不生效，这是新手最容易踩的坑！
package com.example.springboot.data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching; // 开启缓存注解支持
@SpringBootApplication
@EnableCaching // 必须添加，否则缓存注解无效
public class SpringbootCacheApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringbootCacheApplication.class, args);
    }
}
四、第三步：缓存核心注解实战（必掌握）
开启缓存支持后，通过5个核心注解实现缓存的增、删、改、查联动，结合MyBatis的User表CRUD，实战演示缓存的使用，全程贴合新手需求，每一步都有详细说明。
前提：已创建User实体类、UserMapper（MyBatis XML方式）、UserService（业务层），延续前文的User表（id、name、age、address、createTime）。
4.1 核心注解详解（5个，重点掌握前3个）
注解
作用
核心说明（避坑）
@Cacheable
查询缓存：方法执行前，先查缓存；有缓存则返回缓存数据，无缓存则执行方法，将结果存入缓存
必须指定value（缓存名称，自定义，如"user"），key可选（默认方法参数作为key）
@CachePut
更新缓存：执行方法后，将结果存入缓存（覆盖原有缓存），用于新增、修改操作
与@Cacheable的value、key一致，确保更新的是同一缓存数据
@CacheEvict
删除缓存：执行方法后，删除指定缓存，用于删除操作
可通过allEntries=true删除整个缓存（如删除所有用户缓存）
@Caching
组合注解：用于一个方法上添加多个缓存注解（如同时添加@Cacheable和@CacheEvict）
新手初期很少用到，复杂场景才需要
@CacheConfig
类级注解：统一指定该类所有缓存注解的value（缓存名称），简化代码
无需重复在每个注解上写value，适合类中所有方法用同一缓存名称
4.2 实战：结合UserService实现缓存CRUD
在UserService中添加缓存注解，实现“查询缓存、新增/修改更新缓存、删除清空缓存”，确保缓存与数据库数据一致（避坑核心）。
package com.example.springboot.data.service;
import com.example.springboot.data.entity.User;
import com.example.springboot.data.mapper.UserXmlMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
// @CacheConfig(cacheNames = "user") // 类级注解，统一指定缓存名称，下文可省略value="user"
public class UserCacheService {
    @Autowired
    private UserXmlMapper userXmlMapper;
    // 1. 查询缓存：根据ID查询用户
    // value：缓存名称（自定义），key：缓存的key（用#id表示方法参数id）
    @Cacheable(value = "user", key = "#id")
    public User getUserById(Integer id) {
        // 第一次执行：无缓存，执行该方法（查询数据库），将结果存入缓存
        // 第二次执行：有缓存，直接返回缓存数据，不执行该方法（不查数据库）
        System.out.println("查询数据库，id：" + id); // 用于测试缓存是否生效
        return userXmlMapper.getUserById(id);
    }
    // 2. 查询所有用户（缓存所有用户列表）
    @Cacheable(value = "user", key = "'allUser'") // key自定义为"allUser"，避免与单个用户key冲突
    public List<User> getAllUser() {
        System.out.println("查询数据库，所有用户");
        return userXmlMapper.getAllUser();
    }
    // 3. 新增用户：新增后更新缓存，确保下次查询能获取到新增数据
    @CachePut(value = "user", key = "#user.id") // key为新增用户的id，与查询的key一致
    public User addUser(User user) {
        userXmlMapper.addUser(user); // 新增到数据库
        return user; // 必须返回user，否则缓存中会存入null
    }
    // 4. 修改用户：修改后更新缓存，覆盖原有缓存数据
    @CachePut(value = "user", key = "#user.id")
    public User updateUser(User user) {
        userXmlMapper.updateUser(user); // 修改数据库
        return user; // 必须返回修改后的user，用于更新缓存
    }
    // 5. 删除用户：删除后删除对应缓存，避免缓存中还有该用户数据
    @CacheEvict(value = "user", key = "#id") // 删除指定id的缓存
    public void deleteUser(Integer id) {
        userXmlMapper.deleteUser(id); // 删除数据库数据
    }
    // 可选：删除所有用户后，清空整个user缓存（避免缓存残留）
    @CacheEvict(value = "user", allEntries = true)
    public void deleteAllUser() {
        userXmlMapper.deleteAllUser();
    }
}
4.3 测试缓存效果（新手必做）
通过SpringBoot测试类或Controller调用Service方法，观察控制台输出，验证缓存是否生效：
测试@Cacheable：两次调用getUserById(1)，控制台只打印一次“查询数据库，id：1”，说明第二次使用了缓存；
测试@CachePut：调用addUser(User)后，再调用getUserById(新增用户id)，无需查询数据库，直接返回缓存数据；
测试@CacheEvict：调用deleteUser(1)后，再调用getUserById(1)，控制台会重新打印“查询数据库，id：1”，说明缓存已删除；
避坑提醒：调用@CachePut和@CacheEvict注解的方法，必须确保方法执行成功（无异常），否则缓存不会更新/删除，导致缓存与数据库不一致。
五、新手常见坑及解决方案（重点避坑）
坑1：缓存注解不生效 解决方案：① 检查启动类是否添加@EnableCaching注解（最常见）；② 检查缓存依赖是否引入完整（spring-boot-starter-cache + caffeine）；③ 检查注解是否添加在Service层的方法上（不要添加在Controller层）。
坑2：缓存与数据库数据不一致 解决方案：① 新增/修改操作必须添加@CachePut注解，确保缓存同步更新；② 删除操作必须添加@CacheEvict注解，删除对应缓存；③ 避免缓存有效期过长，合理配置expireAfterWrite（如10-30分钟）。
坑3：@Cacheable注解的方法返回null，导致缓存null值 解决方案：① 确保查询方法不会返回null（如查询不到数据返回空对象，而非null）；② 可添加unless = "#result == null"（不缓存null值），示例：@Cacheable(value = "user", key = "#id", unless = "#result == null")。
坑4：缓存key冲突 解决方案：① 不同方法的缓存key尽量区分（如查询所有用户用key="allUser"）；② 避免使用默认key（默认方法参数拼接），手动指定key更安全。
坑5：修改Caffeine配置后不生效 解决方案：修改application.properties后，重启SpringBoot项目（缓存配置修改后需重启才能生效）。
六、进阶补充（新手后续学习）
缓存切换：后期可将Caffeine（本地缓存）切换为Redis（分布式缓存），只需修改依赖和配置，无需修改业务代码（Spring Cache抽象的优势）；
缓存策略优化：根据业务调整Caffeine的maximumSize和expireAfterWrite，避免缓存容量过大或数据过期不合理；
复杂场景：学习@Caching组合注解，实现复杂缓存逻辑（如查询后删除指定缓存）；
事务与缓存：若Service方法添加了@Transactional注解，需确保缓存注解与事务注解的顺序（@Transactional在@Cacheable之前），避免事务未提交就缓存数据。
新手提示：缓存的核心是“减少重复查询”，适合用于查询频繁、修改较少的数据（如用户详情、字典数据）；对于修改频繁的数据（如订单数据），需谨慎使用缓存，或缩短缓存有效期，避免数据不一致。先掌握本地缓存（Caffeine），再学习分布式缓存（Redis），循序渐进更高效。

