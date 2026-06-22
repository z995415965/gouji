# 多乐够级记牌器

<div align="center">

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52B8?style=for-the-badge&logo=kotlin&logoColor=white)
![API](https://img.shields.io/badge/API-26%2B-brightgreen?style=for-the-badge)

**基于录屏+OCR的够级记牌神器，全自动识别，解放双手！**

</div>

## ✨ 功能特性

- 🃏 **自动记牌** - 录屏识别够级游戏画面，自动统计剩余牌数
- 🔮 **悬浮窗显示** - 小巧悬浮球，点击展开记牌面板，不影响游戏
- 📊 **精准统计** - 支持5副牌/6副牌模式，实时计算剩余牌数
- 🎯 **够级规则** - 内置完整够级游戏规则，识别联邦、够级牌
- 🔒 **隐私安全** - 纯本地OCR识别，无需联网，数据不上传
- ⚡ **轻量高效** - 低内存占用，流畅运行在中低端安卓设备

## 📱 系统要求

- Android 8.0+ (API 26+)
- 悬浮窗权限
- 录屏权限
- 建议关闭电池优化

## 🚀 快速开始

### 1. 编译安装

```bash
# 方式一：Android Studio
1. 打开项目
2. 连接Android设备
3. 点击 Run 按钮

# 方式二：命令行
chmod +x build.sh
./build.sh
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. 首次使用

1. 打开APP，点击"开始记牌"
2. 按提示授予悬浮窗权限
3. 按提示授予录屏权限
4. 按提示关闭电池优化（推荐）
5. 切换到够级游戏，开始自动记牌

### 3. 使用记牌器

- **悬浮球** - 屏幕上的小圆球，可拖动
- **展开面板** - 点击悬浮球，显示剩余牌数
- **重置牌局** - 新局开始时点击"重置"按钮
- **切换牌型** - 首页下拉选择5副牌或6副牌

## 📖 详细教程

详见 `DEPLOYMENT_GUIDE.md` 文档

## 🏗️ 项目结构

```
gouji-card-counter/
├── app/
│   ├── src/main/java/com/example/goujicardcounter/
│   │   ├── logic/          # 够级规则引擎
│   │   ├── recognition/    # OCR识别模块
│   │   ├── service/        # 后台服务
│   │   ├── ui/             # 界面组件
│   │   │   └── floating/   # 悬浮窗
│   │   └── permission/     # 权限管理
│   └── src/main/res/       # 资源文件
├── .github/                # GitHub配置
├── build.gradle.kts        # 项目构建配置
└── DEPLOYMENT_GUIDE.md     # 部署指南
```

## 🔧 技术栈

- **语言**: Kotlin
- **UI**: View System + 悬浮窗
- **OCR**: PaddleOCR Lite (本地)
- **录屏**: MediaProjection API
- **后台**: Foreground Service
- **权限**: Runtime Permission

## ⚠️ 注意事项

1. **录屏权限** - 需要持续录屏才能识别牌面
2. **悬浮窗权限** - 必须开启才能显示记牌面板
3. **电池优化** - 建议关闭以保证后台稳定运行
4. **游戏画面** - 确保够级游戏画面清晰可见
5. **隐私保护** - 所有识别在本地完成，不上传任何数据

## 📄 许可证

MIT License

## 🙏 致谢

- PaddleOCR - 百度开源OCR引擎
- AndroidX - AndroidX库
- Material Design - Google设计语言

---

**Made with ❤️ for 够级爱好者**
