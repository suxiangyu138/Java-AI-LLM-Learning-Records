# Vibe Coding 程式碼重構技巧

> 避免 AI 生成混亂的義大利麵程式碼



你好，我是魚皮。

你可能遇到過這樣的情況：剛開始用 AI 做專案時，程式碼清晰簡潔，看起來很舒服。但隨著功能越加越多，程式碼開始變得混亂。最後，你自己都不敢動這些程式碼了，因為改一個地方，可能會影響到其他地方。

這種情況在 Vibe Coding 中特別常見，因為 AI 可能只關注 “能不能跑”，忽略了 “好不好維護”。下面我就來教你如何識別和償還技術債，讓你的程式碼始終保持優雅。




## 一、什麼是技術債？

技術債是一個很形象的比喻。

想像一下，你要蓋一棟房子。為了快速完工，你用了一些臨時的方案：地基沒打牢，牆沒砌直，電線隨便拉。房子是蓋起來了，也能住人，但隱患很多。如果不及時修復，以後問題會越來越大，修復的成本也會越來越高。

技術債就是這樣。為了快速實現功能，你（或 AI）採用了一些不夠好的方案。這些方案當時能用，但會給未來埋下隱患。隨著專案發展，這些隱患會變成實際的問題，拖慢你的開發速度。

在今年的研究中發現，AI 生成的程式碼特別容易產生技術債。因為 AI 擅長快速實現功能，但不擅長考慮長遠的架構和可維護性。它會給你一個 “高度功能化但系統性缺乏架構判斷” 的程式碼。



### 技術債的表現

那麼，如何判斷你的專案有沒有技術債呢？最明顯的信號是：改程式碼變得越來越困難，你開始害怕修改程式碼，因為不知道會影響到哪裡。如果你發現自己經常覺得 “這個地方不敢動”、“改了這裡可能會影響那裡”，那就說明技術債已經比較嚴重了。



### 技術債的危害

技術債的危害是累積的。剛開始可能只是程式碼有點亂，不影響功能。但隨著時間推移，問題會越來越嚴重。

- 開發速度變慢，因為要花更多時間理解和修改程式碼。
- Bug 越來越多，因為程式碼太複雜，容易出錯。
- 新功能難以添加，因為現有架構不支持。
- 團隊協作困難，因為沒人能完全理解程式碼。

最糟糕的是，到了某個臨界點，你可能不得不重寫整個專案。這就是技術債的 “破產”。




## 二、AI 生成程式碼的常見問題

用 AI 做 Vibe Coding 時，如果上下文管理不當、需求描述不夠清晰，或者讓 AI 一次性實現太複雜的功能，生成的程式碼可能會出現一些質量問題。下面是幾個典型場景，了解它們能幫你更好地引導 AI。



### 過度嵌套

AI 為了確保程式碼能運行，有時會生成嵌套很深的程式碼。

什麼是嵌套？

就是 if 裡面套 if，循環裡面套循環，像俄羅斯套娃一樣。比如：

```typescript
function processData(data: any) {
  if (data) {
    if (data.items) {
      if (data.items.length > 0) {
        data.items.forEach(item => {
          if (item.active) {
            if (item.price > 0) {
              // 實際邏輯
            }
          }
        });
      }
    }
  }
}
```

這種程式碼很難讀，也很難維護。更好的寫法是提前返回：

```typescript
function processData(data: any) {
  if (!data?.items?.length) return;
  
  const activeItems = data.items.filter(item => 
    item.active && item.price > 0
  );
  
  activeItems.forEach(item => {
    // 實際邏輯
  });
}
```

顯然，第二種寫法更清晰，也更容易理解。



### 重複程式碼

AI 可能不會主動複用程式碼，而是為每個需求生成新的程式碼。

舉個例子，假設你讓 AI 分別實現用戶列表頁、文章列表頁、評論列表頁。AI 會給你三套幾乎一樣的程式碼，只是資料欄位不同。或者你可能在多個組件裡都有這樣的程式碼：

