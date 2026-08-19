package com.bitchat.android.mesh

import com.bitchat.android.model.BitchatMessage
import com.bitchat.android.calling.CallSignalingPacket

/**
 * Shared mesh delegate interface for transport-agnostic callbacks.
 */
interface MeshDelegate {
    fun didReceiveMessage(message: BitchatMessage)
    fun didUpdatePeerList(peers: List<String>)
    fun didReceiveChannelLeave(channel: String, fromPeer: String)
    fun didReceiveDeliveryAck(messageID: String, recipientPeerID: String)
    fun didReceiveReadReceipt(messageID: String, recipientPeerID: String)
    fun didReceiveVerifyChallenge(peerID: String, payload: ByteArray, timestampMs: Long) {}
    fun didReceiveVerifyResponse(peerID: String, payload: ByteArray, timestampMs: Long) {}
    fun didReceiveCallSignal(peerID: String, signal: CallSignalingPacket) {}
    /** Current Noise generation either proved peer state or exhausted its 5-second watchdog. */
    fun didResolvePrivateMediaPolicy(peerID: String) {}
    fun decryptChannelMessage(encryptedContent: ByteArray, channel: String): String?
    fun getNickname(): String?
    fun isFavorite(peerID: String): Boolean
}
