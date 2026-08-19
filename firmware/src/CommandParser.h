#pragma once

#include <Arduino.h>

/**
 * ASCII line protocol parser.
 *
 * The app writes one command per BLE write. Commands are case-insensitive,
 * whitespace-separated, and every verb takes a leading camera id (0-3,
 * matching the app's `CameraDirection.entries` order: 0=North, 1=East,
 * 2=South, 3=West):
 *
 *   AIM <cam> <az> <el>   set both axes at once (degrees)
 *   AZ  <cam> <deg>       set azimuth target
 *   EL  <cam> <deg>       set elevation target
 *   CENTER <cam>          move both axes to their home angle
 *   STOP <cam>            freeze both axes at their current position
 *   GET <cam>             request an immediate status notification
 *
 * See firmware/README.md for the matching status protocol.
 */
enum class CommandType {
    None,     // blank / ignore
    Aim,      // hasAz && hasEl
    Azimuth,  // hasAz
    Elevation,// hasEl
    Center,
    Stop,
    Get,
    Error     // malformed; `error` holds the reason
};

struct ParsedCommand {
    CommandType type = CommandType::None;
    int camera = -1; // 0-3; unset (-1) for None/Error
    float azimuth = 0.0f;
    float elevation = 0.0f;
    String error;
};

/** Parse a single command line. Never throws; malformed input -> Error. */
ParsedCommand parseCommand(const String& line);
