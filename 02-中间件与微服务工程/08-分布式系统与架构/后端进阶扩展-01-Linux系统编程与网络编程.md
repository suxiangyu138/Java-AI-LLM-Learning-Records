# Linux 系统编程 & 网络编程

## 📌 定位
**课外自学 | 第一梯队优先级 | 后端·中间件开发必学**

操作系统课讲理论，这门课教实战——直接和Linux内核提供的系统调用打交道，理解文件、进程、Socket、信号的底层行为。

## 🎯 核心章节

### 1. 文件 I/O 与系统调用
- **系统调用 vs 库函数**：`open/read/write/close`（系统调用，进入内核态）vs `fopen/fread/fwrite/fclose`（C库，用户态缓冲）
- **文件描述符（fd）**：非负整数，内核为每个进程维护的打开文件表索引。0=stdin, 1=stdout, 2=stderr
- **阻塞 vs 非阻塞 I/O**：`fcntl(fd, F_SETFL, O_NONBLOCK)`——设置非阻塞标志
- **mmap 内存映射**：文件直接映射到虚拟内存，零拷贝技术的基础（Kafka高性能的秘密）

### 2. 进程管理与IPC
- **fork + exec**：创建子进程+替换进程映像——Linux创建新进程的唯一方式
- **孤儿进程/僵尸进程**：父先死→孤儿被init收养；子先死父未回收→僵尸（`waitpid`回收）
- **守护进程（Daemon）**：脱离终端、后台运行——`nohup` + `setsid()` + 切换工作目录
- **进程间通信（IPC实战）**：
  - 匿名管道：`pipe(fd)`——父子进程单向通信
  - 命名管道（FIFO）：`mkfifo`——任意进程间通信
  - 共享内存 + 信号量：最快的IPC方式——`shmget/shmat` + `sem_wait/sem_post`
  - 消息队列：`msgget/msgsnd/msgrcv`

### 3. 信号处理
- **常用信号**：SIGINT(Ctrl+C)、SIGKILL(-9,无法捕获)、SIGTERM(-15,优雅终止)、SIGCHLD(子进程状态变化)、SIGPIPE(向已关闭管道写)
- **信号处理函数**：`signal/sigaction`——注意信号处理函数中可安全调用的函数（异步信号安全）
- **实战**：优雅关闭——捕获SIGTERM→释放资源→保存状态→退出

### 4. Socket 网络编程（⭐ 核心）
- **TCP Socket 流程**：
  ```
  Server: socket()→bind()→listen()→accept()→read()/write()→close()
  Client: socket()→connect()→write()/read()→close()
  ```
- **关键函数**：
  - `socket(AF_INET, SOCK_STREAM, 0)`——创建IPv4 TCP套接字
  - `bind/listen/accept`——绑定地址+监听+接受连接
  - `send/recv`——带flag的数据收发（比read/write更精细的控制）
  - `setsockopt`——SO_REUSEADDR(端口重用)、SO_KEEPALIVE(心跳检测)

### 5. I/O 多路复用（⭐ 高并发基石）
- **select**：fd_set位图，1024限制，O(n)轮询——每次调用需重新传入fd集合
- **poll**：pollfd数组，无数量限制——但仍O(n)轮询，且需在内核态和用户态间拷贝
- **epoll（Linux王牌）**：
  - `epoll_create`→`epoll_ctl(EPOLL_CTL_ADD)`→`epoll_wait`
  - 红黑树管理所有fd + 就绪链表（rdlist）直接返回就绪fd——O(1)
  - LT（水平触发，默认）：只要fd就绪，每次epoll_wait都会通知
  - ET（边缘触发，高效）：只在状态变化时通知一次——必须配合非阻塞I/O
- **Reactor 模式**：主线程epoll_wait监听事件→分配给Worker线程处理——Netty/Nginx的核心模式

### 6. 高性能网络编程技巧
- **零拷贝**：`sendfile/splice`——数据在内核态直接传输，不经过用户态
- **I/O 向量化**：`readv/writev`——一次系统调用读写多个缓冲区
- **C10K/ C10M 问题**：连接数上万后，线程模型不够→事件驱动+非阻塞+epoll

## ✅ 学习建议
- **必做项目**：手写一个基于epoll的多线程TCP服务器（echo server→HTTP server→简易Redis）
- 推荐：《UNIX环境高级编程》(APUE) + 《Linux/UNIX系统编程手册》(TLPI)
- 理解阻塞/非阻塞/同步/异步的区别——面试必问
- 选做：用C语言实现简易HTTP服务器，对比nginx的并发处理模型
