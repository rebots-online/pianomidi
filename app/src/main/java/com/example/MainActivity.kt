package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.clay.ClayButton
import com.example.ui.clay.ClayCard
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PianoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF1E2028) // Deep cosmic clay slate
                ) { innerPadding ->
                    PianoWorkspace(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun PianoWorkspace(
    modifier: Modifier = Modifier,
    viewModel: PianoViewModel = viewModel()
) {
    // Collect ViewModel states reactively
    val isRecording by viewModel.isRecording.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isMetronomeOn by viewModel.isMetronomeOn.collectAsState()
    val bpm by viewModel.bpm.collectAsState()
    val currentBeat by viewModel.currentMetronomeBeat.collectAsState()
    val beatProgress by viewModel.beatProgress.collectAsState()
    val octave by viewModel.keyboardOctave.collectAsState()
    val activeKeys by viewModel.activeKeys.collectAsState()
    
    val decayFactor by viewModel.decayFactor.collectAsState()
    val overtoneMix by viewModel.overtoneMix.collectAsState()
    
    val liveAmplitude by viewModel.liveAmplitude.collectAsState()
    val liveActivePitches by viewModel.liveActivePitches.collectAsState()
    val liveRubatoDeviation by viewModel.liveRubatoDeviation.collectAsState()
    
    val recordings by viewModel.allRecordings.collectAsState()
    val selectedRecording by viewModel.selectedRecording.collectAsState()
    val playbackProgressMs by viewModel.playbackProgressMs.collectAsState()

    // UI Tab selection: HOLISTIC, REDUCTIONIST or COLLABORATION view
    var activeTab by remember { mutableStateOf("HOLISTIC") }

    BoxWithConstraints(
        modifier = modifier
            .background(Color(0xFF1E2028))
            .testTag("piano_workspace")
    ) {
        val isWideScreen = maxWidth >= 640.dp

        Column(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                // Wide Screen Split Layout: Side-by-Side
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Interactive Tab Display (Holistic, Analytics, Collab)
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title Header (Clay brand card)
                        ClayCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFF23252F)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "CLAYMorphic PIANO",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFFFD1DC)
                                    )
                                    Text(
                                        text = "Expressive MIDI Recorder & Touch Analyzer",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0x88FFFFFF)
                                    )
                                }

                                Text(
                                    text = "VOICES: ${liveActivePitches.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFC4E0E5),
                                    modifier = Modifier
                                        .background(Color(0x11FFFFFF), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Custom Segmented Control (Tabs switch)
                        ClayCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFFECE6E2),
                            elevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val holisticActive = activeTab == "HOLISTIC"
                                ClayButton(
                                    onClick = { activeTab = "HOLISTIC" },
                                    backgroundColor = if (holisticActive) Color(0xFFFFD1DC) else Color(0xFFECE6E2),
                                    elevation = if (holisticActive) 2.dp else 4.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tab_holistic_button")
                                ) {
                                    Text(
                                        text = "HOLISTIC",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (holisticActive) Color.Red.darken(0.15f) else Color(0xFF7D7266)
                                    )
                                }

                                val reductionistActive = activeTab == "REDUCTIONIST"
                                ClayButton(
                                    onClick = { activeTab = "REDUCTIONIST" },
                                    backgroundColor = if (reductionistActive) Color(0xFFC4E0E5) else Color(0xFFECE6E2),
                                    elevation = if (reductionistActive) 2.dp else 4.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tab_reductionist_button")
                                ) {
                                    Text(
                                        text = "ANALYTICS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (reductionistActive) Color(0xFF2E4E54) else Color(0xFF7D7266)
                                    )
                                }

                                val collabActive = activeTab == "COLLABORATION"
                                ClayButton(
                                    onClick = { activeTab = "COLLABORATION" },
                                    backgroundColor = if (collabActive) Color(0xFFD5F3D6) else Color(0xFFECE6E2),
                                    elevation = if (collabActive) 2.dp else 4.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tab_collab_button")
                                ) {
                                    Text(
                                        text = "COLLAB",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (collabActive) Color(0xFF2E6330) else Color(0xFF7D7266)
                                    )
                                }

                                val midiActive = activeTab == "MIDI"
                                ClayButton(
                                    onClick = { activeTab = "MIDI" },
                                    backgroundColor = if (midiActive) Color(0xFFFBE0D6) else Color(0xFFECE6E2),
                                    elevation = if (midiActive) 2.dp else 4.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tab_midi_button")
                                ) {
                                    Text(
                                        text = "MIDI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (midiActive) Color(0xFF8B4513) else Color(0xFF7D7266)
                                    )
                                }
                            }
                        }

                        // Active Tab display area
                        when (activeTab) {
                            "HOLISTIC" -> {
                                HolisticMorphingView(
                                    amplitude = liveAmplitude,
                                    activePitches = liveActivePitches,
                                    rubatoDeviation = liveRubatoDeviation,
                                    beatProgress = beatProgress,
                                    currentBeat = currentBeat,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            "REDUCTIONIST" -> {
                                ReductionistView(
                                    bpm = bpm,
                                    onBpmChange = { viewModel.setBpm(it) },
                                    octave = octave,
                                    onOctaveChange = { viewModel.setKeyboardOctave(it) },
                                    decayFactor = decayFactor,
                                    onDecayChange = { viewModel.setDecayFactor(it) },
                                    overtoneMix = overtoneMix,
                                    onOvertoneChange = { viewModel.setOvertoneMix(it) },
                                    selectedRecording = selectedRecording,
                                    playbackProgressMs = playbackProgressMs,
                                    onPositionSelected = { viewModel.seekTo(it) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            "COLLABORATION" -> {
                                CollaborationView(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            "MIDI" -> {
                                MidiConfigView(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Right Column: Playback Sequencer, recordings checklist, feedback carousel
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PlaybackSequencer(
                            isRecording = isRecording,
                            isPlaying = isPlaying,
                            isMetronomeOn = isMetronomeOn,
                            recordings = recordings,
                            selectedRecording = selectedRecording,
                            onStartRecording = { viewModel.startRecording() },
                            onStopRecording = { title -> viewModel.stopAndSaveRecording(title) },
                            onCancelRecording = { viewModel.cancelRecording() },
                            onStartPlayback = { viewModel.startPlayback() },
                            onStopPlayback = { viewModel.stopPlayback() },
                            onSelectRecording = { viewModel.selectRecording(it) },
                            onDeleteRecording = { viewModel.deleteRecording(it) },
                            onMetronomeToggle = { viewModel.setMetronomeEnabled(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // Portrait Compact Phone Layout
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Title Header
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFF23252F)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CLAYMorphic PIANO",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFFFD1DC)
                                )
                                Text(
                                    text = "Expressive MIDI Recorder & Touch Analyzer",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0x88FFFFFF)
                                )
                            }

                            Text(
                                text = "VOICES: ${liveActivePitches.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFC4E0E5),
                                modifier = Modifier
                                    .background(Color(0x11FFFFFF), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Tabs Switcher
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFECE6E2),
                        elevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val holisticActive = activeTab == "HOLISTIC"
                            ClayButton(
                                onClick = { activeTab = "HOLISTIC" },
                                backgroundColor = if (holisticActive) Color(0xFFFFD1DC) else Color(0xFFECE6E2),
                                elevation = if (holisticActive) 2.dp else 4.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tab_holistic_button")
                            ) {
                                Text(
                                    text = "HOLISTIC",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (holisticActive) Color.Red.darken(0.15f) else Color(0xFF7D7266)
                                )
                            }

                            val reductionistActive = activeTab == "REDUCTIONIST"
                            ClayButton(
                                onClick = { activeTab = "REDUCTIONIST" },
                                backgroundColor = if (reductionistActive) Color(0xFFC4E0E5) else Color(0xFFECE6E2),
                                elevation = if (reductionistActive) 2.dp else 4.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tab_reductionist_button")
                            ) {
                                Text(
                                    text = "ANALYTICS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (reductionistActive) Color(0xFF2E4E54) else Color(0xFF7D7266)
                                )
                            }

                            val collabActive = activeTab == "COLLABORATION"
                            ClayButton(
                                onClick = { activeTab = "COLLABORATION" },
                                backgroundColor = if (collabActive) Color(0xFFD5F3D6) else Color(0xFFECE6E2),
                                elevation = if (collabActive) 2.dp else 4.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tab_collab_button")
                            ) {
                                Text(
                                    text = "COLLAB",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (collabActive) Color(0xFF2E6330) else Color(0xFF7D7266)
                                )
                            }

                            val midiActive = activeTab == "MIDI"
                            ClayButton(
                                onClick = { activeTab = "MIDI" },
                                backgroundColor = if (midiActive) Color(0xFFFBE0D6) else Color(0xFFECE6E2),
                                elevation = if (midiActive) 2.dp else 4.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tab_midi_button")
                            ) {
                                Text(
                                    text = "MIDI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (midiActive) Color(0xFF8B4513) else Color(0xFF7D7266)
                                )
                            }
                        }
                    }

                    // Display active tab
                    when (activeTab) {
                        "HOLISTIC" -> {
                            HolisticMorphingView(
                                amplitude = liveAmplitude,
                                activePitches = liveActivePitches,
                                rubatoDeviation = liveRubatoDeviation,
                                beatProgress = beatProgress,
                                currentBeat = currentBeat,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "REDUCTIONIST" -> {
                            ReductionistView(
                                bpm = bpm,
                                onBpmChange = { viewModel.setBpm(it) },
                                octave = octave,
                                onOctaveChange = { viewModel.setKeyboardOctave(it) },
                                decayFactor = decayFactor,
                                onDecayChange = { viewModel.setDecayFactor(it) },
                                overtoneMix = overtoneMix,
                                onOvertoneChange = { viewModel.setOvertoneMix(it) },
                                selectedRecording = selectedRecording,
                                playbackProgressMs = playbackProgressMs,
                                onPositionSelected = { viewModel.seekTo(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "COLLABORATION" -> {
                            CollaborationView(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "MIDI" -> {
                            MidiConfigView(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Transport
                    PlaybackSequencer(
                        isRecording = isRecording,
                        isPlaying = isPlaying,
                        isMetronomeOn = isMetronomeOn,
                        recordings = recordings,
                        selectedRecording = selectedRecording,
                        onStartRecording = { viewModel.startRecording() },
                        onStopRecording = { title -> viewModel.stopAndSaveRecording(title) },
                        onCancelRecording = { viewModel.cancelRecording() },
                        onStartPlayback = { viewModel.startPlayback() },
                        onStopPlayback = { viewModel.stopPlayback() },
                        onSelectRecording = { viewModel.selectRecording(it) },
                        onDeleteRecording = { viewModel.deleteRecording(it) },
                        onMetronomeToggle = { viewModel.setMetronomeEnabled(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Keyboard anchored to the bottom of the column as a base console
            PianoKeyboard(
                octave = octave,
                activeKeys = activeKeys,
                onKeyPress = { pitch, vel -> viewModel.pianoKeyPress(pitch, vel) },
                onKeyRelease = { pitch -> viewModel.pianoKeyRelease(pitch) },
                modifier = Modifier.fillMaxWidth()
            )
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

