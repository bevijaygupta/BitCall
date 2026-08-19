package com.bitchat.android.calling.tier1

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat

/** Platform Opus capability wrapper used by the Tier 1 audio pipeline. */
class OpusStreamCodec {
    companion object {
        const val SAMPLE_RATE = 48_000
        const val CHANNEL_COUNT = 1
        const val DEFAULT_BITRATE = 20_000

        fun isSupported(): Boolean = runCatching {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.any { info ->
                !info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_AUDIO_OPUS, true) }
            } && MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.any { info ->
                info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_AUDIO_OPUS, true) }
            }
        }.getOrDefault(false)
    }

    fun createEncoder(bitrate: Int = DEFAULT_BITRATE): MediaCodec {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, SAMPLE_RATE, CHANNEL_COUNT).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate.coerceIn(16_000, 24_000))
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, SAMPLE_RATE / 50 * 2)
        }
        return MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
            .firstOrNull { info ->
                info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_AUDIO_OPUS, true) }
            }
            ?.let { MediaCodec.createByCodecName(it.name).also { codec -> codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE) } }
            ?: MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS).also {
                it.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }
    }
}