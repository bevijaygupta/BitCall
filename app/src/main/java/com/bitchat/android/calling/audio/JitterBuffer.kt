package com.bitchat.android.calling.audio

import java.util.TreeMap

/** Small bounded sequence buffer for the raw Tier 1 audio path. */
class JitterBuffer<T>(private val capacity: Int = 64) {
    private val frames = TreeMap<Long, T>()

    @Synchronized
    fun offer(sequence: Long, frame: T): Boolean {
        if (frames.size >= capacity && !frames.containsKey(sequence)) return false
        frames[sequence] = frame
        return true
    }

    @Synchronized
    fun poll(sequence: Long): T? = frames.remove(sequence)

    @Synchronized
    fun clear() = frames.clear()

    @Synchronized
    fun size(): Int = frames.size
}