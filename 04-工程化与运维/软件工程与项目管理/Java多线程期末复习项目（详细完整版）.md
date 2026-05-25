Java多线程期末复习项目（详细完整版）
一、项目概述
1.1 项目目标
本项目专为Java多线程期末复习设计，通过“模拟多窗口售票系统”这一经典场景，完整覆盖多线程核心知识点，实现“边编码、边复习、边巩固”的目的。项目功能齐全，代码注释详细，每一步都对应多线程核心考点，帮助快速掌握线程创建、线程状态、同步机制、线程通信、线程池等高频考点，同时具备可扩展性，可根据复习需求新增功能。
核心复习考点：
线程的创建方式（继承Thread类、实现Runnable接口、实现Callable接口+FutureTask）
线程的状态及转换（新建、就绪、运行、阻塞、终止）
线程同步机制（synchronized关键字、Lock锁、volatile关键字）
线程通信（wait()、notify()、notifyAll()、Condition）
线程池（ThreadPoolExecutor、Executors工具类）
线程安全问题（可见性、原子性、有序性）及解决方案
1.2 项目需求
模拟火车站多窗口售票场景，具体需求如下：
总票数固定（如100张），多个窗口（如3个）同时售票；
保证线程安全，避免出现超卖、重卖现象（核心考点：同步机制）；
实时显示每个窗口的售票情况（如“窗口1售出第5张票，剩余票数：95”）；
支持三种线程创建方式实现售票功能，可切换测试；
使用线程池优化线程管理，避免线程频繁创建销毁的开销；
实现线程通信：当票数售罄时，所有窗口停止售票并提示“票已售罄”；
添加线程优先级设置、线程休眠（模拟售票延迟），复习线程状态转换；
异常处理：处理线程中断、售票过程中的异常场景。
1.3 技术选型
核心技术：Java SE 8+（多线程核心API）、Junit（可选，用于单元测试）
开发工具：IDEA（推荐）、Eclipse，无需额外框架，纯Java原生实现，贴合期末复习重点。
二、项目整体结构（分层设计，清晰易懂）
项目采用简单分层，便于理解和扩展，结构如下：
com.review.multithread
├─ entity          // 实体类（封装票数信息，保证线程安全）
│  └─ Ticket.java  // 票数实体，包含售票、剩余票数查询等方法
├─ thread          // 线程相关类（三种线程创建方式）
│  ├─ ThreadTicket.java    // 继承Thread类实现售票
│  ├─ RunnableTicket.java  // 实现Runnable接口实现售票
│  └─ CallableTicket.java  // 实现Callable接口实现售票
├─ pool            // 线程池相关类
│  └─ TicketThreadPool.java // 线程池管理，优化线程使用
├─ test            // 测试类（入口类，切换不同实现方式测试）
│  └─ TicketTest.java       // 测试入口，运行售票系统
└─ util            // 工具类（可选，封装日志、异常处理）
   └─ ThreadUtil.java       // 线程相关工具方法（如打印日志、设置优先级）
