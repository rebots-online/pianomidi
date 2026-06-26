package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.NoteEvent
import com.example.data.Recording
import com.example.ui.clay.ClayButton
import com.example.ui.clay.ClayCard
import com.example.ui.clay.ClaySlider
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@Composable
fun ReductionistView(
    bpm: Int,
    onBpmChange: (Int) -> Unit,
    octave: Int,
    onOctaveChange: (Int) -> Unit,
    decayFactor: Float,
    onDecayChange: (Float) -> Unit,
    overtoneMix: Float,
    onOvertoneChange: (Float) -> Unit,
    selectedRecording: Recording?,
    playbackProgressMs: Long = 0L,
    onPositionSelected: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reductionist_controls_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Interactive Parameter Settings (Claymorphic Grid)
        ClayCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFFF5EBE0) // Sand-Clay base
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "REDUCTIONIST PARAMETERS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF7E7266)
                )

                // BPM Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tempo Framework",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D534A)
                        )
                        Text(
                            text = "$bpm BPM (metronome frequency)",
                            fontSize = 11.sp,
                            color = Color(0xFF8D8176)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClayButton(
                            onClick = { onBpmChange(bpm - 5) },
                            backgroundColor = Color(0xFFECE6E2),
                            elevation = 3.dp,
                            modifier = Modifier.testTag("bpm_minus_button")
                        ) {
                            Text("-5", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D534A))
                        }
                        ClayButton(
                            onClick = { onBpmChange(bpm + 5) },
                            backgroundColor = Color(0xFFECE6E2),
                            elevation = 3.dp,
                            modifier = Modifier.testTag("bpm_plus_button")
                        ) {
                            Text("+5", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D534A))
                        }
                    }
                }

                // Keyboard Octave Shift
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keyboard Octave",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D534A)
                        )
                        Text(
                            text = "C${octave} starting pitch offset",
                            fontSize = 11.sp,
                            color = Color(0xFF8D8176)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClayButton(
                            onClick = { onOctaveChange(octave - 1) },
                            backgroundColor = Color(0xFFC4E0E5),
                            enabled = octave > 1,
                            modifier = Modifier.testTag("octave_minus_button")
                        ) {
                            Text("< Oct", fontSize = 11.sp, color = Color(0xFF2E4E54))
                        }
                        ClayButton(
                            onClick = { onOctaveChange(octave + 1) },
                            backgroundColor = Color(0xFFC4E0E5),
                            enabled = octave < 7,
                            modifier = Modifier.testTag("octave_plus_button")
                        ) {
                            Text("Oct >", fontSize = 11.sp, color = Color(0xFF2E4E54))
                        }
                    }
                }

                // Synthesizer Envelope: Decay
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Damping / Decay Rate", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D534A))
                        Text(text = String.format("%.2f s", 0.1f + decayFactor * 3.5f), fontSize = 11.sp, color = Color(0xFF8D8176))
                    }
                    ClaySlider(
                        value = decayFactor,
                        onValueChange = onDecayChange,
                        barColor = Color(0xFFECE6E2),
                        knobColor = Color(0xFFFFD1DC) // Pastel Pink
                    )
                }

                // Synthesizer Timbre: Brightness
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Overtone Brightness (Chime)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D534A))
                        Text(text = String.format("%d%%", (overtoneMix * 100).toInt()), fontSize = 11.sp, color = Color(0xFF8D8176))
                    }
                    ClaySlider(
                        value = overtoneMix,
                        onValueChange = onOvertoneChange,
                        barColor = Color(0xFFECE6E2),
                        knobColor = Color(0xFFC4E0E5) // Pastel Blue
                    )
                }
            }
        }

        // 2. Performance Analytics (If recording selected)
        if (selectedRecording != null) {
            ClayCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFECE6E2) // Light gray clay
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PERFORMANCE ANALYTICS REPORT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF5D534A)
                    )

                    // Descriptive rating of timing discipline (Rubato Classification)
                    val rubatoClass = when {
                        selectedRecording.rubatoScore < 0.15f -> "METRONOMIC QUANTIZED GRID (Strictly Mechanical)"
                        selectedRecording.rubatoScore < 0.40f -> "EMOTIVE RUBATO (Expressive, disciplined elastic timing)"
                        selectedRecording.rubatoScore < 0.75f -> "EXPANSIVE FLOW (Highly rubato, elastic structure)"
                        else -> "UNSTRUCTURED TIMING VARIANCE (High timing deviations)"
                    }

                    Text(
                        text = selectedRecording.title.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2A2C35)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "RUBATO INDEX", fontSize = 10.sp, color = Color(0xFF8D8176))
                            Text(
                                text = String.format("%.2f", selectedRecording.rubatoScore),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedRecording.rubatoScore in 0.15f..0.50f) Color(0xFF2E4E54) else Color(0xFF8B4545)
                            )
                        }

                        Column {
                            Text(text = "POLYPHONIC VOICING", fontSize = 10.sp, color = Color(0xFF8D8176))
                            Text(
                                text = String.format("%.1f Notes/onset", selectedRecording.voicingDensity),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D534A)
                            )
                        }

                        Column {
                            Text(text = "TOUCH IMPACT", fontSize = 10.sp, color = Color(0xFF8D8176))
                            Text(
                                text = String.format("%d%% (Velocity)", (selectedRecording.averageVelocity * 100).toInt()),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D534A)
                            )
                        }
                    }

                    Text(
                        text = "Timing Style: $rubatoClass",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF5D534A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0A000000), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 3. Scrollable Piano Roll Sequencer representation
                    Text(
                        text = "ANALYTICAL PIANO ROLL (Vertical pitch class vs. Horizontal beat structure)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF8D8176)
                    )

                    val noteEvents = remember(selectedRecording) {
                        val moshi = Moshi.Builder().build()
                        val listType = Types.newParameterizedType(List::class.java, NoteEvent::class.java)
                        val adapter = moshi.adapter<List<NoteEvent>>(listType)
                        adapter.fromJson(selectedRecording.noteEventsJson) ?: emptyList()
                    }

                    PianoRollSequencer(
                        noteEvents = noteEvents,
                        bpm = selectedRecording.bpm,
                        totalDurationMs = selectedRecording.durationMs,
                        playbackProgressMs = playbackProgressMs,
                        onPositionSelected = onPositionSelected
                    )
                }
            }
        } else {
            // Empty state tip
            ClayCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFECE6E2)
            ) {
                Text(
                    text = "Begin playing and hit RECORD to capture your session. The detailed analytical piano roll and expressive rubato feedback will generate automatically upon saving.",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF7D7266),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PianoRollSequencer(
    noteEvents: List<NoteEvent>,
    bpm: Int,
    totalDurationMs: Long,
    playbackProgressMs: Long = 0L,
    onPositionSelected: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 150.dp
) {
    if (noteEvents.isEmpty()) return

    val scrollState = rememberScrollState()

    // Determine notes boundary to scale vertical axis
    val minPitch = noteEvents.minOf { it.pitch }.coerceAtLeast(36)
    val maxPitch = noteEvents.maxOf { it.pitch }.coerceAtMost(96)
    val pitchRange = (maxPitch - minPitch).coerceAtLeast(12)

    val scaleX = 0.15f // pixels per millisecond
    val rowHeightPx = 45f // pixels per pitch class row

    // Total width of the horizontal roll: total length in ms * scaling factor (minimum 400dp width)
    val widthPx = (totalDurationMs * scaleX).coerceAtLeast(600f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(0xFF1E2028), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .horizontalScroll(scrollState)
            .testTag("piano_roll_scroll_container")
    ) {
        Canvas(
            modifier = Modifier
                .width(widthPx.dp)
                .fillMaxHeight()
                .pointerInput(totalDurationMs) {
                    detectTapGestures { offset ->
                        val tappedMs = (offset.x / scaleX).toLong().coerceIn(0, totalDurationMs)
                        onPositionSelected(tappedMs)
                    }
                }
                .testTag("piano_roll_canvas")
        ) {
            val h = size.height
            val w = size.width

            // Calculate precise pixel grids
            val totalNotes = pitchRange + 2
            val stepY = h / totalNotes

            // 1. Draw horizontal lines for pitch reference grid
            for (i in 0..totalNotes) {
                val y = i * stepY
                drawLine(
                    color = Color(0x0CFFFFFF),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            // 2. Draw vertical structural grid lines representing absolute metronome beats
            val beatLengthMs = 60000.0 / bpm
            val totalBeats = (totalDurationMs.toDouble() / beatLengthMs).toInt()

            for (b in 0..totalBeats + 1) {
                val beatTimeMs = b * beatLengthMs
                val x = (beatTimeMs * scaleX).toFloat()

                val isDownbeat = b % 4 == 0
                val gridColor = if (isDownbeat) Color(0x33C4E0E5) else Color(0x18FFFFFF)
                val strokeWidth = if (isDownbeat) 2f else 1f

                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = strokeWidth
                )

                // Label beat markers
                if (isDownbeat) {
                    // Draw small beat numbers at top
                    // Simple indicator lines
                    drawLine(
                        color = Color(0x66C4E0E5),
                        start = Offset(x, 0f),
                        end = Offset(x, 15f),
                        strokeWidth = 3f
                    )
                }
            }

            // 3. Draw Note events as rounded clay capsules
            noteEvents.forEach { event ->
                // Calculate position
                val noteOffsetFromMax = maxPitch - event.pitch + 1
                val y = noteOffsetFromMax * stepY + (stepY * 0.15f)
                val noteHeight = stepY * 0.7f

                val xStart = (event.startMs * scaleX).toFloat()
                val noteWidth = (event.durationMs * scaleX).toFloat().coerceAtLeast(15f)

                // Color capsule based on velocity/touch impact
                val noteColor = Color(0xFFFFD1DC).copy(alpha = 0.4f + event.velocity * 0.6f) // soft pink to glowing neon

                // Draw a beautiful claymorphic note bar capsule
                drawRoundRect(
                    color = noteColor,
                    topLeft = Offset(xStart, y),
                    size = Size(noteWidth, noteHeight),
                    cornerRadius = CornerRadius(noteHeight / 2f, noteHeight / 2f)
                )

                // Inner white shine for 3D pill look
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.4f),
                    topLeft = Offset(xStart + 2f, y + 2f),
                    size = Size(noteWidth - 4f, noteHeight * 0.3f),
                    cornerRadius = CornerRadius(noteHeight * 0.15f, noteHeight * 0.15f)
                )

                // Stroke outline
                drawRoundRect(
                    color = Color(0xFFFBC4B6).copy(alpha = 0.8f),
                    topLeft = Offset(xStart, y),
                    size = Size(noteWidth, noteHeight),
                    cornerRadius = CornerRadius(noteHeight / 2f, noteHeight / 2f),
                    style = Stroke(width = 1f)
                )
            }

            // 4. Draw a moving vertical playhead representing current playback position
            if (playbackProgressMs > 0) {
                val playheadX = (playbackProgressMs * scaleX).toFloat()
                drawLine(
                    color = Color(0xFFFF4D4D), // beautiful glowing red playhead
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, h),
                    strokeWidth = 3f
                )
                // draw a small circle at the top of the playhead to make it look like a real tape playhead
                drawCircle(
                    color = Color(0xFFFF4D4D),
                    radius = 8f,
                    center = Offset(playheadX, 8f)
                )
            }
        }
    }
}
