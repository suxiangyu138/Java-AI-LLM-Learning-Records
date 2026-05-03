03.31 13:18
Github实战Java后端企业级开发全流程（Demo项目）
本教程以「企业级用户管理Demo项目」为案例，全程基于Github完成“仓库初始化→团队协作→开发编码→版本控制→测试部署”全流程，既巩固Github核心操作（仓库创建、分支管理、Commit/Push/Pull Request、Merge等），也贴合Java后端企业级开发规范（分层架构、代码规范、README编写、.gitignore配置），适合有基础Java知识、想强化Github实战能力的开发者。
前置准备：已安装JDK（1.8+）、IDEA、Git客户端，已注册Github账号；了解基础Java后端知识（SpringBoot、MySQL）。
第一步：Github仓库初始化（企业级规范）
企业开发中，仓库是项目的基础，需提前规划仓库结构、配置忽略文件、明确分支规范，避免后续混乱。
1.1 创建Github仓库（核心操作）
登录Github账号，点击右上角「+」→「New repository」，进入仓库创建页面；
填写仓库核心信息（企业级规范）：
Repository name：java-enterprise-user-demo（规范：项目类型+功能+demo，全小写，用连字符分隔）；
Description：企业级Java后端用户管理Demo，基于SpringBoot+MySQL，用于Github实战练习（清晰描述项目用途，方便团队协作）；
Visibility：选择「Public」（练习用，企业级项目一般选Private，需邀请团队成员）；
勾选「Add a README file」（必选，企业项目需包含项目说明、启动步骤等）；
Add .gitignore：选择「Java」（自动生成Java项目常用的忽略文件，避免提交无用文件，如target、.idea等）；
Choose a license：选择「MIT License」（开源许可，企业项目可根据需求选择，非必须但规范）；
点击「Create repository」，完成仓库创建，此时仓库包含3个核心文件：README.md（项目说明）、.gitignore（忽略配置）、LICENSE（许可文件）。
1.2 仓库结构规划（企业级要求）
企业级项目需有清晰的目录结构，提前在README.md中规划，后续编码严格遵循，避免目录混乱。在README.md中添加「项目结构」模块，内容如下：
# 项目结构
java-enterprise-user-demo/
├── src/                          # 源代码目录
│   ├── main/
│   │   ├── java/com/example/user/ # 主程序包
│   │   │   ├── controller/       # 控制层（接收请求、返回响应）
│   │   │   ├── service/          # 服务层（业务逻辑处理）
│   │   │   ├── mapper/           # 持久层（操作数据库）
│   │   │   ├── entity/           # 实体类（对应数据库表）
│   │   │   ├── config/           # 配置类（SpringBoot配置）
│   │   │   └── UserApplication.java # 项目启动类
│   │   └── resources/            # 资源目录
│   │       ├── application.yml   # 全局配置文件
│   │       ├── application-dev.yml # 开发环境配置
│   │       └── application-prod.yml # 生产环境配置
│   └── test/                     # 测试代码目录
├── pom.xml                       # Maven依赖配置
├── README.md                     # 项目说明文档
└── .gitignore                    # Git忽略文件
1.3 克隆仓库到本地（核心操作）
仓库创建完成后，需要克隆到本地开发环境，后续编码、提交都在本地操作，再推送到Github远程仓库。
进入Github仓库页面，点击「Code」，复制HTTPS链接（如：https://github.com/你的用户名/java-enterprise-user-demo.git）；
打开本地Git Bash（或IDEA的Terminal），进入想要存放项目的目录（如：D:\workspace）；
执行克隆命令：git clone 复制的HTTPS链接，等待克隆完成；
克隆完成后，本地会出现「java-enterprise-user-demo」文件夹，即为本地仓库，与Github远程仓库保持同步。
第二步：分支管理（企业级协作核心）
企业开发中，不会直接在主分支（main）上开发，而是通过分支管理实现多人协作、版本控制，常用规范：main（主分支，存放上线代码）、develop（开发分支，存放开发中的代码）、feature/xxx（功能分支，单个功能开发）、hotfix/xxx（修复分支，线上bug修复）。
2.1 创建开发分支（develop）
进入本地仓库目录（cd java-enterprise-user-demo）；
查看当前分支：git branch，默认只有main分支（* main表示当前在main分支）；
创建并切换到develop分支：git checkout -b develop（等价于git branch develop + git checkout develop）；
将develop分支推送到Github远程仓库：git push -u origin develop（-u表示关联远程分支，后续push可直接用git push）；
刷新Github仓库页面，可看到分支列表中新增develop分支，此时远程仓库有两个分支：main、develop。
2.2 创建功能分支（feature/user）
我们开发「用户查询」功能，创建专门的功能分支，避免直接修改develop分支，符合企业级“单个功能一个分支”的规范。
确保当前在develop分支：git checkout develop（若已在则跳过）；
创建并切换到功能分支：git checkout -b feature/user（feature/xxx格式，xxx为功能名称）；
查看分支：git branch，可看到当前在feature/user分支，分支层级：main → develop → feature/user。
第三步：本地开发（Java后端编码+Git提交）
进入本地仓库，用IDEA打开项目，进行Java后端开发（基于SpringBoot），同时同步使用Git提交代码，每完成一个小功能提交一次，提交信息要规范（企业级要求）。
3.1 初始化Java项目（SpringBoot）
打开IDEA，选择「Open」，找到本地仓库目录（java-enterprise-user-demo），点击「OK」打开项目；
初始化SpringBoot项目（IDEA内置Spring Initializr）：
点击「File」→「New」→「Module」，选择「Spring Initializr」，点击「Next」；
Group：com.example（企业级规范，一般为公司域名反写）；
Artifact：user-demo（项目名称，与仓库名称对应）；
Dependencies：勾选「Spring Web」（web开发）、「Spring Data JPA」（持久层）、「MySQL Driver」（MySQL驱动），点击「Finish」；
删除IDEA自动生成的多余文件（如.mvn、HELP.md），确保项目结构与之前规划的一致。
3.2 编写核心代码（简单用户查询功能）
按照分层架构编写代码，完成“查询所有用户”的简单功能，贴合企业级开发规范（命名规范、注释规范）。
3.2.1 实体类（entity/User.java）
package com.example.user.entity;
import javax.persistence.*;
import lombok.Data;
/**
 * 用户实体类，对应数据库user表
 * 企业级规范：实体类名与表名对应，属性与字段对应，添加必要注释
 */
