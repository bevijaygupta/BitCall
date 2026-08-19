package com.bitchat.android.calling.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Bundle
import com.bitchat.android.calling.tier1.OpusStreamCodec
import com.bitchat.android.calling.tier1.WifiAwareAudioSession
import com.bitchat.android.wifiaware.SyncedSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/** Full-duplex platform Opus capture/playback for an established Tier 1 socket. */
class CallMediaEngine(
    private val socket: SyncedSocket,
    private val audioRouter: CallAudioRouter,
    private val scope: CoroutineScope
) {
    companion object {
        private const val SAMPLE_RATE = OpusStreamCodec.SAMPLE_RATE
        private const val FRAME_SAMPLES = SAMPLE_RATE / 50
        private const val FRAME_BYTES = FRAME_SAMPLES * 2
        private const val CODEC_TIMEOUT_US = 10_000L
    }

    private val running = AtomicBoolean(false)
    private var session: WifiAwareAudioSession? = null
    private var captureJob: Job? = null
    private var recorder: AudioRecord? = null
    private var encoder: MediaCodec? = null
    private var decoder: MediaCodec? = null
    private var track: AudioTrack? = null
    val audioSession: WifiAwareAudioSession?
        get() = session

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (!running.compareAndSet(false, true)) return true
        return try {
            val minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0 || !OpusStreamCodec.isSupported()) error("Opus or microphone unavailable")
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer * 2, FRAME_BYTES * 4)
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) error("Microphone unavailable")
            val outputBuffer = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(FRAME_BYTES * 4)
            val audioTrack = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(outputBuffer)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            val op = OpusStreamCodec()
            val enc = op.createEncoder().apply { start() }
            val decFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, SAMPLE_RATE, 1)
            val dec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS).apply {
                configure(decFormat, null, null, 0)
                start()
            }
            if (!audioRouter.start()) error("Audio focus unavailable")
            recorder = record
            track = audioTrack
            encoder = enc
            decoder = dec
            session = WifiAwareAudioSession(socket, ::onEncodedFrame) { stop() }.also { it.start() }
            record.startRecording()
            audioTrack.play()
            captureJob = scope.launch(Dispatchers.IO) { captureLoop() }
            true
        } catch (_: Throwable) {
            stop()
            false
        }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        captureJob?.cancel()
        captureJob = null
        runCatching { recorder?.stop() }
        runCatching { track?.stop() }
        session?.close()
        session = null
        encoder?.release()
        decoder?.release()
        track?.release()
        recorder?.release()
        encoder = null
        decoder = null
        track = null
        recorder = null
        audioRouter.stop()
    }

    private fun captureLoop() {
        val pcm = ByteArray(FRAME_BYTES)
        val info = MediaCodec.BufferInfo()
        while (running.get()) {
            val read = recorder?.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING) ?: break
            if (read != FRAME_BYTES) continue
            val codec = encoder ?: break
            val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
            if (inputIndex >= 0) {
                codec.getInputBuffer(inputIndex)?.let { input ->
                    input.clear()
                    input.order(ByteOrder.LITTLE_ENDIAN).put(pcm)
                    codec.queueInputBuffer(inputIndex, 0, pcm.size, System.nanoTime() / 1_000, 0)
                }
            }
            drainEncoder(codec, info)
        }
    }

    private fun drainEncoder(codec: MediaCodec, info: MediaCodec.BufferInfo) {
        while (true) {
            when (val index = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> return
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                else -> if (index >= 0) {
                    if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        codec.getOutputBuffer(index)?.let { output ->
                            val frame = ByteArray(info.size)
                            output.position(info.offset)
                            output.get(frame)
                            session?.sendOpusFrame(frame)
                        }
                    }
                    codec.releaseOutputBuffer(index, false)
                }
            }
        }
    }

    private fun onEncodedFrame(sequence: Long, payload: ByteArray) {
        val codec = decoder ?: return
        val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
        if (inputIndex < 0) return
        codec.getInputBuffer(inputIndex)?.let { input ->
            input.clear()
            input.put(payload)
            codec.queueInputBuffer(inputIndex, 0, payload.size, sequence * 20_000L, 0)
        }
        val info = MediaCodec.BufferInfo()
        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)
            if (outputIndex < 0) break
            codec.getOutputBuffer(outputIndex)?.let { output ->
                val pcm = ByteArray(info.size)
                output.position(info.offset)
                output.get(pcm)
                track?.write(pcm, 0, pcm.size)
            }
            codec.releaseOutputBuffer(outputIndex, false)
        }
    }
}