package com.bitchat.android.calling.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

data class CallHistoryEntry(
    val callId: String,
    val peerId: String,
    val peerName: String,
    val tier: String,
    val startedAt: Date,
    val durationMs: Long,
    val missed: Boolean
)

/** Process-local history surface; persistence can follow the existing local conversation store. */
class CallHistoryViewModel : ViewModel() {
    private val _entries = MutableStateFlow<List<CallHistoryEntry>>(emptyList())
    val entries: StateFlow<List<CallHistoryEntry>> = _entries.asStateFlow()

    fun add(entry: CallHistoryEntry) {
        _entries.value = listOf(entry) + _entries.value
    }

    fun clear() {
        _entries.value = emptyList()
    }
}