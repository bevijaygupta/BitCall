package com.bitchat.android.calling

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Encrypted mesh call lifecycle message. Multi-byte values use big-endian order. */
class CallSignalingPacket private constructor(
    val callId: ByteArray,
    val signalType: SignalType,
    val payload: Payload
) {
    enum class SignalType(val value: UByte) {
        RING(0x01u),
        ACCEPT(0x02u),
        REJECT(0x03u),
        HANGUP(0x04u),
        RINGING_ACK(0x05u);

        companion object {
            fun fromValue(value: UByte): SignalType? = entries.firstOrNull { it.value == value }
        }
    }

    sealed interface Payload {
        data object None : Payload
        data class Accept(val wifiAwarePassphrase: String, val wifiAwareServiceId: String) : Payload
        data class Reject(val reasonCode: ReasonCode) : Payload
    }

    enum class ReasonCode(val value: UByte) {
        DECLINED(0u),
        BUSY(1u);

        companion object {
            fun fromValue(value: UByte): ReasonCode? = entries.firstOrNull { it.value == value }
        }
    }

    fun encode(): ByteArray {
        val output = ByteArrayOutputStream(CALL_ID_SIZE + 3)
        output.write(callId)
        output.write(signalType.value.toInt())
        when (val signalPayload = payload) {
            Payload.None -> Unit
            is Payload.Reject -> output.write(signalPayload.reasonCode.value.toInt())
            is Payload.Accept -> {
                writeString(output, signalPayload.wifiAwarePassphrase)
                writeString(output, signalPayload.wifiAwareServiceId)
            }
        }
        return output.toByteArray()
    }

    companion object {
        const val CALL_ID_SIZE = 16
        private const val STRING_LENGTH_SIZE = 2

        fun create(callId: ByteArray, signalType: SignalType, payload: Payload): CallSignalingPacket? {
            if (callId.size != CALL_ID_SIZE || !payloadMatches(signalType, payload)) return null
            if (payload is Payload.Accept &&
                (payload.wifiAwarePassphrase.toByteArray(Charsets.UTF_8).size > 0xFFFF ||
                    payload.wifiAwareServiceId.toByteArray(Charsets.UTF_8).size > 0xFFFF)
            ) return null
            return CallSignalingPacket(callId.copyOf(), signalType, payload)
        }

        fun decode(data: ByteArray): CallSignalingPacket? {
            if (data.size < CALL_ID_SIZE + 1) return null
            val callId = data.copyOfRange(0, CALL_ID_SIZE)
            val signalType = SignalType.fromValue(data[CALL_ID_SIZE].toUByte()) ?: return null
            val cursor = Cursor(data, CALL_ID_SIZE + 1)
            val payload = when (signalType) {
                SignalType.RING, SignalType.HANGUP, SignalType.RINGING_ACK -> Payload.None
                SignalType.REJECT -> {
                    val reason = cursor.readByte()?.let(ReasonCode::fromValue) ?: return null
                    Payload.Reject(reason)
                }
                SignalType.ACCEPT -> {
                    val passphrase = cursor.readString() ?: return null
                    val serviceId = cursor.readString() ?: return null
                    Payload.Accept(passphrase, serviceId)
                }
            }
            if (cursor.position != data.size) return null
            return create(callId, signalType, payload)
        }

        private fun payloadMatches(signalType: SignalType, payload: Payload): Boolean = when (signalType) {
            SignalType.ACCEPT -> payload is Payload.Accept
            SignalType.REJECT -> payload is Payload.Reject
            SignalType.RING, SignalType.HANGUP, SignalType.RINGING_ACK -> payload == Payload.None
        }

        private fun writeString(output: ByteArrayOutputStream, value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            output.write(bytes.size ushr 8)
            output.write(bytes.size and 0xFF)
            output.write(bytes)
        }

        private class Cursor(private val data: ByteArray, var position: Int) {
            fun readByte(): UByte? = if (position < data.size) data[position++].toUByte() else null

            fun readString(): String? {
                if (data.size - position < STRING_LENGTH_SIZE) return null
                val length = ((data[position].toInt() and 0xFF) shl 8) or
                    (data[position + 1].toInt() and 0xFF)
                position += STRING_LENGTH_SIZE
                if (data.size - position < length) return null
                val value = runCatching {
                    Charsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(data, position, length))
                        .toString()
                }.getOrNull() ?: return null
                position += length
                return value
            }
        }
    }
}