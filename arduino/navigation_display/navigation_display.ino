#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <TFT_eSPI.h>
#include <SPI.h>

// BLE定义
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

// TFT屏幕引脚定义（根据实际连接修改）
TFT_eSPI tft = TFT_eSPI();

// BLE服务器相关
BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;

// 导航数据结构
struct NavigationData {
  String roadName;
  String action;
  int distance;
  int remainingTime;
  int speed;
  bool isActive;
};

NavigationData navData;

// 接收缓冲区
String receivedData = "";

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("设备已连接");
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("设备已断开");
    }
};

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();

      if (rxValue.length() > 0) {
        Serial.print("收到数据: ");
        for (int i = 0; i < rxValue.length(); i++) {
          Serial.print(rxValue[i]);
          receivedData += rxValue[i];
        }
        Serial.println();

        // 检查是否收到完整的数据包（以换行符结束）
        if (receivedData.indexOf('\n') != -1) {
          parseNavigationData(receivedData);
          receivedData = "";
        }
      }
    }
};

void setup() {
  Serial.begin(115200);
  Serial.println("ESP32导航显示系统启动");

  // 初始化TFT屏幕
  tft.init();
  tft.setRotation(1); // 横屏显示
  tft.fillScreen(TFT_BLACK);
  tft.setTextColor(TFT_WHITE, TFT_BLACK);
  tft.setTextSize(2);
  tft.setCursor(10, 10);
  tft.print("导航显示系统");

  // 初始化BLE
  BLEDevice::init("ESP32_Navigation_Display");
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);

  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ |
                      BLECharacteristic::PROPERTY_WRITE |
                      BLECharacteristic::PROPERTY_NOTIFY |
                      BLECharacteristic::PROPERTY_INDICATE
                    );

  pCharacteristic->setCallbacks(new MyCallbacks());
  pCharacteristic->addDescriptor(new BLE2902());

  pService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();

  Serial.println("等待设备连接...");

  // 初始化导航数据
  navData.roadName = "未开始导航";
  navData.action = "等待导航开始";
  navData.distance = 0;
  navData.remainingTime = 0;
  navData.speed = 0;
  navData.isActive = false;

  updateDisplay();
}

void loop() {
  // 处理BLE连接状态变化
  if (!deviceConnected && oldDeviceConnected) {
    delay(500);
    pServer->startAdvertising();
    Serial.println("重新开始广播");
    oldDeviceConnected = deviceConnected;
  }
  if (deviceConnected && !oldDeviceConnected) {
    oldDeviceConnected = deviceConnected;
  }

  delay(100);
}

// 解析导航数据（JSON格式）
void parseNavigationData(String data) {
  Serial.println("解析导航数据: " + data);

  // 简单的JSON解析
  if (data.indexOf("\"roadName\":") != -1) {
    int start = data.indexOf("\"roadName\":") + 11;
    int end = data.indexOf("\"", start);
    navData.roadName = data.substring(start, end);
  }

  if (data.indexOf("\"action\":") != -1) {
    int start = data.indexOf("\"action\":") + 9;
    int end = data.indexOf("\"", start);
    navData.action = data.substring(start, end);
  }

  if (data.indexOf("\"distance\":") != -1) {
    int start = data.indexOf("\"distance\":") + 11;
    int end = data.indexOf(",", start);
    if (end == -1) end = data.indexOf("}", start);
    navData.distance = data.substring(start, end).toInt();
  }

  if (data.indexOf("\"remainingTime\":") != -1) {
    int start = data.indexOf("\"remainingTime\":") + 16;
    int end = data.indexOf(",", start);
    if (end == -1) end = data.indexOf("}", start);
    navData.remainingTime = data.substring(start, end).toInt();
  }

  if (data.indexOf("\"speed\":") != -1) {
    int start = data.indexOf("\"speed\":") + 8;
    int end = data.indexOf(",", start);
    if (end == -1) end = data.indexOf("}", start);
    navData.speed = data.substring(start, end).toInt();
  }

  navData.isActive = true;

  // 更新显示
  updateDisplay();
}

