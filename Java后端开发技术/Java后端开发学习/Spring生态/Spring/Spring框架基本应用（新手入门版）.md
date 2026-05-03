03.20 17:39
Spring框架基本应用（新手入门版）
Spring是Java EE领域的轻量级开源框架，核心定位是“简化开发、解耦依赖”，通过IOC（控制反转）和AOP（面向切面编程）两大核心特性，解决传统Java开发中代码耦合度高、维护困难等问题，广泛应用于JavaWeb、微服务等开发场景。
本教程聚焦Spring基本应用，从核心概念入手，结合简单实操案例，帮助新手快速掌握Spring的基础用法，为后续学习Spring Boot、Spring Cloud奠定基础。
一、Spring核心概念（必懂，奠定应用基础）
学习Spring基本应用前，需先掌握3个核心概念，理解其设计思想，才能更好地运用框架：
1. IOC（控制反转）：核心核心思想
传统Java开发中，对象的创建、依赖的管理均由开发者手动控制（例如通过new关键字创建对象、手动注入依赖），导致代码耦合度高，修改一个对象可能影响多个相关对象。
Spring的IOC机制颠覆了这种方式：将对象的创建、依赖注入、生命周期管理等权力，全部交给Spring容器（Container），开发者无需手动创建对象，只需通过配置告诉Spring“需要什么对象”，Spring会自动完成对象的创建和组装，实现“控制权反转”。
简单理解：以前是“开发者管对象”，现在是“Spring管对象”，开发者只需要“用对象”，无需关心对象的创建过程。
2. DI（依赖注入）：IOC的具体实现
DI（Dependency Injection，依赖注入）是IOC的具体实现方式，指Spring容器在创建对象时，自动将该对象所需的依赖对象（如Service、Dao层对象）注入到当前对象中，无需开发者手动赋值。
常见的依赖注入方式有3种（新手重点掌握前2种）：
构造方法注入：通过对象的构造方法，将依赖对象传入，适合依赖对象必须存在的场景（强制依赖）。
setter方法注入：通过对象的setter方法，将依赖对象注入，适合依赖对象可选的场景（可选依赖）。
注解注入（常用）：通过@Autowired、@Resource等注解，快速完成依赖注入，简化配置（后续实操重点讲解）。
3. Bean：Spring容器管理的对象
Spring容器中管理的所有对象，都称为Bean。简单来说，开发者定义的类（如Service类、Dao类），通过配置告诉Spring，Spring就会将其创建为Bean，并存入容器中，供开发者随时获取和使用。
Bean的核心特点：由Spring容器创建和管理，生命周期由Spring控制（从创建、初始化到销毁，无需开发者干预）。
二、Spring基本应用前提：环境搭建
Spring基本应用依赖JDK和Spring核心jar包，需先完成环境搭建（以Spring 5.x版本为例，适配JDK 8及以上，与前文JavaWeb环境兼容）。
1. 环境要求
JDK：JDK 8及以上（推荐JDK 8，兼容性最好，与Spring 5.x完美适配）。
Spring版本：Spring 5.x（稳定版，新手首选，避免使用过高版本，减少兼容性问题）。
开发工具：IntelliJ IDEA（推荐，与Spring集成度高，操作便捷）。
2. 导入核心jar包
Spring基本应用需导入4个核心jar包（新手可通过Maven导入，无需手动下载，简化配置）：
spring-core：Spring核心包，提供IOC、DI的核心实现。
spring-beans：Bean相关包，负责Bean的创建、管理。
spring-context：Spring上下文包，负责容器的初始化和Bean的装配。
spring-expression：Spring表达式包，支持表达式语言（可选，基础应用可暂不导入）。
Maven依赖配置（直接复制到pom.xml中，IDEA会自动下载jar包）：
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>5.3.28</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <version>5.3.28</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-beans</artifactId>
    <version>5.3.28</version>
</dependency>
三、Spring基本应用实操（核心环节，新手必练）
Spring基本应用的核心流程：定义Bean → 配置Bean（告诉Spring管理哪些Bean） → 获取Spring容器 → 从容器中获取Bean并使用。以下通过“用户服务（UserService）调用数据访问（UserDao）”的简单案例，演示Spring的基本用法。
实操1：定义Bean（编写业务类、数据访问类）
先编写2个简单的类，作为Spring容器管理的Bean，体现依赖关系（UserService依赖UserDao）。
1. 数据访问层：UserDao（模拟数据查询）
// UserDao.java
public class UserDao {
    // 模拟查询用户信息
    public void queryUser() {
        System.out.println("查询用户信息：id=1，name=张三");
    }
}
2. 业务逻辑层：UserService（依赖UserDao）
// UserService.java
public class UserService {
    // 依赖UserDao对象（无需手动new，由Spring注入）
    private UserDao userDao;
    // 1. setter方法注入（重点）：提供UserDao的setter方法，供Spring注入
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
    // 业务方法：调用UserDao的查询方法
    public void getUserInfo() {
        userDao.queryUser(); // 使用注入的UserDao对象
    }
}
实操2：配置Bean（告诉Spring管理哪些Bean）
Spring配置Bean有2种方式：XML配置（传统方式，新手易理解）、注解配置（简化方式，实际开发常用），以下先讲解XML配置，再讲解注解配置。
方式1：XML配置（基础，必掌握）
1. 在src/main/resources目录下，创建Spring配置文件，命名为applicationContext.xml（文件名可自定义，建议统一规范）。
2. 配置文件内容（核心是<bean>标签，定义Bean及依赖注入）：
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd">
    <!-- 1. 配置UserDao Bean：告诉Spring创建UserDao对象，id为userDao（唯一标识） -->
    <bean id="userDao" class="com.example.dao.UserDao"&gt;&lt;/bean&gt;
    <!-- 2. 配置UserService Bean：id为userService，class为UserService的全路径 -->
    <bean id="userService" class="com.example.service.UserService"&gt;
        <!-- setter方法注入：将id为userDao的Bean注入到UserService的userDao属性中 -->
        <property name="userDao" ref="userDao"/>
    </bean>
