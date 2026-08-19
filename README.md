# BitCall

BitCall is a private, account-free Android app for messaging and voice calls over nearby devices
and available internet connections.

It is designed around a simple idea: people should be able to communicate without a phone
number, SIM card, recharge, or centralized account.

## What BitCall Does

- Sends private messages over encrypted local mesh connections.
- Discovers nearby devices through Bluetooth Low Energy.
- Uses Wi-Fi Aware for higher-bandwidth local links when supported.
- Routes internet communication through Nostr relays.
- Supports identity-based contacts instead of phone numbers.
- Provides local channels, media sharing, voice notes, and encrypted conversations.
- Adds experimental no-number voice calling with Mesh and Internet tiers.

## Calling Progress

The calling work is active and experimental. The current repository contains:

- Call identity and lifecycle state management.
- Encrypted mesh signaling for ring, accept, reject, acknowledge, and hangup events.
- Local-first Mesh tier selection with Wi-Fi Aware capability detection.
- Opus capability detection and experimental full-duplex audio capture/playback.
- Shared Wi-Fi Aware socket multiplexing for mesh packets and call audio.
- Microphone audio focus, communication mode, speaker routing, echo cancellation, and noise
  suppression.
- Incoming-call UI with lock-screen support.
- Call foreground service and peer-list call actions.
- Strict protocol codecs and focused unit tests.
- Initial Nostr call-signaling codec and mutual-favorite admission foundation.

Calling is not production-ready. Real two-device testing is still required for Wi-Fi Aware,
microphone behavior, teardown, interruptions, and different Android hardware. Internet calling
still needs its complete signaling integration, WebRTC media engine, STUN/TURN configuration, and
network failure testing.

## Important Limits

- A distant device cannot be reached without a network path.
- Offline calling works only when devices can reach one another through the local mesh.
- Wi-Fi availability and data cost depend on the network being used.
- BitCall is not an emergency-calling replacement.

## Technology

- Kotlin and Jetpack Compose
- MVVM with Coroutines and Flow
- Bluetooth Low Energy mesh networking
- Wi-Fi Aware local transport
- Nostr relay transport
- Noise sessions for encrypted local communication
- Opus audio for live calling
- Android foreground services for background call continuity

## Build

Requirements:

- Android Studio
- Android SDK API 26 or newer
- JDK 21 with `javac`

```bash
git clone https://github.com/bevijaygupta/BitCall.git
cd BitCall
./gradlew assembleDebug
```

Install a debug build on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The app requests the Bluetooth, nearby-device, microphone, location, Wi-Fi, and notification
permissions required by the enabled features.

## Test

```bash
# Unit tests
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug

# Instrumented tests
./gradlew connectedAndroidTest
```

Radio behavior, Wi-Fi Aware links, microphone routing, and call teardown must be tested on real
devices. Emulators are useful for protocol and UI tests but cannot represent every hardware path.

## Contribute

BitCall needs people who can test, review, design, and build. Contributions are welcome from:

- Android and Kotlin developers
- Audio, Opus, and WebRTC engineers
- Mesh networking and security researchers
- UX and accessibility contributors
- People testing different Android phones and Wi-Fi Aware chipsets

Good first contributions include:

- Testing a call between two real Android devices.
- Improving malformed-packet and call-teardown tests.
- Reviewing [docs/CALLING_V1.md](docs/CALLING_V1.md).
- Completing Nostr signaling and WebRTC integration.
- Improving call UI, interruptions, Bluetooth headset routing, and battery behavior.
- Opening focused bug reports and pull requests.

Before contributing, read [AGENTS.md](AGENTS.md), [docs/testing-conventions.md](docs/testing-conventions.md),
and the relevant documentation under `docs/`. Never include credentials, private device data,
peer IDs, or raw logs in issues, tests, commits, or screenshots.

## License

This project is released into the public domain. See [LICENSE.md](LICENSE.md).
