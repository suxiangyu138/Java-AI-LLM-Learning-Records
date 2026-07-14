# Socket状态及相关注意事项

## 📑 目录

- [一、Socket核心状态分类（基于TCP协议）](#一socket核心状态分类基于tcp协议)
- [二、Socket状态切换核心逻辑（TCP）](#二socket状态切换核心逻辑tcp)
- [三、关键注意事项](#三关键注意事项)

---

Socket（套接字）作为网络通信的核心端点，其状态会随着网络操作（创建、连接、数据收发、关闭）的执行发生动态变化。不同状态对应不同的操作权限，正确理解和把控Socket状态，是避免通信异常、资源泄漏的关键。

---

## 一、Socket核心状态分类（基于TCP协议）

> 由于TCP协议是面向连接的，其Socket状态变化最完整、最具代表性；UDP协议无连接，Socket状态相对简单（仅存在"未使用""已创建""已关闭"三种基础状态）。

### （一）初始状态

| 状态 | 说明 |
|------|------|
| **CLOSED（关闭状态）** | Socket的初始状态，套接字句柄未创建，无任何资源占用 |

### （二）创建与监听状态（服务器端为主）

| 状态 | 说明 |
|------|------|
| **SYN_SENT（同步已发送）** | 仅客户端特有。调用 `connect()` 发送SYN报文后进入该状态，等待服务器的SYN+ACK响应 |
| **LISTEN（监听状态）** | 仅服务器端特有。调用 `listen()` 后进入该状态，等待客户端的连接请求 |
| **SYN_RCVD（同步已接收）** | 仅服务器端特有。收到SYN后发送SYN+ACK，进入该状态，等待客户端的ACK确认 |

### （三）连接建立状态（核心通信状态）

| 状态 | 说明 |
|------|------|
| **ESTABLISHED（已建立连接）** | TCP三次握手完成后，双方进入该状态，可正常调用 `send()`/`recv()` 进行数据收发 |

### （四）关闭状态（连接终止）

| 状态 | 说明 |
|------|------|
| **FIN_WAIT_1（终止等待1）** | 主动关闭方发送FIN报文后进入，等待对方的ACK响应 |
| **FIN_WAIT_2（终止等待2）** | 主动关闭方收到ACK响应后进入，等待对方发送FIN报文 |
| **CLOSE_WAIT（关闭等待）** | 被动关闭方收到FIN报文后发送ACK，进入该状态，可继续发送未完成的数据 |
| **LAST_ACK（最后确认）** | 被动关闭方发送FIN报文后进入，等待主动关闭方的ACK响应 |
| **TIME_WAIT（时间等待）** | 主动关闭方收到FIN并发送ACK后进入，默认等待2MSL（约1-4分钟），超时后自动回归CLOSED状态 |

---

## 二、Socket状态切换核心逻辑（TCP）

```
初始状态：
  CLOSED → 客户端调用connect() → SYN_SENT

服务器端：
  CLOSED → socket()/bind() → LISTEN → 收到SYN → SYN_RCVD → 收到ACK → ESTABLISHED

客户端：
  SYN_SENT → 收到SYN+ACK → 发送ACK → ESTABLISHED

关闭连接（主动关闭方）：
  ESTABLISHED → send FIN → FIN_WAIT_1 → 收到ACK → FIN_WAIT_2 → 收到FIN → 发送ACK → TIME_WAIT → 超时 → CLOSED

关闭连接（被动关闭方）：
  ESTABLISHED → 收到FIN → 发送ACK → CLOSE_WAIT → 发送FIN → LAST_ACK → 收到ACK → CLOSED
```

---

## 三、关键注意事项

### 3.1 状态与操作匹配

不同状态下的Socket仅支持特定操作：

- **LISTEN** 状态的Socket无法直接收发数据，需通过 `accept()` 获取新的ESTABLISHED状态Socket。
- **CLOSED** 状态的Socket无法调用 `send()`/`recv()`。

### 3.2 避免TIME_WAIT状态异常

TIME_WAIT是正常的协议机制，但大量TIME_WAIT状态的Socket会占用端口资源。可通过设置 `SO_REUSEADDR` 选项，允许端口快速复用。

> **注意**：需谨慎使用 `SO_REUSEADDR`，避免端口冲突。

### 3.3 正确关闭Socket

- 通信完成后需主动调用 `closesocket()` 关闭Socket。
- 服务器端需关闭所有通过 `accept()` 获取的客户端Socket。

### 3.4 异常状态处理

网络中断、超时等情况会导致Socket状态异常（如SYN_SENT超时、ESTABLISHED状态突然中断），需在程序中添加异常检测，及时关闭异常Socket。

### 3.5 UDP Socket状态注意

UDP无连接，Socket仅存在CLOSED、已创建（未绑定/已绑定）、已关闭三种状态，无需三次握手和四次挥手，调用 `closesocket()` 后直接回归CLOSED状态。

### 3.6 多线程场景下的状态管理

多线程编程中，多个线程可能操作同一个Socket，需做好线程同步，避免同时对Socket执行关闭、收发等操作。

### 3.7 状态与网络应用模式的关联

| 模式 | Socket状态特点 |
|------|--------------|
| C/S模式 | 服务器端Socket长期处于LISTEN状态等待连接，客户端Socket通过状态切换完成连接与通信 |
| B/S模式 | 浏览器的Socket与Web服务器的Socket通过ESTABLISHED状态完成HTTP请求/响应的传输 |
| P2P模式 | 每个节点的Socket在已创建状态下直接建立连接，切换至ESTABLISHED状态进行数据交互 |

---

## 📖 相关阅读

- [TCPIP协议服务](./TCPIP协议服务.md)
- [TCPIP协议首部（后端实战版，含WinSock调试关联）](./TCPIP协议首部（后端实战版，含WinSock调试关联）.md)
- [Socket信息与控制（Winsock后端开发实战）](./Socket信息与控制（Winsock后端开发实战）.md)
- [Socket信息与控制 支持例程（Winsock后端实战）](./Socket信息与控制%20支持例程（Winsock后端实战）.md)
