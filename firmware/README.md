# CardinalPointer ESP32 Servo Interface

Firmware for an ESP32 that receives aiming orders over **BLE** and drives **two
standard 5V PWM servos** (azimuth + elevation) to align a device. Built with
**PlatformIO** / Arduino framework and **NimBLE**.

Tracks issue [#8](https://github.com/MajorTwip/CardinalPointer/issues/8).

## Hardware

| Signal            | Default GPIO | Notes                                  |
| ----------------- | ------------ | -------------------------------------- |
| Azimuth servo PWM | `18`         | Horizontal aim (pan / app "swivel")    |
| Elevation servo PWM | `19`       | Vertical aim (tilt / app "depression") |
| Servo V+          | external 5V  | **Not** the ESP32 3V3 pin              |
| Servo GND         | common GND   | Must be tied to ESP32 GND              |

Pins, pulse-width calibration, angle limits, and slew rate all live in
[`src/config.h`](src/config.h).

> Power servos from a dedicated 5V supply able to source their stall current.
> Share the ground with the ESP32; do not back-power the board from the servo rail.

## Build & flash

```bash
# from firmware/
pio run                 # compile (default env: esp32dev)
pio run -t upload       # flash over USB
pio device monitor      # serial log @ 115200
```

An ESP32-S3 environment is also provided: `pio run -e esp32-s3-devkitc-1`.

## BLE protocol

Advertised as **`CardinalPointer-Servo`**.

| Role    | UUID                                     | Properties     |
| ------- | ---------------------------------------- | -------------- |
| Service | `6b1a0001-8f3a-4b7d-9c2e-0a1b2c3d4e5f`   | —              |
| Command | `6b1a0002-8f3a-4b7d-9c2e-0a1b2c3d4e5f`   | Write / WriteNR |
| Status  | `6b1a0003-8f3a-4b7d-9c2e-0a1b2c3d4e5f`   | Read / Notify  |

### Commands (write to the Command characteristic)

One ASCII command per write, case-insensitive, whitespace-separated:

| Command         | Meaning                                        |
| --------------- | ---------------------------------------------- |
| `AIM <az> <el>` | Set both target angles at once (degrees)       |
| `AZ <deg>`      | Set azimuth target                             |
| `EL <deg>`      | Set elevation target                           |
| `CENTER`        | Move both axes to their home angle             |
| `STOP`          | Freeze both axes at their current position     |
| `GET`           | Request an immediate status notification       |

Angles outside the configured limits are clamped and acknowledged with an `ERR`.

### Status (notifications from the Status characteristic)

| Message                        | Meaning                                        |
| ------------------------------ | ---------------------------------------------- |
| `POS <az> <el> MOVING`         | Current angles; still slewing toward target    |
| `POS <az> <el> IDLE`           | Current angles; target reached                 |
| `ERR <text>`                   | Rejected command or clamped angle              |

While moving, `POS ... MOVING` is streamed roughly every 100 ms, followed by a
single `POS ... IDLE` on arrival.

### Example session

```
<- AIM 120 60
-> POS 90.0 90.0 MOVING
-> POS 108.3 78.3 MOVING
-> POS 120.0 60.0 IDLE
```

## Mapping to the app

The CardinalPointer app talks to controllers through the `Transport` contract in
`core` (`Command` out, `StatusUpdate` in). A future `BleTransport` maps:

- `Command.SetCameraSwivel(deg)`     → `AZ <deg>`
- `Command.SetCameraDepression(deg)` → `EL <deg>`
- `StatusUpdate.CameraSwivel/CameraDepression` ← parsed from `POS` notifications

Because commands and status are plain text, the protocol is easy to exercise from
any generic BLE tool (e.g. nRF Connect) before the app adapter exists.
