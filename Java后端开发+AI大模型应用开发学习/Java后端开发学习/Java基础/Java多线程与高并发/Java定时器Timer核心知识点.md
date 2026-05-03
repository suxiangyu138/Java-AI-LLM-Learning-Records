03.18 15:58
Java定时器Timer核心知识点
一、核心定义与底层原理
Java中的定时器主要指java.util.Timer类，搭配java.util.TimerTask抽象类使用，是JDK原生提供的、基于单线程+任务队列的定时任务调度工具，核心作用是实现任务的延迟单次执行、周期性重复执行，无需依赖第三方框架，适合轻量级定时场景。
底层运行逻辑：Timer内部维护一个优先级任务队列和一个独立的调度线程，所有定时任务都会加入该队列，调度线程循环从队列中取出到期任务执行，单线程特性决定了同一时间只能执行一个任务，任务执行顺序按到期时间先后排序。
核心角色分工：
Timer：定时器调度器，负责管理任务队列、启动调度线程、提交/取消任务；
TimerTask：抽象任务类，业务逻辑需继承该类并重写run()方法，run()方法内为具体定时执行的代码。
二、核心API与基础使用步骤
1. 核心API方法
方法
作用
Timer()
无参构造，创建默认定时器，调度线程为非守护线程
Timer(boolean isDaemon)
指定调度线程是否为守护线程，守护线程随主线程退出而终止
schedule(TimerTask task, long delay)
延迟delay毫秒后，单次执行任务
schedule(TimerTask task, Date time)
指定具体日期时间，单次执行任务
schedule(TimerTask task, long delay, long period)
延迟delay毫秒后，按period毫秒间隔，固定延迟周期性执行
scheduleAtFixedRate(TimerTask task, long delay, long period)
延迟delay毫秒后，按period毫秒间隔，固定速率周期性执行
cancel()
终止定时器，清空队列所有任务，调度线程彻底退出
TimerTask.cancel()
取消单个任务，阻止该任务后续执行（已在执行的任务无法取消）
2. 标准使用步骤
自定义任务类，继承TimerTask，重写run()方法，编写业务逻辑；
创建Timer实例，启动定时器；
调用schedule或scheduleAtFixedRate方法，提交定时任务；
任务完成或程序退出前，调用cancel()释放资源，避免线程残留。
3. 极简代码示例
import java.util.Timer;
import java.util.TimerTask;
public class JavaTimerDemo {
    public static void main(String[] args) {
        // 1. 创建定时器，设置为守护线程
        Timer timer = new Timer(true);
        // 2. 定义定时任务
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("定时任务执行，当前时间：" + System.currentTimeMillis());
            }
        };
        // 单次执行：延迟2秒执行
        timer.schedule(task, 2000);
        // 周期性执行：延迟1秒，每3秒执行一次
        // timer.schedule(task, 1000, 3000);
        // 主线程等待5秒后关闭定时器
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 3. 关闭定时器，释放资源
        timer.cancel();
        System.out.println("定时器已终止");
    }
}
三、两种核心调度模式（关键区别）
Timer的周期性任务分为固定延迟（schedule）和固定速率（scheduleAtFixedRate），二者执行逻辑差异极大，是高频考点和实战易错点：
1. 固定延迟（schedule）
以上一次任务执行结束时间为基准，计算下一次任务的执行时间。如果前一个任务执行耗时过长、被阻塞，后续任务会顺延执行，不会追赶延迟，任务间隔始终保持设定的period值，不会出现任务叠加，适合对执行频率不严格、避免任务并发的场景。
2. 固定速率（scheduleAtFixedRate）
以任务初始启动时间为基准，按固定周期计算每次执行时间，属于绝对时间调度。如果前一个任务延迟执行，后续任务会快速追赶，短时间内连续执行补齐次数，可能出现任务并发执行，适合严格按照固定频率执行、不能漏执行的场景（如定时统计、心跳上报）。
四、核心注意事项（高频坑点）
单线程调度缺陷：Timer底层仅一个调度线程，所有任务串行执行。若单个任务执行耗时过长、抛出未捕获异常，会直接导致调度线程终止，队列中所有剩余任务都无法执行，这是Timer最致命的缺点。
线程安全问题：Timer和TimerTask均非线程安全，多线程环境下操作同一个Timer实例需加锁，避免任务提交和取消冲突；
异常处理必须严谨：run()方法内必须捕获所有异常，禁止抛出未捕获异常，否则会直接杀死调度线程，所有定时任务失效；
资源必须手动释放：Timer的调度线程默认是非守护线程，若不调用cancel()关闭，即使主线程退出，定时器线程仍会存活，导致JVM无法正常退出，造成线程泄漏；
不支持复杂调度：仅支持固定延迟、固定速率，无法实现cron表达式、动态修改周期、任务分片等复杂需求，灵活性极差；
精度有限：执行精度依赖系统线程调度，受CPU负载、GC停顿影响，无法做到毫秒级绝对精准，不适合高精度定时场景；
任务不可重复提交：一个TimerTask实例只能提交给一个Timer执行一次，重复提交会抛出异常，需重新创建实例。
五、适用场景与替代方案
1. 适用场景
轻量级、单任务、简单定时需求，比如小型工具类延迟执行、简单周期性日志打印、单机轻量级心跳，不适合生产环境复杂定时任务、高并发、多任务调度场景。
2. 推荐替代方案（生产环境必备）
ScheduledExecutorService：JDK1.5+提供的并发工具，基于线程池调度，支持多线程并行执行任务，单个任务异常不影响其他任务，稳定性远超Timer，是原生定时任务首选；
Quartz：功能强大的开源定时框架，支持cron表达式、集群部署、任务持久化、动态调度，适合复杂企业级定时任务；
Spring Task：Spring框架内置定时工具，注解式开发，支持cron表达式，整合Spring项目极简，日常开发最常用。
六、核心总结
Java原生Timer是入门级定时工具，核心优势是轻量、无依赖、上手简单，但单线程、异常敏感、功能单一三大缺陷决定了它不适合生产环境。日常学习需掌握其底层原理、两种调度模式和避坑要点，实际项目开发中，优先选用ScheduledExecutorService或Spring Task替代，兼顾稳定性和灵活性。

