# Java网络编程：线程详细知识点剖析

## 📑 目录

- [一、线程基础](#一线程基础)
- [二、Java线程的创建方式](#二java线程的创建方式)
- [三、Java线程的生命周期](#三java线程的生命周期)
- [四、Java线程的核心机制](#四java线程的核心机制)
- [五、线程在Java网络编程中的典型应用](#五线程在java网络编程中的典型应用)
- [六、网络编程中线程的常见问题及解决方案](#六网络编程中线程的常见问题及解决方案)
- [七、总结](#七总结)

---

## 一、线程基础

### 1.1 线程的定义

线程（Thread）是程序执行的最小单元，是进程的一个子集，共享进程的内存空间，但拥有自己独立的程序计数器、虚拟机栈和本地方法栈。在Java网络编程中，线程的核心作用是 **"并发处理"**。

### 1.2 线程与进程的区别（网络编程视角）

| 对比维度 | 进程 | 线程 |
|---------|------|------|
| 资源分配 | 独立内存空间 | 共享进程资源 |
| 通信成本 | 高（如Socket跨进程通信） | 低 |
| 切换效率 | 低 | 高 |
| 网络编程场景 | 多进程服务器 | 多线程服务器 |

### 1.3 线程的核心特性

| 特性 | 说明 |
|------|------|
| **并发性** | 多个线程同时执行，对应多客户端同时交互 |
| **共享性** | 共享进程的堆和方法区，可共享Socket、IO流等资源 |
| **独立性** | 每个线程拥有独立栈空间，执行逻辑互不干扰 |

---

## 二、Java线程的创建方式

### 2.1 继承Thread类

```java
public class ClientThread extends Thread {
    private String host;
    private int port;

    public ClientThread(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port);
             OutputStream os = socket.getOutputStream()) {
            os.write("Hello Server".getBytes());
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ClientThread("127.0.0.1", 8080).start();
    }
}
```

### 2.2 实现Runnable接口（推荐基础场景）

```java
public class ServerHandler implements Runnable {
    private Socket clientSocket;

    public ServerHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (InputStream is = clientSocket.getInputStream();
             OutputStream os = clientSocket.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int len = is.read(buffer);
            String clientMsg = new String(buffer, 0, len);
            System.out.println("收到客户端消息：" + clientMsg);
            os.write(("服务器已收到：" + clientMsg).getBytes());
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { clientSocket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ServerHandler(clientSocket)).start();
        }
    }
}
```

### 2.3 实现Callable接口（带返回值场景）

```java
public class ClientCallable implements Callable<String> {
    private String host;
    private int port;
    private String sendMsg;

    public ClientCallable(String host, int port, String sendMsg) {
        this.host = host;
        this.port = port;
        this.sendMsg = sendMsg;
    }

    @Override
    public String call() throws Exception {
        try (Socket socket = new Socket(host, port);
             OutputStream os = socket.getOutputStream();
             InputStream is = socket.getInputStream()) {
            os.write(sendMsg.getBytes());
            os.flush();
            byte[] buffer = new byte[1024];
            int len = is.read(buffer);
            return new String(buffer, 0, len);
        }
    }

    public static void main(String[] args) throws Exception {
        ClientCallable callable = new ClientCallable("127.0.0.1", 8080, "Hello Server");
        FutureTask<String> futureTask = new FutureTask<>(callable);
        new Thread(futureTask).start();
        String serverResponse = futureTask.get();
        System.out.println("服务器响应：" + serverResponse);
    }
}
```

### 2.4 线程池（企业级网络编程最优实践）

```java
public class ThreadPoolServer {
    public static void main(String[] args) throws IOException {
        ExecutorService threadPool = Executors.newFixedThreadPool(5);
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("服务器启动，等待客户端连接...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            threadPool.submit(new ServerHandler(clientSocket));
        }
    }
}
```

| 创建方式 | 优点 | 缺点 | 适用场景 |
|---------|------|------|---------|
| 继承Thread类 | 简单直接 | Java单继承限制，灵活性不足 | 简单客户端场景 |
| 实现Runnable接口 | 解耦线程逻辑与类继承，灵活 | 无返回值 | 基础服务器线程 |
| 实现Callable接口 | 有返回值、可抛异常 | 需配合FutureTask | 需获取执行结果 |
| 线程池 | 复用线程、控制并发数、便于管理 | 需合理配置参数 | 企业级高并发场景 |

---

## 三、Java线程的生命周期

### 3.1 核心状态

| 状态 | 说明 | 网络编程场景 |
|------|------|-------------|
| **新建（New）** | 创建Thread对象，未启动 | `new Thread(handler)` |
| **就绪（Runnable）** | 调用start()，等待CPU调度 | `thread.start()` |
| **运行（Running）** | CPU调度到该线程，执行run() | 处理客户端读写操作 |
| **阻塞（Blocked）** | 线程暂停执行，等待条件满足 | `accept()`、`read()` 阻塞 |
| **终止（Terminated）** | run()执行完毕或异常抛出 | 客户端断开，线程终止 |

### 3.2 阻塞类型的区分

| 阻塞类型 | 触发条件 | 恢复方式 |
|---------|---------|---------|
| IO阻塞 | Socket的read()、accept() | IO操作完成 |
| 同步阻塞 | wait()方法 | notify()/notifyAll() |
| 睡眠阻塞 | sleep()方法 | 睡眠时间结束 |

---

## 四、Java线程的核心机制

### 4.1 线程同步

#### synchronized关键字

```java
public class ServerCounter {
    private int onlineCount = 0;

    public synchronized void addOnlineCount() {
        onlineCount++;
    }

    public synchronized void subtractOnlineCount() {
        onlineCount--;
    }

    public synchronized int getOnlineCount() {
        return onlineCount;
    }
}
```

#### Lock锁

```java
public class ServerCounter {
    private int onlineCount = 0;
    private Lock lock = new ReentrantLock();

    public void addOnlineCount() {
        lock.lock();
        try {
            onlineCount++;
        } finally {
            lock.unlock();
        }
    }
}
```

#### 原子类

```java
public class ServerCounter {
    private AtomicInteger onlineCount = new AtomicInteger(0);

    public void addOnlineCount() {
        onlineCount.incrementAndGet();
    }

    public int getOnlineCount() {
        return onlineCount.get();
    }
}
```

### 4.2 线程通信

```java
public class DataContainer {
    private byte[] data;
    private boolean hasData = false;

    public synchronized void setData(byte[] data) {
        while (hasData) {
            try { wait(); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        this.data = data;
        hasData = true;
        notify();
    }

    public synchronized byte[] getData() {
        while (!hasData) {
            try { wait(); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        hasData = false;
        notify();
        return data;
    }
}
```

### 4.3 线程中断

网络编程中，线程常因IO操作进入阻塞状态，需要中断阻塞的线程：

```java
public class ClientThread extends Thread {
    private Socket socket;

    @Override
    public void run() {
        try {
            socket = new Socket("127.0.0.1", 8080);
            InputStream is = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int len = is.read(buffer); // 阻塞等待数据
        } catch (IOException e) {
            System.out.println("线程被中断，释放资源");
            try { if (socket != null) socket.close(); } catch (IOException ex) { }
        }
    }

    public void stopThread() {
        this.interrupt(); // 设置中断标志
    }
}
```

---

## 五、线程在Java网络编程中的典型应用

### 5.1 BIO（阻塞IO）中的线程模型

- **模型**：一个主线程监听端口，每接收一个客户端连接，启动一个子线程处理。
- **优点**：实现简单。
- **缺点**：线程资源消耗大，无法支持高并发。

### 5.2 NIO（非阻塞IO）中的线程模型

- **模型**：通过Selector多路复用器，一个线程管理多个客户端连接。
- **核心分工**：Selector线程（监听连接和IO事件）+ 工作线程（处理IO读写）。
- **优点**：线程资源消耗少，支持高并发。

### 5.3 Netty中的线程模型

- **Reactor模式**：Boss线程组（接收连接）+ Worker线程组（处理IO读写）。
- **优点**：高性能、高并发、易扩展。

---

## 六、网络编程中线程的常见问题及解决方案

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 线程资源泄露 | 未释放Socket、IO流 | 使用try-with-resources；设置超时时间；使用线程池 |
| 线程死锁 | 多个线程互相持有对方需要的锁 | 统一锁的获取顺序；使用tryLock()设置超时 |
| 并发安全问题 | 未进行同步控制 | 使用synchronized、Lock、原子类 |
| 线程过多导致服务器崩溃 | BIO模型中创建大量线程 | 使用NIO/Netty；使用线程池控制并发数 |

---

## 七、总结

- **简单场景**（客户端数量少）：使用Runnable接口创建线程。
- **需要返回结果场景**：使用Callable + FutureTask。
- **企业级高并发场景**：使用线程池 + NIO/Netty，控制线程资源，提升并发能力。
- 需重点关注线程资源泄露、死锁、并发安全等问题。

---

## 📖 相关阅读

- [Java网络编程详细知识点剖析](./Java网络编程详细知识点剖析.md)
- [Java网络编程：非阻塞IO详细知识点剖析](./Java网络编程：非阻塞IO详细知识点剖析.md)
- [Java网络编程：服务器Socket详细知识点剖析](./Java网络编程：服务器Socket详细知识点剖析.md)
- [Java网络编程：客户端Socket详细知识点剖析](./Java网络编程：客户端Socket详细知识点剖析.md)
