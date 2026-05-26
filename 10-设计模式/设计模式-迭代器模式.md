Java后端开发：迭代器模式核心知识点全解析
一、模式基础定义与核心定位
迭代器模式（Iterator Pattern）属于行为型设计模式，核心宗旨是：提供一种统一的方式，遍历集合/容器对象中的各个元素，同时不暴露集合内部的底层存储结构（数组、链表、哈希表等），将遍历逻辑与集合本身解耦，让不同集合、不同结构都能通过同一套接口完成遍历操作。
简单来说，迭代器模式就是给各类容器做一个“通用遍历工具”，不管底层是ArrayList的数组、LinkedList的链表，还是自定义的树形、哈希结构，客户端都不用关心内部怎么存的，只用一套固定的迭代器方法就能挨个取出元素，彻底分离“集合本身”和“元素遍历”两大职责。它是Java集合框架（Collection、List、Set）的底层核心设计，也是我们日常写foreach循环、遍历各类集合的底层支撑，属于高频使用、但开发者常忽略其设计思想的经典模式，完美贴合单一职责与开闭原则，让集合扩展和遍历逻辑互不干扰。
核心设计思想：分离集合与遍历，统一遍历接口，屏蔽底层差异，按需遍历。迭代器专门负责遍历，集合专门负责存储，二者独立变化，新增集合类型只需配套新增迭代器，不改动原有遍历逻辑，客户端无感底层结构变化。
核心适用前提
需要遍历不同类型的集合/容器，且希望用统一的遍历方式，不用针对每种集合写单独遍历逻辑；
不想暴露集合内部的存储结构（数组、链表、树等），仅对外提供安全的元素访问方式；
需要支持多种遍历方式（正序、倒序、跳跃遍历），且遍历逻辑可独立扩展；
遍历过程中需要支持安全删除、暂停遍历、继续遍历等灵活操作；
后端常见场景：Java集合类（List/Set/Map遍历）、自定义容器遍历、树形结构层级遍历、分页迭代、数据流逐行读取。
二、四大核心角色（Java后端标准规范）
迭代器模式角色分工清晰，和Java源码中的Iterator、Iterable接口完全对应，贴合后端开发实际使用场景，便于理解JDK源码设计，核心包含四大角色，职责边界明确，和其他行为型模式逻辑统一，方便整体串联学习：
抽象迭代器（Iterator）：顶层迭代器接口，定义统一的遍历方法，比如hasNext()（判断是否有下一个元素）、next()（获取下一个元素）、remove()（删除元素，可选），规范所有迭代器的遍历行为，是实现统一遍历的核心。
具体迭代器（Concrete Iterator）：实现抽象迭代器接口，针对特定集合的底层结构，实现具体的遍历逻辑，记录当前遍历的位置（游标），完成元素获取、位置移动、元素删除等操作，专属对应某一种集合。
抽象聚合（Aggregate/Iterable）：顶层集合/容器接口，定义创建迭代器的方法，比如iterator()，规范所有集合对外提供迭代器的统一入口，让客户端无需关心迭代器创建细节。
具体聚合（Concrete Aggregate）：实现抽象聚合接口，是实际存储元素的集合对象，封装内部存储结构（数组、链表等），重写迭代器创建方法，返回对应自身的具体迭代器实例。
简单记忆：抽象迭代器定遍历规范，具体迭代器做实际遍历，抽象聚合定集合规范，具体聚合存数据并造迭代器，四层完全解耦，遍历逻辑和存储结构互不影响。
三、Java源码中的迭代器（JDK原生实现）
Java本身已经通过原生接口完美实现了迭代器模式，日常开发中我们无需手写基础迭代器，直接使用即可，这也是迭代器模式最核心的落地场景，对应关系如下：
抽象迭代器：java.util.Iterator 接口，核心方法：hasNext()、next()、remove()；
抽象聚合：java.lang.Iterable 接口，核心方法：iterator()，所有Collection集合都实现了该接口；
具体聚合：ArrayList、LinkedList、HashSet等集合类，内部维护数据存储结构；
具体迭代器：集合内部的私有内部类（比如ArrayList的Itr、ListItr），实现Iterator接口，适配数组结构实现遍历。
我们日常使用的foreach循环，底层就是通过Iterable和Iterator实现的自动遍历，完全屏蔽了集合底层的数组、链表差异，这也是迭代器模式最直观的价值体现。
四、Java后端实战代码实现
为了彻底理解迭代器模式，这里手写一套自定义集合+自定义迭代器的完整代码，模拟ArrayList的数组存储结构，实现正序迭代器，贴合Java源码设计，代码可直接复用，清晰体现迭代器与集合的解耦逻辑，对比普通for循环，凸显迭代器模式的优势。
场景说明
自定义一个通用的数组集合容器，封装底层数组存储结构，对外不暴露数组细节，仅通过迭代器完成元素遍历，支持添加元素、获取迭代器、安全遍历操作，后续可轻松扩展链表集合、倒序迭代器，无需改动客户端遍历代码。
步骤1：定义抽象迭代器（Iterator）
复刻JDK Iterator风格，定义统一的遍历方法，规范所有迭代器行为。
/**
 * 抽象迭代器：定义统一遍历接口
     */
    public interface MyIterator<E> {
    /**
     * 判断是否存在下一个元素
     * @return 存在返回true，不存在返回false
     */
    boolean hasNext();
    /**
     * 获取下一个元素，并移动游标
     * @return 下一个元素
     */
    E next();
    /**
     * 删除当前元素（可选实现）
     */
    default void remove() {
        throw new UnsupportedOperationException("默认不支持删除操作");
    }
    }
    步骤2：定义抽象聚合（Iterable）
    定义集合顶层接口，规范获取迭代器的方法，所有自定义集合都要实现该接口。
    /**
 * 抽象聚合（集合）：定义创建迭代器的方法
     */
    public interface MyCollection<E> {
    /**
     * 获取迭代器
     * @return 对应集合的迭代器实例
     */
    MyIterator<E> iterator();
    /**
     * 添加元素
     * @param element 待添加元素
     */
    void add(E element);
    /**
     * 获取集合大小
     * @return 元素个数
     */
    int size();
    }
    步骤3：定义具体聚合（自定义数组集合）
    实现抽象集合接口，封装数组作为底层存储，对外隐藏数组细节，同时定义内部迭代器类。
    /**
 * 具体聚合：自定义数组集合，底层用数组存储元素
     */
    public class MyArrayList<E> implements MyCollection<E> {
    // 底层存储数组，对外完全隐藏
    private Object[] elementData;
    // 元素实际个数
    private int size;
    // 构造器，初始化默认容量
    public MyArrayList() {
        this.elementData = new Object[10];
        this.size = 0;
    }
    // 添加元素，扩容逻辑简化
    @Override
    public void add(E element) {
        elementData[size++] = element;
    }
    // 获取迭代器，返回内部自定义迭代器
    @Override
    public MyIterator<E> iterator() {
        return new ArrayListIterator();
    }
    @Override
    public int size() {
        return size;
    }
    /**
     * 具体迭代器：私有内部类，访问外部集合的元素，实现遍历逻辑
     * 完全适配数组结构，屏蔽底层细节
     */
    private class ArrayListIterator implements MyIterator<E> {
        // 遍历游标，记录当前位置
        private int cursor;
        public ArrayListIterator() {
            this.cursor = 0;
        }
        @Override
        public boolean hasNext() {
            // 游标未到末尾，说明还有元素
            return cursor < size;
        }
        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            return (E) elementData[cursor++];
        }
        // 重写删除方法
        @Override
        public void remove() {
            // 数组删除元素逻辑，简化实现
            System.arraycopy(elementData, cursor, elementData, cursor - 1, size - cursor);
            elementData[--size] = null;
        }
    }
    }
    步骤4：客户端调用（统一遍历，无感底层）
    客户端仅通过抽象迭代器和抽象集合接口遍历，完全不关心底层是数组还是其他结构，代码通用且简洁。
    public class Client {
    public static void main(String[] args) {
        // 1. 创建自定义集合
        MyCollection<String> collection = new MyArrayList<>();
        // 2. 添加元素
        collection.add("Java");
        collection.add("Python");
        collection.add("C++");
        collection.add("Go");
        // 3. 获取迭代器，统一遍历
        MyIterator<String> iterator = collection.iterator();
        System.out.println("=====自定义迭代器遍历=====");
        while (iterator.hasNext()) {
            String element = iterator.next();
            System.out.println("元素：" + element);
        }
        // 4. 模拟foreach遍历（底层原理一致）
        System.out.println("=====模拟foreach遍历=====");
        for (iterator = collection.iterator(); iterator.hasNext(); ) {
            System.out.println("元素：" + iterator.next());
        }
    }
    }
    五、迭代器模式的核心优势（Java后端价值）
    彻底分离集合与遍历职责：遵循单一职责原则，集合专注存储元素，迭代器专注遍历逻辑，二者独立开发、独立扩展，互不干扰，代码结构更清晰。
    统一遍历接口，屏蔽底层差异：无论集合底层是数组、链表、树还是哈希结构，客户端都用同一套hasNext()、next()方法遍历，无需针对不同集合写不同遍历逻辑，代码通用性极强。
    隐藏集合内部结构，保障安全性：集合底层存储数组、链表等细节完全对外屏蔽，仅通过迭代器安全访问元素，避免外部直接修改内部结构，提升数据安全性。
    支持多种遍历方式，扩展性强：一个集合可实现多个迭代器，比如正序、倒序、跳跃遍历，新增遍历方式只需新增迭代器类，无需修改集合本身，完全遵循开闭原则。
    支持灵活遍历操作：相比普通for循环，迭代器支持暂停遍历、中途删除、异步遍历等操作，遍历过程更灵活，尤其是遍历过程中删除元素，可避免普通for循环的下标越界问题。
    适配Java生态，无缝兼容：完全贴合JDK集合设计，手写自定义集合实现Iterable接口，即可直接使用foreach循环，兼容现有Java代码规范，适配后端各类集合场景。
    六、核心缺点与局限性
    类数量增多，结构略显复杂：每个自定义集合都需要配套一个迭代器类，集合类型较多时，会增加额外的类文件，轻度提升项目结构复杂度。
    遍历效率相对固定：迭代器是顺序逐一遍历，不适合需要随机访问的场景，比如数组直接通过下标访问，效率比迭代器更高。
    删除操作有局限性：迭代器遍历过程中，集合本身不能直接增删元素，否则会抛出并发修改异常（ConcurrentModificationException），只能通过迭代器自身的remove()方法操作。
    不适合复杂结构遍历：对于多层嵌套、网状结构，普通迭代器遍历难度大，需要额外定制迭代器，实现成本较高。
    单向遍历限制：基础迭代器多为单向遍历，如需双向遍历（前后均可），需要额外实现双向迭代器（如ListIterator）。
    七、Java后端高频落地场景
    Java集合框架遍历：ArrayList、LinkedList、HashSet、HashMap等集合的遍历，底层全依赖迭代器模式，foreach循环的底层实现。
    自定义容器遍历：后端开发中自定义的树形容器、数组容器、缓存容器，对外隐藏存储结构，通过迭代器提供统一遍历。
    数据流逐行读取：IO流读取、文件逐行解析、数据库结果集（ResultSet）遍历，本质也是迭代器思想，逐行获取数据。
    分页迭代加载：大数据量分页查询、懒加载迭代，通过迭代器控制分页加载逻辑，避免一次性加载全部数据。
    双向/特殊遍历：ListIterator双向迭代、倒序迭代器、过滤迭代器，满足特殊遍历需求。
    源码框架设计：MyBatis的结果集遍历、Spring的集合工具类、各类缓存框架的元素遍历，均基于迭代器模式。
    八、Java后端开发注意事项（避坑指南）
    禁止遍历过程中直接修改集合：迭代器遍历期间，不能通过集合本身的add/remove方法增删元素，否则抛出ConcurrentModificationException，必须用迭代器自带的remove()方法。
    优先使用JDK原生迭代器：日常开发无需手写迭代器，直接使用Java自带的Iterator、ListIterator，避免重复造轮子，提升代码规范性。
    迭代器是一次性使用的：一个迭代器遍历完成后，游标无法重置，如需再次遍历，必须重新调用集合的iterator()方法获取新迭代器。
    区分迭代器与普通for循环：普通for循环适合随机访问、已知长度的集合；迭代器适合统一遍历、屏蔽底层结构、需要中途删除的场景，按需选择。
    并发场景需加锁：多线程环境下，一个线程遍历、一个线程修改集合，会出现并发安全问题，建议使用并发集合（CopyOnWriteArrayList）或遍历前加锁。
    迭代器不要共享使用：每个遍历流程单独获取迭代器，不要多个线程共用一个迭代器实例，避免游标错乱。
    自定义迭代器做好空值处理：手写迭代器时，做好元素空值、游标越界的判断，避免空指针和下标越界异常。
    贴合Iterable接口规范：自定义集合务必实现java.lang.Iterable接口，而非自定义迭代接口，这样才能直接使用foreach循环，兼容Java生态。
    九、迭代器模式核心易错点
    高频误区：误以为foreach循环是独立语法，实则底层依赖迭代器；误以为迭代器会修改集合结构，实则迭代器仅负责遍历，删除操作也是间接操作集合。 关键区分：迭代器模式核心是解耦遍历与存储，普通循环是硬编码遍历，依赖集合底层结构，灵活性和扩展性远不如迭代器。
    后端核心总结：迭代器模式是Java集合的底层灵魂，核心价值是“统一遍历、屏蔽底层、职责分离”，日常开发虽无需手写迭代器，但理解其设计思想，能更好地使用集合、排查遍历异常、自定义容器。开发中遵循JDK迭代器规范，规避并发修改、游标复用等坑点，就能安全高效地完成各类集合遍历操作。
