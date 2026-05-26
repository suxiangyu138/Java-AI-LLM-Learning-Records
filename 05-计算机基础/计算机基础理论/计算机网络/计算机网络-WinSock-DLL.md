WinSock DLL开发（理论+实战，后端视角）
在WinSock后端开发中，将核心的Socket通信逻辑（如连接建立、数据收发、信息获取）封装为DLL（动态链接库），是企业级项目的常用实践——既能实现代码复用、模块解耦，也能便于后期维护和版本迭代（如修改通信逻辑无需重新编译整个主程序）。本文先讲解WinSock DLL的核心理论，再结合实战案例，完成“WinSock核心功能DLL的创建、调用、调试”全流程，所有例程基于Winsock 2.2版本（C语言），可直接编译复用，衔接前文Socket信息与控制、主机名称与地址等知识点。
一、WinSock DLL核心理论（后端必懂）
DLL（Dynamic Link Library，动态链接库）是Windows系统中一种可共享的代码和资源文件，与静态库（.lib）的核心区别是：DLL在程序运行时加载，而非编译时链接，可被多个程序同时调用，节省内存和磁盘空间。结合WinSock开发，重点掌握以下核心理论，避免踩坑。
（一）WinSock DLL的核心作用（后端场景）
后端开发中，WinSock DLL主要用于封装“可复用的Socket通信模块”，解决以下核心需求：
模块解耦：将Socket初始化、连接、收发、释放等核心逻辑封装为DLL，主程序只需调用DLL接口，无需关注底层实现，降低代码耦合度。
代码复用：多个后端程序（如不同的客户端/服务器）可共用同一个WinSock DLL，避免重复开发，提升开发效率。
维护便捷：当需要修改Socket通信逻辑（如优化端口复用、适配IPv6）时，只需修改DLL并重新编译，无需修改主程序，降低维护成本。
隐藏实现：DLL仅暴露对外接口，核心代码（如加密通信、异常处理逻辑）可隐藏，提升代码安全性。
（二）WinSock DLL与Winsock库的关联
WinSock DLL本身依赖Windows系统的Winsock核心库（ws2_32.lib/ws2_32.dll），开发时需注意以下关联逻辑：
WinSock DLL的开发，本质是“对ws2_32.dll接口的二次封装”——将ws2_32.dll提供的基础函数（如socket、bind、send、recv），封装为更贴合业务的自定义接口（如Winsock_Init、Socket_Connect、Socket_Send）。
编译WinSock DLL时，需链接ws2_32.lib（与前文Socket编程一致），确保DLL能正常调用Winsock核心接口。
主程序调用WinSock DLL时，无需再单独链接ws2_32.lib，只需加载DLL并调用其暴露的接口即可（DLL已内部关联Winsock库）。
（三）WinSock DLL开发的关键注意事项（理论避坑）
后端开发中，WinSock DLL的核心坑点集中在“资源管理”和“接口设计”，提前掌握以下理论要点，避免后期调试困难：
Winsock初始化与清理：DLL中需统一管理Winsock的初始化（WSAStartup）和清理（WSACleanup），建议在DLL加载时初始化，卸载时清理，避免主程序重复初始化/清理。
Socket资源管理：DLL中创建的Socket句柄，需提供对应的释放接口（如Socket_Close），避免主程序调用DLL后，Socket资源泄漏（与前文Socket资源释放逻辑一致）。
接口设计规范：DLL暴露的接口需简洁、统一，参数清晰（如传入IP、端口、缓冲区，返回操作结果），避免接口冗余，便于主程序调用。
错误处理：DLL内部需完善错误处理（调用WSAGetLastError获取错误码），并将错误信息反馈给主程序（如通过返回值、输出参数），便于主程序排查问题。
线程安全：若后端主程序是多线程场景，DLL接口需保证线程安全（如使用互斥锁保护共享资源），避免多线程同时调用DLL接口导致的状态错乱。
（四）WinSock DLL的加载与调用方式（后端实战基础）
主程序调用WinSock DLL，主要有两种方式，后端开发可根据场景选择：
1. 静态加载（隐式链接）
    核心特点：编译主程序时，需关联DLL对应的.lib文件（导入库），程序运行时自动加载DLL，开发简单、便捷，适用于DLL路径固定、接口稳定的场景（后端开发首选）。
    关键步骤：① 编译DLL时，生成.dll文件和.lib导入库；② 主程序中包含DLL的头文件，链接.lib导入库；③ 直接调用DLL暴露的接口。
