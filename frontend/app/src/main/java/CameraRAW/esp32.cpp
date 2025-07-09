#define ESPDIR "/esp32_directory"
#define CAMERA_MODEL_AI_THINKER
#define ACK_TIMEOUT_MS 15000
#include "esp_camera.h"
#include "BLEDevice.h"
#include <BLE2902.h>
#include "SD_MMC.h"
#include "FS.h"

// ============= Camera Configuration =============
bool initCamera() {
    camera_config_t config;
    config.pin_pwdn = 32;
    config.pin_reset = -1;
    config.pin_xclk = 0;
    config.pin_sscb_sda = 26;
    config.pin_sscb_scl = 27;
    config.pin_d7 = 35;
    config.pin_d6 = 34;
    config.pin_d5 = 39;
    config.pin_d4 = 36;
    config.pin_d3 = 21;
    config.pin_d2 = 19;
    config.pin_d1 = 18;
    config.pin_d0 = 5;
    config.pin_vsync = 25;
    config.pin_href = 23;
    config.pin_pclk = 22;
    config.xclk_freq_hz = 20000000;
    config.ledc_timer = LEDC_TIMER_0;
    config.ledc_channel = LEDC_CHANNEL_0;
    config.pixel_format = PIXFORMAT_JPEG;
    config.frame_size = FRAMESIZE_SVGA;
    config.jpeg_quality = 12;
    config.fb_count = 2;

    if (esp_camera_init(&config) != ESP_OK) {
        Serial.println("Camera initialization failed!");
        return false;
    }
    return true;
}

// ============= BLE Configuration =============
#define SERVICE_UUID        "ABCD0001-0000-1000-8000-00805F9B34FB"
#define COMMAND_UUID        "ABCD0002-0000-1000-8000-00805F9B34FB"
#define DATA_UUID           "ABCD0003-0000-1000-8000-00805F9B34FB"

BLEServer *pServer;
BLEService *pService;
BLECharacteristic *pCommandChar;
BLECharacteristic *pDataChar;
bool isRecording = false;
String currentFilename;
bool waitingForAck = false;
unsigned long ackWaitStart;
std::vector<String> imageFiles;
unsigned long totalRecordingTime;



class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        Serial.println("Device connected");
    }

    void onDisconnect(BLEServer* pServer) {
        Serial.println("Device disconnected - restart advertising");
        delay(500);
        pServer->startAdvertising();
        wipeSDFolder(ESPDIR);
        waitingForAck = false;
    }
};

class CommandCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pChar) {
        String value = pChar->getValue().c_str();
        if (value == "START") {
            isRecording = true;
            Serial.println("Recording started via BLE");
        } else if (value == "STOP") {
            isRecording = false;
            Serial.println("Recording stopped via BLE");
        } else if (value == "RECEIVED"){
            if(waitingForAck){
                Serial.println("ACK received from Android");
                waitingForAck = false;
            }
        }
    }
};

void initBLE() {
    BLEDevice::init("ESP32-CAM-Recorder");
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    pService = pServer->createService(SERVICE_UUID);
    BLEDevice::setMTU(517);

    pCommandChar = pService->createCharacteristic(
            COMMAND_UUID,
            BLECharacteristic::PROPERTY_WRITE
    );

    pDataChar = pService->createCharacteristic(
            DATA_UUID,
            BLECharacteristic::PROPERTY_READ |
            BLECharacteristic::PROPERTY_NOTIFY
    );

    pDataChar->addDescriptor(new BLE2902());
    pCommandChar->setCallbacks(new CommandCallbacks());
    pService->start();

    BLEAdvertising *pAdvertising = pServer->getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->start();
    Serial.println("BLE Advertising Started");
}

// ============= SD Card Configuration =============
bool initSDCard() {
    if (!SD_MMC.begin()) {
        Serial.println("SD Card Mount Failed");
        return false;
    }
    if (SD_MMC.cardType() == CARD_NONE) {
        Serial.println("No SD Card detected");
        return false;
    }
    return true;
}

void wipeSDFolder(const char* folderPath) {
    // Initialize SD card
    if (!SD_MMC.begin()) {
        Serial.println("SD Card Mount Failed");
        return;
    }

    // Open the folder
    File root = SD_MMC.open(folderPath);
    if (!root) {
        Serial.println("Failed to open directory");
        return;
    }

    if (!root.isDirectory()) {
        Serial.println("Not a directory");
        return;
    }

    // Iterate through all files
    File file = root.openNextFile();
    while (file) {
        String filePath = String(folderPath) + "/" + String(file.name());

        // Delete the file
        if (SD_MMC.remove(filePath.c_str())) {
            Serial.printf("Deleted: %s\n", filePath.c_str());
        } else {
            Serial.printf("Failed to delete: %s\n", filePath.c_str());
        }
        file = root.openNextFile();
    }

    Serial.println("Folder wipe complete");
    SD_MMC.end();
}

