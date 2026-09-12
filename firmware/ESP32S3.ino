#define MainDir "/MainDir"
#define BackupDir "/BackupDir"
#define CAMERA_MODEL_XIAO_ESP32S3

#define WAKE_UP_PIN GPIO_NUM_4 // pin 4
#define IDLE_TIMEOUT 30000 // 30s
#define ACK_TIMEOUT_MS 20000 // 20s
#define AUTO_RECORD_MS 7000 // 7s

unsigned long lastLoopTime = 0;
unsigned long totalRecordingTime = 0;
unsigned long startTime = 0;
unsigned long autoRecordTime = 0;


#include "esp_camera.h"
#include "BLEDevice.h"
#include <BLE2902.h>
#include <SD.h>
#include "FS.h"

enum SystemState {BOOT, IDLE , RECORDING, STOP_RECORDING , SENDING_FILE , AUTO, IMPORT};
SystemState currentState = BOOT;

// * Camera pin definitions * // 
#define PWDN_GPIO_NUM     -1
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM     10
#define SIOD_GPIO_NUM     40
#define SIOC_GPIO_NUM     39

#define Y9_GPIO_NUM       48
#define Y8_GPIO_NUM       11
#define Y7_GPIO_NUM       12
#define Y6_GPIO_NUM       14
#define Y5_GPIO_NUM       16
#define Y4_GPIO_NUM       18
#define Y3_GPIO_NUM       17
#define Y2_GPIO_NUM       15
#define VSYNC_GPIO_NUM    38
#define HREF_GPIO_NUM     47
#define PCLK_GPIO_NUM     13

// * Camera Config * //
bool initCamera()
{
    camera_config_t config;
    config.ledc_channel = LEDC_CHANNEL_0;
    config.ledc_timer = LEDC_TIMER_0;
    config.pin_d0 = Y2_GPIO_NUM;
    config.pin_d1 = Y3_GPIO_NUM;
    config.pin_d2 = Y4_GPIO_NUM;
    config.pin_d3 = Y5_GPIO_NUM;
    config.pin_d4 = Y6_GPIO_NUM;
    config.pin_d5 = Y7_GPIO_NUM;
    config.pin_d6 = Y8_GPIO_NUM;
    config.pin_d7 = Y9_GPIO_NUM;
    config.pin_xclk = XCLK_GPIO_NUM;
    config.pin_pclk = PCLK_GPIO_NUM;
    config.pin_vsync = VSYNC_GPIO_NUM;
    config.pin_href = HREF_GPIO_NUM;
    config.pin_sccb_sda = SIOD_GPIO_NUM;
    config.pin_sccb_scl = SIOC_GPIO_NUM;
    config.pin_pwdn = PWDN_GPIO_NUM;
    config.pin_reset = RESET_GPIO_NUM;
    config.xclk_freq_hz = 20000000;
    config.pixel_format = PIXFORMAT_JPEG;  //PIXFORMAT_JPEG;
    config.frame_size = FRAMESIZE_XGA; // FRAMESIZE_SVGA
    config.fb_location = CAMERA_FB_IN_PSRAM;
    config.jpeg_quality = 12;
    config.fb_count = 1;
    config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;

    if (psramFound())
    {
        Serial.println("Using psram");
        config.jpeg_quality = 6;  // Lower = better quality but larger files
        config.fb_count = 2;
        config.grab_mode = CAMERA_GRAB_LATEST;
    } else{
        Serial.println("Psram not found");
        config.jpeg_quality = 12;
        config.frame_size = FRAMESIZE_VGA;  // FRAMESIZE_SVGA
        config.fb_location = CAMERA_FB_IN_DRAM;
    }

    if (esp_camera_init(&config) != ESP_OK)
    {
        Serial.println("Camera initialization failed!");
        return false;
    }
    return true;
}

// * Service Characteristics * //
#define SERVICE_UUID   "ABCD0001-0000-1000-8000-00805F9B34FB"
#define COMMAND_UUID   "ABCD0002-0000-1000-8000-00805F9B34FB"
#define DATA_UUID      "ABCD0003-0000-1000-8000-00805F9B34FB"

// * BLE Configuration * //
BLEServer *pServer;
BLEService *pService;
BLECharacteristic *pCommandChar;
BLECharacteristic *pDataChar;

bool isRecording = false;
bool waitingForAck = false;
bool ackTimeout = false;
unsigned int idleStartTime = 0;

String currentFileName;
unsigned long ackWaitStart;
std::vector<String> imageFiles;
int imageCount = 0;

class MyServerCallbacks: public BLEServerCallbacks
{
    void onConnect(BLEServer* pServer)
    {
        Serial.println("Device Connected");
        idleStartTime = millis();
    }

