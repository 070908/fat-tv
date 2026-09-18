# fat-tv UI 改进方案与参考指南

## 一、当前 UI 现状诊断

### 1.1 现有布局概览

| 页面 | 布局方式 | 主要问题 |
|------|---------|---------|
| **首页** (activity_main.xml) | 垂直 LinearLayout + 水平按钮组 | 纯文字按钮，无图标；缺少视觉层次；无焦点动效 |
| **搜索页** (activity_search.xml) | EditText + RecyclerView | 列表项过于简陋，无封面图；选中态仅颜色变化 |
| **播放页** (activity_player.xml) | 封面 + 标题 + 控制按钮 | 控制按钮复用 ic_logo（无实际图标）；无进度条/歌词区；无背景氛围 |
| **设置页** (activity_settings.xml) | 表单列表 | 未读取但推断为普通表单风格 |
| **列表项** (item_song.xml) | 左右布局（标题+艺人） | 无封面图；无时长/专辑信息；焦点态不突出 |

### 1.2 核心视觉问题

- **配色单一**：全黑底 + 纯白字，无品牌色，焦点仅通过 `btn_focusable` 做简单边框
- **无图标系统**：所有按钮、控制项都用同一个 `ic_logo`，用户无法凭视觉辨识功能
- **无卡片化**：列表项是扁平条，无圆角/阴影/选中放大效果
- **无背景氛围**：播放页纯黑底，无专辑封面模糊背景/渐变
- **字体层级混乱**：标题 42sp、正文 24sp、列表 20sp，但无统一的字号阶梯和字重设计

---

## 二、TV 端 UI 设计核心原则（10 英尺体验）

> 用户坐在 3-5 米外的沙发上，使用遥控器操作——所有设计决策必须围绕这一场景。

### 2.1 Google 官方 Leanback 设计规范要点

| 原则 | 具体要求 | fat-tv 当前差距 |
|------|---------|----------------|
| **焦点可见** | 选中项必须有明显放大 + 高亮/描边/外发光 | 仅简单边框变色，无放大 |
| **方向导航** | 上下左右键必须有明确的移动路径，禁止斜向跳转 | 需检查 `android:nextFocus` 配置 |
| **控件不重叠** | 可点击元素不可重叠，否则焦点无法识别 | 当前布局无重叠问题 |
| **简洁至上** | 每屏不超过 7 个主要选项，减少层级 | 首页 4 个按钮符合，搜索页列表需分页 |
| **信息密度** | 文字 ≥ 12sp，行高 1.4 倍，对比度 ≥ 4.5:1 | 当前满足，但字体层级需优化 |
| **安全区** | 关键信息距屏幕边缘 ≥ 48dp，避免被 TV 裁切 | 当前 padding 48dp 符合 |

### 2.2 焦点设计黄金法则

- **放大倍率**：焦点放大 1.1-1.2 倍（TV 行业惯例）
- **动画时长**：焦点切换动画 150-250ms（太短跳脱，太长拖沓）
- **视觉层次**：焦点态 = 放大 + 高亮色边框 + 轻微阴影/外发光
- **焦点记忆**：返回上一页时焦点应落在上次离开的位置

---

## 三、优秀参考案例

### 3.1 官方参考：Android Leanback Showcase

- **项目**：`androidtv-Leanback`（Google 官方示例）
- **仓库**：https://github.com/android/tv-samples
- **亮点**：
  - `BrowseSupportFragment`：左侧分类导航 + 右侧横向卡片行
  - `ImageCardView`：带封面图、标题、描述的圆角卡片，焦点时放大 + 阴影加深
  - `DetailsFragment`：左侧大封面 + 右侧详情 + 操作按钮行
  - 内置飞框动画（Focus Highlight）和缩放过渡

### 3.2 音乐类 App 参考

#### A. Universal Android Music Player（Google 官方）
- **仓库**：https://github.com/googlesamples/android-universalmusicplayer
- **亮点**：跨设备（Phone/Tablet/TV/Auto/Wear）统一设计，TV 端使用 MediaBrowserService + Leanback 播放控件