@Data // Lombok注解，简化getter/setter
@Entity
@Table(name = "user")
public class User {
    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 用户名
     */
    @Column(name = "username", nullable = false, length = 50)
    private String username;
    /**
     * 密码（实际开发中需加密存储）
     */
    @Column(name = "password", nullable = false, length = 100)
    private String password;
    /**
     * 手机号
     */
    @Column(name = "phone", length = 11)
    private String phone;
}
3.2.2 持久层（mapper/UserMapper.java）
package com.example.user.mapper;
import com.example.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
/**
 * 用户持久层，操作数据库
 * 继承JpaRepository，无需编写基础CRUD方法
 */
@Repository
public interface UserMapper extends JpaRepository<User, Long> {
    // 后续可添加自定义查询方法，如根据用户名查询
}
3.2.3 服务层（service/UserService.java）
package com.example.user.service;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
/**
 * 用户服务层，处理业务逻辑
 */
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    /**
     * 查询所有用户
     * @return 所有用户列表
     */
    public List<User> findAll() {
        return userMapper.findAll();
    }
}
3.2.4 控制层（controller/UserController.java）
package com.example.user.controller;
import com.example.user.entity.User;
import com.example.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
/**
 * 用户控制层，接收前端请求，返回响应
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;
    /**
     * 查询所有用户接口
     * @return 所有用户列表（JSON格式）
     */
    @GetMapping("/list")
    public List<User> findAll() {
        return userService.findAll();
    }
}
3.2.5 配置文件（application-dev.yml）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/user_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC
    username: root # 你的MySQL用户名
    password: 123456 # 你的MySQL密码
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update # 自动创建/更新数据库表
    show-sql: true # 打印SQL语句
