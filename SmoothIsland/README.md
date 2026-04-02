# SmoothIsland

LSPosed 模块，为小米 HyperOS 3 超级岛（Super Island）实现连续曲率（Squircle）圆角，消除直线与圆弧交接处的视觉折角感，效果接近 iOS Dynamic Island。

## 原理

通过 Hook `GradientDrawable.draw()`，识别 SystemUI 中属于超级岛的高圆角胶囊形 Drawable，并用三次贝塞尔曲线构建的连续曲率路径替换标准圆角绘制。

## 要求

- 已 Root（KernelSU / Magisk）
- 已安装 LSPosed 框架
- MIUI / HyperOS 系统（minSdk 31，即 Android 12+）

## 编译

1. 用 Android Studio 打开本项目
2. 菜单 **Build → Generate Signed Bundle / APK → APK → release**
3. 或命令行执行 `./gradlew assembleRelease`
4. 产物位于 `app/build/outputs/apk/release/app-release.apk`

## 安装

1. 安装编译好的 APK
2. 打开 LSPosed Manager → 模块 → 勾选 SmoothIsland
3. 作用域选择 **系统界面（com.android.systemui）**
4. 重启设备

## 参数调整

在 `MainHook.java` 中修改以下常量后重新编译：

| 常量 | 默认值 | 说明 |
|------|--------|------|
| `SQUIRCLE_FACTOR` | `0.70` | 连续曲率强度，0.55~0.80，越大越柔和 |
| `PILL_THRESHOLD` | `0.35` | 圆角判定阈值，避免误改其他控件 |
