03.13 17:03
Java多线程核心知识点
一、 多线程基础概念
1. 程序、进程与线程
- 程序：是为完成特定任务、用某种语言编写的一组指令的集合，是静态的代码。
- 进程：是程序的一次执行过程，是系统进行资源分配和调度的独立单位，有自己的生命周期，运行时会占用内存、CPU等系统资源。
- 线程：是进程中的一个执行单元，是CPU调度和分派的基本单位，一个进程可以包含多个线程，多个线程共享进程的内存空间和资源（如文件句柄、网络连接等），线程本身只拥有少量运行时必需的资源（如程序计数器、栈、寄存器）。
2. 并发与并行
- 并发：多个线程在同一时间段内交替执行，宏观上看起来像同时运行，微观上是CPU在多个线程间快速切换，适用于单核CPU场景。
- 并行：多个线程在同一时刻同时执行，需要多核CPU支持，多个核心分别处理不同的线程，真正实现同时运行。
3. 多线程的优势与应用场景
- 优势：提高CPU利用率，提升程序响应速度，实现任务的异步执行，例如在GUI程序中，主线程负责界面渲染，子线程处理耗时操作（如网络请求、文件读写），避免界面卡顿。
- 应用场景：文件下载、数据批量处理、网络通信、实时监控、并发服务器开发等。
4. 线程的生命周期
- Java线程的生命周期包含新建（New）、就绪（Runnable）、运行（Running）、阻塞（Blocked）、等待（Waiting）、超时等待（Timed Waiting）、终止（Terminated） 七个状态，状态之间通过特定操作转换。
- 新建状态：通过 new 关键字创建线程对象后，线程处于新建状态，此时线程尚未启动，未分配系统资源。
- 就绪状态：调用线程的 start() 方法后，线程进入就绪状态，该状态下线程具备运行条件，等待CPU调度执行。
- 运行状态：CPU调度就绪状态的线程，线程进入运行状态，执行 run() 方法中的代码。
- 阻塞状态：线程因竞争锁失败（如进入 synchronized 代码块但未获取锁）、执行 Thread.sleep(long) 、等待I/O操作完成等原因，放弃CPU使用权，进入阻塞状态，阻塞解除后回到就绪状态。
- 等待状态：线程执行 Object.wait() 、 Thread.join() 、 LockSupport.park() 等方法后，进入等待状态，该状态下线程需要被其他线程显式唤醒（如调用 Object.notify() / notifyAll() ），否则会一直等待。
- 超时等待状态：线程执行 Thread.sleep(long) 、 Object.wait(long) 、 Thread.join(long) 等方法后，进入超时等待状态，该状态下线程等待一段时间后会自动唤醒，回到就绪状态。
- 终止状态：线程的 run() 方法执行完毕，或因异常终止，线程进入终止状态，该状态下线程的生命周期结束，无法再恢复。
二、 线程的创建方式
Java中创建线程有四种核心方式，每种方式有不同的实现逻辑和适用场景：
1. 继承 Thread 类
- 步骤：定义类继承 Thread 类，重写 run() 方法（线程执行的核心逻辑），创建该类的实例对象，调用 start() 方法启动线程。
- 示例核心代码：
java
class MyThread extends Thread {
    @Override
    public void run() {
        // 线程执行逻辑
        System.out.println("继承Thread类的线程执行");
    }
}
// 启动线程
MyThread thread = new MyThread();
thread.start();
 
- 缺点：Java是单继承机制，继承 Thread 类后无法再继承其他类，灵活性受限。
2. 实现 Runnable 接口
- 步骤：定义类实现 Runnable 接口，重写 run() 方法，创建该类的实例对象，将其作为参数传入 Thread 类的构造方法，创建 Thread 对象，调用 start() 方法启动线程。
- 示例核心代码：
java
class MyRunnable implements Runnable {
    @Override
    public void run() {
        // 线程执行逻辑
        System.out.println("实现Runnable接口的线程执行");
    }
}
// 启动线程
Runnable runnable = new MyRunnable();
Thread thread = new Thread(runnable);
thread.start();
 
