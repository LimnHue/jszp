# 上岸备课 Android

面向教师招聘备考的原生 Android 应用。UI 根据 `output/ui-mockups` 中的设计图实现，包含试讲、结构化、模板和设置四个业务模块。

## 技术栈

- Kotlin 1.9.24
- Jetpack Compose + Material 3
- Kotlin Serialization
- 单 Activity + ViewModel + Repository
- 版本化 JSON 本地仓库

## 主要能力

- 学段与学科自由组合，每个组合对应独立题库。
- 试讲按教材、题材检索，内容分为课程信息、试讲主体、板书图片。
- 结构化按问题种类检索，支持随机换题、答题思路和计时练习。
- 模板按种类与关键词检索，支持随机抽取和收藏。
- `.md`、`.txt`、`.docx` 文档导入，按 Markdown 标题或自定义章节名自动切分。
- 教材、题材、问题种类、模板种类和内容结构均可在设置中增删。
- 倒计时/正计时、默认练习时长、五套配色与卡片透明度可配置。
- 当前题库或完整备考库可导出为 JSON，并通过 Android 分享面板发送。

## 目录

```text
app/src/main/java/com/shangan/teacherprep/
├── data/       # 数据模型、示例数据、持久化与导入导出
├── feature/    # 各业务页面
├── ui/         # 通用 Compose 组件与主题
├── util/       # 文档解析等无界面工具
├── AppViewModel.kt
└── MainActivity.kt
```

新增字段时优先给序列化模型提供默认值，以保持旧备考库兼容。备考库文件带有 `schemaVersion`，后续出现破坏性结构变化时，应在 `AppRepository.load()` 中增加迁移逻辑。

## 构建

确认 `local.properties` 中的 Android SDK 路径正确后运行：

```powershell
.\gradlew.bat test assembleDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。
