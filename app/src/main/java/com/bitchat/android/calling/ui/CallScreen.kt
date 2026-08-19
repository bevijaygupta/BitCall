package com.bitchat.android.calling.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitchat.android.R
import com.bitchat.android.calling.CallTier

@Composable
fun CallScreen(
    peerName: String,
    tier: CallTier,
    incoming: Boolean = false,
    onAccept: () -> Unit = {},
    onDecline: () -> Unit = {},
    onHangup: () -> Unit = {}
) {
    var muted by remember { mutableStateOf(false) }
    var speakerEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(peerName, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(if (tier == CallTier.MESH) stringResource(R.string.call_mesh_badge) else stringResource(R.string.call_internet_badge))
        Spacer(Modifier.height(32.dp))
        if (incoming) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onAccept) { Text(stringResource(R.string.call_accept)) }
                Button(onClick = onDecline) { Text(stringResource(R.string.call_decline)) }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { muted = !muted }) {
                    Icon(if (muted) Icons.Default.MicOff else Icons.Default.Mic, stringResource(R.string.call_mute))
                }
                IconButton(onClick = { speakerEnabled = !speakerEnabled }) {
                    Icon(Icons.Default.VolumeUp, stringResource(R.string.call_speaker))
                }
                IconButton(onClick = onHangup) {
                    Icon(Icons.Default.CallEnd, stringResource(R.string.call_hangup))
                }
            }
        }
    }
}