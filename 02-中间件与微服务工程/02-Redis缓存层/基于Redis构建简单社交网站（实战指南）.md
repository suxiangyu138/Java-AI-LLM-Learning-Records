# 基于 Redis 构建简单社交网站（实战指南）

> **定位**：依托 Redis 高性能和丰富数据结构，构建轻量级社交网站核心功能（用户/好友/动态/点赞评论），无需数据库。

---

## 目录

1. [架构与数据结构设计](#1-架构与数据结构设计)
2. [核心组件实现](#2-核心组件实现)
3. [接口开发](#3-接口开发)
4. [部署测试与扩展](#4-部署测试与扩展)

---

## 1. 架构与数据结构设计

### Redis Key 设计

| 功能 | 数据结构 | Key 格式 | 说明 |
|------|----------|----------|------|
| 用户信息 | Hash | `user:userId` | 用户名/密码/头像/简介 |
| 登录态 | String | `session:sessionId` | sessionId→userId，过期 2h |
| 好友关系 | Set | `friend:userId` | 双向添加 |
| 动态列表 | ZSet | `moment:userId` | Score=发布时间 |
| 动态详情 | Hash | `moment:momentId` | 内容/时间/发布者 |
| 点赞 | Set | `like:momentId` | 点赞 userId（去重） |
| 评论 | List | `comment:momentId` | JSON 格式，时间序 |

---

## 2. 核心组件实现

### 用户组件关键方法

```java
// 注册：自增 ID + MD5 密码 → Hash 存储
Long userId = stringRedisTemplate.opsForValue().increment("user:id:seq", 1);
hashOperations.putAll("user:" + userId, userInfo);

// 登录：UUID sessionId → String 存储，过期 2h
String sessionId = UUID.randomUUID().toString().replace("-", "");
stringRedisTemplate.opsForValue().set("session:" + sessionId, userId.toString(), 7200, TimeUnit.SECONDS);
```

### 好友组件关键方法

```java
// 双向添加
setOperations.add("friend:" + userId, friendId.toString());
setOperations.add("friend:" + friendId, userId.toString());

// 查看好友列表 → 遍历好友 ID 获取用户名
Set<String> friendIdSet = setOperations.members("friend:" + userId);
```

### 动态组件关键方法

```java
// 发布：自增 ID → Hash 存详情 + ZSet 存用户关联
Long momentId = stringRedisTemplate.opsForValue().increment("moment:id:seq", 1);
zSetOperations.add("moment:user:" + userId, momentId.toString(), System.currentTimeMillis());

// 好友动态：合并所有好友的 ZSet → 按时间倒序
zSetOperations.unionAndStore(unionKey, friendMomentKeys, unionKey);
```

### 互动组件关键方法

```java
// 点赞：Set 去重 + 原子递增点赞数
setOperations.add("like:" + momentId, userId.toString());
hashOperations.increment("moment:" + momentId, "likeCount", 1);

// 评论：List 左插 JSON（按时间倒序）
listOperations.leftPush("comment:" + momentId, commentJson);
```

---

## 3. 接口开发

```java
@RestController
@RequestMapping("/api/social")
public class SocialController {
    @PostMapping("/register")  // 注册
    @PostMapping("/login")     // 登录 → 返回 sessionId
    @PostMapping("/friend/add")    // 添加好友
    @GetMapping("/friend/list")    // 好友列表
    @PostMapping("/moment/publish") // 发布动态
    @GetMapping("/moment/friend")  // 好友朋友圈
    @PostMapping("/moment/like")   // 点赞
    @PostMapping("/moment/comment") // 评论
}
```

---

## 4. 部署测试与扩展

### 扩展方向

| 功能 | 实现思路 |
|------|----------|
| 私信 | List 实现一对一聊天 |
| 转发 | Set 存储转发关系 |
| 热搜 | ZSet 实现关键词热搜 |
| 关注粉丝 | 双向/单向关系 |

> ⚠️ 本设计适合小型社交网站（用户 < 1 万），数据量过大需引入 MySQL 存储核心数据。
