---
name: pio
description: Build, upload, and monitor the CardinalPointer ESP32 servo firmware (firmware/) using the PlatformIO CLI. Use when asked to compile, flash, upload, or serial-monitor this firmware, or to find/verify the ESP32's COM port.
---

# PlatformIO build / upload / monitor

This repo's PlatformIO CLI is **not** on the global PATH — it lives in the
PlatformIO Core home directory's virtualenv. Prepend it before any `pio`
invocation (PowerShell and bash syntax below); do this once per shell/tool call:

```bash
# bash
export PATH="$HOME/.platformio/penv/Scripts:$PATH"
pio.exe --version
```

```powershell
# PowerShell
$env:PATH = "$env:USERPROFILE\.platformio\penv\Scripts;$env:PATH"
pio.exe --version
```

All commands below assume the working directory is `firmware/` (where
`platformio.ini` lives). Default env is `esp32dev` (generic ESP32 WROOM-32
DevKit); `esp32-s3-devkitc-1` is also defined for the S3 variant.

## 1. Find the board's serial port

```bash
pio.exe device list
```

The ESP32 WROOM board in this project shows up as a **CH340 USB-SERIAL**
device (Hardware ID `VID:PID=1A86:7523`), e.g. `COM12`. Ignore entries whose
description is "Standard Serial over Bluetooth link" — those are paired
Bluetooth COM ports, not the board. If more than one CH340-style device is
listed, ask the user which one before uploading.

## 2. Build

```bash
pio.exe run -e esp32dev
```

Drop `-e esp32dev` to build the `default_envs` target, or swap in
`esp32-s3-devkitc-1` for that board.

## 3. Upload

```bash
pio.exe run -e esp32dev -t upload --upload-port COM12
```

Always pass `--upload-port` explicitly with the port found in step 1 —
auto-detection can pick the wrong device when Bluetooth COM ports are also
present.

## 4. Monitor

```bash
pio.exe device monitor -p COM12 -b 115200
```

Baud rate matches `monitor_speed` in `platformio.ini` (currently 115200).
Serial monitor commands are long-running/interactive — run them with
`run_in_background` or in a way the user can interrupt (Ctrl+C), since they
don't exit on their own.

## Notes

- Pin mapping, servo calibration, angle limits, and BLE UUIDs live in
  `src/config.h` — check there before assuming hardware wiring matches
  defaults (currently azimuth=GPIO18, elevation=GPIO19).
- `pio.exe run -e esp32dev -t upload` without `--upload-port` will
  auto-detect, but prefer being explicit given the Bluetooth COM port noise
  on this machine.
