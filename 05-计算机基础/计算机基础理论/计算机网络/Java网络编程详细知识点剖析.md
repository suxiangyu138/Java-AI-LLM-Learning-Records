Java网络编程详细知识点剖析
一、Java网络编程核心基础
1.1 网络编程核心概念
Java网络编程的本质是实现不同设备（或同一设备不同进程）之间的数据传输，其核心依赖于TCP/IP协议簇。关键概念如下：
IP地址：设备在网络中的唯一标识，分为IPv4（32位，如192.168.1.1）和IPv6（128位，解决IPv4地址枯竭问题）。Java中通过InetAddress类封装IP地址及主机名相关操作。
端口号：同一设备上不同进程的唯一标识，范围0-65535，其中0-1023为系统端口（如80端口对应HTTP、443对应HTTPS），1024-49151为注册端口，49152-65535为临时端口。
协议：规定数据传输的格式、速率、时序等规则，Java网络编程主要基于TCP和UDP两种传输层协议。
Socket（套接字）：网络通信的端点，是Java实现网络连接的核心工具，封装了IP地址和端口号，分为客户端Socket和服务器端ServerSocket。
1.2 TCP/IP协议簇核心分层
Java网络编程基于TCP/IP协议簇的应用层、传输层、网络层、数据链路层（物理层）四层模型（简化自OSI七层模型），各层职责及Java相关实现如下：
应用层：提供具体的网络应用服务，如HTTP、FTP、SMTP等。Java中常用的应用层协议实现有HttpURLConnection、HttpClient（Java 11+）。
传输层：负责端到端的数据传输，核心协议为TCP和UDP。Java中通过Socket（TCP）、DatagramSocket（UDP）封装该层功能。
网络层：负责IP地址解析、路由选择，核心协议为IP。Java中通过InetAddress、InetSocketAddress类操作该层相关信息。
数据链路层/物理层：负责数据的物理传输（如网线、无线信号），由操作系统和硬件实现，Java无需直接操作。
二、传输层协议：TCP与UDP详解
2.1 TCP协议（面向连接、可靠传输）
2.1.1 TCP核心特性
TCP（Transmission Control Protocol，传输控制协议）是面向连接、可靠、面向字节流的传输协议，适用于对数据可靠性要求高的场景（如文件传输、登录验证），核心特性：
面向连接：通信前必须通过“三次握手”建立连接，通信结束后通过“四次挥手”关闭连接。
可靠传输：通过序列号、确认应答（ACK）、重传机制、流量控制（滑动窗口）、拥塞控制，确保数据不丢失、不重复、有序到达。
面向字节流：将数据视为连续的字节序列，无固定数据包大小，由应用层自行划分数据边界。
2.1.2 TCP三次握手与四次挥手
三次握手（建立连接）
目的是确认双方的发送和接收能力，流程如下：
客户端 → 服务器：发送SYN（同步）报文，请求建立连接，携带初始序列号（seq=x）。
服务器 → 客户端：发送SYN+ACK（同步+确认）报文，确认客户端请求，携带自己的初始序列号（seq=y）和对客户端的确认号（ack=x+1）。
客户端 → 服务器：发送ACK（确认）报文，确认服务器的响应，携带确认号（ack=y+1），连接建立完成。
注意：三次握手缺一不可，若第三次握手丢失，服务器会认为连接未建立，一段时间后释放连接资源。
四次挥手（关闭连接）
目的是确保双方都已完成数据传输，安全释放连接，流程如下：
客户端 → 服务器：发送FIN（结束）报文，告知服务器已完成数据发送，请求关闭连接。
服务器 → 客户端：发送ACK报文，确认收到客户端的FIN，此时服务器仍可向客户端发送数据。
服务器 → 客户端：发送FIN报文，告知客户端服务器已完成数据发送，请求关闭连接。
客户端 → 服务器：发送ACK报文，确认收到服务器的FIN，连接关闭完成。
2.1.3 Java中TCP编程实现（核心API）
Java通过java.net.Socket（客户端）和java.net.ServerSocket（服务器端）实现TCP通信，核心步骤如下：
服务器端步骤
创建ServerSocket对象，绑定指定端口（端口号需在有效范围，且未被占用）。
调用ServerSocket.accept()方法，监听客户端连接（阻塞方法，直到有客户端连接），返回一个Socket对象，用于与该客户端通信。
通过Socket.getInputStream()获取输入流，读取客户端发送的数据。
通过Socket.getOutputStream()获取输出流，向客户端发送响应数据。
通信结束后，关闭输入流、输出流、Socket和ServerSocket。
客户端步骤
创建Socket对象，指定服务器端的IP地址和端口号，发起连接。
通过Socket.getOutputStream()获取输出流，向服务器端发送数据。
通过Socket.getInputStream()获取输入流，读取服务器端的响应数据。
通信结束后，关闭输入流、输出流和Socket。
TCP编程示例（简单回声程序）
// 服务器端
public class TcpServer {
    public static void main(String[] args) throws IOException {
        // 1. 绑定端口8888
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("服务器已启动，等待客户端连接...");
        // 2. 监听客户端连接（阻塞）
        Socket socket = serverSocket.accept();
        System.out.println("客户端已连接：" + socket.getInetAddress());
        // 3. 读取客户端数据
        InputStream is = socket.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String clientMsg = br.readLine();
        System.out.println("收到客户端消息：" + clientMsg);
        // 4. 向客户端发送响应（回声）
        OutputStream os = socket.getOutputStream();
        PrintWriter pw = new PrintWriter(os, true); // autoFlush=true
        pw.println("服务器已收到：" + clientMsg);
        // 5. 关闭资源
        pw.close();
        br.close();
        socket.close();
        serverSocket.close();
    }
}
// 客户端
public class TcpClient {
    public static void main(String[] args) throws IOException {
        // 1. 连接服务器（IP为localhost，端口8888）
        Socket socket = new Socket("localhost", 8888);
        // 2. 向服务器发送数据
        OutputStream os = socket.getOutputStream();
        PrintWriter pw = new PrintWriter(os, true);
        pw.println("Hello TCP Server!");
        // 3. 读取服务器响应
        InputStream is = socket.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String serverMsg = br.readLine();
        System.out.println("收到服务器消息：" + serverMsg);
        // 4. 关闭资源
        br.close();
        pw.close();
        socket.close();
    }
}
2.2 UDP协议（无连接、不可靠传输）
2.2.1 UDP核心特性
UDP（User Datagram Protocol，用户数据报协议）是无连接、不可靠、面向数据报的传输协议，适用于对实时性要求高、可容忍少量数据丢失的场景（如视频通话、广播、游戏），核心特性：
无连接：通信前无需建立连接，直接发送数据，效率高。
不可靠传输：不保证数据的到达顺序、不重传丢失的数据，也不进行流量控制和拥塞控制。
面向数据报：数据以固定大小的“数据报”为单位传输，每个数据报包含发送方和接收方的IP、端口信息，数据报大小有限制（通常不超过65535字节）。
2.2.2 Java中UDP编程实现（核心API）
Java通过java.net.DatagramSocket（发送/接收数据报的套接字）和java.net.DatagramPacket（封装数据报的类）实现UDP通信，核心步骤如下：
接收方步骤
创建DatagramSocket对象，绑定指定端口。
创建DatagramPacket对象，指定接收数据的缓冲区和缓冲区大小。
调用DatagramSocket.receive()方法，接收数据报（阻塞方法，直到收到数据）。
解析DatagramPacket中的数据、发送方IP和端口。
通信结束后，关闭DatagramSocket。
发送方步骤
创建DatagramSocket对象（无需绑定端口，系统会分配临时端口）。
将发送的数据转换为字节数组。
创建DatagramPacket对象，封装字节数组、数据长度、接收方IP和端口。
调用DatagramSocket.send()方法，发送数据报。
通信结束后，关闭DatagramSocket。
UDP编程示例（简单数据发送接收）
// 接收方（服务器端）
public class UdpReceiver {
    public static void main(String[] args) throws IOException {
        // 1. 绑定端口9999
        DatagramSocket socket = new DatagramSocket(9999);
        System.out.println("接收方已启动，等待数据...");
        // 2. 创建缓冲区，接收数据
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        // 3. 接收数据（阻塞）
        socket.receive(packet);
        // 4. 解析数据
        String data = new String(packet.getData(), 0, packet.getLength());
        String senderIp = packet.getAddress().getHostAddress();
        int senderPort = packet.getPort();
        System.out.println("收到来自 " + senderIp + ":" + senderPort + " 的消息：" + data);
        // 5. 关闭资源
        socket.close();
    }
}
// 发送方（客户端）
public class UdpSender {
    public static void main(String[] args) throws IOException {
        // 1. 创建DatagramSocket
        DatagramSocket socket = new DatagramSocket();
        // 2. 准备发送的数据
        String msg = "Hello UDP Receiver!";
        byte[] data = msg.getBytes();
        // 3. 封装数据报（接收方IP：localhost，端口9999）
        InetAddress receiverIp = InetAddress.getByName("localhost");
        int receiverPort = 9999;
        DatagramPacket packet = new DatagramPacket(data, data.length, receiverIp, receiverPort);
        // 4. 发送数据
        socket.send(packet);
        System.out.println("数据已发送");
        // 5. 关闭资源
        socket.close();
    }
}
2.3 TCP与UDP对比
对比维度
TCP
UDP
连接方式
面向连接（三次握手）
无连接
可靠性
可靠（重传、确认、流量控制）
不可靠（无重传、无确认）
数据传输方式
面向字节流
面向数据报
效率
较低（连接建立/关闭耗时，开销大）
较高（无连接开销，直接发送）
适用场景
文件传输、登录验证、HTTP/HTTPS
视频通话、广播、游戏、DNS查询
三、Java网络编程核心API详解
3.1 InetAddress类（IP地址操作）
InetAddress类位于java.net包下，用于封装IP地址和主机名，无构造方法，通过静态方法获取实例，核心方法如下：
static InetAddress getByName(String host)：根据主机名（如"localhost"）或IP地址字符串获取实例，若主机名无法解析，抛出UnknownHostException。
static InetAddress getLocalHost()：获取本机的IP地址实例。
String getHostName()：获取主机名。
String getHostAddress()：获取IP地址字符串。
boolean isReachable(int timeout)：测试是否能在指定超时时间内到达该IP地址对应的主机。
// 示例：InetAddress使用
public class InetAddressDemo {
    public static void main(String[] args) throws UnknownHostException {
        // 根据主机名获取IP地址
        InetAddress baidu = InetAddress.getByName("www.baidu.com");
        System.out.println("百度主机名：" + baidu.getHostName());
        System.out.println("百度IP地址：" + baidu.getHostAddress());
        // 获取本机IP地址
        InetAddress localHost = InetAddress.getLocalHost();
        System.out.println("本机主机名：" + localHost.getHostName());
        System.out.println("本机IP地址：" + localHost.getHostAddress());
    }
}
3.2 Socket与ServerSocket类（TCP核心）
3.2.1 Socket类（客户端套接字）
用于客户端与服务器端建立连接，核心构造方法和方法：
构造方法：
Socket(String host, int port)：根据服务器端主机名和端口号创建Socket，发起连接。
Socket(InetAddress address, int port)：根据服务器端IP地址实例和端口号创建Socket。
核心方法：
InputStream getInputStream()：获取与该Socket关联的输入流，用于读取服务器端数据。
OutputStream getOutputStream()：获取与该Socket关联的输出流，用于向服务器端发送数据。
void close()：关闭Socket，释放相关资源（关闭前会自动关闭关联的输入流和输出流）。
InetAddress getInetAddress()：获取服务器端的IP地址。
int getPort()：获取服务器端的端口号。
3.2.2 ServerSocket类（服务器端套接字）
用于监听客户端连接，核心构造方法和方法：
构造方法：
ServerSocket(int port)：绑定指定端口，创建ServerSocket实例，若端口已被占用，抛出BindException。
ServerSocket(int port, int backlog)：指定端口和最大连接队列长度（backlog，默认50），队列用于存放等待连接的客户端。
核心方法：
Socket accept()：监听客户端连接，阻塞方法，返回与客户端通信的Socket实例。
void close()：关闭ServerSocket，释放端口资源，不再监听客户端连接。
int getLocalPort()：获取ServerSocket绑定的端口号。
3.3 DatagramSocket与DatagramPacket类（UDP核心）
3.3.1 DatagramSocket类（UDP套接字）
用于发送和接收UDP数据报，核心构造方法和方法：
构造方法：
DatagramSocket()：创建DatagramSocket，绑定系统分配的临时端口（适用于发送方）。
DatagramSocket(int port)：绑定指定端口，创建DatagramSocket（适用于接收方）。
核心方法：
void send(DatagramPacket p)：发送数据报。
void receive(DatagramPacket p)：接收数据报，阻塞方法，将接收的数据存入DatagramPacket的缓冲区。
void close()：关闭DatagramSocket，释放端口资源。
3.3.2 DatagramPacket类（数据报封装）
用于封装UDP数据报，包含数据、数据长度、发送方/接收方IP和端口，核心构造方法和方法：
构造方法（接收数据用）：
DatagramPacket(byte[] buf, int length)：指定接收数据的缓冲区和缓冲区长度。
构造方法（发送数据用）：
DatagramPacket(byte[] buf, int length, InetAddress address, int port)：指定发送的数据、数据长度、接收方IP和端口。
核心方法：
byte[] getData()：获取数据报中的数据（缓冲区）。
int getLength()：获取数据报中有效数据的长度（不是缓冲区长度）。
InetAddress getAddress()：获取发送方（接收数据时）或接收方（发送数据时）的IP地址。
int getPort()：获取发送方（接收数据时）或接收方（发送数据时）的端口号。
3.4 其他常用API
HttpURLConnection：用于发送HTTP请求（GET、POST等），基于TCP协议，是Java原生的HTTP客户端工具，适用于简单的HTTP通信。
HttpClient：Java 11引入的新HTTP客户端，替代HttpURLConnection，支持异步请求、连接池、HTTPS等，功能更强大、效率更高。
SocketAddress：封装IP地址和端口号，常用实现类为InetSocketAddress，可用于Socket和ServerSocket的绑定、连接。
四、Java网络编程进阶知识
4.1 TCP粘包与拆包问题
4.1.1 问题产生原因
TCP是面向字节流的协议，会将应用层发送的数据拆分成多个数据包（拆包），或多个应用层数据合并成一个数据包（粘包），原因如下：
发送方：TCP的Nagle算法（默认开启）会将小数据包合并发送，减少网络开销。
接收方：TCP接收缓冲区会缓存数据，若应用层读取不及时，多个数据包会被缓存在一起，导致粘包。
网络因素：数据包在传输过程中可能被拆分（如超过MTU最大传输单元），导致拆包。
4.1.2 解决方案
核心思路：给数据添加“边界”，让接收方能够准确区分不同的应用层数据，常用方案：
固定长度法：规定每个应用层数据包的固定长度，接收方每次读取固定长度的数据，不足部分补0。优点：简单；缺点：浪费带宽，不灵活。
分隔符法：在数据末尾添加特殊分隔符（如"\n"、"\r\n"），接收方通过分隔符区分数据。优点：灵活；缺点：若数据中包含分隔符，会导致误判，需对数据进行转义。
长度字段法（推荐）：在数据头部添加一个固定长度的字段（如4字节int），表示后续数据的长度，接收方先读取长度字段，再根据长度读取对应的数据。优点：灵活、可靠，无数据误判问题。
4.2 多线程TCP服务器
基础TCP服务器为“单线程”，一次只能处理一个客户端连接，效率极低。多线程TCP服务器通过为每个客户端连接创建一个独立线程，实现同时处理多个客户端请求，核心思路：
服务器端启动后，通过ServerSocket.accept()监听客户端连接。
每接收到一个客户端连接（获取Socket实例），创建一个新的线程，将Socket传入线程。
线程中处理与该客户端的通信（读取数据、发送响应），通信结束后关闭Socket和线程。
// 多线程TCP服务器示例
public class MultiThreadTcpServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("多线程服务器已启动，等待客户端连接...");
        while (true) {
            // 监听客户端连接（阻塞）
            Socket socket = serverSocket.accept();
            System.out.println("客户端已连接：" + socket.getInetAddress());
            // 为每个客户端创建一个线程处理通信
            new Thread(new ClientHandler(socket)).start();
        }
    }
    // 客户端处理线程
    static class ClientHandler implements Runnable {
        private Socket socket;
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        @Override
        public void run() {
            try (
                // 自动关闭资源（try-with-resources）
                InputStream is = socket.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                OutputStream os = socket.getOutputStream();
                PrintWriter pw = new PrintWriter(os, true)
            ) {
                String clientMsg;
                // 循环读取客户端数据，直到客户端关闭连接
                while ((clientMsg = br.readLine()) != null) {
                    System.out.println("收到客户端 " + socket.getInetAddress() + " 消息：" + clientMsg);
                    pw.println("服务器已收到：" + clientMsg);
                }
                System.out.println("客户端 " + socket.getInetAddress() + " 已断开连接");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close(); // 关闭Socket
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
注意：多线程服务器存在线程创建过多的问题（如大量客户端连接），会导致系统资源耗尽。优化方案：使用线程池管理线程，限制线程数量，提高资源利用率。
4.3 线程池TCP服务器（优化版）
通过ThreadPoolExecutor创建线程池，复用线程处理客户端连接，避免线程频繁创建和销毁的开销，核心代码示例：
public class ThreadPoolTcpServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8888);
        // 创建线程池，核心线程数5，最大线程数10，空闲线程存活时间60秒
        ExecutorService threadPool = new ThreadPoolExecutor(
            5, 10, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20), // 任务队列
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy() // 拒绝策略
        );
        System.out.println("线程池服务器已启动，等待客户端连接...");
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("客户端已连接：" + socket.getInetAddress());
            // 将客户端处理任务提交到线程池
            threadPool.submit(new ClientHandler(socket));
        }
    }
    // 客户端处理线程（与多线程版本一致）
    static class ClientHandler implements Runnable {
        // 省略与多线程版本相同的代码...
    }
}
4.4 UDP广播与组播
4.4.1 UDP广播
广播是指发送方发送的数据报，同一网络内的所有设备都能接收，核心是使用“广播地址”（IPv4中，广播地址为255.255.255.255，适用于本地局域网）。Java实现UDP广播的关键：
发送方：将数据报的接收方IP设为广播地址，端口设为指定端口。
接收方：绑定该指定端口，即可接收广播数据。
4.4.2 UDP组播
组播是指发送方发送的数据报，只有加入指定“组播组”的设备才能接收，比广播更高效（减少网络带宽占用），核心是使用“组播地址”（IPv4中，组播地址范围为224.0.0.0-239.255.255.255）。Java实现UDP组播的关键：
发送方：使用MulticastSocket（DatagramSocket的子类）发送数据报，接收方IP设为组播地址。
接收方：创建MulticastSocket，绑定指定端口，调用joinGroup(InetAddress group)方法加入组播组，即可接收组播数据。
4.5 HTTPS与SSL/TLS
HTTPS是HTTP协议的安全版本，基于SSL/TLS协议对数据进行加密传输，避免数据被窃取、篡改。Java中实现HTTPS通信的核心API：
HttpsURLConnection：HttpURLConnection的子类，支持HTTPS请求，自动处理SSL/TLS握手。
SSLContext：用于创建SSL/TLS上下文，配置密钥库、信任库（用于验证服务器证书）。
TrustManager：用于验证服务器证书的合法性，自定义信任策略（如忽略证书校验，仅用于测试）。
五、Java网络编程常见问题与解决方案
5.1 端口被占用问题
现象：启动ServerSocket或DatagramSocket时，抛出BindException: Address already in use异常。
解决方案：
更换未被占用的端口（推荐使用1024以上的非系统端口）。
关闭占用该端口的进程（Windows使用netstat -ano | findstr 端口号查看进程ID，再用taskkill /f /pid 进程ID关闭；Linux使用netstat -anp | grep 端口号查看进程，再用kill -9 进程ID关闭）。
设置Socket的SO_REUSEADDR选项，允许端口复用：socket.setReuseAddress(true);（需在绑定端口前设置）。
5.2 连接超时问题
现象：客户端连接服务器时，长时间无响应，最终抛出ConnectException: Connection timed out异常。
解决方案：
检查服务器端是否正常启动，端口是否正确，IP地址是否可达（使用ping命令测试）。
检查防火墙是否拦截了该端口的通信（关闭防火墙或开放对应端口）。
设置Socket的连接超时时间：socket.setSoTimeout(3000);（单位：毫秒），避免长时间阻塞。
5.3 数据读取/发送异常
现象：读取数据时抛出IOException，或数据发送后接收方无法收到。
解决方案：
检查输入流/输出流是否正确关闭（推荐使用try-with-resources自动关闭）。
TCP通信中，确保发送方和接收方的读写顺序一致（如发送方先发送，接收方再读取）。
UDP通信中，确保接收方绑定的端口与发送方指定的端口一致，且未被占用。
检查数据编码/解码格式是否一致（如发送方用UTF-8编码，接收方也需用UTF-8解码）。
5.4 线程泄漏问题
现象：多线程服务器运行一段时间后，系统资源耗尽，无法处理新的客户端连接。
解决方案：
使用线程池管理线程，避免无限制创建线程。
确保线程中的异常被正确捕获，避免线程因未处理异常而终止但资源未释放。
通信结束后，及时关闭Socket和相关流资源，避免资源泄漏。
六、Java网络编程实战场景
6.1 简单HTTP客户端（发送GET/POST请求）
// 使用HttpURLConnection发送GET请求
public class HttpGetDemo {
    public static void main(String[] args) throws IOException {
        // 1. 创建URL对象
        URL url = new URL("https://www.baidu.com");
        // 2. 打开连接，获取HttpURLConnection对象
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // 3. 设置请求方法（GET）
        conn.setRequestMethod("GET");
        // 4. 设置请求超时时间
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        // 5. 判断响应码（200表示请求成功）
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            // 6. 读取响应数据
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            // 输出响应内容
            System.out.println(sb.toString());
            // 关闭资源
            br.close();
        }
        // 关闭连接
        conn.disconnect();
    }
}
6.2 简单文件传输（TCP）
实现客户端向服务器端发送文件，核心思路：客户端读取本地文件，通过TCP发送给服务器端，服务器端接收数据后写入本地文件。
// 服务器端（接收文件）
public class FileServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9999);
        System.out.println("文件服务器已启动，等待客户端发送文件...");
        Socket socket = serverSocket.accept();
        // 读取客户端发送的文件名
        InputStream is = socket.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String fileName = br.readLine();
        // 创建文件输出流，写入本地文件
        FileOutputStream fos = new FileOutputStream("D:/receive/" + fileName);
        byte[] buffer = new byte[1024];
        int len;
        // 读取文件数据并写入
        while ((len = is.read(buffer)) != -1) {
            fos.write(buffer, 0, len);
        }
        System.out.println("文件接收完成！");
        // 关闭资源
        fos.close();
        br.close();
        socket.close();
        serverSocket.close();
    }
}
// 客户端（发送文件）
public class FileClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 9999);
        // 发送文件名
        OutputStream os = socket.getOutputStream();
        PrintWriter pw = new PrintWriter(os, true);
        String filePath = "D:/test.txt";
        File file = new File(filePath);
        pw.println(file.getName());
        // 读取本地文件，发送给服务器端
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = fis.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        // 关闭输出流，告知服务器端文件发送完毕
        socket.shutdownOutput();
        System.out.println("文件发送完成！");
        // 关闭资源
        fis.close();
        pw.close();
        socket.close();
    }
}
七、总结
Java网络编程的核心是基于TCP/IP协议簇，通过Socket、ServerSocket、DatagramSocket等API实现不同设备间的数据传输。关键要点如下：
TCP与UDP是传输层核心协议，分别适用于可靠传输和实时传输场景，需根据业务需求选择。
核心API的使用的是基础，需熟练掌握InetAddress、Socket、ServerSocket、DatagramSocket等类的方法。
进阶知识（粘包拆包、多线程/线程池服务器、广播组播、HTTPS）是解决实际开发问题的关键。
实际开发中，需注意资源释放、异常处理、性能优化（如线程池、连接池），避免常见问题。
Java网络编程是Java后端开发、分布式系统、物联网等领域的基础，掌握其核心知识点和实战技巧，能为后续学习更高阶的网络框架（如Netty）打下坚实基础。
