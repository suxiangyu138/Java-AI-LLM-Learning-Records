# 文件上传下载系统设计文档

## 1. 项目定位

该项目适合作为 Java 后端求职简历中的中高质量实战项目，核心目标是完成一个支持**文件上传、下载、秒传、断点续传、类型校验、存储切换**的通用文件服务模块，可扩展到企业网盘、头像中心、资料管理、合同归档、AI 知识库素材管理等场景。

推荐你把它包装成“通用文件中心服务”，而不是只写成“上传下载 demo”。这样在简历表达上更像真实业务系统，和你主攻的 Java 后端 + AI 应用开发路线也更匹配。

---

## 2. 核心功能

### 2.1 必做功能

- 单文件上传
- 多文件上传
- 文件下载
- 文件删除
- 文件预览图片/文本类文件
- 文件类型校验
- 文件大小限制
- 本地存储与阿里云 OSS 存储切换
- 文件元数据入库

### 2.2 进阶功能

- 分片上传
- 断点续传
- 秒传
- 文件 MD5 去重
- 下载鉴权
- 防止文件重名覆盖
- 上传进度展示
- 临时访问 URL
- 批量下载
- 回收站机制

### 2.3 简历加分功能

- 基于 Redis 记录上传会话状态
- 基于 MinIO/OSS 抽象统一对象存储接口
- 大文件分片合并
- 分布式环境下分片状态一致性控制
- 上传任务失败重试
- 恶意文件拦截
- 操作日志与审计日志

---

## 3. 技术栈建议

### 3.1 后端

- Java 17
- Spring Boot 3
- Spring MVC
- MyBatis Plus
- MySQL
- Redis
- Sa-Token 或 Spring Security
- Hutool
- Maven

### 3.2 存储层

- 本地存储：适合开发和本地联调
- 阿里云 OSS：适合项目包装和生产场景表达

### 3.3 前端

- Vue3 + Element Plus
- 或者直接用 Thymeleaf 做轻量演示页

如果你的目标是简历项目效率优先，建议：

- 后端主项目重点做扎实
- 前端只做上传页、文件列表页、进度条页
- 把亮点放在后端设计、分片上传、断点续传、OSS 抽象上

---

## 4. 系统模块划分

### 4.1 模块结构

```text
file-service
├─ auth               权限模块
├─ file-core          文件核心模块
├─ file-storage       存储策略模块
├─ file-task          分片上传任务模块
├─ file-search        文件查询模块
├─ common             公共模块
```

### 4.2 file-core 核心职责

- 接收上传请求
- 校验文件类型、大小、业务归属
- 生成文件唯一标识
- 调用存储策略保存文件
- 落库文件元数据
- 返回访问路径或下载标识

### 4.3 file-storage 存储策略

建议使用策略模式统一抽象：

```java
public interface FileStorageService {
    String upload(byte[] bytes, String path, String contentType);
    void delete(String path);
    byte[] download(String path);
    String getPreviewUrl(String path, long expireSeconds);
}
```

实现类：

- `LocalFileStorageService`
- `OssFileStorageService`

这样你后续切 MinIO、腾讯云 COS 也非常方便。

---

## 5. 数据库设计

### 5.1 文件主表 file_info

