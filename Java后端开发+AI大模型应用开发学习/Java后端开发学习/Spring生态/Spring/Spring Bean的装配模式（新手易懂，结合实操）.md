03.20 17:47
Spring Bean的装配模式（新手易懂，结合实操）
Spring Bean的装配模式，本质是「Spring容器创建Bean、注入依赖的方式」，核心目的是实现Bean的解耦和灵活管理。结合此前学习的Spring IOC/DI思想，Bean装配是IOC的具体落地方式，常用装配模式分为3大类，每类模式有其适用场景，新手需重点掌握核心模式的用法，避免装配错误。
补充说明：Bean装配的核心前提——所有被装配的Bean，必须被Spring容器识别（即通过配置或注解标识为Bean），否则无法完成装配。以下讲解的所有模式，均基于Spring 5.x版本（适配JDK 8+），与此前Spring基本应用的环境、案例保持一致。
一、核心装配模式分类（3大类，重点掌握前2类）
Spring Bean装配模式主要分为「手动装配」和「自动装配」两大类，其中自动装配是实际开发中最常用的方式；此外还有「基于注解的装配」（自动装配的延伸，简化配置），三类模式各有优劣，需根据开发场景选择。
二、手动装配模式（传统方式，易懂但繁琐）
手动装配，即开发者通过XML配置文件，手动指定Bean的创建和依赖注入，Spring容器仅执行开发者的配置指令，不自动推断依赖。适合简单项目或需要精准控制Bean依赖的场景，新手可先通过手动装配理解Bean装配的核心逻辑。
手动装配主要有2种方式，均基于XML配置（与此前XML配置Bean的方式一致）：
1. 构造方法装配（强制依赖，推荐用于必须存在的依赖）
通过Bean的构造方法，手动将依赖的Bean注入到当前Bean中，要求当前Bean必须提供对应参数的构造方法，Spring容器会根据构造方法的参数，匹配对应的Bean完成注入。
实操案例（延续此前UserService、UserDao案例）：
// 1. 改造UserService，提供含UserDao参数的构造方法
public class UserService {
    private UserDao userDao;
    // 构造方法（参数为依赖的UserDao）
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }
    public void getUserInfo() {
        userDao.queryUser();
    }
}
2. XML配置（手动指定构造方法注入）：
<!-- 1. 配置UserDao Bean -->
<bean id="userDao" class="com.example.dao.UserDao"/>
<!-- 2. 构造方法装配UserService，通过constructor-arg指定依赖 -->
<bean id="userService" class="com.example.service.UserService"&gt;
    <!-- constructor-arg：构造方法参数，ref指向依赖的Bean的id -->
    <constructor-arg ref="userDao"/>
</bean>
关键注意事项：
若Bean有多个构造方法，可通过「index」（参数索引，从0开始）或「name」（参数名称）区分，避免注入错误。
构造方法装配是「强制依赖」——若依赖的Bean不存在（未配置），Spring容器初始化会直接报错，适合依赖必须存在的场景（如Service依赖Dao）。
2. Setter方法装配（可选依赖，最常用的手动装配方式）
通过Bean的setter方法，手动将依赖的Bean注入到当前Bean中，要求当前Bean必须提供对应属性的setter方法，Spring容器通过调用setter方法，完成依赖注入。
该方式与此前Spring基本应用中演示的setter注入完全一致，是手动装配中最灵活的方式，适合依赖可选的场景（即依赖不存在时，Bean仍可创建）。
实操案例（简化版）：
<!-- UserDao Bean -->
<bean id="userDao" class="com.example.dao.UserDao"/>
<!-- Setter方法装配UserService，通过property指定依赖 -->
<bean id="userService" class="com.example.service.UserService"&gt;
    <!-- property：对应setter方法（setUserDao对应name="userDao"），ref指向依赖Bean -->
    <property name="userDao" ref="userDao"/>
