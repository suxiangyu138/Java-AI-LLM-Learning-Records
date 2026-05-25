SpringMVC常用注解（新手必备，衔接实操）
结合此前SpringMVC简介中讲解的核心组件（Controller、请求映射、参数绑定等），SpringMVC的核心优势之一就是通过注解简化Web层开发，无需手动配置Servlet、映射请求，仅需通过简单注解，即可完成请求接收、参数绑定、结果返回等核心操作。
补充衔接：此前我们了解到，SpringMVC的Controller是请求处理的核心，而这些注解主要作用于Controller类或其方法上，同时遵循Spring IOC思想——Controller类需通过注解标识为Bean，才能被SpringMVC容器管理，与此前Bean装配的@Service、@Repository注解逻辑一致。本教程仍基于Spring 5.x（适配JDK 8+），延续User案例，每个注解均搭配实操代码，让新手快速上手，同时标注重点和避坑点。
一、核心基础注解（必掌握，奠定开发基础）
这类注解是SpringMVC开发的基石，主要用于标识Controller、映射请求，是所有Web请求处理的前提。
1. @Controller：标识控制器Bean（核心）
    作用
    用于标识一个Java类为SpringMVC的控制器（Controller），告知SpringMVC容器：该类负责处理前端请求，同时将该类交给Spring IOC容器管理（等同于@Service、@Repository，本质是@Component的衍生注解）。
    关键：被@Controller标识的类，其内部的方法可通过@RequestMapping等注解映射前端请求，若无该注解，SpringMVC无法识别该类为控制器。
    语法与实操案例
    // com.example.controller.UserController.java
    import org.springframework.stereotype.Controller;
    // @Controller：标识该类为SpringMVC控制器，交给Spring管理
    @Controller
    public class UserController {
    // 后续添加请求映射方法（如查询用户、新增用户）
    }
    避坑点
    不要遗漏@Controller注解：否则该类无法被SpringMVC识别，前端请求无法映射到其内部方法，会报404错误。
    无需额外添加@Component注解：@Controller已包含@Component的功能，重复添加无意义。
2. @RequestMapping：请求映射（核心）
    作用
    用于将前端请求的URL，映射到Controller类或其方法上，是SpringMVC实现“请求→方法”映射的核心注解。可作用于类上（全局路径），也可作用于方法上（具体路径），组合使用可实现更精准的请求映射。
    核心属性（常用3个）
    value：指定请求的URL路径（必填），如value="/user/get"，可简化为@RequestMapping("/user/get")。
    method：指定请求的HTTP方法（可选），如GET、POST、PUT、DELETE，默认支持所有HTTP方法，指定后仅匹配对应方法的请求（如method = RequestMethod.GET，仅接收GET请求）。
    params：指定请求必须包含的参数（可选），如params="id"，表示请求必须携带id参数，否则无法匹配该方法。
    语法与实操案例（结合User案例）
    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestMethod;
    @Controller
    // 类上添加@RequestMapping：全局路径（所有方法的URL都需拼接该路径）
    @RequestMapping("/user")
    public class UserController {
    // 方法上添加@RequestMapping：具体路径，组合后完整URL：/user/get
    // method = RequestMethod.GET：仅接收GET请求
    @RequestMapping(value = "/get", method = RequestMethod.GET)
    public String getUser() {
        // 模拟查询用户业务逻辑（后续衔接Service层）
        System.out.println("查询用户信息");
        // 返回视图名称（后续视图解析器会解析为具体页面）
        return "userDetail";
    }
    // 简化写法：value可省略，仅指定URL
    @RequestMapping("/list")
    public String getUserList() {
        System.out.println("查询所有用户列表");
        return "userList";
    }
    // 带参数的请求映射：必须携带name参数才能匹配
    @RequestMapping(value = "/search", params = "name")
    public String searchUser(String name) {
        System.out.println("根据姓名查询用户：" + name);
        return "userSearch";
    }
    }
    避坑点
    URL路径规范：建议类上用全局路径（如/user），方法上用具体路径（如/get），避免路径重复或混乱。
    method属性匹配：若前端发送的HTTP方法（如POST）与注解指定的method（如GET）不匹配，会报405错误（请求方法不允许）。
    二、请求参数绑定注解（必掌握，接收前端参数）
    前端请求（如表单提交、URL传参）会携带参数，这类注解用于将前端传递的参数，自动绑定到Controller方法的参数上，无需手动解析请求参数，简化开发，核心适配GET、POST等请求方式。
    1. @RequestParam：绑定单个请求参数
    作用
    用于将前端传递的单个请求参数（URL传参、表单提交的单个参数），绑定到Controller方法的参数上，支持参数名不一致、必填校验、默认值设置。
    核心属性（常用3个）
    name/value：指定前端传递的参数名（必填，若方法参数名与前端参数名一致，可省略）。
    required：指定该参数是否必填（可选，默认true），若为true且前端未传递该参数，会报400错误。
    defaultValue：指定参数的默认值（可选），若前端未传递该参数，会使用默认值（此时required自动变为false）。
    实操案例
    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestParam;
    @Controller
    @RequestMapping("/user")
    public class UserController {
    // 1. 方法参数名与前端参数名一致（前端传递id参数），可省略@RequestParam
    @RequestMapping("/get")
    public String getUser(Integer id) {
        System.out.println("根据ID查询用户：" + id);
        return "userDetail";
    }
    // 2. 方法参数名与前端参数名不一致（前端传userName，方法参数名是name）
    @RequestMapping("/search")
    public String searchUser(@RequestParam(name = "userName") String name) {
        System.out.println("根据姓名查询用户：" + name);
        return "userSearch";
    }
    // 3. 非必填参数，设置默认值
    @RequestMapping("/list")
    public String getUserList(@RequestParam(defaultValue = "1") Integer pageNum) {
        System.out.println("查询第" + pageNum + "页用户列表");
        return "userList";
    }
    // 4. 必填参数（默认required=true）
    @RequestMapping("/delete")
    public String deleteUser(@RequestParam Integer id) {
        System.out.println("删除ID为" + id + "的用户");
        return "redirect:/user/list"; // 重定向到用户列表页
    }
    }
