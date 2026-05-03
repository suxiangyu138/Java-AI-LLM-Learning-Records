# Vibe Coding 安全防護技巧

> 保護你的專案和 API 密鑰



你好，我是魚皮。

很多沒有程式基礎的同學在用 AI 做專案時，完全不考慮安全問題。反正程式能跑就行，至於安全不安全，等出了問題再說。但實際上，一個安全問題可能會毀掉整個專案。

我見過有人因為 API Key 洩露，一夜之間被刷了幾千。也見過有人的資料庫被刪，所有用戶資料都沒了。至於那些大公司的專案，但凡出點兒問題，都會引起軒然大波。

這篇文章，我就來講講 Vibe Coding 中最容易忽視的安全問題，以及如何避免它們。




## 一、致命問題 - API Key 洩露

在所有安全問題中，API Key 洩露是最常見、也是最致命的之一。



### 什麼是 API Key？

API Key 就像你家的鑰匙，有了它，就能使用某個服務。比如 OpenAI 的 API Key 能讓你調用 ChatGPT，Supabase 的 API Key 能讓你訪問資料庫。

問題是，如果這個鑰匙被別人拿到了，他們就能冒充你使用這些服務。如果是付費服務，他們會花你的錢；如果是資料庫，他們可能會讀取、修改甚至刪除你的資料。




### 洩露的常見方式

API Key 最常見的洩露方式是：**直接寫在程式裡，然後上傳到 GitHub**。

很多同學可能會這樣寫調用 AI 大模型的程式：

```typescript
// ❌ 千萬不要這樣做！
const OPENAI_API_KEY = 'sb-yupi-abc123def456...';

const response = await fetch('https://xxx/v1/chat/completions', {
  headers: {
    'Authorization': `Bearer ${OPENAI_API_KEY}`
  }
});
```

然後把程式推送到 GitHub。

結果呢？

由於你的 GitHub 專案選擇了公開，任何人都能看到你的程式，也就能看到你的 API Key。更糟糕的是，有很多自動化腳本專門在 GitHub 上掃描 API Key，一旦發現就會立刻使用。

我聽說過有位老哥把 OpenAI 的 API Key 直接寫在了前端程式裡，然後別人直接從瀏覽器的開發者工具裡找到了他的 API Key，幾個小時內就被刷了上千。等他發現時，錢已經花光了。

這個教訓告訴我們：**API Key 洩露不是小事，一定要重視。**




## 二、如何正確管理敏感資訊？

既然不能把 API Key 寫在程式裡，那應該怎麼做呢？



### 使用環境變數

正確的做法是使用環境變數。

環境變數是存儲在系統或運行環境中的配置資訊，不會被包含在程式裡。

#### 第一步、建立 .env 檔案

在專案根目錄建立一個 `.env` 檔案：

```
OPENAI_API_KEY=sb-yupi-abc123def456...
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGci...
DATABASE_URL=postgresql://...
```



#### 第二步、在程式中使用

在程式裡透過 `process.env` 訪問這些變數：

```typescript
// ✅ 正確的做法
const OPENAI_API_KEY = process.env.OPENAI_API_KEY;

const response = await fetch('https://xxx/v1/chat/completions', {
  headers: {
    'Authorization': `Bearer ${OPENAI_API_KEY}`
  }
});
```



#### 第三步、添加到 .gitignore

確保 `.env` 檔案不會被上傳到 GitHub：

```
# .gitignore
.env
.env.local
.env.*.local
```



#### 第四步、建立 .env.example

為了讓其他人知道需要哪些環境變數，建立一個 `.env.example` 檔案：

```
OPENAI_API_KEY=your_openai_api_key_here
SUPABASE_URL=your_supabase_url_here
SUPABASE_ANON_KEY=your_supabase_key_here
DATABASE_URL=your_database_url_here
```

這個檔案可以上傳到 GitHub，因為它不包含真實的密鑰。



### 前端和後端的區別

這裡有一個重要的區別：**前端程式是公開的，後端程式是私密的。**

