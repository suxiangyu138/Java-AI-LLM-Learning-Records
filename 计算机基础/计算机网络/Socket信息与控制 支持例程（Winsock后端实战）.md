# Socket信息与控制 支持例程（Winsock后端实战）

## 📑 目录

- [例程1：Socket本地/远端地址信息获取](#例程1socket本地远端地址信息获取)
- [例程2：Socket选项设置与信息获取](#例程2socket选项设置与信息获取)
- [例程3：Socket连接控制（关闭连接/强制关闭）](#例程3socket连接控制关闭连接强制关闭)
- [例程4：综合例程（Socket信息获取+控制）](#例程4综合例程socket信息获取控制)
- [例程使用注意事项](#例程使用注意事项)

---

本文提供4个核心支持例程，均基于Winsock 2.2版本（C语言），可直接复制编译运行（VS/MinGW均可，需链接ws2_32.lib）。例程覆盖Socket信息获取、Socket控制操作，贴合后端开发实际场景（服务器/客户端），每个例程包含完整注释、错误处理和资源释放。

> **核心说明**：所有例程均包含Winsock初始化、资源清理的完整流程，严格遵循后端开发"异常全覆盖、资源可控"的原则，同时融入端口复用、非阻塞设置、资源释放顺序等避坑点。

---

## 例程1：Socket本地/远端地址信息获取（后端调试常用）

**核心功能**：创建TCP服务器，接收客户端连接后，获取服务器本地绑定地址、客户端远端连接地址，用于后端日志打印、客户端信息监控。

```c
#include <winsock2.h>
#include <stdio.h>
#include <string.h>
#pragma comment(lib, "ws2_32.lib")

#define SERVER_PORT 8080  // 服务器端口
#define BUF_SIZE 1024     // 缓冲区大小

int main() {
    WSADATA wsaData;
    SOCKET serverSock, clientSock;
    struct sockaddr_in serverAddr, localAddr, clientAddr;
    int ret, addrLen;
    char recvBuf[BUF_SIZE] = {0};

    // 1. 初始化Winsock
    ret = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (ret != 0) {
        printf("WSAStartup失败，错误码：%d\n", WSAGetLastError());
        return 1;
    }
    if (LOBYTE(wsaData.wVersion) != 2 || HIBYTE(wsaData.wVersion) != 2) {
        printf("Winsock版本不支持\n");
        WSACleanup();
        return 1;
    }

    // 2. 创建TCP Socket
    serverSock = socket(AF_INET, SOCK_STREAM, 0);
    if (serverSock == INVALID_SOCKET) {
        printf("创建Socket失败，错误码：%d\n", WSAGetLastError());
        WSACleanup();
        return 1;
    }

    // 3. 设置端口复用
    int optval = 1;
    setsockopt(serverSock, SOL_SOCKET, SO_REUSEADDR, (char*)&optval, sizeof(optval));

    // 4. 绑定本地地址
    memset(&serverAddr, 0, sizeof(serverAddr));
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(SERVER_PORT);
    serverAddr.sin_addr.s_addr = htonl(INADDR_ANY);
    ret = bind(serverSock, (struct sockaddr*)&serverAddr, sizeof(serverAddr));
    if (ret == SOCKET_ERROR) {
        printf("绑定失败，错误码：%d\n", WSAGetLastError());
        closesocket(serverSock);
        WSACleanup();
        return 1;
    }

    // 5. 获取服务器本地绑定地址（getsockname用法）
    addrLen = sizeof(localAddr);
    ret = getsockname(serverSock, (struct sockaddr*)&localAddr, &addrLen);
    if (ret == SOCKET_ERROR) {
        printf("获取本地地址失败，错误码：%d\n", WSAGetLastError());
        closesocket(serverSock);
        WSACleanup();
        return 1;
    }
    printf("服务器初始化完成，本地绑定信息：\n");
    printf("本地IP：%s（绑定所有网卡）\n", inet_ntoa(localAddr.sin_addr));
    printf("本地端口：%d\n", ntohs(localAddr.sin_port));

    // 6. 监听客户端连接
    ret = listen(serverSock, 5);
    if (ret == SOCKET_ERROR) {
        printf("监听失败，错误码：%d\n", WSAGetLastError());
        closesocket(serverSock);
        WSACleanup();
        return 1;
    }
    printf("等待客户端连接...\n");

    // 7. 接收客户端连接
    addrLen = sizeof(clientAddr);
    clientSock = accept(serverSock, (struct sockaddr*)&clientAddr, &addrLen);
    if (clientSock == INVALID_SOCKET) {
        printf("接收连接失败，错误码：%d\n", WSAGetLastError());
        closesocket(serverSock);
        WSACleanup();
        return 1;
    }

    // 8. 获取客户端远端地址（getpeername用法）
    ret = getpeername(clientSock, (struct sockaddr*)&clientAddr, &addrLen);
    if (ret == SOCKET_ERROR) {
        printf("获取客户端地址失败，错误码：%d\n", WSAGetLastError());
        closesocket(clientSock);
        closesocket(serverSock);
        WSACleanup();
        return 1;
    }
    printf("客户端连接成功，远端信息：\n");
    printf("客户端IP：%s\n", inet_ntoa(clientAddr.sin_addr));
    printf("客户端端口：%d\n", ntohs(clientAddr.sin_port));

    // 9. 简单数据交互
    ret = recv(clientSock, recvBuf, BUF_SIZE-1, 0);
    if (ret > 0) {
        printf("收到客户端数据：%s\n", recvBuf);
        char sendBuf[] = "地址信息已获取，连接正常！";
        send(clientSock, sendBuf, strlen(sendBuf), 0);
    }

    // 10. 资源释放
    shutdown(clientSock, SD_BOTH);
    Sleep(100);
    closesocket(clientSock);
    closesocket(serverSock);
    WSACleanup();
    printf("程序退出，资源已全部释放\n");
    return 0;
}
```

---

## 例程2：Socket选项设置与信息获取（后端优化常用）

**核心功能**：创建Socket后，设置端口复用、接收/发送缓冲区大小、非阻塞模式，再通过 `getsockopt` 获取选项信息，验证设置是否生效。

```c
#include <winsock2.h>
#include <stdio.h>
#pragma comment(lib, "ws2_32.lib")

#define BUF_SIZE 8 * 1024  // 自定义缓冲区大小（8KB）

int main() {
    WSADATA wsaData;
    SOCKET sock;
    int ret, optval, optlen;

    // 1. 初始化Winsock
    ret = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (ret != 0) {
        printf("WSAStartup失败，错误码：%d\n", WSAGetLastError());
        return 1;
    }

    // 2. 创建TCP Socket
    sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock == INVALID_SOCKET) {
        printf("创建Socket失败，错误码：%d\n", WSAGetLastError());
        WSACleanup();
        return 1;
    }

    // 3. 设置Socket选项
    // 3.1 端口复用
    optval = 1;
    ret = setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (char*)&optval, sizeof(optval));
    if (ret == SOCKET_ERROR) {
        printf("设置端口复用失败，错误码：%d\n", WSAGetLastError());
        closesocket(sock);
        WSACleanup();
        return 1;
    }

    // 3.2 设置接收/发送缓冲区大小
    optval = BUF_SIZE;
    ret = setsockopt(sock, SOL_SOCKET, SO_RCVBUF, (char*)&optval, sizeof(optval));
    if (ret == SOCKET_ERROR) {
        printf("设置接收缓冲区失败，错误码：%d\n", WSAGetLastError());
        closesocket(sock);
        WSACleanup();
        return 1;
    }
    ret = setsockopt(sock, SOL_SOCKET, SO_SNDBUF, (char*)&optval, sizeof(optval));
    if (ret == SOCKET_ERROR) {
        printf("设置发送缓冲区失败，错误码：%d\n", WSAGetLastError());
        closesocket(sock);
        WSACleanup();
        return 1;
    }

    // 3.3 设置非阻塞模式
    u_long mode = 1; // 1=非阻塞，0=阻塞
    ret = ioctlsocket(sock, FIONBIO, &mode);
    if (ret == SOCKET_ERROR) {
        printf("设置非阻塞模式失败，错误码：%d\n", WSAGetLastError());
        closesocket(sock);
        WSACleanup();
        return 1;
    }

    // 4. 获取Socket选项信息，验证设置生效
    optlen = sizeof(optval);

    getsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (char*)&optval, &optlen);
    printf("端口复用选项：%s\n", optval ? "开启（后端推荐）" : "关闭");

    getsockopt(sock, SOL_SOCKET, SO_RCVBUF, (char*)&optval, &optlen);
    printf("接收缓冲区大小：%d 字节（设置值：%d）\n", optval, BUF_SIZE);

    getsockopt(sock, SOL_SOCKET, SO_SNDBUF, (char*)&optval, &optlen);
    printf("发送缓冲区大小：%d 字节（设置值：%d）\n", optval, BUF_SIZE);

    getsockopt(sock, SOL_SOCKET, SO_ERROR, (char*)&optval, &optlen);
    printf("非阻塞模式设置：%s（错误码：%d，0表示正常）\n", mode ? "开启" : "关闭", optval);

    // 5. 资源释放
    closesocket(sock);
    WSACleanup();
    printf("程序退出，资源已释放\n");
    return 0;
}
```

---

## 例程3：Socket连接控制（关闭连接/强制关闭，后端异常处理）

**核心功能**：模拟客户端与服务器连接，实现正常关闭连接（shutdown + closesocket）和异常场景下强制关闭连接（SO_LINGER选项）。

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
    SOCKET clientSock;
    struct sockaddr_in serverAddr;
    int ret;
    char sendBuf[] = "测试连接控制：正常关闭与强制关闭";

    // 1. 初始化Winsock
    ret = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (ret != 0) {
        printf("WSAStartup失败，错误码：%d\n", WSAGetLastError());
        return 1;
    }

    // 2. 创建客户端Socket
    clientSock = socket(AF_INET, SOCK_STREAM, 0);
    if (clientSock == INVALID_SOCKET) {
        printf("创建Socket失败，错误码：%d\n", WSAGetLastError());
        WSACleanup();
        return 1;
    }

    // 3. 连接服务器
    memset(&serverAddr, 0, sizeof(serverAddr));
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(SERVER_PORT);
    serverAddr.sin_addr.s_addr = inet_addr(SERVER_IP);
    ret = connect(clientSock, (struct sockaddr*)&serverAddr, sizeof(serverAddr));
    if (ret == SOCKET_ERROR) {
        printf("连接服务器失败，错误码：%d（请先启动服务器例程）\n", WSAGetLastError());
        closesocket(clientSock);
        WSACleanup();
        return 1;
    }
    printf("连接服务器成功，发送测试数据...\n");
    send(clientSock, sendBuf, strlen(sendBuf), 0);

    // 4. 场景1：正常关闭连接
    printf("\n=== 正常关闭连接 ===");
    shutdown(clientSock, SD_BOTH);
    Sleep(200);

    int optval, optlen = sizeof(optval);
    getsockopt(clientSock, SOL_SOCKET, SO_ERROR, (char*)&optval, &optlen);
    printf("\n关闭连接后，Socket状态：%s（错误码：%d）\n", optval != 0 ? "已关闭" : "正常", optval);
    closesocket(clientSock);
    printf("正常关闭完成，Socket资源已释放\n");

    // 5. 场景2：强制关闭连接
    printf("\n=== 强制关闭连接 ===");
    clientSock = socket(AF_INET, SOCK_STREAM, 0);
    connect(clientSock, (struct sockaddr*)&serverAddr, sizeof(serverAddr));
    printf("重新连接服务器成功，准备强制关闭...\n");

    struct linger ling;
    ling.l_onoff = 1;
    ling.l_linger = 0;
    setsockopt(clientSock, SOL_SOCKET, SO_LINGER, (char*)&ling, sizeof(ling));

    closesocket(clientSock);
    printf("强制关闭完成，Socket资源已释放（可能丢失未发送数据）\n");

    // 6. 资源清理
    WSACleanup();
    printf("\n程序退出，所有资源已释放\n");
    return 0;
}
```

---

## 例程4：综合例程（Socket信息获取+控制，后端实战完整版）

**核心功能**：整合所有重点操作，实现TCP服务器的完整逻辑，是后端Socket开发的实战模板。

```c
#include <winsock2.h>
#include <stdio.h>
#include <string.h>
#pragma comment(lib, "ws2_32.lib")

#define SERVER_PORT 8080
#define BUF_SIZE 1024
#define BACKLOG 5

int main() {
    WSADATA wsaData;
    SOCKET serverSock, clientSock;
    struct sockaddr_in serverAddr, localAddr, clientAddr;
    int ret, addrLen = sizeof(struct sockaddr_in);
    char recvBuf[BUF_SIZE] = {0};
    char sendBuf[BUF_SIZE] = {0};

    // 1. 初始化Winsock
    ret = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (ret != 0) {
        printf("[错误] WSAStartup失败，错误码：%d\n", WSAGetLastError());
        return 1;
    }
    if (LOBYTE(wsaData.wVersion) != 2 || HIBYTE(wsaData.wVersion) != 2) {
        printf("[错误] Winsock版本不支持，需2.2版本\n");
        WSACleanup();
        return 1;
    }
    printf("[信息] Winsock初始化成功，版本：%d.%d\n",
        LOBYTE(wsaData.wVersion), HIBYTE(wsaData.wVersion));

    // 2. 创建TCP Socket
    serverSock = socket(AF_INET, SOCK_STREAM, 0);
    if (serverSock == INVALID_SOCKET) {
        printf("[错误] 创建Socket失败，错误码：%d\n", WSAGetLastError());
        WSACleanup();
        return 1;
    }
    printf("[信息] Socket创建成功，句柄：%d\n", serverSock);

    // 3. 设置Socket选项
    int optval = 1;
    ret = setsockopt(serverSock, SOL_SOCKET, SO_REUSEADDR, (char*)&optval, sizeof(optval));
    if (ret == SOCKET_ERROR) {
        printf("[错误] 设置端口复用失败，错误码：%d\n", WSAGetLastError());
        closesocket(serverSock);
        WSACleanup();
        return 1;
    }
    optval = 8 * 1024;
    setsockopt(serverSock, SOL_SOCKET, SO_RCVBUF, (char*)&optval, sizeof(optval));
    setsockopt(serverSock, SOL_SOCKET, SO_SNDBUF, (char*)&optval, sizeof(optval));
    printf("[信息] Socket选项设置完成（端口复用开启，缓冲区8KB）\n");

    // 4. 绑定本地地址
    memset(&serverAddr, 0, sizeof(serverAddr));
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(SERVER_PORT);
    serverAddr.sin_addr.s_addr = htonl(INADDR_ANY);
    ret = bind(serverSock, (struct sockaddr*)&serverAddr, sizeof(serverAddr));
    if (ret == SOCKET_ERROR) {
        printf("[错误] 绑定失败，错误码：%d（端口可能被占用）\n", WSAGetLastError());
        closesocket(serverSock);
        WSACleanup();
        return 1;
    }

    // 5. 获取本地绑定信息
    ret = getsockname(serverSock, (struct sockaddr*)&localAddr, &addrLen);
    if (ret == SOCKET_ERROR) {
        printf("[错误] 获取本地地址失败，错误码：%d\n", WSAGetLastError());
        closesocket(serverSock);
        WSACleanup();
        return 1;
    }
    printf("[信息] 服务器绑定成功，本地IP：%s，端口：%d\n",
        inet_ntoa(localAddr.sin_addr), ntohs(localAddr.sin_port));

    // 6. 监听客户端连接
    ret = listen(serverSock, BACKLOG);
    if (ret == SOCKET_ERROR) {
        printf("[错误] 监听失败，错误码：%d\n", WSAGetLastError());
        closesocket(serverSock);
        WSACleanup();
        return 1;
    }
    printf("[信息] 服务器开始监听，等待客户端连接（监听队列长度：%d）...\n", BACKLOG);

    // 7. 循环接收客户端连接
    while (1) {
        clientSock = accept(serverSock, (struct sockaddr*)&clientAddr, &addrLen);
        if (clientSock == INVALID_SOCKET) {
            printf("[错误] 接收连接失败，错误码：%d\n", WSAGetLastError());
            continue;
        }

        // 8. 获取客户端远端信息
        ret = getpeername(clientSock, (struct sockaddr*)&clientAddr, &addrLen);
        if (ret == SOCKET_ERROR) {
            printf("[错误] 获取客户端地址失败，错误码：%d\n", WSAGetLastError());
            closesocket(clientSock);
            continue;
        }
        printf("\n[信息] 客户端连接成功，IP：%s，端口：%d，Socket句柄：%d\n",
            inet_ntoa(clientAddr.sin_addr), ntohs(clientAddr.sin_port), clientSock);

        // 9. 数据交互
        ret = recv(clientSock, recvBuf, BUF_SIZE-1, 0);
        if (ret == SOCKET_ERROR) {
            printf("[错误] 接收数据失败，错误码：%d\n", WSAGetLastError());
            closesocket(clientSock);
            continue;
        } else if (ret == 0) {
            printf("[信息] 客户端主动断开连接\n");
            closesocket(clientSock);
            continue;
        }
        printf("[信息] 收到客户端数据：%s（长度：%d字节）\n", recvBuf, ret);

        sprintf(sendBuf, "服务器已收到数据，响应内容：%s", recvBuf);
        ret = send(clientSock, sendBuf, strlen(sendBuf), 0);
        if (ret == SOCKET_ERROR) {
            printf("[错误] 发送响应失败，错误码：%d\n", WSAGetLastError());
            closesocket(clientSock);
            continue;
        }
        printf("[信息] 响应发送成功，长度：%d字节\n", ret);

        // 10. 关闭客户端连接
        shutdown(clientSock, SD_BOTH);
        Sleep(100);
        closesocket(clientSock);
        printf("[信息] 客户端连接已关闭，Socket句柄：%d\n", clientSock);

        if (strcmp(recvBuf, "exit") == 0) {
            printf("\n[信息] 收到退出指令，服务器即将退出\n");
            break;
        }
    }

    // 11. 资源释放
    closesocket(serverSock);
    WSACleanup();
    printf("[信息] 服务器退出，所有资源已释放\n");
    return 0;
}
```

---

## 例程使用注意事项

| 注意事项 | 说明 |
|---------|------|
| **编译运行** | 所有例程均需链接ws2_32.lib（VS中通过 `#pragma comment(lib, "ws2_32.lib")` 自动链接） |
| **运行顺序** | 例程3需先运行例程1的服务器；例程4可单独运行 |
| **错误排查** | 通过printf打印的错误码，结合 `WSAGetLastError()` 的返回值排查问题 |
| **代码复用** | 例程中的核心函数调用可直接复制到后端项目中 |
| **多线程扩展** | 例程4为单线程阻塞模式，高并发场景可添加多线程或IOCP模型 |

---

## 📖 相关阅读

- [Socket信息与控制（Winsock后端开发实战）](./Socket信息与控制（Winsock后端开发实战）.md)
- [Socket状态及相关注意事项](./Socket状态及相关注意事项.md)
- [TCPIP协议首部（后端实战版，含WinSock调试关联）](./TCPIP协议首部（后端实战版，含WinSock调试关联）.md)
