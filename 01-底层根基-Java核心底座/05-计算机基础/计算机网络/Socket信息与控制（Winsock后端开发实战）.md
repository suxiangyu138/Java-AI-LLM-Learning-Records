# Socket信息与控制（Winsock后端开发实战）

## 📑 目录

- [一、核心概念：Socket信息与控制的后端价值](#一核心概念socket信息与控制的后端价值)
- [二、Socket信息获取（后端常用，实战优先）](#二socket信息获取后端常用实战优先)
- [三、Socket控制（后端核心，资源与性能管控）](#三socket控制后端核心资源与性能管控)
- [四、后端开发注意事项](#四后端开发注意事项)
- [五、与前文内容的关联](#五与前文内容的关联)

---

## 一、核心概念：Socket信息与控制的后端价值

对后端开发而言，Socket不仅是通信端点，更是需要精准管理的"资源"：

| 类别 | 核心内容 | 后端价值 |
|------|---------|---------|
| **Socket信息** | 获取Socket的当前状态和配置（本地IP/端口、远端IP/端口、选项配置、连接状态） | 日志监控、问题排查 |
| **Socket控制** | 主动调整Socket的配置和状态（设置选项、关闭Socket、终止连接） | 优化性能、释放资源、处理异常 |

---

## 二、Socket信息获取（后端常用，实战优先）

### （一）获取Socket绑定/连接的地址信息

#### 1. getsockname（获取本地绑定的IP/端口）

**函数原型**：`int getsockname(SOCKET s, struct sockaddr* name, int* namelen);`

**核心作用**：获取指定Socket绑定的本地主机地址（IP+端口）。

**后端实战实例**：

```c
#include <winsock2.h>
#include <stdio.h>
#pragma comment(lib, "ws2_32.lib")

#define PORT 8080

int main() {
    WSADATA wsaData;
    WSAStartup(MAKEWORD(2, 2), &wsaData);

    SOCKET serverSock = socket(AF_INET, SOCK_STREAM, 0);
    struct sockaddr_in serverAddr;
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(PORT);
    serverAddr.sin_addr.s_addr = htonl(INADDR_ANY);
    bind(serverSock, (struct sockaddr*)&serverAddr, sizeof(serverAddr));

    // 获取本地绑定的地址信息
    struct sockaddr_in localAddr;
    int addrLen = sizeof(localAddr);
    int ret = getsockname(serverSock, (struct sockaddr*)&localAddr, &addrLen);
    if (ret == SOCKET_ERROR) {
        printf("获取本地绑定地址失败，错误码：%d\n", WSAGetLastError());
        closesocket(serverSock);
        WSACleanup();
        return 1;
    }

    printf("服务器本地绑定信息：\n");
    printf("IP地址：%s\n", inet_ntoa(localAddr.sin_addr));
    printf("端口号：%d\n", ntohs(localAddr.sin_port));

    closesocket(serverSock);
    WSACleanup();
    return 0;
}
```

#### 2. getpeername（获取远端连接的IP/端口）

**函数原型**：`int getpeername(SOCKET s, struct sockaddr* name, int* namelen);`

**核心作用**：获取与当前Socket建立连接的远端设备地址，仅适用于已建立连接的Socket。

```c
// 假设已完成服务器初始化、绑定、监听
SOCKET clientSock = accept(serverSock, NULL, NULL);
if (clientSock == INVALID_SOCKET) {
    printf("接收连接失败，错误码：%d\n", WSAGetLastError());
    return 1;
}

struct sockaddr_in clientAddr;
int addrLen = sizeof(clientAddr);
int ret = getpeername(clientSock, (struct sockaddr*)&clientAddr, &addrLen);
if (ret == SOCKET_ERROR) {
    printf("获取客户端地址失败，错误码：%d\n", WSAGetLastError());
    closesocket(clientSock);
    return 1;
}

printf("客户端连接信息：\n");
printf("客户端IP：%s\n", inet_ntoa(clientAddr.sin_addr));
printf("客户端端口：%d\n", ntohs(clientAddr.sin_port));
```

### （二）获取Socket选项信息

**函数原型**：`int getsockopt(SOCKET s, int level, int optname, char* optval, int* optlen);`

**参数说明**：

| 参数 | 说明 |
|------|------|
| `level` | 选项级别：`SOL_SOCKET`（通用选项）、`IPPROTO_TCP`（TCP选项） |
| `optname` | 选项名称：`SO_REUSEADDR`、`SO_RCVBUF`、`SO_SNDBUF` |
| `optval` | 输出参数，存储获取到的选项值 |
| `optlen` | 输入/输出参数 |

```c
int optval;
int optlen = sizeof(optval);

// 获取端口复用选项
getsockopt(serverSock, SOL_SOCKET, SO_REUSEADDR, (char*)&optval, &optlen);
printf("端口复用选项：%s\n", optval ? "开启" : "关闭");

// 获取接收缓冲区大小
getsockopt(serverSock, SOL_SOCKET, SO_RCVBUF, (char*)&optval, &optlen);
printf("接收缓冲区大小：%d 字节\n", optval);

// 获取发送缓冲区大小
getsockopt(serverSock, SOL_SOCKET, SO_SNDBUF, (char*)&optval, &optlen);
printf("发送缓冲区大小：%d 字节\n", optval);
```

### （三）补充：获取Socket状态信息

```c
int optval;
int optlen = sizeof(optval);
getsockopt(clientSock, SOL_SOCKET, SO_ERROR, (char*)&optval, &optlen);
if (optval == 0) {
    printf("Socket连接正常（ESTABLISHED状态）\n");
} else {
    printf("Socket连接异常，错误码：%d\n", optval);
}
```

---

## 三、Socket控制（后端核心，资源与性能管控）

### （一）设置Socket选项（setsockopt，后端优化核心）

#### 1. 端口复用（SO_REUSEADDR，后端必设）

```c
SOCKET serverSock = socket(AF_INET, SOCK_STREAM, 0);
int optval = 1;
setsockopt(serverSock, SOL_SOCKET, SO_REUSEADDR, (char*)&optval, sizeof(optval));
// 后续执行bind()操作
```

> **核心作用**：解决服务器重启时"端口被TIME_WAIT状态占用"（错误码10048）的问题，必须在 `bind()` 之前调用。

#### 2. 调整接收/发送缓冲区大小

```c
int bufSize = 8 * 1024; // 8KB
setsockopt(serverSock, SOL_SOCKET, SO_RCVBUF, (char*)&bufSize, sizeof(bufSize));
setsockopt(serverSock, SOL_SOCKET, SO_SNDBUF, (char*)&bufSize, sizeof(bufSize));
```

#### 3. 设置Socket为非阻塞模式（FIONBIO，高并发必备）

```c
u_long mode = 1; // 1=非阻塞，0=阻塞
ioctlsocket(serverSock, FIONBIO, &mode);
```

> **注意**：非阻塞模式下，send()、recv()、accept()等函数会立即返回，需通过 `WSAGetLastError()` 判断是否为"资源暂时不可用"（错误码10035）。

### （二）关闭Socket与连接（资源释放核心）

#### 1. shutdown（关闭连接，可选）

```c
// SD_RECEIVE：关闭接收功能
// SD_SEND：关闭发送功能
// SD_BOTH：同时关闭接收和发送功能（后端最常用）
shutdown(clientSock, SD_BOTH);
Sleep(100); // 等待数据发送/接收完毕
closesocket(clientSock);
```

#### 2. closesocket（关闭Socket，必用）

```c
int closesocket(SOCKET s);
```

> **后端注意事项**：
> - `closesocket()` 会自动触发TCP四次挥手。
> - 建议先调用 `shutdown()` 再调用 `closesocket()`。
> - 多线程场景下需确保同一Socket不被多个线程同时关闭。

### （三）补充：强制终止Socket连接（后端异常处理）

```c
struct linger ling;
ling.l_onoff = 1;  // 开启强制关闭
ling.l_linger = 0;  // 超时0秒，立即关闭
setsockopt(clientSock, SOL_SOCKET, SO_LINGER, (char*)&ling, sizeof(ling));
closesocket(clientSock);
```

> **注意**：强制关闭会丢失未发送完成的数据，仅适用于异常场景（如客户端超时未响应）。

---

## 四、后端开发注意事项

1. **信息获取函数的前提条件**
   - `getsockname`：必须在 `bind()` 后调用。
   - `getpeername`：必须在 `connect()`/`accept()` 后调用。
   - `getsockopt`：可在Socket创建后任意阶段调用。

2. **选项设置的顺序要求**
   - `SO_REUSEADDR`、`SO_LINGER` 需在 `bind()`、`connect()` 之前设置。
   - 非阻塞模式（FIONBIO）可在Socket创建后任意阶段设置。

3. **资源释放的完整性**
   - 所有创建的Socket必须调用 `closesocket()` 关闭。
   - `getaddrinfo` 返回值需单独释放（`freeaddrinfo`）。

4. **错误处理的全面性**
   - 所有Socket操作函数均需校验返回值。
   - 调用 `WSAGetLastError()` 获取错误码。

5. **多线程场景的线程安全**
   - 多个线程操作同一个Socket时，需通过互斥锁等同步机制确保原子性。

6. **避免过度配置Socket选项**
   - 仅配置业务必需的Socket选项，默认配置已适配大多数场景。

---

## 五、与前文内容的关联

- **与Socket状态关联**：通过 `getsockopt` 获取Socket状态，通过 `shutdown`、`closesocket` 切换Socket状态。
- **与主机名称/地址关联**：`getsockname`、`getpeername` 获取的IP地址，本质是主机地址的具体应用。
- **与后端实战关联**：前文TCP服务器/客户端案例中，可添加Socket信息获取代码（打印本地/远端地址），添加 `setsockopt`（SO_REUSEADDR）解决服务器重启时的端口占用问题。

---

## 📖 相关阅读

- [Socket信息与控制 支持例程（Winsock后端实战）](./Socket信息与控制%20支持例程（Winsock后端实战）.md)
- [Socket状态及相关注意事项](./Socket状态及相关注意事项.md)
- [TCPIP协议首部（后端实战版，含WinSock调试关联）](./TCPIP协议首部（后端实战版，含WinSock调试关联）.md)
- [TCPIP协议服务](./TCPIP协议服务.md)
