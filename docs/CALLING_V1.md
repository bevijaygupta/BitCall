# BitCall v1 wire protocol

BitCall adds one-to-one call signaling without changing the existing mesh, Noise, or Nostr
protocols. A call has a random 16-byte `callId` that remains stable until hangup.

## Type assignments

| Layer | Name | Value | Purpose |
| --- | --- | --- | --- |
| Outer mesh packet | `MessageType.CALL_SIGNAL` | `0x2A` | Reserved for call signaling envelopes |
| Noise inner payload | `NoisePayloadType.CALL_SIGNAL` | `0x22` | Encrypted call signaling |
| Outer private packet | `MessageType.NOISE_ENCRYPTED` | `0x11` | Existing recipient-directed Noise envelope |

Tier 1 signaling is sent as `NOISE_ENCRYPTED` carrying a `NoisePayload` of type `CALL_SIGNAL`.
The outer `CALL_SIGNAL` value is reserved for compatible transport adapters; new senders must
prefer the encrypted envelope so signaling is never public.

## CallSignalingPacket

All multi-byte integers use big-endian order:

```text
[callId: 16 bytes][signalType: UInt8][payload]
```

`RING`, `HANGUP`, and `RINGING_ACK` have no payload. `REJECT` carries one byte: `0` declined or
`1` busy. `ACCEPT` carries two UTF-8 strings, each encoded as `[length: UInt16 BE][bytes]`: the
Wi-Fi Aware passphrase followed by the Wi-Fi Aware service ID. Invalid signal/payload pairs,
truncated lengths, unknown values, invalid UTF-8, and trailing bytes are rejected.

## Tier policy

The caller chooses Mesh only when the target is in the current mesh peer set and both devices
support Wi-Fi Aware. Otherwise the call manager selects Internet for the later Nostr/WebRTC
phase. No phone number, account, or centralized call log is introduced.

Phase 1 adds the lifecycle codec and policy foundation. Wi-Fi Aware NDP audio, foreground call
service, and call UI must be wired only after their platform-specific transport APIs are tested
on physical devices.

## Tier 1 audio multiplexing

Tier 1 audio shares the established Wi-Fi Aware `SyncedSocket` with mesh traffic. Audio frames
use a private `BCA1` magic prefix followed by a 32-bit sequence number, a 16-bit Opus payload
length, two reserved bytes, and the Opus access unit. The Aware listener detects this prefix
before attempting mesh packet decoding, so only one reader owns the socket. Payloads above 4096
bytes or malformed frames are rejected and the session is closed.