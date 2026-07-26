# WinSock网络编程（C++后端）
> 核心定位：掌握Windows平台网络编程核心API与I/O模型，理解跨平台Socket移植

## 目录
1. [WinSock概述](#1-winsock概述)
2. [WinSock初始化与清理](#2-winsock初始化与清理)
3. [Socket创建与配置](#3-socket创建与配置)
4. [TCP服务器与客户端](#4-tcp服务器与客户端)
5. [UDP通信](#5-udp通信)
6. [Socket信息获取与控制](#6-socket信息获取与控制)
7. [WinSock IO模型](#7-winsock-io模型)
8. [BSD Sockets移植指南](#8-bsd-sockets移植指南)
9. [调试与最佳实践](#9-调试与最佳实践)

---

## 1. WinSock概述

### 1.1 WinSock的本质

WinSock（Windows Sockets）是微软在Windows平台上对Berkeley Socket API进行标准化封装与扩展的网络通信接口标准。它并非独立的网络协议，而是一套接口规范，通过动态链接库实现具体功能。

> **本质**：Windows内核网络能力与用户态程序的桥梁，兼容BSD Sockets标准并扩展Windows特性。

### 1.2 WinSock版本演进

| 版本 | 关键特性 | 依赖库 |
|------|---------|--------|
| **WinSock 1.0** | 奠基版本，Berkeley Socket核心功能移植 | winsock.dll |
| **WinSock 1.1** | 完善基础框架，支持IPv4、TCP/UDP | winsock.dll |
| **WinSock 2.0** | 重大升级，支持IPv4/IPv6双栈、多I/O模型、重叠I/O | ws2_32.dll |
| **WinSock 2.2** | 当前主流版本，新增QoS、LSP等扩展能力 | ws2_32.dll |

> **💡 实际开发中，所有新项目都应使用WinSock 2.2版本，链接`ws2_32.lib`并包含`<winsock2.h>`。**

### 1.3 WinSock与BSD Sockets的核心关系

WinSock是BSD Sockets的Windows实现，二者核心通信逻辑一致，大部分接口名称和用法相同，但WinSock针对Windows特性增加了初始化、清理、错误处理逻辑。

| 对比维度 | BSD Sockets (Linux/Unix) | WinSock (Windows) |
|---------|-------------------------|-------------------|
| **初始化** | 无需初始化 | `WSAStartup()` 必须调用 |
| **清理** | 无需清理 | `WSACleanup()` 必须调用 |
| **Socket句柄** | `int` 类型 | `SOCKET` 类型（unsigned int） |
| **关闭Socket** | `close(fd)` | `closesocket(s)` |
| **错误处理** | `errno` / `perror()` | `WSAGetLastError()` |
| **头文件** | `<sys/socket.h>` 等 | `<winsock2.h>` |
| **非阻塞模式** | `fcntl()` O_NONBLOCK | `ioctlsocket()` FIONBIO |
| **信号处理** | 支持 SIGPIPE | 不支持 SIGPIPE |

### 1.4 核心组件

| 组件 | 说明 |
|------|------|
| **SOCKET句柄** | 网络通信的核心端点标识，由`socket()`创建，`closesocket()`销毁 |
| **ws2_32.dll** | WinSock 2.x核心动态链接库，由`WSAStartup()`动态加载 |
| **WSADATA** | 存储Winsock初始化后的运行时信息 |
| **sockaddr_in** | 存储IPv4网络地址（协议族、端口号、IP地址） |

---

## 2. WinSock初始化与清理

### 2.1 WSAStartup

所有WinSock编程的第一步，用于初始化WinSock库、加载`ws2_32.dll`、协商版本。

```c
int WSAStartup(WORD wVersionRequested, LPWSADATA lpWSAData);
```

| 参数 | 说明 |
|------|------|
| `wVersionRequested` | 请求的WinSock版本，推荐`MAKEWORD(2,2)` |
| `lpWSAData` | 输出参数，存储初始化后的运行信息 |

**完整初始化流程：**

```c
#include <winsock2.h>
#include <stdio.h>
#pragma comment(lib, "ws2_32.lib")

int main() {
    WSADATA wsaData;
    int ret = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (ret != 0) {
        printf("WSAStartup失败，错误码：%d\n", WSAGetLastError());
        return 1;
    }

    // 验证版本是否匹配
    if (LOBYTE(wsaData.wVersion) != 2 || HIBYTE(wsaData.wVersion) != 2) {
        printf("不支持WinSock 2.2版本\n");
        WSACleanup();
        return 1;
    }

    printf("WinSock初始化成功\n");
    printf("最高支持版本：%d.%d\n", LOBYTE(wsaData.wHighVersion), HIBYTE(wsaData.wHighVersion));
    printf("描述：%s\n", wsaData.szDescription);
    printf("系统状态：%s\n", wsaData.szSystemStatus);

    // ... 后续Socket操作 ...

    WSACleanup();
    return 0;
}
```

### 2.2 WSACleanup

释放WinSock资源，卸载`ws2_32.dll`。

```c
int WSACleanup(void);
```

> **⚠️ 注意事项：**
> - 必须在程序退出前调用，且必须与`WSAStartup`配对
> - 调用前需确保所有Socket已通过`closesocket()`关闭
> - 多次调用`WSAStartup`需要对应多次`WSACleanup`

---

## 3. Socket创建与配置

### 3.1 socket — 创建Socket句柄

```c
SOCKET socket(int af, int type, int protocol);
```

| 参数 | 说明 |
|------|------|
| `af` | 地址族：`AF_INET`(IPv4)、`AF_INET6`(IPv6)、`AF_UNSPEC`(双栈) |
| `type` | Socket类型：`SOCK_STREAM`(TCP)、`SOCK_DGRAM`(UDP)、`SOCK_RAW` |
| `protocol` | 协议，通常设为0自动匹配 |

```c
SOCKET sock = socket(AF_INET, SOCK_STREAM, 0);
if (sock == INVALID_SOCKET) {
    printf("创建Socket失败，错误码：%d\n", WSAGetLastError());
    return 1;
}
```

### 3.2 bind — 绑定地址与端口

服务器端必须调用，将Socket与本地IP和端口绑定。

```c
int bind(SOCKET s, const struct sockaddr* name, int namelen);
```

```c
struct sockaddr_in serverAddr;
serverAddr.sin_family = AF_INET;           // IPv4
serverAddr.sin_port = htons(8080);         // 端口（网络字节序）
serverAddr.sin_addr.s_addr = htonl(INADDR_ANY); // 绑定所有本地IP

int ret = bind(serverSock, (struct sockaddr*)&serverAddr, sizeof(serverAddr));
if (ret == SOCKET_ERROR) {
    printf("绑定失败，错误码：%d\n", WSAGetLastError());
    closesocket(serverSock);
    return 1;
}
```

> **💡 `INADDR_ANY`表示绑定所有本地IP地址，当服务器有多个网卡时非常有用。端口号必须用`htons()`转换为网络字节序。**

### 3.3 listen — 设置监听

```c
int listen(SOCKET s, int backlog);
```

`backlog`：监听队列的最大长度，推荐值5-10，最大`SOMAXCONN`。

> **⚠️ backlog过小会导致客户端连接被拒绝（WSAECONNREFUSED），过大则浪费系统资源。**

### 3.4 accept — 接受连接

阻塞函数，等待客户端连接并返回一个新的Socket用于通信。

```c
SOCKET accept(SOCKET s, struct sockaddr* addr, int* addrlen);
```

```c
struct sockaddr_in clientAddr;
int clientAddrLen = sizeof(clientAddr);
SOCKET clientSock = accept(serverSock, (struct sockaddr*)&clientAddr, &clientAddrLen);
if (clientSock == INVALID_SOCKET) {
    printf("接收连接失败，错误码：%d\n", WSAGetLastError());
    return 1;
}
printf("客户端连接：IP=%s, Port=%d\n",
       inet_ntoa(clientAddr.sin_addr), ntohs(clientAddr.sin_port));
```

### 3.5 connect — 发起连接

客户端调用，向服务器发起TCP连接（触发三次握手）。

```c
int connect(SOCKET s, const struct sockaddr* name, int namelen);
```

```c
struct sockaddr_in serverAddr;
serverAddr.sin_family = AF_INET;
serverAddr.sin_port = htons(8080);
serverAddr.sin_addr.s_addr = inet_addr("127.0.0.1");

int ret = connect(clientSock, (struct sockaddr*)&serverAddr, sizeof(serverAddr));
if (ret == SOCKET_ERROR) {
    printf("连接失败，错误码：%d\n", WSAGetLastError());
    closesocket(clientSock);
    return 1;
}
```

---

## 4. TCP服务器与客户端

### 4.1 数据收发：send / recv

```c
int send(SOCKET s, const char* buf, int len, int flags);
int recv(SOCKET s, char* buf, int len, int flags);
```

| 返回值 | 含义 |
|--------|------|
| **> 0** | 成功发送/接收的字节数 |
| **0** | 对端关闭连接（仅recv） |
| **SOCKET_ERROR** | 失败，需调用`WSAGetLastError()`获取错误码 |

```c
char recvBuf[1024] = {0};
int recvLen = recv(clientSock, recvBuf, sizeof(recvBuf) - 1, 0);
if (recvLen == SOCKET_ERROR) {
    printf("接收失败，错误码：%d\n", WSAGetLastError());
} else if (recvLen == 0) {
    printf("客户端断开连接\n");
} else {
    printf("收到数据：%s\n", recvBuf);
    int sendLen = send(clientSock, "OK", 2, 0);
    if (sendLen == SOCKET_ERROR) {
        printf("发送失败，错误码：%d\n", WSAGetLastError());
    }
}
```

### 4.2 完整TCP回声服务器

```c
#include <winsock2.h>
#include <stdio.h>
#include <string.h>
#pragma comment(lib, "ws2_32.lib")

#define PORT 8080
#define BUF_SIZE 1024

int main() {
    WSADATA wsaData;
    int ret = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (ret != 0) {
        printf("WSAStartup失败，错误码：%d\n", WSAGetLastError());
        return 1;
    }

    // 创建监听Socket
    SOCKET listenSock = socket(AF_INET, SOCK_STREAM, 0);
    if (listenSock == INVALID_SOCKET) {
        printf("socket创建失败，错误码：%d\n", WSAGetLastError());
        WSACleanup();
        return 1;
    }

    // 允许端口复用（调试时避免TIME_WAIT问题）
    int opt = 1;
    setsockopt(listenSock, SOL_SOCKET, SO_REUSEADDR, (char*)&opt, sizeof(opt));

    // 绑定
    struct sockaddr_in serverAddr;
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(PORT);
    serverAddr.sin_addr.s_addr = htonl(INADDR_ANY);
    ret = bind(listenSock, (struct sockaddr*)&serverAddr, sizeof(serverAddr));
    if (ret == SOCKET_ERROR) {
        printf("bind失败，错误码：%d\n", WSAGetLastError());
        closesocket(listenSock);
        WSACleanup();
        return 1;
    }

    // 监听
    ret = listen(listenSock, SOMAXCONN);
    if (ret == SOCKET_ERROR) {
        printf("listen失败，错误码：%d\n", WSAGetLastError());
        closesocket(listenSock);
        WSACleanup();
        return 1;
    }
    printf("回声服务器启动，监听端口：%d\n", PORT);

    // 循环接受客户端连接
    while (1) {
        struct sockaddr_in clientAddr;
        int addrLen = sizeof(clientAddr);
        SOCKET clientSock = accept(listenSock,
                                   (struct sockaddr*)&clientAddr, &addrLen);
        if (clientSock == INVALID_SOCKET) {
            printf("accept失败，错误码：%d\n", WSAGetLastError());
            continue;
        }
        printf("新客户端连接：%s:%d\n",
               inet_ntoa(clientAddr.sin_addr), ntohs(clientAddr.sin_port));

        // 回声处理：接收数据并原样返回
        char buf[BUF_SIZE] = {0};
        int recvLen = recv(clientSock, buf, BUF_SIZE - 1, 0);
        if (recvLen > 0) {
            printf("收到：%s (len=%d)\n", buf, recvLen);
            send(clientSock, buf, recvLen, 0); // 回声
        }
        closesocket(clientSock);
    }

    closesocket(listenSock);
    WSACleanup();
    return 0;
}
```

### 4.3 完整TCP客户端

```c
#include <winsock2.h>
#include <stdio.h>
#include <string.h>
#pragma comment(lib, "ws2_32.lib")

#define SERVER_IP "127.0.0.1"
#define SERVER_PORT 8080
#define BUF_SIZE 1024

int main() {
    WSADATA wsaData;
    int ret = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (ret != 0) {
        printf("WSAStartup失败，错误码：%d\n", WSAGetLastError());
        return 1;
    }

    SOCKET clientSock = socket(AF_INET, SOCK_STREAM, 0);
    if (clientSock == INVALID_SOCKET) {
        printf("socket创建失败，错误码：%d\n", WSAGetLastError());
        WSACleanup();
        return 1;
    }

    struct sockaddr_in serverAddr;
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(SERVER_PORT);
    serverAddr.sin_addr.s_addr = inet_addr(SERVER_IP);

    ret = connect(clientSock, (struct sockaddr*)&serverAddr, sizeof(serverAddr));
    if (ret == SOCKET_ERROR) {
        printf("连接服务器失败，错误码：%d\n", WSAGetLastError());
        closesocket(clientSock);
        WSACleanup();
        return 1;
    }
    printf("连接服务器成功（%s:%d）\n", SERVER_IP, SERVER_PORT);

    // 发送数据
    char sendBuf[BUF_SIZE] = {0};
    printf("请输入消息：");
    fgets(sendBuf, BUF_SIZE, stdin);
    sendBuf[strlen(sendBuf) - 1] = '\0';

    ret = send(clientSock, sendBuf, strlen(sendBuf), 0);
    if (ret == SOCKET_ERROR) {
        printf("发送失败，错误码：%d\n", WSAGetLastError());
        closesocket(clientSock);
        WSACleanup();
        return 1;
    }

    // 接收回声
    char recvBuf[BUF_SIZE] = {0};
    ret = recv(clientSock, recvBuf, BUF_SIZE - 1, 0);
    if (ret > 0) {
        recvBuf[ret] = '\0';
        printf("服务器回声：%s\n", recvBuf);
    } else if (ret == 0) {
        printf("服务器关闭连接\n");
    }

    closesocket(clientSock);
    WSACleanup();
    return 0;
}
```

---

## 5. UDP通信

### 5.1 函数原型

```c
int sendto(SOCKET s, const char* buf, int len, int flags,
           const struct sockaddr* to, int tolen);
int recvfrom(SOCKET s, char* buf, int len, int flags,
             struct sockaddr* from, int* fromlen);
```

### 5.2 UDP服务器示例

```c
#include <winsock2.h>
#include <stdio.h>
#pragma comment(lib, "ws2_32.lib")

#define PORT 8080
#define BUF_SIZE 1024

int main() {
    WSADATA wsaData;
    WSAStartup(MAKEWORD(2, 2), &wsaData);

    SOCKET udpSock = socket(AF_INET, SOCK_DGRAM, 0);
    if (udpSock == INVALID_SOCKET) {
        printf("socket创建失败，错误码：%d\n", WSAGetLastError());
        WSACleanup();
        return 1;
    }

    struct sockaddr_in serverAddr;
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(PORT);
    serverAddr.sin_addr.s_addr = htonl(INADDR_ANY);

    if (bind(udpSock, (struct sockaddr*)&serverAddr, sizeof(serverAddr))
        == SOCKET_ERROR) {
        printf("bind失败，错误码：%d\n", WSAGetLastError());
        closesocket(udpSock);
        WSACleanup();
        return 1;
    }
    printf("UDP服务器启动，监听端口：%d\n", PORT);

    char buf[BUF_SIZE] = {0};
    struct sockaddr_in clientAddr;
    int addrLen = sizeof(clientAddr);

    int recvLen = recvfrom(udpSock, buf, BUF_SIZE, 0,
                           (struct sockaddr*)&clientAddr, &addrLen);
    if (recvLen > 0) {
        printf("收到客户端 %s:%d 数据：%s\n",
               inet_ntoa(clientAddr.sin_addr),
               ntohs(clientAddr.sin_port), buf);
        sendto(udpSock, buf, recvLen, 0,
               (struct sockaddr*)&clientAddr, addrLen);
    }

    closesocket(udpSock);
    WSACleanup();
    return 0;
}
```

> **💡 UDP与TCP的核心差异：UDP无连接，无需listen/accept，直接使用`recvfrom`接收数据。**

### 5.3 TCP与UDP选择

| 维度 | TCP | UDP |
|------|-----|-----|
| 连接性 | 面向连接（三次握手） | 无连接 |
| 可靠性 | 可靠（重传、排序、流量控制） | 不可靠（尽最大努力交付） |
| 性能 | 延迟较高 | 低延迟 |
| 数据边界 | 字节流（无边界，需处理粘包） | 数据报（有边界） |
| 适用场景 | 文件传输、HTTP、远程调用 | 音视频、实时游戏、DNS |

---

## 6. Socket信息获取与控制

### 6.1 getsockname — 获取本地地址

获取指定Socket绑定的本地IP地址和端口。

```c
int getsockname(SOCKET s, struct sockaddr* name, int* namelen);
```

```c
struct sockaddr_in localAddr;
int addrLen = sizeof(localAddr);
int ret = getsockname(serverSock, (struct sockaddr*)&localAddr, &addrLen);
if (ret == 0) {
    printf("本地地址：%s:%d\n",
           inet_ntoa(localAddr.sin_addr),
           ntohs(localAddr.sin_port));
}
```

### 6.2 getpeername — 获取远端地址

获取与当前Socket建立连接的远端设备地址，仅适用于已建立连接的Socket。

```c
int getpeername(SOCKET s, struct sockaddr* name, int* namelen);
```

```c
struct sockaddr_in peerAddr;
int addrLen = sizeof(peerAddr);
int ret = getpeername(clientSock, (struct sockaddr*)&peerAddr, &addrLen);
if (ret == 0) {
    printf("对端地址：%s:%d\n",
           inet_ntoa(peerAddr.sin_addr),
           ntohs(peerAddr.sin_port));
}
```

### 6.3 setsockopt — 设置Socket选项

| 选项 | 级别 | 作用 | 设置时机 |
|------|------|------|---------|
| `SO_REUSEADDR` | `SOL_SOCKET` | 端口复用，解决TIME_WAIT问题 | `bind()`之前 |
| `SO_KEEPALIVE` | `SOL_SOCKET` | 定期发送心跳包检测连接 | 连接建立后 |
| `TCP_NODELAY` | `IPPROTO_TCP` | 禁用Nagle算法，减少延迟 | 连接建立后 |
| `SO_RCVTIMEO` | `SOL_SOCKET` | 接收超时设置 | 任意阶段 |
| `SO_SNDTIMEO` | `SOL_SOCKET` | 发送超时设置 | 任意阶段 |

```c
// SO_REUSEADDR — 端口复用
int opt = 1;
setsockopt(serverSock, SOL_SOCKET, SO_REUSEADDR, (char*)&opt, sizeof(opt));

// SO_KEEPALIVE — TCP心跳保活
opt = 1;
setsockopt(clientSock, SOL_SOCKET, SO_KEEPALIVE, (char*)&opt, sizeof(opt));

// TCP_NODELAY — 禁用Nagle算法（实时通信场景）
opt = 1;
setsockopt(clientSock, IPPROTO_TCP, TCP_NODELAY, (char*)&opt, sizeof(opt));

// SO_RCVTIMEO — 接收超时5秒
int timeout = 5000;
setsockopt(clientSock, SOL_SOCKET, SO_RCVTIMEO, (char*)&timeout, sizeof(timeout));
```

### 6.4 ioctlsocket — 非阻塞模式

```c
u_long mode = 1; // 1=非阻塞，0=阻塞
int ret = ioctlsocket(sock, FIONBIO, &mode);
if (ret == SOCKET_ERROR) {
    printf("设置非阻塞失败，错误码：%d\n", WSAGetLastError());
}
```

> **⚠️ 非阻塞模式下，send/recv/accept等函数会立即返回。若返回SOCKET_ERROR且WSAGetLastError()返回WSAEWOULDBLOCK(10035)，表示资源暂时不可用（非真正错误）。**

### 6.5 getsockopt — 获取选项值

```c
int optval;
int optlen = sizeof(optval);

// 获取发送缓冲区大小
getsockopt(sock, SOL_SOCKET, SO_SNDBUF, (char*)&optval, &optlen);
printf("发送缓冲区：%d 字节\n", optval);

// 获取接收缓冲区大小
getsockopt(sock, SOL_SOCKET, SO_RCVBUF, (char*)&optval, &optlen);
printf("接收缓冲区：%d 字节\n", optval);

// 检查Socket错误状态
getsockopt(sock, SOL_SOCKET, SO_ERROR, (char*)&optval, &optlen);
if (optval == 0) {
    printf("Socket连接正常\n");
} else {
    printf("Socket异常，错误码：%d\n", optval);
}
```

### 6.6 shutdown — 优雅关闭连接

```c
int shutdown(SOCKET s, int how);
```

| `how`值 | 含义 |
|---------|------|
| `SD_RECEIVE` | 关闭接收功能 |
| `SD_SEND` | 关闭发送功能 |
| `SD_BOTH` | 同时关闭接收和发送 |

```c
shutdown(clientSock, SD_BOTH);
// 等待数据发送完毕
Sleep(100);
closesocket(clientSock);
```

> **💡 建议先调用`shutdown()`再调用`closesocket()`，确保数据完整传输。**

### 6.7 强制关闭连接

```c
struct linger ling;
ling.l_onoff = 1;   // 开启强制关闭
ling.l_linger = 0;   // 超时0秒，立即关闭（RST）
setsockopt(clientSock, SOL_SOCKET, SO_LINGER, (char*)&ling, sizeof(ling));
closesocket(clientSock);
```

> **⚠️ 强制关闭会丢失未发送完成的数据，仅用于异常场景。**

---

## 7. WinSock IO模型

### 7.1 IO模型对比

| IO模型 | 特性 | 复杂度 | 并发能力 | 适用场景 |
|--------|------|--------|---------|---------|
| **阻塞I/O** | 函数阻塞直到操作完成 | 低 | 低 | 单连接、简单测试 |
| **非阻塞I/O** | 立即返回，轮询检查状态 | 中 | 中 | 少量连接、简单并发 |
| **WSAAsyncSelect** | 网络事件映射为Windows消息 | 中 | 中 | 有窗口的Win32应用 |
| **WSAEventSelect** | 网络事件映射为事件对象 | 中 | 中 | 无窗口的多线程应用 |
| **重叠I/O** | 重叠结构+事件通知/回调 | 高 | 高 | 高性能服务器 |
| **IOCP** | 完成端口 + 线程池 | 很高 | 极高 | 万级并发服务器 |

### 7.2 阻塞I/O

函数调用后阻塞当前线程，直到操作完成。最简单但并发能力最低。

```c
// recv会阻塞直到有数据到达
int recvLen = recv(sock, buf, sizeof(buf), 0);
```

### 7.3 非阻塞I/O

函数立即返回，通过循环轮询或select模型检查Socket状态。

```c
u_long mode = 1;
ioctlsocket(sock, FIONBIO, &mode);

// 非阻塞recv，可能返回WSAEWOULDBLOCK
int ret = recv(sock, buf, sizeof(buf), 0);
if (ret == SOCKET_ERROR && WSAGetLastError() == WSAEWOULDBLOCK) {
    // 暂无数据，稍后重试
    Sleep(10);
}
```

### 7.4 WSAEventSelect

将网络事件与事件对象关联，通过`WSAWaitForMultipleEvents`等待事件。

```c
WSAEVENT hEvent = WSACreateEvent();
WSAEventSelect(sock, hEvent, FD_READ | FD_CLOSE);

// 等待事件发生
WSAWaitForMultipleEvents(1, &hEvent, FALSE, WSA_INFINITE, FALSE);
WSAResetEvent(hEvent);

// 处理网络事件
WSANETWORKEVENTS networkEvents;
WSAEnumNetworkEvents(sock, hEvent, &networkEvents);
```

### 7.5 IOCP（完成端口）

Windows平台最高性能的IO模型，是后端高并发场景的首选方案。

**核心概念：**
- **完成端口**：一个内核对象，管理多个Socket的异步IO操作
- **线程池**：少量工作线程处理大量并发连接
- **重叠结构**：每个IO操作关联一个`OVERLAPPED`结构

**核心流程：**

```c
// 1. 创建完成端口
HANDLE hIocp = CreateIoCompletionPort(
    INVALID_HANDLE_VALUE, NULL, 0, 0);
if (hIocp == NULL) {
    printf("创建完成端口失败，错误码：%d\n", GetLastError());
    return 1;
}

// 2. 将Socket与完成端口关联
CreateIoCompletionPort(
    (HANDLE)sock, hIocp, (ULONG_PTR)sock, 0);

// 3. 投递异步IO请求
WSAOVERLAPPED overlapped = {0};
WSABUF wsaBuf;
wsaBuf.buf = recvBuf;
wsaBuf.len = sizeof(recvBuf);
DWORD flags = 0;
WSARecv(sock, &wsaBuf, 1, NULL, &flags, &overlapped, NULL);

// 4. 工作线程获取完成结果
while (1) {
    DWORD bytesTransferred;
    ULONG_PTR completionKey;
    LPOVERLAPPED lpOverlapped;

    BOOL ret = GetQueuedCompletionStatus(
        hIocp, &bytesTransferred,
        &completionKey, &lpOverlapped, INFINITE);

    if (ret) {
        // 处理IO完成事件
        SOCKET completedSock = (SOCKET)completionKey;
        // ... 处理数据 ...
    }
}
```

> **💡 IOCP推荐场景：万级客户端并发连接，如网游服务器、即时通信服务器、WebSocket网关。**

---

## 8. BSD Sockets移植指南

### 8.1 核心差异总结

| 差异点 | BSD Sockets (Linux) | WinSock (Windows) | 移植方案 |
|--------|-------------------|-------------------|---------|
| 初始化 | 无 | `WSAStartup` | `#ifdef _WIN32` 包裹 |
| 清理 | 无 | `WSACleanup` | `#ifdef _WIN32` 包裹 |
| 错误码 | `errno` | `WSAGetLastError()` | 封装错误打印函数 |
| Socket类型 | `int` | `SOCKET` | `typedef`统一 |
| 关闭函数 | `close()` | `closesocket()` | `#define close closesocket` |
| 非阻塞 | `fcntl() O_NONBLOCK` | `ioctlsocket() FIONBIO` | 条件编译 |
| 信号SIGPIPE | 支持 | 不支持 | 移除处理代码 |
| 头文件 | `<sys/socket.h>`等 | `<winsock2.h>` | 条件编译 |
| 链接库 | 无需手动链接 | `ws2_32.lib` | pragma comment |

### 8.2 跨平台Socket封装模板

```c
#ifdef _WIN32
#include <winsock2.h>
#pragma comment(lib, "ws2_32.lib")
#define CLOSE_SOCKET(s) closesocket(s)
typedef int socklen_t;
#else
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <errno.h>
#define CLOSE_SOCKET(s) close(s)
#define SOCKET int
#define INVALID_SOCKET (-1)
#define SOCKET_ERROR (-1)
#endif

// 统一错误打印
void print_error(const char* msg) {
#ifdef _WIN32
    printf("%s, 错误码：%d\n", msg, WSAGetLastError());
#else
    perror(msg);
#endif
}

// 统一初始化函数
int init_network() {
#ifdef _WIN32
    WSADATA wsaData;
    int ret = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (ret != 0) return -1;
    if (LOBYTE(wsaData.wVersion) != 2 || HIBYTE(wsaData.wVersion) != 2) {
        WSACleanup();
        return -1;
    }
#endif
    return 0;
}

void cleanup_network() {
#ifdef _WIN32
    WSACleanup();
#endif
}
```

### 8.3 移植注意事项

> **⚠️ 注意事项：**
> - 条件编译标识使用`#ifdef _WIN32`，不要使用`_WIN64`（仅64位环境定义）
> - 非阻塞模式必须通过条件编译区分实现
> - 移除所有SIGPIPE信号处理代码
> - 使用`inet_ntop`/`inet_pton`替代`inet_ntoa`/`inet_addr`以支持IPv6

---

## 9. 调试与最佳实践

### 9.1 WinSock错误码速查

| 错误码 | 宏定义 | 含义 | 常见场景 |
|-------|--------|------|---------|
| 10048 | `WSAEADDRINUSE` | 地址已在使用 | 端口被占用，需启用`SO_REUSEADDR` |
| 10054 | `WSAECONNRESET` | 连接被远程主机重置 | 对端异常退出，检查RST标志 |
| 10060 | `WSAETIMEDOUT` | 连接超时 | 服务器未启动/防火墙拦截 |
| 10061 | `WSAECONNREFUSED` | 连接被拒绝 | 服务器未监听目标端口 |
| 10093 | `WSANOTINITIALISED` | 未初始化WinSock | 忘记调用`WSAStartup` |
| 10038 | `WSAENOTSOCK` | 非Socket句柄操作 | 重复`closesocket`同一句柄 |
| 10013 | `WSAEACCES` | 权限被拒绝 | 未以管理员身份运行 |
| 10035 | `WSAEWOULDBLOCK` | 资源暂时不可用 | 非阻塞模式下的正常状态 |

### 9.2 调试工具

| 工具 | 用途 | 常用命令 |
|------|------|---------|
| **Wireshark** | 抓包分析协议首部、排查连接异常 | 设置过滤条件：`tcp.port==8080` |
| **netstat** | 查看端口占用、连接状态 | `netstat -ano \| findstr 8080` |
| **telnet** | 测试TCP端口可达性 | `telnet 127.0.0.1 8080` |
| **Process Monitor** | 监控进程的系统调用和资源访问 | 过滤`ws2_32.dll`操作 |
| **Dependency Walker** | 检查DLL依赖完整性 | 排查`ws2_32.dll`加载问题 |

### 9.3 该做的

- **每个Socket API调用后检查返回值**：`INVALID_SOCKET`或`SOCKET_ERROR`时立即调用`WSAGetLastError()`
- **严格遵循初始化 -> 使用 -> 清理流程**：`WSAStartup` -> ... -> `closesocket` -> `WSACleanup`
- **启用端口复用**：服务器端在`bind()`之前设置`SO_REUSEADDR`
- **异常路径也需清理资源**：使用goto cleanup或RAII模式确保所有路径都释放资源
- **异步IO操作使用WSAOVERLAPPED**：确保重叠结构在整个IO操作期间有效
- **Win10/Win11以管理员身份运行**：避免权限不足导致bind失败（错误码10013）

### 9.4 不该做的

- **忽略错误码**：不可因接口调用失败就直接终止程序，需获取错误码针对性排查
- **混用WinSock与BSD Sockets头文件**：包含顺序需注意（`<winsock2.h>`必须在`<windows.h>`之前）
- **在多线程中同时操作同一Socket**：需通过互斥锁等同步机制确保原子性
- **在非阻塞模式中忽略WSAEWOULDBLOCK**：这不是真正错误，应重试而非终止
- **同时使用WinSock 1.1和2.0接口**：统一使用2.2版本

### 9.5 资源泄漏排查清单

| 检查项 | 说明 |
|--------|------|
| Socket句柄 | 每个`socket()`必须有对应的`closesocket()` |
| WinSock资源 | `WSAStartup()`必须有对应的`WSACleanup()` |
| 完成端口 | `CreateIoCompletionPort()`必须有对应的`CloseHandle()` |
| 事件对象 | `WSACreateEvent()`必须有对应的`WSACloseEvent()` |
| getaddrinfo结果 | `getaddrinfo()`必须有对应的`freeaddrinfo()` |

> **🎯 总结：WinSock编程的核心是"初始化 -> 使用 -> 清理"三步骤，每个API必须检查返回值，所有资源必须有对应的释放操作。掌握WinSock是实现Windows平台高性能网络服务的基石。**