- 优点：避免单继承限制，多个线程可以共享同一个 Runnable 实例的资源，适合多个线程处理同一任务的场景。
3. 实现 Callable 接口 +  FutureTask 
- 特点： Callable 接口的 call() 方法可以返回执行结果，并且可以抛出异常，解决了 Thread 和 Runnable 无法返回结果的问题； FutureTask 实现了 RunnableFuture 接口，可作为 Thread 的构造参数，同时提供了获取结果的 get() 方法。
- 步骤：定义类实现 Callable 接口，重写 call() 方法，创建 Callable 实例，将其传入 FutureTask 构造方法，创建 FutureTask 对象，将 FutureTask 作为参数传入 Thread 构造方法，启动线程，通过 FutureTask 的 get() 方法获取执行结果（ get() 方法是阻塞的，会等待线程执行完毕再返回结果）。
- 示例核心代码：
java
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
class MyCallable implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        // 执行任务并返回结果
        int sum = 0;
        for (int i = 1; i <= 100; i++) {
            sum += i;
        }
        return sum;
    }
}
// 启动线程并获取结果
Callable<Integer> callable = new MyCallable();
FutureTask<Integer> futureTask = new FutureTask<>(callable);
Thread thread = new Thread(futureTask);
thread.start();
// 获取结果，阻塞等待线程执行完成
Integer result = futureTask.get();
System.out.println("计算结果：" + result);
 
4. 使用线程池（ ExecutorService ）
- 特点：通过线程池创建和管理线程，避免频繁创建和销毁线程的开销，提高线程复用率，控制并发线程数，是实际开发中最常用的方式。
- 核心类： Executors （提供静态方法创建线程池）、 ExecutorService （线程池核心接口）、 ThreadPoolExecutor （线程池的核心实现类）。
- 常用线程池类型：
-  newFixedThreadPool(int nThreads) ：创建固定大小的线程池，线程数始终保持不变，超出的任务会在队列中等待。
-  newCachedThreadPool() ：创建可缓存的线程池，线程数根据任务量动态调整，空闲线程超过60秒会被回收。
-  newSingleThreadExecutor() ：创建单线程的线程池，保证任务按顺序执行。
-  newScheduledThreadPool(int corePoolSize) ：创建支持定时和周期性任务的线程池。
- 示例核心代码：
java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class ThreadPoolDemo {
    public static void main(String[] args) {
        // 创建固定大小的线程池
        ExecutorService executor = Executors.newFixedThreadPool(3);
        // 提交任务
        for (int i = 0; i < 5; i++) {
            int taskId = i;
            executor.submit(() -> {
                System.out.println("任务" + taskId + "执行，线程：" + Thread.currentThread().getName());
            });
        }
        // 关闭线程池
        executor.shutdown();
    }
}
 