#### B. Stylish Music Player（开源，GitHub）
- **仓库**：https://github.com/ryanhoo/stylishmusicplayer
- **亮点**：移动端设计极美，TV 端可借鉴其配色方案（深色主题 + 专辑封面取色）和播放页转场动画

### 3.3 TV 直播类参考：my-tv

- **仓库**：https://github.com/lizongying/my-tv
- **亮点**：
  - 极简首屏：仅 3-4 个元素（频道分类 + 当前播放窗口）
  - 深蓝底色 + 亮蓝焦点色 (#223239 + #0096a6)，对比度 4.5:1
  - 频道卡片：16:9 封面 + 频道名，焦点时放大 + 高亮边框

---

## 四、fat-tv 具体改进方案

### 4.1 配色方案升级

**推荐方案：深色主题 + 品牌强调色**

```xml
<!-- values/colors.xml -->
<color name="bg_primary">#0F1419</color>      <!-- 主背景：极深蓝黑，比纯黑更有层次 -->
<color name="bg_surface">#1A2332</color>      <!-- 卡片/面板背景 -->
<color name="bg_overlay">#CC0F1419</color>    <!-- 半透明遮罩 -->
<color name="accent">#00D4FF</color>          <!-- 品牌色：亮青蓝，科技感，焦点态 -->
<color name="accent_dim">#0088AA</color>      <!-- 品牌色暗态：用于已选中/激活 -->
<color name="text_primary">#FFFFFF</color>   <!-- 主文字 -->
<color name="text_secondary">#A0AAB5</color> <!-- 次要文字 -->
<color name="text_disabled">#5A6573</color>  <!-- 禁用态 -->
```

**播放页氛围方案**：专辑封面取主色调 → 生成模糊背景 + 渐变遮罩（类似 Spotify/Apple Music）

### 4.2 首页重构：从文字按钮到卡片导航

**参考 Leanback BrowseFragment 布局**：

```
┌─────────────────────────────────────────┐
│  [Logo] fat TV                           │
│                                          │
│  ┌──────────┐  ┌──────────┐             │
│  │ [icon]   │  │ [icon]   │             │
│  │ 搜索音乐  │  │ 播放列表  │  ← 焦点放大1.15x │
│  └──────────┘  └──────────┘   + 青蓝外发光   │
│                                          │
│  ┌──────────┐  ┌──────────┐             │
│  │ [icon]   │  │ [icon]   │             │
│  │  设置    │  │ 切换音源  │             │
│  └──────────┘  └──────────┘             │
│                                          │
│  [最近播放] 横向卡片行 (可选)              │
└─────────────────────────────────────────┘
```

**实现要点**：
- 按钮改为 **圆角卡片**（`cardCornerRadius="8dp"`）
- 每个卡片包含：图标（48dp）+ 文字，纵向排列
- 焦点态：`scaleX/scaleY=1.15` + `elevation=8dp` + 外发光 drawable
- 背景使用 `bg_primary` 或渐变图

### 4.3 搜索页重构：卡片列表替代扁平条

**当前 vs 改进**：

| 维度 | 当前 | 改进后 |
|------|------|--------|
| 列表项 | 纯文字条（标题+艺人） | 卡片：封面图(80x80) + 标题 + 艺人 + 时长 + 专辑 |
| 焦点态 | 背景色变化 | 放大 1.1x + 边框高亮 + 封面轻微放大 |
| 布局 | 纵向列表 | 支持横向卡片行（HorizontalGridView）或多列网格 |

**列表项布局 (item_song_card.xml)**：

```xml
<androidx.cardview.widget.CardView
    android:layout_width="match_parent"
    android:layout_height="120dp"
    app:cardCornerRadius="8dp"
    app:cardBackgroundColor="@color/bg_surface"
    app:cardElevation="0dp">
    <LinearLayout android:orientation="horizontal">
        <ImageView android:layout_width="100dp" android:layout_height="100dp"
            android:scaleType="centerCrop" android:src="@{song.pic}"/>
        <LinearLayout android:orientation="vertical">
            <TextView android:textSize="22sp" android:textColor="@color/text_primary"/>
            <TextView android:textSize="16sp" android:textColor="@color/text_secondary"/>
            <TextView android:textSize="14sp" android:textColor="@color/text_disabled"/> <!-- 专辑 -->
        </LinearLayout>
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

### 4.4 播放页重构：沉浸式播放器

**参考 Spotify TV / Apple Music TV 布局**：

```
┌─────────────────────────────────────────┐
│  [模糊专辑封面背景 + 渐变遮罩]            │
│                                          │
│       ┌──────────────┐                  │
│       │              │                  │
│       │   专辑封面    │  ← 240x240dp      │
│       │   (圆角12dp) │    焦点时轻微呼吸  │
│       │              │    动画            │
│       └──────────────┘                  │
│                                          │
│          歌曲名称                         │
│          艺人 / 专辑                      │
│                                          │
│   ┌──────┐  ┌──────┐  ┌──────┐         │
│   │ ⏮   │  │ ⏯   │  │ ⏭   │  ← 实际播放控制图标 │
│   └──────┘  └──────┘  └──────┘         │
│                                          │
│   [========●==========] 03:45 / 04:20   │
│                                          │
│   [歌词区域 - 可选，滚动显示当前句]        │
└─────────────────────────────────────────┘
```

**关键改进点**：
- 背景：专辑封面高斯模糊 + 暗渐变，营造沉浸感
- 封面：圆角矩形（`cardCornerRadius="12dp"`），播放时轻微缩放呼吸动画
- 控制按钮：使用矢量图标（`VectorDrawable`）替代 `ic_logo`
- 进度条：横向 ProgressBar，可拖动（DPad 左右键）
- 歌词区：LRC 逐行高亮，当前句放大 + 品牌色

### 4.5 图标系统建设

当前所有按钮都用 `ic_logo`，必须建立图标库：

| 位置 | 当前 | 应使用图标 |
|------|------|-----------|
| 首页-搜索 | ic_logo | 搜索/放大镜图标 |
| 首页-播放列表 | ic_logo | 列表/播放列表图标 |
| 首页-设置 | ic_logo | 齿轮/设置图标 |
| 首页-切换音源 | ic_logo | 地球/切换图标 |
| 播放页-上一首 | ic_logo | 上一曲图标 |
| 播放页-播放/暂停 | ic_logo | 播放/暂停图标 |
| 播放页-下一首 | ic_logo | 下一曲图标 |

**实现方式**：在 `res/drawable/` 添加 `ic_search.xml`、`ic_playlist.xml`、`ic_settings.xml`、`ic_prev.xml`、`ic_play.xml`、`ic_pause.xml`、`ic_next.xml` 等矢量图标。

### 4.6 焦点动效增强

**焦点放大动画 (res/animator/focus_scale.xml)**：

```xml
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <set>
            <objectAnimator android:propertyName="scaleX" android:valueTo="1.15" android:duration="200"/>
            <objectAnimator android:propertyName="scaleY" android:valueTo="1.15" android:duration="200"/>
            <objectAnimator android:propertyName="elevation" android:valueTo="8" android:duration="200"/>
        </set>
    </item>
    <item>
        <set>
            <objectAnimator android:propertyName="scaleX" android:valueTo="1.0" android:duration="200"/>
            <objectAnimator android:propertyName="scaleY" android:valueTo="1.0" android:duration="200"/>
            <objectAnimator android:propertyName="elevation" android:valueTo="0" android:duration="200"/>
        </set>
    </item>
</selector>
```

**焦点边框 Drawable (res/drawable/focus_glow.xml)**：

```xml
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <shape android:shape="rectangle">
            <solid android:color="@color/bg_surface"/>
            <stroke android:width="3dp" android:color="@color/accent"/>
            <corners android:radius="8dp"/>
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/bg_surface"/>
            <corners android:radius="8dp"/>
        </shape>
    </item>
</selector>
```

---

## 五、推荐开源项目与技术方案

### 5.1 可直接引用的 Leanback 组件

| 组件 | 类名 | 用途 | 替代当前方案 |
|------|------|------|-------------|
| BrowseSupportFragment | `androidx.leanback.app.BrowseSupportFragment` | 首页分类浏览 | 替代 activity_main.xml 的 4 按钮 |
| ImageCardView | `androidx.leanback.widget.ImageCardView` | 内容卡片 | 替代 item_song.xml |
| DetailsSupportFragment | `androidx.leanback.app.DetailsSupportFragment` | 内容详情页 | 可作为歌曲详情页扩展 |
| SearchSupportFragment | `androidx.leanback.app.SearchSupportFragment` | 搜索页 | 替代当前搜索页 |
| PlaybackTransportControlGlue | `androidx.leanback.media.PlaybackTransportControlGlue` | 播放控制 | 替代播放页自定义控制 |
| ArrayObjectAdapter | `androidx.leanback.widget.ArrayObjectAdapter` | 数据绑定 | 替代 RecyclerView.Adapter |
| Presenter | `androidx.leanback.widget.Presenter` | 视图渲染 | 替代 RecyclerView.ViewHolder |

**Gradle 依赖**：

```groovy
dependencies {
    implementation 'androidx.leanback:leanback:1.0.0'
    implementation 'androidx.leanback:leanback-preference:1.0.0'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'com.github.bumptech.glide:glide:4.14.2'  // 图片加载
}
```

### 5.2 方案权衡：Leanback vs 自定义

| 维度 | 采用 Leanback | 保持自定义（当前方案） |
|------|--------------|---------------------|
| **开发周期** | 快（组件现成，一周出原型） | 慢（需手写焦点/动画/适配器） |
| **视觉上限** | 中（标准化，易千篇一律） | 高（完全自定义，可打造独特风格） |
| **TV 体验** | 优（Google 验证过的遥控器交互） | 需自行验证焦点导航 |
| **灵活性** | 低（必须按 Leanback 范式） | 高（任意布局） |
| **学习成本** | 中（需理解 Presenter/Adapter 模式） | 低（现有 Android 知识即可） |

**建议**：当前 fat-tv 已有自定义框架且功能验证通过，**不必全盘迁移到 Leanback**。建议分阶段改进：
1. **短期（当前阶段）**：在当前自定义框架上应用配色、图标、卡片、焦点动效
2. **中期（可选）**：搜索页/列表页可局部引入 `HorizontalGridView` + `Presenter` 模式
3. **长期（可选）**：如扩展为完整媒体中心，再考虑 `BrowseSupportFragment`

---

## 六、实施优先级与检查清单

### Phase 1：立即可做（不改架构，纯视觉）

- [ ] 建立 `colors.xml` 和 `dimens.xml` 设计系统
- [ ] 为 6 个按钮/控制位添加矢量图标
- [ ] 首页按钮改为圆角卡片 + 图标 + 文字纵向排列
- [ ] 替换 `btn_focusable.xml` 为带放大动画的焦点态
- [ ] 播放页添加进度条（ProgressBar）和实际图标

### Phase 2：列表页改进

- [ ] 引入 CardView 包裹列表项
- [ ] 列表项添加封面图（Glide 加载）
- [ ] 列表项添加时长、专辑字段
- [ ] 列表焦点态：放大 + 边框高亮

### Phase 3：播放页沉浸化

- [ ] 专辑封面模糊背景（Glide + Blur 或自定义 Shader）
- [ ] 封面圆角 + 呼吸动画
- [ ] 歌词区滚动高亮
- [ ] 播放进度可拖动（DPad 左右调节）

### Phase 4：可选 Leanback 升级

- [ ] 评估 `BrowseSupportFragment` 替换首页
- [ ] 评估 `PlaybackTransportControlGlue` 替换播放控制
- [ ] 评估 `SearchSupportFragment` 替换搜索页

---

## 七、资源链接

- **Google 官方 TV 设计指南**：https://developer.android.com/design/ui/tv
- **Leanback 库文档**：https://developer.android.com/training/tv/playback/compose
- **TV 应用质量检查表**：https://developer.android.com/docs/quality-guidelines/tv-app-quality
- **官方示例代码库**：https://github.com/android/tv-samples
- **my-tv 开源项目（极简 TV 设计参考）**：https://github.com/lizongying/my-tv
- **Android-tv-widget（中文 TV 控件库）**：https://github.com/zhangjianqiang135/android-tv-widget

---

*文档版本：v1.0*
*生成时间：2026-09-18*
*适用项目：fat-tv (com.fattv.app)*
