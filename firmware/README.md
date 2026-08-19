# CardinalPointer ESP32 Servo Interface

Firmware for an ESP32 that receives aiming orders over **BLE** and drives **8
standard 5V PWM servos** (azimuth + elevation for each of 4 cameras) to align
the mount. Built with **PlatformIO** / Arduino framework and **NimBLE**.

Tracks issue [#8](https://github.com/MajorTwip/CardinalPointer/issues/8).

## Hardware

Camera index (0-3) matches the app's `CameraDirection.entries` order: North,
East, South, West.

| Camera | Azimuth GPIO | Elevation GPIO |
| ------ | ------------ | --------------- |
| 0 (North) | `18` | `23` |
| 1 (East)  | `19` | `25` |
| 2 (South) | `21` | `26` |
| 3 (West)  | `22` | `27` |

> **These GPIOs are placeholders** — confirm/adjust against your actual wiring
> in [`src/config.h`](src/config.h) before flashing.

| Signal    | Notes                                   |
| --------- | ---------------------------------------- |
| Servo V+  | external 5V — **not** the ESP32 3V3 pin |
| Servo GND | common GND, must be tied to ESP32 GND   |

Pulse-width calibration, angle limits, and slew rate (shared across all 4
cameras) also live in [`src/config.h`](src/config.h).

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

The firmware requests a 185-byte ATT MTU on connect (`NimBLEDevice::setMTU`)
since 4-camera status lines exceed the default 20-byte usable payload; a
central that doesn't grant a larger MTU will still work with the default
Bluedroid/NimBLE fallback, but lines may be truncated.

### Commands (write to the Command characteristic)

One ASCII command per write, case-insensitive, whitespace-separated. Every
verb takes a leading camera id (`0-3`):

| Command               | Meaning                                        |
| ---------------------- | ---------------------------------------------- |
| `AIM <cam> <az> <el>` | Set both target angles at once (degrees)       |
| `AZ <cam> <deg>`      | Set azimuth target                             |
| `EL <cam> <deg>`      | Set elevation target                           |
| `CENTER <cam>`        | Move both axes to their home angle             |
| `STOP <cam>`          | Freeze both axes at their current position     |
| `GET <cam>`           | Request an immediate status notification       |

Angles outside the configured limits are clamped and acknowledged with an `ERR`.
A missing/out-of-range camera id, or an unknown verb, is also reported as `ERR`
(without a camera id, since none could be determined).

### Status (notifications from the Status characteristic)

| Message                          | Meaning                                        |
| --------------------------------- | ---------------------------------------------- |
| `POS <cam> <az> <el> MOVING`     | Current angles for camera `<cam>`; still slewing |
| `POS <cam> <az> <el> IDLE`       | Current angles for camera `<cam>`; target reached |
| `ERR <cam> <text>`               | Rejected command or clamped angle for camera `<cam>` |
| `ERR <text>`                     | Malformed command (no camera id could be parsed) |

While moving, `POS ... MOVING` is streamed roughly every 100 ms per camera,
followed by a single `POS ... IDLE` on arrival.

### Example session

```
<- AIM 0 120 60
-> POS 0 90.0 90.0 MOVING
-> POS 0 108.3 78.3 MOVING
-> POS 0 120.0 60.0 IDLE
```

## Mapping to the app

The CardinalPointer app talks to controllers through the `Transport` contract in
`core` (`Command` out, `StatusUpdate` in). This protocol is implemented by
`core/.../transport/ble/BleTransport` + `ServoCodec`:

- `Command.SetCameraSwivel(direction, deg)`     → `AZ <channel> <deg>`
- `Command.SetCameraDepression(direction, deg)` → `EL <channel> <deg>`
- `POS <cam> <az> <el> …` notification           → `StatusUpdate.CameraSwivel` + `CameraDepression`, attributed to the `CameraDirection` at index `<cam>`
- `ERR <cam> <text>` / `ERR <text>`              → `StatusUpdate.Error`

`channel`/`<cam>` is the `CameraDirection`'s ordinal (`CameraDirection.entries`
order: North=0, East=1, South=2, West=3) — one `BleTransport` instance (one BLE
connection) addresses all 4 cameras.

Mast-oriented commands (`SetMastHeight`, `Erect`, `Fold`) have no meaning for
this device and are rejected by the transport.

`BleTransport` runs on any platform via the `BlePeripheral` abstraction; the
Android GATT adapter is `androidApp/.../ble/AndroidBlePeripheral`. Because
commands and status are plain text, the protocol is also easy to exercise from a
generic BLE tool (e.g. nRF Connect) — just remember to include a camera id.
