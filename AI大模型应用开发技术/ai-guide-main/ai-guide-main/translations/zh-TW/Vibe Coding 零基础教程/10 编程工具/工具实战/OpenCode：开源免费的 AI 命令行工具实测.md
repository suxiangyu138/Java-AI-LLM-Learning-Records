# OpenCode：開源免費的 AI 命令行工具實測

大家好，我是程式設計師魚皮。

Claude Code 一直是大家公認的 AI 編程命令行工具 Top 1，在 AI 和程式設計師圈子裡幾乎是神一般的存在。

![](https://pic.yupi.icu/1/happy-new-year-claude-coders-v0-o2quvbl99lag1.png)

但是，這狗玩意兒對中國用戶可不太友好……

首先，如果你想要使用 Claude Code，就必須要有特殊的網路 + 官方帳號，否則就會看到一片紅。

![](https://pic.yupi.icu/1/cannotuseclaude.png)

此外，2025 年 9 月，Anthropic 公司不知道抽什麼風，突然宣布 **全面禁止中國控股企業使用 Claude 服務**，不僅包括中國大陸企業，連海外中資控股超過 50% 的公司都在封禁範圍內！

甚至 Anthropic 還特別點名了中國，把咱們稱為 **敵對國家**！

![](https://pic.yupi.icu/1/image-20250905164631315.png)

天下苦 Claude Code 久矣！

但是最近我身邊很多程式設計師朋友開始從 Claude Code 轉向了另一個工具，正是突然大火的開源項目 OpenCode。

![](https://pic.yupi.icu/1/image-20260107174223010.png)

這玩意只用了半年的時間，就在 GitHub 上漲到了 5.2w Star！

這是什麼概念？比我在 GitHub 上開源的幾十個項目的總和加起來都多！慕了慕了……

![](https://pic.yupi.icu/1/opencodestarhistory.png)

OpenCode 到底是什麼？憑什麼這麼火？



## 啥是 OpenCode？

[OpenCode](https://opencode.ai/) 是一款 100% 開源的 AI 編程命令行工具，可以在 **終端、IDE、甚至桌面應用** 中使用。

![](https://pic.yupi.icu/1/screenshot.png)

你可能會問：這玩意兒跟 Claude Code 有啥區別？

試試不就知道了？

接下來我帶大家實操一下，從零開始安裝、配置、到實際寫代碼，一條龍服務~



## 從 0 開始上手 OpenCode

### 1、安裝運行 OpenCode

直接進入 OpenCode 官網，複製一行命令：

![](https://pic.yupi.icu/1/image-20260107174407894.png)

命令如下：

```bash
curl -fsSL https://opencode.ai/install | bash
```

然後在終端中執行，就可以完成安裝了。

安裝完成之後，輸入 `opencode` 進入程序，接下來你就可以愉快地使用了~

![](https://pic.yupi.icu/1/image-20260107174646918.png)

先來個經典的 Hello World，AI 成功給出了回覆。

![](https://pic.yupi.icu/1/image-20260107174755331.png)

恭喜，到這裡你已經掌握了 OpenCode 的 70% 了。



### 2、選擇模式和模型

OpenCode 支援 2 種模式，預設是 Build 模式，用來構建應用、生成代碼。

按一下 Tab 鍵，就可以切換到 Plan 模式，用於生成執行計劃。

![](https://pic.yupi.icu/1/image-20260107174952823.png)

按一下 `Ctrl + p` 鍵，可以打開命令面板，裡面有幾十個內建命令。我們先來試著切換一下大模型：

![](https://pic.yupi.icu/1/image-20260107175255527.png)

預設提供了 4 個免費模型：

![](https://pic.yupi.icu/1/image-20260107175409282.png)

好傢伙，連智譜最新的 GLM-4.7 竟然也免費？那我的 Coding Plan 套餐不是白開了？

![](https://pic.yupi.icu/1/image-20260107175513490.png)

除了免費的模型外，OpenCode 支援超多的 AI 模型，你可以自由選擇：

![](https://pic.yupi.icu/1/image-20260107175614359.png)

選中模型後，配置自己的 API Key 就好了：

![](https://pic.yupi.icu/1/image-20260107175657296.png)

如果你之前有 **Claude Pro/Max 訂閱帳號**，可以直接登入使用，無縫從 Claude Code 遷移過來。

![](https://pic.yupi.icu/1/image-20260107175745963.png)



### 3、快捷指令

OpenCode 支援斜槓命令，輸入 `/`，能看到很多操作，比如查看模型列表、查看 Agents、管理 MCP、切換主題等等：

![](https://pic.yupi.icu/1/image-20260107175926346.png)

支援幾十個不同的主題，顏值都挺高的，從這點也能看出來 OpenCode 很注重用戶的體驗：

![](https://pic.yupi.icu/1/image-20260107180108430.png)

輸入 `@` 可以快速關聯目錄文件，給 AI 添加上下文： 

![](https://pic.yupi.icu/1/image-20260107182710150.png)



### 4、互動體驗

相比於 Claude Code，OpenCode 真是把命令行的互動體驗拉滿了，甚至我覺得它是一個偽裝成命令行的桌面應用。

你可以點擊某條消息，然後會彈出一個消息動作框，你可以撤回消息和 AI 的回覆，也可以複製、或者基於當前對話新開一個對話框。

![](https://pic.yupi.icu/1/image-20260107180609525.png)

你可以通過鼠標上下滾動來切換選單，並且可以直接通過鼠標點擊進入下一步。

你可以按 `Ctrl + p` 鍵打開命令面板，然後開啟側邊欄：

![](https://pic.yupi.icu/1/image-20260107181100523.png)

然後界面就變成了這樣，你管這叫命令行？

![](https://pic.yupi.icu/1/image-20260107181218259.png)



### 5、LSP 支援

細心的你一定看到了，右邊的側邊欄有個 `LSP`，這是什麼鬼東西？老色批？

LSP（Language Server Protocol 語言伺服器協議）是微軟開發的一種通信協議，用於讓代碼編輯器和語言伺服器之間進行通信。

簡單來說，**LSP 就是讓編輯器看懂代碼的技術。**

比如你在 VS Code 裡寫代碼，輸入 `console.` 它會自動提示 `log`、點擊函數名能跳轉到代碼定義、寫錯代碼會畫紅線提醒。這些代碼編輯器的功能，背後都是 LSP 在幹活。

OpenCode 支援 LSP，意味著 AI 能真正理解你的代碼結構，而不是把代碼當普通文字瞎猜，改起來更精準。

比如我讓 AI 介紹我的 AI 答題平台項目中最有價值的代