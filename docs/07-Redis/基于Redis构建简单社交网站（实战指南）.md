03.22 11:31
基于Redis构建简单社交网站（实战指南）
本文将构建一个简单社交网站，聚焦核心社交功能（用户注册登录、好友关系、动态发布与查看、点赞评论），依托Redis的高性能、丰富数据结构（Hash、Set、Sorted Set、List）实现轻量部署，无需复杂架构，适合入门实战，同时保证功能完整、流程流畅，可直接基于此框架扩展更多社交场景。
核心设计原则：简化业务逻辑，聚焦“能用、好用”，依托Redis实现核心数据存储和交互，避免引入过多中间件，降低开发和部署成本；同时预留扩展空间，后续可轻松添加私信、话题、热搜等功能。
一、简单社交网站核心功能与架构设计
1.1 核心功能（极简版）
聚焦社交核心场景，摒弃复杂功能，保留4大核心模块，确保流程闭环：
用户模块：注册、登录（会话管理）、个人信息查看与修改；
好友模块：添加好友、删除好友、查看好友列表；
动态模块：发布动态、查看好友动态（朋友圈）；
互动模块：给动态点赞、评论，查看点赞/评论列表。
1.2 架构设计（轻量版）
采用“前端+后端+Redis”三层架构，无需数据库（简化设计，Redis承担所有数据存储），后端负责业务逻辑处理，Redis负责数据持久化和高性能交互，架构简洁、易部署：
前端：简单页面（注册、登录、首页、个人中心），通过接口与后端交互；
后端：基于Spring Boot开发，集成Redis客户端，提供核心接口；
Redis：存储所有数据（用户信息、好友关系、动态、点赞评论），依托不同数据结构实现高效查询。
1.3 Redis数据结构设计（核心）
根据社交功能特点，合理选择Redis数据结构，确保数据存储高效、查询便捷，核心Key设计遵循“模块:唯一标识”规范，避免冲突：
功能模块
Redis数据结构
Key格式
Value说明
用户信息
Hash
user:userId
存储用户详情（用户名、密码、头像、简介等）
用户登录态
String
session:sessionId
存储sessionId对应的userId，设置过期时间
好友关系
Set
friend:userId
存储该用户的所有好友userId（去重）
动态发布
Sorted Set
moment:userId
存储该用户发布的动态ID，Score为发布时间戳（倒序排序）
动态详情
Hash
moment:momentId
存储动态详情（内容、发布时间、发布者ID）
动态点赞
Set
like:momentId
存储给该动态点赞的userId（去重，避免重复点赞）
动态评论
List
comment:momentId
存储该动态的所有评论（按时间顺序排列）
二、核心功能组件实现（Spring Boot + Redis）
以下组件均为可直接集成的实战代码，依托Spring Boot框架，注入RedisTemplate，实现核心社交功能，代码简洁、注释清晰，适合快速上手。
2.1 基础准备（依赖与配置）
1. 引入依赖（pom.xml）
<!-- Spring Boot核心依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- Redis依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- 工具依赖（JSON序列化、加密） -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.83</version>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
</dependency>
2. Redis配置（application.yml）
spring:
  redis:
    host: 127.0.0.1  # Redis地址，本地部署直接用127.0.0.1
    port: 6379       # Redis端口，默认6379
    password:        # 若Redis设置了密码，填写密码，否则留空
    lettuce:
      pool:
        max-active: 10  # 最大连接数
        max-idle: 5     # 最大空闲连接数
        min-idle: 2     # 最小空闲连接数
  jackson:
    default-property-inclusion: non_null  # JSON序列化时忽略null值
server:
  port: 8080  # 后端服务端口
2.2 用户模块组件（注册、登录、个人信息）
核心功能：用户注册（存储用户信息）、登录（生成会话）、个人信息查看与修改，依托Redis Hash存储用户详情，String存储会话。
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
/**
 * 用户模块组件（注册、登录、个人信息管理）
 */
