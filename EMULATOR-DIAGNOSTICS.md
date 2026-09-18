# fat TV — 模拟器环境诊断与技术储备

## 1. 当前环境故障定案

### 现象
- fat TV APK 在 **雷电模拟器 (Android 14 / API 34)** 和标准 **Android TV 模拟器 (API 34, x86)** 上均出现 **RenderThread SIGSEGV** 崩溃
- `fault addr 0xffffffffffffffe8`，崩溃栈：`GrGLGpu::createTexture` → `GrGLGpu::onCreateTexture` → `GrProxyProvider::createNonMippedProxyFromBitmap`
- 崩溃发生在 **首帧渲染**（位图上传 GPU 纹理时）

### 对照实验
| 测试对象 | 环境 | 结果 |
|---------|------|------|
| fat-tv UI重构版 (Build #24) | 雷电 + host GPU | 崩溃 |
| fat-tv-fixed.apk (历史验证通过版) | 雷电 + host GPU | 崩溃 |
| fat-tv-fixed.apk | 标准 TV 模拟器 API 34 | 崩溃 |
| fat-tv UI重构版 | 标准 TV 模拟器 + SwiftShader | 崩溃 |
| fat-tv UI重构版 | 标准 TV 模拟器 + ANGLE | 崩溃 |
| **系统 Settings App** | 雷电 | **崩溃** |

### 结论
> **非 App 代码问题。** 崩溃是**模拟器虚拟 GPU 转发栈（gfxstream / libEGL_adreno.so）在当前宿主 Intel UHD Graphics 730 上初始化失败**导致。
>
> 关键日志：`libEGL_adreno.so: init_api redirect ... to NULL failed` —— GPU 函数指针未绑定成功，`GrGLGpu::createTexture` 内部解引用空指针崩溃。

---

## 2. 可行验证路径

### 路径 A：真机验证（推荐）
- **设备**：红米 R70A L70M5-RA（目标设备）
- **方式**：ADB 有线/无线连接，直接安装 APK
- **步骤**：
  1. 电视开启「开发者选项」→「USB调试」+「允许安装未知来源」
  2. `adb connect <电视IP>:5555`（无线）或 USB 直连
  3. `adb install -r app-debug.apk`
  4. `adb shell am start -n com.fattv.app/.ui.MainActivity`
  5. `adb logcat -d | grep fattv` 查看运行日志

### 路径 B：更换模拟器镜像
- 使用 **API 30 或更低版本的 x86_64 TV 镜像**（Android 11 的 gfxstream 实现更稳定）
- 或使用 **API 29 x86 镜像**（无 gfxstream，传统硬件加速路径）
- 创建方式：AVD Manager → Create Device → Television → 选择 API 30 / x86_64

### 路径 C：Windows Subsystem for Android (WSA)
- 若设备支持 WSA，可在 Windows 上直接安装 APK 验证
- 需开启 WSA 的开发者模式 + ADB 连接

### 路径 D：另一台 PC 的模拟器
- 若用户有多台 PC，可在不同 GPU（NVIDIA/AMD）上测试模拟器
- Intel UHD 730 的驱动版本 `32.0.101.6129` 可能与当前模拟器版本存在已知兼容问题

---

## 3. 本地构建技术储备

### 方式 1：Git Bash + ./gradlew（当前验证可行，但首次构建慢）
```bash
cd /c/Users/Administrator/.qianfan/workspace/sessions/5912a7bc38e2474398605bddfe390b42/2026-09-17/new-chat
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew assembleDebug --no-daemon
```
- 首次构建耗时：约 8-15 分钟（依赖下载 + 编译）
- 产物：`app/build/outputs/apk/debug/app-debug.apk`
- **注意**：bash tool 超时限制 480000ms（8min），首次构建可能超时，需用后台脚本

### 方式 2：后台构建脚本（已交付 build-local.sh）
```bash
bash build-local.sh
```
- 后台运行 Gradle，轮询产物，15 分钟超时上限
- 日志输出到 `.gradle/build-local.log`
- 产物就绪后自动报告路径和大小

### 方式 3：CI 构建（最可靠）
- 推送到 GitHub main 分支自动触发 Actions
- Build #24 / #26 验证通过
- 产物下载：`https://github.com/070908/fat-tv/actions` → 选择最新成功 Build → Artifacts → `fat-tv-debug-apk`

---

## 4. 快速诊断命令

```bash
# ADB 路径
ADB="/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"

# 雷电连接
$ADB connect 127.0.0.1:5555

# 安装 APK
$ADB install -r app/build/outputs/apk/debug/app-debug.apk

# 启动并观察
$ADB logcat -c
$ADB shell am start -n com.fattv.app/.ui.MainActivity
sleep 5
$ADB logcat -d | grep -E "Fatal signal|fattv|RenderThread"

# 检查进程是否存活
$ADB shell "ps -A | grep fattv"

# 获取完整 tombstone（需要 root）
$ADB shell cat /data/tombstones/tombstone_00 2>/dev/null | head -50
```

---

## 5. 已知限制与注意事项

1. **模拟器 GL 兼容性**：当前环境（Intel UHD 730 + Android Emulator 37.1.11 + API 34 x86 TV 镜像）无法运行任何需要 GPU 渲染的 App（包括系统 Settings）
2. **硬件加速不能关闭**：`android:hardwareAccelerated="false"` 在 API 34 上已无法阻止 RenderThread 创建（系统强制硬件加速）
3. **CI 产物下载**：GitHub API 匿名下载 401，需用户手动在 Actions 页面点击下载；或配置 GITHUB_TOKEN
4. **Gradle 依赖缓存**：`~/.gradle/caches` 首次下载后后续构建快；清理缓存会触发重新下载

---

## 6. 下次修改后验证流程

1. 修改代码 → `git add . && git commit -m "..." && git push origin main`
2. 等待 CI Build 完成（约 3-5 分钟）
3. 在 GitHub Actions 页面下载 APK
4. **真机安装验证**（优先）或更换模拟器镜像验证
5. 若需本地构建：运行 `bash build-local.sh`

---

*文档版本: 2026-09-18*  
*关联任务: fat TV UI重构 — 构建验证与交付*  
*CI 状态: Build #26 ✅ (commit 29162c3)*
