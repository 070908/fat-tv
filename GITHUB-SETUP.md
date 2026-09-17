# GitHub 账号注册与云编译完整指南

## 第一步：注册 GitHub 账号（无需翻墙，国内可访问）

1. 浏览器访问：https://github.com/signup
2. 输入你的邮箱 → 点击 **Continue**
3. 设置密码（8位以上，含大小写字母和数字）→ Continue
4. 设置用户名（英文，如 `zhangsan2024`，会被用于个人主页地址）
5. 验证你不是机器人：按提示选中正确的图片（如"选所有红绿灯"）
6. 邮箱会收到 6 位验证码，填入完成注册
7. 选择计划：免费版（Free）完全够用，点 **Continue for free**

**全程约 2 分钟，不需要任何翻墙工具。**

---

## 第二步：创建仓库

1. 登录后，右上角 `+` 号 → **New repository**
2. 填写：
   - Repository name: `fat-tv`
   - Description: `红米R70A电视音乐播放器`
   - 选择 **Public**（免费，所有人可见）
   - ✅ 勾选 **Add a README file**（可选，方便仓库有初始内容）
3. 点击 **Create repository**

---

## 第三步：上传代码触发自动编译

在你的 `fat-tv` 项目文件夹内操作：

### 方式 A：Git 命令行（推荐，已包含在项目源码中）

打开项目文件夹，右键 → **Git Bash Here**：

```bash
# 1. 初始化 Git
$ git init

# 2. 添加所有文件
$ git add .

# 3. 提交
$ git commit -m "Initial commit: fat TV music player"

# 4. 创建 main 分支
$ git branch -M main

# 5. 连接远程仓库（将 "你的用户名" 替换为实际用户名）
$ git remote add origin https://github.com/你的用户名/fat-tv.git

# 6. 推送代码
$ git push -u origin main
```

首次推送会要求输入 GitHub 用户名和密码（或 Token）。

---

### 方式 B：GitHub 网页上传（不熟悉命令行时）

1. 进入仓库页面 → **Add file** → **Upload files**
2. 将整个 `fat-tv` 文件夹内容压缩为 `fat-tv.zip` 上传
3. 但此方式不会触发 Actions，需要额外点击 Actions 标签手动启用

---

## 第四步：获取编译好的 APK

Push 代码后，GitHub 自动开始编译：

1. 仓库页面 → **Actions** 标签
2. 看到 workflow 运行中（黄色圆圈）→ 等待 5-8 分钟
3. 变为绿色 ✅ 表示成功
4. 点击最新的 workflow run → 页面底部 **Artifacts**
5. 下载 `fat-tv-debug-apk`（无需登录即可下载公开仓库的 Artifacts）

**APK 文件约 3-5 MB，下载后直接安装到电视即可。**

---

## 常见问题

| 问题 | 解决 |
|------|------|
| git push 提示 403 | 2021年后 GitHub 不再支持密码登录，需创建 Personal Access Token：Settings → Developer settings → Personal access tokens → Generate new token（classic），勾选 `repo` 权限，用 Token 替代密码 |
| Actions 显示红色 ❌ | 点击 workflow 查看具体错误，通常是依赖下载失败，重新点 "Re-run jobs" 即可 |
| 找不到下载按钮 | 确保 workflow 已运行完成（绿色），Artifacts 在页面最底部 |

---

**推荐：如果你已有邮箱，整个流程从注册到拿到 APK 约 15 分钟，完全零配置。**
