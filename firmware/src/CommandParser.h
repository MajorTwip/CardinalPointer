#pragma once

#include <Arduino.h>

/**
 * ASCII line protocol parser.
 *
 * The app writes one command per BLE write. Commands are case-insensitive and
 * whitespace-separated:
 *
 *   AIM <az> <el>   set both axes at once (degrees)
 *   AZ  <deg>       set azimuth target
 *   EL  <deg>       set elevation target
 *   CENTER          move both axes to their home angle
 *   STOP            freeze both axes at their current position
 *   GET             request an immediate status notification
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
    float azimuth = 0.0f;
    float elevation = 0.0f;
    String error;
};

/** Parse a single command line. Never throws; malformed input -> Error. */
ParsedCommand parseCommand(const String& line);