三、 线程的核心方法
1.  start() ：启动线程，使线程进入就绪状态，等待CPU调度，一个线程只能调用一次 start() 方法，重复调用会抛出 IllegalThreadStateException 。
2.  run() ：线程的执行体，包含线程要执行的逻辑，直接调用 run() 方法不会启动新线程，只是普通的方法调用。
3.  sleep(long millis) ：静态方法，让当前线程休眠指定的毫秒数，休眠期间线程会进入超时等待状态，持有的锁不会释放，休眠时间到后线程回到就绪状态。
4.  yield() ：静态方法，让当前线程主动放弃CPU使用权，回到就绪状态，重新参与CPU调度，不保证其他线程一定能获得执行权，可能当前线程刚放弃又被调度。
5.  join()  /  join(long millis) ：让调用该方法的线程等待当前线程执行完毕，或等待指定的毫秒数，例如 thread.join() 会让主线程等待 thread 线程执行完成后再继续执行，调用 join() 的线程会进入等待/超时等待状态。
6.  setPriority(int newPriority)  /  getPriority() ：设置和获取线程的优先级，Java线程优先级分为1~10级，默认优先级是5，优先级高的线程获取CPU调度的概率更高，但不保证一定先执行，具体依赖于操作系统的调度机制。
7.  setDaemon(boolean on)  /  isDaemon() ：设置和判断线程是否为守护线程，守护线程是为用户线程服务的，当所有用户线程结束时，守护线程会自动终止，例如JVM的垃圾回收线程就是守护线程；设置守护线程必须在 start() 方法之前，否则会抛出 IllegalThreadStateException 。
8.  interrupt() ：中断线程，不会强制终止线程，只是给线程设置一个中断标志位，线程可以通过 isInterrupted() 方法检测中断标志，自行决定是否停止执行；如果线程处于 wait() 、 join() 、 sleep() 等阻塞状态，调用 interrupt() 会抛出 InterruptedException ，并清除中断标志位。
9.  isInterrupted() ：判断线程的中断标志位是否为 true ，不会清除中断标志位。
10.  Thread.interrupted() ：静态方法，判断当前线程的中断标志位是否为 true ，会清除中断标志位（将标志位重置为 false ）。
四、 线程同步与线程安全
（一） 线程安全问题的产生
多个线程并发访问共享资源（如成员变量、静态变量）时，若同时对共享资源进行修改操作，会导致数据不一致，引发线程安全问题。例如多个线程同时对同一个计数器进行自增操作，可能出现计数结果小于预期的情况。
（二） 线程同步的核心机制
线程同步的目的是保证多个线程对共享资源的操作是有序的，避免并发冲突，Java提供了多种同步机制：
1.  synchronized 关键字
- 是Java内置的同步锁，属于可重入锁（同一个线程可以多次获取同一个锁，不会造成死锁），底层通过JVM的 monitor （监视器）实现。
- 适用范围：
1. 修饰实例方法：锁的是当前类的实例对象，多个线程访问同一个实例的 synchronized 方法时会互斥，访问不同实例的方法不会互斥。
2. 修饰静态方法：锁的是当前类的Class对象，多个线程访问该类的任意静态 synchronized 方法时都会互斥。
3. 修饰代码块：锁的是括号内的对象，格式为 synchronized (lockObj) { // 同步代码 } ，可以灵活指定锁对象，推荐使用专门的锁对象（如 private final Object lock = new Object(); ），避免使用 this 或类对象导致锁范围过大。
- 核心特点：线程获取锁后，其他线程会进入阻塞状态，直到锁被释放；锁的释放时机是同步方法/代码块执行完毕，或抛出异常。
- 示例代码（同步代码块）：
java
class Counter {
    private int count = 0;
    private final Object lock = new Object();
    public void increment() {
        synchronized (lock) {
            count++;
        }
    }
    public int getCount() {
        synchronized (lock) {
            return count;
        }
    }
}
 
2.  Lock 接口及其实现类
-  Lock 是Java.util.concurrent.locks包下的接口，提供了比 synchronized 更灵活的锁机制，支持手动获取锁和释放锁，支持公平锁和非公平锁。
- 常用实现类： ReentrantLock （可重入锁）、 ReentrantReadWriteLock （读写锁）。
-  ReentrantLock 的核心用法：
- 通过 lock() 方法获取锁， unlock() 方法释放锁，必须在 finally 块中释放锁，避免异常导致锁无法释放。
- 支持公平锁：创建 ReentrantLock 时传入 true ，即 new ReentrantLock(true) ，公平锁会按照线程请求锁的顺序分配锁，避免线程饥饿；默认是非公平锁，效率更高。
- 支持尝试获取锁： tryLock() 方法尝试获取锁，获取成功返回 true ，失败返回 false ，不会阻塞线程； tryLock(long time, TimeUnit unit) 可以指定等待时间。
- 示例代码：
java
import java.util.concurrent.locks.ReentrantLock;
class Counter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }
    public int getCount() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
 
