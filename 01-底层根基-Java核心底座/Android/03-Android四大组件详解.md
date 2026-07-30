# Android 四大组件详解

> 🧩 Activity、Service、BroadcastReceiver、ContentProvider —— Android 应用的四大核心组件，是骨架，是灵魂，是一切 Android 应用的基础

---

## 📚 目录

1. [四大组件全景](#1-四大组件全景)
2. [Activity：用户界面的载体](#2-activity用户界面的载体)
3. [Service：后台任务的执行者](#3-service后台任务的执行者)
4. [BroadcastReceiver：系统消息的监听者](#4-broadcastreceiver系统消息的监听者)
5. [ContentProvider：数据的共享者](#5-contentprovider数据的共享者)
6. [Intent：组件间通信的桥梁](#6-intent组件间通信的桥梁)

---

## 1. 四大组件全景

```text
Android 应用的四大组件 → 都必须在 AndroidManifest.xml 中注册！

┌─────────────────────────────────────────────────────┐
│                    AndroidManifest.xml               │
│                                                      │
│  <activity>          ← 带 UI 的屏幕界面               │
│  <service>           ← 后台无界面长期任务              │
│  <receiver>          ← 监听系统/应用广播               │
│  <provider>          ← 共享数据给其他应用              │
│                                                      │
└─────────────────────────────────────────────────────┘

Java 后端类比：
├── Activity      ≈ Controller + View (一个页面)
├── Service       ≈ @Scheduled 后台任务 / 守护线程
├── BroadcastReceiver ≈ 消息队列监听者 (MQ Consumer)
└── ContentProvider ≈ REST API (对外暴露数据接口)
```

| 组件 | 有无 UI | 生命周期方式 | 启动方式 | 跨应用 |
|------|:---:|------|------|:---:|
| **Activity** | ✅ | 用户操作驱动（入栈/出栈） | `startActivity()` | ✅ |
| **Service** | ❌ | 长期运行 | `startService()` / `bindService()` | ✅ |
| **BroadcastReceiver** | ❌ | 瞬态（收到广播即死） | 系统广播 / `sendBroadcast()` | ✅ |
| **ContentProvider** | ❌ | 与应用同生命周期 | `ContentResolver` CRUD | ✅ |

---

## 2. Activity：用户界面的载体

### 2.1 生命周期（必须牢记）

```text
          Activity Launched
                │
    ┌───────────▼───────────┐
    │      onCreate()       │  ← 初始化布局、绑定数据
    └───────────┬───────────┘
                │
    ┌───────────▼───────────┐
    │      onStart()        │  ← Activity 可见但不可交互
    └───────────┬───────────┘
                │
    ┌───────────▼───────────┐
    │      onResume()       │  ← 用户可见、可交互（前台）
    └───────────┬───────────┘
                │
           ═════╪═════ ← Activity Running
                │
    ┌───────────▼───────────┐      ┌──────────────────┐
    │      onPause()        │      │ 部分被遮挡/弹出    │
    └───────────┬───────────┘      │ 对话框 → onResume │
                │                  └──────────────────┘
    ┌───────────▼───────────┐
    │      onStop()         │  ← 完全不可见（切到后台/跳转其他页面）
    └───────────┬───────────┘
         ╱      │      ╲
    onRestart() │       ┊ App 被系统杀死（内存不足）
    (重新可见)   │       ┊
         ╲      │      ╱
    ┌───────────▼───────────┐
    │      onDestroy()      │  ← 用户关闭 / 系统回收
    └───────────────────────┘
```

### 2.2 生命周期方法职责

```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ✅ 做：初始化 View、绑定数据、恢复 savedInstanceState
        // ❌ 勿：耗时操作（网络请求、数据库大量读写）
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // ✅ 做：注册 BroadcastReceiver、绑定 Service
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ 做：开始动画、启动相机预览、恢复游戏
        // ❌ 勿：加载大量资源（已在 onCreate 做过）
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ✅ 做：保存草稿、停止动画、释放相机
        // ❌ 勿：耗时的数据库写入（用 onStop）
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ✅ 做：释放大量资源、取消广播注册
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ 做：最后的清理（ViewModel 被清除、杀掉异步任务）
    }
}
```

### 2.3 启动模式 (launchMode)

| 模式 | 行为 | 场景 |
|------|------|------|
| **standard** (默认) | 每次启动创建新实例，允许多个 | 普通页面 |
| **singleTop** | 如果在栈顶，复用（`onNewIntent()`）；否则新建 | 搜索结果页 |
| **singleTask** | 栈内唯一，启动时清除其上所有 Activity | 主页面 / 首页 |
| **singleInstance** | 独占一个任务栈 | 支付页面、来电界面 |

```xml
<!-- AndroidManifest.xml 中声明 -->
<activity
    android:name=".MainActivity"
    android:launchMode="singleTask" />
```

### 2.4 状态保存与恢复

```java
// 系统杀死进程前保存轻量级状态
@Override
protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString("USER_INPUT", editText.getText().toString());
}

// 重新创建时恢复
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
        String savedText = savedInstanceState.getString("USER_INPUT");
        editText.setText(savedText);
    }
}

// ⚠️ Bundle 只能存 1MB 以下的轻量数据！
// 大量数据用 ViewModel + SavedStateHandle 或持久化到 DB
```

> 🎯 **核心要点**：`onSaveInstanceState` 和 `onRestoreInstanceState` **只在系统杀死进程后重建时**才会被调用（如旋转屏幕）。用户主动 `finish()` 或按返回键不会触发。

---

## 3. Service：后台任务的执行者

### 3.1 Service 类型

| 类型 | 启动方式 | 通信 | 生命周期 | 适用场景 |
|------|---------|------|------|------|
| **前台 Service** | `startForegroundService()` | 通知栏可见 | 独立（即使 Activity 销毁） | 音乐播放、导航、下载 |
| **后台 Service** | `startService()` | 单向 | 独立 | Android 8+ 严格限制 |
| **绑定 Service** | `bindService()` | 双向 (AIDL/Messenger) | 随绑定者销毁 | 应用内通信、音乐控制 |

### 3.2 前台 Service 示例

```java
public class MusicService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 创建通知渠道（Android 8.0+ 必须）
        NotificationChannel channel = new NotificationChannel(
            "music_channel", "Music Playback",
            NotificationManager.IMPORTANCE_LOW
        );

        // 构建前台通知
        Notification notification = new NotificationCompat.Builder(this, "music_channel")
            .setContentTitle("正在播放")
            .setContentText("歌曲名称...")
            .setSmallIcon(R.drawable.ic_music)
            .build();

        // 提升为前台服务
        startForeground(1, notification);

        // 执行后台任务...
        playMusic();

        return START_STICKY;  // 被杀死后自动重启
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
```

### 3.3 Android 后台限制演进

```text
Android 版本对后台服务的限制越来越严格：

Android 8 (API 26):
  → 后台 Service 60 秒限制，推荐前台 Service + 通知

Android 12 (API 31):
  → 前台 Service 启动受限，从后台启动前台 Service 报异常

Android 14 (API 34):
  → 前台 Service 必须声明类型：dataSync、mediaPlayback、location...

推荐替代方案：
├── WorkManager  → 延迟/定期任务（最推荐）
├── JobScheduler  → 系统调度任务
├── AlarmManager  → 定时唤醒
└── Foreground Service → 用户需要感知的任务
```

### 3.4 WorkManager：Service 的现代替代

```java
// 创建后台任务（类似 Spring 的 @Scheduled）
public class SyncWorker extends Worker {

    @NonNull
    @Override
    public Result doWork() {
        // 后台同步数据到服务器
        syncDataToServer();
        return Result.success();
    }
}

// 调度任务
WorkManager.getInstance(context)
    .enqueue(new OneTimeWorkRequest.Builder(SyncWorker.class)
        .setConstraints(new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)  // 需要网络
            .build())
        .build());

// 定期任务（每 15 分钟）
PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
    SyncWorker.class, 15, TimeUnit.MINUTES)
    .build();
```

> 💡 **Java 后端类比**：WorkManager ≈ Spring 的 `@Scheduled` + 分布式任务调度（但单机）。

---

## 4. BroadcastReceiver：系统消息的监听者

### 4.1 广播机制

```text
广播 = Android 的发布-订阅模式：

发送者 → sendBroadcast(intent) → System → 分发给所有匹配的 Receiver

常见系统广播：
├── android.intent.action.BOOT_COMPLETED    ← 开机完成
├── android.intent.action.BATTERY_LOW       ← 电量低
├── android.net.conn.CONNECTIVITY_CHANGE    ← 网络状态变化
├── android.intent.action.AIRPLANE_MODE     ← 飞行模式
├── android.intent.action.SCREEN_OFF        ← 屏幕关闭
└── android.intent.action.TIME_TICK         ← 每分钟一次
```

### 4.2 注册方式对比

| 方式 | 注册 | 生命周期 | Android 8+ 限制 |
|------|------|------|:---:|
| **静态注册** (Manifest) | `<receiver>` in XML | 应用未启动也能收 | 大部分隐式广播被禁止 |
| **动态注册** (代码) | `registerReceiver()` | 组件存活时有效 | ✅ 推荐 |

```java
// 动态注册（代码中注册/注销）
public class MainActivity extends AppCompatActivity {
    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            Log.d("Battery", "当前电量：" + level + "%");
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(batteryReceiver);      // 必须注销！
    }
}
```

### 4.3 本地广播 (LocalBroadcastManager)

```java
// 应用内广播（不走系统进程，更安全、更快）
// Jetpack 已弃用 LocalBroadcastManager，推荐用 Flow/LiveData 替代

// 现代替代方案：SharedFlow
class MyEventBus {
    public static final MutableSharedFlow<String> events = MutableSharedFlow<>();
}

// 发送
MyEventBus.events.emit("DATA_UPDATED");

// 接收
MyEventBus.events.collect { event ->
    // 处理事件
}
```

> 🎯 **Java 后端类比**：LocalBroadcast ≈ Spring Event / Guava EventBus，系统广播 ≈ 接收 MQ 消息。

---

## 5. ContentProvider：数据的共享者

### 5.1 概念与用途

```text
ContentProvider = Android 的跨进程数据共享机制

你的应用                      其他应用
┌──────────┐                ┌──────────┐
│  SQLite  │ ←─ContentProvider──→│  Contacts│
│  Files   │    (URI 访问)       │  App     │
└──────────┘                └──────────┘

系统预置 ContentProvider：
├── content://contacts/people/       ← 联系人
├── content://media/external/images/ ← 相册图片
├── content://sms/                   ← 短信
├── content://call_log/              ← 通话记录
└── content://settings/              ← 系统设置
```

### 5.2 使用 ContentResolver 读取系统数据

```java
// 读取所有联系人（需要 READ_CONTACTS 权限）
Cursor cursor = getContentResolver().query(
    ContactsContract.Contacts.CONTENT_URI,  // URI
    null,                                     // 投影（列）
    null,                                     // 选择（WHERE）
    null,                                     // 选择参数
    null                                      // 排序
);

while (cursor.moveToNext()) {
    String name = cursor.getString(
        cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
    Log.d("Contacts", name);
}
cursor.close();  // ⚠️ 必须关闭 Cursor！
```

### 5.3 创建自定义 ContentProvider

```java
public class MyContentProvider extends ContentProvider {

    // 定义 URI 匹配规则
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI("com.example.myapp.provider", "users", 1);
        uriMatcher.addURI("com.example.myapp.provider", "users/#", 2);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // 类似 Spring 的 @GetMapping
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.query("users", projection, selection, selectionArgs,
                       null, null, sortOrder);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // 类似 @PostMapping
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert("users", null, values);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // 类似 @DeleteMapping
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete("users", selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values,
                     String selection, String[] selectionArgs) {
        // 类似 @PutMapping
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update("users", values, selection, selectionArgs);
    }

    @Override
    public boolean onCreate() { return true; }

    @Override
    public String getType(Uri uri) { return "vnd.android.cursor.dir/users"; }
}
```

> 🎯 **核心要点**：ContentProvider 本质上是对 SQLite/文件的 URI 封装层，为 Android 提供 **REST-like** 的跨进程数据访问。

---

## 6. Intent：组件间通信的桥梁

### 6.1 显式 Intent vs 隐式 Intent

```java
// === 显式 Intent：明确指定目标组件 ===
// 从 Activity A 跳转到 Activity B
Intent intent = new Intent(MainActivity.this, DetailActivity.class);
intent.putExtra("ITEM_ID", 12345);     // 传递数据
startActivity(intent);

// === 隐式 Intent：指定 action，系统匹配合适的应用 ===
// 分享文本
Intent shareIntent = new Intent(Intent.ACTION_SEND);
shareIntent.setType("text/plain");
shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this app!");
startActivity(Intent.createChooser(shareIntent, "分享至"));

// 打电话
Intent dialIntent = new Intent(Intent.ACTION_DIAL);
dialIntent.setData(Uri.parse("tel:10086"));
startActivity(dialIntent);

// 打开网页
Intent webIntent = new Intent(Intent.ACTION_VIEW);
webIntent.setData(Uri.parse("https://developer.android.com"));
startActivity(webIntent);
```

### 6.2 Intent 数据传递

| 传递方式 | 数据量 | 生命周期 |
|---------|:---:|------|
| `putExtra()` 基本类型 | 小 | 单次跳转 |
| `putExtra()` + Serializable | 小-中 | 单次跳转（性能差，已淘汰） |
| `putExtra()` + **Parcelable** | 中 | 单次跳转（Android 推荐） |
| **ViewModel + SavedStateHandle** | 不限 | 跨配置变更存活 |
| **单例 / 全局变量** | 不限 | 进程存活（不推荐，内存回收丢失） |
| **数据库 / DataStore** | 不限 | 持久化 |

```java
// 使用 Parcelable（Android 推荐序列化方式）
// Kotlin 中只需 @Parcelize 注解，Java 中实现 Parcelable 接口
// 或用 Gson 把对象转成 JSON string 放到 Intent 里
Gson gson = new Gson();
String userJson = gson.toJson(user);
intent.putExtra("USER_JSON", userJson);

// 接收
String json = getIntent().getStringExtra("USER_JSON");
User user = gson.fromJson(json, User.class);
```

---

**上一模块**：[02-Android开发环境搭建](./02-Android开发环境搭建.md) ｜ **下一模块**：[04-Android UI与界面开发](./04-Android UI与界面开发.md) ｜ **返回总览**：[00-Android知识体系总览](./00-Android知识体系总览.md)

---

*创建于：2026年7月*