</bean>
关键注意事项：
setter方法的命名规范：必须是「set+属性名（首字母大写）」，否则Spring无法识别（如userDao对应setUserDao()）。
可选依赖特性：若未配置<property>标签，依赖的属性会为null，但Bean仍能正常创建（不会报错），适合非必需的依赖。
三、自动装配模式（核心，简化配置，实际开发首选）
自动装配，即Spring容器根据「约定规则」，自动识别Bean的依赖，无需开发者手动配置依赖关系（无需写<constructor-arg>或<property>），大幅简化XML配置，降低耦合度。
自动装配的核心是「约定优于配置」，Spring提供5种自动装配模式，新手重点掌握前3种（常用），后2种几乎不用。
自动装配模式详解（基于XML配置，通过autowire属性指定）
在<bean>标签中添加「autowire」属性，指定自动装配模式，格式：<bean id="xxx" class="xxx" autowire="装配模式"/>
1. byName（按名称自动装配，最常用） 规则：Spring容器根据「当前Bean的依赖属性名」，去容器中查找「id与属性名一致」的Bean，找到后自动注入。 示例：UserService的依赖属性名是userDao，Spring会自动查找id="userDao"的Bean，注入到userService中。 实操配置： <bean id="userDao" class="com.example.dao.UserDao"/> <!-- byName自动装配：根据userService的依赖属性名（userDao），匹配id=userDao的Bean --> <bean id="userService" class="com.example.service.UserService" autowire="byName"/> 注意：依赖属性名必须与Bean的id完全一致（区分大小写），否则无法装配（属性为null）。
2. byType（按类型自动装配，常用） 规则：Spring容器根据「当前Bean的依赖属性类型」，去容器中查找「类型与属性类型一致」的Bean，找到后自动注入。 示例：UserService的依赖属性userDao的类型是UserDao，Spring会自动查找所有类型为UserDao的Bean，注入到userService中。 实操配置： <bean id="userDao" class="com.example.dao.UserDao"/> <!-- byType自动装配：根据userService的依赖属性类型（UserDao），匹配类型一致的Bean --> <bean id="userService" class="com.example.service.UserService" autowire="byType"/> 注意：容器中「不能有多个同类型的Bean」，否则Spring无法判断注入哪个，会报NoUniqueBeanDefinitionException异常。
3. constructor（按构造方法自动装配，对应手动构造方法装配） 规则：Spring容器根据当前Bean的构造方法参数类型，去容器中查找对应类型的Bean，自动注入到构造方法中，与手动构造方法装配效果一致。 适用场景：依赖是强制依赖（必须存在），且Bean有含依赖参数的构造方法。 实操配置： <bean id="userDao" class="com.example.dao.UserDao"/> <!-- constructor自动装配：根据UserService的构造方法参数类型，匹配UserDao类型的Bean --> <bean id="userService" class="com.example.service.UserService" autowire="constructor"/>
4. default（默认装配，不常用） 规则：继承当前XML配置文件中「beans标签的default-autowire属性」的配置，若beans标签未配置，则等同于no。
5. no（不自动装配，默认值） 规则：不进行自动装配，所有依赖必须手动配置（即手动装配模式），是Spring的默认配置。
四、基于注解的装配（自动装配的延伸，最简化，实际开发主流）
基于注解的装配，是在自动装配的基础上，通过注解替代XML配置，进一步简化开发，无需配置XML的autowire属性，也无需手动配置依赖，Spring容器通过注解自动识别Bean和依赖。
这种方式与此前Spring基本应用中演示的注解配置一致，是目前企业开发中最常用的装配模式，核心注解有2类：
1. 标识Bean的注解（告诉Spring哪些类是Bean，用于装配的前提）
@Component：通用注解，标识普通Bean（适用于任何层）。
@Service：用于业务层（Service）Bean，语义更清晰（推荐）。
@Repository：用于数据访问层（Dao）Bean，语义更清晰（推荐）。
@Controller：用于控制层（Controller）Bean（后续JavaWeb、SpringMVC中常用）。
2. 依赖注入的注解（自动完成装配，核心）
@Autowired（最常用，按类型自动装配） 规则：默认按「类型」自动装配，与XML中的byType模式一致；若容器中有多个同类型Bean，需结合@Qualifier注解指定Bean的id（按名称装配）。 实操案例： @Service // 标识UserService为Bean public class UserService { // @Autowired：自动装配UserDao类型的Bean @Autowired // 若有多个UserDao类型Bean，添加@Qualifier指定id：@Qualifier("userDao1") private UserDao userDao; public void getUserInfo() { userDao.queryUser(); } } @Repository // 标识UserDao为Bean public class UserDao { public void queryUser() { System.out.println("查询用户信息"); } }
@Resource（按名称/类型自动装配，补充） 规则：默认按「名称」自动装配（与byName一致），若未找到对应名称的Bean，再按类型装配（与byType一致）；可通过name属性指定Bean的id。 示例：@Resource(name = "userDao") private UserDao userDao;
关键配置：需在XML中添加「注解扫描」，告诉Spring去哪里查找带有注解的Bean（否则注解无效）：
<context:component-scan base-package="com.example"/>
五、三种装配模式对比（新手选型参考）
装配模式
核心方式
优点
缺点
适用场景
手动装配（构造/setter）
XML配置，手动指定依赖
配置清晰，精准控制依赖，易排查问题
配置繁琐，代码冗余，维护成本高
简单项目、依赖关系复杂（需精准控制）
自动装配（byName/byType）
XML配置autowire属性，Spring自动匹配
简化XML配置，降低耦合
依赖关系不直观，排查问题较难
中等规模项目，依赖关系简单
注解装配（@Autowired等）
注解标识Bean和依赖，Spring自动装配
配置最简化，开发效率高，语义清晰
注解分散在代码中，全局配置不直观
企业开发、大规模项目（主流选型）
六、Bean装配的核心注意事项（新手避坑重点）
装配的前提：被装配的Bean（依赖对象）必须被Spring容器管理（即通过XML配置或注解标识为Bean），否则Spring无法找到依赖，报NoSuchBeanDefinitionException异常。
自动装配的冲突问题：byType和@Autowired（默认）模式下，容器中不能有多个同类型Bean，否则会报错；解决方式：用@Qualifier（注解）或指定Bean的id（XML）区分。
注解扫描的范围：<context:component-scan>的base-package必须正确（指定Bean所在的包路径），否则Spring无法扫描到注解，导致Bean无法装配。
手动装配与自动装配可混用：若部分依赖需要精准控制，可手动装配；其他依赖可自动装配，Spring会优先执行手动装配的配置。
Bean的作用域影响：默认单例Bean（singleton），装配时容器会复用同一个Bean实例；若为原型Bean（prototype），每次装配都会创建新的Bean实例。
版本适配：注解装配需导入spring-context包（此前Maven配置已包含），若缺少该包，注解会无效，导致装配失败。
七、实操总结（新手必练）
1. 新手入门：先掌握「手动装配（setter方法）」，理解Bean依赖注入的核心逻辑；
2. 实际开发：优先使用「注解装配（@Service/@Repository + @Autowired）」，简化配置，提高开发效率；
3. 避坑关键：记住「装配的前提是Bean被容器管理」，避免同类型Bean冲突，注解扫描范围正确。
结合此前Spring基本应用的案例，动手修改装配模式（手动→自动→注解），对比不同模式的差异，快速掌握Bean装配的核心用法，为后续学习Spring Boot（自动装配的极致简化）打下基础。

