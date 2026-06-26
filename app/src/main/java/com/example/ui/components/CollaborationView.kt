package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Comment
import com.example.data.Recording
import com.example.ui.clay.ClayButton
import com.example.ui.clay.ClayCard
import com.example.viewmodel.PianoViewModel
import com.squareup.moshi.Moshi
import com.example.data.NoteEvent
import com.example.viewmodel.Types
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaborationView(
    viewModel: PianoViewModel,
    modifier: Modifier = Modifier
) {
    val selectedRecording by viewModel.selectedRecording.collectAsState()
    val comments by viewModel.selectedRecordingComments.collectAsState()
    val playbackProgressMs by viewModel.playbackProgressMs.collectAsState()
    val isSnippetRecording by viewModel.isSnippetRecording.collectAsState()
    val isSnippetPlaying by viewModel.isSnippetPlaying.collectAsState()

    var senderRole by remember { mutableStateOf("TEACHER") } // "TEACHER" or "STUDENT"
    var commentText by remember { mutableStateOf("") }
    var pinnedTimeMs by remember { mutableStateOf(0L) }
    var attachedSnippetJson by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    val recording = selectedRecording
    if (recording == null) {
        ClayCard(
            modifier = modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF23252F)
        ) {
            Text(
                text = "Select an expressive recording from the carousel below to access the Teacher & Student Collaboration Portal.",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xAAFFFFFF),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        }
        return
    }

    // Auto update pinnedTimeMs to match playbackProgressMs if nothing has been manually selected yet, or when playing back
    val isPlaying by viewModel.isPlaying.collectAsState()
    LaunchedEffect(playbackProgressMs) {
        if (isPlaying && playbackProgressMs > 0) {
            pinnedTimeMs = playbackProgressMs
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("collaboration_view_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Interactive Pinned Timeline Visualizer
        ClayCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF242630)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SESSION TIMELINE (TAP TO PIN)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFC4E0E5)
                    )
                    Text(
                        text = "PIN: ${String.format(Locale.getDefault(), "%.1fs", pinnedTimeMs / 1000f)} / ${String.format(Locale.getDefault(), "%.1fs", recording.durationMs / 1000f)}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                // Interactive Timeline canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color(0xFF181A22), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(recording.durationMs) {
                            detectTapGestures { offset ->
                                val fraction = offset.x / size.width
                                pinnedTimeMs = (fraction * recording.durationMs).toLong().coerceIn(0, recording.durationMs)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Draw progress background
                        if (recording.durationMs > 0) {
                            val progressX = (playbackProgressMs.toFloat() / recording.durationMs) * w
                            drawRect(
                                color = Color(0x33C4E0E5),
                                topLeft = Offset(0f, 0f),
                                size = androidx.compose.ui.geometry.Size(progressX, h)
                            )

                            // Draw pin marker
                            val pinX = (pinnedTimeMs.toFloat() / recording.durationMs) * w
                            drawLine(
                                color = Color(0xFFFF4D4D),
                                start = Offset(pinX, 0f),
                                end = Offset(pinX, h),
                                strokeWidth = 3f
                            )
                            drawCircle(
                                color = Color(0xFFFF4D4D),
                                radius = 6f,
                                center = Offset(pinX, h / 2f)
                            )

                            // Draw existing comments markers
                            comments.forEach { comment ->
                                val cx = (comment.playbackPositionMs.toFloat() / recording.durationMs) * w
                                val commentColor = if (comment.sender == "TEACHER") Color(0xFFC4E0E5) else Color(0xFFFFD1DC)
                                drawCircle(
                                    color = commentColor,
                                    radius = 4f,
                                    center = Offset(cx, h / 4f)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.0s", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0x66FFFFFF))
                    Text(
                        text = "Tapping timeline pins comments to specific positions",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0x44FFFFFF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1fs", recording.durationMs / 1000f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0x66FFFFFF)
                    )
                }
            }
        }

        // 2. Add New Comment Area
        ClayCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFFECE6E2) // neutral warm clay light background
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "POST FEEDBACK COMMENT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF7D7266)
                )

                // Sender role segmented switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isTeacher = senderRole == "TEACHER"
                    ClayButton(
                        onClick = { senderRole = "TEACHER" },
                        backgroundColor = if (isTeacher) Color(0xFFC4E0E5) else Color(0xFFECE6E2),
                        elevation = if (isTeacher) 2.dp else 4.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "I AM TEACHER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTeacher) Color(0xFF2E4E54) else Color(0xFF7D7266)
                        )
                    }

                    val isStudent = senderRole == "STUDENT"
                    ClayButton(
                        onClick = { senderRole = "STUDENT" },
                        backgroundColor = if (isStudent) Color(0xFFFFD1DC) else Color(0xFFECE6E2),
                        elevation = if (isStudent) 2.dp else 4.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "I AM STUDENT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStudent) Color.Red.darken(0.15f) else Color(0xFF7D7266)
                        )
                    }
                }

                // Comment text field
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Ask a question, offer rubato guidance, or suggest a voicing fix...", fontSize = 11.sp, color = Color(0xFFB0A499)) },
                    textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFF534C44)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFC4E0E5),
                        unfocusedBorderColor = Color(0xFFD0C4B9),
                        focusedContainerColor = Color(0xFFFBF9F6),
                        unfocusedContainerColor = Color(0xFFFBF9F6)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(70.dp).testTag("collaboration_comment_input")
                )

                // Snippet Attachment UI Block
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFF5EBE0),
                    elevation = 2.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MIDI SNIPPET ATTACHMENT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF8D8176)
                            )

                            // Status badge
                            val snippetNotesCount = remember(attachedSnippetJson) {
                                attachedSnippetJson?.let { json ->
                                    try {
                                        val moshi = Moshi.Builder().build()
                                        val listType = Types.newParameterizedType(List::class.java, NoteEvent::class.java)
                                        val adapter = moshi.adapter<List<NoteEvent>>(listType)
                                        adapter.fromJson(json)?.size ?: 0
                                    } catch (e: Exception) {
                                        0
                                    }
                                } ?: 0
                            }

                            val badgeText = if (snippetNotesCount > 0) "ATTACHED: $snippetNotesCount NOTES" else "EMPTY"
                            val badgeColor = if (snippetNotesCount > 0) Color(0xFFD5F3D6) else Color(0xFFEBE0D5)
                            val badgeTextColor = if (snippetNotesCount > 0) Color(0xFF2E6330) else Color(0xFF8D8176)

                            Text(
                                text = badgeText,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = badgeTextColor,
                                modifier = Modifier
                                    .background(badgeColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (isSnippetRecording) {
                            // Recording indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.Red)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "PLAY PIANO TO RECORD ILLUSTRATIVE PHRASE...",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red.darken(0.15f)
                                    )
                                }

                                ClayButton(
                                    onClick = {
                                        val snippetJson = viewModel.stopSnippetRecording()
                                        if (snippetJson != null) {
                                            attachedSnippetJson = snippetJson
                                        }
                                    },
                                    backgroundColor = Color(0xFFFBC4B6),
                                    elevation = 2.dp
                                ) {
                                    Text("STOP & ATTACH", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B0000))
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ClayButton(
                                    onClick = { viewModel.startSnippetRecording() },
                                    backgroundColor = Color(0xFFFFD1DC),
                                    elevation = 4.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("RECORD ILLUSTRATIVE MIDI", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Red.darken(0.15f))
                                }

                                if (attachedSnippetJson != null) {
                                    ClayButton(
                                        onClick = { attachedSnippetJson = null },
                                        backgroundColor = Color(0xFFEBE0D5),
                                        elevation = 4.dp
                                    ) {
                                        Text("REMOVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7D7266))
                                    }
                                }
                            }
                        }
                    }
                }

                // Publish actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClayButton(
                        onClick = { pinnedTimeMs = playbackProgressMs },
                        backgroundColor = Color(0xFFF5EBE0)
                    ) {
                        Text(
                            text = "PIN TO PLAYHEAD",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7D7266)
                        )
                    }

                    ClayButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                viewModel.addComment(
                                    sender = senderRole,
                                    text = commentText,
                                    playbackPositionMs = pinnedTimeMs,
                                    attachedMidiJson = attachedSnippetJson
                                )
                                commentText = ""
                                attachedSnippetJson = null
                            }
                        },
                        backgroundColor = if (senderRole == "TEACHER") Color(0xFFC4E0E5) else Color(0xFFFFD1DC),
                        enabled = commentText.isNotBlank()
                    ) {
                        Text(
                            text = "POST FEEDBACK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (senderRole == "TEACHER") Color(0xFF2E4E54) else Color.Red.darken(0.15f)
                        )
                    }
                }
            }
        }

        // 3. Commentary Thread List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "FEEDBACK CHRONICLES (${comments.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xAAFFFFFF),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (comments.isEmpty()) {
                ClayCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF23252F)
                ) {
                    Text(
                        text = "No commentary in this thread yet. Students and teachers can pin precise timestamped comments, ask questions, and record illustrative MIDI attachments to study specific performance areas together.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0x66FFFFFF),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                comments.sortedBy { it.playbackPositionMs }.forEach { comment ->
                    val isTeacher = comment.sender == "TEACHER"
                    val senderBadgeColor = if (isTeacher) Color(0xFFC4E0E5) else Color(0xFFFFD1DC)
                    val senderTextColor = if (isTeacher) Color(0xFF2E4E54) else Color.Red.darken(0.15f)

                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFF2A2C35)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Speaker tag
                                    Text(
                                        text = comment.sender,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = senderTextColor,
                                        modifier = Modifier
                                            .background(senderBadgeColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )

                                    // Pinned Position
                                    val positionSec = comment.playbackPositionMs / 1000f
                                    Text(
                                        text = "Pinned @ ${String.format(Locale.getDefault(), "%.1fs", positionSec)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFC4E0E5),
                                        modifier = Modifier
                                            .background(Color(0x15FFFFFF), RoundedCornerShape(4.dp))
                                            .clickable { pinnedTimeMs = comment.playbackPositionMs }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val df = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                    Text(
                                        text = df.format(Date(comment.timestamp)),
                                        fontSize = 9.sp,
                                        color = Color(0x66FFFFFF)
                                    )

                                    IconButton(
                                        onClick = { viewModel.deleteComment(comment) },
                                        modifier = Modifier.size(24.dp).testTag("delete_comment_${comment.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Comment",
                                            tint = Color(0x44FFFFFF),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Commentary Text
                            Text(
                                text = comment.text,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // MIDI snippet playing back block
                            val snippetJson = comment.attachedMidiJson
                            if (snippetJson != null) {
                                val isPlayingThis = isSnippetPlaying == comment.id

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E2028))
                                        .clickable {
                                            if (isPlayingThis) {
                                                viewModel.stopSnippetPlayback()
                                            } else {
                                                viewModel.startSnippetPlayback(comment.id, snippetJson)
                                            }
                                        }
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = if (isPlayingThis) Color.Green else Color(0xFFFFD1DC),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (isPlayingThis) "PLAYING ILLUSTRATIVE MIDI SNIPPET..." else "PLAY ATTACHED MIDI ILLUSTRATION",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isPlayingThis) Color.Green else Color(0xFFC4E0E5)
                                            )
                                        }

                                        Text(
                                            text = "MIDI SNIPPET",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0x44FFFFFF)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Private extension for clay colors
private fun Color.darken(factor: Float): Color {
    return Color(
        red = (red * (1f - factor)).coerceIn(0f, 1f),
        green = (green * (1f - factor)).coerceIn(0f, 1f),
        blue = (blue * (1f - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
}
