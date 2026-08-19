package com.bitchat.android.calling.tier2

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** JSON payload carried inside the existing encrypted Nostr direct-message envelope. */
data class NostrCallSignal(
    val callId: String,
    val type: Type,
    val sdp: String? = null,
    val candidate: JsonObject? = null
) {
    enum class Type { OFFER, ANSWER, ICE_CANDIDATE, HANGUP, REJECT }

    fun encode(): String = Gson().toJson(
        buildMap {
            put("callId", callId)
            put("type", type.wireValue)
            sdp?.let { put("sdp", it) }
            candidate?.let { put("candidate", it) }
        }
    )

    companion object {
        private const val PREFIX = "bitcall1:"

        fun create(callId: ByteArray, type: Type, sdp: String? = null, candidate: JsonObject? = null): NostrCallSignal? {
            if (callId.size != 16) return null
            if (type == Type.OFFER || type == Type.ANSWER) {
                if (sdp.isNullOrBlank() || candidate != null) return null
            }
            if (type == Type.ICE_CANDIDATE && candidate == null) return null
            if ((type == Type.HANGUP || type == Type.REJECT) && (sdp != null || candidate != null)) return null
            return NostrCallSignal(callId.toHex(), type, sdp, candidate)
        }

        fun decode(content: String): NostrCallSignal? {
            if (!content.startsWith(PREFIX)) return null
            return runCatching {
                val json = JsonParser.parseString(content.removePrefix(PREFIX)).asJsonObject
                val callId = json.get("callId")?.asString ?: return null
                if (callId.length != 32 || callId.any { it.digitToIntOrNull(16) == null }) return null
                val wireType = json.get("type")?.asString ?: return null
                val type = Type.entries.firstOrNull { it.wireValue == wireType } ?: return null
                val sdp = json.get("sdp")?.takeUnless { it.isJsonNull }?.asString
                val candidate = json.getAsJsonObject("candidate")
                create(callId.hexToBytes(), type, sdp, candidate)
            }.getOrNull()
        }

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
        private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        private val Type.wireValue: String
            get() = name.lowercase().replace('_', '-')
    }
}