@Component
public class UserComponent {
    private final StringRedisTemplate stringRedisTemplate;
    private final HashOperations<String, String, String> hashOperations;
    // Key前缀（规范命名，避免冲突）
    private static final String USER_PREFIX = "user:";
    private static final String SESSION_PREFIX = "session:";
    // 会话过期时间（2小时）
    private static final long SESSION_EXPIRE = 7200;
    public UserComponent(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.hashOperations = stringRedisTemplate.opsForHash();
    }
    /**
     * 用户注册
     * @param username 用户名（唯一）
     * @param password 密码（加密存储）
     * @param avatar 头像地址（可选，默认空）
     * @param intro 个人简介（可选，默认空）
     * @return 注册成功返回userId，失败返回null（用户名已存在）
     */
    public Long register(String username, String password, String avatar, String intro) {
        // 1. 校验用户名是否已存在（简化：查询所有用户，判断用户名是否重复）
        if (isUsernameExists(username)) {
            return null;
        }
        // 2. 生成唯一userId（简化：自增ID，Redis自增实现）
        Long userId = stringRedisTemplate.opsForValue().increment("user:id:seq", 1);
        // 3. 密码加密（MD5加密，实际开发可使用更安全的加密方式）
        String encryptedPwd = DigestUtils.md5DigestAsHex(password.getBytes());
        // 4. 存储用户信息（Hash类型）
        String userKey = USER_PREFIX + userId;
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("userId", userId.toString());
        userInfo.put("username", username);
        userInfo.put("password", encryptedPwd);
        userInfo.put("avatar", avatar == null ? "" : avatar);
        userInfo.put("intro", intro == null ? "暂无简介" : intro);
        hashOperations.putAll(userKey, userInfo);
        // 5. 返回userId
        return userId;
    }
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码（明文）
     * @return 登录成功返回sessionId（前端存储，用于后续请求校验），失败返回null
     */
    public String login(String username, String password) {
        // 1. 查询用户ID（根据用户名）
        Long userId = getUserIdByUsername(username);
        if (userId == null) {
            return null; // 用户名不存在
        }
        // 2. 校验密码
        String userKey = USER_PREFIX + userId;
        String encryptedPwd = hashOperations.get(userKey, "password");
        String inputEncryptedPwd = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!inputEncryptedPwd.equals(encryptedPwd)) {
            return null; // 密码错误
        }
        // 3. 生成sessionId，存储会话（String类型）
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String sessionKey = SESSION_PREFIX + sessionId;
        stringRedisTemplate.opsForValue().set(sessionKey, userId.toString(), SESSION_EXPIRE, TimeUnit.SECONDS);
        // 4. 返回sessionId
        return sessionId;
    }
    /**
     * 查看个人信息
     * @param userId 用户ID
     * @return 用户信息Map（包含用户名、头像、简介等）
     */
    public Map<String, String> getUserInfo(Long userId) {
        String userKey = USER_PREFIX + userId;
        return hashOperations.entries(userKey);
    }
    /**
     * 修改个人信息（仅支持头像、简介修改）
     * @param userId 用户ID
     * @param avatar 新头像地址（可选）
     * @param intro 新简介（可选）
     * @return 修改成功返回true
     */
    public boolean updateUserInfo(Long userId, String avatar, String intro) {
        String userKey = USER_PREFIX + userId;
        if (!stringRedisTemplate.hasKey(userKey)) {
            return false; // 用户不存在
        }
        if (avatar != null && !avatar.isEmpty()) {
            hashOperations.put(userKey, "avatar", avatar);
        }
        if (intro != null && !intro.isEmpty()) {
            hashOperations.put(userKey, "intro", intro);
        }
        return true;
    }
    /**
     * 校验会话是否有效（用于接口拦截，判断用户是否登录）
     * @param sessionId 前端传递的sessionId
     * @return 有效返回userId，无效返回null
     */
    public Long validateSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        String sessionKey = SESSION_PREFIX + sessionId;
        String userIdStr = stringRedisTemplate.opsForValue().get(sessionKey);
        if (userIdStr == null) {
            return null; // 会话过期或无效
        }
        // 会话续期（用户操作时刷新过期时间）
        stringRedisTemplate.expire(sessionKey, SESSION_EXPIRE, TimeUnit.SECONDS);
        return Long.parseLong(userIdStr);
    }
    /**
     * 退出登录（删除会话）
     * @param sessionId 会话ID
     * @return 退出成功返回true
     */
    public boolean logout(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        String sessionKey = SESSION_PREFIX + sessionId;
        return Boolean.TRUE.equals(stringRedisTemplate.delete(sessionKey));
    }
    // ------------------------------ 私有工具方法 ------------------------------
    /**
     * 校验用户名是否已存在（简化实现，适合小型社交网站）
     */
    private boolean isUsernameExists(String username) {
        // 模糊查询所有用户Key，遍历判断用户名是否重复（数据量小时可用）
        String pattern = USER_PREFIX + "*";
        return stringRedisTemplate.keys(pattern).stream()
                .anyMatch(key -> username.equals(hashOperations.get(key, "username")));
    }
    /**
     * 根据用户名查询用户ID
     */
    private Long getUserIdByUsername(String username) {
        String pattern = USER_PREFIX + "*";
        return stringRedisTemplate.keys(pattern).stream()
                .filter(key -> username.equals(hashOperations.get(key, "username")))
                .findFirst()
                .map(key -> Long.parseLong(key.replace(USER_PREFIX, "")))
                .orElse(null);
    }
}
2.3 好友模块组件（添加、删除、查看好友）
核心功能：添加好友（双向添加）、删除好友（双向删除）、查看好友列表，依托Redis Set存储好友关系，实现去重和高效查询。
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * 好友模块组件（添加、删除、查看好友）
 */
