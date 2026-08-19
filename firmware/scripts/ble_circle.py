"""Drive the CardinalPointer-Servo rig in a circle over BLE.

The az/el servos are standard 0-180deg positional servos, not continuous
rotation, so "circles" here means tracing a circular path in (azimuth,
elevation) space, centered on home, repeatedly.

Usage:
    python ble_circle.py [--radius 60] [--revolutions 3] [--period 4]
"""
import argparse
import asyncio
import math
import sys
import time

from bleak import BleakClient, BleakScanner

sys.stdout.reconfigure(errors="replace")

DEVICE_NAME = "CardinalPointer-Servo"
CMD_CHAR_UUID = "6b1a0002-8f3a-4b7d-9c2e-0a1b2c3d4e5f"
STATUS_CHAR_UUID = "6b1a0003-8f3a-4b7d-9c2e-0a1b2c3d4e5f"

CENTER_AZ = 90.0
CENTER_EL = 90.0
STEP_HZ = 10  # setpoints per second sent to the device


def on_status(_handle, data: bytearray):
    print(f"<- {data.decode('utf-8', errors='replace')}")


async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--radius", type=float, default=60.0,
                         help="degrees of radius around center (default 60)")
    parser.add_argument("--revolutions", type=float, default=3.0,
                         help="number of full circles to trace (default 3)")
    parser.add_argument("--period", type=float, default=4.0,
                         help="seconds per revolution (default 4)")
    args = parser.parse_args()

    print(f"Scanning for '{DEVICE_NAME}'...")
    device = await BleakScanner.find_device_by_name(DEVICE_NAME, timeout=15.0)
    if device is None:
        print("Device not found. Is it powered and advertising?", file=sys.stderr)
        sys.exit(1)

    print(f"Found {device.address}, connecting...")
    async with BleakClient(device) as client:
        print("Connected.")
        await client.start_notify(STATUS_CHAR_UUID, on_status)

        total_duration = args.revolutions * args.period
        step_dt = 1.0 / STEP_HZ
        start = time.monotonic()

        print(f"Tracing circle: radius={args.radius} deg, "
              f"{args.revolutions} rev @ {args.period}s/rev")

        while True:
            elapsed = time.monotonic() - start
            if elapsed >= total_duration:
                break
            theta = 2 * math.pi * (elapsed / args.period)
            az = CENTER_AZ + args.radius * math.cos(theta)
            el = CENTER_EL + args.radius * math.sin(theta)
            await client.write_gatt_char(
                CMD_CHAR_UUID, f"AIM {az:.1f} {el:.1f}".encode(), response=False)
            await asyncio.sleep(step_dt)

        print("Returning to center...")
        await client.write_gatt_char(CMD_CHAR_UUID, b"CENTER", response=False)
        await asyncio.sleep(2)

        await client.stop_notify(STATUS_CHAR_UUID)
    print("Disconnected.")


if __name__ == "__main__":
    asyncio.run(main())
