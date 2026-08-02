# Android UI 与界面开发

> 🎨 从传统的 View + XML 到现代的 Jetpack Compose 声明式 UI —— Android 界面的两种范式、Material Design 3 设计规范、完整的界面开发指南

---

## 📚 目录

1. [View 体系：传统 XML 方式](#1-view-体系传统-xml-方式)
2. [五大经典布局](#2-五大经典布局)
3. [RecyclerView：列表的王者](#3-recyclerview列表的王者)
4. [Jetpack Compose：声明式 UI 新范式](#4-jetpack-compose声明式-ui-新范式)
5. [Material Design 3](#5-material-design-3)
6. [资源系统](#6-资源系统)

---

## 1. View 体系：传统 XML 方式

### 1.1 View 与 ViewGroup

```text
Android 界面 = View 树

View (单个 UI 元素)           ViewGroup (容器 + View)
├── TextView  (文本)          ├── LinearLayout (线性排列)
├── Button    (按钮)          ├── ConstraintLayout (约束布局) ← 最灵活
├── EditText  (输入框)        ├── FrameLayout (层叠)
├── ImageView (图片)          ├── RelativeLayout (相对位置)
└── ...                       └── RecyclerView (列表)
         │                              │
         └────────────┬─────────────────┘
                ┌─────▼──────┐
                │  根 ViewGroup  │  ← 如 ConstraintLayout
                │  ├── TextView   │
                │  ├── Button     │
                │  └── ImageView  │
                └────────────────┘
```

### 1.2 XML 布局 + Java 代码

`res/layout/activity_main.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/titleText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="用户登录"
        android:textSize="24sp"
        android:textStyle="bold"
        android:gravity="center" />

    <EditText
        android:id="@+id/usernameInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="请输入用户名"
        android:inputType="text" />

    <EditText
        android:id="@+id/passwordInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="请输入密码"
        android:inputType="textPassword" />

    <Button
        android:id="@+id/loginButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="登录" />

</LinearLayout>
```

`MainActivity.java`：

```java
public class MainActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);   // 加载 XML 布局

        // findViewById：从布局中获取 View 引用
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);

        // 设置点击事件
        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();
            performLogin(username, password);     // 调用登录逻辑
        });
    }
}
```

### 1.3 View Binding：告别 findViewById

```java
// build.gradle.kts
android {
    buildFeatures {
        viewBinding = true   // 启用 View Binding
    }
}

// MainActivity.java → 自动生成 ActivityMainBinding 类
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;  // 自动生成的绑定类

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 直接访问，类型安全！不再需要 findViewById
        binding.loginButton.setOnClickListener(v -> {
            String username = binding.usernameInput.getText().toString();
            String password = binding.passwordInput.getText().toString();
            performLogin(username, password);
        });
    }
}
```

---

## 2. 五大经典布局

### 2.1 布局对比

| 布局 | 排列方式 | 适用场景 | 性能 | 推荐度 |
|------|---------|------|:---:|:---:|
| **ConstraintLayout** | 约束关系 | 复杂界面 | ⭐⭐⭐ | ✅ 首选 |
| **LinearLayout** | 线性（水平/垂直） | 简单排列 | ⭐⭐⭐ | ✅ |
| **FrameLayout** | 层叠（堆叠） | 占位/覆盖 | ⭐⭐⭐ | ✅ |
| **RelativeLayout** | 相对位置 | 中等复杂 | ⭐⭐ | ❌ (用 ConstraintLayout) |
| **TableLayout** | 表格 | 表格式 | ⭐ | ❌ (用 RecyclerView) |

### 2.2 ConstraintLayout：现代布局首选

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- 标题居中，顶部对齐 -->
    <TextView
        android:id="@+id/title"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="欢迎回来"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="32dp" />

    <!-- 输入框在标题下方 -->
    <EditText
        android:id="@+id/email"
        android:layout_width="0dp"           <!-- 0dp = match_constraint -->
        android:layout_height="wrap_content"
        android:hint="邮箱"
        app:layout_constraintTop_toBottomOf="@id/title"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_margin="16dp" />

    <!-- 登录按钮在输入框下方 -->
    <Button
        android:id="@+id/loginBtn"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="登录"
        app:layout_constraintTop_toBottomOf="@id/email"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_margin="16dp" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

> 💡 **为什么 ConstraintLayout 是首选**：扁平化视图树（无嵌套），性能最佳；支持百分比、角度、链、引导线等高级特性。

---

## 3. RecyclerView：列表的王者

### 3.1 Adapter 模式

```text
RecyclerView = Android 的列表框架，适配器模式

      数据源 (List<Item>)
           │
    ┌──────▼──────┐
    │   Adapter   │  ← 适配器：数据 → View 的转换器
    │  (你写的)    │
    └──────┬──────┘
           │
    ┌──────▼──────┐
    │ LayoutManager│ ← 布局管理器：决定排列方式
    │ (线性/网格/瀑布)│
    └──────┬──────┘
           │
    ┌──────▼──────┐
    │ RecyclerView │ ← 实际显示的 View
    └─────────────┘
```

### 3.2 RecyclerView 完整示例

```java
// 1. 数据模型
public class User {
    public String name;
    public String email;
    public int avatarResId;
}

// 2. 布局文件 (item_user.xml)
//    一个 ConstraintLayout 包含 ImageView + 两个 TextView

// 3. ViewHolder：持有单个 item 的 View 引用
public class UserViewHolder extends RecyclerView.ViewHolder {
    ImageView avatar;
    TextView nameText, emailText;

    public UserViewHolder(View itemView) {
        super(itemView);
        avatar = itemView.findViewById(R.id.avatar);
        nameText = itemView.findViewById(R.id.nameText);
        emailText = itemView.findViewById(R.id.emailText);
    }
}

// 4. Adapter：数据 → View
public class UserAdapter extends RecyclerView.Adapter<UserViewHolder> {
    private List<User> users;

    public UserAdapter(List<User> users) {
        this.users = users;
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.nameText.setText(user.name);
        holder.emailText.setText(user.email);
        holder.avatar.setImageResource(user.avatarResId);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }
}

// 5. 在 Activity/Fragment 中使用
RecyclerView recyclerView = findViewById(R.id.recyclerView);
recyclerView.setLayoutManager(new LinearLayoutManager(this));
UserAdapter adapter = new UserAdapter(userList);
recyclerView.setAdapter(adapter);
```

### 3.3 RecyclerView vs ListView

| 维度 | ListView | RecyclerView |
|------|---------|-------------|
| **ViewHolder 模式** | 手动实现 | **强制内置** |
| **布局管理** | 仅垂直列表 | 线性/网格/瀑布/自定义 |
| **动画** | ❌ 需手动 | ✅ ItemAnimator 内置 |
| **Item 装饰** | ❌ | ✅ ItemDecoration |
| **性能** | 一般 | **优秀**（缓存池优化） |
| **状态** | 2026 年已废弃 | ✅ 唯一推荐 |

---

## 4. Jetpack Compose：声明式 UI 新范式

### 4.1 声明式 vs 命令式

```text
命令式 (View + XML)：描述 "怎么做"
  textView.setText("Hello");
  textView.setTextSize(24);
  button.setEnabled(false);

声明式 (Compose)：描述 "是什么"
  Text(text = "Hello", fontSize = 24.sp)
  Button(onClick = { }, enabled = false) { Text("Click") }
```

### 4.2 Compose 核心组件

```kotlin
// Compose 使用 Kotlin DSL 编写，强烈推荐用 Kotlin
// 如果你只会 Java，Compose 是学习 Kotlin 的绝佳动力

@Composable
fun LoginScreen() {
    // 状态管理：类似 React 的 useState
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // 标题
        Text(
            text = "欢迎登录",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 用户名输入
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 密码输入
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 登录按钮
        Button(
            onClick = { performLogin(username, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("登录")
        }
    }
}

// 在 Activity 中使用
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAppTheme {     // MaterialTheme 包装
                LoginScreen()
            }
        }
    }
}
```

### 4.3 Compose 常用组件速查

| 分类 | Compose 组件 | View 对应 | 说明 |
|------|------------|-----------|------|
| **布局** | `Column` / `Row` / `Box` | `LinearLayout` / `FrameLayout` | 线性/层叠 |
| **文本** | `Text()` | `TextView` | 文本显示 |
| **输入** | `TextField()` / `OutlinedTextField()` | `EditText` | 文本输入 |
| **按钮** | `Button()` / `TextButton()` / `IconButton()` | `Button` | 各种按钮 |
| **图片** | `Image()` | `ImageView` | 图片显示 |
| **列表** | `LazyColumn()` / `LazyRow()` | `RecyclerView` | 高性能列表 |
| **卡片** | `Card()` | `CardView` | 卡片容器 |
| **进度** | `LinearProgressIndicator()` / `CircularProgressIndicator()` | `ProgressBar` | 进度条 |
| **弹窗** | `AlertDialog()` / `ModalBottomSheet()` | `AlertDialog` | 对话框 |
| **导航** | `NavigationBar()` / `NavigationRail()` | `BottomNavigationView` | 导航栏 |
| **开关** | `Switch()` / `Checkbox()` | `Switch` / `CheckBox` | 开关/勾选 |
| **下拉** | `DropdownMenu()` | `PopupMenu` / `Spinner` | 下拉菜单 |

### 4.4 View 体系与 Compose 对比

| 维度 | View (XML) | Jetpack Compose |
|------|-----------|-----------------|
| **编写方式** | XML + Java/Kotlin 分离 | Kotlin DSL 单一文件 |
| **UI 更新** | 手动 findViewById + setText | 自动重组（Recomposition） |
| **状态管理** | 手动同步 | `remember` / `mutableStateOf` |
| **复用性** | `<include>` / Fragment | `@Composable` 函数 |
| **预览** | Design 编辑器 | `@Preview` 注解 |
| **学习曲线** | 较低（直观 XML） | 中等（需函数式思维） |
| **性能** | 基础好 | `remember` 跳过不必要重组 |
| **推荐度** | 维护旧项目 | ✅ **新项目首选** |

> 🎯 **核心建议**：2026 年新项目 **优先选 Jetpack Compose**。如果是维护传统项目（Java + XML），保持 View 体系即可。

---

## 5. Material Design 3

### 5.1 Material 3 核心概念

```text
Material Design 3 (Material You) → Android 12+ 默认设计语言

核心特性：
├── Dynamic Color (动态取色) → 从壁纸提取色调
├── 圆角更大 → 感性化设计
├── 新组件：NavigationBar、SearchBar、TopAppBar...
├── 色调系统：Primary/Secondary/Tertiary/Error + Surface 变体
└── 字体系统升级 → Display/Headline/Title/Label/Body
```

### 5.2 主题系统

```xml
<!-- res/values/themes.xml -->
<resources>
    <style name="Theme.MyApp" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- 主色调 -->
        <item name="colorPrimary">@color/md_theme_primary</item>
        <item name="colorOnPrimary">@color/md_theme_on_primary</item>
        <!-- 容器色 -->
        <item name="colorPrimaryContainer">@color/md_theme_primary_container</item>
        <!-- Surface 系列 -->
        <item name="colorSurface">@color/md_theme_surface</item>
        <item name="colorError">@color/md_theme_error</item>
    </style>
</resources>
```

### 5.3 Material 3 常用组件

| 组件 | 用途 | 替代的旧组件 |
|------|------|-------------|
| **TopAppBar** | 顶部应用栏 | ActionBar / Toolbar |
| **NavigationBar** | 底部导航 (3-5 项) | BottomNavigationView |
| **NavigationRail** | 侧边导航 (平板) | 新增 |
| **FloatingActionButton** | 主要操作按钮 | 同名（样式升级） |
| **Chip** | 标签/筛选项 | Chip（样式升级） |
| **Card** | 卡片容器 | CardView（样式升级） |
| **SearchBar** | 搜索栏 | SearchView（体验升级） |
| **TimePicker / DatePicker** | 时间/日期选择 | 同名（对话框→内联） |

---

## 6. 资源系统

### 6.1 资源目录结构

```text
res/
├── layout/            ← XML 布局文件
├── drawable/          ← 图片 (PNG/WebP/VectorDrawable/Shape)
│   ├── ic_logo.png        ← 位图（多分辨率放不同 mipmap 目录）
│   ├── bg_gradient.xml    ← Shape Drawable (纯代码画的形状)
│   └── ic_check.xml       ← Vector Drawable (矢量图标)
├── mipmap/            ← 应用图标 (自动选择设备分辨率)
│   ├── mipmap-mdpi/   (48×48)
│   ├── mipmap-hdpi/   (72×72)
│   ├── mipmap-xhdpi/  (96×96)
│   ├── mipmap-xxhdpi/ (144×144)
│   └── mipmap-xxxhdpi/(192×192)
├── values/            ← 值资源
│   ├── strings.xml         ← 字符串（支持多语言）
│   ├── colors.xml          ← 颜色定义
│   ├── dimens.xml          ← 尺寸定义
│   ├── themes.xml          ← 主题
│   └── styles.xml          ← 样式
├── font/              ← 字体文件 (.ttf/.otf)
├── anim/              ← 属性动画
├── animator/          ← Animator 动画
├── xml/               ← 任意 XML 配置（如 FileProvider 路径）
└── raw/               ← 原始文件（如 JSON 配置文件）
```

### 6.2 资源使用

```java
// Java 中引用资源
String appName = getString(R.string.app_name);           // 字符串
int color = getColor(R.color.md_theme_primary);          // 颜色
float dimen = getResources().getDimension(R.dimen.margin); // 尺寸
Drawable icon = getDrawable(R.drawable.ic_check);        // 图片

// XML 中引用资源
// @string/app_name       → 引用字符串
// @color/md_theme_primary → 引用颜色
// @dimen/margin          → 引用尺寸
// ?attr/colorPrimary     → 引用主题中的属性（动态取色）
```

### 6.3 屏幕适配

```text
Android 屏幕适配策略：

1. dp (Density-independent Pixel)  ← 始终用 dp，不用 px！
   1 dp = 1px @ 160dpi 屏幕
   1 dp ≈ 2px @ 320dpi (hdpi)
   1 dp ≈ 3px @ 480dpi (xxhdpi)

2. sp (Scale-independent Pixel)   ← 字体用 sp
   与 dp 类似，但会受用户字体大小设置影响

3. 多布局适配
   res/layout/              ← 默认（手机竖屏）
   res/layout-land/         ← 横屏
   res/layout-sw600dp/      ← 最小宽度 ≥ 600dp（平板）
   res/layout-sw840dp/      ← 大平板

4. ConstraintLayout 百分比约束
   app:layout_constraintWidth_percent="0.5"  ← 占 50% 宽
```

---

**上一模块**：[03-Android四大组件详解](./03-Android四大组件详解.md) ｜ **下一模块**：[05-Android数据存储与网络通信](./05-Android数据存储与网络通信.md) ｜ **返回总览**：[00-Android知识体系总览](./00-Android知识体系总览.md)

---

*创建于：2026年7月*