    void onDisconnect(BLEServer* pServer)
    {
        Serial.println("Device disconnected - restart advertising");

        isRecording = false;
        waitingForAck = false;
        idleStartTime = millis();
        ackTimeout = false;
        currentState = IDLE;

        imageFiles.empty();
        imageCount = 0;
        delay(500);
        pServer->startAdvertising();
    }
};

class CommandCallbacks: public BLECharacteristicCallbacks
{
    void onWrite(BLECharacteristic *pChar)
    {
        String value = pChar->getValue().c_str();

        if (value == "START")
        {
            currentState = RECORDING; 
            Serial.println("Recording started via BLE");
        } else if (value == "STOP" && currentState == RECORDING)
        {
            currentState = STOP_RECORDING;
            Serial.println("Recording stopped via BLE");
        } else if (value == "RECEIVED")
        {
            waitingForAck = false;
            Serial.println("ACK received from Device");
        } else if (value == "SD" && (currentState != RECORDING || currentState != SENDING_FILE)){
            currentState = IMPORT;
        }
         else
        {
            Serial.println("Unknown command " + value);
        }
    }
};


void initBLE()
{
    BLEDevice::init("ESP32-CAM-Recorder");
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    pService = pServer->createService(SERVICE_UUID);
    BLEDevice::setMTU(183); // need to check, maybe lower MTU would suit better. 517 switched to 253.

    pCommandChar = pService->createCharacteristic(
        COMMAND_UUID,
        BLECharacteristic::PROPERTY_WRITE |
        BLECharacteristic::PROPERTY_NOTIFY
        );
    
    pDataChar = pService->createCharacteristic(
    DATA_UUID,
    BLECharacteristic::PROPERTY_READ |
    BLECharacteristic::PROPERTY_NOTIFY |
    BLECharacteristic::PROPERTY_WRITE 
    );

    pDataChar->setAccessPermissions(ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE); 

    //pDataChar->addDescriptor(new BLE2902()); - deprecated; Automatically added when notifications enabled on a characteristic
    pCommandChar->setCallbacks(new CommandCallbacks());
    pService->start();

    BLEAdvertising *pAdvertising = pServer->getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->start();

    Serial.println("BLE Advertising Started");
    
}

// * SD-Card Configuration * //
bool initSDCard() {
  if(!SD.begin(21)){ // GPIO 21
    Serial.println("SD-Card mount failed!");
    return false;
  }

  uint8_t cardType = SD.cardType();
  if(cardType == CARD_NONE) {
    Serial.println("No SD-Card detected!");
    return false;
  }

  Serial.println("SD-Card type: ");
  if(cardType == CARD_SDHC){ // currently using SDHC
     Serial.println("SDHC");
  } else if(cardType == CARD_MMC){
    Serial.println("MMC");
  } else if(cardType == CARD_SD){
    Serial.println("SDSC");
  } else{
    Serial.println("Unkown SD-Card");
  }
  
  return true;
     
}

// * SD-Card Clean-Up *//
void wipeSDFolder(const char* folderPath){
  File root = SD.open(folderPath);
  if(!root){
    Serial.printf("Failed to open directory: %s\n",folderPath);
    return;
  }

  if(!root.isDirectory()){
    Serial.printf("Given path isnt a directory: %s\n",folderPath);
    return;
  }

  File file = root.openNextFile();
  while(file){
      String filePath = String(folderPath) + "/" + String(file.name());

      if(SD.remove(filePath.c_str())){
        Serial.printf("Deleted: %s\n", filePath.c_str());
      } else{
        Serial.printf("Failed to delete: %s\n", filePath.c_str());
      }
      file = root.openNextFile();
  }

  Serial.println("Folder wipe complete!");
  SD.end();
}

// * Save Image to SD-Card (Post Capture) *//
String saveImageToSD(camera_fb_t *fb,const char* filePath){

 if(!SD.begin(21)){ // GPIO 21
    Serial.println("SD-Card mount failed!");
    return "";
  }

  if(!SD.exists(filePath)){
    Serial.printf("Attempting to create directory: %s\n", filePath);
    if(!SD.mkdir(filePath)){
      Serial.printf("Failed to create directory: %s. SD-Error: %d\n",filePath, SD.cardType());
      return "";
    }
    Serial.printf("Successfully created directory: %s\n",filePath);
  }

  char fileName[40];
  sprintf(fileName, "%s/image%d.jpg", filePath, imageCount++);

  File file = SD.open(fileName, FILE_WRITE);
  if(!file){
    Serial.printf("Failed to create file %s\n", fileName);
    return "";
  }

  size_t bytesWritten = file.write(fb->buf, fb->len);
  file.close();

  if(bytesWritten != fb->len){
    Serial.printf("Write incomplete! wrote %d/%d bytes to %s\n",bytesWritten, fb->len, fileName);
    SD.remove(fileName);
    return "";
  }

  Serial.printf("Successfully saved: %s (%dbytes)\n", fileName , fb->len);
  imageFiles.push_back(String(fileName));
  return String(fileName);
}

