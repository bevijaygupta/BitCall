package com.bitchat.android.calling

enum class CallTier { MESH, INTERNET }

sealed interface CallState {
    data object Idle : CallState
    data class Ringing(val callId: ByteArray, val peerId: String, val incoming: Boolean) : CallState
    data class Connecting(val callId: ByteArray, val peerId: String, val tier: CallTier) : CallState
    data class Active(val callId: ByteArray, val peerId: String, val tier: CallTier, val startedAtMs: Long) : CallState
    data class Ended(val callId: ByteArray, val peerId: String, val reason: EndReason) : CallState
}

enum class EndReason { HANGUP, REJECTED, FAILED }