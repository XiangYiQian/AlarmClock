# ⏰ 闹钟 APP (AlarmClock)

> 仿小米闹钟的安卓单机闹钟应用，支持自定义铃音和在线音乐搜索。

## 功能特性

### 📋 闹钟管理
- ✅ **新建/编辑/删除闹钟** — 长按列表项删除
- ✅ **自定义时间** — 可设置任意大于当前时间的时间点
- ✅ **重复设置** — 每天、工作日、周末、自定义星期
- ✅ **一键开关** — 列表页直接启用/禁用闹钟
- ✅ **标签** — 自定义闹钟名称

### 🔔 铃音设置
- ✅ **默认铃声** — 系统默认闹钟铃声
- ✅ **本地音频** — 从手机文件选择任意音频作为铃音
- ✅ **在线搜索音乐** — 搜索流行音乐并下载设为铃音
- ✅ **音量调节** — 0-100 音量滑块
- ✅ **音量渐强** — 30 秒内逐渐增大音量 (温柔唤醒)

### 💪 更多功能
- ✅ **震动** — 闹钟响时同步震动
- ✅ **贪睡** — 5 分钟后再次提醒，最多 3 次
- ✅ **全屏响铃界面** — 锁屏上方显示，含贪睡/关闭按钮
- ✅ **通知栏控制** — 通知栏快捷贪睡/关闭
- ✅ **开机自启** — 手机重启后自动恢复闹钟
- ✅ **深色模式** — 跟随系统深色模式

## 技术架构

### 技术栈
- **语言**: Kotlin
- **UI**: Material Design 3 (Material You)
- **数据库**: Room (SQLite)
- **调度**: AlarmManager (精确闹钟)
- **音频**: MediaPlayer + Vibrator
- **网络**: Retrofit2 + OkHttp3 + Gson
- **图片加载**: Glide

### 项目结构
```
AlarmClockApp/
├── app/
│   ├── build.gradle.kts          # 模块构建配置
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # 清单文件 (权限/组件)
│   │   ├── java/com/homi/alarmclock/
│   │   │   ├── AlarmClockApp.kt          # Application
│   │   │   ├── model/
│   │   │   │   ├── Alarm.kt              # 闹钟数据模型
│   │   │   │   └── MusicSearchResult.kt  # 音乐搜索结果模型
│   │   │   ├── data/
│   │   │   │   ├── AlarmDao.kt           # 数据访问层
│   │   │   │   └── AlarmDatabase.kt      # Room 数据库
│   │   │   ├── util/
│   │   │   │   ├── AlarmScheduler.kt     # 闹钟调度管理
│   │   │   │   ├── RingtonePlayer.kt     # 铃音/震动播放
│   │   │   │   ├── MusicDownloadManager.kt # 音乐下载
│   │   │   │   ├── MusicApiService.kt    # 音乐 API 接口
│   │   │   │   └── RetrofitClient.kt     # Retrofit 客户端
│   │   │   ├── receiver/
│   │   │   │   ├── AlarmReceiver.kt      # 闹钟触发接收
│   │   │   │   ├── BootReceiver.kt       # 开机重启调度
│   │   │   │   └── NotificationActionReceiver.kt # 通知操作
│   │   │   ├── service/
│   │   │   │   └── AlarmService.kt       # 前台播放服务
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt       # 闹钟列表页
│   │   │       ├── AddEditAlarmActivity.kt # 添加/编辑页
│   │   │       ├── MusicSearchActivity.kt # 在线音乐搜索
│   │   │       └── AlarmFiringActivity.kt # 全屏响铃界面
│   │   └── res/                          # 布局/颜色/主题/图标
│   └── proguard-rules.pro
├── build.gradle.kts              # 根构建配置
├── settings.gradle.kts
└── gradle.properties
```

## 构建指南

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高
- JDK 17
- Android SDK 34 (compileSdk)
- 最低支持: Android 8.0 (API 26)

### 步骤

1. **导入项目**
   ```bash
   # 将 AlarmClockApp 文件夹拷到你的电脑上
   # 用 Android Studio 打开 AlarmClockApp 目录
   ```

2. **等待 Gradle 同步**
   - Android Studio 会自动下载依赖，耐心等待

3. **配置音乐 API (可选)**
   - 默认使用网易云音乐 API
   - 如需更换，修改 `RetrofitClient.kt` 中的 `BASE_URL`
   - 可替换为自建 API 或其他音乐服务

4. **连接手机构建**
   - 手机开启 USB 调试
   - 点击 Run 按钮

5. **生成 APK**
   ```bash
   ./gradlew assembleRelease
   # APK 路径: app/build/outputs/apk/release/app-release.apk
   ./gradlew assembleDebug
   # APK 路径: app/build/outputs/apk/debug/app-debug.apk
   ```

## 权限说明

| 权限 | 用途 |
|------|------|
| SCHEDULE_EXACT_ALARM | 精确闹钟调度 |
| WAKE_LOCK | 唤醒 CPU |
| VIBRATE | 震动提醒 |
| RECEIVE_BOOT_COMPLETED | 开机自动恢复闹钟 |
| FOREGROUND_SERVICE | 前台播放铃音 |
| POST_NOTIFICATIONS | 通知栏显示 |
| READ_MEDIA_AUDIO | 读取本地音频文件 |
| INTERNET | 在线搜索音乐 |
| WRITE_EXTERNAL_STORAGE | 保存下载的音乐 |

## 使用说明

### 添加闹钟
1. 打开 APP → 点击右下角 **+** 按钮
2. 点击时间区域设置闹钟时间
3. 选择重复模式 (一次性 / 每天 / 工作日 / 自选)
4. 输入标签 (如"起床")
5. 选择铃音 (默认 / 本地音频 / 在线搜索)
6. 调整音量、震动、渐强、贪睡等选项
7. 点击 **保存**

### 在线搜索音乐设为铃音
1. 编辑闹钟 → 点击"在线搜索音乐"
2. 输入歌曲名或歌手名
3. 搜索结果中选择歌曲 → 自动下载
4. 下载完成后自动设为铃音

### 多余闹钟管理
- **关闭**: 列表页直接切换开关
- **删除**: 长按列表项 → 确认删除

## 截图参考 (UI 设计)

### 主界面
- Material 3 卡片式列表
- 大号时间显示 (42sp)
- 标签、重复方式、铃音信息一目了然
- 右下角 FAB 添加按钮

### 添加/编辑界面
- 大号时间选择器
- 圆形 Chip 选择重复星期
- 铃音选择入口 (本地 + 在线)
- 音量滑块、震动/渐强/贪睡开关

### 全屏响铃界面
- 黑色全屏背景
- 80sp 超大时间显示
- 贪睡 + 关闭按钮
- 锁屏上方显示

## 音乐 API 说明

项目默认使用网易云音乐公开 API 搜索音乐。由于此 API 为非官方接口，可能存在不稳定情况。

如需更稳定的方案:
1. 搭建自己的音乐 API 服务
2. 修改 `RetrofitClient.kt` 中的 `BASE_URL`
3. 适配 `MusicApiService.kt` 接口

## License

MIT License — 自由使用和修改