#pragma once

#include <ESP32Servo.h>

/**
 * One rate-limited servo axis.
 *
 * setTarget() records where we want to be; update() nudges the physical servo
 * toward that target no faster than `slewDegPerSec`, writing the servo on every
 * call. Targets are clamped to [minDeg, maxDeg]; setTarget() reports via its
 * return value whether the request had to be clamped.
 */
class ServoAxis {
public:
    ServoAxis(int pin, float minDeg, float maxDeg, float homeDeg);

    void begin();

    /** Request a new target angle. Returns false if the value was clamped. */
    bool setTarget(float deg);

    /** Freeze the axis wherever it currently is. */
    void stop();

    /**
     * Advance the physical position toward the target.
     * @param dtSec           seconds since the previous update
     * @param slewDegPerSec   max angular speed; <= 0 means jump instantly
     */
    void update(float dtSec, float slewDegPerSec);

    float target() const { return targetDeg_; }
    float current() const { return currentDeg_; }
    bool moving() const;

private:
    void writeServo(float deg);

    Servo servo_;
    const int pin_;
    const float minDeg_;
    const float maxDeg_;
    float targetDeg_;
    float currentDeg_;
};
