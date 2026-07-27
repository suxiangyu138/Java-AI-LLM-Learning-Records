# Spring高级49讲

> 深度剖析Spring5底层原理，涵盖BeanFactory、AOP代理、SpringMVC、SpringBoot启动流程、自动配置等核心机制，共174集完整课程。

### 导学

- `000` 导学

### 第一讲 BeanFactory & ApplicationContext

- `001` BeanFactory与ApplicationContext
- `002` BeanFactory功能
- `003` ApplicationContext功能1
- `004` ApplicationContext功能2,3
- `005` ApplicationContext功能4
- `006` 小结

### 第二讲 BeanFactory 底层实现

- `007` BeanFactory实现
- `008` BeanFactory实现
- `009` BeanFactory实现-后处理器排序
- `010` ApplicationContext实现1,2
- `011` ApplicationContext实现3
- `012` ApplicationContext实现4

### 第三讲 Bean生命周期、模板方法

- `013` Bean生命周期
- `014` 模板方法

### 第四讲 Bean后处理器、@Autowired底层

- `015` 常见Bean后处理器1,2
- `016` 常见Bean后处理器3
- `017` @Autowired Bean后处理器执行分析
- `018` @Autowired Bean后处理器执行分析

### 第五讲 工厂后处理器、组件扫描、@Bean、Mapper

- `019` 常见工厂后处理器
- `020` 工厂后处理器模拟实现-组件扫描
- `021` 工厂后处理器模拟实现-组件扫描
- `022` 工厂后处理器模拟实现-@Bean
- `023` 工厂后处理器模拟实现-Mapper
- `024` 工厂后处理器模拟实现-Mapper

### 第六讲 Aware接口、@Autowired失效场景

- `025` Aware与InitializingBean接口
- `026` @Autowired失效分析

### 第七讲 Bean初始化与销毁

- `027` 初始化与销毁

### 第八讲 Scope作用域、作用域失效解决方案

- `028` Scope
- `029` Scope失效解决1,2
- `030` Scope失效解决3,4

### 第九/十讲 AOP增强方式

- `031` AOP之ajc增强
- `032` AOP之agent增强

### 第十一讲 AOP代理：JDK动态代理、CGLIB代理

- `033` AOP之proxy增强-JDK
- `034` AOP之proxy增强-CGLIB

### 第十二讲 JDK动态代理底层源码、字节码、反射优化

- `035` JDK代理原理
- `036` JDK代理原理
- `037` JDK代理源码
- `038` JDK代理字节码生成
- `039` JDK反射优化

### 第十三讲 CGLIB代理、MethodProxy底层

- `040` CGLIB代理原理
- `041` CGLIB代理原理-MethodProxy

### 第十四讲 MethodProxy原理

- `042` MethodProxy原理
- `043` MethodProxy原理

### 第十五讲 Spring代理选择逻辑

- `044` Spring选择代理
- `045` Spring选择代理
- `046` Spring选择代理

### 第十六讲 AOP切点匹配

- `047` 切点匹配
- `048` 切点匹配

### 第十七讲 Advisor、切面创建、@Order执行顺序

- `049` Advisor与@Aspect
- `050` findEligibleAdvisors
- `051` wrapIfNecessary
- `052` 代理创建时机
- `053` 吐槽@Order
- `054` 高级切面转低级切面

### 第十八讲 通知统一转换、适配器、调用链（责任链）

- `055` 统一转换为环绕通知
- `056` 统一转换为环绕通知
- `057` 适配器模式
- `058` 调用链执行
- `059` 模拟实现调用链
- `060` 模拟实现调用链-责任链模式

### 第十九讲 动态通知调用

- `061` 动态通知调用
- `062` 动态通知调用

### 第二十讲 SpringMVC DispatcherServlet初始化

- `063` DispatcherServlet初始化
- `064` DispatcherServlet初始化
- `065` DispatcherServlet初始化

### 第二十一讲 HandlerMapping、HandlerAdapter

- `066` RequestMapping HandlerMapping
- `067` RequestMapping HandlerAdapter
- `068` RequestMappingHandlerAdapter-参数和返回值
- `069` RequestMappingHandlerAdapter-自定义参数处理器
- `070` RequestMappingHandlerAdapter-自定义返回值处理器
- `071` 参数解析器-准备
- `072` 参数解析器-准备
- `073` 参数解析器-@RequestParam 0-4
- `074` 参数解析器-组合模式
- `075` 参数解析器5-9
- `076` 参数解析器10-12

### 第二十二讲 获取参数名

- `077` 获取参数名
- `078` 获取参数名

### 第二十三讲 类型转换、ConversionService、数据绑定

- `079` 两套底层转换接口
- `080` 一套高层转换接口
- `081` 类型转换与数据绑定演示
- `082` Web环境下数据绑定演示
- `083` 绑定器工厂
- `084` 绑定器工厂-@InitBinder扩展
- `085` 绑定器工厂-ConversionService扩展
- `086` 绑定器工厂-默认ConversionService
- `087` 加餐-如何获取泛型参数