在前端（瀏覽器中運行的程式）中，即使你用了環境變數，最終這些值還是會被打包到 JavaScript 檔案裡，用戶可以透過開發者工具看到。所以，**絕對不要在前端程式中使用敏感的 API Key**！

正確的做法是：

- 敏感的 API 調用放在後端
- 前端只調用你自己的後端 API
- 後端驗證用戶身份後，再調用第三方 API

透過程式舉一些例子：

```typescript
// ❌ 不要在前端直接調用 OpenAI
// 前端程式
const response = await fetch('https://api.openai.com/v1/chat/completions', {
  headers: { 'Authorization': `Bearer ${OPENAI_API_KEY}` }
});

// ✅ 應該這樣做
// 前端程式：調用自己的後端
const response = await fetch('/api/chat', {
  method: 'POST',
  body: JSON.stringify({ message: userMessage })
});

// 後端程式：調用 OpenAI
export async function POST(request: Request) {
  const { message } = await request.json();
  
  // 在後端使用 API Key
  const response = await fetch('https://api.openai.com/v1/chat/completions', {
    headers: { 'Authorization': `Bearer ${process.env.OPENAI_API_KEY}` },
    body: JSON.stringify({ messages: [{ role: 'user', content: message }] })
  });
  
  return response;
}
```



### 使用密鑰管理服務

如果是生產環境，建議使用專門的密鑰管理服務，比如 Vercel 的環境變數管理、AWS Secrets Manager、HashiCorp Vault 等。這些服務提供了更安全的密鑰存儲和訪問控制，大公司一般會這麼做。



## 三、其他常見安全問題

除了 API Key 洩露，還有一些常見的安全問題。



### SQL 注入

SQL 注入是最經典的安全漏洞之一。如果你直接把用戶輸入拼接到 SQL 查詢裡，攻擊者可以透過特殊的輸入來執行惡意的 SQL 語句。

```typescript
// ❌ 危險：SQL 注入風險
const query = `SELECT * FROM users WHERE email = '${userEmail}'`;

// ✅ 安全：使用參數化查詢
const query = 'SELECT * FROM users WHERE email = ?';
const result = await db.execute(query, [userEmail]);
```

好在，如果你用的是 Supabase、Prisma 等現代工具，它們會自動幫你防止 SQL 注入。但如果你寫原始 SQL，一定要注意這個問題。



### XSS 攻擊

XSS（跨站腳本攻擊）是指攻擊者在你的網站上注入惡意腳本。

比如，如果你直接把用戶輸入的內容顯示在頁面上：

```typescript
// ❌ 危險：XSS 風險
function Comment({ text }) {
  return <div dangerouslySetInnerHTML={{ __html: text }} />;
}
```

攻擊者可以輸入 `<script>alert('魚皮是狗')</script>`，這段腳本就會在其他用戶的瀏覽器中執行。