@Component
public class FriendComponent {
    private final StringRedisTemplate stringRedisTemplate;
    private final SetOperations<String, String> setOperations;
    private final UserComponent userComponent;
    // Key前缀
    private static final String FRIEND_PREFIX = "friend:";
    public FriendComponent(StringRedisTemplate stringRedisTemplate, UserComponent userComponent) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.setOperations = stringRedisTemplate.opsForSet();
        this.userComponent = userComponent;
    }
    /**
     * 添加好友（双向添加：A添加B，B也添加A）
     * @param userId 当前用户ID
     * @param friendId 好友用户ID
     * @return 添加成功返回true，失败返回false（用户不存在、已是好友）
     */
    public boolean addFriend(Long userId, Long friendId) {
        // 1. 校验用户是否存在
        if (userId.equals(friendId) || !isUserExists(userId) || !isUserExists(friendId)) {
            return false;
        }
        // 2. 校验是否已是好友
        String userFriendKey = FRIEND_PREFIX + userId;
        if (setOperations.isMember(userFriendKey, friendId.toString())) {
            return false;
        }
        // 3. 双向添加好友
        setOperations.add(userFriendKey, friendId.toString());
        setOperations.add(FRIEND_PREFIX + friendId, userId.toString());
        return true;
    }
    /**
     * 删除好友（双向删除：A删除B，B也删除A）
     * @param userId 当前用户ID
     * @param friendId 好友用户ID
     * @return 删除成功返回true，失败返回false（不是好友、用户不存在）
     */
    public boolean deleteFriend(Long userId, Long friendId) {
        if (userId.equals(friendId) || !isUserExists(userId) || !isUserExists(friendId)) {
            return false;
        }
        String userFriendKey = FRIEND_PREFIX + userId;
        if (!setOperations.isMember(userFriendKey, friendId.toString())) {
            return false;
        }
        // 双向删除
        setOperations.remove(userFriendKey, friendId.toString());
        setOperations.remove(FRIEND_PREFIX + friendId, userId.toString());
        return true;
    }
    /**
     * 查看好友列表（返回好友ID和用户名）
     * @param userId 当前用户ID
     * @return 好友列表（Map：key=好友ID，value=好友用户名）
     */
    public Set<Map<String, String>> getFriendList(Long userId) {
        String userFriendKey = FRIEND_PREFIX + userId;
        Set<String> friendIdSet = setOperations.members(userFriendKey);
        if (friendIdSet == null || friendIdSet.isEmpty()) {
            return Set.of();
        }
        // 遍历好友ID，获取用户名，封装返回
        return friendIdSet.stream()
                .map(friendIdStr -> {
                    Long friendId = Long.parseLong(friendIdStr);
                    Map<String, String> friendInfo = userComponent.getUserInfo(friendId);
                    Map<String, String> result = new java.util.HashMap<>();
                    result.put("friendId", friendIdStr);
                    result.put("username", friendInfo.get("username"));
                    return result;
                })
                .collect(Collectors.toSet());
    }
    /**
     * 校验是否是好友
     * @param userId 当前用户ID
     * @param friendId 目标用户ID
     * @return 是好友返回true，否则返回false
     */
    public boolean isFriend(Long userId, Long friendId) {
        if (!isUserExists(userId) || !isUserExists(friendId)) {
            return false;
        }
        String userFriendKey = FRIEND_PREFIX + userId;
        return setOperations.isMember(userFriendKey, friendId.toString());
    }
    // 私有工具方法：校验用户是否存在
    private boolean isUserExists(Long userId) {
        return stringRedisTemplate.hasKey(USER_PREFIX + userId);
    }
    // 引用UserComponent的USER_PREFIX，避免重复定义
    private static final String USER_PREFIX = "user:";
}
2.4 动态模块组件（发布、查看好友动态）
核心功能：发布动态（存储动态详情）、查看好友动态（汇总所有好友的动态，按时间倒序），依托Redis Sorted Set存储动态ID（按时间排序）、Hash存储动态详情。
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;
/**
 * 动态模块组件（发布、查看好友动态）
 */
