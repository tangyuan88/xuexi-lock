# 架构与还原说明

## 源码来源

本项目是一个**开源重建版本**。原始应用（学习锁 V2.6.5，包名 `com.zqc.studylock`）仅以编译后的 APK 形式提供，没有附带 Java 源码。本项目通过以下方式还原：

1. **静态分析 APK**：解包后得到 `classes.dex`、`assets/panel.html`、`assets/legacy_index.json`、`AndroidManifest.xml` 等资源。
2. **DEX 字符串表解析**：提取出全部类名（如 `LockManager`、`CfgServer`、`QuestionBank`、`QuizSession`、`WrongBook`、`LanService` 等）、API 路径（`/api/*`）、SharedPreferences 键（`K_*`）、配置项与方法名。
3. **控制端源码对照**：`assets/panel.html` 与独立控制端 HTML 是完整的浏览器端源码，直接作为 `controller/index.html` 与 `app/src/main/assets/panel.html` 保留。
4. **Kotlin 重写**：基于上述信息，用 Kotlin 重新实现核心逻辑，保持架构分层与原应用一致，但不包含任何混淆或反向工程得到的字节码。

因此，本项目是**功能等价的干净实现**，而非对原字节码的反编译转储。原版 APK 保留在 `releases/v2.6.5/` 供对照研究。

## 模块架构

```
                    ┌─────────────────────────┐
   浏览器控制端 ────▶│  CfgServer (net)        │  HTTP :8899 + token 鉴权
   (controller/)    │  ├─ HttpServer          │  原生 ServerSocket，线程池
                    │  └─ 路由 /api/*         │
                    └───────────┬─────────────┘
                                │ 读写
                    ┌───────────▼─────────────┐
                    │  App (单例装配)          │
                    │  Prefs / QuestionBank /  │
                    │  WrongBook / RestClock / │
                    │  Speech / LockManager    │
                    └───────────┬─────────────┘
                                │ 驱动
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
  LockManager 状态机       LanService 前台保活     BootReceiver/AlarmReceiver
  IDLE→LOCKED→SOLVED       START_STICKY           开机自启 / 定时触发
        │
        ├─ QuizSession 答题校验（答错重排 + 错题本加权）
        ├─ RestClock 休息计时（含定时计划时段判断）
        └─ Speech 语音播报（系统 TTS）
```

## 关键设计

### 局域网通信
`HttpServer` 是基于 `ServerSocket` + 固定线程池（8 线程）的轻量实现，只实现控制端所需的 GET/POST、查询参数解析、JSON 体读取与 multipart 上传。不依赖 NanoHTTPD 等第三方库，减少依赖面。每个连接 10 秒超时，单请求异常不影响服务整体。

### 鉴权模型
8 位数字验证码，5 分钟有效期。令牌与生成时间写入 SharedPreferences（`lan_token_v1` / `lan_token_at`）。所有写操作与敏感读操作校验 `?t=` 参数；`/api/status` 故意不鉴权，便于控制端探测在线状态。

### 配置存储
`Prefs` 封装 SharedPreferences（文件名 `study_lock`），键名与原应用 DEX 中的 `K_*` 常量保持一致，便于从原版数据迁移。所有数值型设置在 setter 中做边界钳制。

### 答题状态机
`QuizSession` 维护题目队列：答对推进计数，答错则重置连对计数并把该题重新排入队尾，直到答对为止。这保证「连续答对 N 题」的语义。错题本按错误次数加权抽题，连对 2 次自动移出。

### 保活
`LanService` 以前台服务常驻，`START_STICKY`；`BootReceiver` 监听开机与应用更新重启服务；`AlarmReceiver` 处理休息提醒与心跳。原应用另有针对各厂商（MIUI/EMUI/ColorOS 等）的自启动引导页，本项目在 `MainActivity` 中提供通用的权限入口。

## 与原应用的差异

- **语音**：原应用集成 Azure 神经网络 TTS（`Mstts`/`BingTts`，通过 `Ocp-Apim-Subscription-Key` 调用 `cognitiveservices`），本项目默认使用系统 TTS 以保证离线可用；配置字段 `azure_key`/`azure_region` 予以保留，便于后续接入。
- **题库**：原应用 V2.5.2 起不再内置题库，本项目同样通过 zip 导入，不预置任何题库内容（仅提供格式示例）。
- **伪装/ donate 模块**：原应用含应用图标伪装与捐赠二维码等功能，非核心，本项目未还原。
- **UI**：本项目提供功能完整但样式简朴的 Material 界面；原应用的精细化视觉（全屏锁定面板动画、拼音标注 RubySpan 等）未逐一复刻。

## 扩展建议

- 接入 Azure TTS：实现 `Speech` 的替代实现，使用 `azure_key` 与 `azure_region` 调用 SSML 接口。
- 增强安全：将 HTTP 替换为 HTTPS，或改为局域网 mDNS + 配对码握手。
- 增加家长端 App：复用 `docs/API.md` 协议开发原生控制端。