2. 动态加载（显式链接）
    核心特点：主程序运行时，通过LoadLibrary函数加载DLL，通过GetProcAddress获取DLL接口地址，使用完成后通过FreeLibrary释放DLL，灵活性高，适用于DLL路径不固定、需要动态切换DLL版本的场景。
    关键步骤：① 主程序通过LoadLibrary加载DLL；② 通过GetProcAddress获取接口指针；③ 调用接口；④ 调用FreeLibrary释放DLL。
    二、WinSock DLL实战（核心环节，可直接复用）
    本节围绕“WinSock核心功能DLL”展开实战，分3个环节：① 创建WinSock DLL（封装Socket初始化、连接、收发、释放等核心接口）；② 静态加载DLL（主程序调用DLL接口，实现TCP客户端通信）；③ 动态加载DLL（主程序动态调用DLL接口，实现TCP服务器通信），所有代码可直接复制编译运行（VS 2019/2022均可）。
    实战准备
    1. 开发环境：Windows系统 + VS（或MinGW），确保已配置C语言编译环境；
    2. 核心依赖：链接ws2_32.lib（DLL中需添加，主程序静态加载时需添加，动态加载时无需添加）；
    3. 编译说明：DLL项目需设置为“动态链接库”，生成.dll和.lib文件；主程序项目需关联DLL的头文件和.lib文件（静态加载），或指定DLL路径（动态加载）。
    环节1：创建WinSock DLL（核心封装）
    目标：封装WinSock核心接口，包含Winsock初始化、Socket创建、TCP连接、数据收发、Socket关闭、Winsock清理，接口简洁、可复用，包含完整的错误处理和资源管理。
    步骤1：创建DLL头文件（WinsockDll.h）
    暴露DLL接口，定义接口函数和错误码，便于主程序调用和错误排查。

# ragma once

# nclude <winsock2.h>

# nclude <stdio.h>
// 导出宏定义（Windows DLL标准写法，确保接口能被主程序调用）

# fdef WINSOCKDLL_EXPORTS

# efine WINSOCKDLL_API __declspec(dllexport)

# lse

# efine WINSOCKDLL_API __declspec(dllimport)

# ndif
// 自定义错误码（后端调试常用，区分不同错误场景）

# efine DLL_SUCCESS 0          // 操作成功

# efine DLL_ERR_WSASTARTUP 1   // Winsock初始化失败

# efine DLL_ERR_SOCKET 2       // Socket创建失败

# efine DLL_ERR_CONNECT 3      // 连接失败

# efine DLL_ERR_SEND 4         // 发送数据失败

# efine DLL_ERR_RECV 5         // 接收数据失败

# efine DLL_ERR_CLOSE 6        // 关闭Socket失败

# efine DLL_ERR_PARAM 7        // 参数错误
// 1. Winsock初始化（DLL加载时自动调用，也可手动调用）
WINSOCKDLL_API int Winsock_Init(void);
// 2. 创建TCP Socket（返回Socket句柄，失败返回INVALID_SOCKET）
WINSOCKDLL_API SOCKET Socket_Create_TCP(void);
// 3. TCP客户端连接服务器（返回错误码，DLL_SUCCESS表示成功）
WINSOCKDLL_API int Socket_Connect_TCP(SOCKET sock, const char* server_ip, u_short server_port);
// 4. 发送数据（返回发送的字节数，失败返回-1）
WINSOCKDLL_API int Socket_Send(SOCKET sock, const char* buf, int len);
// 5. 接收数据（返回接收的字节数，失败返回-1，客户端断开返回0）
WINSOCKDLL_API int Socket_Recv(SOCKET sock, char* buf, int len);
// 6. 关闭Socket（返回错误码，DLL_SUCCESS表示成功）
WINSOCKDLL_API int Socket_Close(SOCKET sock);
// 7. Winsock清理（DLL卸载时自动调用，也可手动调用）
WINSOCKDLL_API void Winsock_Cleanup(void);
// 8. 获取最后一次错误码（便于主程序排查问题）
WINSOCKDLL_API int Winsock_GetLastError(void);
步骤2：创建DLL源文件（WinsockDll.c）
实现头文件中暴露的接口，整合前文Winsock函数用法，完善错误处理和资源管理。