```typescript
const handleSubmit = async () => {
  setLoading(true);
  try {
    const response = await fetch('/api/data', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    const result = await response.json();
    setData(result);
  } catch (error) {
    console.error(error);
  } finally {
    setLoading(false);
  }
};
```

這些重複程式碼應該提取成一個公共函數或自訂 Hook。



### 缺少抽象

AI 傾向於寫具體的、直接的程式碼，而不是抽象的、可複用的程式碼。

比如，你要顯示用戶列表和文章列表，AI 可能會給你兩個完全獨立的組件，即使它們的結構幾乎一樣。

更好的做法是創建一個通用的列表組件，然後用不同的資料和渲染函數來複用。



### 命名隨意

AI 有時候會用相對隨意的命名，比如 `data`、`result`、`temp`、`handleClick`。這些名字不能準確表達意圖，會讓程式碼難以理解。

出現這種情況可能是因為你的需求描述不夠具體，AI 不知道這個變數或函數的真實用途。

好的命名應該是 **見名知意** 的，比如 `userData`、`apiResponse`、`temporaryBuffer`、`handleLoginButtonClick`。

如果你發現 AI 生成的命名太隨意，可以在需求中明確說明：請使用有意義的變數名和函數名，能清楚表達它們的用途。



## 三、如何利用 AI 重構程式碼？

既然 AI 會產生技術債，那能不能用 AI 來償還技術債呢？

答案是可以的。這也是 Vibe Coding 的一大優勢 —— **AI 既能快速寫程式碼，也能快速改程式碼**。



### 讓 AI 識別問題

你可以把程式碼貼給 AI，讓它從專業的角度審查，幫你找出程式碼中的問題。

````markdown
請審查這段程式碼，找出可以改進的地方：
```typescript
【貼上你的程式碼】
```

請從以下角度分析：
1. 有沒有重複程式碼？
2. 函數是否太長？
3. 命名是否清晰？
4. 有沒有過度嵌套？
5. 能否提取公共邏輯？
````

AI 會給你一份詳細的分析報告。



### 讓 AI 提供重構方案

找到問題後，讓 AI 給你重構方案，比如：

- 你提到了這段程式碼有重複邏輯。請給我一個重構方案，把重複的部分提取成公共函數。
- 這個函數太長了。請幫我把它拆分成幾個小函數，每個函數只做一件事。

AI 會給你具體的重構程式碼。



### 小步重構

注意，不建議一次性重構整個專案，這樣風險太大，說不定重構完你整個專案都無法運行了。

正確的做法是小步重構，每次只改一小部分。

比如，你發現一個函數太長了，不要一次性把它拆成 10 個小函數。先拆成 2 個，測試通過了，再繼續拆。每一步都要確保功能不變，測試都通過。

這樣即使出了問題，也容易回退。




### 重構的時機

什麼時候應該重構？

我的建議是：

1）不要專門安排時間重構，而是在日常開發中隨時重構。當你發現程式碼有問題時，立刻改掉，不要拖到以後。

2）完成功能後重構。功能做完了，測試通過了，花 10 ~ 15 分鐘看看程式碼，有沒有可以改進的地方。

3）添加新功能前重構。如果你發現現有程式碼不適合添加新功能，先重構一下，讓程式碼更容易擴展。

4）定期集中重構。每個月或每個大版本，花半天時間集中重構，處理累積的技術債。



## 四、模組化和程式碼複用

模組化是避免技術債的關鍵。而且，模組化的程式碼對 AI 也更友好 —— 當你需要修改某個功能時，AI 只需要閱讀相關的小模組，而不是整個幾百行的大檔案，這樣 AI 能更準確地理解和修改程式碼。




### 什麼是模組化？

模組化就是把程式碼分成獨立的、可複用的模組。每個模組只做一件事，做好一件事。模組之間通過清晰的接口通信，互不干擾。

好的模組化有這些特點：

- 高內聚：模組內部的程式碼緊密相關
- 低耦合：模組之間的依賴盡可能少
- 單一職責：每個模組只負責一件事
- 接口清晰：模組的輸入輸出明確




### 組件的拆分

