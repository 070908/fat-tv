# fat TV 本地编译与安装完整指南

## 环境准备

### 1. 安装 Android Studio
- 官网下载：https://developer.android.com/studio
- 安装时勾选：Android SDK、Android SDK Platform、Android Virtual Device
- 推荐版本：Android Studio Hedgehog (2023.1.1) 或更新

### 2. 配置 JDK
Android Studio 自带 OpenJDK，如需独立安装：
- 下载 OpenJDK 17：https://adoptium.net/
- 设置环境变量 `JAVA_HOME`

### 3. 确认 SDK 路径
Windows 默认路径：`C:\Users\<用户名>\AppData\Local\Android\Sdk`

---

## 第一步：解压项目

1. 将 `fat-tv.tar.gz` 解压到任意目录（如 `D:\Projects\fat-tv`）
2. **关键**：编辑 `fat-tv/local.properties`
   ```
   sdk.dir=C:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
   ```
   将路径改为你的实际 Android SDK 路径（注意双反斜杠）

---

## 第二步：打开项目

1. 启动 Android Studio
2. `File → Open`，选择解压后的 `fat-tv` 文件夹
3. 等待 Gradle 同步（首次约 5-15 分钟，自动下载依赖）
   - 如遇网络问题，配置国内 Maven 镜像：
     编辑 `fat-tv/settings.gradle`，在 repositories 中添加：
     ```groovy
     maven { url 'https://maven.aliyun.com/repository/public' }
     maven { url 'https://maven.aliyun.com/repository/google' }
     ```

---

## 第三步：编译 APK

### 方式 A：图形界面（推荐）

1. 菜单栏：`Build → Build Bundle(s) / APK(s) → Build APK(s)`
2. 等待编译完成（约 2-5 分钟）
3. 右下角弹出通知：`locate` 点击定位 APK

### 方式 B：命令行

在项目根目录打开终端：
```bash
# Windows
gradlew.bat assembleDebug

# Mac/Linux
./gradlew assembleDebug
```

APK 输出位置：
```
fat-tv/app/build/outputs/apk/debug/app-debug.apk
```

---

## 第四步：安装到 R70A 电视

### 前置：电视端开启调试

1. 红米 R70A 遥控器：主页 → 设置 → 账户与安全
2. 开启两项：
   - ✅ 安装未知来源应用（允许安装第三方 APK）
   - ✅ ADB 调试（允许电脑连接调试）
3. 查看电视 IP：设置 → 网络 → 当前 WiFi → 记住 IP 地址（如 192.168.1.105）

### 方式 A：ADB 无线安装（推荐开发调试）

```bash
# 1. 连接电视（电脑与电视同局域网）
adb connect 192.168.1.105:5555

# 2. 确认连接成功
adb devices
# 应显示：192.168.1.105:5555  device

# 3. 安装 APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 4. 安装成功提示：Success
```

### 方式 B：U 盘安装

1. 将 `app-debug.apk` 复制到 U 盘根目录
2. U 盘插入电视 USB 接口
3. 电视上打开 高清播放器/文件管理器
4. 找到 APK 文件，按遥控器 OK 键安装

### 方式 C：小米电视远程安装（无需 ADB）

1. 电视上打开 应用商店 → 应用管理 → 远程安装
2. 屏幕显示地址（如 `http://192.168.1.105:1234`）
3. 电脑浏览器访问该地址
4. 上传 `app-debug.apk` 文件
5. 电视自动安装，屏幕提示"安装成功"

---

## 第五步：运行与调试

### 启动应用
- 安装后按遥控器 主页 → 应用列表 → 找到 "fat TV"

### 查看日志（排查问题）
```bash
# 连接电视后查看实时日志
adb logcat | grep fatTV
```

### 常见安装失败排查

| 问题 | 原因 | 解决 |
|------|------|------|
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 已安装同名包但签名不同 | `adb uninstall com.fattv.app` 后重装 |
| `INSTALL_FAILED_OLDER_SDK` | API 版本不匹配 | R70A 是 Android 8+，本应用 minSdk=22，理论上不会触发 |
| `device unauthorized` | ADB 未授权 | 电视端确认允许调试，重新连接 |
| Gradle 同步卡住 | 网络下载依赖失败 | 配置阿里云 Maven 镜像 |

---

## 替代方案：无需本地 Android Studio 的构建方式

### 方式 C：GitHub Actions 自动构建（推荐零配置方案）

项目已包含 `.github/workflows/build.yml`，将项目推送到 GitHub 仓库后自动编译：

1. 注册 GitHub 账号，创建新仓库（如 `fat-tv`）
2. 推送代码：
   ```bash
   cd fat-tv
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/你的用户名/fat-tv.git
   git push -u origin main
   ```
3. GitHub 自动触发 Actions 编译，约 5-8 分钟
4. 进入仓库 → Actions → 点击最新 workflow run → Artifacts 下载 `fat-tv-debug-apk`

### 方式 D：Docker 本地构建（有 Docker 环境时）

项目已包含 `Dockerfile` 和 `docker-compose.yml`：

```bash
cd fat-tv
docker-compose up --build
```

构建完成后 APK 位于 `./app/build/outputs/apk/debug/app-debug.apk`

---

## 项目结构速查

编译时若需修改：
- **改背景透明度**：`app/src/main/res/layout/activity_main.xml` 中 `android:alpha="0.35"`
- **改音源默认地址**：`app/src/main/java/com/fattv/app/source/SourceManager.java` 中 `KEY_LOCAL_URL` 默认值
- **添加新音源**：实现 `SourceProvider` 接口，在 `SourceManager.init()` 中 `providers.add()`

---

## 进阶：修改后重新编译

每次修改代码后，只需重新执行 `Build → Build APK(s)` 或 `gradlew assembleDebug`，然后 `adb install -r app-debug.apk`（-r 表示覆盖安装保留数据）。