@Component
public class MomentComponent {
    private final StringRedisTemplate stringRedisTemplate;
    private final HashOperations<String, String, String> hashOperations;
    private final ZSetOperations<String, String> zSetOperations;
    private final FriendComponent friendComponent;
    // Key前缀
    private static final String MOMENT_PREFIX = "moment:";
    private static final String USER_MOMENT_PREFIX = "moment:user:";
    public MomentComponent(StringRedisTemplate stringRedisTemplate, FriendComponent friendComponent) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.hashOperations = stringRedisTemplate.opsForHash();
        this.zSetOperations = stringRedisTemplate.opsForZSet();
        this.friendComponent = friendComponent;
    }
    /**
     * 发布动态
     * @param userId 发布者ID
     * @param content 动态内容
     * @return 发布成功返回momentId，失败返回null
     */
    public Long publishMoment(Long userId, String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        // 1. 生成唯一momentId（Redis自增）
        Long momentId = stringRedisTemplate.opsForValue().increment("moment:id:seq", 1);
        // 2. 存储动态详情（Hash类型）
        String momentKey = MOMENT_PREFIX + momentId;
        long publishTime = System.currentTimeMillis();
        Map<String, String> momentInfo = new HashMap<>();
        momentInfo.put("momentId", momentId.toString());
        momentInfo.put("userId", userId.toString());
        momentInfo.put("content", content);
        momentInfo.put("publishTime", String.valueOf(publishTime));
        momentInfo.put("likeCount", "0"); // 初始点赞数为0
        hashOperations.putAll(momentKey, momentInfo);
        // 3. 关联用户与动态（Sorted Set，按发布时间倒序）
        String userMomentKey = USER_MOMENT_PREFIX + userId;
        zSetOperations.add(userMomentKey, momentId.toString(), publishTime);
        // 4. 返回momentId
        return momentId;
    }
    /**
     * 查看个人动态（自己发布的动态）
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 动态列表（按发布时间倒序）
     */
    public List<Map<String, String>> getMyMoment(Long userId, int pageNum, int pageSize) {
        String userMomentKey = USER_MOMENT_PREFIX + userId;
        // 倒序查询（Score越大，发布时间越晚），分页处理
        long start = (pageNum - 1) * (long) pageSize;
        long end = pageNum * (long) pageSize - 1;
        Set<ZSetOperations.TypedTuple<String>> momentTuples = zSetOperations.reverseRangeWithScores(userMomentKey, start, end);
        if (momentTuples == null || momentTuples.isEmpty()) {
            return new ArrayList<>();
        }
        // 遍历动态ID，获取动态详情
        return momentTuples.stream()
                .map(tuple -> {
                    String momentId = tuple.getValue();
                    return hashOperations.entries(MOMENT_PREFIX + momentId);
                })
                .collect(Collectors.toList());
    }
    /**
     * 查看好友动态（朋友圈，汇总所有好友的动态）
     * @param userId 当前用户ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 好友动态列表（按发布时间倒序）
     */
    public List<Map<String, String>> getFriendMoment(Long userId, int pageNum, int pageSize) {
        // 1. 获取当前用户的所有好友ID
        Set<Map<String, String>> friendList = friendComponent.getFriendList(userId);
        if (friendList.isEmpty()) {
            return new ArrayList<>();
        }
        // 2. 汇总所有好友的动态ID（Sorted Set合并，按发布时间倒序）
        String unionKey = "moment:union:" + userId; // 临时合并Key
        List<String> friendMomentKeys = friendList.stream()
                .map(friend -> USER_MOMENT_PREFIX + friend.get("friendId"))
                .collect(Collectors.toList());
        // 合并所有好友的动态Key，存储到临时Key中
        zSetOperations.unionAndStore(unionKey, friendMomentKeys, unionKey);
        // 3. 分页查询合并后的动态（倒序）
        long start = (pageNum - 1) * (long) pageSize;
        long end = pageNum * (long) pageSize - 1;
        Set<ZSetOperations.TypedTuple<String>> momentTuples = zSetOperations.reverseRangeWithScores(unionKey, start, end);
        // 4. 删除临时Key，避免占用内存
        stringRedisTemplate.delete(unionKey);
        if (momentTuples == null || momentTuples.isEmpty()) {
            return new ArrayList<>();
        }
        // 5. 获取动态详情，返回结果
        return momentTuples.stream()
                .map(tuple -> {
                    String momentId = tuple.getValue();
                    return hashOperations.entries(MOMENT_PREFIX + momentId);
                })
                .collect(Collectors.toList());
    }
    /**
     * 删除动态
     * @param userId 发布者ID
     * @param momentId 动态ID
     * @return 删除成功返回true，失败返回false（不是自己的动态）
     */
    public boolean deleteMoment(Long userId, Long momentId) {
        String momentKey = MOMENT_PREFIX + momentId;
        // 校验动态是否存在，且发布者是当前用户
        if (!stringRedisTemplate.hasKey(momentKey) || !userId.toString().equals(hashOperations.get(momentKey, "userId"))) {
            return false;
        }
        // 1. 删除动态详情
        stringRedisTemplate.delete(momentKey);
        // 2. 删除用户与动态的关联
        String userMomentKey = USER_MOMENT_PREFIX + userId;
        zSetOperations.remove(userMomentKey, momentId.toString());
        // 3. 删除该动态的点赞和评论（后续互动组件关联）
        stringRedisTemplate.delete("like:" + momentId);
        stringRedisTemplate.delete("comment:" + momentId);
        return true;
    }
}
2.5 互动模块组件（点赞、评论）
核心功能：给动态点赞（去重）、取消点赞、评论动态、查看点赞/评论列表，依托Redis Set存储点赞用户（去重）、List存储评论（按时间顺序）。
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * 互动模块组件（点赞、评论）
 */
