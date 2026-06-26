package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Recording
import com.example.ui.clay.ClayButton
import com.example.ui.clay.ClayCard
import com.example.ui.clay.ClayToggle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaybackSequencer(
    isRecording: Boolean,
    isPlaying: Boolean,
    isMetronomeOn: Boolean,
    recordings: List<Recording>,
    selectedRecording: Recording?,
    onStartRecording: () -> Unit,
    onStopRecording: (String) -> Unit,
    onCancelRecording: () -> Unit,
    onStartPlayback: () -> Unit,
    onStopPlayback: () -> Unit,
    onSelectRecording: (Recording?) -> Unit,
    onDeleteRecording: (Recording) -> Unit,
    onMetronomeToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var recordingTitleInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("playback_sequencer_container"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Core Transport Action Controls
        ClayCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFFF5EBE0) // warm clay sand
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Record Button (balanced weight)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        ClayButton(
                            onClick = { 
                                recordingTitleInput = ""
                                showSaveDialog = true 
                            },
                            backgroundColor = Color(0xFFFBC4B6), // alerting coral
                            modifier = Modifier.fillMaxWidth().testTag("stop_record_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF8B0000))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("STOP REC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B0000))
                            }
                        }
                    } else {
                        ClayButton(
                            onClick = onStartRecording,
                            backgroundColor = Color(0xFFFFD1DC), // Soft clay red-pink
                            enabled = !isPlaying,
                            modifier = Modifier.fillMaxWidth().testTag("start_record_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.Red)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RECORD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red.darken(0.15f))
                            }
                        }
                    }
                }

                // 2. Playback Button (balanced weight - stays stable)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedRecording != null && !isRecording) {
                        if (isPlaying) {
                            ClayButton(
                                onClick = onStopPlayback,
                                backgroundColor = Color(0xFFC4E0E5),
                                modifier = Modifier.fillMaxWidth().testTag("stop_play_button")
                            ) {
                                Text("STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E4E54), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                        } else {
                            ClayButton(
                                onClick = onStartPlayback,
                                backgroundColor = Color(0xFFC4E0E5),
                                modifier = Modifier.fillMaxWidth().testTag("start_play_button")
                            ) {
                                Text("PLAY BACK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E4E54), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    } else {
                        // Stable placeholder so button columns don't shift around unpleasantly
                        ClayButton(
                            onClick = {},
                            backgroundColor = Color(0xFFECE6E2),
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("NO SESSION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0x447D7266), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                // 3. Metronome Toggle control right (balanced weight)
                Row(
                    modifier = Modifier.weight(1.2f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "METRONOME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF7D7266)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ClayToggle(
                        checked = isMetronomeOn,
                        onCheckedChange = onMetronomeToggle,
                        modifier = Modifier.testTag("metronome_toggle_switch")
                    )
                }
            }
        }

        // 2. Saved Recordings horizontal carousel
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "SAVED EXPRESSIVE SESSIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xAAFFFFFF),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (recordings.isEmpty()) {
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF23252F) // dark clay panel
                ) {
                    Text(
                        text = "No recorded sessions yet. Play on-screen and press RECORD to generate your first claymorphic analytical file.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0x66FFFFFF),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .testTag("recordings_carousel"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    recordings.forEach { recording ->
                        val isSelected = selectedRecording?.id == recording.id
                        val cardBg = if (isSelected) Color(0xFFC4E0E5) else Color(0xFF2A2C35)
                        val titleColor = if (isSelected) Color(0xFF2E4E54) else Color.White
                        val metaColor = if (isSelected) Color(0xFF5A7E85) else Color(0x88FFFFFF)

                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .clickable { onSelectRecording(recording) }
                        ) {
                            ClayCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = cardBg,
                                elevation = if (isSelected) 3.dp else 5.dp
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = recording.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = titleColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        IconButton(
                                            onClick = { onDeleteRecording(recording) },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .testTag("delete_recording_${recording.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Recording",
                                                tint = if (isSelected) Color(0xFF8B4545) else Color(0x44FFFFFF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    val df = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                    Text(
                                        text = df.format(Date(recording.timestamp)),
                                        fontSize = 10.sp,
                                        color = metaColor
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${recording.bpm} BPM",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = metaColor
                                        )
                                        Text(
                                            text = String.format("%.1fs", recording.durationMs / 1000.0f),
                                            fontSize = 10.sp,
                                            color = metaColor
                                        )
                                    }

                                    // Small Rubato Tag inside the card
                                    val rubatoText = when {
                                        recording.rubatoScore < 0.15f -> "GRID"
                                        recording.rubatoScore < 0.40f -> "EMOTIVE"
                                        recording.rubatoScore < 0.75f -> "FLOW"
                                        else -> "VAR"
                                    }
                                    Text(
                                        text = "TIMING: $rubatoText",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) Color(0xFF2E4E54) else Color(0xFFC4E0E5),
                                        modifier = Modifier
                                            .background(if (isSelected) Color(0x222E4E54) else Color(0x22FFFFFF), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Save Recording Claymorphic Dialog (Fully customized)
        if (showSaveDialog) {
            Dialog(
                onDismissRequest = { 
                    showSaveDialog = false
                    onCancelRecording()
                }
            ) {
                ClayCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_recording_dialog"),
                    backgroundColor = Color(0xFFF5EBE0) // Sand-Clay base
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SAVE CLAYMorphic PERFORMANCE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF7D7266),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Input a title for your expressive session. Rubato profiles and voicing densities will be computed automatically.",
                            fontSize = 11.sp,
                            color = Color(0xFF8D8176),
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = recordingTitleInput,
                            onValueChange = { recordingTitleInput = it },
                            placeholder = { Text("E.g., Moonlight Sonics", color = Color(0xFFB0A499)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFC4E0E5),
                                unfocusedBorderColor = Color(0xFFD0C4B9),
                                focusedContainerColor = Color(0xFFECE6E2),
                                unfocusedContainerColor = Color(0xFFECE6E2)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recording_title_text_field")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ClayButton(
                                onClick = {
                                    showSaveDialog = false
                                    onCancelRecording()
                                },
                                backgroundColor = Color(0xFFECE6E2),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dialog_discard_button")
                            ) {
                                Text("DISCARD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7D7266))
                            }

                            ClayButton(
                                onClick = {
                                    showSaveDialog = false
                                    onStopRecording(recordingTitleInput)
                                },
                                backgroundColor = Color(0xFFFFD1DC), // Soft clay Rose
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dialog_save_button")
                            ) {
                                Text("SAVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red.darken(0.15f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Inline helper to darken colors for clay shading
private fun Color.darken(factor: Float): Color {
    return Color(
        red = (red * (1f - factor)).coerceIn(0f, 1f),
        green = (green * (1f - factor)).coerceIn(0f, 1f),
        blue = (blue * (1f - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
}
