# 学习锁 StudyLock

> 一个面向家庭的**局域网家长控制 / 专注管理** Android 应用。设备在局域网内运行一个轻量 HTTP 服务，家长用浏览器打开控制端页面，即可远程查看状态、调整学习时长、管理题库、设置家长密码，而孩子端通过「答题解锁」机制维持专注。

本项目为**开源重建版本**：从原应用（V2.6.5）的安装包中分析其架构、局域网协议、配置结构与题库格式后，用 Kotlin 重新实现的干净源码。原始二进制安装包保留在 [`releases/`](releases/) 目录供对照，源码不包含混淆代码与任何第三方私有依赖。

---

## 功能特性

- **答题解锁**：结束专注需连续答对 N 道题；答错计入错题本并重新排队，直到答对为止
- **局域网控制端**：浏览器访问设备地址，无需安装额外 App，token（8 位验证码）鉴权，5 分钟自动过期
- **远程配置**：学习时长、答题数量、休息提醒、定时计划、语音播报等均可远程调整
- **题库系统**：支持题库包（zip）导入/导出、启用/停用、删除；兼容新旧两种题目格式
- **错题本**：自动收集错题，按错误次数加权复习，连对 2 次后移出
- **休息提醒**：每学习一段时间全屏提醒休息，可设置时长、家长可提前结束
- **家长密码**：4 位数字密码，未设置时首次设置免验证，密码永不回显
- **定时计划**：按星期几 + 起止时间段限定可学习时段
- **语音播报**：基于系统 TTS 朗读题目与提示；原应用另支持 Azure 神经网络语音（见 `docs/ARCHITECTURE.md`）

## 目录结构

```
.
├── app/src/main/java/com/zqc/studylock/
│   ├── api/        # 配置快照 CfgState / 运行状态 RuntimeStatus
│   ├── core/       # 锁定状态机 LockManager、答题 QuizSession、休息 RestClock
│   ├── data/       # Question / QuestionBank / PackInfo / WrongBook
│   ├── net/        # HttpServer（原生 ServerSocket）/ CfgServer（业务路由）
│   ├── receiver/   # BootReceiver / AlarmReceiver
│   ├── service/    # LanService（前台保活）/ TimerService
│   ├── tts/        # Speech（系统 TTS 封装）
│   ├── ui/         # MainActivity / LockActivity / PinActivity / PacksActivity / RestActivity
│   └── util/       # Prefs（SharedPreferences 封装，含全部配置键）
├── app/src/main/assets/panel.html     # 内嵌控制端页面
├── controller/index.html              # 独立家长控制端页面（可直接浏览器打开）
├── sample-question-bank/              # 题库格式示例
├── docs/API.md                        # 局域网 API 文档
├── docs/ARCHITECTURE.md               # 架构与还原说明
├── docs/QUESTION-BANK-FORMAT.md       # 题库包格式规范
├── tools/                             # 题库工具脚本
├── releases/                          # 原版 V2.6.5 APK 安装包（对照用）
└── LICENSE                            # MIT
```

## 构建

需要 JDK 17 + Android SDK（compileSdk 34）。

```bash
# 方式一：Android Studio
# 直接用 Android Studio 打开项目根目录，Sync 后运行 app 模块到设备

# 方式二：命令行（需安装 Android SDK 并配置 ANDROID_HOME）
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 使用

1. 在设备上安装应用，首次打开会请求**通知权限**、**悬浮窗权限**、**忽略电池优化**——三项均建议授予，否则全屏锁定与保活可能失效。
2. 主界面会显示 `验证码`（8 位数字）和 `控制端地址`。验证码 5 分钟内有效，过期后回到主界面刷新。
3. 在同一局域网的另一台设备的浏览器中打开控制端地址，输入验证码连接。
4. 通过控制端调整配置、导入题库；点击「开始专注」后设备进入锁定界面，答对设定数量的题目即可解锁。

> **安全提示**：本应用仅供家庭内部使用。局域网 HTTP 服务使用明文通信，请勿在公共网络暴露端口 `8899`。

## 局域网 API

控制端与设备间通过 JSON 通信，所有敏感端点需携带 `?t=<验证码>`。完整文档见 [`docs/API.md`](docs/API.md)。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/` | 控制端页面 |
| GET | `/api/status` | 运行状态（无需鉴权） |
| GET | `/api/get?t=` | 读取配置 |
| POST | `/api/set?t=` | 修改配置 |
| POST | `/api/test?t=` | 触发测试（lock/rest/tts） |
| GET | `/api/packs?t=` | 题库包列表 |
| POST | `/api/pack?t=` | 启用/停用题库包 |
| POST | `/api/pack-delete?t=` | 删除题库包 |
| GET | `/api/zips?t=` | 列出下载目录中的 zip |
| POST | `/api/import-local?t=` | 导入本地题库包 |
| POST | `/api/upload?t=` | 上传题库包（multipart） |
| GET | `/api/export?t=` | 导出题库 zip |

## 题库

题库以 zip 包分发，内含 `index.json` 索引与若干 `<id>.json` 题目文件。原应用自 V2.5.2 起不再内置题库，需用户自行导入。格式规范见 [`docs/QUESTION-BANK-FORMAT.md`](docs/QUESTION-BANK-FORMAT.md)，可用 `tools/pack_questions.py` 打包。

## 免责声明

本软件为个人学习与家庭管理用途的开源实现，采用 MIT 许可证。使用者应遵守所在地区的法律法规，尊重被管理者的知情权与隐私。作者不对因使用本软件产生的任何后果负责。

## License

[MIT](LICENSE) © 2026 StudyLock contributors
