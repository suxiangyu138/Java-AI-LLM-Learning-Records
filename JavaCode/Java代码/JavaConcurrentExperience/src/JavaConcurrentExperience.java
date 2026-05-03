import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JavaConcurrentExperience {
    private static int count = 0;
    private static final Lock lock = new ReentrantLock();
    private static final List<String> normalList = new ArrayList<>();
    private static final List<String> concurrentList = new CopyOnWriteArrayList<>();

    // 方案1：在main方法上声明两种异常
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("===== 1. 线程的3种创建方式 =====");
        Thread thread1 = new MyThread();
        thread1.start();

        Thread thread2 = new Thread(new MyRunnable());
        thread2.start();

        FutureTask<Integer> futureTask = new FutureTask<>(new MyCallable());
        Thread thread3 = new Thread(futureTask);
        thread3.start();

        // 获取Callable返回值
        int result = futureTask.get();
        System.out.println("Callable任务返回值：" + result);
        System.out.println();

        // ====================== 模块2：线程安全问题 & 同步机制 ======================
        System.out.println("===== 2. 线程安全 & 同步机制 =====");
        Thread unsafeThread1 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                count++;
            }
        });
        Thread unsafeThread2 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                count++;
            }
        });
        unsafeThread1.start();
        unsafeThread2.start();
        unsafeThread1.join();
        unsafeThread2.join();
        System.out.println("无锁情况下count值（预期20000）：" + count);

        count = 0;
        Thread safeThread1 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                incrementBySync();
            }
        });
        Thread safeThread2 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                incrementBySync();
            }
        });
        safeThread1.start();
        safeThread2.start();
        safeThread1.join();
        safeThread2.join();
        System.out.println("synchronized同步后count值（预期20000）：" + count);

        count = 0;
        Thread lockThread1 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                incrementByLock();
            }
        });
        Thread lockThread2 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                incrementByLock();
            }
        });
        lockThread1.start();
        lockThread2.start();
        lockThread1.join();
        lockThread2.join();
        System.out.println("Lock锁同步后count值（预期20000）：" + count);
        System.out.println();

        // ====================== 模块3：线程协作（CountDownLatch） ======================
        System.out.println("===== 3. 线程协作：CountDownLatch =====");
        int threadNum = 3;
        CountDownLatch latch = new CountDownLatch(threadNum);

        for (int i = 1; i <= threadNum; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    System.out.println("任务" + finalI + "开始执行");
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println("任务" + finalI + "执行完成");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        System.out.println("所有任务执行完毕，主线程继续运行");
        System.out.println();

        // ====================== 模块4：线程池（核心实战） ======================
        System.out.println("===== 4. 线程池核心操作 =====");
        ExecutorService executor = new ThreadPoolExecutor(
                2,
                4,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        for (int i = 1; i <= 5; i++) {
            int finalI = i;
            executor.submit(() -> {
                System.out.println("线程池任务" + finalI + "执行，线程名：" + Thread.currentThread().getName());
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("线程池所有任务执行完成");
        System.out.println();

        // ====================== 模块5：并发容器 vs 普通容器 ======================
        System.out.println("===== 5. 并发容器 vs 普通容器 =====");
        Thread normalThread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                normalList.add("normal" + i);
            }
        });
        Thread normalThread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                normalList.add("normal" + i);
            }
        });
        normalThread1.start();
        normalThread2.start();
        normalThread1.join();
        normalThread2.join();
        System.out.println("普通ArrayList大小（可能不准确）：" + normalList.size());

        Thread concurrentThread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                concurrentList.add("concurrent" + i);
            }
        });
        Thread concurrentThread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                concurrentList.add("concurrent" + i);
            }
        });
        concurrentThread1.start();
        concurrentThread2.start();
        concurrentThread1.join();
        concurrentThread2.join();
        System.out.println("CopyOnWriteArrayList大小（准确）：" + concurrentList.size());
    }

    private static synchronized void incrementBySync() {
        count++;
    }

    private static void incrementByLock() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }

    static class MyThread extends Thread {
        @Override
        public void run() {
            System.out.println("继承Thread的线程执行，线程名：" + Thread.currentThread().getName());
        }
    }

    static class MyRunnable implements Runnable {
        @Override
        public void run() {
            System.out.println("实现Runnable的线程执行，线程名：" + Thread.currentThread().getName());
        }
    }

    static class MyCallable implements Callable<Integer> {
        @Override
        public Integer call() {
            System.out.println("实现Callable的线程执行，线程名：" + Thread.currentThread().getName());
            return 100;
        }
    }
}