Java线程间通信核心知识点与实战规范
线程间通信是Java多线程编程的核心模块，主要解决多线程协同工作、有序执行的问题。当多个线程并发操作共享资源、需要按特定流程配合时，缺乏有效通信会导致CPU空轮询浪费、响应延迟、数据不一致等问题。其核心目标是让线程在条件不满足时主动等待，条件就绪时被精准唤醒，实现高效且安全的线程协作。本文将从基础原理、传统通信方式、显式锁通信、高级并发工具、实战避坑五大维度，系统梳理完整知识体系，保证全文上下文连贯、逻辑递进。
一、线程间通信基础原理
1. 通信的核心必要性
    多线程场景下，线程往往分工不同，常见生产者-消费者、主线程等待子线程、多线程分批执行等协作模式。若仅依靠多线程并发执行，线程只能通过无限循环+休眠的轮询方式检查状态，会带来三大弊端：一是CPU资源被大量空轮询占用，系统开销剧增；二是线程无法实时感知状态变化，业务响应滞后；三是缺少同步管控，极易引发共享数据错乱。因此，线程间通信是实现高效协同、避免资源浪费的必要手段。
2. 通信的核心前提
    所有线程间通信必须满足两个基础条件，缺一不可：第一，存在共享对象或共享资源作为通信载体，线程之间依托该载体传递信号、交换数据；第二，临界区代码必须保证线程安全，通过synchronized内置锁或Lock显式锁实现同步，防止并发读写导致的数据异常。
    二、传统线程间通信：Object监视器方法
    这是Java最基础、最原生的线程通信方式，基于Object类提供的三个核心方法实现，依赖synchronized同步机制，适用于简单的等待-唤醒场景，无需引入额外依赖，上手便捷。
    1. 核心方法与特性详解
    方法名称
    核心作用
    是否释放锁
    调用前提条件
    wait()
    当前线程进入等待状态，释放锁，直至被唤醒或中断
    是
    必须在synchronized同步代码块/同步方法内调用，且线程持有当前对象的锁
    notify()
    随机唤醒一个在当前对象上等待的线程
    否
    同wait()方法，必须持有锁
    notifyAll()
    唤醒所有在当前对象上等待的线程
    否
    同wait()方法，必须持有锁
    关键执行细节：调用wait()后线程会立即释放锁，让其他线程有机会竞争锁执行；调用notify()/notifyAll()不会立即释放锁，需等当前同步代码块/方法执行完毕才释放，被唤醒线程需重新竞争锁才能继续运行。此外，线程存在虚假唤醒问题（未被主动唤醒却意外激活），必须通过循环检查等待条件，杜绝单次判断导致的逻辑异常。
2. 标准实战范式：生产者-消费者模型
    生产者-消费者是线程通信最经典的场景，以下代码基于wait()/notifyAll()实现，严格遵循循环检查条件、同步管控、唤醒对应线程的规范，保证线程安全与协同流畅。
    // 共享资源载体：消息队列
    class MessageQueue {
    // 队列容量，限制生产数量
    private final int CAPACITY = 5;
    private final Queue<String> queue = new LinkedList<>();
    // 生产者方法：生产消息，同步方法保证线程安全
    public synchronized void produce(String message) throws InterruptedException {
        // 循环判断队列是否已满，避免虚假唤醒
        while (queue.size() == CAPACITY) {
            // 队列满，生产者等待，释放锁
            wait();
        }
        // 执行生产逻辑
        queue.add(message);
        System.out.println("生产消息：" + message + "，当前队列长度：" + queue.size());
        // 唤醒所有等待的消费者线程
        notifyAll();
    }
    // 消费者方法：消费消息，同步方法保证线程安全
    public synchronized String consume() throws InterruptedException {
        // 循环判断队列是否为空，避免虚假唤醒
        while (queue.isEmpty()) {
            // 队列空，消费者等待，释放锁
            wait();
        }
        // 执行消费逻辑
        String result = queue.poll();
        System.out.println("消费消息：" + result + "，当前队列长度：" + queue.size());
        // 唤醒所有等待的生产者线程
        notifyAll();
        return result;
    }
    }
    三、显式锁通信：Lock + Condition机制
    JUC（java.util.concurrent）包提供的Lock与Condition组合，是对传统wait()/notify()的升级优化，解决了原生方式灵活性不足、唤醒粒度粗的问题，适用于复杂线程协同、多条件等待的场景，是企业级开发的常用方案。
    1. 与传统通信方式的核心差异
    锁管控：传统方式依赖synchronized自动加锁释放，Lock需手动lock()、unlock()，管控更灵活
    等待队列：传统方式仅有一个等待队列，所有线程共用，唤醒时无法精准区分；Condition支持创建多个独立等待队列，可实现精准唤醒
    附加功能：Lock支持公平锁、可中断锁、超时获取锁，传统方式不支持
    唤醒粒度：可针对不同条件唤醒对应线程，避免notifyAll()带来的无效线程竞争，提升性能
2. 核心API对应关系
    Condition接口的方法与Object监视器方法功能对应，用法更规范：
    Condition.await() ↔ Object.wait()：线程等待，释放锁
    Condition.signal() ↔ Object.notify()：唤醒单个等待线程
    Condition.signalAll() ↔ Object.notifyAll()：唤醒所有等待线程
    额外扩展：awaitUninterruptibly()实现不响应中断的等待，适配特殊业务场景。