### 第二十四讲 @ControllerAdvice 全局绑定

- `088` @ControllerAdvice-@InitBinder
- `089` @ControllerAdvice-@InitBinder

### 第二十五讲 Controller方法完整执行流程

- `090` 控制器方法执行流程
- `091` 控制器方法执行流程
- `092` 控制器方法执行流程-代码

### 第二十六讲 @ModelAttribute 全局数据

- `093` @ControllerAdvice-@ModelAttribute

### 第二十七讲 返回值处理器

- `094` 返回值处理器
- `095` 返回值处理器-1
- `096` 返回值处理器-2-4
- `097` 返回值处理器-5-7

### 第二十八讲 MessageConverter 消息转换器

- `098` MessageConverter
- `099` MessageConverter

### 第二十九讲 @ControllerAdvice 全局响应处理

- `100` @ControllerAdvice-ResponseBodyAdvice
- `101` @ControllerAdvice-ResponseBodyAdvice

### 第三十讲 全局异常处理、Tomcat底层异常机制

- `102` 异常处理
- `103` 异常处理
- `104` @ControllerAdvice-@ExceptionHandler
- `105` Tomcat异常处理
- `106` Tomcat异常处理-自定义错误地址
- `107` Tomcat异常处理-BasicErrorController
- `108` Tomcat异常处理-BasicErrorController

### 第三十一~三十三讲 HandlerMapping & HandlerAdapter底层完整流程

- `109` HandlerMapping与HandlerAdapter
- `110` HandlerMapping与HandlerAdapter
- `111` HandlerMapping与HandlerAdapter
- `112` HandlerMapping与HandlerAdapter
- `113` HandlerMapping与HandlerAdapter
- `114` HandlerMapping与HandlerAdapter
- `115` HandlerMapping与HandlerAdapter
- `116` HandlerMapping与HandlerAdapter

### 第三十六讲 SpringMVC完整执行流程

- `117` MVC执行流程
- `118` MVC执行流程

### 第三十七讲 SpringBoot 骨架项目搭建

- `119` 构建Boot骨架项目

### 第三十八讲 Boot War包、内嵌/外置Tomcat

- `120` 构建Boot War项目
- `121` 构建Boot War项目-用外置Tomcat测试
- `122` 构建Boot War项目-用内嵌Tomcat测试

### 第三十九讲 SpringBoot 完整启动流程（构造+run方法）

- `123` Boot执行流程-构造
- `124` Boot执行流程-构造-1
- `125` Boot执行流程-构造-2
- `126` Boot执行流程-构造-3
- `127` Boot执行流程-构造-4-5
- `128` Boot执行流程-run-1
- `129` Boot执行流程-run-1
- `130` Boot执行流程-run-8-11
- `131` Boot执行流程-run-2,12
- `132` Boot执行流程-run-3
- `133` Boot执行流程-run-4
- `134` Boot执行流程-run-5
- `135` Boot执行流程-run-5
- `136` Boot执行流程-run-6
- `137` Boot执行流程-run-7
- `138` Boot执行流程-小结

### 第四十讲 Tomcat组件、内嵌Tomcat整合Spring

- `139` Tomcat重要组件
- `140` 内嵌Tomcat
- `141` 内嵌Tomcat与Spring整合

### 第四十一讲 SpringBoot 自动配置底层原理

- `142` 自动配置类原理
- `143` 自动配置类原理
- `144` AopAutoConfiguration
- `145` AopAutoConfiguration
- `146` 自动配置类2-4概述
- `147` 自动配置类2-DataSource
- `148` 自动配置类3-MyBatis
- `149` 自动配置类3-Mapper扫描
- `150` 自动配置类4-事务
- `151` 自动配置类5-MVC
- `152` 自定义自动配置类

### 第四十二讲 @Conditional 条件装配底层

- `153` 条件装配底层1
- `154` 条件装配底层2

### 第四十三讲 FactoryBean

- `155` FactoryBean

### 第四十四讲 @Indexed 注解

- `156` @Indexed

### 第四十五讲 Spring代理核心特点总结

- `157` Spring代理的特点
- `158` Spring代理的特点

### 第四十六讲 @Value 注解注入底层

- `159` @Value注入底层1
- `160` @Value注入底层2

### 第四十七讲 @Autowired 完整注入底层源码

- `161` @Autowired注入底层
- `162` @Autowired注入底层
- `163` @Autowired注入底层
- `164` @Autowired注入底层
- `165` @Autowired注入底层
- `166` @Autowired注入底层

### 第四十八讲 Spring事件监听机制

- `167` 事件监听器1
- `168` 事件监听器2
- `169` 事件监听器3
- `170` 事件监听器4
- `171` 事件监听器5

### 第四十九讲 事件发布器底层

- `172` 事件发布器1
- `173` 事件发布器2
