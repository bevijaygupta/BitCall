package com.bitchat.android.calling.tier2

import android.content.Context
import android.util.Log
import com.bitchat.android.favorites.FavoritesPersistenceService
import com.bitchat.android.nostr.NostrIdentityBridge
import com.bitchat.android.nostr.NostrProtocol
import com.bitchat.android.nostr.NostrRelayManager
import com.bitchat.android.services.ContactIdentityResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Phase 2 signaling adapter using existing encrypted NIP-17/NIP-44 relay plumbing. */
class NostrCallSignaling(
    private val context: Context,
    private val scope: CoroutineScope,
    private val senderPeerId: String,
    private val allowUnknownCallers: () -> Boolean = { false },
    private val onSignal: (senderPubkey: String, signal: NostrCallSignal) -> Unit
) {
    companion object { private const val TAG = "NostrCallSignaling" }

    fun send(peerId: String, signal: NostrCallSignal) {
        scope.launch(Dispatchers.IO) {
            val recipient = FavoritesPersistenceService.shared.findNostrPubkeyForPeerID(peerId)
                ?: return@launch
            val recipientHex = ContactIdentityResolver.nostrPubkeyHex(recipient) ?: return@launch
            val identity = NostrIdentityBridge.getCurrentNostrIdentity(context) ?: return@launch
            val content = signal.encode()
            NostrProtocol.createPrivateMessage(content, recipientHex, identity).forEach { event ->
                NostrRelayManager.registerPendingGiftWrap(event.id)
                NostrRelayManager.getInstance(context).sendEvent(event)
            }
        }
    }

    fun receive(senderPubkey: String, content: String): Boolean {
        val signal = NostrCallSignal.decode(content) ?: return false
        if (!allowUnknownCallers() && !isMutualFavorite(senderPubkey)) {
            Log.i(TAG, "Ignoring call signal from non-mutual favorite")
            return false
        }
        onSignal(senderPubkey, signal)
        return true
    }

    private fun isMutualFavorite(senderPubkey: String): Boolean = runCatching {
        FavoritesPersistenceService.shared.findNoiseKey(senderPubkey)
            ?.let { FavoritesPersistenceService.shared.getFavoriteStatus(it)?.isMutual == true }
            ?: false
    }.getOrDefault(false)
}