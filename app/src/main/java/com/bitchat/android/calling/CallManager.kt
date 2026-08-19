package com.bitchat.android.calling

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom

/** Phase 1 lifecycle and local-first tier policy; transport engines own actual media setup. */
class CallManager(
    private val meshPeerIds: () -> Set<String>,
    private val wifiAwareSupported: () -> Boolean,
    private val sendSignal: (peerId: String, signal: CallSignalingPacket) -> Unit = { _, _ -> },
    private val onIncomingRing: (peerId: String, callId: ByteArray) -> Unit = { _, _ -> },
    private val onActive: (CallState.Active) -> Unit = {},
    private val onEnded: (CallState.Ended) -> Unit = {}
) {
    private val random = SecureRandom()
    private val _state = MutableStateFlow<CallState>(CallState.Idle)
    val state: StateFlow<CallState> = _state.asStateFlow()

    fun selectTier(peerId: String): CallTier? = when {
        meshPeerIds().contains(peerId) && wifiAwareSupported() -> CallTier.MESH
        else -> CallTier.INTERNET
    }

    fun beginOutgoing(peerId: String): ByteArray? {
        val tier = selectTier(peerId) ?: return null
        val callId = ByteArray(CallSignalingPacket.CALL_ID_SIZE).also(random::nextBytes)
        _state.value = CallState.Connecting(callId, peerId, tier)
        CallSignalingPacket.create(
            callId,
            CallSignalingPacket.SignalType.RING,
            CallSignalingPacket.Payload.None
        )?.let { sendSignal(peerId, it) }
        return callId
    }

    fun receiveRing(callId: ByteArray, peerId: String): Boolean {
        if (callId.size != CallSignalingPacket.CALL_ID_SIZE || _state.value !is CallState.Idle) return false
        _state.value = CallState.Ringing(callId.copyOf(), peerId, incoming = true)
        CallSignalingPacket.create(
            callId,
            CallSignalingPacket.SignalType.RINGING_ACK,
            CallSignalingPacket.Payload.None
        )?.let { sendSignal(peerId, it) }
        onIncomingRing(peerId, callId.copyOf())
        return true
    }

    fun handleSignal(peerId: String, signal: CallSignalingPacket): Boolean {
        return when (signal.signalType) {
            CallSignalingPacket.SignalType.RING -> receiveRing(signal.callId, peerId)
            CallSignalingPacket.SignalType.RINGING_ACK -> _state.value is CallState.Connecting
            CallSignalingPacket.SignalType.ACCEPT -> {
                val current = _state.value
                if (current !is CallState.Connecting || !current.callId.contentEquals(signal.callId)) return false
                CallState.Active(signal.callId.copyOf(), peerId, current.tier, System.currentTimeMillis()).also {
                    _state.value = it
                    onActive(it)
                }
                true
            }
            CallSignalingPacket.SignalType.REJECT -> {
                val current = _state.value
                if (current !is CallState.Connecting || !current.callId.contentEquals(signal.callId)) return false
                CallState.Ended(signal.callId.copyOf(), peerId, EndReason.REJECTED).also {
                    _state.value = it
                    onEnded(it)
                }
                true
            }
            CallSignalingPacket.SignalType.HANGUP -> {
                val current = _state.value
                if (current !is CallState.Ringing && current !is CallState.Connecting && current !is CallState.Active) return false
                val currentCallId = when (current) {
                    is CallState.Ringing -> current.callId
                    is CallState.Connecting -> current.callId
                    is CallState.Active -> current.callId
                    else -> return false
                }
                if (!currentCallId.contentEquals(signal.callId)) return false
                CallState.Ended(signal.callId.copyOf(), peerId, EndReason.HANGUP).also {
                    _state.value = it
                    onEnded(it)
                }
                true
            }
        }
    }

    fun acceptIncoming(): Boolean {
        val current = _state.value as? CallState.Ringing ?: return false
        val signal = CallSignalingPacket.create(
            current.callId,
            CallSignalingPacket.SignalType.ACCEPT,
            CallSignalingPacket.Payload.Accept("", "")
        ) ?: return false
        sendSignal(current.peerId, signal)
        CallState.Active(current.callId.copyOf(), current.peerId, selectTier(current.peerId) ?: CallTier.MESH, System.currentTimeMillis()).also {
            _state.value = it
            onActive(it)
        }
        return true
    }

    fun rejectIncoming(reasonCode: CallSignalingPacket.ReasonCode = CallSignalingPacket.ReasonCode.DECLINED): Boolean {
        val current = _state.value as? CallState.Ringing ?: return false
        CallSignalingPacket.create(
            current.callId,
            CallSignalingPacket.SignalType.REJECT,
            CallSignalingPacket.Payload.Reject(reasonCode)
        )?.let { sendSignal(current.peerId, it) }
        CallState.Ended(current.callId.copyOf(), current.peerId, EndReason.REJECTED).also {
            _state.value = it
            onEnded(it)
        }
        return true
    }

    fun end(peerId: String, callId: ByteArray, reason: EndReason = EndReason.HANGUP) {
        CallSignalingPacket.create(callId, CallSignalingPacket.SignalType.HANGUP, CallSignalingPacket.Payload.None)
            ?.let { sendSignal(peerId, it) }
        CallState.Ended(callId.copyOf(), peerId, reason).also {
            _state.value = it
            onEnded(it)
        }
    }

    fun reset() {
        _state.value = CallState.Idle
    }
}