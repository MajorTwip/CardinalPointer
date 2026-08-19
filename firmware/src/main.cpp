#include <Arduino.h>
#include <NimBLEDevice.h>

#include "CommandParser.h"
#include "ServoAxis.h"
#include "config.h"

// --- Servo axes, one azimuth+elevation pair per camera ------------------
static ServoAxis azimuthAxes[CAMERA_COUNT] = {
    ServoAxis(PIN_SERVO_AZIMUTH[0], AZIMUTH_MIN_DEG, AZIMUTH_MAX_DEG, AZIMUTH_HOME_DEG),
    ServoAxis(PIN_SERVO_AZIMUTH[1], AZIMUTH_MIN_DEG, AZIMUTH_MAX_DEG, AZIMUTH_HOME_DEG),
    ServoAxis(PIN_SERVO_AZIMUTH[2], AZIMUTH_MIN_DEG, AZIMUTH_MAX_DEG, AZIMUTH_HOME_DEG),
    ServoAxis(PIN_SERVO_AZIMUTH[3], AZIMUTH_MIN_DEG, AZIMUTH_MAX_DEG, AZIMUTH_HOME_DEG),
};
static ServoAxis elevationAxes[CAMERA_COUNT] = {
    ServoAxis(PIN_SERVO_ELEVATION[0], ELEVATION_MIN_DEG, ELEVATION_MAX_DEG, ELEVATION_HOME_DEG),
    ServoAxis(PIN_SERVO_ELEVATION[1], ELEVATION_MIN_DEG, ELEVATION_MAX_DEG, ELEVATION_HOME_DEG),
    ServoAxis(PIN_SERVO_ELEVATION[2], ELEVATION_MIN_DEG, ELEVATION_MAX_DEG, ELEVATION_HOME_DEG),
    ServoAxis(PIN_SERVO_ELEVATION[3], ELEVATION_MIN_DEG, ELEVATION_MAX_DEG, ELEVATION_HOME_DEG),
};

static bool wasMoving[CAMERA_COUNT] = {false, false, false, false};
static unsigned long lastStatusMs[CAMERA_COUNT] = {0, 0, 0, 0};

// --- BLE handles --------------------------------------------------------
static NimBLECharacteristic* statusChar = nullptr;
static bool clientConnected = false;

// Build a status line for one camera: "POS <cam> <az> <el> <MOVING|IDLE>".
static String statusLine(int cam) {
    const bool moving = azimuthAxes[cam].moving() || elevationAxes[cam].moving();
    String s = "POS ";
    s += String(cam);
    s += ' ';
    s += String(azimuthAxes[cam].current(), 1);
    s += ' ';
    s += String(elevationAxes[cam].current(), 1);
    s += moving ? " MOVING" : " IDLE";
    return s;
}

static void notify(const String& payload) {
    if (statusChar == nullptr || !clientConnected) {
        return;
    }
    statusChar->setValue(payload.c_str());
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
            const bool okAz = azimuthAxes[cmd.camera].setTarget(cmd.azimuth);
            const bool okEl = elevationAxes[cmd.camera].setTarget(cmd.elevation);
            if (!okAz || !okEl) notify("ERR " + String(cmd.camera) + " angle clamped to limits");
            break;
        }

        case CommandType::Azimuth:
            if (!azimuthAxes[cmd.camera].setTarget(cmd.azimuth)) {
                notify("ERR " + String(cmd.camera) + " azimuth clamped");
            }
            break;

        case CommandType::Elevation:
            if (!elevationAxes[cmd.camera].setTarget(cmd.elevation)) {
                notify("ERR " + String(cmd.camera) + " elevation clamped");
            }
            break;

        case CommandType::Center:
            azimuthAxes[cmd.camera].setTarget(AZIMUTH_HOME_DEG);
            elevationAxes[cmd.camera].setTarget(ELEVATION_HOME_DEG);
            break;

        case CommandType::Stop:
            azimuthAxes[cmd.camera].stop();
            elevationAxes[cmd.camera].stop();
            break;

        case CommandType::Get:
            break; // just report below
    }

    notify(statusLine(cmd.camera));
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
    // Default ATT MTU (23 bytes / 20 usable) is tight for 4-camera status
    // lines like "POS 3 102.5 45.0 MOVING" — request a larger one.
    NimBLEDevice::setMTU(185);

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
    statusChar->setValue(statusLine(0).c_str());

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

    for (int cam = 0; cam < CAMERA_COUNT; cam++) {
        azimuthAxes[cam].begin();
        elevationAxes[cam].begin();
    }
    setupBle();
}

void loop() {
    static unsigned long lastUpdateMs = millis();

    const unsigned long now = millis();
    const float dt = (now - lastUpdateMs) / 1000.0f;
    lastUpdateMs = now;

    for (int cam = 0; cam < CAMERA_COUNT; cam++) {
        azimuthAxes[cam].update(dt, SLEW_DEG_PER_SEC);
        elevationAxes[cam].update(dt, SLEW_DEG_PER_SEC);

        const bool moving = azimuthAxes[cam].moving() || elevationAxes[cam].moving();

        // Stream progress while moving, and emit one final frame on arrival.
        if (moving && now - lastStatusMs[cam] >= STATUS_INTERVAL_MS) {
            notify(statusLine(cam));
            lastStatusMs[cam] = now;
        } else if (!moving && wasMoving[cam]) {
            notify(statusLine(cam));
        }
        wasMoving[cam] = moving;
    }

    delay(5);
}