-  ReentrantReadWriteLock （读写锁）：
- 分为读锁（共享锁）和写锁（排他锁），多个线程可以同时获取读锁，读锁和写锁、写锁和写锁之间互斥。
- 适用场景：读多写少的场景，例如缓存系统，读取数据时加读锁，写入数据时加写锁，提高并发读取效率。
- 核心方法： readLock().lock() / readLock().unlock() （获取/释放读锁）、 writeLock().lock() / writeLock().unlock() （获取/释放写锁）。
3. 线程安全的集合类
- 替代非线程安全的集合（如 ArrayList 、 HashMap ），避免手动同步的麻烦，常用的线程安全集合包括：
1.  Vector 、 Hashtable ：早期的线程安全集合，通过 synchronized 修饰方法实现同步，效率较低。
2.  Collections.synchronizedXXX() ：通过工具类包装非线程安全集合，返回线程安全的集合，例如 Collections.synchronizedList(new ArrayList<>()) 。
3.  java.util.concurrent 包下的并发集合：如 ConcurrentHashMap （高效的线程安全哈希表）、 CopyOnWriteArrayList （写时复制的ArrayList，适合读多写少场景）、 ConcurrentLinkedQueue （并发队列），这些集合采用了更高效的并发策略，性能优于传统的线程安全集合。
（三） 线程间通信
多个线程协同完成任务时，需要进行通信，Java提供了基于 Object 类的方法和 Condition 接口的通信方式：
1. 基于 Object 的 wait() 、 notify() 、 notifyAll() 
- 必须在 synchronized 方法或代码块中使用，调用者是锁对象，否则会抛出 IllegalMonitorStateException 。
-  wait() ：让当前线程释放锁，进入等待状态，直到被其他线程的 notify() 或 notifyAll() 唤醒。
-  notify() ：唤醒等待在该锁对象上的一个线程，具体唤醒哪个线程由JVM决定。
-  notifyAll() ：唤醒等待在该锁对象上的所有线程。
- 经典应用：生产者-消费者模型，生产者线程生产数据后调用 notify() 唤醒消费者，消费者消费数据后调用 wait() 等待生产。
2. 基于 Condition 接口的 await() 、 signal() 、 signalAll() 
-  Condition 是 Lock 接口的配套接口，通过 Lock.newCondition() 方法创建，提供了比 wait() / notify() 更灵活的通信方式，支持多个条件队列。
-  await() ：对应 wait() ，让当前线程释放锁，进入等待状态。
-  signal() ：对应 notify() ，唤醒一个等待在该 Condition 上的线程。
-  signalAll() ：对应 notifyAll() ，唤醒所有等待在该 Condition 上的线程。
- 优点：一个 Lock 可以创建多个 Condition ，实现不同线程的分组通信，例如生产者-消费者模型中，可以分别创建生产者条件和消费者条件，精准唤醒对应线程。
五、 线程池核心原理
（一） 线程池的核心参数（ ThreadPoolExecutor ）
 ThreadPoolExecutor 是线程池的核心实现类，其构造方法包含七个核心参数，决定了线程池的工作机制：
java
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutionHandler handler)
 
