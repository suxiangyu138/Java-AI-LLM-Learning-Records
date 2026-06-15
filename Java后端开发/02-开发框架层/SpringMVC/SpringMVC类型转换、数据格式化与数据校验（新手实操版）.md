SpringMVC类型转换、数据格式化与数据校验（新手实操版）
结合此前学习的SpringMVC常用注解（@RequestParam、@ModelAttribute等）和标签库，我们知道Controller会接收前端传递的请求参数，并绑定到方法参数或实体类中，最终用于业务逻辑处理。
但前端传递的参数本质都是字符串类型（如表单输入、URL传参），而Controller方法参数可能是Integer、Date、Double等类型；同时，前端传递的日期、数字等数据格式可能不规范，参数也可能存在合法性问题（如年龄为负数、姓名为空）。
补充衔接：SpringMVC提供了三大核心功能解决上述问题——类型转换（将前端字符串参数转为后端所需类型）、数据格式化（规范日期、数字等数据的展示和接收格式）、数据校验（校验参数合法性），三者协同工作，确保后端接收的参数“类型正确、格式规范、数据合法”，减少手动处理代码，提升开发效率，是SpringMVCWeb开发中不可或缺的核心环节。
本教程仍基于Spring 5.x，延续User案例，每个功能均搭配实操代码，标注重点和避坑点，适配新手入门。
一、SpringMVC类型转换（核心：字符串→后端指定类型）
1. 核心概念
    前端传递的所有请求参数（无论是URL传参、表单提交，还是JSON数据），本质都是字符串类型；而后端Controller方法的参数可能是Integer、Long、Date、Boolean等类型，SpringMVC的类型转换功能，会自动将前端字符串参数，转换为后端方法所需的参数类型，无需开发者手动转换（如手动将String转为Integer）。
    关键：SpringMVC内置了大量常用类型的转换器，可满足大部分开发场景；若内置转换器无法满足需求（如自定义枚举类型转换），可自定义转换器。
2. 内置类型转换器（常用，无需配置）
    SpringMVC内置了多种常用转换器，无需额外配置，自动生效，新手重点掌握以下几种：
    前端参数类型（字符串）
    后端目标类型
    示例（前端→后端）
    数字字符串
    Integer、Long、Double、Float
    "100" → 100（Integer）、"123.45" → 123.45（Double）
    布尔字符串
    Boolean
    "true" → true、"false" → false（不区分大小写）
    日期字符串（特定格式）
    Date
    "2024-10-01" → Date对象（需配合数据格式化，后续讲解）
    数组字符串
    数组（如String[]、Integer[]）
    "1,2,3" → [1,2,3]（Integer[]）
3. 实操案例（内置转换器使用）
    结合User案例，Controller方法接收前端不同类型参数，SpringMVC自动完成类型转换：
    @Controller
    @RequestMapping("/user")
    public class UserController {
    // 1. 字符串→Integer（前端传递id="1"，自动转为Integer 1）
    @GetMapping("/get/{id}")
    public String getUser(@PathVariable Integer id, Model model) {
        System.out.println("接收的id（Integer类型）：" + id);
        User user = new User("张三", 20, "男");
        user.setId(id);
        model.addAttribute("user", user);
        return "userDetail";
    }
    // 2. 字符串→Double（前端传递score="98.5"，自动转为Double 98.5）
    @GetMapping("/score")
    @ResponseBody
    public String getScore(@RequestParam Double score) {
        System.out.println("接收的分数（Double类型）：" + score);
        return "分数：" + score;
    }
    // 3. 字符串→Boolean（前端传递enabled="true"，自动转为Boolean true）
    @GetMapping("/status")
    @ResponseBody
    public String getStatus(@RequestParam Boolean enabled) {
        System.out.println("接收的状态（Boolean类型）：" + enabled);
        return "状态：" + enabled;
    }
    // 4. 字符串→数组（前端传递ids="1,2,3"，自动转为Integer[]）
    @GetMapping("/batchDelete")
    @ResponseBody
    public String batchDelete(@RequestParam Integer[] ids) {
        System.out.println("接收的id数组（Integer[]）：" + Arrays.toString(ids));
        return "删除成功，id：" + Arrays.toString(ids);
    }
    }
    测试说明：前端请求URL示例（直接在浏览器输入即可测试）：
    http://localhost:8080/user/get/1 → id自动转为Integer 1
    http://localhost:8080/user/score?score=98.5 → score自动转为Double 98.5
    http://localhost:8080/user/status?enabled=true → enabled自动转为Boolean true
    http://localhost:8080/user/batchDelete?ids=1,2,3 → ids自动转为Integer[] [1,2,3]
