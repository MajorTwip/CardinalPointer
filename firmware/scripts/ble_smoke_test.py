"""Ad-hoc BLE smoke test for the CardinalPointer-Servo firmware.

Scans for the device, connects, subscribes to status notifications, and
sends a couple of commands so you can watch the servos move and see status
come back over BLE (not USB serial).

Usage:
    python ble_smoke_test.py
"""
import asyncio
import sys

from bleak import BleakClient, BleakScanner

sys.stdout.reconfigure(errors="replace")

DEVICE_NAME = "CardinalPointer-Servo"
CMD_CHAR_UUID = "6b1a0002-8f3a-4b7d-9c2e-0a1b2c3d4e5f"
STATUS_CHAR_UUID = "6b1a0003-8f3a-4b7d-9c2e-0a1b2c3d4e5f"


def on_status(_handle, data: bytearray):
    print(f"<- {data!r} -> {data.decode('utf-8', errors='replace')}")


async def main():
    print(f"Scanning for '{DEVICE_NAME}'...")
    device = await BleakScanner.find_device_by_name(DEVICE_NAME, timeout=15.0)
    if device is None:
        print("Device not found. Is it powered and advertising?", file=sys.stderr)
        sys.exit(1)

    print(f"Found {device.address}, connecting...")
    async with BleakClient(device) as client:
        print("Connected.")
        await client.start_notify(STATUS_CHAR_UUID, on_status)

        for cmd in ["GET", "AIM 120 60", "CENTER"]:
            print(f"-> {cmd}")
            await client.write_gatt_char(CMD_CHAR_UUID, cmd.encode(), response=False)
            await asyncio.sleep(3)

        await client.stop_notify(STATUS_CHAR_UUID)
    print("Disconnected.")


if __name__ == "__main__":
    asyncio.run(main())
