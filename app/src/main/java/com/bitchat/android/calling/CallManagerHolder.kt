package com.bitchat.android.calling

/** Process-local bridge for the lock-screen activity to reach the active ViewModel call manager. */
object CallManagerHolder {
    @Volatile
    var manager: CallManager? = null
}