# nclude "WinsockDll.h"

# ragma comment(lib, "ws2_32.lib") // 链接Winsock核心库
// 全局变量：存储最后一次错误码，供主程序获取
static int g_last_error = DLL_SUCCESS;
// DLL加载时自动调用（可选，用于初始化Winsock）
BOOL WINAPI DllMain(HINSTANCE hInstance, DWORD dwReason, LPVOID lpReserved) {
    switch (dwReason) {
        case DLL_PROCESS_ATTACH: // 主程序加载DLL时
            Winsock_Init(); // 自动初始化Winsock
            break;
        case DLL_PROCESS_DETACH: // 主程序卸载DLL时
            Winsock_Cleanup(); // 自动清理Winsock
            break;
        case DLL_THREAD_ATTACH:
        case DLL_THREAD_DETACH:
            break;
    }
    return TRUE;
}
// 1. Winsock初始化
WINSOCKDLL_API int Winsock_Init(void) {
    WSADATA wsaData;
    int ret = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (ret != 0) {
        g_last_error = DLL_ERR_WSASTARTUP;
        return g_last_error;
    }
    if (LOBYTE(wsaData.wVersion) != 2 || HIBYTE(wsaData.wVersion) != 2) {
        g_last_error = DLL_ERR_WSASTARTUP;
        WSACleanup();
        return g_last_error;
    }
    g_last_error = DLL_SUCCESS;
    return DLL_SUCCESS;
}
// 2. 创建TCP Socket
WINSOCKDLL_API SOCKET Socket_Create_TCP(void) {
    SOCKET sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock == INVALID_SOCKET) {
        g_last_error = DLL_ERR_SOCKET;
        return INVALID_SOCKET;
    }
    // 设置端口复用（后端必设，避免端口占用）
    int optval = 1;
    setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (char*)&optval, sizeof(optval));
    g_last_error = DLL_SUCCESS;
    return sock;
}
// 3. TCP客户端连接服务器
WINSOCKDLL_API int Socket_Connect_TCP(SOCKET sock, const char* server_ip, u_short server_port) {
    if (sock == INVALID_SOCKET || server_ip == NULL || server_port == 0) {
        g_last_error = DLL_ERR_PARAM;
        return g_last_error;
    }
    struct sockaddr_in serverAddr;
    memset(&serverAddr, 0, sizeof(serverAddr));
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(server_port);
    serverAddr.sin_addr.s_addr = inet_addr(server_ip);
    int ret = connect(sock, (struct sockaddr*)&serverAddr, sizeof(serverAddr));
    if (ret == SOCKET_ERROR) {
        g_last_error = DLL_ERR_CONNECT;
        return g_last_error;
    }
    g_last_error = DLL_SUCCESS;
    return DLL_SUCCESS;
}
// 4. 发送数据
WINSOCKDLL_API int Socket_Send(SOCKET sock, const char* buf, int len) {
    if (sock == INVALID_SOCKET || buf == NULL || len <= 0) {
        g_last_error = DLL_ERR_PARAM;
        return -1;
    }
    int send_len = send(sock, buf, len, 0);
    if (send_len == SOCKET_ERROR) {
        g_last_error = DLL_ERR_SEND;
        return -1;
    }
    g_last_error = DLL_SUCCESS;
    return send_len;
}
// 5. 接收数据
WINSOCKDLL_API int Socket_Recv(SOCKET sock, char* buf, int len) {
    if (sock == INVALID_SOCKET || buf == NULL || len <= 0) {
        g_last_error = DLL_ERR_PARAM;
        return -1;
    }
    int recv_len = recv(sock, buf, len - 1, 0); // 预留1字节存'\0'
    if (recv_len == SOCKET_ERROR) {
        g_last_error = DLL_ERR_RECV;
        return -1;
    } else if (recv_len == 0) {
        g_last_error = DLL_SUCCESS; // 客户端断开，不算错误
        return 0;
    }
    buf[recv_len] = '\0'; // 字符串结尾，避免乱码
    g_last_error = DLL_SUCCESS;
    return recv_len;
}
// 6. 关闭Socket
WINSOCKDLL_API int Socket_Close(SOCKET sock) {
    if (sock == INVALID_SOCKET) {
        g_last_error = DLL_ERR_PARAM;
        return g_last_error;
    }
    // 正常关闭连接，确保数据传输完成
    shutdown(sock, SD_BOTH);
    Sleep(100);
    int ret = closesocket(sock);
    if (ret == SOCKET_ERROR) {
        g_last_error = DLL_ERR_CLOSE;
        return g_last_error;
    }
    g_last_error = DLL_SUCCESS;
    return DLL_SUCCESS;
}
// 7. Winsock清理
WINSOCKDLL_API void Winsock_Cleanup(void) {
    WSACleanup();
    g_last_error = DLL_SUCCESS;
}
// 8. 获取最后一次错误码
WINSOCKDLL_API int Winsock_GetLastError(void) {
    return g_last_error;
}
步骤3：编译DLL（VS操作）
1. 打开VS，创建“动态链接库（DLL）”项目，命名为WinsockDll；
2. 将上述头文件（WinsockDll.h）和源文件（WinsockDll.c）添加到项目中；
3. 项目属性设置：① 配置类型设为“动态链接库(.dll)”；② 字符集设为“多字节字符集”（避免编码问题）；③ 确保链接器中添加了ws2_32.lib；
4. 编译项目，生成WinsockDll.dll（Debug/Release目录下）和WinsockDll.lib（导入库，Debug/Release目录下）。
    环节2：静态加载WinSock DLL（主程序实战，TCP客户端）
    目标：主程序通过静态加载方式，调用上述DLL的接口，实现TCP客户端功能，连接服务器并完成数据交互，贴合后端客户端开发场景。
    步骤1：创建主程序项目（TCP客户端）
    1. 打开VS，创建“控制台应用”项目，命名为WinsockDll_Client；
    2. 将WinsockDll.h头文件、WinsockDll.lib导入库，复制到主程序项目目录下；
    3. 将WinsockDll.dll复制到主程序的Debug/Release目录下（确保程序运行时能找到DLL）。
    步骤2：主程序代码（WinsockDll_Client.c）

