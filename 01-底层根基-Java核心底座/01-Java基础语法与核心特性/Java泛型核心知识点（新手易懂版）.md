Java泛型核心知识点

泛型就是Java中带<>尖括号的语法，是JDK5引入的核心特性，本质是类型参数化，把数据类型当作参数传递，让类、接口、方法可以适配多种数据类型，同时提前约束类型，避免运行时类型转换异常。结合你之前学习的设计模式代码，集合、工具类、通用组件里的<>全都是泛型，吃透这些核心知识点，就能彻底看懂相关代码。
核心一句话：泛型=给代码“贴类型标签”，编译期检查类型，杜绝错放数据、取出后强转报错，让代码更安全、更通用、更简洁。

一、泛型的核心作用
提高代码安全性：编译阶段就校验数据类型，把运行时的ClassCastException（类型转换异常）提前到编译期解决，避免程序运行崩溃
消除强制类型转换：取出数据时无需手动强转，代码更简洁，减少冗余写法
实现代码复用：一套通用代码可以处理多种类型，不用为每个类型单独写类或方法，比如通用List、Map，不用针对String、自定义类各写一套集合
提升代码可读性：通过<>直接看出容器或方法能处理的类型，一眼看懂代码用途

二、泛型的基础语法与常用占位符
1. 基础语法格式
    泛型通过尖括号<类型>标识，类型只能写引用类型（类、接口、数组），禁止写基本数据类型（int、long、boolean等，需替换为对应包装类Integer、Long、Boolean）。
    JDK7及以上支持菱形语法：右侧<>可以不写具体类型，自动匹配左侧的泛型类型，简化写法。
    // 标准写法
    List<String> list = new ArrayList<String>();
    // 菱形语法（推荐，简化代码）
    List<String> list = new ArrayList<>();
    // 错误写法：泛型不支持基本类型
    List<int> list = new ArrayList<>();
2. 常用泛型占位符（约定俗成，方便理解）
    占位符只是字母标识，无强制规定，日常开发遵循通用约定，看到就能明白含义：
    E：Element（元素），多用于集合，代表存储的元素类型，例：List<E>
    T：Type（类型），代表普通任意类型，例：class Generic<T>
    K：Key（键），多用于Map的键，例：Map<K,V>
    V：Value（值），多用于Map的值，例：Map<K,V>
    U/R：代表其他额外类型


三、泛型的三大分类（对应实战场景）
    泛型主要分为三类，覆盖日常开发和设计模式中的所有泛型场景，逐个拆解说明：
    1. 泛型类（设计模式常用）
    在类名后加<占位符>，整个类内都可以使用该泛型类型，成员变量、方法参数、返回值都能复用，适合通用工具类、业务基类。
    // 定义泛型类，T为占位符
    public class Result<T> {
    private T data;
    // 泛型方法参数
    public void setData(T data) {
        this.data = data;
    }
    // 泛型返回值
    public T getData() {
        return data;
    }
    }
    // 使用泛型类，指定具体类型为String
    Result<String> result1 = new Result<>();
    // 指定具体类型为自定义类（适配设计模式场景）
    Result<MenuComponent> result2 = new Result<>();
3. 泛型接口
    和泛型类用法一致，在接口名后加<占位符>，实现类可以指定具体类型，也可以继续保留泛型。
    // 定义泛型接口
    public interface BaseService<T> {
    T getById(Long id);
    }
    // 实现类指定具体类型
    public class UserServiceImpl implements BaseService<User> {
    @Override
    public User getById(Long id) {
        return null;
    }
    }
4. 泛型方法
    在方法返回值前加<占位符>，仅当前方法使用泛型，灵活适配单个方法的通用需求，静态方法必须用泛型方法，不能使用类的泛型。
    public class GenericMethod {
    // 泛型方法：返回值、参数都用泛型T
    public static <T> T getFirstElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
    }
    // 调用泛型方法，自动识别类型
    List<String> strList = new ArrayList<>();
    String str = GenericMethod.getFirstElement(strList);
    
    
四、泛型在集合中的使用（最常用）
    日常开发和设计模式代码中，泛型90%用在集合里，核心是约束集合存储的元素类型，彻底避免类型混乱。
    List集合：单类型元素约束，List<String>只能存字符串，List<RoleFlyweight>只能存角色对象（享元模式）
    Set集合：去重单类型约束，Set<Integer>只能存不重复整数
    Map集合：双类型约束，键和值分别指定类型，Map<String, MenuComponent>键为字符串，值为菜单对象（组合模式）
    // 组合模式中的容器集合，约束只能存菜单组件
    private List<MenuComponent> childMenuList = new ArrayList<>();
    // 享元模式中的享元池，约束键和值类型
    private static final Map<String, RoleFlyweight> FLYWEIGHT_POOL = new ConcurrentHashMap<>();


五、泛型通配符（适配灵活场景）
    通配符用?表示，代表任意类型，解决泛型无法协变的问题，分为三种，适用于参数接收、返回值限定场景：
    ? 无界通配符：代表任意类型，只能读取，不能添加元素（除null） 例：public void printList(List<?> list) { ... }
    ? extends T 上界通配符：代表T或T的子类，只读不写，适合读取数据 例：List<? extends Number> list，可接收Integer、Double类型集合
    ? super T 下界通配符：代表T或T的父类，可写不可读，适合添加数据 例：List<? super Number> list，可接收Number、Object类型集合

六、泛型的核心限制与注意事项（必记避坑）
    不支持基本数据类型：必须使用包装类，int→Integer，long→Long，boolean→Boolean
    无法使用泛型类型创建对象：不能new T()，因为泛型在编译后会类型擦除，JVM无法识别具体类型
    静态方法不能使用类泛型：静态方法属于类，不属于实例，必须单独定义泛型方法
    泛型类型在运行时会被擦除：Java泛型是伪泛型，编译后<>会消失，字节码中变回原始类型，目的是兼容旧代码
    不能创建泛型数组：禁止new List<String>[]，会引发类型安全问题
    泛型类型不参与继承：List<Object>不是List<String>的父类，二者无继承关系，不能相互赋值
    异常类不能定义泛型：泛型异常无法被精准捕获，语法不支持

七、泛型常见误区（新手高频踩坑）
    误区1：菱形语法是空指针？不是，是JDK7+简化写法，自动匹配左侧类型
    误区2：泛型可以存任意类型？不是，一旦指定类型，只能存该类型及子类对象
    误区3：List<?>和List<Object>一样？不一样，List<?>只读不可添加，List<Object>可添加任意对象
    误区4：泛型会影响运行性能？不会，编译后泛型被擦除，运行时无额外性能损耗
    组合模式：List<MenuComponent> 约束容器菜单只能存菜单组件，避免混入其他对象，保证树形结构规范
    享元模式：Map<String, RoleFlyweight> 约束享元池的键值类型，确保共享对象类型统一，避免错乱
    装饰模式/适配器模式：泛型接口定义通用规范，适配不同类型的被装饰、被适配对象，实现一套代码适配多种类型

核心总结：Java泛型就是用<>约束类型的语法，核心是安全、通用、简洁，重点掌握集合泛型、泛型类、泛型方法，牢记“不支持基本类型、运行时类型擦除、静态方法单独泛型”三大注意事项，就能完全看懂所有带<>的Java代码，再也不会有理解障碍。
