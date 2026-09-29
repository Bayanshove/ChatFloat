# ChatFloat

Android 懸浮窗 AI 翻譯 / 覆訊 App，支援自訂 OpenAI 相容 API，專為 TikTok 聊天場景設計。

## 功能

- 🫧 系統級懸浮小氣泡，可拖曳、點開面板
- 📋 手動讀取剪貼板內容並翻譯（不自動讀取，避免二進制亂碼崩潰）
- 💭 顯示 AI 思考過程（reasoning_content），獨立卡片展示
- 📝 翻譯結果 + 俚語解釋卡片
- 💬 覆訊生成 + 一鍵複製
- 📝 日誌記錄（App 內開關、導出到 Download、清除）
- 🌐 多語言介面（系統預設 / English / 簡體中文 / 繁體中文）
- 🛡️ 輸入過濾：自動移除控制字符、限制長度，防止二進制數據崩潰

## 目錄結構

```
ChatFloat/
├── app/src/main/java/com/hwcloud/chatfloat/
│   ├── WelcomeActivity.java        # 歡迎頁 + 語言選擇
│   ├── MainActivity.java           # 主界面
│   ├── SettingsActivity.java       # 設定頁
│   ├── BubbleService.java          # 懸浮窗服務（拆分為 part1 + part2）
│   ├── BubbleService_part1.java    # 懸浮窗服務（上半部）
│   ├── BubbleService_part2.java    # 懸浮窗服務（下半部）
│   ├── ApiClient.java             # OpenAI API 呼叫 + reasoning 解析
│   ├── LanguageUtils.java         # 多語言工具
│   ├── Logger.java                # 日誌工具
│   └── ... (其他 Activity)
├── app/src/main/res/
│   └── values/...                 # 多語言資源（en / zh-rCN / zh-rTW）
└── build/merge_bubbleservice.sh   # 合併腳本
```

## 建置

### 1. 合併 BubbleService.java

由於文件大小限制，`BubbleService.java` 被拆分為兩個部分：

```bash
cd app/src/main/java/com/hwcloud/chatfloat
cat BubbleService_part1.java BubbleService_part2.java > BubbleService.java
```

或執行合併腳本：

```bash
./build/merge_bubbleservice.sh
```

### 2. 安裝 Gradle Wrapper

```bash
git clone --depth 1 https://github.com/gradle/gradle.git /tmp/gradle
cp -r /tmp/gradle/gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.jar
chmod +x gradlew
```

### 3. 建置 APK

```bash
./gradlew assembleDebug
```

輸出：`app/build/outputs/apk/debug/app-debug.apk`

## 安裝

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

授權懸浮窗權限 → 設定 API（base URL / key / model）→ 開始使用。

## 授權

MIT License