4. 自定义类型转换器（进阶，适配特殊场景）
    当内置转换器无法满足需求时（如前端传递“男/女”，需转为自定义枚举类型；或日期格式不匹配），可自定义类型转换器，步骤如下（以“字符串→性别枚举”为例）：
    步骤1：定义枚举类型（GenderEnum）
    // 自定义性别枚举
    public enum GenderEnum {
    MALE("男"), FEMALE("女");
    private String gender;
    GenderEnum(String gender) {
        this.gender = gender;
    }
    // 根据字符串获取枚举对象（核心方法）
    public static GenderEnum getByGender(String gender) {
        for (GenderEnum e : GenderEnum.values()) {
            if (e.gender.equals(gender)) {
                return e;
            }
        }
        throw new IllegalArgumentException("性别参数不合法");
    }
    // getter方法
    public String getGender() {
        return gender;
    }
    }
    步骤2：自定义转换器（实现Converter接口）
    import org.springframework.core.convert.converter.Converter;
    // 自定义转换器：String → GenderEnum
    public class StringToGenderEnumConverter implements Converter<String, GenderEnum> {
    // 核心方法：将前端传递的字符串（如"男"）转为GenderEnum枚举
    @Override
    public GenderEnum convert(String source) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("性别参数不能为空");
        }
        // 调用枚举的静态方法，根据字符串获取枚举对象
        return GenderEnum.getByGender(source);
    }
    }
    步骤3：配置转换器（交给SpringMVC容器管理）
    通过XML配置或注解配置，将自定义转换器注册到SpringMVC中（以XML配置为例，与此前applicationContext.xml配置衔接）：
    <!-- 配置类型转换服务 -->
    <bean id="conversionService" class="org.springframework.context.support.ConversionServiceFactoryBean">
    <!-- 注册自定义转换器 -->
    <property name="converters">
        <set>
            <bean class="com.example.converter.StringToGenderEnumConverter"/>
        </set>
    </property>
    </bean>
    <!-- 开启SpringMVC注解驱动，并指定转换服务 -->
    <mvc:annotation-driven conversion-service="conversionService"/>
    步骤4：实操测试
    @GetMapping("/getByGender")
    @ResponseBody
    public String getByGender(@RequestParam GenderEnum gender) {
    System.out.println("接收的性别枚举：" + gender);
    return "性别：" + gender.getGender();
    }
    测试URL：http://localhost:8080/user/getByGender?gender=男 → 前端传递“男”，自动转为GenderEnum.MALE。
    二、SpringMVC数据格式化（核心：规范数据展示/接收格式）
    1. 核心概念
    数据格式化是对“类型转换”的补充，主要用于规范日期、数字等数据的接收和展示格式。例如：前端传递日期字符串“2024-10-01”，后端需转为Date对象，同时希望页面展示时格式为“2024年10月01日”；或前端传递数字“10000”，页面展示为“10,000”，这都需要通过数据格式化实现。
    SpringMVC提供了注解式格式化方式，无需额外配置，直接在实体类属性或方法参数上添加注解即可。
