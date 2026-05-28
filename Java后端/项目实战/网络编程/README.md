# 网络编程 (Network Programming)

> Java 网络编程实战练习：TCP Socket、HTTP 通信、JavaMail 邮件发送

## 项目概述

Java 网络编程基础实践项目，涵盖三大核心网络通信场景：TCP Socket 编程、HTTP 数据获取和 SMTP 邮件发送。通过实际编码掌握 Java 网络 I/O、协议通信和数据交换。

## 技术栈

| 技术 | 说明 |
|------|------|
| Java | JDK 8+ |
| Maven | 项目管理与依赖 |
| java.net.Socket | TCP 客户端/服务端通信 |
| java.net.HttpURLConnection | HTTP 请求与响应处理 |
| JavaMail API | SMTP 邮件发送 |
| I/O Streams | 字节流/字符流网络数据传输 |

## 功能特性

- **TCP 服务端**：多线程 Socket Server，接收并响应客户端请求
- **TCP 客户端**：连接服务端，发送消息并接收响应
- **HTTP 数据获取**：使用 HttpURLConnection 请求 Web API 或网页数据
- **邮件发送**：通过 SMTP 协议发送电子邮件（需配置邮箱账号）

## 项目结构

```
网络编程/
├── src/
│   └── main/
│       └── java/
│           └── network/
│               ├── TCPServer.java       # TCP 服务端
│               ├── SendEmailDemo.java   # 邮件发送示例
│               └── WebDataDemo.java     # HTTP 数据获取示例
├── target/                      # Maven 构建输出
├── pom.xml                      # Maven 配置（含 JavaMail 依赖）
└── README.md
```

## 快速开始

```bash
# 编译项目
mvn compile

# 运行 TCP 服务端
java -cp target/classes network.TCPServer

# 运行邮件发送（需先配置邮箱账号密码）
java -cp target/classes network.SendEmailDemo

# 运行 HTTP 数据获取
java -cp target/classes network.WebDataDemo
```

## 核心知识点

| 知识点 | 应用 |
|--------|------|
| ServerSocket | 服务端监听端口，accept() 等待连接 |
| Socket | 客户端连接，getInputStream/getOutputStream |
| TCP 三次握手 | 连接建立过程理解 |
| I/O 流 | BufferedReader/PrintWriter 网络数据读写 |
| HttpURLConnection | HTTP GET/POST 请求，响应码处理 |
| SMTP 协议 | 邮件发送流程（MIME 格式） |
| JavaMail API | MimeMessage、Transport.send() |
| 多线程 | 服务端为每个客户端分配独立线程 |
| try-with-resources | 自动关闭网络资源 |

## 注意事项

- TCP 编程务必在 finally 块中关闭 Socket 资源，或使用 try-with-resources
- 邮件发送需要开启邮箱的 SMTP 服务并获取授权码（非登录密码）
- HttpURLConnection 在 Java 11+ 可替换为 HttpClient API
- 网络操作需处理 IOException 和 UnknownHostException