server:
  port: 8080 # 项目端口
3.3 Git提交代码（企业级规范）
代码编写完成后，提交到本地仓库，提交信息要清晰、规范，避免“修改代码”“提交”等模糊描述，企业级规范：类型: 描述（如：feat: 新增用户查询接口）。
查看本地修改的文件：git status，可看到新增的java文件、配置文件等（红色表示未暂存）；
暂存所有修改：git add .（.表示所有修改，也可指定单个文件，如git add src/main/java/com/example/user/entity/User.java）；
查看暂存状态：git status，此时文件变为绿色（已暂存）；
提交到本地仓库：git commit -m "feat: 完成用户查询核心代码（实体类、mapper、service、controller）"；
查看提交记录：git log，可看到本次提交的信息（作者、时间、提交描述），确认提交成功。
第四步：推送分支到Github（远程同步）
本地提交完成后，将feature/user功能分支推送到Github远程仓库，让团队成员可见，也为后续合并到develop分支做准备。
确保当前在feature/user分支：git checkout feature/user；
推送分支到远程：git push -u origin feature/user（首次推送需加-u，关联远程分支）；
刷新Github仓库页面，进入「Branches」，可看到feature/user分支已推送成功，点击分支可查看提交的代码。
第五步：Pull Request（PR）与合并分支（企业级协作）
企业开发中，功能开发完成后，不会直接合并到develop分支，而是通过Pull Request（PR）申请合并，由团队负责人审核代码，审核通过后再合并，避免代码问题。
5.1 创建Pull Request（PR）
进入Github仓库页面，点击「Pull requests」→「New pull request」；
选择合并的分支：
base repository：你的仓库（java-enterprise-user-demo）；
base：develop（目标分支，合并到develop）；
compare：feature/user（源分支，要合并的功能分支）；
点击「Create pull request」，填写PR信息（企业级规范）：
Title：feat: 新增用户查询功能（与提交信息一致，清晰说明PR用途）；
Description：填写功能说明、修改内容，如：「完成用户查询核心功能，包含实体类、持久层、服务层、控制层，可通过/api/user/list接口查询所有用户，依赖MySQL数据库」；
点击「Create pull request」，完成PR创建，此时团队负责人可看到PR，进行代码审核。
5.2 审核与合并分支
（模拟团队审核，自己作为负责人审核自己的代码）
进入创建的PR页面，点击「Files changed」，查看提交的代码（检查语法、规范、功能合理性）；
若代码无问题，点击「Merge pull request」→「Confirm merge」，完成分支合并；
合并完成后，点击「Delete branch」，删除feature/user分支（功能已合并，无需保留功能分支，避免分支冗余）；
此时，develop分支已包含用户查询功能的代码，远程仓库的develop分支与本地develop分支不一致，需要拉取远程代码到本地。
第六步：拉取远程代码（同步本地分支）
分支合并后，远程develop分支已更新，本地develop分支需要同步远程代码，避免后续开发出现冲突。
切换到本地develop分支：git checkout develop；
拉取远程develop分支的代码：git pull origin develop；
查看本地代码，可看到feature/user分支的代码已同步到本地develop分支，此时本地与远程develop分支保持一致。
第七步：测试与部署（企业级收尾）
代码合并到develop分支后，进行本地测试，测试通过后，合并到main分支（上线分支），完成项目上线准备。
7.1 本地测试
启动MySQL，创建数据库user_db（与配置文件中的数据库名称一致）；
在IDEA中启动SpringBoot项目（运行UserApplication.java）；
打开浏览器，访问http://localhost:8080/api/user/list，可看到返回的JSON数据（空列表，因为数据库中无数据），测试接口正常；
测试通过后，提交测试记录到本地develop分支：git commit -m "test: 测试用户查询接口，接口正常"，并推送至远程：git push origin develop。
7.2 合并到main分支（上线分支）
企业级项目中，develop分支测试通过后，需要合并到main分支，作为上线代码，流程与合并到develop分支一致：
在Github上创建PR：base为main，compare为develop；
审核代码、测试记录，确认无问题后，合并PR，删除无用分支；
拉取远程main分支到本地：git checkout main → git pull origin main，此时本地main分支为最新上线代码。
第八步：Github进阶操作（企业级补充）
结合本次项目，补充企业开发中常用的Github进阶操作，进一步巩固知识。
8.1 版本标签（Tag）
企业级项目上线后，会给main分支打标签（Tag），标记版本号（如v1.0.0），方便后续回滚、迭代。
切换到main分支：git checkout main；
创建标签：git tag -a v1.0.0 -m "v1.0.0 版本：完成用户查询功能，上线"（-a表示带注释，-m为注释内容）；
查看标签：git tag，可看到v1.0.0标签；
推送标签到远程：git push origin v1.0.0；
刷新Github仓库，点击「Releases」→「Draft a new release」，选择v1.0.0标签，填写版本说明，发布版本（企业级规范，方便用户、团队查看版本迭代记录）。
8.2 冲突解决（企业级必备）
多人协作时，若两人修改同一文件的同一行，推送代码时会出现冲突，需手动解决冲突。
模拟冲突场景（自己操作两个分支，模拟两人协作）：
基于develop分支创建新功能分支：git checkout -b feature/user-update；
修改UserController.java，添加一个接口（如修改用户接口），提交并推送：git commit -m "feat: 新增用户修改接口" → git push origin feature/user-update；
切换回develop分支，手动修改UserController.java的同一行代码（如修改查询接口的返回提示），提交并推送：git commit -m "fix: 修改用户查询接口返回格式" → git push origin develop；
创建PR（feature/user-update → develop），此时Github会提示「This branch has conflicts that must be resolved」（有冲突）；
解决冲突：点击PR页面的「Resolve conflicts」，编辑冲突文件，保留需要的代码，删除冲突标记（<<<<<<<、=======、>>>>>>>），点击「Mark as resolved」，再提交PR，合并即可。
8.3 .gitignore补充配置
企业级项目中，除了默认的Java忽略文件，还需添加一些自定义忽略内容（如日志文件、配置文件副本等），修改.gitignore文件，添加以下内容：
# 日志文件
logs/
*.log
# 配置文件副本
application-*.yml.bak
# IDEA配置文件（若未自动忽略）
.idea/
*.iml
*.iws
*.ipr
# 构建产物
target/
build/
# 临时文件
tmp/
temp/
修改后，提交到远程仓库：git add .gitignore → git commit -m "docs: 补充.gitignore配置，忽略日志、临时文件等" → git push origin develop。
第九步：项目总结与Github知识梳理
9.1 项目总结
本次通过「用户管理Demo项目」，完整体验了企业级Java后端开发的Github流程，从仓库初始化到版本发布，每一步都贴合企业规范，完成了：
Github核心操作：仓库创建、克隆、分支管理、Commit/Push/PR、合并分支、标签、冲突解决；
Java后端开发：SpringBoot分层架构、代码规范、配置文件、接口开发、本地测试；
企业级规范：仓库命名、分支规范、提交信息规范、文档编写、忽略文件配置。
9.2 Github核心知识梳理（重点巩固）
仓库相关：创建仓库（含README、.gitignore）、克隆仓库（git clone）、推送仓库（git push）；
分支相关：创建分支（git checkout -b）、切换分支（git checkout）、推送分支（git push -u）、删除分支（git branch -d 本地分支；git push origin --delete 远程分支）；
版本控制：暂存（git add）、提交（git commit -m）、拉取（git pull）、查看提交记录（git log）；
协作相关：Pull Request（PR）、代码审核、冲突解决；
进阶相关：标签（git tag）、版本发布、.gitignore配置。
后续可基于本项目扩展功能（如用户新增、删除、修改），继续练习Github操作，也可邀请同学、朋友模拟团队协作，进一步巩固企业级开发流程。