2. @PathVariable：绑定URL路径参数（RESTful风格）
    作用
    用于绑定URL路径中的参数（RESTful风格常用），如URL为“/user/get/1”，其中“1”是路径参数（id），通过@PathVariable可将其绑定到方法参数上，使URL更简洁。
    补充：RESTful风格是目前前后端分离开发的主流，核心是用URL表示资源，用HTTP方法表示操作（如GET查询、DELETE删除），@PathVariable是实现RESTful接口的核心注解。
    实操案例（RESTful风格）
    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.PathVariable;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestMethod;
    @Controller
    @RequestMapping("/user")
    public class UserController {
    // RESTful风格：URL路径中的{id}是路径参数，与方法参数id绑定
    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    public String getUser(@PathVariable Integer id) {
        System.out.println("根据ID查询用户：" + id);
        return "userDetail";
    }
    // 多个路径参数（如根据id和name查询）
    @RequestMapping(value = "/get/{id}/{name}", method = RequestMethod.GET)
    public String getUserByIdAndName(@PathVariable Integer id, @PathVariable String name) {
        System.out.println("查询ID为" + id + "、姓名为" + name + "的用户");
        return "userDetail";
    }
    }
    说明：前端请求URL为“/user/get/1/张三”，即可将id=1、name=张三绑定到方法参数上，无需通过“?id=1&name=张三”传参，URL更简洁规范。
