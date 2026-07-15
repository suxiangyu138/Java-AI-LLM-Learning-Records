# WinSock 期末知识点总结

> **定位**：WinSock（Windows Sockets）是 Windows 平台下实现 TCP/IP 协议的编程接口。本总结围绕期末高频考点、核心概念、编程流程、关键 API 及易错点展开。

---

## 目录

1. [核心基础概念](#1-核心基础概念)
2. [TCP/UDP 编程流程](#2-tcpudp-编程流程)
3. [核心 API 详解](#3-核心-api-详解)
4. [字节序转换](#4-字节序转换)
5. [易错点与常见错误](#5-易错点与常见错误)
6. [高频考点汇总](#6-高频考点汇总)

---

## 1. 核心基础概念

### 1.1 WinSock 定义

> Windows Sockets 规范——Windows 系统下用于网络通信的编程接口（API 集合），基于 BSD Socket，适配 Windows 消息机制和多线程。

### 1.2 TCP vs UDP

| 维度 | TCP | UDP |
|------|-----|-----|
| 连接方式 | 面向连接 | 无连接 |
| 可靠性 | ✅ 可靠 | ❌ 不可靠 |
| 传输方式 | 面向字节流 | 面向数据报 |
| 拥塞控制 | ✅ 有 | ❌ 无 |
| WinSock 类型 | `SOCK_STREAM` | `SOCK_DGRAM` |
| 应用场景 | 文件传输、聊天 | 视频直播、广播 |

### 1.3 套接字（Socket）

| 类型 | 宏 | 协议 | 特点 |
|------|-----|------|------|
| 流式套接字 | `SOCK_STREAM` | TCP | 可靠、有序字节流 |
| 数据报套接字 | `SOCK_DGRAM` | UDP | 无连接、不可靠 |

> Socket = IP + 端口（1-65535，知名端口 1-1023，自定义用 1024+）

### 1.4 字节序

| 类型 | 存储方式 | 使用场景 |
|------|----------|----------|
| **大端序**（网络字节序） | 高位字节存低地址 | 网络传输标准 |
| **小端序**（主机字节序） | 高位字节存高地址 | Windows 主机默认 |

---

## 2. TCP/UDP 编程流程

### 2.1 TCP 编程流程

```text
服务器（被动连接）                    客户端（主动连接）
───────────────                     ───────────────
WSAStartup()                        WSAStartup()
socket()                            socket()
bind()                                  │
listen()                                │
accept() ←──────── 阻塞等待 ──────── connect()
    │                                     │
recv() ←────────── 数据 ──────────── send()
send() ─────────── 数据 ────────────→ recv()
    │                                     │
closesocket()                       closesocket()
WSACleanup()                        WSACleanup()
```

**关键注意**：
- `accept()` 返回**新套接字**用于通信，原套接字继续监听
- 必须先启动服务器，客户端才能 `connect()`
- `closesocket()` 按"通信套接字 → 监听套接字"顺序关闭

### 2.2 UDP 编程流程（简化）

| 步骤 | 服务器 | 客户端 |
|------|--------|--------|
| 1 | `WSAStartup()` | `WSAStartup()` |
| 2 | `socket(SOCK_DGRAM)` | `socket(SOCK_DGRAM)` |
| 3 | `bind()` | 可选 `bind()` |
| 4 | `recvfrom()` / `sendto()` | `sendto()` / `recvfrom()` |
| 5 | `closesocket()` + `WSACleanup()` | 同左 |

> UDP 无需 `listen()` / `accept()` / `connect()`，收发用 `recvfrom()` / `sendto()`，需指定对方 IP 和端口。

---

## 3. 核心 API 详解

### 3.1 API 速查表

| API | 用途 | 关键参数 | 返回值 |
|-----|------|----------|--------|
| `WSAStartup()` | 初始化 WinSock 库 | `MAKEWORD(2,2), &wsaData` | 0=成功 |
| `socket()` | 创建套接字 | `AF_INET, SOCK_STREAM/DGRAM, protocol` | 套接字句柄 / `INVALID_SOCKET` |
| `bind()` | 绑定 IP 和端口 | 套接字, `sockaddr_in*`, `sizeof` | 0=成功 |
| `listen()` | 监听（TCP 服务器） | 套接字, `backlog`（通常 5） | 0=成功 |
| `accept()` | 接受连接（TCP 服务器） | 套接字, `sockaddr*`, `addrlen*` | 新套接字 / `INVALID_SOCKET` |
| `connect()` | 发起连接（TCP 客户端） | 套接字, 服务器 `sockaddr_in*`, `sizeof` | 0=成功 |
| `send()` | 发送数据（TCP） | 套接字, `buf`, `len`, 0 | 发送字节数 / `SOCKET_ERROR` |
| `recv()` | 接收数据（TCP） | 套接字, `buf`, `len`, 0 | 接收字节数（0=对方关闭） |
| `sendto()` | 发送数据（UDP） | + `sockaddr*` 目标地址 | 发送字节数 |
| `recvfrom()` | 接收数据（UDP） | + `sockaddr*` 获取来源地址 | 接收字节数 |
| `closesocket()` | 关闭套接字 | 套接字句柄 | 0=成功 |
| `WSACleanup()` | 清理 WinSock 库 | 无 | 0=成功 |

### 3.2 WSAStartup 示例

```c
WSADATA wsaData;
int ret = WSAStartup(MAKEWORD(2, 2), &wsaData);
if (ret != 0) {
    // 初始化失败，常见：WSAEINVAL（版本不支持）
}
```

---

## 4. 字节序转换

> 必背 4 个函数，考试必考。

| 函数 | 方向 | 位数 | 用途 |
|------|------|:----:|------|
| `htons()` | 主机 → 网络 | 16 位 | **端口**转换 |
| `ntohs()` | 网络 → 主机 | 16 位 | 端口转换 |
| `htonl()` | 主机 → 网络 | 32 位 | **IP 地址**转换 |
| `ntohl()` | 网络 → 主机 | 32 位 | IP 地址转换 |

> 口诀：**h**ost **to** **n**etwork **s**hort（端口 16 位）/ **l**ong（IP 32 位）

---

## 5. 易错点与常见错误

| 错误 | 原因 | 解决 |
|------|------|------|
| **初始化失败** | 未调用 `WSAStartup()` / 版本号错误 | 确保先调用 `WSAStartup(MAKEWORD(2,2), &wsaData)` |
| **bind 失败** | 端口被占用 / IP 错误 / 套接字无效 | 换端口、检查 IP、判断 `socket()` 返回值 |
| **connect 失败** | 服务器未启动 / IP 端口错误 / 网络不通 | 检查服务器状态、核对 IP 和端口 |
| **收发失败** | 套接字无效 / TCP 未连接 / 缓冲区不足 | 检查句柄、确保连接建立、`recv()` 返回 0=对方关闭 |
| **资源泄漏** | 未 `closesocket()` / 未 `WSACleanup()` | 关闭所有套接字 → 再 `WSACleanup()` |
| **字节序错误** | IP/端口未转网络字节序 | 端口用 `htons()`，IP 用 `htonl()` |

---

## 6. 高频考点汇总

| # | 考点 |
|---|------|
| 1 | WinSock 定义、与 BSD Socket 关系 |
| 2 | TCP 与 UDP 核心区别（5 维度） |
| 3 | 套接字两种类型及对应协议 |
| 4 | TCP 客户端/服务器完整编程流程（步骤顺序） |
| 5 | 核心 API 的功能、参数、返回值 |
| 6 | 字节序转换 4 个函数 |
| 7 | 常见错误及解决方法 |

---

> 🎯 **备考重点**：理论（概念+API+协议区别+易错点）+ 实操（能独立编写 TCP 通信程序）。TCP 服务器/客户端编程流程是最核心考点，步骤顺序不能出错。
