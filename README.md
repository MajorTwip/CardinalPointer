# CardinalPointer

Cross-platform control app architecture for a mast system with 4 cameras.

## Goals

- Run on **Android** and **Windows**
- Communicate with the mast controller over **USB serial** (Android + Windows)
- Be ready to add **Android Intent** transport later without rewriting core logic
- Show and remote-control:
  - Camera direction (cardinal camera selection + swivel)
  - Camera depression (tilt downward)
  - Mast target height
  - Mast erection / folding actions

## Proposed Architecture

### 1) Shared Core (platform-independent)

Keep all business logic in a shared core with no direct platform API calls:

- Domain models (`MastState`, `CameraState`, `Orientation`, limits)
- Use-cases (`SetCameraOrientation`, `SetMastHeight`, `StartErection`, `StartFolding`)
- Command/response protocol mapping
- Validation and clamping rules

### 2) Transport Abstraction

Define one transport contract in the core:

```text
Transport
  send(command) -> response
  subscribeStatusUpdates(callback)
```

Adapters:

- `UsbSerialTransport` (implemented first for Android + Windows)
- `AndroidIntentTransport` (planned, same contract, plug-in later)

This keeps UI and use-cases unchanged when intent-based integration is added.

### 3) Platform Shells

- **Android app shell**: native UI + USB serial adapter binding
- **Windows app shell**: native UI + USB serial adapter binding

Both shells call the same shared core API.

## Domain Model (required behavior)

### Camera topology

- 4 fixed camera mounts: `North`, `East`, `South`, `West`
- Per camera controls:
  - `swivelDegrees`: range **[-30, +30]**
  - `depressionDegrees`: range **[-45, 0]** (down is negative)

### Mast controls

- `targetHeight` (system-configured min/max limits)
- Actions:
  - `erect()`
  - `fold()`
- State tracking:
  - current height
  - motion state (`Idle`, `Erecting`, `Folding`, `Error`)

## Control Workflow

1. App subscribes to status stream via selected transport.
2. User selects camera and desired swivel/depression.
3. Core validates limits and emits command over transport.
4. User sets target mast height and starts erection/folding.
5. Core updates view-model state from incoming status responses.

## Extensibility for Android Intents

When intent details are known, add `AndroidIntentTransport` implementing the same `Transport` contract:

- Map core commands to intents
- Map intent responses/events back to status updates
- Reuse unchanged UI state handling and business rules

This satisfies the requirement to support intents later without architectural rework.