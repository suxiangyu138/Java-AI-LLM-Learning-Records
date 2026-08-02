# Android 数据存储与网络通信

> 💾 SharedPreferences → DataStore → SQLite → Room (ORM)，Retrofit + OkHttp 双雄组合 —— Android 的数据持久化与网络通信全栈指南，每个方案都有 Java 后端对比

---

## 📚 目录

1. [数据存储方案全景对比](#1-数据存储方案全景对比)
2. [轻量存储：DataStore](#2-轻量存储datastore)
3. [关系型存储：Room](#3-关系型存储room)
4. [文件存储](#4-文件存储)
5. [网络通信：Retrofit + OkHttp](#5-网络通信retrofit--okhttp)
6. [序列化方案对比](#6-序列化方案对比)

---

## 1. 数据存储方案全景对比

| 方案 | 数据类型 | 容量 | 跨进程 | 后端类比 | 推荐场景 |
|------|---------|:---:|:---:|------|------|
| **DataStore** | KV 键值对 | 小 | ❌ | Redis / application.properties | 用户偏好、设置 |
| **Room** | 结构化数据 | 不限 | ✅ (ContentProvider) | JPA/Hibernate + H2 | 本地数据库 |
| **内部存储** | 文件 | 不限 | ❌ | 文件系统 | 私有文件、缓存 |
| **外部存储** | 文件 | 不限 | ✅ | 共享目录 | 下载、图片、大文件 |
| **SharedPreferences** | KV 键值对（旧） | 小 | ❌ | — (已废弃) | ⚠️ 不推荐（迁移到 DataStore） |

---

## 2. 轻量存储：DataStore

### 2.1 SharedPreferences 为什么被淘汰

```text
SharedPreferences 的问题：
├── 同步阻塞读取（主线程读取会 ANR）
├── 全量读写（改一个 key，整文件重新写）
├── 无类型安全（全是 String/Int/Boolean 转换）
├── 无事务支持
└── apply() 异步写入可能丢失

DataStore 的优势：
├── 基于 Kotlin Coroutines + Flow（异步）
├── 类型安全（Protobuf / Kotlin Serialization）
├── 增量写入（只写变化的部分）
└── 原子操作 + 异常恢复
```

### 2.2 Preferences DataStore

```kotlin
// build.gradle.kts → dependencies
implementation("androidx.datastore:datastore-preferences:1.1.1")

// 1. 创建 DataStore
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// 2. 定义 key（类型安全！）
object SettingsKeys {
    val USERNAME = stringPreferencesKey("username")
    val TOKEN = stringPreferencesKey("token")
    val LOGIN_COUNT = intPreferencesKey("login_count")
    val DARK_MODE = booleanPreferencesKey("dark_mode")
}

// 3. 写入
suspend fun saveUsername(context: Context, username: String) {
    context.settingsDataStore.edit { preferences ->
        preferences[SettingsKeys.USERNAME] = username
        preferences[SettingsKeys.LOGIN_COUNT] = (preferences[SettingsKeys.LOGIN_COUNT] ?: 0) + 1
    }
}

// 4. 读取（Flow: 数据变化自动通知）
fun getUsername(context: Context): Flow<String?> {
    return context.settingsDataStore.data.map { preferences ->
        preferences[SettingsKeys.USERNAME]
    }
}

// 5. 在 ViewModel 中使用
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: DataStore<Preferences>
) : ViewModel() {
    val username: StateFlow<String?> = settingsDataStore.data
        .map { it[SettingsKeys.USERNAME] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
```

### 2.3 用 Java 访问 DataStore

```java
// DataStore 以 Kotlin Coroutines 为核心，Java 使用需借助 RxJava 或 Guava
// 推荐方案：Java 项目继续用 SharedPreferences 做简单 KV
// 或用 MMKV (腾讯开源) 作为高性能替代

// MMKV 使用示例
MMKV kv = MMKV.defaultMMKV();
kv.encode("username", "john_doe");
kv.encode("login_count", 5);
String username = kv.decodeString("username", "");
int count = kv.decodeInt("login_count", 0);
```

---

## 3. 关系型存储：Room

### 3.1 Room 架构 —— Android 官方 ORM

```text
Room = SQLite 的抽象层 = Android 版 JPA + Spring Data

┌────────────────────────────────────┐
│            Room Database           │
│  ┌──────────┐  ┌────────────────┐  │
│  │  Entity  │  │      DAO       │  │
│  │ (表定义) │←─│ (CRUD 接口)    │  │
│  └──────────┘  └────────────────┘  │
│        自动生成实现类               │
└────────────────────────────────────┘

Java 后端对比：
├── Entity    = @Entity (JPA)
├── DAO       = JpaRepository (Spring Data)
├── Database  = DataSource + EntityManagerFactory
└── Migration = Flyway / Liquibase
```

### 3.2 Room 完整示例

```java
// === 1. Entity：定义表结构 ===
@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "username")
    public String username;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    public User(String username, String email) {
        this.username = username;
        this.email = email;
        this.createdAt = System.currentTimeMillis();
    }
}

// === 2. DAO：定义 CRUD 操作 ===
@Dao
public interface UserDao {

    // 查询所有用户
    @Query("SELECT * FROM users ORDER BY created_at DESC")
    List<User> getAllUsers();

    // 按 ID 查
    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserById(long userId);

    // 搜索
    @Query("SELECT * FROM users WHERE username LIKE :query")
    List<User> searchUsers(String query);

    // 插入
    @Insert
    long insertUser(User user);

    // 批量插入
    @Insert
    List<Long> insertAll(List<User> users);

    // 更新
    @Update
    int updateUser(User user);

    // 删除
    @Delete
    int deleteUser(User user);

    // 使用 LiveData / Flow 监听数据变化（Kotlin）
    // @Query("SELECT * FROM users") → Flow<List<User>>
}

// === 3. Database：定义数据库 ===
@Database(
    entities = {User.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();

    // 单例模式
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "app_database.db"
                    )
                    .fallbackToDestructiveMigration()  // 开发阶段可用
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}

// === 4. 使用 ===
// 注意：Room 操作必须在后台线程执行！
Executors.newSingleThreadExecutor().execute(() -> {
    AppDatabase db = AppDatabase.getInstance(context);
    UserDao userDao = db.userDao();

    // 插入
    User user = new User("john_doe", "john@example.com");
    long id = userDao.insertUser(user);

    // 查询
    List<User> allUsers = userDao.getAllUsers();
    for (User u : allUsers) {
        Log.d("DB", u.username + " - " + u.email);
    }
});
```

### 3.3 Room 数据库迁移

```java
// 版本 1 → 2：添加 age 列
static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    @Override
    public void migrate(SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE users ADD COLUMN age INTEGER DEFAULT 0 NOT NULL");
    }
};

// 版本 2 → 3：添加 address 表
static final Migration MIGRATION_2_3 = new Migration(2, 3) {
    @Override
    public void migrate(SupportSQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `addresses` ("
            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
            + "`user_id` INTEGER NOT NULL, "
            + "`street` TEXT, "
            + "`city` TEXT, "
            + "FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE)");
    }
};

// 在 Database 构建时注册 Migration
Room.databaseBuilder(context, AppDatabase.class, "app_database.db")
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
    .build();
```

> 🎯 **JPA 开发者注意**：Room 没有 `@OneToMany` / `@ManyToOne` 自动关联。多表关联必须手动定义关系类 + 联合查询。

---

## 4. 文件存储

### 4.1 Android 存储路径全景

```text
Android 存储分为：

内部存储 (Internal Storage) —— 应用私有，其他应用不可见
├── files/          → getFilesDir()       ← 持久文件
├── cache/          → getCacheDir()       ← 缓存（系统可清理）
└── databases/      → Room/SQLite 数据

外部存储 (External Storage) —— 可共享（需权限）
├── 应用专属外部文件 → getExternalFilesDir() ← API 19+ 无需权限
├── 应用专属外部缓存 → getExternalCacheDir()
├── 公共目录        → Environment.getExternalStoragePublicDirectory()
│   ├── DCIM/         ← 照片
│   ├── Downloads/    ← 下载
│   └── Music/        ← 音乐
└── MediaStore       ← Android 10+ 推荐用 MediaStore API 访问

Scoped Storage (分区存储) —— Android 10+ 强制
└── 应用只能访问自己的目录 + MediaStore，无法随意访问整个文件系统
```

### 4.2 文件读写

```java
// === 写文件到内部存储 ===
String filename = "config.json";
String fileContents = "{\"version\": \"1.0\"}";

try (FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE)) {
    fos.write(fileContents.getBytes());
}

// === 读文件 ===
try (FileInputStream fis = openFileInput(filename)) {
    InputStreamReader isr = new InputStreamReader(fis);
    BufferedReader br = new BufferedReader(isr);
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
        sb.append(line);
    }
    String content = sb.toString();
}

// === 缓存文件 ===
File cacheFile = new File(getCacheDir(), "temp_image.png");
// 系统可能自动清理，不要存重要数据
```

### 4.3 Android 文件权限演进

```text
Android 存储权限变化：

Android 9 及以前：
├── READ_EXTERNAL_STORAGE
└── WRITE_EXTERNAL_STORAGE

Android 10 (API 29)：
├── Scoped Storage 默认启用
├── 应用可直接访问自己的外部目录（无需权限）
└── 访问公共目录需通过 MediaStore

Android 11 (API 30)：
├── 强制 Scoped Storage
└── MANAGE_EXTERNAL_STORAGE（特殊权限，Google Play 严格审核）

Android 13 (API 33)：
├── READ_EXTERNAL_STORAGE 废弃
├── 细分权限：READ_MEDIA_IMAGES、READ_MEDIA_VIDEO、READ_MEDIA_AUDIO
└── 图片选择器 (Photo Picker) 推荐代替自定义文件选择
```

---

## 5. 网络通信：Retrofit + OkHttp

### 5.1 技术栈对比

| 层 | Android | Spring Boot (后端类比) |
|------|---------|-------|
| **HTTP 引擎** | OkHttp | Apache HttpClient / Netty |
| **API 声明** | Retrofit (接口 + 注解) | RestClient / WebClient |
| **序列化** | Gson / Moshi | Jackson |
| **拦截器** | OkHttp Interceptor | Spring Interceptor / Filter |
| **图片加载** | Glide / Coil | 不适用 |
| **WebSocket** | OkHttp WebSocket | Spring WebSocket |

### 5.2 Retrofit + OkHttp 完整示例

```kotlin
// === 1. 定义 API 接口 ===
interface ApiService {

    // GET 请求
    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") userId: Long): Response<User>

    // POST 请求
    @POST("api/users")
    suspend fun createUser(@Body user: User): Response<User>

    // 带查询参数
    @GET("api/users")
    suspend fun searchUsers(@Query("q") query: String,
                            @Query("page") page: Int = 1): Response<List<User>>

    // 表单提交
    @FormUrlEncoded
    @POST("api/login")
    suspend fun login(@Field("username") username: String,
                      @Field("password") password: String): Response<LoginResponse>

    // 文件上传
    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<UploadResponse>
}

// === 2. 创建 Retrofit 实例 ===
object RetrofitClient {
    private const val BASE_URL = "https://api.example.com/"

    // OkHttp 客户端（可添加拦截器）
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            // 添加通用请求头
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${TokenManager.getToken()}")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY  // 打印请求/响应日志
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())  // JSON 转换
        .build()
        .create(ApiService::class.java)
}

// === 3. 使用（在 ViewModel / Repository 中）===
class UserRepository {
    private val api = RetrofitClient.apiService

    suspend fun getUser(userId: Long): Result<User> {
        return try {
            val response = api.getUser(userId)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// === 4. 在 ViewModel 中调用 ===
@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    fun loadUser(userId: Long) {
        viewModelScope.launch {
            repository.getUser(userId).onSuccess { user ->
                _user.value = user
            }.onFailure { error ->
                // 处理错误
                Log.e("UserVM", "Failed to load user", error)
            }
        }
    }
}
```

### 5.3 网络请求的 Java 版本

```java
// Retrofit 同样完美支持 Java
public interface ApiService {
    @GET("api/users/{id}")
    Call<User> getUser(@Path("id") long userId);

    @POST("api/users")
    Call<User> createUser(@Body User user);
}

// 使用（需要处理异步回调）
apiService.getUser(1L).enqueue(new Callback<User>() {
    @Override
    public void onResponse(Call<User> call, Response<User> response) {
        if (response.isSuccessful()) {
            User user = response.body();
            // 更新 UI...
        }
    }

    @Override
    public void onFailure(Call<User> call, Throwable t) {
        Log.e("API", "请求失败", t);
    }
});
```

### 5.4 OkHttp Interceptor 实战

```java
// 1. 日志拦截器：打印所有请求和响应
HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
logging.setLevel(HttpLoggingInterceptor.Level.BODY);

// 2. Token 自动刷新拦截器
class TokenRefreshInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request().newBuilder()
            .header("Authorization", "Bearer " + getAccessToken())
            .build();

        Response response = chain.proceed(request);

        // 如果 Token 过期（401），自动刷新
        if (response.code() == 401) {
            String newToken = refreshToken();
            // 用新 Token 重试请求
            request = request.newBuilder()
                .header("Authorization", "Bearer " + newToken)
                .build();
            response = chain.proceed(request);
        }
        return response;
    }
}

// 3. 缓存拦截器
Cache cache = new Cache(new File(context.getCacheDir(), "http_cache"), 10 * 1024 * 1024);
OkHttpClient client = new OkHttpClient.Builder()
    .cache(cache)
    .addInterceptor(new TokenRefreshInterceptor())
    .addInterceptor(logging)
    .build();
```

> 💡 **Retrofit 使用注意**：Android 默认不允许 HTTP 明文请求（Android 9+）。开发环境用 `android:usesCleartextTraffic="true"`，生产环境必须 HTTPS。

---

## 6. 序列化方案对比

### 6.1 方案全景

| 方案 | 速度 | APK 体积 | Kotlin 支持 | Java 支持 | 推荐度 |
|------|:---:|:---:|:---:|:---:|:---:|
| **Gson** | 中等 | 中等 | ✅ | ✅ | ⭐⭐⭐ (经典) |
| **Moshi** | 快 | 小 | ✅ | ✅ | ⭐⭐⭐⭐ (Kotlin 友好) |
| **Kotlinx Serialization** | 最快 | 最小 | ✅ | ❌ | ⭐⭐⭐⭐⭐ (Kotlin 首选) |
| **Jackson** | 慢 | 大 | ⚠️ | ✅ | ⭐⭐ (太重) |
| **Serializable** (Java) | 慢 | — | ⚠️ | ✅ | ❌ (Android 别用) |
| **Parcelable** (Android) | 最快 | — | ✅ (Parcelize) | ⚠️ (手写繁琐) | ⭐⭐⭐⭐ (进程间) |

### 6.2 Gson vs Moshi 对比

```java
// === Gson（最经典，Java 生态标准）===
Gson gson = new Gson();
// 序列化
String json = gson.toJson(user);
// 反序列化
User user = gson.fromJson(json, User.class);

// Retrofit 中使用 Gson
Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())

// === Moshi（更现代，Kotlin 支持更好）===
// 代码生成方式（编译时，零反射）
Moshi moshi = new Moshi.Builder()
    .add(KotlinJsonAdapterFactory())  // Kotlin data class 支持
    .build();
JsonAdapter<User> adapter = moshi.adapter(User.class);
String json = adapter.toJson(user);
User user = adapter.fromJson(json);
```

### 6.3 选型建议

```text
Java 项目：     → Gson（生态成熟，资料多）
Kotlin 项目：   → Kotlinx Serialization（最快、最小）
跨进程传递：    → Parcelable（@Parcelize 注解）
文件缓存：      → 存 JSON → Gson/Moshi
Room 数据库：   → @Embedded 直接存对象
SharedPreferences/DataStore → 转 JSON String 存储
```

---

**上一模块**：[04-Android UI与界面开发](./04-Android UI与界面开发.md) ｜ **下一模块**：[06-Android构建发布与性能优化](./06-Android构建发布与性能优化.md) ｜ **返回总览**：[00-Android知识体系总览](./00-Android知识体系总览.md)

---

*创建于：2026年7月*
