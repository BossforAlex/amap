# 高德地图导航数据监听系统

一个用于监听和显示高德地图导航数据的应用程序，提供实时导航监控、数据智能转换和蓝牙设备交互功能。

## 功能特性

🗺️ **实时导航监听**：监听高德地图导航数据，专业术语转通俗中文
📝 **智能数据转换**：JSON数据转自然语言描述，状态码转文字说明
🔍 **关键信息突出**：隐藏技术字段，突出显示关键导航信息
📋 **数据管理功能**：清空数据、复制信息、缓存管理
📱 **蓝牙设备交互**：实现真实蓝牙设备显示与交互

## 系统架构

### 硬件部分
- **ESP32开发板**：接收蓝牙数据并显示在TFT屏幕上
- **TFT显示屏**：实时显示导航信息

### 软件部分
- **Android应用**：监听高德地图导航数据，通过蓝牙发送到ESP32
- **Arduino程序**：ESP32端程序，接收并显示导航数据

## 文件结构

```
AMAP/
├── arduino/                    # Arduino程序
│   └── navigation_display/     # ESP32导航显示程序
├── android/                    # Android应用
│   ├── app/src/main/          # 应用源码
│   │   ├── java/com/amap/navigation_listener/
│   │   │   ├── MainActivity.java
│   │   │   ├── NavigationFragment.java
│   │   │   ├── BluetoothFragment.java
│   │   │   ├── DataConverterFragment.java
│   │   │   ├── NavigationListenerService.java
│   │   │   └── NavigationData.java
│   │   └── res/               # 资源文件
│   └── build.gradle           # Gradle构建文件
├── .github/workflows/         # GitHub Actions
│   └── build-apk.yml          # APK自动构建流程
└── docs/                      # 项目文档
```

## 快速开始

### Android应用编译

1. 克隆仓库到本地
```bash
git clone https://github.com/yourusername/AMAP.git
```

2. 使用Android Studio打开`android`目录

3. 构建项目：
   - 点击 `Build > Make Project`
   - 或使用命令行：`./gradlew assembleDebug`

### GitHub Actions自动编译

项目已配置GitHub Actions，每次推送到main分支时会自动编译APK文件。

1. 将代码推送到GitHub仓库
2. 进入GitHub仓库的Actions标签页
3. 等待构建完成
4. 在Artifacts中下载编译好的APK文件

### Arduino程序上传

1. 打开`arduino/navigation_display/navigation_display.ino`
2. 安装必要的库：
   - BLEDevice
   - TFT_eSPI
3. 根据你的ESP32和TFT屏幕连接修改引脚配置
4. 上传到ESP32开发板

## 使用说明

### Android应用

1. 安装并打开应用
2. 授予必要的权限（蓝牙、位置等）
3. 点击"连接设备"，选择ESP32导航显示设备
4. 打开高德地图开始导航
5. 应用将实时显示导航信息并通过蓝牙发送给ESP32

### 数据转换功能

- **状态码转换**：输入1-20的状态码，查看对应的导航状态说明
- **JSON转自然语言**：输入导航数据JSON，转换为通俗的自然语言描述
- **专业术语转换**：自动将专业术语转换为易懂的说法

### ESP32显示

ESP32接收到数据后会在TFT屏幕上显示：
- 当前道路名称
- 下一步导航动作
- 距离目的地距离
- 剩余时间
- 当前速度
- 连接状态

## 蓝牙通信协议

使用BLE（低功耗蓝牙）进行通信：
- 服务UUID：`4fafc201-1fb5-459e-8fcc-c5c9c331914b`
- 特征值UUID：`beb5483e-36e1-4688-b7f5-ea07361b26a8`

数据格式（JSON）：
```json
{
  "roadName": "当前道路名称",
  "action": "下一步动作",
  "distance": 500,
  "remainingTime": 300,
  "speed": 60,
  "isActive": true
}
```

## 开发说明

### 添加新功能

1. 在对应的Fragment中添加UI组件
2. 在Java类中实现业务逻辑
3. 更新布局文件
4. 测试功能

### 修改导航数据监听

编辑`NavigationListenerService.java`中的`extractNavigationData`方法，根据高德地图UI的变化调整解析逻辑。

## 注意事项

1. Android 6.0及以上需要动态申请权限
2. Android 12及以上需要额外的蓝牙权限
3. 高德地图版本更新可能影响数据监听
4. ESP32的TFT屏幕驱动需要根据具体型号配置

## 许可证

本项目采用MIT许可证 - 详见LICENSE文件

## 贡献

欢迎提交Issue和Pull Request来改进项目。

## 更新日志

### v1.0.0 (2026-02-10)
- 初始版本发布
- 实现基本导航监听功能
- 添加蓝牙通信
- 实现数据转换功能
- 支持GitHub Actions自动编译