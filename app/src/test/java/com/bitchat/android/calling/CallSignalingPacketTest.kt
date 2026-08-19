package com.bitchat.android.calling

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallSignalingPacketTest {
    private val callId = ByteArray(16) { (it + 1).toByte() }

    @Test
    fun ringGoldenVectorRoundTrips() {
        val packet = CallSignalingPacket.create(
            callId,
            CallSignalingPacket.SignalType.RING,
            CallSignalingPacket.Payload.None
        )!!
        assertArrayEquals(callId + byteArrayOf(0x01), packet.encode())
        val decoded = CallSignalingPacket.decode(packet.encode())!!
        assertArrayEquals(callId, decoded.callId)
        assertEquals(CallSignalingPacket.SignalType.RING, decoded.signalType)
    }

    @Test
    fun acceptUsesLengthPrefixedUtf8Fields() {
        val packet = CallSignalingPacket.create(
            callId,
            CallSignalingPacket.SignalType.ACCEPT,
            CallSignalingPacket.Payload.Accept("pass", "service")
        )!!
        assertArrayEquals(
            callId + byteArrayOf(0x02, 0, 4) + "pass".toByteArray() + byteArrayOf(0, 7) + "service".toByteArray(),
            packet.encode()
        )
        assertEquals(packet.payload, CallSignalingPacket.decode(packet.encode())!!.payload)
    }

    @Test
    fun rejectsMalformedAndMismatchedSignals() {
        assertNull(CallSignalingPacket.decode(ByteArray(16)))
        assertNull(CallSignalingPacket.decode(callId + byteArrayOf(0x01, 0x00)))
        assertNull(CallSignalingPacket.decode(callId + byteArrayOf(0x03, 0x02)))
        assertNull(
            CallSignalingPacket.create(
                callId,
                CallSignalingPacket.SignalType.RING,
                CallSignalingPacket.Payload.Reject(CallSignalingPacket.ReasonCode.BUSY)
            )
        )
    }
}