2. 常用格式化注解（必掌握）
    重点掌握日期格式化和数字格式化注解，直接作用于实体类属性或方法参数上，核心注解如下：
    （1）@DateTimeFormat：日期格式化（最常用）
    作用：指定日期的接收格式（前端→后端）和展示格式（后端→前端），核心属性pattern（指定格式字符串）。
    常用格式：yyyy-MM-dd（年-月-日）、yyyy-MM-dd HH:mm:ss（年-月-日 时:分:秒）、yyyy年MM月dd日。
    （2）@NumberFormat：数字格式化
    作用：指定数字的接收和展示格式，核心属性pattern（指定格式字符串），支持整数、小数、百分比等格式。
    常用格式：#,###（千分位分隔，如10,000）、#.##（保留2位小数，如123.45）、#%（百分比，如50%）。
3. 实操案例（结合User案例，实体类格式化）
    修改User实体类，添加格式化注解，实现日期、数字的规范接收和展示：
    import org.springframework.format.annotation.DateTimeFormat;
    import org.springframework.format.annotation.NumberFormat;
    import java.util.Date;
    public class User {
    private Integer id;
    private String name;
    // 数字格式化：年龄保留0位小数（整数），展示为千分位格式
    @NumberFormat(pattern = "#,###")
    private Integer age;
    private String gender;
    // 日期格式化：接收格式为yyyy-MM-dd，展示格式也为yyyy-MM-dd
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthday;
    // 数字格式化：保留2位小数（如薪资）
    @NumberFormat(pattern = "#.##")
    private Double salary;
    // 无参构造、有参构造、getter/setter、toString方法（省略，此前已定义）
    }
    案例1：表单提交（接收格式化数据）
    前端表单传递格式化数据，Controller自动接收并转换：
    // 接收表单提交的格式化数据
    @PostMapping("/add")
    public String addUser(@ModelAttribute User user, Model model) {
    System.out.println("接收的用户信息：" + user);
    // 传递格式化后的用户数据到页面展示
    model.addAttribute("user", user);
    return "userDetail";
    }
    前端表单（addUser.jsp，关键部分）：
    <form:form modelAttribute="user" action="/user/add" method="post">
    <p>姓名：<form:input path="name"/></p>
    <p>年龄：<form:input path="age" placeholder="如10000"/></p>
    <p>性别：<form:radiobutton path="gender" value="男"/>男 <form:radiobutton path="gender" value="女"/>女</p>
    <p>生日：<form:input path="birthday" placeholder="如2024-10-01"/></p>
    <p>薪资：<form:input path="salary" placeholder="如12345.67"/></p>
    <p><form:button type="submit">新增用户</form:button></p>
    </form:form>
    测试说明：前端输入“2024-10-01”（生日），后端自动转为Date对象；输入“12345”（年龄），后端自动转为Integer 12345，页面展示时会显示为“12,345”。
    案例2：页面展示格式化数据
    在userDetail.jsp页面，使用<c:out>标签展示格式化后的数据：
    <p>年龄：<c:out value="${user.age}"/></p> <!-- 展示为12,345 -->
    <p>生日：<c:out value="${user.birthday}"/></p> <!-- 展示为2024-10-01 -->
    <p>薪资：<c:out value="${user.salary}"/></p> <!-- 展示为12345.67 -->
    三、SpringMVC数据校验（核心：确保参数合法性）
    1. 核心概念
    数据校验是指对前端传递的参数进行合法性校验（如姓名不能为空、年龄必须为正数、手机号格式正确等），避免非法数据进入业务逻辑，导致程序异常或数据错误。SpringMVC支持JSR-380规范（Java Bean Validation），通过注解实现参数校验，无需手动编写校验代码。
    前提：需导入数据校验依赖（Maven），否则校验注解无效：
    <!-- 数据校验依赖（JSR-380） -->
    <dependency>
    <groupId>javax.validation</groupId>
    <artifactId>validation-api</artifactId>
    <version>2.0.1.Final</version>
    </dependency>
    <dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>6.1.7.Final</version>
    </dependency>
