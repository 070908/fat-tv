# fat TV - Android TV 网络音乐播放器

适配红米 R70A (L70M5-RA) 及其他国产 Android TV 的通用网络音乐播放器。

## 特性

- **半透明底壳背景** - 使用海绵宝宝主题图作为背景，alpha 0.35 半透明效果
- **DPad 遥控导航** - 全遥控器焦点导航，兼容无 GMS 国产电视
- **统一音源架构** - SourceManager 支持多源切换与自动降级
- **公网音源直连** - 默认通过 MusicFree/LX 音源插件协议访问网络音乐
- **局域网后端** - 支持 xiaomusic / LX Server 等本地后端切换
- **Media3 ExoPlayer** - 现代媒体播放引擎，支持后台播放

## 项目结构

```
fat-tv/
├── app/
│   ├── src/main/
│   │   ├── java/com/fattv/app/
│   │   │   ├── model/Song.java              # 歌曲数据模型
│   │   │   ├── player/
│   │   │   │   └── MusicPlaybackService.java # Media3 后台播放服务
│   │   │   ├── source/
│   │   │   │   ├── SourceProvider.java      # 音源接口定义
│   │   │   │   ├── SourceManager.java       # 统一音源管理器
│   │   │   │   ├── PublicSourceAdapter.java # 公网音源适配器
│   │   │   │   └── LocalSourceAdapter.java  # 局域网后端适配器
│   │   │   ├── ui/
│   │   │   │   ├── MainActivity.java        # 首页
│   │   │   │   ├── SearchActivity.java      # 搜索页
│   │   │   │   ├── PlayerActivity.java      # 播放器页
│   │   │   │   ├── SettingsActivity.java    # 设置页
│   │   │   │   └── adapter/SongAdapter.java # 歌曲列表适配器
│   │   │   └── utils/HttpUtils.java         # HTTP 工具
│   │   ├── res/
│   │   │   ├── drawable/
│   │   │   │   ├── bg_main.png              # 半透明背景图 (1.png)
│   │   │   │   ├── ic_logo.png              # 应用图标 (12.png)
│   │   │   │   ├── btn_focusable.xml        # 焦点按钮样式
│   │   │   │   └── item_focusable.xml       # 焦点列表项样式
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml        # 首页布局
│   │   │   │   ├── activity_search.xml      # 搜索页布局
│   │   │   │   ├── activity_player.xml      # 播放器页布局
│   │   │   │   ├── activity_settings.xml    # 设置页布局
│   │   │   │   └── item_song.xml            # 歌曲列表项布局
│   │   │   ├── mipmap-*/ic_launcher.png     # 各分辨率图标
│   │   │   └── values/
│   │   │       ├── strings.xml              # 字符串资源
│   │   │       └── themes.xml               # 主题样式
│   │   └── AndroidManifest.xml              # 应用清单
│   ├── build.gradle                         # App 模块构建配置
│   └── proguard-rules.pro                   # ProGuard 规则
├── build.gradle                             # 项目级构建配置
├── settings.gradle                          # 项目设置
├── gradle/wrapper/                          # Gradle Wrapper
└── local.properties                         # SDK 路径 (需修改)
```

## 本地编译指南

### 环境要求

- JDK 8 或 11+
- Android Studio Hedgehog (2023.1.1) 或更新版本
- Android SDK API 34
- Gradle 8.2 (通过 Wrapper 自动下载)

### 编译步骤

1. **修改 SDK 路径**
   编辑 `local.properties`，将 `sdk.dir` 改为你的 Android SDK 实际路径：
   ```
   sdk.dir=C:\\Users\\<你的用户名>\\AppData\\Local\\Android\\Sdk
   ```

2. **打开项目**
   在 Android Studio 中选择 `File → Open`，选择 `fat-tv` 文件夹。

3. **同步 Gradle**
   首次打开时会自动下载 Gradle Wrapper 和依赖库，等待同步完成。

4. **构建 APK**
   - 菜单选择 `Build → Build Bundle(s) / APK(s) → Build APK(s)`
   - 或使用命令行：
     ```bash
     ./gradlew assembleDebug
     ```

5. **输出位置**
   生成的 APK 位于：`app/build/outputs/apk/debug/app-debug.apk`

### 安装到电视

#### 方法一：ADB 无线调试（推荐开发调试）

1. 在 R70A 电视上：设置 → 账户与安全 → 开启 **ADB 调试**
2. 确保电脑和电视在同一局域网
3. 查看电视 IP：设置 → 网络 → 无线网络 → 当前连接的 WiFi → IP 地址
4. 电脑端执行：
   ```bash
   adb connect <电视IP>:5555
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

#### 方法二：U盘安装

1. 将 APK 复制到 U 盘
2. 插入电视 USB 接口
3. 使用电视文件管理器或第三方应用市场（如当贝市场）安装

#### 方法三：小米电视远程安装

1. 电视上打开 应用商店 → 应用管理 → 远程安装
2. 电脑浏览器访问电视显示的地址
3. 上传 APK 文件完成安装

## 音源配置

### 公网音源（默认）

应用启动后默认使用公网音源。需在 **设置页** 配置第三方音源插件地址。

### 局域网后端

1. 在一台常开设备上部署 [xiaomusic](https://github.com/hanxi/xiaomusic) 或 LX Server
2. 在 fat TV 设置页选择"局域网后端"
3. 输入服务器地址（如 `http://192.168.1.100:8090`）
4. 保存后自动检测连通性，失效时会弹出切换提示

## 已知限制与后续扩展

1. **QuickJS 引擎未集成** - PublicSourceAdapter 当前使用占位数据，需集成 quickjs-android 或类似 JS 引擎以执行真实 MusicFree/LX 音源脚本
2. **播放控制简化** - 当前仅实现播放/暂停，上一首/下一首功能需扩展播放队列管理
3. **歌词显示** - 播放器页需添加歌词滚动组件
4. **本地媒体** - LocalMediaAdapter 未实现，可后续添加扫描本地音乐功能

## 合规声明

本应用仅供个人学习研究使用。音源插件接入需遵守各平台服务条款，禁止商用或大规模分发。

---

**fat TV** - Powered by Media3 + SourceManager Architecture
