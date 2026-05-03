04.28 11:01
npm 核心知识点
下面从定义、核心作用、核心命令、依赖管理、包管理机制、镜像源、工程化、高频问题八大维度，一次性吃透 npm，适配前端开发、Node.js 后端、工程化部署、面试高频考点。
 
一、基础定义
npm（Node Package Manager） 是 Node.js 官方内置的包管理工具，也是全球最大的开源软件注册表。
- 核心定位：代码共享、依赖管理、工程化脚本执行
- 组成：
1. 注册表：全球开源包仓库（https://registry.npmjs.org）
2. 命令行工具：安装 Node.js 自带，无需单独安装
3.  package.json ：项目配置清单
 
二、核心作用
1. 依赖管理：自动下载、安装、升级、卸载第三方包
2. 版本控制：精准锁定依赖版本，解决版本冲突
3. 脚本执行：配置项目启动、打包、测试、部署命令
4. 工程化规范：统一项目结构、入口文件、依赖声明
5. 包发布：将自己的代码发布为开源包供他人使用
 
三、核心文件解析
1.  package.json （项目核心配置文件）
项目的身份证+说明书，手动创建或  npm init  生成，关键字段：
json
{
  "name": "项目名",
  "version": "1.0.0",
  "main": "入口文件.js",
  "scripts": { "start": "node index.js" },
  "dependencies": {}, // 生产依赖
  "devDependencies": {} // 开发依赖
}
 
-  dependencies ：项目运行必须的依赖（如 Vue、React、Axios）
-  devDependencies ：仅开发/打包需要的依赖（如 Webpack、ESLint、Vite）
2.  package-lock.json 
版本锁文件，作用：
- 精准锁定所有依赖的完整版本树
- 保证不同环境安装的依赖版本完全一致，避免版本漂移
- 不要手动修改，由 npm 自动维护
3.  node_modules 
存放所有下载的第三方依赖包，项目体积最大的文件夹。
 
四、高频核心命令
1. 初始化项目
bash
npm init       # 一步步配置生成 package.json
npm init -y    # 快速生成默认配置
 
2. 安装依赖
bash
npm install 包名                # 安装生产依赖
npm install 包名 -D             # 安装开发依赖
npm install 包名@版本号          # 安装指定版本
npm install                     # 根据 package.json 安装所有依赖
 
3. 卸载依赖
bash
npm uninstall 包名
npm uninstall 包名 -D
 
4. 执行脚本
bash
npm run 脚本名
npm start       # 特殊简写，无需 run
 
5. 版本查看
bash
npm -v          # 查看 npm 版本
node -v         # 查看 Node 版本
 
 
五、依赖版本规则（SemVer 语义化版本）
格式： 主版本.次版本.补丁版本 （如  2.1.3 ）
- 主版本：不兼容的破坏性更新
- 次版本：新增功能，向下兼容
- 补丁版本：bug 修复，向下兼容
版本符号规则
-  ^2.1.3 ：锁定主版本，自动更新次版本、补丁
-  ~2.1.3 ：锁定主、次版本，仅更新补丁
-  2.1.3 ：完全锁定版本，不自动更新
 
六、镜像源配置（国内开发必学）
默认源在国外，速度极慢，切换淘宝镜像：
bash
# 查看当前源
npm config get registry
# 切换淘宝镜像
npm config set registry https://registry.npmmirror.com
# 切回官方源
npm config set registry https://registry.npmjs.org
 
 
七、依赖安装机制
1. 扁平化安装：npm3+ 自动扁平化依赖，减少嵌套层级
2. 幽灵依赖：代码中引用了  package.json  未声明的依赖，极易报错
3. 依赖提升：公共依赖提到顶层  node_modules ，减少体积
 
八、npm 5 大高频面试考点
1.  dependencies  和  devDependencies  区别
- 生产依赖：项目上线运行必需
- 开发依赖：仅开发、打包阶段使用，上线会被剔除
2.  package.json  和  package-lock.json  区别
- package.json：声明依赖版本范围
- package-lock.json：锁定依赖精确版本，保证环境一致性
3. npm 与 yarn / pnpm 的区别
- npm：官方原生，生态最全
- yarn：早期速度快、稳定性强
- pnpm：硬链接+软链接，节省磁盘空间，安装速度最快
4. 幽灵依赖是什么？怎么解决？
代码引用未声明的依赖，解决：规范声明依赖、使用 pnpm 严格校验
5. 为什么安装依赖会有 peerDependencies？
对等依赖，要求宿主项目必须安装指定版本的依赖，版本不匹配会警告。
 
九、一句话总结
npm 是 Node.js 生态的基石，管依赖、管版本、管脚本、管工程。
掌握核心命令 + 版本规则 + 镜像源配置，就能搞定 90% 的前端/Node.js 工程化问题。