3. @ModelAttribute：绑定实体类参数（表单提交常用）
    作用
    用于将前端表单提交的多个参数，自动封装为对应的实体类对象（如User），无需手动为实体类的每个属性赋值，简化参数绑定（适合表单提交、多参数传递场景）。
    实操案例（表单提交User信息）
    // 1. 实体类User（此前已定义，包含name、age、gender属性及getter/setter）
    // 2. Controller方法
    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.ModelAttribute;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestMethod;
    @Controller
    @RequestMapping("/user")
    public class UserController {
    // 表单提交（POST请求），将表单参数封装为User对象
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public String addUser(@ModelAttribute User user) {
        // 模拟新增用户业务（调用Service层）
        System.out.println("新增用户：" + user);
        return "redirect:/user/list"; // 重定向到用户列表页
    }
    }
    说明：前端表单的input标签name属性，需与User实体类的属性名一致（如<input name="name" />、<input name="age" />），SpringMVC会自动将表单参数封装为User对象，传递到方法中。
    三、响应结果返回注解（必掌握，返回数据/视图）
    Controller方法执行完成后，需返回处理结果（视图页面或数据），这类注解用于指定返回结果的类型，适配传统Web应用（视图渲染）和前后端分离应用（JSON数据）。
    1. @ResponseBody：返回JSON数据（前后端分离常用）
    作用
    用于告知SpringMVC：该方法的返回值不是视图名称，而是需要转换为JSON数据，直接返回给前端（无需视图解析器解析），是前后端分离开发的核心注解。
    补充：SpringMVC会自动将返回的对象（如User、List<User>）转换为JSON格式，无需手动处理JSON序列化。
    实操案例（返回JSON数据）
    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.ResponseBody;
    @Controller
    @RequestMapping("/user")
    public class UserController {
    // 方法返回User对象，@ResponseBody将其转换为JSON返回
    @RequestMapping("/getJson")
    @ResponseBody
    public User getUserJson() {
        // 模拟查询用户，返回User对象
        User user = new User("张三", 20, "男");
        user.setId(1);
        return user; // 最终返回JSON：{"id":1,"name":"张三","age":20,"gender":"男"}
    }
    // 返回List集合，转换为JSON数组
    @RequestMapping("/listJson")
    @ResponseBody
    public List<User> getUserListJson() {
        List&lt;User&gt; userList = new ArrayList<>();
        userList.add(new User("张三", 20, "男"));
        userList.add(new User("李四", 22, "女"));
        return userList; // 返回JSON数组：[{"id":null,"name":"张三",...},...]
    }
    }
2. @RestController：@Controller + @ResponseBody（简化注解）
    作用
    组合注解，等同于@Controller + @ResponseBody，用于标识一个“仅返回JSON数据”的控制器（前后端分离场景常用），无需在每个方法上单独添加@ResponseBody，简化代码。
    实操案例（简化前后端分离接口）
    // @RestController = @Controller + @ResponseBody
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;
    @RestController
    @RequestMapping("/user")
    public class UserController {
    // 无需添加@ResponseBody，直接返回JSON
    @RequestMapping("/getJson")
    public User getUserJson() {
        User user = new User("张三", 20, "男");
        user.setId(1);
        return user;
    }
    @RequestMapping("/listJson")
    public List<User> getUserListJson() {
        List<User> userList = new ArrayList<>();
        userList.add(new User("张三", 20, "男"));
        userList.add(new User("李四", 22, "女"));
        return userList;
    }
    }
    避坑点
    @RestController标识的控制器，所有方法的返回值都会被转换为JSON，无法返回视图（若需返回视图，需使用@Controller，而非@RestController）。