三、分步实现项目（每一步对应复习考点，详细注释）
第一步：创建实体类Ticket（核心：线程安全的共享资源）
考点：共享资源的线程安全问题、synchronized关键字、volatile关键字、原子性保证。
说明：票数是多个线程共享的资源，必须保证售票操作的原子性（即“查询剩余票数→判断是否有票→售出票数”这一系列操作不能被打断），否则会出现超卖、重卖。这里使用synchronized实现同步，volatile保证票数的可见性。
package com.review.multithread.entity;
/**
 * 票数实体类（共享资源）
 * 核心考点：线程安全、synchronized同步方法、volatile可见性
     */
    public class Ticket {
    // 总票数，volatile保证可见性：一个线程修改后，其他线程立即可见
    private volatile int totalTicket;
    // 标志位：是否售罄，volatile保证多线程间可见
    private volatile boolean isSoldOut = false;
    // 构造方法，初始化总票数
    public Ticket(int totalTicket) {
        this.totalTicket = totalTicket;
    }
    /**
     * 售票方法（核心：同步方法，保证原子性）
     * synchronized修饰方法：锁对象是this（当前Ticket实例），多个线程竞争同一把锁
     * @param windowName 窗口名称（用于打印日志）
     * @return 售票结果（成功/失败）
     */
    public synchronized boolean sellTicket(String windowName) {
        // 先判断是否售罄
        if (isSoldOut) {
            System.out.println(windowName + "：票已售罄，无法售票！");
            return false;
        }
        // 模拟售票延迟（复习：Thread.sleep()，使线程进入阻塞状态）
        try {
            // 随机延迟100-300毫秒，模拟真实售票场景
            Thread.sleep((long) (Math.random() * 200 + 100));
        } catch (InterruptedException e) {
            // 处理线程中断异常（考点：线程中断）
            System.out.println(windowName + "：售票被中断，操作失败！");
            Thread.currentThread().interrupt(); // 恢复中断状态
            return false;
        }
        // 售票操作（原子性，因为被synchronized修饰）
        totalTicket--;
        // 判断是否售罄（售罄后设置标志位，通知其他线程）
        if (totalTicket <= 0) {
            isSoldOut = true;
            System.out.println(windowName + "：售出最后一张票！票已全部售罄！");
        } else {
            System.out.println(windowName + "：售出1张票，剩余票数：" + totalTicket);
        }
        return true;
    }
    //  getter方法（无需同步，因为volatile保证可见性）
    public int getTotalTicket() {
        return totalTicket;
    }
    public boolean isSoldOut() {
        return isSoldOut;
    }
    }
    复习要点：
    volatile关键字：解决可见性问题，当isSoldOut和totalTicket被修改后，其他线程能立即看到最新值，避免线程读取到旧数据；
    synchronized同步方法：锁对象是当前Ticket实例，确保同一时刻只有一个线程能执行sellTicket方法，避免并发修改共享资源；
    Thread.sleep(long millis)：使线程进入阻塞状态，睡眠时间结束后进入就绪状态，等待CPU调度；
    线程中断：InterruptedException异常，中断线程时，需调用Thread.currentThread().interrupt()恢复中断状态，避免中断信号丢失。
    第二步：实现三种线程创建方式（核心考点：线程创建）
    Java中三种线程创建方式，分别实现售票功能，可切换测试，对比三种方式的区别。
    2.1 方式一：继承Thread类（ThreadTicket）
    考点：继承Thread类、重写run()方法、线程启动（start()方法）、线程优先级。
    package com.review.multithread.thread;
    import com.review.multithread.entity.Ticket;
    /**
 * 方式一：继承Thread类实现售票线程
 * 考点：Thread类的使用、run()方法重写、start()启动线程、线程优先级
     */
    public class ThreadTicket extends Thread {
    // 共享的票数对象（多个线程共用同一个Ticket实例，否则会各自售票，出现重复）
    private Ticket ticket;
    // 窗口名称
    private String windowName;
    // 构造方法，传入共享票数和窗口名称
    public ThreadTicket(Ticket ticket, String windowName) {
        this.ticket = ticket;
        this.windowName = windowName;
        // 设置线程优先级（考点：线程优先级1-10，默认5，优先级高的线程更易获得CPU调度）
        this.setPriority(Thread.NORM_PRIORITY); // 正常优先级（5）
    }
    /**
     * 重写run()方法：线程执行的核心逻辑
     * 注意：不能直接调用run()方法（只是普通方法调用），必须调用start()方法启动线程
     */
    @Override
    public void run() {
        // 循环售票，直到票售罄
        while (!ticket.isSoldOut()) {
            // 调用售票方法（同步方法，保证线程安全）
            ticket.sellTicket(windowName);
            // 每次售票后，线程礼让（考点：Thread.yield()，使当前线程放弃CPU，进入就绪状态）
            Thread.yield();
        }
    }
    }
    2.2 方式二：实现Runnable接口（RunnableTicket）
    考点：实现Runnable接口、重写run()方法、Thread类的构造方法传入Runnable实例、避免单继承限制。
    package com.review.multithread.thread;
    import com.review.multithread.entity.Ticket;
    /**
 * 方式二：实现Runnable接口实现售票线程（推荐使用，避免单继承限制）
 * 考点：Runnable接口、Thread构造方法传入Runnable、线程启动
     */
    public class RunnableTicket implements Runnable {
    // 共享的票数对象（多个线程共用同一个实例）
    private Ticket ticket;
    // 窗口名称
    private String windowName;
    // 构造方法
    public RunnableTicket(Ticket ticket, String windowName) {
        this.ticket = ticket;
        this.windowName = windowName;
    }
    /**
     * 重写run()方法：线程执行逻辑
     * 与继承Thread类的区别：Runnable接口是函数式接口，无单继承限制，更灵活
     */
    @Override
    public void run() {
        // 循环售票，直到票售罄
        while (!ticket.isSoldOut()) {
            ticket.sellTicket(windowName);
            // 线程礼让
            Thread.yield();
        }
    }
    }
    2.3 方式三：实现Callable接口（CallableTicket）
    考点：实现Callable接口、重写call()方法（有返回值、可抛异常）、FutureTask类（获取返回值）、线程启动。
    说明：Callable接口与Runnable接口的区别：call()有返回值、可抛出异常，适合需要获取线程执行结果的场景（如统计每个窗口的售票数量）。
    package com.review.multithread.thread;
    import com.review.multithread.entity.Ticket;
    import java.util.concurrent.Callable;
    /**
 * 方式三：实现Callable接口实现售票线程（有返回值、可抛异常）
 * 考点：Callable接口、call()方法、FutureTask获取返回值
     */
    public class CallableTicket implements Callable<Integer> {
    // 共享的票数对象
    private Ticket ticket;
    // 窗口名称
    private String windowName;
    // 记录当前窗口售票数量（用于返回结果）
    private int sellCount = 0;
    // 构造方法
    public CallableTicket(Ticket ticket, String windowName) {
        this.ticket = ticket;
        this.windowName = windowName;
    }
    /**
     * 重写call()方法：线程执行逻辑，有返回值（当前窗口售票数量），可抛异常
     * @return 当前窗口售票数量
     * @throws Exception 可抛出异常（区别于Runnable的run()方法）
     */
    @Override
    public Integer call() throws Exception {
        // 循环售票，直到票售罄
        while (!ticket.isSoldOut()) {
            // 调用售票方法，成功则计数+1
            if (ticket.sellTicket(windowName)) {
                sellCount++;
            }
            Thread.yield();
        }
        // 返回当前窗口的售票数量（线程执行结果）
        System.out.println(windowName + "：售票结束，共售出" + sellCount + "张票");
        return sellCount;
    }
    }
    第三步：实现线程池（核心考点：线程池）
    考点：ThreadPoolExecutor的使用、线程池参数、Executors工具类、线程池的优势（复用线程、减少创建销毁开销）。
    说明：直接创建线程会频繁创建和销毁，消耗系统资源，线程池可以复用线程，提高效率。这里使用ThreadPoolExecutor（推荐，灵活可控），也可使用Executors工具类快速创建。
    package com.review.multithread.pool;
    import com.review.multithread.thread.RunnableTicket;
    import com.review.multithread.entity.Ticket;
    import java.util.concurrent.LinkedBlockingQueue;
    import java.util.concurrent.ThreadPoolExecutor;
    import java.util.concurrent.TimeUnit;
    /**
 * 线程池管理类（复习线程池核心考点）
 * 考点：ThreadPoolExecutor参数、线程池工作原理、线程复用
     */
    public class TicketThreadPool {
    // 线程池实例（核心参数：核心线程数、最大线程数、空闲时间、队列、拒绝策略）
    private ThreadPoolExecutor threadPool;
    /**
     * 初始化线程池
     * @param corePoolSize 核心线程数（常驻线程，即使空闲也不销毁）
     * @param maximumPoolSize 最大线程数（线程池能容纳的最大线程数）
     * @param keepAliveTime 空闲线程存活时间（核心线程外的线程，空闲超过该时间则销毁）
     * @param unit 时间单位
     * @param queueCapacity 任务队列容量（当核心线程都在工作时，新任务放入队列）
     */
    public void initThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                               TimeUnit unit, int queueCapacity) {
        // 初始化线程池（推荐使用ThreadPoolExecutor，而非Executors，避免资源耗尽风险）
        threadPool = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                new LinkedBlockingQueue<Runnable>(queueCapacity), // 有界队列，避免无界队列耗尽内存
                new ThreadPoolExecutor.AbortPolicy() // 拒绝策略：当线程池满且队列满时，抛出异常
        );
    }
    /**
     * 提交售票任务到线程池
     * @param ticket 共享票数对象
     * @param windowNames 窗口名称数组（如{"窗口1","窗口2","窗口3"}）
     */
    public void submitTicketTask(Ticket ticket, String[] windowNames) {
        // 为每个窗口创建一个售票任务，提交到线程池
        for (String windowName : windowNames) {
            // 这里使用RunnableTicket（也可替换为其他线程实现）
            RunnableTicket task = new RunnableTicket(ticket, windowName);
            threadPool.submit(task); // 提交任务到线程池，线程池会分配线程执行
        }
    }
    /**
     * 关闭线程池（考点：线程池关闭）
     * shutdown()：等待所有已提交的任务执行完毕后关闭，不接受新任务
     * shutdownNow()：立即关闭，中断正在执行的任务，返回未执行的任务
     */
    public void shutdownThreadPool() {
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            System.out.println("线程池已关闭，等待所有任务执行完毕...");
        }
    }
    }
    复习要点：
    ThreadPoolExecutor核心参数：核心线程数、最大线程数、空闲时间、任务队列、拒绝策略，这是期末高频考点；
    线程池工作原理：当提交任务时，先判断核心线程是否已满→未满则创建核心线程执行任务→已满则判断队列是否已满→队列未满则放入队列→队列已满则判断是否达到最大线程数→未达到则创建非核心线程执行→达到则执行拒绝策略；
    拒绝策略：AbortPolicy（抛出异常）、CallerRunsPolicy（由调用线程执行）、DiscardPolicy（丢弃任务）、DiscardOldestPolicy（丢弃队列中最老的任务）。
    第四步：实现线程通信（核心考点：线程通信）
    考点：wait()、notifyAll()方法、Condition接口、线程间协作。
    说明：在原有Ticket类基础上，新增线程通信逻辑，当票数售罄时，通知所有售票线程停止工作；也可实现“当票数小于10张时，提示‘余票不足’”的功能，强化线程通信知识点。
    修改Ticket类，新增线程通信相关代码（修改后的完整代码）：
    package com.review.multithread.entity;
    /**
 * 票数实体类（新增线程通信功能）
 * 核心考点：线程通信（wait()、notifyAll()）、同步锁
     */
    public class Ticket {
    private volatile int totalTicket;
    private volatile boolean isSoldOut = false;
    public Ticket(int totalTicket) {
        this.totalTicket = totalTicket;
    }
    // 同步方法（锁对象：this）
    public synchronized boolean sellTicket(String windowName) {
        // 线程通信：当票售罄时，让线程等待（避免无效循环）
        while (isSoldOut) {
            try {
                System.out.println(windowName + "：票已售罄，进入等待状态...");
                // wait()：使当前线程释放锁，进入等待状态（阻塞），直到被notify()/notifyAll()唤醒
                this.wait(); // 必须在同步代码块/同步方法中调用，否则抛出IllegalMonitorStateException
            } catch (InterruptedException e) {
                System.out.println(windowName + "：等待被中断，退出售票！");
                Thread.currentThread().interrupt();
                return false;
            }
        }
        // 模拟售票延迟
        try {
            Thread.sleep((long) (Math.random() * 200 + 100));
        } catch (InterruptedException e) {
            System.out.println(windowName + "：售票被中断，操作失败！");
            Thread.currentThread().interrupt();
            return false;
        }
        totalTicket--;
        // 余票不足10张时，提示信息（可选，强化线程通信）
        if (totalTicket <= 10 && totalTicket > 0) {
            System.out.println("【余票提醒】" + windowName + "：剩余票数不足10张，仅剩" + totalTicket + "张！");
            // notifyAll()：唤醒所有等待该锁的线程（让其他窗口尽快知晓余票情况）
            this.notifyAll();
        }
        if (totalTicket <= 0) {
            isSoldOut = true;
            System.out.println(windowName + "：售出最后一张票！票已全部售罄！");
            // 唤醒所有等待的线程，告知票已售罄，让它们退出等待
            this.notifyAll();
        } else {
            System.out.println(windowName + "：售出1张票，剩余票数：" + totalTicket);
        }
        return true;
    }
    // 新增：唤醒所有等待的线程（用于手动唤醒，可选）
    public synchronized void wakeupAllThreads() {
        this.notifyAll();
    }
    // getter方法
    public int getTotalTicket() {
        return totalTicket;
    }
    public boolean isSoldOut() {
        return isSoldOut;
    }
    }
    复习要点：
    wait()方法：必须在同步代码块/同步方法中调用（持有锁时），调用后释放锁，线程进入等待状态，直到被唤醒；
    notifyAll()方法：唤醒所有等待该锁的线程，线程被唤醒后，需要重新竞争锁，才能继续执行；
    notify()方法：只唤醒一个等待该锁的线程，随机性较强，实际开发中优先使用notifyAll()，避免线程饿死；
    Condition接口：可替代wait()/notify()，更灵活（如可指定唤醒特定线程），可自行扩展（课后练习）。
    第五步：测试类（项目入口，切换不同实现方式）
    考点：线程启动、线程池使用、Callable+FutureTask获取返回值、多线程测试。
    测试类包含4种测试场景，可分别运行，复习不同知识点：
    测试继承Thread类的线程创建方式；
    测试实现Runnable接口的线程创建方式；
    测试实现Callable接口+FutureTask的线程创建方式（获取返回值）；
    测试线程池的使用（推荐，贴近实际开发）。
    package com.review.multithread.test;
    import com.review.multithread.entity.Ticket;
    import com.review.multithread.thread.CallableTicket;
    import com.review.multithread.thread.RunnableTicket;
    import com.review.multithread.thread.ThreadTicket;
    import com.review.multithread.pool.TicketThreadPool;
    import java.util.concurrent.FutureTask;
    import java.util.concurrent.TimeUnit;
    /**
 * 测试类（项目入口）
 * 可切换不同线程实现方式，测试多线程售票功能，复习核心考点
     */
    public class TicketTest {
    // 总票数（可修改，如100张、50张）
    private static final int TOTAL_TICKET = 100;
    // 窗口数量（可修改，如3个、5个）
    private static final String[] WINDOW_NAMES = {"窗口1", "窗口2", "窗口3"};
    public static void main(String[] args) throws Exception {
        // 选择测试场景（注释掉其他场景，运行当前场景）
        // testThreadWay(); // 场景1：继承Thread类
        // testRunnableWay(); // 场景2：实现Runnable接口
        // testCallableWay(); // 场景3：实现Callable接口（获取返回值）
        testThreadPoolWay(); // 场景4：线程池（推荐）
    }
    /**
     * 场景1：测试继承Thread类的线程创建方式
     */
    public static void testThreadWay() {
        System.out.println("=== 继承Thread类 测试多窗口售票 ===");
        // 创建共享票数对象（多个线程共用一个实例）
        Ticket ticket = new Ticket(TOTAL_TICKET);
        // 创建多个线程（每个窗口一个线程）
        ThreadTicket window1 = new ThreadTicket(ticket, WINDOW_NAMES[0]);
        ThreadTicket window2 = new ThreadTicket(ticket, WINDOW_NAMES[1]);
        ThreadTicket window3 = new ThreadTicket(ticket, WINDOW_NAMES[2]);
        // 启动线程（调用start()方法，而非run()）
        window1.start();
        window2.start();
        window3.start();
        // 等待所有线程执行完毕（考点：Thread.join()，等待线程终止）
        try {
            window1.join();
            window2.join();
            window3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("=== 所有窗口售票结束 ===");
    }
    /**
     * 场景2：测试实现Runnable接口的线程创建方式
     */
    public static void testRunnableWay() {
        System.out.println("=== 实现Runnable接口 测试多窗口售票 ===");
        Ticket ticket = new Ticket(TOTAL_TICKET);
        // 创建Runnable实例，传入共享票数
        RunnableTicket task1 = new RunnableTicket(ticket, WINDOW_NAMES[0]);
        RunnableTicket task2 = new RunnableTicket(ticket, WINDOW_NAMES[1]);
        RunnableTicket task3 = new RunnableTicket(ticket, WINDOW_NAMES[2]);
        // 创建Thread对象，传入Runnable实例，启动线程
        new Thread(task1).start();
        new Thread(task2).start();
        new Thread(task3).start();
        // 这里无需join()，因为主线程结束不影响子线程执行，可观察控制台输出
    }
    /**
     * 场景3：测试实现Callable接口的线程创建方式（获取返回值）
     */
    public static void testCallableWay() throws Exception {
        System.out.println("=== 实现Callable接口 测试多窗口售票（获取返回值） ===");
        Ticket ticket = new Ticket(TOTAL_TICKET);
        // 1. 创建Callable实例
        CallableTicket task1 = new CallableTicket(ticket, WINDOW_NAMES[0]);
        CallableTicket task2 = new CallableTicket(ticket, WINDOW_NAMES[1]);
        CallableTicket task3 = new CallableTicket(ticket, WINDOW_NAMES[2]);
        // 2. 创建FutureTask实例（包装Callable，用于获取返回值）
        FutureTask<Integer> futureTask1 = new FutureTask<>(task1);
        FutureTask<Integer> futureTask2 = new FutureTask<>(task2);
        FutureTask<Integer> futureTask3 = new FutureTask<>(task3);
        // 3. 创建Thread对象，传入FutureTask，启动线程
        new Thread(futureTask1).start();
        new Thread(futureTask2).start();
        new Thread(futureTask3).start();
        // 4. 获取线程执行结果（get()方法会阻塞，直到线程执行完毕并返回结果）
        int count1 = futureTask1.get();
        int count2 = futureTask2.get();
        int count3 = futureTask3.get();
        // 验证总售票数是否等于总票数（确保线程安全，无超卖、重卖）
        System.out.println("=== 售票结果统计 ===");
        System.out.println(WINDOW_NAMES[0] + "：" + count1 + "张");
        System.out.println(WINDOW_NAMES[1] + "：" + count2 + "张");
        System.out.println(WINDOW_NAMES[2] + "：" + count3 + "张");
        System.out.println("总售票数：" + (count1 + count2 + count3) + "张");
        System.out.println("总票数：" + TOTAL_TICKET + "张");
    }
    /**
     * 场景4：测试线程池的使用（推荐，贴近实际开发）
     */
    public static void testThreadPoolWay() throws Exception {
        System.out.println("=== 线程池 测试多窗口售票 ===");
        Ticket ticket = new Ticket(TOTAL_TICKET);
        // 1. 创建线程池管理类
        TicketThreadPool threadPool = new TicketThreadPool();
        // 2. 初始化线程池（核心参数：3个核心线程、5个最大线程、30秒空闲时间、队列容量10）
        threadPool.initThreadPool(3, 5, 30, TimeUnit.SECONDS, 10);
        // 3. 提交售票任务到线程池
        threadPool.submitTicketTask(ticket, WINDOW_NAMES);
        // 4. 等待票售罄，关闭线程池
        while (!ticket.isSoldOut()) {
            Thread.sleep(1000); // 每隔1秒检查一次票数是否售罄
        }
        // 关闭线程池
        threadPool.shutdownThreadPool();
        System.out.println("=== 所有窗口售票结束，线程池已关闭 ===");
    }
    }
    第六步：工具类（可选，优化代码，复习异常处理）
    封装线程相关工具方法，如打印日志、设置线程名称、异常处理，使代码更简洁，复习异常处理知识点。
    package com.review.multithread.util;
    /**
 * 线程工具类（可选）
 * 考点：异常处理、线程相关工具方法
     */
    public class ThreadUtil {
    /**
     * 打印线程日志（包含线程名称、时间、内容）
     * @param threadName 线程名称（窗口名称）
     * @param message 日志内容
     */
    public static void printThreadLog(String threadName, String message) {
        long time = System.currentTimeMillis(); // 当前时间戳
        System.out.printf("[%d] %s：%s%n", time, threadName, message);
    }
    /**
     * 安全设置线程优先级
     * @param thread 线程对象
     * @param priority 优先级（1-10）
     */
    public static void setThreadPrioritySafe(Thread thread, int priority) {
        // 校验优先级范围，避免异常
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException("线程优先级必须在1-10之间");
        }
        thread.setPriority(priority);
    }
    /**
     * 安全休眠线程（处理中断异常）
     * @param millis 休眠时间（毫秒）
     */
    public static void sleepSafe(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            System.out.println("线程休眠被中断：" + e.getMessage());
            Thread.currentThread().interrupt(); // 恢复中断状态
        }
    }
    }
    使用方式：在Ticket类或线程类中，替换原有日志打印、线程休眠代码，例如：
    // 替换原有System.out.println
    ThreadUtil.printThreadLog(windowName, "售出1张票，剩余票数：" + totalTicket);
    // 替换原有Thread.sleep()
    ThreadUtil.sleepSafe((long) (Math.random() * 200 + 100));
    四、项目测试与复习巩固
    4.1 测试步骤
    打开IDEA/Eclipse，创建Java项目，创建对应包（com.review.multithread.entity、thread等）；
    复制上述所有代码，按包结构粘贴到对应类中；
    运行TicketTest类，切换不同测试场景（注释掉其他场景，保留一个），观察控制台输出；
    验证功能：是否有超卖、重卖现象，票售罄后是否所有窗口停止，线程池是否正常关闭，Callable是否能获取正确返回值。
    4.2 常见问题与解决方案（期末高频考点）
    问题1：出现超卖、重卖现象 → 原因：未保证售票操作的原子性，未使用同步机制 → 解决方案：使用synchronized或Lock锁，确保售票方法同步；
    问题2：线程启动后无反应 → 原因：直接调用run()方法（普通方法调用），未调用start()方法 → 解决方案：调用thread.start()启动线程；
    问题3：wait()方法抛出IllegalMonitorStateException → 原因：wait()未在同步代码块/同步方法中调用 → 解决方案：将wait()放在synchronized修饰的代码中；
    问题4：线程池提交任务后，程序立即结束 → 原因：主线程未等待线程池任务执行完毕就退出 → 解决方案：使用threadPool.awaitTermination()或循环检查任务是否执行完毕；
    问题5：Callable无法获取返回值 → 原因：未使用FutureTask包装，或未调用get()方法 → 解决方案：使用FutureTask包装Callable，调用get()方法获取返回值。
    4.3 扩展练习（深化复习）
    为了进一步巩固多线程知识点，可完成以下扩展练习（期末可能考类似场景）：
    使用Lock锁（ReentrantLock）替代synchronized，实现售票同步（复习Lock与synchronized的区别）；
    使用Condition接口替代wait()/notifyAll()，实现线程通信（复习Condition的使用）；
    新增“退票”功能，模拟退票后票数增加，线程重新开始售票（复习线程通信的灵活使用）；
    使用CountDownLatch（倒计时器），等待所有窗口售票结束后，打印总售票统计（复习并发工具类）；
    模拟线程死锁场景（如两个窗口互相等待对方释放锁），并解决死锁（复习死锁的产生条件与解决方案）。
    五、核心知识点总结（期末复习重点）
    5.1 线程创建方式对比
    创建方式
    核心方法
    是否有返回值
    是否可抛异常
    优缺点
    继承Thread类
    重写run()
    无
    无
    优点：简单直接；缺点：单继承限制，无法继承其他类
    实现Runnable接口
    重写run()
    无
    无
    优点：无单继承限制，灵活；缺点：无返回值，无法抛异常
    实现Callable接口
    重写call()
    有
    有
    优点：有返回值、可抛异常；缺点：需配合FutureTask使用，稍复杂
    5.2 线程同步机制对比
    同步方式
    核心特点
    优点
    缺点
    synchronized
    JVM层面，自动释放锁，可修饰方法、代码块
    简单易用，无需手动释放锁，安全性高
    灵活性差，无法中断等待锁的线程，无法尝试获取锁
    Lock（ReentrantLock）
    API层面，手动释放锁（try-finally），可中断、可尝试获取锁
    灵活性高，支持条件变量（Condition），可控制锁的获取与释放
    需手动释放锁，易遗漏（导致死锁），代码稍复杂
    volatile
    保证可见性、有序性，不保证原子性
    轻量级，无锁开销，适用于单线程写、多线程读场景
    无法保证原子性，不能用于复杂同步场景
    5.3 线程状态转换（期末必考）
    Java线程的5种状态（生命周期）：
    新建状态（New）：创建线程对象（new Thread()），未调用start()方法；
    就绪状态（Runnable）：调用start()方法后，线程等待CPU调度（此时线程已具备执行条件）；
    运行状态（Running）：CPU调度线程，执行run()/call()方法中的逻辑；
    阻塞状态（Blocked）：线程暂时失去执行条件（如sleep()、wait()、等待锁），等待重新具备执行条件后进入就绪状态；
    终止状态（Terminated）：线程执行完毕（run()/call()方法执行结束）或异常终止。
    状态转换触发条件：
    New → Runnable：调用start()方法；
    Runnable → Running：CPU调度；
    Running → Blocked：调用sleep()、wait()，或等待锁；
    Blocked → Runnable：sleep()时间到、被notify()/notifyAll()唤醒、获取锁；
    Running → Terminated：run()/call()执行完毕，或抛出未捕获的异常。
    六、项目总结
    本项目通过“多窗口售票系统”这一经典场景，完整覆盖了Java多线程期末复习的核心考点，从线程创建、线程同步、线程通信，到线程池、线程状态转换，每一步代码都对应具体考点，且注释详细、功能齐全。
    复习建议：
    先理解项目整体结构，再逐行阅读代码，结合注释回忆对应知识点；
    运行每个测试场景，观察输出结果，分析线程执行过程，理解线程安全的重要性；
    完成扩展练习，深化对多线程知识点的理解，应对期末可能出现的复杂场景；
    整理核心知识点总结，背诵线程创建、同步机制、状态转换等高频考点，确保考试时能快速答题。
    通过本项目的编码和测试，能够快速巩固Java多线程的核心知识，轻松应对期末考试中的多线程相关题目和编程题。
