#include "CommandParser.h"

#include "config.h"

namespace {

// Returns true and writes `out` if `token` is a valid decimal number.
bool parseFloat(const String& token, float& out) {
    if (token.length() == 0) {
        return false;
    }
    char* end = nullptr;
    const float value = strtod(token.c_str(), &end);
    if (end == token.c_str() || *end != '\0') {
        return false;
    }
    out = value;
    return true;
}

// Returns true and writes `out` (0..CAMERA_COUNT-1) if `token` is a valid camera id.
bool parseCamera(const String& token, int& out) {
    if (token.length() == 0) {
        return false;
    }
    char* end = nullptr;
    const long value = strtol(token.c_str(), &end, 10);
    if (end == token.c_str() || *end != '\0') {
        return false;
    }
    if (value < 0 || value >= CAMERA_COUNT) {
        return false;
    }
    out = static_cast<int>(value);
    return true;
}

ParsedCommand fail(const String& reason) {
    ParsedCommand cmd;
    cmd.type = CommandType::Error;
    cmd.error = reason;
    return cmd;
}

} // namespace

ParsedCommand parseCommand(const String& rawLine) {
    String line = rawLine;
    line.trim();
    if (line.length() == 0) {
        return ParsedCommand{}; // CommandType::None
    }

    // Split into up to four whitespace-separated tokens (verb + camera id +
    // up to two numeric args).
    String tokens[4];
    int count = 0;
    int i = 0;
    const int n = line.length();
    while (i < n && count < 4) {
        while (i < n && isspace(line[i])) i++;
        const int start = i;
        while (i < n && !isspace(line[i])) i++;
        if (i > start) {
            tokens[count++] = line.substring(start, i);
        }
    }

    String verb = tokens[0];
    verb.toUpperCase();

    if (verb == "CENTER" || verb == "STOP" || verb == "GET") {
        if (count < 2) {
            return fail(verb + " needs <cam>");
        }
        int camera;
        if (!parseCamera(tokens[1], camera)) {
            return fail(verb + " camera id invalid");
        }
        ParsedCommand cmd;
        cmd.camera = camera;
        cmd.type = verb == "CENTER" ? CommandType::Center
            : verb == "STOP" ? CommandType::Stop
            : CommandType::Get;
        return cmd;
    }

    if (verb == "AIM") {
        if (count < 4) {
            return fail("AIM needs <cam> <az> <el>");
        }
        int camera;
        if (!parseCamera(tokens[1], camera)) {
            return fail("AIM camera id invalid");
        }
        ParsedCommand cmd;
        cmd.type = CommandType::Aim;
        cmd.camera = camera;
        if (!parseFloat(tokens[2], cmd.azimuth) || !parseFloat(tokens[3], cmd.elevation)) {
            return fail("AIM angles not numeric");
        }
        return cmd;
    }

    if (verb == "AZ") {
        if (count < 3) {
            return fail("AZ needs <cam> <deg>");
        }
        int camera;
        if (!parseCamera(tokens[1], camera)) {
            return fail("AZ camera id invalid");
        }
        ParsedCommand cmd;
        cmd.type = CommandType::Azimuth;
        cmd.camera = camera;
        if (!parseFloat(tokens[2], cmd.azimuth)) {
            return fail("AZ angle not numeric");
        }
        return cmd;
    }

    if (verb == "EL") {
        if (count < 3) {
            return fail("EL needs <cam> <deg>");
        }
        int camera;
        if (!parseCamera(tokens[1], camera)) {
            return fail("EL camera id invalid");
        }
        ParsedCommand cmd;
        cmd.type = CommandType::Elevation;
        cmd.camera = camera;
        if (!parseFloat(tokens[2], cmd.elevation)) {
            return fail("EL angle not numeric");
        }
        return cmd;
    }

    return fail("unknown command: " + verb);
}
