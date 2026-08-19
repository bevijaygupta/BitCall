package com.bitchat.android.calling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallManagerTest {
    @Test
    fun meshPeerWithAwareSupportUsesMeshTier() {
        val manager = CallManager(meshPeerIds = { setOf("peer-a") }, wifiAwareSupported = { true })

        assertEquals(CallTier.MESH, manager.selectTier("peer-a"))
        assertTrue(manager.beginOutgoing("peer-a")!!.size == CallSignalingPacket.CALL_ID_SIZE)
        assertTrue(manager.state.value is CallState.Connecting)
    }

    @Test
    fun unsupportedAwareOrMissingPeerFallsBackToInternetTier() {
        val manager = CallManager(meshPeerIds = { setOf("peer-a") }, wifiAwareSupported = { false })

        assertEquals(CallTier.INTERNET, manager.selectTier("peer-a"))
        assertEquals(CallTier.INTERNET, manager.selectTier("peer-b"))
    }
}