在前端開發框架 React 中，組件是最基本的模組。你可以把組件理解為頁面上的一個個獨立部分，比如導航欄、搜尋框、用戶卡片等。

但很多人（包括 AI）會寫出很 “笨重” 的組件。

比如，一個用戶資料頁面，AI 可能會把所有邏輯都寫在一個組件裡：獲取資料、表單驗證、提交處理、錯誤提示…… 結果就是一個幾百行的巨型組件。

更好的做法是拆分成多個小組件：

```typescript
// 主組件，負責協調
function ProfilePage() {
  const { user, loading, error } = useUser();
  
  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage error={error} />;
  
  return (
    <div>
      <ProfileHeader user={user} />
      <ProfileForm user={user} />
    </div>
  );
}

// 子組件，各司其職
function ProfileHeader({ user }) {
  return (
    <div>
      <Avatar src={user.avatar} />
      <h1>{user.name}</h1>
    </div>
  );
}

function ProfileForm({ user }) {
  // 表單邏輯
}
```

這樣一來，每個組件都很小，很容易理解和測試。

即使你不了解前端或 React，也能理解這個思想 —— 把大的功能拆成小的、獨立的部分，每個部分只做一件事。這個原則適用於所有程式語言和框架。




### 函數的提取

當你發現一段程式碼在多個地方重複出現時，就應該提取成函數。

比如，你在多個地方都需要格式化日期：

```typescript
// 重複的程式碼
const formattedDate1 = new Date(date1).toLocaleDateString('zh-CN');
const formattedDate2 = new Date(date2).toLocaleDateString('zh-CN');
const formattedDate3 = new Date(date3).toLocaleDateString('zh-CN');

// 提取成函數
function formatDate(date: Date | string): string {
  return new Date(date).toLocaleDateString('zh-CN');
}

const formattedDate1 = formatDate(date1);
const formattedDate2 = formatDate(date2);
const formattedDate3 = formatDate(date3);
```

這樣不僅減少了重複，還讓程式碼更容易維護。如果以後要改日期格式，只需要改一個地方。



### 自訂 Hook 的使用

在前端開發框架 React 中，自訂 Hook 是複用邏輯的好方法。Hook 是一種特殊的函數，用來管理狀態和副作用。

💡 你不需要理解什麼是 Hook、組件、狀態管理這些專業術語。只需要告訴 AI：

```markdown
這段邏輯在多個地方重複了，請幫我提取成可複用的模組。
```

然後 AI 會自動幫你做好抽象和複用。

舉個例子，你在多個組件裡都需要獲取用戶資料，可以把獲取用戶資料這部分程式碼提取成 Hook，在其他地方複用：

```typescript
// 提取前：每個組件都重複這些邏輯
function ProfilePage() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    fetchUser().then(setUser).catch(setError).finally(() => setLoading(false));
  }, []);
  
  // ...
}

// 提取後：創建自訂 Hook
function useUser() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    fetchUser().then(setUser).catch(setError).finally(() => setLoading(false));
  }, []);
  
  return { user, loading, error };
}

// 使用時很簡單
function ProfilePage() {
  const { user, loading, error } = useUser();
  // ...
}
```

自訂 Hook 讓程式碼更簡潔，也更容易測試。




## 五、從玩具專案到商業產品

很多人用 AI 做出來的是玩具專案 —— 能用，但不夠專業、或者賺不了錢。

這時你可能會思考：如何把它變成可擴展的商業產品呢？



### 玩具專案的特徵

玩具專案一般有這些特徵：

- 所有程式碼都在一個檔案裡，或者檔案組織很混亂
- 沒有明確的架構，程式碼想到哪寫到哪
- 缺少錯誤處理，只考慮了正常情況
- 沒有測試，全靠手動測試
- 硬編碼很多，配置和程式碼混在一起。

這樣的專案可以用來演示或學習，但不適合作為商業產品長期維護和擴展。



### 商業產品的特徵

商業產品應該是這樣的：

