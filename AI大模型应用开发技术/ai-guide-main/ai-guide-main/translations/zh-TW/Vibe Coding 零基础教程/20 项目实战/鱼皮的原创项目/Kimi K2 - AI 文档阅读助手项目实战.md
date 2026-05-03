# Kimi K2 - AI 文件閱讀助手專案實戰

本專案是一個 AI 文件閱讀助手網站，可以幫你快速讀懂各種複雜的文件（論文、技術文件、PDF 等），還能幫你管理文件。

專案包含完整的前端和後端，全程通過和 AI 對話完成開發，不寫一行程式碼，適合想快速實踐完整 Vibe Coding 開發流程、想學習如何用 AI 開發實用工具的同學。



---



大家好，我是程式設計師魚皮。開學季到了，想必很多朋友要開始收集和閱讀論文，像我自己學習新技術知識也會去閱讀文件，我深知閱讀文件的痛苦。明明每個詞拆開都知道什麼意思，連一起就看不懂。

![](https://pic.yupi.icu/1/1757559057843-b9d37369-49bf-4eec-878a-c70ac945cbd9.png)

為了幫助大家免受文件的折磨，我用 AI 開發了個 AI 文件助手網站，可以幫你快速讀懂各種複雜的文件、還幫你管理文件。

![](https://pic.yupi.icu/1/1757561248387-205bf672-7a6c-452a-a283-698fb526601c.png)

網站完全免費，程式碼完全開源！

開源倉庫：[github.com/liyupi/literature-assistant](https://github.com/liyupi/literature-assistant)

![](https://pic.yupi.icu/1/1757829143308-d5bfdac6-847a-4061-9194-4821bdf3d3dc.png)

下面先教大家如何使用網站，再分享這個網站的製作過程，還有國內使用 Claude Code 的方法哦。

⭐️ 推薦觀看影片版，2 分鐘學會：[bilibili.com/video/BV1MnpVzdETW](https://www.bilibili.com/video/BV1MnpVzdETW/)



## 如何使用？

先下載開源程式碼到自己電腦，然後直接執行我提供的快速啟動腳本，打開網頁就能看到效果了。

💡 要確保你的電腦有 Node.js 和 Java 環境，可以參考 README.md 文件安裝。

![](https://pic.yupi.icu/1/1757567928358-ad506045-faaf-47b1-b742-190c83c94ad3.png)



當你要閱讀文件時，點擊「單個導入」按鈕，上傳文件檔案，然後需要填寫 Kimi AI 的 API Key。

![](https://pic.yupi.icu/1/1757560732751-de713284-c039-41c1-b9f4-0af34e4c703c.png)



選擇 Kimi 是因為他們剛剛發布了新版本的 K2 模型，在程式設計、推理和文件理解方面都很不錯；

而且支援 256K 的上下文，幾十萬字的論文也能搞定。

![](https://pic.yupi.icu/1/1757560219469-2eab0801-a9b3-49dd-b680-d1d27c1850cf.png)



在側重考察真實軟體工程任務的 SWE-bench Verified 等基準測試中，新版 Kimi K2 模型的表現也很不錯：

![](https://pic.yupi.icu/1/1757560161782-1c78bd3c-a3a5-42f9-b79b-a0b07d808e6b.png)



只需要登入 [Kimi 的開發控制台](https://platform.moonshot.cn/)，然後進入 API Key 管理來獲取一個呼叫大模型的密鑰。

![](https://pic.yupi.icu/1/1757560312832-cfd4158d-8c31-4c22-8b42-573551334863.png)



雖然新人有免費額度，但是不要洩露自己的密鑰哦！

![](https://pic.yupi.icu/1/1757560674974-180d8475-e83e-4b35-ba9b-2ded866aa730.png)



填寫好 API Key，就可以生成文件閱讀指南啦，生成速度非常快。

![](https://pic.yupi.icu/1/1757560771758-4c3df93e-889d-4c74-afa2-90e69347f286.png)



AI 生成的效果還是不錯的，圖文並茂，能幫你更快理解複雜的文件。

![](https://pic.yupi.icu/1/1757560820325-87111dd8-cd82-49d0-9385-24ea9a33e885.png)



你還可以批次導入多個文件，同時呼叫 AI 生成閱讀指南，提高效率。

![](https://pic.yupi.icu/1/1757560886812-1706415c-cff5-4475-872f-ba66891563f7.png)



此外，你還可以把這個網站當做自己的智能文件收藏夾，可以分類檢索已經導入的文件、下載原始檔案、隨時查看文件閱讀指南。**不要再讓自己收藏過的文件找不到啦~**

![](https://pic.yupi.icu/1/1757560925975-079ad961-7cc8-498b-99e2-84a4a534023f.png)



## 怎麼實現？

如果是以前，這種網站可能要做個好幾天。但現在 AI 程式設計技術已經很成熟了，我選用 Claude Code AI 開發工具，輕輕鬆鬆一天搞定，而且一行程式碼都不用自己寫。

首先在終端輸入一行指令來安裝 Claude Code：

```bash
npm install -g @anthropic-ai/claude-code
```

然後執行 `claude` 指令，就可以向它提問了~

結果，報錯啦！

![](https://pic.yupi.icu/1/1756451588360-37620a0b-bc2f-4ad5-adf9-4efde87f17ed.png)

**可惡啊，這破玩意還不支援國內使用！**

不過沒關係，我們可以更換為 Kimi。在終端內輸入指令來配置一段環境變數（注意區分作業系統）：

```bash
# Linux/macOS 啟動高速版 kimi-k2-turbo-preview 模型
export ANTHROPIC_BASE_URL=https://api.moonshot.cn/anthropic
export ANTHROPIC_AUTH_TOKEN=<你的 API 密鑰>
export ANTHROPIC_MODEL=kimi-k2-turbo-preview
export ANTHROPIC_SMALL_FAST_MODEL=kimi-k2-turbo-preview

# Windows Powershell 啟動高速版 kimi-k2-turbo-preview 模型
$env:ANTHROPIC_BASE_URL="https://api.moonshot.cn/anthropic";
$env:ANTHROPIC_AUTH_TOKEN=<你的 API 密鑰>
$env:ANTHROPIC_MODEL="kimi-k2-turbo-preview"
$env:ANTHROPIC_SMALL_FAST_MODEL="kimi-k2-turbo-preview"
```



然後就可以愉快地使用 Claude Code 生成程式碼了~

![](https://pic.yupi.icu/1/1756451713145-29e5ba15-76b2-4848-903a-f098b942e2f9.png)



對於包含完整前後端的網站，很難用一段提示詞就讓 AI 生成出滿意的效果，因此我們需要像企業真實開發一樣 **分解工作步驟**。先後端、再前端、最後前後端對接聯調，而且最好一個一個地開發功能，出了問題及時調整。

分享一些參考的提示詞：

![](https://pic.yupi.icu/1/1757567728565-5724de3e-7863-457b-9866-368c1535bd2c.png)



------



以上就是本期分享，希望這個工具對大家有幫助，也不要忘記給魚皮三連支持，謝謝大家~

![](https://pic.yupi.icu/1/1757829315038-73ef4fd7-7fef-4fa2-859d-11bb28381933.webp)


## 推薦資源

1）魚皮 AI 導航網站：[AI 資源大全、最新 AI 資訊、免費 AI 教程](https://ai.codefather.cn)

2）程式設計導航學習圈：[學習路線、程式設計教程、實戰專案、求職寶典、交流答疑](https://www.codefather.cn)

3）程式設計師面試八股文：[實習/校招/社招高頻考點、企業真題解析](https://www.mianshiya.com)

4）程式設計師寫履歷神器：[專業模板、豐富例句、直通面試](https://www.laoyujianli.com)

5）1 對 1 模擬面試：[實習/校招/社招面試拿 Offer 必備](https://ai.mianshiya.com)