```sql
CREATE TABLE file_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    file_ext VARCHAR(50),
    content_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    file_md5 VARCHAR(64),
    storage_type VARCHAR(20) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    access_url VARCHAR(1000),
    biz_type VARCHAR(50),
    biz_id BIGINT,
    uploader_id BIGINT,
    upload_status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 5.2 分片任务表 upload_task

```sql
CREATE TABLE upload_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_md5 VARCHAR(64) NOT NULL,
    chunk_total INT NOT NULL,
    chunk_size BIGINT NOT NULL,
    uploaded_chunks VARCHAR(2000),
    status TINYINT DEFAULT 0,
    uploader_id BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_id (task_id)
);
```

### 5.3 可选分片明细表 upload_task_chunk

如果你想做得更规范，建议增加分片明细表，而不是把 `uploaded_chunks` 存成字符串。

```sql
CREATE TABLE upload_task_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    chunk_index INT NOT NULL,
    chunk_md5 VARCHAR(64),
    chunk_size BIGINT,
    upload_status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_chunk (task_id, chunk_index)
);
```

对于面试来说，这张表很加分，因为能体现你知道“用结构化数据管理上传状态”，而不是图省事乱存。

---

## 6. 上传流程设计

### 6.1 普通上传流程

```text
前端选择文件
→ 后端校验大小/类型
→ 生成唯一文件名
→ 上传到本地或 OSS
→ 保存 file_info
→ 返回文件 URL/文件ID
```

### 6.2 秒传流程

```text
前端计算文件 MD5
→ 调用秒传校验接口
→ 服务端按 MD5 查询 file_info
→ 若已存在相同文件，则直接复用文件记录或建立业务关联
→ 返回上传成功
```

### 6.3 分片上传 + 断点续传流程

```text
1. 前端切片
2. 前端计算文件 MD5
3. 调用初始化接口，创建 upload_task
4. 查询已上传分片列表
5. 前端只上传缺失分片
6. 服务端保存每个分片
7. 全部分片上传完成后触发合并
8. 合并成功后上传最终文件到本地/OSS
9. 保存 file_info
10. 清理临时分片
```

---

## 7. 接口设计

## 7.1 普通上传

```http
POST /api/files/upload
Content-Type: multipart/form-data
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fileId": 1,
    "fileName": "resume.pdf",
    "url": "https://xxx/resume.pdf"
  }
}
```

## 7.2 下载文件

```http
GET /api/files/download/{fileId}
```

处理要点：

- 设置 `Content-Disposition`
- 设置 `Content-Type`
- 流式输出，避免大文件一次性加载内存

## 7.3 初始化分片任务

```http
POST /api/files/chunk/init
```

请求体：

```json
{
  "fileName": "movie.mp4",
  "fileMd5": "xxxx",
  "chunkTotal": 20,
  "chunkSize": 5242880
}
```

## 7.4 上传单个分片

```http
POST /api/files/chunk/upload
Content-Type: multipart/form-data
```

参数：

- taskId
- chunkIndex
- file

## 7.5 查询已上传分片

```http
GET /api/files/chunk/uploaded?taskId=xxx
```

## 7.6 合并分片

```http
POST /api/files/chunk/merge
```

---

## 8. 断点续传实现要点

### 8.1 核心思想

断点续传的本质不是“继续传一个大文件”，而是：

- 大文件先切成多个分片
- 服务端记录哪些分片已经上传成功
- 客户端重试时只补传未完成分片

### 8.2 推荐实现方式

- 前端：SparkMD5 计算文件 MD5
- 分片大小：5MB 或 10MB
- Redis：缓存任务状态、分布式锁
- MySQL：持久化上传任务
- 临时目录：保存 chunk 文件

### 8.3 合并逻辑关键点

- 按分片下标顺序合并
- 合并前校验分片数量是否完整
- 合并后重新计算整文件 MD5
- 校验成功再正式入库
- 最后删除临时分片目录

### 8.4 常见坑

- 分片重复上传
- 合并时顺序错乱
- 临时文件未清理
- 并发重复合并
- 上传中断后状态丢失
- 大文件直接读入内存导致 OOM

---

## 9. 文件类型校验设计

### 9.1 不能只看后缀

错误做法：

- 只校验 `.jpg`、`.png`、`.pdf`

因为用户可以把恶意脚本文件改名后上传。

### 9.2 正确校验方式

建议多层校验：

- 校验文件后缀白名单
- 校验 `Content-Type`
- 校验文件魔数
- 对高风险类型做严格限制

### 9.3 白名单示例

```text
图片：jpg、jpeg、png、gif、webp
文档：pdf、doc、docx、xls、xlsx、ppt、pptx、txt
压缩包：zip、rar、7z
```

### 9.4 高风险文件建议禁止

```text
exe、sh、bat、cmd、js、jsp、php、html
```

如果业务必须上传源码或脚本文件，建议：

- 存储时重命名
- 下载时强制附件下载
- 不允许浏览器直接执行

---

## 10. 本地存储与 OSS 设计

### 10.1 本地存储

优点：

- 开发简单
- 本地联调方便
- 成本低

缺点：

- 不适合多机部署
- 扩容麻烦
- 不利于生产环境高可用

### 10.2 阿里云 OSS

优点：

- 天然适合海量文件存储
- 支持高可用
- 支持 CDN 加速
- 支持签名 URL
- 更贴近真实企业方案

缺点：

- 接入成本稍高
- 需要云账号和费用

### 10.3 推荐项目表达方式

你简历中建议写成：

- 基于策略模式封装本地存储与阿里云 OSS 双存储实现
- 支持配置化切换存储引擎
- 统一文件上传、下载、删除、预览接口

这句话非常适合放项目亮点里。

---

## 11. 下载设计

### 11.1 下载的两种方式

- 方式一：后端代理下载
- 方式二：返回 OSS 签名下载链接

### 11.2 推荐方案

#### 小文件

- 可以由后端直接输出文件流

#### 大文件 / OSS 文件

- 推荐生成带过期时间的签名 URL
- 减轻后端带宽压力

### 11.3 下载安全控制

建议增加：

- 登录鉴权
- 文件所属权限校验
- 下载次数限制
- 防盗链
- 敏感文件签名短链

---

## 12. 关键实现细节

### 12.1 文件唯一命名

推荐命名规则：

```text
年/月/日/UUID.后缀
```

示例：

```text
2026/05/02/550e8400-e29b-41d4-a716-446655440000.pdf
```

优点：

- 防止重名覆盖
- 目录层级清晰
- 便于排查和归档

### 12.2 流式下载

不要写成一次性读入字节数组的大文件下载。

建议使用响应输出流按块传输。

### 12.3 上传限制

建议同时做：

- 单文件大小限制
- 单次请求总大小限制
- 用户总容量限制
- 单日上传次数限制

### 12.4 分布式锁

合并分片时建议使用 Redis 锁，避免同一个任务被重复合并。

---

## 13. 面试高频问题

### 13.1 为什么要分片上传

因为大文件直接上传：

- 容易超时
- 网络波动后要重传全部内容
- 用户体验差
- 服务端压力更大

### 13.2 断点续传怎么做

核心是记录已成功上传的分片索引，然后客户端只补传缺失分片。

### 13.3 秒传怎么做

核心是文件内容指纹，通常使用 MD5。上传前先查库，已有相同 MD5 文件则直接复用。

### 13.4 为什么不能只校验文件后缀

因为后缀可以伪造，必须结合 MIME 类型和文件魔数做综合判断。

### 13.5 本地存储和 OSS 怎么抽象

通过统一存储接口 + 策略模式，把上传、下载、删除、预览等能力抽象出来。

---

## 14. 项目亮点写法

### 14.1 简历版表达

- 设计并实现通用文件中心服务，支持普通上传、分片上传、断点续传、秒传、文件预览与下载鉴权
- 基于策略模式抽象本地存储与阿里云 OSS 双存储实现，支持配置化切换，提升系统可扩展性
- 基于文件 MD5 实现秒传与去重，减少重复文件上传与存储占用
- 使用 Redis + MySQL 管理上传任务状态，支持大文件断点续传与分片合并，提升弱网场景上传成功率
- 实现文件后缀、MIME、魔数多层校验机制，降低恶意文件上传风险

### 14.2 面试版亮点

你可以重点讲 3 个层次：

1. 功能层：上传、下载、预览、秒传、断点续传
2. 设计层：存储策略抽象、任务状态管理、分片合并机制
3. 优化层：去重、鉴权、安全校验、并发控制

这样表达会比单纯背接口流程更像真正做过项目的人。

---

## 15. 推荐开发顺序

建议你按下面顺序做，效率最高：

1. 完成文件普通上传/下载/删除
2. 做本地存储版跑通
3. 落库 file_info
4. 抽象 FileStorageService
5. 接入阿里云 OSS
6. 实现文件类型校验
7. 实现秒传
8. 实现分片上传
9. 实现断点续传
10. 做前端上传进度页
11. 最后补权限、日志、异常处理

这个顺序很适合你当前“求职项目优先”的路线，不会一上来就陷进复杂分片逻辑里。

---

## 16. 最终项目包装建议

如果你想把这个项目做成真正能写进简历的一档项目，建议命名为：

- 通用文件中心系统
- 企业级文件上传下载平台
- 云存储文件服务系统

其中最推荐：

**通用文件中心系统**

原因：

- 名称专业
- 容易扩展业务场景
- 后续可以继续接“AI 知识库文档导入”“用户头像中心”“简历附件中心”等模块

这对你走 Java 后端 + AI 应用开发路线非常顺手。

---

## 17. 你下一步最该做什么

如果你准备正式开做，这个项目最合理的 MVP 范围是：

- Spring Boot + MySQL + Redis
- 本地存储 + OSS 双实现
- 普通上传下载
- 文件类型校验
- 秒传
- 分片上传 + 断点续传
- Vue3 简单上传页面

这个版本已经足够做成一段很像样的简历项目。
