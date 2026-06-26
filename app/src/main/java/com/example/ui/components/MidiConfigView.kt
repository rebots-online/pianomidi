package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.midi.MyMidiDevice
import com.example.ui.clay.ClayButton
import com.example.ui.clay.ClayCard
import com.example.viewmodel.PianoViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MidiConfigView(
    viewModel: PianoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val midiManager = viewModel.midiDeviceManager

    val discoveredDevices by midiManager.discoveredDevices.collectAsState()
    val connectedDevice by midiManager.connectedDevice.collectAsState()
    val connectionStatus by midiManager.connectionStatus.collectAsState()

    // Required Bluetooth permissions depending on SDK version
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("midi_config_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Connection Status Card
        ClayCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF23252F)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MIDI DEVICE CONSOLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFD1DC)
                    )

                    // Status Badge
                    val statusColor = when (connectionStatus) {
                        "CONNECTED" -> Color(0xFFD5F3D6)
                        "CONNECTING..." -> Color(0xFFFFF2CC)
                        "SCANNING BLE..." -> Color(0xFFE2F0D9)
                        "DISCONNECTED" -> Color(0x22FFFFFF)
                        else -> Color(0xFFFFD1DC)
                    }
                    val statusTextColor = when (connectionStatus) {
                        "CONNECTED" -> Color(0xFF2E6330)
                        "CONNECTING..." -> Color(0xFF7F6000)
                        "SCANNING BLE..." -> Color(0xFF385723)
                        "DISCONNECTED" -> Color(0xAAFFFFFF)
                        else -> Color(0xFF8B0000)
                    }

                    Text(
                        text = connectionStatus,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = statusTextColor,
                        modifier = Modifier
                            .background(statusColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (connectedDevice != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x11FFFFFF), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "CONNECTED DEVICE:",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0x88FFFFFF)
                            )
                            Text(
                                text = connectedDevice?.name ?: "Unknown Instrument",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Protocol: ${connectedDevice?.type} MIDI standard channel 1",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0x55FFFFFF)
                            )
                        }

                        ClayButton(
                            onClick = { midiManager.disconnect() },
                            backgroundColor = Color(0xFFFFD1DC)
                        ) {
                            Text("DISCONNECT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B0000))
                        }
                    }
                } else {
                    Text(
                        text = "No hardware controller currently bound. Connect your digital piano via USB OTG cable or scan for Bluetooth LE MIDI keyboards to enjoy low-latency, tactile performance capabilities.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0x88FFFFFF)
                    )
                }
            }
        }

        // 2. Scan & Discover Controllers Panel
        ClayCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2A2C35)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DISCOVER CHANNELS (${discoveredDevices.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFC4E0E5)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { midiManager.refreshUsbDevices() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh USB",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        ClayButton(
                            onClick = {
                                if (hasPermissions) {
                                    midiManager.startBleScanning()
                                } else {
                                    launcher.launch(requiredPermissions)
                                }
                            },
                            backgroundColor = Color(0xFFC4E0E5),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("SCAN BLE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E4E54))
                        }
                    }
                }

                if (!hasPermissions) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x15FFD1DC), RoundedCornerShape(8.dp))
                            .clickable { launcher.launch(requiredPermissions) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Permissions Required",
                            tint = Color(0xFFFFD1DC),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Bluetooth permission is required to scan for BLE keyboards. Tap to grant.",
                            fontSize = 10.sp,
                            color = Color(0xFFFFD1DC),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Discovered Devices List
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    discoveredDevices.forEach { device ->
                        val isSelected = connectedDevice?.id == device.id
                        val typeBadgeBg = when (device.type) {
                            "USB" -> Color(0xFFC4E0E5)
                            "BLE" -> Color(0xFFFFD1DC)
                            else -> Color(0xFFECE6E2)
                        }
                        val typeBadgeText = when (device.type) {
                            "USB" -> Color(0xFF2E4E54)
                            "BLE" -> Color.Red.darken(0.15f)
                            else -> Color(0xFF7D7266)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0x22C4E0E5) else Color(0xFF1E2028))
                                .clickable { midiManager.connectToDevice(device) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = device.type,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = typeBadgeText,
                                    modifier = Modifier
                                        .background(typeBadgeBg, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )

                                Column {
                                    Text(
                                        text = device.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFC4E0E5) else Color.White
                                    )
                                    val subText = when (device) {
                                        is MyMidiDevice.UsbDevice -> "Manufacturer: USB General MIDI Class"
                                        is MyMidiDevice.BleDevice -> "Hardware MAC: ${device.device.address}"
                                        is MyMidiDevice.VirtualDevice -> "Synthesizer feedback loop emulator"
                                    }
                                    Text(
                                        text = subText,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0x44FFFFFF)
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Connected",
                                    tint = Color(0xFFC4E0E5),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Virtual Controller Simulator Board
        val isVirtualConnected = connectedDevice is MyMidiDevice.VirtualDevice
        AnimatedVisibility(visible = isVirtualConnected) {
            ClayCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFECE6E2) // Neutral warm light clay background
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "VIRTUAL CONTROLLER BOARD (SIMULATOR)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF7D7266)
                    )

                    Text(
                        text = "This board acts as a loopback virtual piano. Tap to trigger MIDI inputs to test rubato metrics, analytics trackers, and comment snippet recordings perfectly.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xAA7D7266)
                    )

                    // Chord trigger buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ClayButton(
                                onClick = {
                                    // C Major Chord: C4(60), E4(64), G4(67)
                                    midiManager.triggerVirtualNoteOn(60, 0.8f)
                                    midiManager.triggerVirtualNoteOn(64, 0.8f)
                                    midiManager.triggerVirtualNoteOn(67, 0.8f)
                                },
                                backgroundColor = Color(0xFFC4E0E5),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("C MAJOR CHORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E4E54))
                            }

                            ClayButton(
                                onClick = {
                                    // A Minor Chord: A3(57), C4(60), E4(64)
                                    midiManager.triggerVirtualNoteOn(57, 0.7f)
                                    midiManager.triggerVirtualNoteOn(60, 0.7f)
                                    midiManager.triggerVirtualNoteOn(64, 0.7f)
                                },
                                backgroundColor = Color(0xFFFFD1DC),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("A MINOR CHORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Red.darken(0.15f))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ClayButton(
                                onClick = {
                                    // F Major Chord: F3(53), A3(57), C4(60)
                                    midiManager.triggerVirtualNoteOn(53, 0.9f)
                                    midiManager.triggerVirtualNoteOn(57, 0.9f)
                                    midiManager.triggerVirtualNoteOn(60, 0.9f)
                                },
                                backgroundColor = Color(0xFFD5F3D6),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("F MAJOR CHORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E6330))
                            }

                            ClayButton(
                                onClick = {
                                    // G Major Chord: G3(55), B3(59), D4(62)
                                    midiManager.triggerVirtualNoteOn(55, 0.85f)
                                    midiManager.triggerVirtualNoteOn(59, 0.85f)
                                    midiManager.triggerVirtualNoteOn(62, 0.85f)
                                },
                                backgroundColor = Color(0xFFFFF2CC),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("G MAJOR CHORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7F6000))
                            }
                        }

                        ClayButton(
                            onClick = {
                                // Release all keys
                                for (p in 30..90) {
                                    midiManager.triggerVirtualNoteOff(p)
                                }
                            },
                            backgroundColor = Color(0xFFFBC4B6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("RELEASE ALL SUSTAINED KEYS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B0000), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

// Private helper extension for clay dark colors
private fun Color.darken(factor: Float): Color {
    return Color(
        red = (red * (1f - factor)).coerceIn(0f, 1f),
        green = (green * (1f - factor)).coerceIn(0f, 1f),
        blue = (blue * (1f - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
}
