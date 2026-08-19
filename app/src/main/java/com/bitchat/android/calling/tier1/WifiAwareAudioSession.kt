package com.bitchat.android.calling.tier1

import com.bitchat.android.wifiaware.SyncedSocket
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/** Carries bounded, framed Opus access units over an existing Wi-Fi Aware data path. */
class WifiAwareAudioSession(
    private val socket: SyncedSocket,
    private val onFrame: (sequence: Long, payload: ByteArray) -> Unit,
    private val onClosed: (Throwable?) -> Unit = {}
) {
    companion object {
        private const val HEADER_SIZE = 12
        private const val MAX_OPUS_FRAME_BYTES = 4_096
        private const val MAGIC = 0x42434131

        fun isAudioFrame(frame: ByteArray): Boolean {
            if (frame.size < 4) return false
            return ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN).int == MAGIC
        }
    }

    private val closed = AtomicBoolean(false)
    private var sequence = 0L

    fun start() {
        // The owning Wi-Fi Aware listener reads the shared socket. Call frames are
        // multiplexed there and delivered through acceptIncomingFrame().
    }

    fun acceptIncomingFrame(frame: ByteArray): Boolean {
        if (closed.get() || !isAudioFrame(frame)) return false
        val decoded = decode(frame) ?: run {
            closeInternal(IOException("Malformed call audio frame"))
            return true
        }
        onFrame(decoded.first, decoded.second)
        return true
    }

    fun sendOpusFrame(payload: ByteArray): Boolean {
        if (closed.get() || payload.isEmpty() || payload.size > MAX_OPUS_FRAME_BYTES) return false
        val frame = ByteBuffer.allocate(HEADER_SIZE + payload.size)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(MAGIC)
            .putInt((sequence++).toInt())
            .putShort(payload.size.toShort())
            .putShort(0)
            .put(payload)
            .array()
        return try {
            socket.write(frame)
            true
        } catch (_: IOException) {
            close()
            false
        }
    }

    fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { socket.close() }
            onClosed(null)
        }
    }

    private fun closeInternal(error: Throwable?) {
        if (closed.compareAndSet(false, true)) {
            runCatching { socket.close() }
            onClosed(error)
        }
    }

    private fun decode(frame: ByteArray): Pair<Long, ByteArray>? {
        if (frame.size < HEADER_SIZE) return null
        val input = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN)
        if (input.int != MAGIC) return null
        val sequence = input.int.toLong() and 0xFFFF_FFFFL
        val length = input.short.toInt() and 0xFFFF
        input.short
        if (length !in 1..MAX_OPUS_FRAME_BYTES || input.remaining() != length) return null
        return sequence to ByteArray(length).also(input::get)
    }

}