# nclude <stdio.h>

# nclude <string.h>

# nclude "WinsockDll.h" // 包含DLL头文件
// 链接DLL导入库（静态加载核心）

# ragma comment(lib, "WinsockDll.lib")

# efine SERVER_IP "127.0.0.1"  // 服务器IP（本地调试）

# efine SERVER_PORT 8080       // 服务器端口

# efine BUF_SIZE 1024          // 缓冲区大小
int main() {
    SOCKET clientSock;
    int ret, recv_len, send_len;
    char sendBuf[BUF_SIZE] = {0};
    char recvBuf[BUF_SIZE] = {0};
    // 1. 调用DLL接口，初始化Winsock（也可依赖DLL自动初始化）
    ret = Winsock_Init();
    if (ret != DLL_SUCCESS) {
        printf("Winsock初始化失败，错误码：%d（DLL错误）\n", Winsock_GetLastError());
        return 1;
    }
    printf("Winsock初始化成功（通过DLL）\n");
    // 2. 调用DLL接口，创建TCP Socket
    clientSock = Socket_Create_TCP();
    if (clientSock == INVALID_SOCKET) {
        printf("Socket创建失败，错误码：%d（DLL错误）\n", Winsock_GetLastError());
        Winsock_Cleanup();
        return 1;
    }
    printf("Socket创建成功，句柄：%d\n", clientSock);
    // 3. 调用DLL接口，连接服务器（需先启动TCP服务器）
    ret = Socket_Connect_TCP(clientSock, SERVER_IP, SERVER_PORT);
    if (ret != DLL_SUCCESS) {
        printf("连接服务器失败，错误码：%d（DLL错误）\n", Winsock_GetLastError());
        Socket_Close(clientSock);
        Winsock_Cleanup();
        return 1;
    }
    printf("连接服务器成功（%s:%d）\n", SERVER_IP, SERVER_PORT);
    // 4. 调用DLL接口，发送数据
    printf("请输入要发送的消息：");
    fgets(sendBuf, BUF_SIZE, stdin);
    sendBuf[strlen(sendBuf) - 1] = '\0'; // 去掉换行符
    send_len = Socket_Send(clientSock, sendBuf, strlen(sendBuf));
    if (send_len == -1) {
        printf("发送数据失败，错误码：%d（DLL错误）\n", Winsock_GetLastError());
        Socket_Close(clientSock);
        Winsock_Cleanup();
        return 1;
    }
    printf("数据发送成功，发送长度：%d字节\n", send_len);
    // 5. 调用DLL接口，接收数据
    recv_len = Socket_Recv(clientSock, recvBuf, BUF_SIZE);
    if (recv_len == -1) {
        printf("接收数据失败，错误码：%d（DLL错误）\n", Winsock_GetLastError());
        Socket_Close(clientSock);
        Winsock_Cleanup();
        return 1;
    } else if (recv_len == 0) {
        printf("服务器主动断开连接\n");
        Socket_Close(clientSock);
        Winsock_Cleanup();
        return 1;
    }
    printf("收到服务器响应：%s（长度：%d字节）\n", recvBuf, recv_len);
    // 6. 调用DLL接口，关闭Socket
    ret = Socket_Close(clientSock);
    if (ret != DLL_SUCCESS) {
        printf("关闭Socket失败，错误码：%d（DLL错误）\n", Winsock_GetLastError());
        Winsock_Cleanup();
        return 1;
    }
    // 7. 调用DLL接口，清理Winsock（也可依赖DLL自动清理）
    Winsock_Cleanup();
    printf("程序退出，所有资源已释放\n");
    return 0;
}
步骤3：运行测试
1. 启动前文的TCP服务器（如例程4的综合服务器）；
2. 编译并运行主程序，输入消息，即可完成与服务器的连接和数据交互；
3. 若运行失败，通过Winsock_GetLastError()获取DLL错误码，排查问题（如服务器未启动、端口占用）。
    环节3：动态加载WinSock DLL（主程序实战，TCP服务器）
    目标：主程序通过动态加载方式，调用DLL接口，实现TCP服务器的核心功能（绑定、监听、接收连接、数据交互），灵活适配不同DLL版本，贴合后端服务器开发场景。
    步骤1：主程序代码（WinsockDll_Server.c）

