03.20 12:27
WinSock快速参考
本文整合前文WinSock2核心用法、调试规范、平台适配、可选特性及TCP/IP协议首部相关知识点，提炼高频考点、核心接口、调试技巧，作为快速查阅手册，方便后端开发调试时快速检索，全程贴合前文内容，确保上下文流畅一致。
一、核心基础（必记）
1. 版本与初始化（WinSock2）
主流版本：WinSock 2.2（唯一推荐，MAKEWORD(2,2)），兼容所有Windows主流版本（Win7/Win10/Win11）
核心接口：WSAStartup（初始化）、WSACleanup（资源清理），必须成对调用
调试重点：初始化返回值≠0则失败，立即调用WSAGetLastError()获取错误码
2. 核心数据类型
Socket句柄：SOCKET，无效句柄=INVALID_SOCKET（≠-1）
地址结构体：sockaddr_in（IPv4），核心字段：sin_family=AF_INET、sin_port（需htons转换）、sin_addr（IP地址）
错误码：WSAGetLastError()，仅在接口调用失败后立即调用才有效
3. 字节序转换（必用）
端口：htons（主机字节序→网络字节序）、ntohs（网络字节序→主机字节序）
IP地址：inet_addr（字符串IP→网络字节序）、inet_ntoa（网络字节序→字符串IP）
二、核心接口（按流程排序，高频使用）
接口名称
核心功能
关键注意事项（衔接前文）
WSAStartup
初始化WinSock2
版本必须为2.2，失败则直接退出，需调用WSACleanup
socket
创建Socket（TCP/UDP）
类型：SOCK_STREAM（TCP）、SOCK_DGRAM（UDP），返回INVALID_SOCKET则失败
bind
服务器绑定IP和端口
端口需htons转换，失败大概率是端口占用（错误码10048），可设SO_REUSEADDR
listen
服务器监听连接
仅TCP可用，第二个参数为最大监听队列长度
accept
服务器接收客户端连接
阻塞模式下会等待连接，返回新的Socket句柄，需单独关闭
connect
客户端连接服务器
需指定服务器IP和端口，失败检查网络、防火墙（错误码10060/10061）
send/recv
TCP数据收发
返回值为收发字节数，失败返回SOCKET_ERROR，大数据量需循环收发
sendto/recvfrom
UDP数据收发
需指定目标IP和端口，UDP无连接，需手动处理丢包重传
closesocket
关闭Socket句柄
必须调用，避免资源泄漏，不可重复关闭（错误码10038）
WSACleanup
清理WinSock资源
程序退出前必调用，与WSAStartup成对出现
三、可选特性（按需启用，核心要点）
特性名称
核心功能
适用场景
调试注意事项
SO_REUSEADDR
端口复用
服务器频繁重启调试
bind前启用，检查setsockopt返回值
FIONBIO（非阻塞）
Socket非阻塞模式
高并发场景
WinSock用ioctlsocket，BSD用fcntl，需处理错误码10035
SO_BROADCAST
UDP广播通信
局域网设备发现
仅UDP可用，需关闭防火墙，目标IP设为广播地址
TCP_NODELAY
禁用Nagle算法
实时性要求高的TCP通信
仅TCP可用，调试时抓包确认小数据包立即发送
IOCP（完成端口）
WinSock高并发
万级客户端连接
Windows特有，需释放完成端口句柄，避免资源泄漏
四、调试核心要点（快速排查问题）
1. 常用工具
VS调试：断点（核心接口）、监视窗口（变量、错误码）、调用堆栈（崩溃排查）
系统命令：netstat -ano（端口占用）、ping/telnet（网络连通）、netsh winsock reset（重置配置）
第三方工具：Wireshark（抓包，查看协议首部）、Dependency Walker（DLL依赖）
2. 高频错误码（必记）
10048（WSAEADDRINUSE）：端口占用→netstat排查进程
10054（WSAECONNRESET）：连接被重置→检查对方程序是否正常
10060（WSAETIMEDOUT）：连接超时→检查IP/端口、网络
10061（WSAECONNREFUSED）：连接被拒绝→服务器未启动
10093（WSANOTINITIALISED）：未初始化WinSock→检查WSAStartup
3. 避坑重点
不混用TCP/UDP接口（如TCP用sendto），不混用WinSock与BSD接口
DLL加载需匹配位数（32/64位），动态加载需检查接口指针非NULL
跨平台移植需用#ifdef _WIN32区分，避免接口混用
数据收发后需校验长度，UDP需处理丢包，TCP需处理粘包
五、TCP/IP协议首部（调试关联重点）
IP首部（20字节）：重点看源/目标IP、协议字段（TCP=6，UDP=17）、TTL（连接超时排查）
TCP首部（20字节）：重点看源/目标端口、标志位（SYN=连接，RST=重置）、序号/确认号（数据乱序排查）
UDP首部（8字节）：重点看源/目标端口、数据报长度（避免超长丢弃）
调试技巧：用Wireshark抓包，对照首部字段定位连接、数据异常
六、平台适配要点
Windows平台：以管理员身份运行调试程序，适配32/64位，关闭防火墙
跨平台移植（BSD→WinSock）：接口适配（close→closesocket）、条件编译区分平台
DLL适配：与主程序位数一致，静态加载需链接.lib，动态加载需检查路径