</beans>
关键说明：
<bean>标签：id是Bean的唯一标识（后续获取Bean需用到），class是Bean的全类名（包名+类名）。
<property>标签：用于setter方法注入，name是UserService中依赖属性的名称（userDao），ref是要注入的Bean的id（userDao）。
方式2：注解配置（简化，实际开发常用）
注解配置可替代XML配置，简化代码，核心是通过注解告诉Spring“哪个类是Bean”“依赖哪个Bean”。
1. 给Bean类添加注解（@Component：通用注解，标识普通Bean）：
// UserDao.java
import org.springframework.stereotype.Component;
@Component // 告诉Spring，该类是Bean，由Spring管理
public class UserDao {
    public void queryUser() {
        System.out.println("查询用户信息：id=1，name=张三");
    }
}
// UserService.java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@Component // 标识为Bean
public class UserService {
    // @Autowired：自动注入依赖，Spring会自动找到UserDao类型的Bean，注入到该属性中
    @Autowired
    private UserDao userDao;
    // 无需setter方法（@Autowired可省略setter方法，直接注入）
    public void getUserInfo() {
        userDao.queryUser();
    }
}
2. 配置Spring注解扫描（告诉Spring去哪里找带有注解的Bean）：
在applicationContext.xml中添加扫描配置（只需1行，指定Bean所在的包路径）：
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context
                           http://www.springframework.org/schema/context/spring-context.xsd"&gt;
    <!-- 注解扫描：扫描com.example包下所有带有Spring注解的类 -->
    <context:component-scan base-package="com.example"/>
</beans>
补充注解（常用）：
@Service：用于标注业务层（Service）Bean，与@Component功能一致，语义更清晰。
@Repository：用于标注数据访问层（Dao）Bean，与@Component功能一致。
@Resource：与@Autowired功能类似，用于依赖注入，区别是@Autowired按类型注入，@Resource按名称注入。
实操3：获取Spring容器，使用Bean
Spring容器初始化后，开发者可通过容器获取Bean，无需手动创建对象，直接调用Bean的方法。编写测试类，演示完整流程：
// TestSpring.java
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
public class TestSpring {
    public static void main(String[] args) {
        // 1. 初始化Spring容器：加载XML配置文件（注解配置也需加载该文件）
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        // 2. 从容器中获取UserService Bean（id为userService，与XML配置中的id一致）
        UserService userService = (UserService) context.getBean("userService");
        // 3. 调用Bean的方法（此时UserDao已被Spring自动注入，可直接使用）
        userService.getUserInfo();
    }
}
实操4：运行结果与说明
运行TestSpring的main方法，控制台输出：查询用户信息：id=1，name=张三，说明Spring容器已成功创建UserDao和UserService Bean，并完成了依赖注入，Spring基本应用运行成功。
关键总结：整个过程中，我们没有手动new UserDao和UserService对象，也没有手动给UserService赋值userDao，全部由Spring容器完成，实现了解耦。
四、Spring基本应用的核心注意事项（新手避坑）
Bean的id必须唯一：在XML配置中，多个<bean>的id不能重复，否则Spring容器初始化会报错。
依赖注入的前提：被注入的Bean（如UserDao）必须被Spring容器管理，否则Spring无法找到依赖对象，会报NoSuchBeanDefinitionException异常。
注解扫描范围：<context:component-scan>的base-package必须正确，否则Spring无法扫描到带有注解的Bean，导致Bean无法创建。
@Autowired注入规则：默认按“类型”注入，若容器中有多个同类型的Bean，需结合@Qualifier注解指定Bean的id，否则会报NoUniqueBeanDefinitionException异常。
Spring容器的生命周期：ApplicationContext是Spring的核心容器，初始化后会自动创建所有单例Bean（默认是单例），销毁时会自动销毁Bean，无需开发者干预。
版本适配：Spring版本与JDK版本需适配（Spring 5.x适配JDK 8及以上，Spring 6.x适配JDK 17及以上），否则会出现编译或运行报错。
五、Spring基本应用拓展（新手进阶）
掌握基础应用后，可进一步学习以下内容，完善Spring应用能力：
Bean的作用域：默认是单例（singleton），即容器中只有一个Bean实例，可通过scope属性修改为原型（prototype，每次获取Bean都创建新实例）。
Bean的生命周期方法：通过@PostConstruct（初始化方法）、@PreDestroy（销毁方法），自定义Bean的初始化和销毁逻辑。
AOP基础：Spring的另一个核心特性，用于处理日志、事务、权限等横切逻辑，简化重复代码（后续可单独学习）。
Spring与JavaWeb结合：将Spring容器集成到JavaWeb项目中，实现Service、Dao层Bean的统一管理，为后续项目开发奠定基础。
至此，Spring框架的基本应用已全部讲解完成，核心是理解IOC和DI的思想，掌握Bean的定义、配置和使用流程。
新手建议多动手实操案例，熟悉注解配置和XML配置的用法，理解“解耦”的核心价值，为后续学习Spring全家桶打下坚实基础。

