#include <WiFi.h>
#include <WiFiUdp.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_MPU6050.h>
#include "esp32-hal-ledc.h"

#define IN_1  27 
#define IN_2  26
#define IN_3  25  
#define IN_4  33  

const char *ssid = "Redmi";
const char *password = "12345678";

IPAddress phoneIP(192, 168, 233, 120);
IPAddress myIP, IPclient;
char incomingPacket[255];

const int UDP_PORT_RECEIVE = 44086;
const int UDP_PORT_SEND = 35082;

Adafruit_MPU6050 mpu;
WiFiUDP UDP;

  void goAhead() { 
    digitalWrite(IN_1, HIGH); 
    digitalWrite(IN_2, LOW);
    digitalWrite(IN_3, HIGH);  
    digitalWrite(IN_4, LOW);
  }

void goBack() { 
  digitalWrite(IN_1, LOW); 
  digitalWrite(IN_2, HIGH);
  digitalWrite(IN_3, LOW); 
  digitalWrite(IN_4, HIGH);
}

void goRight() { 
  digitalWrite(IN_1, LOW);
  digitalWrite(IN_2, HIGH);
  digitalWrite(IN_3, HIGH);
  digitalWrite(IN_4, LOW);
}

void goLeft() {
  digitalWrite(IN_1, HIGH);
  digitalWrite(IN_2, LOW);
  digitalWrite(IN_3, LOW);
  digitalWrite(IN_4, HIGH);
}

void stopRobot() { 
  digitalWrite(IN_1, LOW);
  digitalWrite(IN_2, LOW);
  digitalWrite(IN_3, LOW);
  digitalWrite(IN_4, LOW);
}

void sendSensorData() {
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);

  String data = "Acceleration: X=" + String(a.acceleration.x, 2)
              + " Y=" + String(a.acceleration.y, 2)
              + " Z=" + String(a.acceleration.z, 2)
              + " Gyro X=" + String(g.gyro.x, 2)
              + " Y=" + String(g.gyro.y, 2)
              + " Z=" + String(g.gyro.z, 2);

  UDP.beginPacket(phoneIP, UDP_PORT_SEND);  
  UDP.print(data);
  UDP.endPacket();

  Serial.println("Sent: " + data);
}



void setup() {
  Serial.begin(115200);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  while (WiFi.waitForConnectResult() != WL_CONNECTED);

  myIP = WiFi.localIP();
  Serial.print("Đã kết nối với ");
  Serial.println(ssid);
  Serial.print("ESP32 IP: ");
  Serial.println(myIP);

  UDP.begin(UDP_PORT_RECEIVE);
  Wire.begin(21, 22);

  Serial.println("Khởi tạo MPU6050...");
  if (!mpu.begin()) {
    Serial.println("Không tìm thấy MPU6050");
    while (1) {
      Serial.println("Đang thử lại...");
      delay(1000);
    }
  }

  mpu.setAccelerometerRange(MPU6050_RANGE_8_G);
  mpu.setGyroRange(MPU6050_RANGE_500_DEG);
  mpu.setFilterBandwidth(MPU6050_BAND_5_HZ);

  pinMode(IN_1, OUTPUT);
  pinMode(IN_2, OUTPUT);
  pinMode(IN_3, OUTPUT);
  pinMode(IN_4, OUTPUT);

  // Start sensor task trên Core 1 (freeRTOS)
  xTaskCreatePinnedToCore(
    sensorTask,          
    "SensorTask",        
    4096,          
    NULL,                 
    1,                  
    NULL,                
    1                     
  );
}
void sensorTask(void *parameter) {
  while (true) {
    sendSensorData();
    vTaskDelay(1000 / portTICK_PERIOD_MS);
  }
}

void loop() {
  int packetSize = UDP.parsePacket();
  if (packetSize) {
    IPclient = UDP.remoteIP();    
    int len = UDP.read(incomingPacket, 255);
    if (len > 0) {
      incomingPacket[len] = '\0'; 
    }

    Serial.print("Received: ");
    Serial.println(incomingPacket);

    if (strcmp(incomingPacket, "F") == 0) {
      goAhead();
    } else if (strcmp(incomingPacket, "B") == 0) {
      goBack();
    } else if (strcmp(incomingPacket, "L") == 0) {
      goLeft();
    } else if (strcmp(incomingPacket, "R") == 0) {
      goRight();
    } else if (strcmp(incomingPacket, "S") == 0) {
      stopRobot();
    } else {
      Serial.println("Unknown Command");
    }
  }
}
