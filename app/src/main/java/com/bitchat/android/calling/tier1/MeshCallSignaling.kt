package com.bitchat.android.calling.tier1

import com.bitchat.android.calling.CallSignalingPacket
import com.bitchat.android.model.NoisePayload
import com.bitchat.android.model.NoisePayloadType

/** Bridges call signals onto the existing encrypted Noise payload path. */
class MeshCallSignaling(
    private val sendNoisePayload: (peerId: String, payload: NoisePayload) -> Unit,
    private val onSignal: (peerId: String, signal: CallSignalingPacket) -> Unit
) {
    fun send(peerId: String, signal: CallSignalingPacket) {
        sendNoisePayload(peerId, NoisePayload(NoisePayloadType.CALL_SIGNAL, signal.encode()))
    }

    /** Returns false so the mesh handler can treat malformed signaling as undeliverable. */
    fun receive(peerId: String, payload: ByteArray): Boolean {
        val signal = CallSignalingPacket.decode(payload) ?: return false
        onSignal(peerId, signal)
        return true
    }
}