1.  corePoolSize ：核心线程数，线程池长期保持的线程数量，即使线程空闲也不会被回收（除非设置了 allowCoreThreadTimeOut(true) ）。
2.  maximumPoolSize ：最大线程数，线程池允许创建的最大线程数量。
3.  keepAliveTime  +  unit ：非核心线程的空闲存活时间，超过该时间的非核心线程会被回收。
4.  workQueue ：任务队列，用于存放等待执行的任务，必须是阻塞队列，常用的有 ArrayBlockingQueue （有界队列）、 LinkedBlockingQueue （无界队列）、 SynchronousQueue （同步队列）。
5.  threadFactory ：线程工厂，用于创建新线程，可以自定义线程的名称、优先级、是否为守护线程等。
6.  handler ：拒绝策略，当线程池和任务队列都满了时，对新提交的任务的处理策略，常用的拒绝策略有：
-  ThreadPoolExecutor.AbortPolicy ：默认策略，直接抛出 RejectedExecutionException 。
-  ThreadPoolExecutor.CallerRunsPolicy ：由提交任务的线程执行该任务。
-  ThreadPoolExecutor.DiscardPolicy ：直接丢弃新任务，不抛出异常。
-  ThreadPoolExecutor.DiscardOldestPolicy ：丢弃队列中最旧的任务，然后尝试提交新任务。
（二） 线程池的工作流程
1. 提交任务后，线程池首先判断核心线程是否全部繁忙，若核心线程有空闲，创建新的核心线程执行任务。
2. 若核心线程全部繁忙，判断任务队列是否已满，若未满，将任务加入队列等待执行。
3. 若任务队列已满，判断线程数是否达到最大线程数，若未达到，创建非核心线程执行任务。
4. 若线程数达到最大线程数，执行拒绝策略。
（三） 线程池的关闭
1.  shutdown() ：平缓关闭线程池，不再接收新任务，等待已提交的任务（包括队列中的任务）执行完毕后关闭线程池。
2.  shutdownNow() ：立即关闭线程池，不再接收新任务，尝试中断正在执行的任务，返回队列中未执行的任务列表。
六、 并发工具类（ java.util.concurrent ）
1.  CountDownLatch ：倒计时门闩，让一个或多个线程等待其他线程完成操作后再执行；通过 countDown() 方法减少计数器， await() 方法等待计数器变为0，计数器只能递减，无法重置。
2.  CyclicBarrier ：循环栅栏，让多个线程到达一个屏障点时阻塞，直到所有线程都到达屏障点后，所有线程才继续执行；通过 await() 方法等待，计数器可以重置，支持循环使用。
3.  Semaphore ：信号量，用于控制同时访问特定资源的线程数量，通过 acquire() 方法获取许可， release() 方法释放许可，适用于限流场景（如接口限流）。
4.  Exchanger ：交换器，让两个线程在指定点交换数据，通过 exchange(V x) 方法交换数据，线程会阻塞直到另一个线程调用 exchange() 方法。
七、 线程安全问题的常见解决方案
1. 避免共享资源：将共享变量改为局部变量，局部变量存储在线程的栈中，属于线程私有，不会被其他线程访问。
2. 使用不可变对象：如 String 、 Integer 等不可变类，其内部状态创建后无法修改，天然线程安全。
3. 加锁同步：使用 synchronized 或 Lock 保证对共享资源的操作是原子性的。
4. 使用原子类： java.util.concurrent.atomic 包下的类（如 AtomicInteger 、 AtomicLong ），基于CAS（Compare-And-Swap，比较并交换）机制实现原子操作，无需加锁，效率更高；CAS是一种乐观锁机制，通过比较内存值和预期值，若相等则更新，否则重试。
5. 使用线程池：统一管理线程，控制并发数，避免手动创建线程带来的安全隐患。
八、 死锁
（一） 死锁的产生条件
死锁是指多个线程因竞争资源而相互等待，且无法自行解除的状态，必须同时满足以下四个条件：
1. 互斥条件：资源只能被一个线程占用，其他线程无法访问。
2. 请求与保持条件：线程持有已获取的资源，同时请求新的资源。
3. 不剥夺条件：线程已持有的资源不能被其他线程强行剥夺，只能由线程自行释放。
4. 循环等待条件：多个线程形成环形的资源等待链，每个线程都在等待下一个线程释放资源。
（二） 死锁的避免与解决
1. 破坏死锁的条件：
- 破坏请求与保持条件：线程一次性申请所有需要的资源。
- 破坏不剥夺条件：允许线程主动释放已持有的资源。
- 破坏循环等待条件：对资源进行编号，线程按编号顺序申请资源。
2. 排查死锁：使用 jps 命令查看进程ID，再用 jstack 命令查看线程堆栈信息，定位死锁的线程和资源。

