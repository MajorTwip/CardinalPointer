#include <Arduino.h>
#include <NimBLEDevice.h>

#include "CommandParser.h"
#include "ServoAxis.h"
#include "config.h"

// --- Servo axes ---------------------------------------------------------
static ServoAxis azimuth(PIN_SERVO_AZIMUTH, AZIMUTH_MIN_DEG, AZIMUTH_MAX_DEG,
                         AZIMUTH_HOME_DEG);
static ServoAxis elevation(PIN_SERVO_ELEVATION, ELEVATION_MIN_DEG,
                           ELEVATION_MAX_DEG, ELEVATION_HOME_DEG);

// --- BLE handles --------------------------------------------------------
static NimBLECharacteristic* statusChar = nullptr;
static bool clientConnected = false;

// Build the current status line: "POS <az> <el> <MOVING|IDLE>".
static String statusLine() {
    const bool moving = azimuth.moving() || elevation.moving();
    String s = "POS ";
    s += String(azimuth.current(), 1);
    s += ' ';
    s += String(elevation.current(), 1);
    s += moving ? " MOVING" : " IDLE";
    return s;
}

static void notify(const String& payload) {
    if (statusChar == nullptr || !clientConnected) {
        return;
    }
    statusChar->setValue(payload);
    statusChar->notify();
    Serial.print("-> ");
    Serial.println(payload);
}

// Apply a parsed command and acknowledge with a status/error notification.
static void applyCommand(const ParsedCommand& cmd) {
    switch (cmd.type) {
        case CommandType::None:
            return;

        case CommandType::Error:
            notify("ERR " + cmd.error);
            return;

        case CommandType::Aim: {
            const bool okAz = azimuth.setTarget(cmd.azimuth);
            const bool okEl = elevation.setTarget(cmd.elevation);
            if (!okAz || !okEl) notify("ERR angle clamped to limits");
            break;
        }

        case CommandType::Azimuth:
            if (!azimuth.setTarget(cmd.azimuth)) notify("ERR azimuth clamped");
            break;

        case CommandType::Elevation:
            if (!elevation.setTarget(cmd.elevation)) notify("ERR elevation clamped");
            break;

        case CommandType::Center:
            azimuth.setTarget(AZIMUTH_HOME_DEG);
            elevation.setTarget(ELEVATION_HOME_DEG);
            break;

        case CommandType::Stop:
            azimuth.stop();
            elevation.stop();
            break;

        case CommandType::Get:
            break; // just report below
    }

    notify(statusLine());
}

// --- BLE callbacks ------------------------------------------------------
class ServerCallbacks : public NimBLEServerCallbacks {
    void onConnect(NimBLEServer*) override {
        clientConnected = true;
        Serial.println("BLE client connected");
    }
    void onDisconnect(NimBLEServer* server) override {
        clientConnected = false;
        Serial.println("BLE client disconnected, advertising again");
        server->startAdvertising();
    }
};

class CommandCallbacks : public NimBLECharacteristicCallbacks {
    void onWrite(NimBLECharacteristic* characteristic) override {
        const std::string value = characteristic->getValue();
        if (value.empty()) {
            return;
        }
        const String line(value.c_str());
        Serial.print("<- ");
        Serial.println(line);
        applyCommand(parseCommand(line));
    }
};

static void setupBle() {
    NimBLEDevice::init(BLE_DEVICE_NAME);
    NimBLEDevice::setPower(ESP_PWR_LVL_P9);

    NimBLEServer* server = NimBLEDevice::createServer();
    server->setCallbacks(new ServerCallbacks());

    NimBLEService* service = server->createService(BLE_SERVICE_UUID);

    NimBLECharacteristic* cmdChar = service->createCharacteristic(
        BLE_CMD_CHAR_UUID,
        NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_NR);
    cmdChar->setCallbacks(new CommandCallbacks());

    statusChar = service->createCharacteristic(
        BLE_STATUS_CHAR_UUID,
        NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::NOTIFY);
    statusChar->setValue(statusLine());

    service->start();

    NimBLEAdvertising* advertising = NimBLEDevice::getAdvertising();
    advertising->addServiceUUID(BLE_SERVICE_UUID);
    advertising->setScanResponse(true);
    NimBLEDevice::startAdvertising();

    Serial.println("BLE advertising as " BLE_DEVICE_NAME);
}

void setup() {
    Serial.begin(115200);
    delay(200);
    Serial.println();
    Serial.println("CardinalPointer ESP32 servo interface starting");

    azimuth.begin();
    elevation.begin();
    setupBle();
}

void loop() {
    static unsigned long lastUpdateMs = millis();
    static unsigned long lastStatusMs = 0;
    static bool wasMoving = false;

    const unsigned long now = millis();
    const float dt = (now - lastUpdateMs) / 1000.0f;
    lastUpdateMs = now;

    azimuth.update(dt, SLEW_DEG_PER_SEC);
    elevation.update(dt, SLEW_DEG_PER_SEC);

    const bool moving = azimuth.moving() || elevation.moving();

    // Stream progress while moving, and emit one final frame on arrival.
    if (moving && now - lastStatusMs >= STATUS_INTERVAL_MS) {
        notify(statusLine());
        lastStatusMs = now;
    } else if (!moving && wasMoving) {
        notify(statusLine());
    }
    wasMoving = moving;

    delay(5);
}