@Component
public class InteractionComponent {
    private final StringRedisTemplate stringRedisTemplate;
    private final SetOperations<String, String> setOperations;
    private final ListOperations<String, String> listOperations;
    private final UserComponent userComponent;
    // Key前缀
    private static final String LIKE_PREFIX = "like:";
    private static final String COMMENT_PREFIX = "comment:";
    private static final String MOMENT_PREFIX = "moment:";
    public InteractionComponent(StringRedisTemplate stringRedisTemplate, UserComponent userComponent) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.setOperations = stringRedisTemplate.opsForSet();
        this.listOperations = stringRedisTemplate.opsForList();
        this.userComponent = userComponent;
    }
    /**
     * 给动态点赞（去重，同一用户只能点赞一次）
     * @param userId 点赞用户ID
     * @param momentId 动态ID
     * @return 点赞成功返回true，失败返回false（动态不存在、已点赞）
     */
    public boolean likeMoment(Long userId, Long momentId) {
        String momentKey = MOMENT_PREFIX + momentId;
        if (!stringRedisTemplate.hasKey(momentKey)) {
            return false; // 动态不存在
        }
        String likeKey = LIKE_PREFIX + momentId;
        // 尝试添加点赞用户ID，Set自动去重，添加成功返回true
        Boolean success = setOperations.add(likeKey, userId.toString());
        if (Boolean.TRUE.equals(success)) {
            // 点赞成功，更新动态的点赞数（自增1）
            String momentLikeCountKey = MOMENT_PREFIX + momentId;
            stringRedisTemplate.opsForHash().increment(momentLikeCountKey, "likeCount", 1);
            return true;
        }
        return false; // 已点赞
    }
    /**
     * 取消给动态点赞
     * @param userId 点赞用户ID
     * @param momentId 动态ID
     * @return 取消成功返回true，失败返回false（未点赞、动态不存在）
     */
    public boolean cancelLikeMoment(Long userId, Long momentId) {
        String momentKey = MOMENT_PREFIX + momentId;
        if (!stringRedisTemplate.hasKey(momentKey)) {
            return false;
        }
        String likeKey = LIKE_PREFIX + momentId;
        // 尝试删除点赞用户ID，删除成功返回true
        Long removeCount = setOperations.remove(likeKey, userId.toString());
        if (removeCount != null && removeCount > 0) {
            // 取消点赞成功，更新动态的点赞数（自减1）
            String momentLikeCountKey = MOMENT_PREFIX + momentId;
            stringRedisTemplate.opsForHash().increment(momentLikeCountKey, "likeCount", -1);
            return true;
        }
        return false; // 未点赞
    }
    /**
     * 查看动态的点赞列表（返回点赞用户ID和用户名）
     * @param momentId 动态ID
     * @return 点赞列表
     */
    public List<Map<String, String>> getLikeList(Long momentId) {
        String likeKey = LIKE_PREFIX + momentId;
        Set<String> likeUserIdSet = setOperations.members(likeKey);
        if (likeUserIdSet == null || likeUserIdSet.isEmpty()) {
            return new ArrayList<>();
        }
        // 遍历点赞用户ID，获取用户名
        return likeUserIdSet.stream()
                .map(userIdStr -> {
                    Long userId = Long.parseLong(userIdStr);
                    Map<String, String> userInfo = userComponent.getUserInfo(userId);
                    Map<String, String&gt; likeInfo = new java.util.HashMap<>();
                    likeInfo.put("userId", userIdStr);
                    likeInfo.put("username", userInfo.get("username"));
                    return likeInfo;
                })
                .collect(Collectors.toList());
    }
    /**
     * 给动态评论
     * @param userId 评论用户ID
     * @param momentId 动态ID
     * @param content 评论内容
     * @return 评论成功返回true，失败返回false（动态不存在）
     */
    public boolean commentMoment(Long userId, Long momentId, String content) {
        String momentKey = MOMENT_PREFIX + momentId;
        if (!stringRedisTemplate.hasKey(momentKey) || content == null || content.isEmpty()) {
            return false;
        }
        String commentKey = COMMENT_PREFIX + momentId;
        // 评论信息：JSON格式（userId、username、content、commentTime）
        Map<String, String> userInfo = userComponent.getUserInfo(userId);
        String commentJson = String.format(
                "{\"userId\":\"%s\",\"username\":\"%s\",\"content\":\"%s\",\"commentTime\":\"%s\"}",
                userId.toString(),
                userInfo.get("username"),
                content,
                System.currentTimeMillis()
        );
        // 插入评论（List左侧插入，按时间倒序；右侧插入则按时间正序）
        listOperations.leftPush(commentKey, commentJson);
        return true;
    }
    /**
     * 查看动态的评论列表（按时间倒序）
     * @param momentId 动态ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 评论列表
     */
    public List<Map<String, String>> getCommentList(Long momentId, int pageNum, int pageSize) {
        String commentKey = COMMENT_PREFIX + momentId;
        // 分页查询（List左侧是最新评论，按倒序分页）
        long start = (pageNum - 1) * (long) pageSize;
        long end = pageNum * (long) pageSize - 1;
        List<String> commentJsonList = listOperations.range(commentKey, start, end);
        if (commentJsonList == null || commentJsonList.isEmpty()) {
            return new ArrayList<>();
        }
        // JSON反序列化为Map，返回评论详情
        return commentJsonList.stream()
                .map(json -> com.alibaba.fastjson.JSON.parseObject(json, Map.class))
                .collect(Collectors.toList());
    }
}
三、核心接口开发（简单示例）
基于上述组件，开发核心接口，供前端调用，接口采用RESTful风格，简化参数校验，聚焦功能实现，示例如下（Controller层）：
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * 简单社交网站核心接口Controller
 */
