# QQ Chat - 企业级消息交互平台（命令行终端版）

> 基于 Java Socket 的企业级即时通讯平台，类似 QQ，支持私聊、群聊、好友管理、离线消息等功能。

## 目录

- [系统架构](#系统架构)
- [功能特性](#功能特性)
- [快速开始](#快速开始)
- [客户端命令](#客户端命令)
- [通信协议](#通信协议)
- [项目结构](#项目结构)
- [数据存储](#数据存储)
- [并发模型](#并发模型)
- [设计决策](#设计决策)

---

## 系统架构

```
┌─────────────────┐         TCP (端口9999)         ┌─────────────────┐
│   QQClient-1    │◄──────────────────────────────►│                 │
│   (终端客户端)   │                                │    QQServer     │
└─────────────────┘                                │                 │
                                                   │  ┌───────────┐  │
┌─────────────────┐                                │  │ ThreadPool│  │
│   QQClient-2    │◄──────────────────────────────►│  │  (50线程)  │  │
│   (终端客户端)   │                                │  └───────────┘  │
└─────────────────┘                                │                 │
                                                   │  ┌───────────┐  │
       ...                                         │  │ Session   │  │
                                                   │  │ Registry  │  │
┌─────────────────┐                                │  └───────────┘  │
│   QQClient-N    │◄──────────────────────────────►│                 │
│   (终端客户端)   │                                │  ┌───────────┐  │
└─────────────────┘                                │  │ Heartbeat │  │
                                                   │  │ Monitor   │  │
                                                   │  └───────────┘  │
                                                   │                 │
                                                   │   data/ (JSON)  │
                                                   │   ├─ users.json │
                                                   │   ├─ groups.json│
                                                   │   └─ messages/  │
                                                   └─────────────────┘
```

**核心设计：**
- **C/S 架构**：基于 TCP 长连接，客户端与服务端保持持久连接
- **多线程**：服务端使用固定线程池（50线程）处理并发连接，每个客户端独立一个 ClientHandler
- **协议**：自定义文本协议，基于行分隔 + Base64 编码消息体
- **存储**：纯 JSON 文件存储，零外部依赖，支持原子写入

---

## 功能特性

| 功能 | 描述 |
|------|------|
| 用户注册/登录 | 密码 SHA-256 + Salt 哈希存储 |
| 好友管理 | 添加/删除好友，双向好友关系 |
| 私聊 | 实时 P2P 消息，单条发送或连续聊天模式 |
| 群聊 | 创建/加入/离开群组，群消息广播 |
| 在线状态 | 好友上线/下线实时通知 |
| 离线消息 | 用户离线时消息暂存，上线后自动投递 |
| 心跳检测 | 客户端每20秒发PING，服务端90秒超时踢下线 |
| 消息持久化 | 私聊和群聊消息历史存储到 JSON 文件 |
| 用户搜索 | 按用户名/昵称模糊搜索 |

---

## 快速开始

### 环境要求

- **JDK 21+** (使用了虚拟线程无关特性，21以上均可运行)
- 操作系统：Windows / Linux / macOS
- 零外部依赖，纯 Java 标准库

### 编译

```bash
# 进入项目根目录
cd 类QQ通信平台

# 编译所有源文件
javac -encoding UTF-8 -d out -sourcepath src src/com/qqchat/server/QQServer.java src/com/qqchat/client/QQClient.java
```

### 启动服务端

```bash
# 默认端口 9999，数据目录 data/
java -cp out com.qqchat.server.QQServer

# 指定端口
java -cp out com.qqchat.server.QQServer 8888

# 指定端口和数据目录
java -cp out com.qqchat.server.QQServer 8888 my_data
```

### 启动客户端

```bash
# 连接本地服务器（默认 localhost:9999）
java -cp out com.qqchat.client.QQClient

# 连接指定服务器
java -cp out com.qqchat.client.QQClient 192.168.1.100 8888
```

### 多客户端测试

打开多个终端窗口，分别启动客户端即可测试多人在线聊天。

---

## 客户端命令

### 认证
| 命令 | 说明 |
|------|------|
| `/login <username> <password>` | 登录 |
| `/register <username> <pwd> <nick>` | 注册新账号 |
| `/quit` | 退出客户端 |

### 好友管理
| 命令 | 说明 |
|------|------|
| `/addfriend <username>` | 添加好友 |
| `/delfriend <username>` | 删除好友 |
| `/friends` | 查看好友列表及在线状态 |

### 私聊
| 命令 | 说明 |
|------|------|
| `/msg <username> <message>` | 发送单条消息 |
| `/chat <username>` | 进入聊天模式（直接输入文本发消息） |
| `/exit` | 退出聊天模式 |

### 群组管理
| 命令 | 说明 |
|------|------|
| `/creategroup <groupName>` | 创建群组 |
| `/joingroup <groupId>` | 加入群组 |
| `/leavegroup <groupId>` | 离开群组 |
| `/groups` | 查看我的群组列表 |
| `/groupinfo <groupId>` | 查看群组详情和成员 |

### 群聊
| 命令 | 说明 |
|------|------|
| `/gmsg <groupId> <message>` | 发送群消息 |
| `/gchat <groupId>` | 进入群聊天模式 |

### 个人中心
| 命令 | 说明 |
|------|------|
| `/profile` | 查看个人资料 |
| `/setnickname <newNickname>` | 修改昵称 |
| `/search <query>` | 搜索用户 |

### 其他
| 命令 | 说明 |
|------|------|
| `/help` | 显示帮助信息 |

---

## 通信协议

### 传输层

- **传输方式**：TCP 长连接
- **编码**：UTF-8
- **帧格式**：每行一个消息，`\n` 分割
- **消息体编码**：Base64（解决消息内容可能包含 `\n` 的问题）

### 帧格式

```
COMMAND|senderId|targetId|timestamp|payloadBase64\n
```

字段以 `|` 分隔，共5个字段。

**示例：**
```
# Alice 给 Bob 发消息
SEND_MSG|alice|bob|1715692800000|SGVsbG8gQm9iIQ==

# 服务端返回成功
AUTH_OK|SERVER|alice|1715692800001|TG9naW4gc3VjY2Vzc2Z1bA==
```

### 命令集

#### 认证
| Command | 方向 | 说明 |
|---------|------|------|
| REGISTER | C→S | 注册，payload = `username\|password\|nickname` |
| LOGIN | C→S | 登录，payload = `username\|password` |
| LOGOUT | C→S | 登出 |
| AUTH_OK | S→C | 认证成功 |
| AUTH_FAIL | S→C | 认证失败 |

#### 好友
| Command | 方向 | 说明 |
|---------|------|------|
| ADD_FRIEND | C→S | 添加好友 |
| DEL_FRIEND | C→S | 删除好友 |
| FRIEND_LIST | C→S | 请求好友列表 |
| FRIEND_LIST_RESP | S→C | 好友列表响应 |
| FRIEND_REQUEST | S→C | 被添加好友通知 |
| FRIEND_ONLINE | S→C | 好友上线通知 |
| FRIEND_OFFLINE | S→C | 好友下线通知 |

#### 消息
| Command | 方向 | 说明 |
|---------|------|------|
| SEND_MSG | C→S | 发私聊消息 |
| RECV_MSG | S→C | 接收私聊消息 |
| MSG_DELIVERED | S→C | 消息已送达确认 |
| OFFLINE_MSGS | S→C | 离线消息批量投递 |

#### 群组
| Command | 方向 | 说明 |
|---------|------|------|
| CREATE_GROUP | C→S | 创建群组 |
| JOIN_GROUP | C→S | 加入群组 |
| LEAVE_GROUP | C→S | 离开群组 |
| GROUP_LIST | C→S | 群组列表 |
| GROUP_LIST_RESP | S→C | 群组列表响应 |
| GROUP_INFO | C→S | 群组详情 |
| GROUP_INFO_RESP | S→C | 群组详情响应 |
| SEND_GROUP_MSG | C→S | 发群消息 |
| RECV_GROUP_MSG | S→C | 接收群消息 |

#### 系统
| Command | 方向 | 说明 |
|---------|------|------|
| PING | C→S | 心跳 |
| PONG | S→C | 心跳响应 |
| ERROR | S→C | 错误 |
| SERVER_SHUTDOWN | S→C | 服务端关闭通知 |

---

## 项目结构

```
类QQ通信平台/
├── src/com/qqchat/
│   ├── protocol/           # 通信协议层
│   │   ├── Command.java    # 协议命令枚举 (30+ commands)
│   │   ├── ProtocolFrame.java  # 消息帧 (序列化/反序列化)
│   │   └── ProtocolParser.java # 消息解析器
│   │
│   ├── model/              # 数据模型
│   │   ├── User.java       # 用户实体
│   │   ├── Message.java    # 消息实体
│   │   ├── Group.java      # 群组实体
│   │   ├── FriendRelation.java # 好友关系
│   │   ├── MessageType.java    # 消息类型枚举
│   │   ├── MessageStatus.java  # 消息状态枚举
│   │   └── FriendStatus.java   # 好友状态枚举
│   │
│   ├── server/             # 服务端
│   │   ├── QQServer.java   # 服务端入口
│   │   ├── ClientHandler.java  # 客户端连接处理器
│   │   ├── SessionRegistry.java # 在线会话注册表
│   │   └── HeartbeatMonitor.java # 心跳检测器
│   │
│   ├── client/             # 客户端
│   │   ├── QQClient.java   # 客户端入口 + 命令处理
│   │   ├── ServerListener.java # 服务端消息监听线程
│   │   └── ChatSession.java    # 客户端会话状态
│   │
│   ├── service/            # 业务逻辑层
│   │   ├── UserService.java    # 用户服务
│   │   ├── FriendService.java  # 好友服务
│   │   ├── GroupService.java   # 群组服务
│   │   └── ChatService.java    # 聊天服务
│   │
│   ├── persistence/        # 持久化层
│   │   ├── JsonFileStore.java  # JSON 文件读写
│   │   ├── MessageStore.java   # 消息存储
│   │   └── JsonSerializer.java # JSON 序列化/反序列化
│   │
│   └── util/               # 工具类
│       ├── PasswordHasher.java # 密码哈希 (SHA-256 + Salt)
│       ├── IdGenerator.java    # ID 生成器
│       └── TimestampUtil.java  # 时间戳工具
│
├── data/                   # 运行时数据 (自动创建)
│   ├── users.json          # 用户数据
│   ├── groups.json         # 群组数据
│   ├── friend_relations.json # 好友关系
│   └── messages/           # 消息历史
│       ├── {user_user}_private.json
│       └── {groupId}_group.json
│
├── out/                    # 编译输出 (自动创建)
└── README.md               # 本文档
```

**共 24 个 Java 源文件**，按分层架构组织。

---

## 数据存储

### 存储格式

纯 JSON 文件存储，无外部数据库依赖。

### users.json

```json
{
  "alice": {
    "userId": "alice",
    "username": "alice",
    "passwordHash": "a1b2c3:sha256hash...",
    "nickname": "Alice",
    "friends": ["bob"],
    "groups": ["g_1234567890_12345"],
    "createdAt": 1715692800000,
    "lastSeen": 1715692800000,
    "online": true
  }
}
```

### groups.json

```json
{
  "g_1715692800000_12345": {
    "groupId": "g_1715692800000_12345",
    "name": "技术交流群",
    "ownerId": "alice",
    "members": ["alice", "bob"],
    "createdAt": 1715692800000
  }
}
```

### 原子写入

所有文件写入采用 **temp-file-then-rename** 策略，防止进程崩溃时文件损坏：
```java
writeToTempFile() → Files.move(tmp, target, ATOMIC_MOVE)
```

---

## 并发模型

### 服务端

```
ServerSocket.accept() [主线程]
    │
    ▼
ExecutorService (50线程)
    │
    ├── ClientHandler-1 [线程]
    ├── ClientHandler-2 [线程]
    ├── ...
    └── ClientHandler-N [线程]
    
HeartbeatMonitor [守护线程]
    └── 每30秒检查心跳，90秒超时断开
```

- **SessionRegistry**：`ConcurrentHashMap`，线程安全
- **JsonFileStore**：方法级 `synchronized`，按文件名锁
- **ChatService**：synchronized 方法，保证离线消息队列一致性

### 客户端

```
主线程: 命令输入循环 (阻塞 System.in)
    │
    ├── ServerListener [守护线程]
    │   └── 阻塞读取服务端消息，实时显示
    │
    └── Pinger [守护线程]
        └── 每20秒发送 PING 心跳
```

---

## 设计决策

| 决策 | 原因 |
|------|------|
| 零外部依赖 | 纯 Java 标准库，无需 Maven/Gradle，`javac` + `java` 直接运行 |
| 行分隔协议 | 简单可靠，便于调试（可 telnet 测试） |
| Base64 消息体 | 消息内容可能含换行符，Base64 编码后为单行 |
| 固定线程池(50) | 适合学习/演示场景的生产者-消费者模型 |
| Thread-per-client | 阻塞 I/O 模型，实现简单，代码可读性好 |
| JSON 文件存储 | 无数据库依赖，数据可读可审计 |
| SHA-256 + Salt | 密码安全存储，不在网络传输中暴露哈希 |
| 双向好友关系 | 与 QQ 一致，A 加 B 则双方互为好友 |
| 群主离开自动转让 | 群主离开时将群主转让给第一个成员，无成员时删除群 |

---

## 版本

v1.0.0 - 命令行终端版

## License

MIT