// * Send File From SD-Card Using BLE * //
void sendFile(const char* filePath){
  File root = SD.open(filePath);
    if(!root){
      Serial.printf("Failed to open SD-Card directory %s\n", filePath);
      return;
    }

    // Step 1
    int totalImages = 0;
    File file = root.openNextFile();
    while(file){
      if(!file.isDirectory()){
        totalImages++;
      }
      file = root.openNextFile();
    }
    root.close();

    if(totalImages == 0){
      Serial.println("No images were found");
      return;
    }


    // Step 2
   uint8_t imageCount[2] = {
    (uint8_t)(totalImages >> 8),
    (uint8_t)(totalImages & 0xFF)
    };

    Serial.printf("Sending image count to device : %d images [%02X %02X]\n",totalImages, imageCount[0], imageCount[1]);

    pDataChar->setValue(imageCount,2);
    pDataChar->notify();
    delay(150);

    // Step 3
    root = SD.open(filePath);
    int i = 0;
    while((file = root.openNextFile())){
      if(!file.isDirectory()){
        String fileName = file.name();
        size_t fileSize = file.size();


        // header

        uint8_t header[4] = {
          (uint8_t)(fileSize >> 24),
          (uint8_t)(fileSize >> 16),
          (uint8_t)(fileSize >> 8),
          (uint8_t)(fileSize & 0xFF)
        };

        Serial.printf("Sending header for %d byte file: %02X %02X %02X %02X\n",fileSize,header[0],header[1],header[2],header[3]);

        pDataChar->setValue(header,4);
        pDataChar->notify();
        delay(1000);

        const size_t chunkSize = 180;
        uint8_t buffer[chunkSize];
        size_t totalSent = 0;

        Serial.printf("Starting BLE transfer of %s (%d bytes)\n", fileName.c_str(), fileSize);
        unsigned long startTime = millis();


        // Step 3.5
        waitingForAck = true;
        Serial.println("WaitingForAck");
        ackWaitStart = millis();

        while(file.available()){
          size_t bytesRead = file.read(buffer,chunkSize);
          if(bytesRead > 0){
            pDataChar->setValue(buffer, bytesRead);
            pDataChar->notify();
            totalSent += bytesRead;
  
            delay(50);
          }
        }
        Serial.printf("Total Sent: %d\n",totalSent);

        // Step 4
        while(waitingForAck && millis() - ackWaitStart < ACK_TIMEOUT_MS) {
          delay(250);
        }

        if(waitingForAck){
          Serial.printf("ACK timeout at image %d\n",i);
          ackTimeout = true;
        }
        i++;
        file.close();

      }
    }
    root.close();
    Serial.println("All files sent");

    if(!ackTimeout){
      wipeSDFolder(filePath);
    } else{
      copyToBackupDir(filePath,BackupDir);
    }
}
void copyToBackupDir(const char* sourceDir,const char* targetDir){
  File src = SD.open(sourceDir);
  if(!src || !src.isDirectory()){
    Serial.println("Source directory not found!");
    return;
  }

  if(!SD.exists(targetDir)){
    SD.mkdir(targetDir);
  }

  File file = src.openNextFile();
  while(file){
    if(!file.isDirectory()){
      String fileName = file.name();
      String baseName = fileName.substring(String(sourceDir).length());
      String newPath = String(targetDir) + baseName;

      Serial.printf("Moving %s -> %s\n",fileName.c_str(),newPath.c_str());

      File targetFile = SD.open(newPath, FILE_WRITE);
      if(targetFile){
        while(file.available()){
          targetFile.write(file.read());
        }
        targetFile.close();
      } else{
        Serial.println("Failed to create destination file!");
        return;
      }
    }
    file.close();
    file = src.openNextFile();

  }
  src.close();
  wipeSDFolder(sourceDir);
}

void goToDeepSleep(){
  Serial.println("Preparing deep sleep...");


  esp_sleep_enable_ext0_wakeup(WAKE_UP_PIN,0);

  Serial.flush();
  esp_deep_sleep_start();
}

bool isDirectoryEmpty(const char* filePath) {
  File dir = SD.open(filePath);
  if (!dir) {
    Serial.println("Failed to open directory");
    return true;
  }
  if (!dir.isDirectory()) {
    Serial.println("Path is not a directory");
    dir.close();
    return true;
  }

  File file = dir.openNextFile();
  if (file) {
    file.close();
    dir.close();
    return false;
  }
  dir.close();
  return true;
}

