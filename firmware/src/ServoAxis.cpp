#include "ServoAxis.h"

#include "config.h"

#include <Arduino.h>
#include <math.h>

ServoAxis::ServoAxis(int pin, float minDeg, float maxDeg, float homeDeg)
    : pin_(pin),
      minDeg_(minDeg),
      maxDeg_(maxDeg),
      targetDeg_(constrain(homeDeg, minDeg, maxDeg)),
      currentDeg_(targetDeg_) {}

void ServoAxis::begin() {
    servo_.setPeriodHertz(SERVO_FREQ_HZ);
    servo_.attach(pin_, SERVO_MIN_US, SERVO_MAX_US);
    writeServo(currentDeg_);
}

bool ServoAxis::setTarget(float deg) {
    const float clamped = constrain(deg, minDeg_, maxDeg_);
    targetDeg_ = clamped;
    return clamped == deg;
}

void ServoAxis::stop() {
    targetDeg_ = currentDeg_;
}

bool ServoAxis::moving() const {
    return fabsf(targetDeg_ - currentDeg_) > 0.05f;
}

void ServoAxis::update(float dtSec, float slewDegPerSec) {
    if (!moving()) {
        return;
    }

    if (slewDegPerSec <= 0.0f || dtSec <= 0.0f) {
        currentDeg_ = targetDeg_;
    } else {
        const float maxStep = slewDegPerSec * dtSec;
        const float delta = targetDeg_ - currentDeg_;
        if (fabsf(delta) <= maxStep) {
            currentDeg_ = targetDeg_;
        } else {
            currentDeg_ += (delta > 0 ? maxStep : -maxStep);
        }
    }

    writeServo(currentDeg_);
}

void ServoAxis::writeServo(float deg) {
    const long us = lroundf(SERVO_MIN_US +
                            (deg - minDeg_) * (SERVO_MAX_US - SERVO_MIN_US) /
                                (maxDeg_ - minDeg_));
    servo_.writeMicroseconds(static_cast<int>(us));
}