String saveImageToSD(camera_fb_t *fb) {
    static int imageCount = 0;

    if(!SD_MMC.begin()) {
        Serial.println("SD Card not mounted!");
        return "";
    }
    if(!SD_MMC.exists(ESPDIR)) {
        Serial.printf("Attempting to create directory: %s\n", ESPDIR);
        if(!SD_MMC.mkdir(ESPDIR)) {
            Serial.printf("Failed to create directory %s. SD Error: %d\n", ESPDIR, SD_MMC.cardType());
            return "";
        }
        Serial.printf("Successfully created directory %s\n", ESPDIR);
    }
    char filename[40];
    sprintf(filename, "%s/image%d.jpg", ESPDIR, imageCount++);

    File file = SD_MMC.open(filename, FILE_WRITE);
    if(!file) {
        Serial.printf("Failed to create file %s\n", filename);
        return "";
    }

    size_t bytesWritten = file.write(fb->buf, fb->len);
    file.close();

    if(bytesWritten != fb->len) {
        Serial.printf("Write incomplete! Wrote %d/%d bytes to %s\n", bytesWritten, fb->len, filename);
        SD_MMC.remove(filename);
        return "";
    }

    Serial.printf("Successfully saved: %s (%d bytes)\n", filename, fb->len);
    imageFiles.push_back(String(filename));
    return String(filename);
}

void sendFileViaBLE() {
    File root = SD_MMC.open(ESPDIR);
    if (!root) {
        Serial.println("Failed to open SD card directory");
        return;
    }

    // Step 1: Count total image files
    int totalImages = 0;
    File file = root.openNextFile();
    while (file) {
        if (!file.isDirectory()) {
            totalImages++;
        }
        file = root.openNextFile();
    }
    root.close();

    if (totalImages == 0) {
        Serial.println("No images to send.");
        return;
    }

    // Step 2: Send the total image count
    uint8_t imageCount[2] = {
            (uint8_t)(totalImages >> 8),
            (uint8_t)(totalImages & 0xFF)
    };

    Serial.printf("Sending image count: %d images [%02X %02X]\n",
                  totalImages, imageCount[0], imageCount[1]);


    pCommandChar->setValue(imageCount, 2);
    pCommandChar->notify();
    delay(150);

    // Step 3: Send each file one by one
    root = SD_MMC.open(ESPDIR);
    while ((file = root.openNextFile())) {
        if (!file.isDirectory()) {
            String filename = file.name();
            size_t fileSize = file.size();

            // Send file size first (4 bytes = header)
            uint8_t sizeInfo[4] = {
                    (uint8_t)(fileSize >> 24),
                    (uint8_t)(fileSize >> 16),
                    (uint8_t)(fileSize >> 8),
                    (uint8_t)(fileSize & 0xFF)
            };

            Serial.printf("Sending file size header for %d byte file: %02X %02X %02X %02X\n",
                          fileSize,
                          sizeInfo[0], sizeInfo[1], sizeInfo[2], sizeInfo[3]);


            pDataChar->setValue(sizeInfo, 4);
            pDataChar->notify();
            delay(50);

            const size_t chunkSize = 128;
            uint8_t buffer[chunkSize];
            size_t totalSent = 0;
            Serial.printf("Starting BLE transfer of %s (%d bytes)\n", filename.c_str(), fileSize);
            unsigned long startTime = millis();

            while (file.available()) {
                size_t bytesRead = file.read(buffer, chunkSize);
                if (bytesRead > 0) {
                    pDataChar->setValue(buffer, bytesRead);
                    pDataChar->notify();
                    totalSent += bytesRead;
                    delay(15);
                }
            }
            file.close();
            SD_MMC.remove(filename.c_str());
            Serial.printf("Completed and removed from SDcard %s\n", filename.c_str());

            // Ack timeout for missing response from receiving file
            waitingForAck = true;
            ackWaitStart = millis();
            while (waitingForAck && millis() - ackWaitStart < ACK_TIMEOUT_MS) {
                delay(10);
            }
            if (waitingForAck) {
                Serial.println("ACK timeout, continuing...");
            }

            // **Possibly add a mechanism to wait for ACK before sending the next file**
        }
    }
    root.close();
    Serial.println("All images sent.");
}
// ============= Main Functions =============
void setup() {
    Serial.begin(115200);
    Serial.println("\n\nStarting ESP32-CAM Recorder");

    if (!initCamera()) {
        Serial.println("Camera initialization failed! Restarting...");
        ESP.restart();
    }

    if (!initSDCard()) {
        Serial.println("SD Card initialization failed! Skipping SD functions...");
    }

    initBLE();
    Serial.println("System ready - Waiting for BLE commands...");

    pinMode(4, OUTPUT);
    digitalWrite(4, LOW);
}

enum TransferState { IDLE, RECORDING, SENDING_FILE };
TransferState currentState = IDLE;

static unsigned long lastTime = 0;
unsigned long startTime;

void loop() {
    // Recording logic
    if (isRecording && !waitingForAck) {
        if(startTime == 0){
            startTime = millis();
        }
        currentState = RECORDING;
        camera_fb_t *fb = esp_camera_fb_get();
        if (fb) {

            Serial.printf("Captured image (%d bytes)\n", fb->len);

            Serial.printf("Loop execution time: %lu ms\n", millis() - lastTime);
            lastTime = millis();
            String filename = saveImageToSD(fb);
            esp_camera_fb_return(fb);
            if (filename.length() > 0) {
                Serial.println("stopped recording");
            }
        }

    }


    if (!isRecording && currentState == RECORDING) {
        totalRecordingTime = millis() - startTime;
        Serial.println("Total recording time:\n");
        Serial.println(totalRecordingTime);
        Serial.println("starting to send file");
        currentState = SENDING_FILE;
        sendFileViaBLE();
        imageFiles.clear();
        currentState = IDLE;
    }

    if (currentState == SENDING_FILE) {
        Serial.println("file should have arrived");
        if (waitingForAck && millis() - ackWaitStart > ACK_TIMEOUT_MS) {
            Serial.println("ACK timeout - Android didn't confirm receipt");
            waitingForAck = false;
            wipeSDFolder(ESPDIR);
        }
        currentState = IDLE;
    }
}