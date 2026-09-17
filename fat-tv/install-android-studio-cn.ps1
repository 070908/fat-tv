# 使用清华大学镜像站下载 Android Studio（国内可访问）
# 以管理员身份运行 PowerShell 后执行本脚本

$ErrorActionPreference = "Stop"

# 清华镜像 Android Studio 地址
$BaseUrl = "https://mirrors.tuna.tsinghua.edu.cn/android/studio/"
$Version = "2023.1.1.28"
$FileName = "android-studio-$Version-windows.exe"
$DownloadUrl = "https://mirrors.tuna.tsinghua.edu.cn/android/studio/$Version/$FileName"

$TempDir = "$env:TEMP"
$Installer = "$TempDir\$FileName"

Write-Host "========================================"
Write-Host "  正在从清华大学镜像站下载 Android Studio"
Write-Host "  版本: $Version"
Write-Host "  预计大小: ~1 GB"
Write-Host "========================================"

# 检查是否已存在安装程序
if (Test-Path $Installer) {
    Write-Host "检测到已下载的安装文件，跳过下载"
} else {
    try {
        Write-Host "开始下载... (可能需要 5-20 分钟，取决于网速)"
        # 使用 BITS 下载（支持断点续传）
        Start-BitsTransfer -Source $DownloadUrl -Destination $Installer -DisplayName "Android Studio" -Description "下载中..."
        Write-Host "下载完成: $Installer"
    } catch {
        Write-Warning "BITS 下载失败，尝试 Invoke-WebRequest..."
        Invoke-WebRequest -Uri $DownloadUrl -OutFile $Installer -UseBasicParsing
    }
}

# 验证文件存在
if (-not (Test-Path $Installer)) {
    throw "下载失败，文件不存在"
}

# 静默安装
$InstallDir = "$env:LOCALAPPDATA\Android\Android Studio"
Write-Host "开始静默安装到: $InstallDir"
Start-Process -FilePath $Installer -ArgumentList "/S /D=`"$InstallDir`"" -Wait -NoNewWindow

# 设置环境变量
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", "$env:LOCALAPPDATA\Android\Sdk", "User")

Write-Host ""
Write-Host "========================================"
Write-Host "  Android Studio 安装完成"
Write-Host "  安装路径: $InstallDir"
Write-Host "========================================"
Write-Host ""
Write-Host "下一步："
Write-Host "1. 重启电脑（使环境变量生效）"
Write-Host "2. 在开始菜单找到 Android Studio 启动"
Write-Host "3. 首次启动后按 BUILD-GUIDE.md 步骤配置 SDK"
