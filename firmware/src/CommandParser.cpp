#include "CommandParser.h"

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

    // Split into up to three whitespace-separated tokens.
    String tokens[3];
    int count = 0;
    int i = 0;
    const int n = line.length();
    while (i < n && count < 3) {
        while (i < n && isspace(line[i])) i++;
        const int start = i;
        while (i < n && !isspace(line[i])) i++;
        if (i > start) {
            tokens[count++] = line.substring(start, i);
        }
    }

    String verb = tokens[0];
    verb.toUpperCase();

    if (verb == "CENTER") {
        ParsedCommand cmd;
        cmd.type = CommandType::Center;
        return cmd;
    }
    if (verb == "STOP") {
        ParsedCommand cmd;
        cmd.type = CommandType::Stop;
        return cmd;
    }
    if (verb == "GET") {
        ParsedCommand cmd;
        cmd.type = CommandType::Get;
        return cmd;
    }

    if (verb == "AIM") {
        if (count < 3) {
            return fail("AIM needs <az> <el>");
        }
        ParsedCommand cmd;
        cmd.type = CommandType::Aim;
        if (!parseFloat(tokens[1], cmd.azimuth) || !parseFloat(tokens[2], cmd.elevation)) {
            return fail("AIM angles not numeric");
        }
        return cmd;
    }

    if (verb == "AZ") {
        if (count < 2) {
            return fail("AZ needs <deg>");
        }
        ParsedCommand cmd;
        cmd.type = CommandType::Azimuth;
        if (!parseFloat(tokens[1], cmd.azimuth)) {
            return fail("AZ angle not numeric");
        }
        return cmd;
    }

    if (verb == "EL") {
        if (count < 2) {
            return fail("EL needs <deg>");
        }
        ParsedCommand cmd;
        cmd.type = CommandType::Elevation;
        if (!parseFloat(tokens[1], cmd.elevation)) {
            return fail("EL angle not numeric");
        }
        return cmd;
    }

    return fail("unknown command: " + verb);
}
