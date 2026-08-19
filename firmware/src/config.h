#pragma once

// -------------------------------------------------------------------------
// Camera count
// -------------------------------------------------------------------------
// One two-axis (azimuth+elevation) aimer per camera. Must match the app's
// `CameraDirection.entries` order: index 0=North, 1=East, 2=South, 3=West.
static constexpr int CAMERA_COUNT = 4;

// -------------------------------------------------------------------------
// Servo wiring
// -------------------------------------------------------------------------
// 8 standard 5V hobby servos (2 per camera) driven by hardware PWM (LEDC) via
// ESP32Servo. Power the servos from a dedicated 5V rail and tie its ground to
// the ESP32 ground. Do NOT power servos from the ESP32 3V3 pin.
//
// AZIMUTH  = horizontal aim (pan / "swivel" in the app domain)
// ELEVATION = vertical aim  (tilt / "depression" in the app domain)
//
// NOTE: these GPIOs are placeholders — confirm/adjust against your actual
// wiring before flashing. Indexed the same way as CameraDirection above.
static constexpr int PIN_SERVO_AZIMUTH[CAMERA_COUNT]   = {18, 19, 21, 22};
static constexpr int PIN_SERVO_ELEVATION[CAMERA_COUNT] = {23, 25, 26, 27};

// -------------------------------------------------------------------------
// Servo pulse calibration (microseconds)
// -------------------------------------------------------------------------
// 500..2500us spans the full travel of most 180-degree servos. Narrow these
// if your servo buzzes or strains at the extremes.
static constexpr int SERVO_MIN_US  = 500;
static constexpr int SERVO_MAX_US  = 2500;
static constexpr int SERVO_FREQ_HZ = 50;

// -------------------------------------------------------------------------
// Logical angle limits (degrees)
// -------------------------------------------------------------------------
// Targets outside these bounds are clamped and reported back with an ERR line.
static constexpr float AZIMUTH_MIN_DEG   = 0.0f;
static constexpr float AZIMUTH_MAX_DEG   = 180.0f;
static constexpr float ELEVATION_MIN_DEG = 0.0f;
static constexpr float ELEVATION_MAX_DEG = 180.0f;

// Angle each axis assumes at boot / on CENTER.
static constexpr float AZIMUTH_HOME_DEG   = 90.0f;
static constexpr float ELEVATION_HOME_DEG = 90.0f;

// -------------------------------------------------------------------------
// Motion
// -------------------------------------------------------------------------
// Maximum slew rate. Movement is rate-limited so the mount tracks smoothly
// instead of snapping. Set to 0 for instant jumps to the target.
static constexpr float SLEW_DEG_PER_SEC = 120.0f;

// How often the firmware pushes a status notification while moving.
static constexpr unsigned long STATUS_INTERVAL_MS = 100;

// -------------------------------------------------------------------------
// BLE identifiers
// -------------------------------------------------------------------------
#define BLE_DEVICE_NAME      "CardinalPointer-Servo"
#define BLE_SERVICE_UUID     "6b1a0001-8f3a-4b7d-9c2e-0a1b2c3d4e5f"
#define BLE_CMD_CHAR_UUID    "6b1a0002-8f3a-4b7d-9c2e-0a1b2c3d4e5f"  // app -> device (write)
#define BLE_STATUS_CHAR_UUID "6b1a0003-8f3a-4b7d-9c2e-0a1b2c3d4e5f"  // device -> app (notify)
