package com.bitchat.android.calling.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bitchat.android.calling.CallTier
import com.bitchat.android.calling.CallManagerHolder
import com.bitchat.android.ui.theme.BitchatTheme

class IncomingCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val caller = intent.getStringExtra(EXTRA_CALLER_NAME).orEmpty().ifBlank { "BitCall" }
        setContent {
            BitchatTheme {
                CallScreen(
                    peerName = caller,
                    tier = CallTier.MESH,
                    incoming = true,
                    onAccept = {
                        CallManagerHolder.manager?.acceptIncoming()
                        setResult(RESULT_OK)
                        finish()
                    },
                    onDecline = {
                        CallManagerHolder.manager?.rejectIncoming()
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_CALLER_NAME = "com.bitchat.android.calling.CALLER_NAME"
        const val EXTRA_CALL_ID = "com.bitchat.android.calling.CALL_ID"
    }
}