# nclude <stdio.h>

# nclude <string.h>

# nclude <winsock2.h>
// 定义DLL接口函数指针（与DLL暴露的接口一一对应）
typedef int (*PFN_Winsock_Init)(void);
typedef SOCKET (*PFN_Socket_Create_TCP)(void);
typedef int (*PFN_Socket_Connect_TCP)(SOCKET, const char*, u_short);
typedef int (*PFN_Socket_Send)(SOCKET, const char*, int);
typedef int (*PFN_Socket_Recv)(SOCKET, char*, int);
typedef int (*PFN_Socket_Close)(SOCKET);
typedef void (*PFN_Winsock_Cleanup)(void);
typedef int (*PFN_Winsock_GetLastError)(void);

# efine SERVER_PORT 8080       // 服务器端口

# efine BUF_SIZE 1024          // 缓冲区大小

# efine DLL_PATH "WinsockDll.dll" // DLL路径（可自定义）
int main() {
    // 1. 动态加载DLL
    HINSTANCE hDll = LoadLibraryA(DLL_PATH);
    if (hDll == NULL) {
        printf("动态加载DLL失败，错误码：%d\n", GetLastError());
        return 1;
    }
    printf("DLL动态加载成功（路径：%s）\n", DLL_PATH);
    // 2. 获取DLL接口函数指针（核心：与DLL接口名一致）
    PFN_Winsock_Init Winsock_Init = (PFN_Winsock_Init)GetProcAddress(hDll, "Winsock_Init");
    PFN_Socket_Create_TCP Socket_Create_TCP = (PFN_Socket_Create_TCP)GetProcAddress(hDll, "Socket_Create_TCP");
    PFN_Socket_Send Socket_Send = (PFN_Socket_Send)GetProcAddress(hDll, "Socket_Send");
    PFN_Socket_Recv Socket_Recv = (PFN_Socket_Recv)GetProcAddress(hDll, "Socket_Recv");
    PFN_Socket_Close Socket_Close = (PFN_Socket_Close)GetProcAddress(hDll, "Socket_Close");
    PFN_Winsock_Cleanup Winsock_Cleanup = (PFN_Winsock_Cleanup)GetProcAddress(hDll, "Winsock_Cleanup");
    PFN_Winsock_GetLastError Winsock_GetLastError = (PFN_Winsock_GetLastError)GetProcAddress(hDll, "Winsock_GetLastError");
    // 校验接口指针（避免空指针调用）
    if (!Winsock_Init || !Socket_Create_TCP || !Socket_Send || !Socket_Recv || !Socket_Close || !Winsock_Cleanup || !Winsock_GetLastError) {
        printf("获取DLL接口失败，错误码：%d\n", GetLastError());
        FreeLibrary(hDll); // 释放DLL
        return 1;
    }
    // 3. 调用DLL接口，初始化Winsock
    int ret = Winsock_Init();
    if (ret != 0) {
        printf("Winsock初始化失败，DLL错误码：%d\n", Winsock_GetLastError());
        FreeLibrary(hDll);
        return 1;
    }
    printf("Winsock初始化成功（动态加载DLL）\n");
    // 4. 调用DLL接口，创建TCP Socket（服务器监听Socket）
    SOCKET serverSock = Socket_Create_TCP();
    if (serverSock == INVALID_SOCKET) {
        printf("创建监听Socket失败，DLL错误码：%d\n", Winsock_GetLastError());
        Winsock_Cleanup();
        FreeLibrary(hDll);
        return 1;
    }
    // 绑定本地地址（服务器必做，DLL未封装，直接调用Winsock函数）
    struct sockaddr_in serverAddr;
    memset(&serverAddr, 0, sizeof(serverAddr));
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(SERVER_PORT);
    serverAddr.sin_addr.s_addr = htonl(INADDR_ANY);
    ret = bind(serverSock, (struct sockaddr*)&serverAddr, sizeof(serverAddr));
    if (ret == SOCKET_ERROR) {
        printf("绑定失败，错误码：%d\n", WSAGetLastError());
        Socket_Close(serverSock);
        Winsock_Cleanup();
        FreeLibrary(hDll);
        return 1;
    }
    // 监听客户端连接（DLL未封装，直接调用Winsock函数）
    ret = listen(serverSock, 5);
    if (ret == SOCKET_ERROR) {
        printf("监听失败，错误码：%d\n", WSAGetLastError());
        Socket_Close(serverSock);
        Winsock_Cleanup();
        FreeLibrary(hDll);
        return 1;
    }
    printf("服务器启动成功，监听端口：%d，等待客户端连接...\n", SERVER_PORT);
    // 接收客户端连接（循环接收）
    SOCKET clientSock;
    struct sockaddr_in clientAddr;
    int addrLen = sizeof(clientAddr);
    char recvBuf[BUF_SIZE] = {0};
    char sendBuf[BUF_SIZE] = {0};
    int recv_len, send_len;
    while (1) {
        clientSock = accept(serverSock, (struct sockaddr*)&clientAddr, &addrLen);
        if (clientSock == INVALID_SOCKET) {
            printf("接收连接失败，错误码：%d\n", WSAGetLastError());
            continue;
        }
        printf("\n客户端连接成功，IP：%s，端口：%d\n", inet_ntoa(clientAddr.sin_addr), ntohs(clientAddr.sin_port));
        // 调用DLL接口，接收客户端数据
        recv_len = Socket_Recv(clientSock, recvBuf, BUF_SIZE);
        if (recv_len == -1) {
            printf("接收数据失败，DLL错误码：%d\n", Winsock_GetLastError());
            Socket_Close(clientSock);
            continue;
        } else if (recv_len == 0) {
            printf("客户端主动断开连接\n");
            Socket_Close(clientSock);
            continue;
        }
        printf("收到客户端数据：%s（长度：%d字节）\n", recvBuf, recv_len);
        // 调用DLL接口，发送响应
        sprintf(sendBuf, "服务器已收到数据：%s", recvBuf);
        send_len = Socket_Send(clientSock, sendBuf, strlen(sendBuf));
        if (send_len == -1) {
            printf("发送响应失败，DLL错误码：%d\n", Winsock_GetLastError());
            Socket_Close(clientSock);
            continue;
        }
        printf("响应发送成功，长度：%d字节\n", send_len);
        // 关闭客户端Socket
        Socket_Close(clientSock);
        printf("客户端连接已关闭\n");
        // 退出条件
        if (strcmp(recvBuf, "exit") == 0) {
            printf("收到退出指令，服务器即将退出\n");
            break;
        }
    }
    // 5. 资源释放（顺序：关闭服务器Socket→清理Winsock→释放DLL）
    Socket_Close(serverSock);
    Winsock_Cleanup();
    FreeLibrary(hDll); // 动态加载必须释放DLL
    printf("服务器退出，所有资源已释放\n");
    return 0;
}
步骤2：运行测试
1. 将WinsockDll.dll复制到主程序的Debug/Release目录下（确保LoadLibrary能找到DLL）；
2. 编译并运行主程序，启动服务器；
3. 运行环节2的客户端程序，即可完成客户端与服务器的通信（通过DLL接口实现）；
4. 若需切换DLL版本，只需替换WinsockDll.dll文件，无需重新编译主程序，体现动态加载的灵活性。
    三、WinSock DLL后端实战避坑指南（重点必看）
    结合上述实战，总结后端开发中WinSock DLL的常见坑点及解决方案，确保DLL稳定、可复用，避免出现资源泄漏、接口调用失败等问题。
    1. DLL加载失败（最常见坑）
    问题表现：主程序静态加载时提示“无法解析的外部符号”，动态加载时LoadLibrary返回NULL。
    解决方案：
    静态加载：确保主程序已关联DLL的.lib导入库，且.lib文件路径正确；DLL头文件中的导出宏（WINSOCKDLL_API）必须正确定义，接口名称与DLL中的一致。
    动态加载：确保DLL路径正确（相对路径需放在主程序目录下，绝对路径需写完整）；GetProcAddress获取接口时，接口名称必须与DLL中暴露的名称完全一致（区分大小写）。
