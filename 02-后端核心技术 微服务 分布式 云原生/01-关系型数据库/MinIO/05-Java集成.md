# Java 集成
> MinIO Java SDK 全解：依赖与客户端、上传下载、分片上传、流式、预签名、版本操作——S3 兼容意味着这套代码切云 OSS 零改动

## 📚 目录
1. [依赖与客户端初始化](#1-依赖与客户端初始化)
2. [基础操作：桶与对象](#2-基础操作桶与对象)
3. [上传全形态](#3-上传全形态)
4. [分片上传：大文件的正确姿势](#4-分片上传大文件的正确姿势)
5. [下载与流式处理](#5-下载与流式处理)
6. [预签名 URL 生成](#6-预签名-url-生成)
7. [版本控制与删除](#7-版本控制与删除)
8. [与云 OSS 的无缝切换](#8-与云-oss-的无缝切换)

## 1. 依赖与客户端初始化

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.x</version>        <!-- 2026 当前 8.5+ -->
</dependency>
```

```java
// 客户端初始化（生产：配置类 + 环境变量注入）
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                // 生产走 HTTPS；开发连自签 CA 时在此配置信任库
                // .httpClient(trustingCaClient())
                .build();
    }
}
```

```yaml
# application.yml（密钥走环境变量，不进仓库）
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
```

> 🎯 依赖哲学：**SDK 只有几十 KB，全部能力都是 S3 HTTP API 的封装**——网络问题排查、签名问题、超时配置，底层都在 HTTP 层（与 [Web 高阶 HTTP 体系](../../09-Web开发全流程/Web%20高阶知识/01-HTTP协议深入.md) 同源）。

## 2. 基础操作：桶与对象

```java
@Service
public class MinioService {
    private final MinioClient client;

    // ═══ 桶操作 ═══
    public void ensureBucket(String bucket) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder()
                .bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucket).build());
        }
    }

    // ═══ 对象存在性 ═══
    public boolean objectExists(String bucket, String objectKey) {
        try {
            client.statObject(StatObjectArgs.builder()
                    .bucket(bucket).object(objectKey).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;               // NoSuchKey → 不存在
        }
    }
}
```

| 操作 | 方法 | 注意 |
|------|------|------|
| 建桶 | `makeBucket` | 桶名小写、全局唯一 |
| 查桶 | `bucketExists` | 幂等检查 |
| 对象信息 | `statObject` | 拿大小/ETag/元数据 |
| 列对象 | `listObjects` | 支持 prefix + 分页 |
| 删除 | `removeObject` | 见 §7 版本场景 |

## 3. 上传全形态

```java
// ═══ 形态一：字节流上传（接口接收 MultipartFile → 直接转存）═══
public String uploadFile(String bucket, String objectKey,
        InputStream stream, long size, String contentType) throws Exception {
    client.putObject(PutObjectArgs.builder()
            .bucket(bucket)
            .object(objectKey)
            .stream(stream, size, -1)          // 大小已知
            .contentType(contentType)
            .build());
    return objectKey;
}

// ═══ 形态二：文件上传（服务器本地文件）═══
client.uploadObject(UploadObjectArgs.builder()
        .bucket(bucket).object(objectKey)
        .filename("/tmp/photo.jpg")
        .build());

// ═══ 形态三：带自定义元数据 ═══
client.putObject(PutObjectArgs.builder()
        .bucket(bucket).object(objectKey)
        .stream(stream, size, -1)
        .contentType("image/jpeg")
        .userMetadata(Map.of("userId", "42", "originalName", "photo.jpg"))
        .build());
```

| 上传参数 | 说明 |
|---------|------|
| `.stream(stream, size, -1)` | 输入流 + 已知大小（`-1` = 未知，用分片） |
| `.contentType` | 重要！决定下载时的 Content-Type |
| `.userMetadata` | 自定义元数据（`X-Amz-Meta-*`，可回读） |
| 大小阈值 | **>100MB 用分片上传**（§4），`putObject` 适合中小文件 |

> ⚠️ **上传前先设计键**：`users/{userId}/avatars/{uuid}.jpg` 这样的键前缀（[01 §2](01-对象存储核心概念与S3模型.md)）——上传瞬间定死组织方式，后面改键 = 全部重传。

## 4. 分片上传：大文件的正确姿势

```java
// ═══ 大文件分片上传（>100MB 或流大小未知）═══
public void uploadLarge(String bucket, String objectKey, InputStream stream,
        long totalSize) throws Exception {
    // 1. 创建分片上传会话
    CreateMultipartUploadResponse init = client.createMultipartUpload(
            CreateMultipartUploadArgs.builder()
                    .bucket(bucket).object(objectKey)
                    .contentType("application/octet-stream")
                    .build());

    // 2. 分片上传（每片 5MB-5GB，推荐 10-100MB）
    List<Part> parts = new ArrayList<>();
    int partNumber = 1;
    byte[] buf = new byte[5 * 1024 * 1024];     // 5MB 一片
    int len;
    while ((len = stream.read(buf)) != -1) {
        ByteArrayInputStream partStream = new ByteArrayInputStream(buf, 0, len);
        UploadPartResponse resp = client.uploadPart(
                UploadPartArgs.builder()
                        .bucket(bucket).object(objectKey)
                        .uploadId(init.uploadId())       // 会话 ID
                        .partNumber(partNumber)          // 片号（1 开始）
                        .stream(partStream, len, -1)
                        .build());
        parts.add(new Part(partNumber++, resp.etag()));
    }

    // 3. 完成合并
    client.completeMultipartUpload(CompleteMultipartUploadArgs.builder()
            .bucket(bucket).object(objectKey)
            .uploadId(init.uploadId())
            .parts(parts)
            .build());
}
```

| 分片规则 | 说明 |
|---------|------|
| 每片大小 | 5MB 起（最大 5GB），推荐 10-100MB |
| 片数上限 | 10,000 片（超大文件算好片大小） |
| 断点续传 | 用 `listParts` 查已传片，续传未传部分 |
| 中断清理 | 未 complete 的分片会占用存储——配生命周期自动清理（`AbortIncompleteMultipartUpload`） |

> 🎯 面试点：**"大文件必须分片"的三个理由**——① 单请求大小限制；② 断点续传（网络抖动不用重传全部）；③ 并发加速（多片并行上传）。MinIO SDK 的 `putObject` 超过阈值会自动转分片，但**显式分片才能控制片大小与续传**。

## 5. 下载与流式处理

```java
// ═══ 下载到流（不落盘，直接响应前端）═══
public void download(String bucket, String objectKey, HttpServletResponse resp)
        throws Exception {
    GetObjectResponse obj = client.getObject(GetObjectArgs.builder()
            .bucket(bucket).object(objectKey).build());

    resp.setContentType(obj.headers().get("Content-Type"));
    resp.setContentLengthLong(obj.objectSize());
    // 附件下载（文件名用 RFC 5987 编码，见 Web 高阶体系）
    resp.setHeader("Content-Disposition",
            "attachment; filename*=UTF-8''" + URLEncoder.encode("文件.pdf", "UTF-8"));

    try (obj; ServletOutputStream out = resp.getOutputStream()) {
        obj.transferTo(out);            // 流式转发（内存恒定）
    }
}

// ═══ 范围读取（断点续传/视频拖拽）═══
client.getObject(GetObjectArgs.builder()
        .bucket(bucket).object(objectKey)
        .offset(1024 * 1024)            // 从 1MB 开始
        .length(1024 * 1024)            // 读 1MB
        .build());
```

> 💡 流式铁律：**下载永远"流式转发"**（`transferTo`），别 `readAllBytes()` 进内存——大文件 + 高并发 = OOM。`GetObjectResponse` 实现了 `AutoCloseable`，务必 try-with-resources。

## 6. 预签名 URL 生成

```java
// ═══ 预签名 GET（限时下载/图片直读）═══
public String presignedGet(String bucket, String objectKey, int expirySeconds)
        throws Exception {
    return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket(bucket).object(objectKey)
            .expiry(expirySeconds)
            .build());
}

// ═══ 预签名 PUT（前端直传，不经应用服务器）═══
public String presignedPut(String bucket, String objectKey, int expirySeconds)
        throws Exception {
    return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
            .method(Method.PUT)
            .bucket(bucket).object(objectKey)
            .expiry(expirySeconds)
            .build());
}

// 典型接口（Controller）
@GetMapping("/presigned/upload")
public Result<String> uploadUrl(@RequestParam String objectKey) {
    return Result.ok(minioService.presignedPut("avatars", objectKey, 900));  // 15 分钟
}
```

| 场景 | 预签名 | 过期建议 |
|------|--------|---------|
| 图片直读 | GET | 1 小时（长缓存资源可更长） |
| 文件下载 | GET | 按业务（15min-24h） |
| 前端直传 | PUT | **15 分钟**（越短越安全） |

> 🎯 架构收益（[06](06-后端架构配合.md) 展开）：**预签名 PUT 让文件不经过应用服务器**——应用只签发 URL，前端直接传 MinIO，带宽/内存/磁盘压力全部转移到对象存储。

## 7. 版本控制与删除

```java
// ═══ 删除对象（版本控制开启时打"删除标记"）═══
client.removeObject(RemoveObjectArgs.builder()
        .bucket(bucket).object(objectKey)
        .build());

// 彻底删除指定版本
client.removeObject(RemoveObjectArgs.builder()
        .bucket(bucket).object(objectKey)
        .versionId(versionId)             // 指定版本 ID
        .build());

// 回滚 = 复制旧版本覆盖最新（CopyObject）
client.copyObject(CopyObjectArgs.builder()
        .bucket(bucket).object(objectKey)
        .source(CopySource.builder()
                .bucket(bucket).object(objectKey)
                .versionId(oldVersionId)   // 旧版本
                .build())
        .build());
```

> ⚠️ 删除语义提醒：**版本控制开启后 `removeObject` 不是物理删除**——只是打删除标记（数据还在，可恢复）。物理清理靠生命周期规则或 `mc rm --versions`。别惊讶"删了但容量没降"。

## 8. 与云 OSS 的无缝切换

```java
// ═══ 切换云 OSS：只改端点与凭证，代码零改动 ═══
// AWS S3（中国区）：
//   endpoint: https://s3.cn-north-1.amazonaws.com.cn
// 阿里云 OSS（S3 兼容模式）：
//   endpoint: https://oss-cn-hangzhou.aliyuncs.com
//   access-key/secret-key: RAM 凭证

// 配置化切换（dev → prod 一条配置）
minio:
  endpoint: ${STORAGE_ENDPOINT}      # 本地 MinIO / 云 OSS 端点
  access-key: ${STORAGE_ACCESS_KEY}
  secret-key: ${STORAGE_SECRET_KEY}
```

| 切换项 | 说明 |
|--------|------|
| 端点 | 唯一必须改的 |
| 凭证 | 云厂商 RAM/AK |
| 桶名 | 云 OSS 桶名全局唯一（换桶名注意） |
| 区域 | S3 部分操作带 Region 参数（SDK 自动） |
| 预签名 | 兼容（云厂商支持） |
| 代码 | **零改动**（S3 API 兼容的承诺） |

> 🎯 架构价值：**S3 兼容 = 存储方案"可替换"**——开发用 MinIO（免费/私有），生产切云（托管/带宽），甚至双写迁移——这是选择 S3 兼容自托管方案的最大工程收益。

---

**下一模块**：[06-后端架构配合](06-后端架构配合.md) / **返回总览**：[00-MinIO总览](00-MinIO总览.md)