- 清晰的目錄結構，一眼就能看出哪裡放什麼
- 明確的架構，比如 MVC、MVVM 或其他模式
- 完善的錯誤處理，考慮了各種異常情況
- 充足的測試，核心功能都有測試覆蓋
- 配置分離，環境變數、配置檔案和程式碼分開
- 文件完善，有 README、API 文件、註解等

從玩具到商業產品，需要你有意識地提升程式碼質量和重構。

![](https://pic.yupi.icu/1/projectpk%E5%A4%A7.jpeg)




### 重構的步驟

如何把玩具專案重構成商業產品？

我建議分步進行：

1）整理目錄結構。把程式碼按功能或類型分類，放到不同的資料夾裡。比如組件放 `components`，工具函數放 `lib`，類型定義放 `types`。

2）提取重複程式碼。找出重複的邏輯，提取成公共函數或組件。這一步能大大減少程式碼量。

3）拆分大檔案。把大的檔案拆成小的檔案，每個檔案只負責一件事。比如一個大的 `utils.ts` 可以拆成 `format.ts`、`validate.ts`、`storage.ts` 等。

4）添加類型定義。如果用 TypeScript，給所有函數和組件加上完整的類型。這能幫你發現很多潛在問題。

5）改進命名。把不清晰的變數名、函數名改成描述性的名字。這能讓程式碼更容易理解。

6）添加測試和文件。為核心功能寫測試，為專案寫 README，為複雜邏輯加註解。

以上這些步驟也都可以通過 AI 主導 + 人工校驗來完成。關鍵是每一步都要確保功能不變、順利通過測試。不要貪多，一步一步來。



## 六、實戰案例 - 重構一個混亂的專案

讓我用一個真實的例子，展示如何重構一個混亂的專案。



### 初始狀態

假設你用 AI 做了一個待辦事項應用，所有程式碼都在一個 `App.tsx` 檔案裡，大概 500 行。

```typescript
// App.tsx (500 行)
function App() {
  const [todos, setTodos] = useState([]);
  const [input, setInput] = useState('');
  const [filter, setFilter] = useState('all');
  const [loading, setLoading] = useState(false);
  
  // 100 行的資料獲取邏輯
  useEffect(() => { /* ... */ }, []);
  
  // 50 行的添加邏輯
  const handleAdd = () => { /* ... */ };
  
  // 50 行的刪除邏輯
  const handleDelete = () => { /* ... */ };
  
  // 50 行的編輯邏輯
  const handleEdit = () => { /* ... */ };
  
  // 50 行的過濾邏輯
  const filteredTodos = todos.filter(/* ... */);
  
  // 200 行的 JSX
  return (
    <div>
      {/* 很多很多程式碼 */}
    </div>
  );
}
```

雖然程式碼能用，但是所有功能邏輯都寫到一起，不利於閱讀和維護。




### 重構步驟

#### 第一步、提取自訂 Hook

首先，我們把所有和待辦事項資料相關的邏輯（獲取、添加、刪除、更新）都提取到一個獨立的 Hook 裡。這樣主組件就不用關心資料是怎麼管理的，只需要調用這些方法就行。

```typescript
// hooks/useTodos.ts
function useTodos() {
  const [todos, setTodos] = useState([]);
  const [loading, setLoading] = useState(false);
  
  const addTodo = async (text: string) => { /* ... */ };
  const deleteTodo = async (id: string) => { /* ... */ };
  const updateTodo = async (id: string, text: string) => { /* ... */ };
  
  useEffect(() => {
    // 獲取資料
  }, []);
  
  return { todos, loading, addTodo, deleteTodo, updateTodo };
}
```



#### 第二步、拆分組件

接下來，把 UI 部分拆分成多個小組件。每個組件只負責顯示和處理自己那一部分的邏輯，比如輸入框只管輸入、列表只管顯示、過濾器只管篩選。這樣每個組件都很簡單，容易理解和修改。

```typescript
// components/TodoList.tsx
function TodoList({ todos, onDelete, onEdit }) {
  return (
    <div>
      {todos.map(todo => (
        <TodoItem 
          key={todo.id} 
          todo