3. @RequestMapping的衍生注解（简化请求方法）
    @RequestMapping可通过method属性指定HTTP方法，SpringMVC提供了4个衍生注解，简化不同HTTP方法的请求映射，与@RequestMapping功能一致，更简洁。
    @GetMapping：等同于@RequestMapping(method = RequestMethod.GET)，仅接收GET请求（查询场景常用）。
    @PostMapping：等同于@RequestMapping(method = RequestMethod.POST)，仅接收POST请求（新增场景常用）。
    @PutMapping：等同于@RequestMapping(method = RequestMethod.PUT)，仅接收PUT请求（修改场景常用）。
    @DeleteMapping：等同于@RequestMapping(method = RequestMethod.DELETE)，仅接收DELETE请求（删除场景常用）。
    实操案例（简化写法）
    import org.springframework.web.bind.annotation.*;
    @RestController
    @RequestMapping("/user")
    public class UserController {
    // 替代@RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    @GetMapping("/get/{id}")
    public User getUser(@PathVariable Integer id) {
        User user = new User("张三", 20, "男");
        user.setId(id);
        return user;
    }
    // 替代@RequestMapping(value = "/add", method = RequestMethod.POST)
    @PostMapping("/add")
    public String addUser(@ModelAttribute User user) {
        System.out.println("新增用户：" + user);
        return "success";
    }
    // 替代@RequestMapping(value = "/update", method = RequestMethod.PUT)
    @PutMapping("/update")
    public String updateUser(@ModelAttribute User user) {
        System.out.println("修改用户：" + user);
        return "success";
    }
    // 替代@RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
    @DeleteMapping("/delete/{id}")
    public String deleteUser(@PathVariable Integer id) {
        System.out.println("删除用户：" + id);
        return "success";
    }
    }
    四、其他常用辅助注解（了解，提升开发效率）
    这类注解虽非必掌握，但在实际开发中常用，可简化异常处理、请求头获取等操作，贴合SpringMVC的解耦思想。
    1. @ExceptionHandler：全局异常处理
    作用：用于处理Controller方法执行过程中出现的异常，可作用于Controller类（局部异常处理）或专门的异常处理类（全局异常处理），避免异常直接暴露给前端，提升用户体验。
    import org.springframework.web.bind.annotation.ExceptionHandler;
    import org.springframework.web.bind.annotation.RestControllerAdvice;
    // @RestControllerAdvice：全局异常处理类（结合@ExceptionHandler）
    @RestControllerAdvice
    public class GlobalExceptionHandler {
    // 处理所有Exception类型的异常
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e) {
        // 记录异常日志（实际开发中常用）
        e.printStackTrace();
        // 返回友好提示，而非异常堆栈信息
        return "请求异常：" + e.getMessage();
    }
    // 处理特定异常（如空指针异常）
    @ExceptionHandler(NullPointerException.class)
    public String handleNullPointerException(NullPointerException e) {
        return "空指针异常：请检查参数是否正确";
    }
    }
2. @RequestHeader：获取请求头信息
    作用：用于获取前端请求头中的信息（如Token、User-Agent等），绑定到方法参数上，常用于权限校验、请求来源判断等场景。
    @GetMapping("/getToken")
    @ResponseBody
    public String getToken(@RequestHeader("Token") String token) {
    System.out.println("前端传递的Token：" + token);
    // 模拟权限校验：判断Token是否有效
    if ("123456".equals(token)) {
        return "Token有效";
    }
    return "Token无效";
    }
    五、新手避坑重点（高频错误，提前规避）
    注解导包错误：所有SpringMVC注解都在org.springframework.web.bind.annotation包下，不要导入错误包（如导入Spring核心包的注解），否则注解无效。
    @ResponseBody使用场景：仅在需要返回JSON数据时使用，若用于传统Web应用（返回视图），会导致视图解析失败，报404错误。
    参数绑定失败：@RequestParam、@ModelAttribute绑定参数时，前端参数名需与方法参数名/实体类属性名一致，否则无法绑定（参数为null）。
    RESTful风格路径参数错误：@PathVariable绑定的参数名，需与URL路径中的{参数名}一致，否则无法绑定。
    衍生注解使用错误：@GetMapping、@PostMapping等衍生注解，仅对应一种HTTP方法，若前端发送的方法不匹配，会报405错误。
    Controller未被扫描：若Controller类未被Spring注解扫描（未配置@ComponentScan），@Controller注解无效，前端请求无法映射，报404错误。
    六、实操总结（新手必练）
    1. 核心注解优先级：@Controller/@RestController（标识Bean）→ @RequestMapping及其衍生注解（请求映射）→ 参数绑定注解（接收参数）→ @ResponseBody（返回数据）。
    2. 场景适配：传统Web应用（返回视图）用@Controller + @RequestMapping；前后端分离（返回JSON）用@RestController + 衍生注解（@GetMapping等）。
    3. 衔接此前知识：Controller层可通过@Autowired注入Service层Bean，与此前Bean装配的逻辑完全一致，后续实操会结合Service、Dao层，实现完整的“请求→业务→数据库→响应”链路。
    新手练习：结合User案例，编写Controller方法，使用上述常用注解，实现“新增用户（POST）、查询用户（GET）、修改用户（PUT）、删除用户（DELETE）”的完整接口，测试参数绑定和JSON返回效果，熟练掌握注解用法。