![](https://pic.yupi.icu/1/image-20260105155517608.png)

正確的做法是：

```typescript
// ✅ 安全：React 會自動轉義
function Comment({ text }) {
  return <div>{text}</div>;
}
```

React 預設會轉義所有內容，除非你用 `dangerouslySetInnerHTML`。所以，**除非必要，否則不要使用 `dangerouslySetInnerHTML`**。



### CSRF 攻擊

CSRF（跨站請求偽造）是指攻擊者誘導用戶在已登錄的網站上執行非預期的操作。

比如你登錄了銀行網站，然後在另一個標籤頁打開了一個惡意網站。這個惡意網站裡有一段程式，會自動向銀行網站發送轉賬請求。因為你還在登錄狀態，銀行網站會認為這是你本人的操作，就執行了轉賬。這就是 CSRF 攻擊。

防禦 CSRF 有 3 種常用方法：

1）使用 CSRF Token：伺服器生成一個隨機令牌，每次表單提交時都要帶上這個令牌，伺服器驗證令牌是否正確。

2）使用 SameSite Cookie 屬性：設置 Cookie 的 SameSite 屬性，讓瀏覽器只在同站請求時發送 Cookie。

3）驗證請求的 Referer 頭：檢查請求是從哪個網站發起的，如果不是自己的網站就拒絕。

如果你用的是 Next.js、Nuxt.js 等現代框架，它們一般會自動處理 CSRF 防護。




### 身份驗證和授權

不要相信前端的任何驗證！

**前端驗證只是為了用戶體驗，真正的驗證必須在後端做。**

舉一些程式例子：

```typescript
// ❌ 不安全：只在前端檢查
function AdminPanel() {
  const isAdmin = localStorage.getItem('isAdmin') === 'true';
  if (!isAdmin) return <div>無權訪問</div>;
  return <div>管理面板</div>;
}

// ✅ 安全：在後端驗證
// 前端
function AdminPanel() {
  const { data, error } = useFetch('/api/admin/data');
  if (error) return <div>無權訪問</div>;
  return <div>管理面板</div>;
}

// 後端
export async function GET(request: Request) {
  const user = await verifyToken(request);
  if (!user.isAdmin) {
    return new Response('Forbidden', { status: 403 });
  }
  // 返回資料
}
```



### 依賴包的安全

如果你的專案用了很多第三方包，這些包也可能有安全漏洞。

城門失火殃及池魚，建議定期運行 `npm audit` 命令檢查漏洞。

如果發現漏洞，運行下列命令，它會自動更新有漏洞的包。

```bash
npm audit fix
```

對於無法自動修復的漏洞，要手動檢查並決定是否需要更換包。



## 四、安全檢查清單

每次發布專案前，建議結合 AI + 人工過一遍這個清單。




### 密鑰和敏感資訊

- [ ] 所有 API Key 都使用環境變數
- [ ] .env 檔案已添加到 .gitignore
- [ ] 建立了 .env.example 檔案
- [ ] 敏感的 API 調用都在後端進行
- [ ] 沒有在前端程式中暴露密鑰
- [ ] 生產環境的密鑰和開發環境不同


### 用戶輸入驗證

- [ ] 所有用戶輸入都經過驗證
- [ ] 前端和後端都做了驗證
- [ ] 使用了參數化查詢，防止 SQL 注入
- [ ] 避免使用 dangerouslySetInnerHTML
- [ ] 對用戶上傳的檔案進行了類型和大小檢查


### 身份驗證和授權

- [ ] 使用了安全的身份驗證方案（如 JWT、OAuth）
- [ ] 密碼經過加密存儲（使用 bcrypt 等）
- [ ] 敏感操作需要重新驗證
- [ ] 實現了權限控制，不同用戶有不同權限
- [ ] Session 有過期時間


### HTTPS 和傳輸安全

- [ ] 生產環境使用 HTTPS
- [ ] Cookie 設置了 Secure 和 HttpOnly 標誌
- [ ] 使用了 SameSite Cookie 防止 CSRF
- [ ] 敏感資料傳輸時加密


### 依賴和第三方服務

- [ ] 定期運行 npm audit 檢查漏洞
- [ ] 只使用可信的第三方包
- [ ] 定期更新依賴包
- [ ] 審查第三方包的權限


### 錯誤處理和日誌

- [ ] 錯誤資訊不暴露敏感資訊
- [ ] 生產環境不顯示詳細的錯誤堆棧
- [ ] 日誌不記錄密碼等敏感資訊
- [ ] 實現了錯誤監控和告警



## 五、讓 AI 幫你做安全檢查

毫無疑問，AI 也能幫你發現並修復安全問題。

你可以讓 AI 幫你審查程式的安全性：

```markdown
請從安全角度審查這段程式，找出潛在的安全問題：
【貼上你的程式】
重點檢查：
1. 有沒有 SQL 注入風險？
2. 有沒有 XSS 攻擊風險？
3. 用戶輸入是否經過驗證？
4. 敏感資訊是否暴露？
5. 錯誤處理是否安全？
```

AI 會給你詳細的安全分析。

發現問題後，讓 AI 幫你修復：

```markdown
你提到了這段程式有 SQL 注入風險。請給我一個安全的實現方案，使用參數化查詢。
這裡的用戶輸入沒有驗證。請添加驗證邏輯，確保郵箱格式正確，密碼長度至少 8 位。
```

但要注意，**不要完全依賴 AI 的安全建議**。AI 可能會遺漏一些問題，或者給出不夠安全的方案。你要結合自己的判斷，必要時查閱官方文件或者找多個 AI 大模型確認。




### 使用安全掃描工具

除了 AI，還可以使用專門的安全掃描工具：

- Snyk：掃描依賴包的漏洞
- SonarQube：程式質量和安全分析
- OWASP ZAP：Web 應用安全測試

![](https://pic.yupi.icu/1/evo-by-snyk-hp-image_j0acql.png)

這些工具能自動發現很多安全問題。



## 六、安全開發的習慣

安全不是一次性的工作，而是要養成習慣，時刻銘記著。



### 最小權限原則

給每個用戶、每個服務只分配必要的權限，不要給多餘的權限。

比如，如果一個 API Key 只需要讀取資料，就不要給它寫入權限。如果一個用戶只是普通用戶，就不要給他管理員權限。

這樣，即使某個密鑰或帳戶被盜，損失也會小一些。




### 定期輪換密鑰

不要一個 API Key 用到天荒地老。定期更換密鑰，比如每 3 個月或每 6 個月換一次。

大多數服務都支援建立多個 API Key。你可以先建立新的 Key，更新到專案中，確認沒問題後，再刪除舊的 Key。



### 監控異常活動

很多 API 服務都提供了使用量監控和告警功能，記得開啟，及時發現異常。

如果你的 API 調用量突然暴增，可能是 Key 被盜用了。如果有人嘗試多次登錄失敗，可能是在暴力破解密碼。



### 保持更新

安全漏洞會不斷被發現，軟體包也會不斷更新修復漏洞。定期更新你的依賴包，關注安全公告，及時修復已知的安全問題。



### 備份資料

即使做了所有防護，還是可能出問題。建議定期備份資料，能讓你在最壞的情況下也能恢復。

如果你使用了 Supabase 等第三方後端服務，可能會自動備份。如果是自己的資料庫，要設置定期備份。



## 寫在最後

安全問題往往是最容易被忽視的，因為它不像功能或性能那樣直觀。但一旦出了安全問題，後果可能是災難性的。

最後總結一下本文的要點：

1. API Key 洩露是最大的風險：絕對不要把 API Key 寫在程式裡，要使用環境變數。
2. 前端和後端要區分：敏感的操作必須在後端進行，不要相信前端的任何驗證。
3. 了解常見漏洞：SQL 注入、XSS、CSRF 等都是要防範的。
4. 使用安全檢查清單：每次發布前過一遍清單，確保沒有遺漏。
5. 養成安全習慣：最小權限、定期輪換密鑰、監控異常、保持更新、備份資料。
6. 善用工具：AI 和安全掃描工具能幫你發現問題，但不要完全依賴它們。

安全是一個持續的過程，不是一勞永逸的。保持警惕，定期檢查，才能保護好你的專案和用戶。

希望這些安全防護技巧能幫你避免常見的安全問題，讓你的 Vibe Coding 專案更加安全可靠。

學習辛苦了，給自己加個雞腿 🍗，吃完就出發！




## 推薦資源

1）魚皮 AI 導航網站：[AI 資源大全、最新 AI 資訊、免費 AI 教程](https://ai.codefather.cn)

2）編程導航學習圈：[學習路線、編程教程、實戰專案、求職寶典、交流答疑](https://www.codefather.cn)

3）程式員面試八股文：[實習/校招/社招高頻考點、企業真題解析](https://www.mianshiya.com)

4）程式員寫履歷神器：[專業模板、豐富例句、直通面試](https://www.laoyujianli.com)

5）1 對 1 模擬面試：[實習/校招/社招面試拿 Offer 必備](https://ai.mianshiya.com)