// 更新屏幕显示
void updateDisplay() {
  tft.fillScreen(TFT_BLACK);

  // 标题
  tft.setTextSize(3);
  tft.setTextColor(TFT_CYAN, TFT_BLACK);
  tft.setCursor(10, 10);
  tft.print("导航信息");

  // 分割线
  tft.drawLine(0, 45, tft.width(), 45, TFT_WHITE);

  if (navData.isActive) {
    // 道路名称
    tft.setTextSize(2);
    tft.setTextColor(TFT_YELLOW, TFT_BLACK);
    tft.setCursor(10, 60);
    tft.print("当前道路: ");
    tft.setTextColor(TFT_WHITE, TFT_BLACK);
    tft.setCursor(10, 85);
    tft.print(navData.roadName);

    // 导航动作
    tft.setTextColor(TFT_YELLOW, TFT_BLACK);
    tft.setCursor(10, 115);
    tft.print("下一步: ");
    tft.setTextColor(TFT_GREEN, TFT_BLACK);
    tft.setCursor(10, 140);
    tft.print(navData.action);

    // 距离
    tft.setTextColor(TFT_YELLOW, TFT_BLACK);
    tft.setCursor(10, 170);
    tft.print("距离: ");
    tft.setTextColor(TFT_WHITE, TFT_BLACK);
    tft.setCursor(80, 170);
    tft.print(navData.distance);
    tft.print(" 米");

    // 剩余时间
    tft.setTextColor(TFT_YELLOW, TFT_BLACK);
    tft.setCursor(10, 200);
    tft.print("剩余时间: ");
    tft.setTextColor(TFT_WHITE, TFT_BLACK);
    tft.setCursor(120, 200);
    tft.print(formatTime(navData.remainingTime));

    // 速度
    tft.setTextColor(TFT_YELLOW, TFT_BLACK);
    tft.setCursor(10, 230);
    tft.print("当前速度: ");
    tft.setTextColor(TFT_WHITE, TFT_BLACK);
    tft.setCursor(120, 230);
    tft.print(navData.speed);
    tft.print(" km/h");

    // 连接状态
    if (deviceConnected) {
      tft.fillCircle(tft.width() - 20, 20, 5, TFT_GREEN);
      tft.setTextSize(1);
      tft.setTextColor(TFT_GREEN, TFT_BLACK);
      tft.setCursor(tft.width() - 60, 30);
      tft.print("已连接");
    } else {
      tft.fillCircle(tft.width() - 20, 20, 5, TFT_RED);
      tft.setTextSize(1);
      tft.setTextColor(TFT_RED, TFT_BLACK);
      tft.setCursor(tft.width() - 60, 30);
      tft.print("未连接");
    }
  } else {
    // 未开始导航
    tft.setTextSize(2);
    tft.setTextColor(TFT_WHITE, TFT_BLACK);
    tft.setCursor(10, 80);
    tft.print("等待导航开始...");

    // 连接状态
    if (deviceConnected) {
      tft.fillCircle(tft.width() - 20, 20, 5, TFT_GREEN);
      tft.setTextSize(1);
      tft.setTextColor(TFT_GREEN, TFT_BLACK);
      tft.setCursor(tft.width() - 60, 30);
      tft.print("已连接");
    } else {
      tft.fillCircle(tft.width() - 20, 20, 5, TFT_RED);
      tft.setTextSize(1);
      tft.setTextColor(TFT_RED, TFT_BLACK);
      tft.setCursor(tft.width() - 60, 30);
      tft.print("未连接");
    }
  }
}

// 格式化时间（秒转换为分钟）
String formatTime(int seconds) {
  if (seconds < 60) {
    return String(seconds) + " 秒";
  } else {
    int minutes = seconds / 60;
    int remainingSeconds = seconds % 60;
    if (remainingSeconds == 0) {
      return String(minutes) + " 分钟";
    } else {
      return String(minutes) + " 分 " + String(remainingSeconds) + " 秒";
    }
  }
}