3. 多条件精准协同实战
    通过创建两个独立Condition，分别管控生产者、消费者的等待与唤醒，彻底避免无效唤醒，提升并发效率，代码逻辑更清晰。
    class AdvancedBlockQueue<T> {
    // 显式可重入锁
    private final Lock lock = new ReentrantLock();
    // 队列未满条件：生产者等待/唤醒
    private final Condition notFull = lock.newCondition();
    // 队列非空条件：消费者等待/唤醒
    private final Condition notEmpty = lock.newCondition();
    private final T[] items;
    private int count;
    private int putIndex;
    private int takeIndex;
    @SuppressWarnings("unchecked")
    public AdvancedBlockQueue(int capacity) {
        this.items = (T[]) new Object[capacity];
    }
    // 生产者入队方法
    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            // 队列满，生产者在notFull条件等待
            while (count == items.length) {
                notFull.await();
            }
            // 入队操作
            items[putIndex] = item;
            putIndex = (putIndex + 1) % items.length;
            count++;
            // 精准唤醒消费者线程
            notEmpty.signal();
        } finally {
            // 手动释放锁，必须放在finally，避免死锁
            lock.unlock();
        }
    }
    // 消费者出队方法
    public T take() throws InterruptedException {
        lock.lock();
        try {
            // 队列空，消费者在notEmpty条件等待
            while (count == 0) {
                notEmpty.await();
            }
            // 出队操作
            T result = items[takeIndex];
            items[takeIndex] = null;
            takeIndex = (takeIndex + 1) % items.length;
            count--;
            // 精准唤醒生产者线程
            notFull.signal();
            return result;
        } finally {
            lock.unlock();
        }
    }
    }
    四、JUC高级线程通信工具
    针对复杂线程协作场景，JUC提供了封装好的高级工具类，无需手动编写等待-唤醒逻辑，降低开发难度，适配批量线程调度、限流、数据交换等特殊场景。
    1. CountDownLatch（倒计时门闩）
    核心功能：一个线程阻塞等待，直至其他N个线程完成任务，倒计时归零后才继续执行，属于一次性工具，倒计时结束无法重置。
    适用场景：主线程等待子线程初始化完成、多任务并行执行后汇总结果、批量数据处理完毕再执行后续逻辑。
    核心方法：countDown()倒计时减1；await()阻塞等待倒计时归零。
2. CyclicBarrier（循环屏障）
    核心功能：一组线程相互等待，所有线程都到达屏障点后，再一起继续执行，支持循环复用，可重复使用。
    适用场景：多线程分批计算、批量任务同步执行、分组处理数据。
    核心方法：await()线程到达屏障并等待；可指定屏障动作，所有线程到达后自动执行。
3. Semaphore（信号量）
    核心功能：控制同时访问某一资源的线程数量，实现限流、资源管控，类似“许可证”机制，线程需先获取许可才能执行，用完释放许可。
    适用场景：接口限流、数据库连接池、文件读写并发管控、稀缺资源共享。
    核心方法：acquire()获取许可；release()释放许可，必须在finally中执行，避免许可泄露。
4. Exchanger（交换器）
    核心功能：专门用于两个线程之间的数据交换，点对点通信，线程到达交换点后阻塞，直至对方线程到达，完成数据交换后继续执行。
    适用场景：线程间数据配对、数据校对、双向数据传递。
    核心方法：exchange(V data) 发送自身数据并获取对方数据，阻塞等待对方线程。
    五、线程间通信核心注意事项
    1. 虚假唤醒规避
    无论使用wait()还是await()，绝对禁止用if语句单次判断等待条件，必须用while循环反复检查，防止虚假唤醒导致线程在条件不满足时执行业务逻辑，引发数据错误。
2. 锁释放规范
    传统synchronized方式无需手动释放锁，等待方法会自动释放；Lock显式锁必须在finally代码块中调用unlock()，确保异常场景下锁也能正常释放，杜绝死锁风险。
3. 线程中断处理
    wait()、await()、await()等方法都会响应线程中断，抛出InterruptedException，捕获异常后需调用Thread.currentThread().interrupt()恢复中断标志，避免中断信号丢失，同时做好资源清理。
4. 通信载体一致性
    多个线程通信必须共用同一个共享对象、同一个锁、同一个Condition实例，若锁或通信载体不一致，同步与通信完全失效，无法实现线程协同。
5. 工具使用边界
    CountDownLatch为一次性工具，计数归零后无法复用，需循环等待场景改用CyclicBarrier；Semaphore获取与释放许可数量必须一致，避免资源泄露；Condition必须与对应Lock绑定，不可跨锁使用。
6. 性能优化要点
    优先使用signal()而非signalAll()、用多Condition精准唤醒，减少无效线程竞争与上下文切换；临界区内仅执行核心共享资源操作，耗时逻辑移出同步代码块，缩短锁持有时间；避免滥用notifyAll()，减少线程唤醒开销。
    六、场景选择与总结
    线程间通信的核心是基于共享资源的等待-唤醒协同，不同场景适配不同方案：简单协作场景选用Object wait/notify，开发成本低；复杂多条件场景选用Lock+Condition，灵活性强；批量线程调度直接使用JUC高级工具，提升开发效率。
    实战中需遵循“最小化锁粒度、精准唤醒线程、杜绝死锁与虚假唤醒”的原则，兼顾线程安全与系统性能，写出稳定、高效的并发代码。