2. Winsock初始化/清理异常
    问题表现：DLL中初始化Winsock后，主程序调用其他Winsock函数失败；或程序退出后，端口仍被占用（资源泄漏）。
    解决方案：
    统一管理：在DLL的DllMain函数中，实现“加载时初始化、卸载时清理”，避免主程序重复调用WSAStartup/WSACleanup。
    异常处理：DLL中初始化失败后，需及时调用WSACleanup清理资源，避免资源泄漏。
3. Socket资源泄漏
    问题表现：程序运行一段时间后，端口被占满、内存飙升，无法创建新的Socket。
    解决方案：
    DLL中必须提供Socket关闭接口（如Socket_Close），主程序调用DLL创建Socket后，必须调用该接口关闭。
    Socket_Close接口中，需先调用shutdown关闭连接，再调用closesocket关闭Socket，确保数据传输完成和资源释放。
4. 线程安全问题
    问题表现：多线程主程序调用DLL接口时，出现Socket状态错乱、数据收发异常。
    解决方案：
    DLL接口中，对共享资源（如全局错误码g_last_error）添加互斥锁（如CriticalSection），避免多线程同时修改。
    每个线程使用独立的Socket句柄，避免多线程共用同一个Socket，导致操作冲突。
5. 编码与字符集问题
    问题表现：主程序与DLL之间传递字符串（如IP地址、消息）时，出现乱码。
    解决方案：
    统一字符集：DLL和主程序的项目属性中，均设置为“多字节字符集”，避免Unicode与多字节编码不兼容。
    字符串结尾处理：接收数据时，预留1字节存'\0'，确保字符串正常显示（如DLL中Socket_Recv接口的处理）。
    四、WinSock DLL后端进阶方向
    掌握上述基础实战后，可结合后端业务需求，对WinSock DLL进行优化，提升其稳定性和实用性：
    扩展接口：封装UDP通信接口（Socket_Create_UDP、Socket_SendTo、Socket_RecvFrom），适配UDP场景（如心跳、日志传输）。
    添加加密功能：在DLL中整合SSL/TLS加密逻辑，实现加密通信（如HTTPS后端），提升数据安全性。
    日志模块：在DLL中添加日志打印功能，记录接口调用过程、错误信息，便于后端调试和问题排查。
    版本管理：在DLL中添加版本接口（如Winsock_GetDllVersion），便于主程序识别DLL版本，实现版本兼容。
    高并发适配：在DLL中封装IOCP模型接口，支持高并发连接，适配后端高并发场景（如万级客户端连接）。
