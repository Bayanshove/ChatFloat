# ChatFloat 🫧

**Android 悬浮窗 AI 聊天翻译工具**

在 TikTok 等社交软件上与外国人聊天时，一键复制消息 → AI 翻译成繁体中文 → 生成地道英文回复。

## ✨ 核心功能

- **悬浮球 + 悬浮面板**：系统层悬浮窗，可覆盖在其他应用上操作
- **复制即翻译**：长按对方消息 → 复制 → 点悬浮球 → 自动读取剪贴板翻译
- **AI 回复生成**：输入中文，生成地道英文（带语气与情绪）
- **自定义 API**：支持任何 OpenAI 兼容 API（DeepSeek / Claude / 本地模型）
- **AI 思考过程可视化**：展示 `reasoning_content` 思考链与结果分区
- **详细日志系统**：UTC+8 时间戳，可开关、可导出到 Download
- **多语言支持**：系统默认 / English / 简体中文 / 繁体中文 (TW)
- **首次启动欢迎页**：简洁品牌化介绍
- **翻译协议**：【译】翻译 / 【回】回复，指令前缀明确识别

## 🔧 使用流程

1. 填入 API 地址 + Key，点「🔄 拉取」选模型，保存
2. 点「🫧 启动悬浮球」→ 允许「显示在其他应用上层」
3. TikTok 长按对方消息 → 复制 → 点悬浮球 → 自动翻译
4. 「我的回复」输入中文 → 生成地道英文 → 复制粘贴

## 🌐 语言切换

设置页顶部有语言下拉框，可即时切换：
- 系统默认 / System Default
- English
- 简体中文
- 繁体中文 (TW)

## 🔒 隐私

- API Key 仅保存在本机 SharedPreferences
- 日志文件存在应用私有目录，手动导出到 Download

## 📝 开发说明

- **语言**：Java（无 Kotlin 依赖）
- **构建**：Gradle 8.5 + AGP 8.2.0 + JDK 17
- **部署**：覆盖安装保留配置
- **APK 大小**：~72KB（极小）

## 📦 构建

```bash
gradle assembleDebug --no-daemon -Dorg.gradle.vfs.watch=false
# 输出: app/build/outputs/apk/debug/app-debug.apk
```

## 📄 License

MIT