2. 常用校验注解（必掌握）
    校验注解作用于实体类属性或方法参数上，核心注解如下，标注了常用属性和说明：
    校验注解
    核心作用
    常用属性
    示例
    @NotNull
    参数不能为空（适用于引用类型）
    message：校验失败提示信息
    @NotNull(message = "姓名不能为空")
    @NotBlank
    字符串不能为空，且不能全为空格（适用于String）
    message：校验失败提示信息
    @NotBlank(message = "姓名不能为空或全为空格")
    @Min
    数字不能小于指定值（适用于数字类型）
    value：最小值；message：提示信息
    @Min(value = 1, message = "年龄不能小于1")
    @Max
    数字不能大于指定值（适用于数字类型）
    value：最大值；message：提示信息
    @Max(value = 150, message = "年龄不能大于150")
    @Pattern
    字符串需匹配指定正则表达式（如手机号、邮箱）
    regexp：正则表达式；message：提示信息
    @Pattern(regexp = "^1[3-9]\\\\d{9}$", message = "手机号格式错误")
    @Email
    字符串需符合邮箱格式
    message：提示信息
    @Email(message = "邮箱格式错误")
3. 实操案例（结合User案例，实体类校验）
    步骤1：修改User实体类，添加校验注解
    import javax.validation.constraints.*;
    import org.springframework.format.annotation.DateTimeFormat;
    import org.springframework.format.annotation.NumberFormat;
    import java.util.Date;
    public class User {
    private Integer id;
    // 姓名：不能为空、不能全为空格，长度1-50
    @NotBlank(message = "姓名不能为空或全为空格")
    @Size(min = 1, max = 50, message = "姓名长度需在1-50之间")
    private String name;
    // 年龄：不能为null，且1-150之间
    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄不能小于1")
    @Max(value = 150, message = "年龄不能大于150")
    @NumberFormat(pattern = "#,###")
    private Integer age;
    // 性别：不能为空，且只能是男/女
    @NotBlank(message = "性别不能为空")
    @Pattern(regexp = "^(男|女)$", message = "性别只能是男或女")
    private String gender;
    // 生日：不能为null，格式为yyyy-MM-dd
    @NotNull(message = "生日不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthday;
    // 邮箱：格式正确
    @Email(message = "邮箱格式错误")
    private String email;
    // getter/setter、toString方法（省略）
    }
    步骤2：Controller方法开启校验
    在Controller方法的实体类参数前添加@Valid注解，开启数据校验；同时添加BindingResult参数，用于接收校验结果（错误信息）。
    import org.springframework.validation.BindingResult;
    import org.springframework.validation.annotation.Validated;
    import org.springframework.web.bind.annotation.*;
    @Controller
    @RequestMapping("/user")
    public class UserController {
    // 新增用户，开启数据校验
    @PostMapping("/add")
    public String addUser(@Valid @ModelAttribute User user, BindingResult bindingResult, Model model) {
        // 1. 判断校验是否失败（有错误信息）
        if (bindingResult.hasErrors()) {
            // 2. 将错误信息存入模型，返回表单页面，显示错误提示
            model.addAttribute("errors", bindingResult.getAllErrors());
            // 返回新增用户表单页面，重新填写
            return "addUser";
        }
        // 校验通过，执行新增业务逻辑
        System.out.println("新增用户：" + user);
        model.addAttribute("user", user);
        return "userDetail";
    }
    }
    步骤3：JSP页面展示校验错误信息
    在addUser.jsp页面，添加错误信息展示逻辑，使用SpringMVC标签库的<form:errors>标签（表单标签库）：
    <%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
    <%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
    <html>
    <head>
    <title>新增用户</title>
    <style>
        .error { color: red; } /* 错误信息红色显示 */
    </style>
    </head>
    <body>
    <h1>新增用户</h1>
    <!-- 展示所有错误信息（可选） -->
    <c:if test="${not empty errors}">
        <div class="error">
            <c:forEach items="${errors}" var="error">
                <p>${error.defaultMessage}</p>
            </c:forEach>
        </div>
    </c:if>
    <form:form modelAttribute="user" action="/user/add" method="post">
        <p>
            姓名：<form:input path="name"/>
            <!-- 展示单个字段的错误信息 -->
            <form:errors path="name" cssClass="error"/>
        </p>
        <p>
            年龄：<form:input path="age"/>
            <form:errors path="age" cssClass="error"/>
        </p>
        <p>
            性别：<form:radiobutton path="gender" value="男"/>男 
            <form:radiobutton path="gender" value="女"/>女
            <form:errors path="gender" cssClass="error"/>
        </p>
        <p>
            生日：<form:input path="birthday" placeholder="如2024-10-01"/>
            <form:errors path="birthday" cssClass="error"/>
        </p>
        <p>
            邮箱：<form:input path="email" placeholder="如xxx@xxx.com"/>
            <form:errors path="email" cssClass="error"/>
        </p>
        <p><form:button type="submit">新增用户</form:button></p>
    </form:form>
    </body>
    </html>
    测试说明
    当前端输入非法数据时（如姓名为空、年龄为0、手机号格式错误），校验失败，页面会显示对应的错误提示（红色），用户可重新填写；只有所有参数都符合校验规则，才会执行新增业务逻辑，确保数据合法。
    四、三大功能协同工作逻辑（重点理解）
    SpringMVC的类型转换、数据格式化、数据校验，在请求处理过程中按以下顺序协同工作，确保参数合法可用：
    前端发送请求，传递字符串参数（如表单数据、URL传参）。
    类型转换：SpringMVC将前端字符串参数，通过内置或自定义转换器，转为后端所需类型（如String→Integer、String→Date）。
    数据格式化：对转换后的参数进行格式化（如Date转为指定格式、数字转为千分位格式），同时确保前端传递的格式符合要求（如日期格式为yyyy-MM-dd）。
    数据校验：对格式化后的参数进行合法性校验（如姓名不能为空、年龄在1-150之间），若校验失败，返回错误信息；若校验通过，将参数传递给业务逻辑层（Service）。
    五、新手避坑重点（高频错误，提前规避）
    类型转换失败：前端传递的参数格式无法转换为后端类型（如“abc”转为Integer），会报400错误（请求参数不合法），需确保前端传递的参数格式与后端类型匹配。
    格式化注解无效：未导入SpringMVC相关依赖，或未开启注解驱动（<mvc:annotation-driven/>），导致@DateTimeFormat、@NumberFormat注解失效。
    数据校验无效：未导入数据校验依赖（validation-api、hibernate-validator），或未在实体类参数前添加@Valid注解，导致校验注解不生效。
    BindingResult参数位置错误：BindingResult必须紧跟在@Valid注解的参数之后，否则无法接收错误信息，会报异常。
    自定义转换器未注册：自定义转换器未配置到SpringMVC容器中（未配置conversionService），导致转换器无法生效。
    日期格式化失败：前端传递的日期格式与@DateTimeFormat的pattern不一致（如前端传“2024/10/01”，pattern为“yyyy-MM-dd”），会报转换失败错误。
    六、实操总结（新手必练）
    1. 核心重点：类型转换解决“字符串→后端类型”的问题，数据格式化解决“数据格式规范”的问题，数据校验解决“参数合法性”的问题，三者协同，确保后端接收的参数安全、合法、可用。
    2. 实操步骤：
    类型转换：优先使用内置转换器，特殊场景自定义转换器并注册。
    数据格式化：在实体类属性上添加@DateTimeFormat、@NumberFormat注解，指定格式。
    数据校验：导入依赖→实体类添加校验注解→Controller方法添加@Valid和BindingResult→页面展示错误信息。
3. 衔接此前知识：三大功能均与Controller层、视图层紧密关联，结合之前的注解（@ModelAttribute、@RequestParam）和标签库（<form:form>、<c:out>），可实现“前端请求→参数处理→校验→业务逻辑→视图展示”的完整链路。新手可结合User案例，完善新增、修改用户功能，添加类型转换、格式化和校验，熟练掌握三大功能的用法。