void autoRecord(){

     if (millis() - autoRecordTime > AUTO_RECORD_MS) {
        Serial.println("Auto record time exceeded, stopping...");
        currentState = IDLE;
        return;
    }

    camera_fb_t *fb = esp_camera_fb_get();
    if (fb) {
        Serial.printf("Captured image (%d bytes)\n", fb->len);
        Serial.printf("Loop execution time %lu ms \n", millis() - lastLoopTime);

        String fileName = saveImageToSD(fb,BackupDir);
        esp_camera_fb_return(fb);

        lastLoopTime = millis();

        if (fileName.length() > 0) {
            Serial.println("Saved frame!");
        }
    } else {
        Serial.println("Failed to capture frame");
    }

}

// * Setup - Called on Start-Up * //
void setup(){
  Serial.begin(115200);
  Serial.println("Starting ESP32S3");

  if(!initCamera()){
    Serial.println("Camera initialization failed!");
    delay(200);
    Serial.println("Restarting..");
    ESP.restart();
  }

  if(!initSDCard()){
    Serial.println("SD-Card initialization failed!");
    delay(100);
    Serial.println("Skipping SD functions...");
  }

  initBLE();
  delay(25);
  idleStartTime = millis();
  Serial.println("System ready, waiting for BLE-Commands");
}

// * Main Loop * //
void loop(){

  switch(currentState){
    case BOOT:
      if (!isDirectoryEmpty(BackupDir)) {
          Serial.println("BackupDir isn't empty, skipping recording");
          currentState = IDLE;
          idleStartTime = millis();
      }
      else{
        currentState = AUTO;
        autoRecordTime = millis();
      }
      break;

    case AUTO:
      autoRecord();
      break;

    case IDLE:
      if(millis() - idleStartTime > IDLE_TIMEOUT){
        wipeSDFolder(MainDir);
        goToDeepSleep();
      }
      break;

    case RECORDING:
      if (!waitingForAck) {
        if (startTime == 0) {
          startTime = millis();
        }
        camera_fb_t* fb = esp_camera_fb_get();
        if (fb) {
          Serial.printf("Captured image (%d bytes)\n", fb->len);
          Serial.printf("Loop execution time %lu ms \n", millis() - lastLoopTime);

          lastLoopTime = millis();
          String fileName = saveImageToSD(fb,MainDir);
          esp_camera_fb_return(fb);

          if (fileName.length() > 0) {
            Serial.printf("Stopped recording!\n");
          }
        }
      }
      break;

    case STOP_RECORDING:
      totalRecordingTime = millis() - startTime;
      Serial.printf("Total recording time %lu ms \n", totalRecordingTime);
      startTime = 0;
      currentState = SENDING_FILE;
      break;

    case SENDING_FILE:
      sendFile(MainDir);
      imageFiles.clear();
      currentState = IDLE;
      idleStartTime = millis(); // reset on entering idle
      Serial.println("Current state is IDLE");
      break;

    case IMPORT:
      sendFile(BackupDir);
      currentState = IDLE;
      idleStartTime = millis();
      break;

    default:
      break;
  
  }
  delay(15);
};
 /* if(currentState == BOOT){
    currentState = AUTO;
    autoRecord();
  }

  if(currentState == IDLE){
    if(millis() - idleStartTime > IDLE_TIMEOUT){
      goToDeepSleep();
    }
  }
  else{
    idleStartTime = millis();
  }
  if(isRecording && !waitingForAck){
    if(startTime == 0){
      startTime = millis();
    }
    currentState = RECORDING;

     Serial.println("Current state is RECORDING");

    camera_fb_t *fb = esp_camera_fb_get();
    if(fb){
      Serial.printf("Captured image (%d bytes)\n",fb->len);
      Serial.printf("Loop execution time %lu ms \n", millis() - lastLoopTime);

      lastLoopTime = millis();
      String fileName = saveImageToSD(fb);
      esp_camera_fb_return(fb);

      if(fileName.length() > 0){
        Serial.printf("Stopped recording!\n");
      }
    }
  }

  if(!isRecording && currentState == RECORDING){
    totalRecordingTime = millis() - startTime;
    Serial.printf("Total recording time %lu ms \n",totalRecordingTime);
    startTime = 0;

    currentState = SENDING_FILE;
     Serial.println("Current state is SENDING_FILE");
  }

  if(currentState == SENDING_FILE){
    sendFile(MainDir);
    imageFiles.clear();
    currentState = IDLE;
    idleStartTime = 0;
    Serial.println("Current state is IDLE");
  }
  delay(15);
};*/