@RestController
@RequestMapping("/api/social")
public class SocialController {
    @Resource
    private UserComponent userComponent;
    @Resource
    private FriendComponent friendComponent;
    @Resource
    private MomentComponent momentComponent;
    @Resource
    private InteractionComponent interactionComponent;
    // ------------------------------ 用户接口 ------------------------------
    @PostMapping("/register")
    public Map<String, Object> register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String avatar,
            @RequestParam(required = false) String intro) {
        Long userId = userComponent.register(username, password, avatar, intro);
        if (userId == null) {
            return Map.of("code", 400, "msg", "用户名已存在", "data", null);
        }
        return Map.of("code", 200, "msg", "注册成功", "data", userId);
    }
    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestParam String username,
            @RequestParam String password) {
        String sessionId = userComponent.login(username, password);
        if (sessionId == null) {
            return Map.of("code", 400, "msg", "用户名或密码错误", "data", null);
        }
        return Map.of("code", 200, "msg", "登录成功", "data", sessionId);
    }
    @GetMapping("/user/info")
    public Map<String, Object> getUserInfo(
            @RequestHeader String sessionId) {
        Long userId = userComponent.validateSession(sessionId);
        if (userId == null) {
            return Map.of("code", 401, "msg", "未登录或会话过期", "data", null);
        }
        Map<String, String> userInfo = userComponent.getUserInfo(userId);
        return Map.of("code", 200, "msg", "success", "data", userInfo);
    }
    // ------------------------------ 好友接口 ------------------------------
    @PostMapping("/friend/add")
    public Map<String, Object> addFriend(
            @RequestHeader String sessionId,
            @RequestParam Long friendId) {
        Long userId = userComponent.validateSession(sessionId);
        if (userId == null) {
            return Map.of("code", 401, "msg", "未登录或会话过期", "data", null);
        }
        boolean success = friendComponent.addFriend(userId, friendId);
        return success ? Map.of("code", 200, "msg", "添加好友成功", "data", null)
                : Map.of("code", 400, "msg", "添加失败（用户不存在或已是好友）", "data", null);
    }
    @GetMapping("/friend/list")
    public Map<String, Object> getFriendList(
            @RequestHeader String sessionId) {
        Long userId = userComponent.validateSession(sessionId);
        if (userId == null) {
            return Map.of("code", 401, "msg", "未登录或会话过期", "data", null);
        }
        Set<Map<String, String>> friendList = friendComponent.getFriendList(userId);
        return Map.of("code", 200, "msg", "success", "data", friendList);
    }
    // ------------------------------ 动态接口 ------------------------------
    @PostMapping("/moment/publish")
    public Map<String, Object> publishMoment(
            @RequestHeader String sessionId,
            @RequestParam String content) {
        Long userId = userComponent.validateSession(sessionId);
        if (userId == null) {
            return Map.of("code", 401, "msg", "未登录或会话过期", "data", null);
        }
        Long momentId = momentComponent.publishMoment(userId, content);
        return Map.of("code", 200, "msg", "发布成功", "data", momentId);
    }
    @GetMapping("/moment/friend")
    public Map<String, Object> getFriendMoment(
            @RequestHeader String sessionId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = userComponent.validateSession(sessionId);
        if (userId == null) {
            return Map.of("code", 401, "msg", "未登录或会话过期", "data", null);
        }
        List<Map<String, String>> friendMoment = momentComponent.getFriendMoment(userId, pageNum, pageSize);
        return Map.of("code", 200, "msg", "success", "data", friendMoment);
    }
    // ------------------------------ 互动接口 ------------------------------
    @PostMapping("/moment/like")
    public Map<String, Object> likeMoment(
            @RequestHeader String sessionId,
            @RequestParam Long momentId) {
        Long userId = userComponent.validateSession(sessionId);
        if (userId == null) {
            return Map.of("code", 401, "msg", "未登录或会话过期", "data", null);
        }
        boolean success = interactionComponent.likeMoment(userId, momentId);
        return success ? Map.of("code", 200, "msg", "点赞成功", "data", null)
                : Map.of("code", 400, "msg", "点赞失败（已点赞或动态不存在）", "data", null);
    }
    @PostMapping("/moment/comment")
    public Map<String, Object> commentMoment(
            @RequestHeader String sessionId,
            @RequestParam Long momentId,
            @RequestParam String content) {
        Long userId = userComponent.validateSession(sessionId);
        if (userId == null) {
            return Map.of("code", 401, "msg", "未登录或会话过期", "data", null);
        }
        boolean success = interactionComponent.commentMoment(userId, momentId, content);
        return success ? Map.of("code", 200, "msg", "评论成功", "data", null)
                : Map.of("code", 400, "msg", "评论失败（动态不存在或内容为空）", "data", null);
    }
}
四、部署与测试
4.1 部署步骤（极简版）
部署Redis：本地部署Redis（默认端口6379），无需额外配置，确保Redis服务正常运行；
启动后端服务：将上述代码整合到Spring Boot项目中，启动项目（默认端口8080）；
前端测试：使用Postman、Apifox等工具调用接口，测试核心功能（注册、登录、添加好友、发布动态等）；
扩展前端：开发简单前端页面（HTML+JS），调用后端接口，实现完整的社交网站交互。
4.2 测试用例（简单示例）
注册：调用POST /api/social/register，传入username、password，返回userId，说明注册成功；
登录：调用POST /api/social/login，传入注册的username、password，返回sessionId，说明登录成功；
添加好友：使用sessionId作为请求头，调用POST /api/social/friend/add，传入另一个用户的userId，返回添加成功；
发布动态：调用POST /api/social/moment/publish，传入动态内容，返回momentId，说明发布成功；
查看好友动态：调用GET /api/social/moment/friend，返回好友发布的动态列表，说明功能正常；
点赞/评论：调用对应的点赞、评论接口，查看动态的点赞数、评论列表，验证功能正常。
五、注意事项与扩展方向
5.1 注意事项（简化版，适合小型社交网站）
数据持久化：开启Redis的RDB或AOF持久化，避免Redis重启后数据丢失；
会话安全：sessionId建议通过HTTPS传输，避免明文泄露；可添加签名机制，防止伪造；
性能优化：对于好友动态查询，若好友数量多，可缓存好友动态列表，减少Redis合并操作；
数据量控制：本设计适合小型社交网站（用户数1万以内），数据量过大时，需拆分Redis Key，或引入数据库存储核心数据。
5.2 扩展方向（按需扩展）
私信功能：基于Redis List实现简单私信（一对一聊天）；
动态转发：新增转发功能，依托Redis Set存储转发关系；
热搜功能：基于Redis Sorted Set实现动态关键词热搜；
用户关注：扩展好友模块，实现“关注-粉丝”关系（双向/单向）；
数据持久化：引入MySQL数据库，存储用户核心数据，Redis作为缓存，提升性能。
六、总结
本文构建的简单社交网站，依托Redis实现了核心社交功能，架构轻量、代码简洁、易部署，适合入门实战和小型社交场景。核心亮点是利用Redis不同数据结构的优势，实现高效的数据存储和查询，避免引入复杂中间件，降低开发和部署成本。
通过本文的实战代码，可快速搭建一个可运行的简单社交网站，后续可根据业务需求，逐步扩展更多功能，同时优化性能和安全性，适配更大规模的用户场景。

