package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.clay.claymorphicShadow

/**
 * Striking-height velocity mapping touch handler
 */
@Composable
fun Modifier.pianoTouchKey(
    pitch: Int,
    onPress: (Float) -> Unit,
    onRelease: () -> Unit
): Modifier {
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnRelease by rememberUpdatedState(onRelease)

    return this.pointerInput(pitch) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            // Calculate striking height velocity: bottom of key = 1.0f (loud), top = 0.4f (soft)
            val strikeHeight = down.position.y / size.height
            val velocity = strikeHeight.coerceIn(0.4f, 1.0f)
            
            currentOnPress(velocity)

            var change = down
            while (true) {
                val event = awaitPointerEvent()
                val isUp = event.changes.any { it.id == change.id && !it.pressed }
                if (isUp) {
                    currentOnRelease()
                    break
                }
            }
        }
    }
}

@Composable
fun PianoKeyboard(
    octave: Int,
    activeKeys: Set<Int>,
    onKeyPress: (Int, Float) -> Unit,
    onKeyRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: Dp = 220.dp
) {
    // 12 Semitones in an octave: C, C#, D, D#, E, F, F#, G, G#, A, A#, B
    // We will show a 14-note keyboard layout (from C to the second F) for an excellent on-screen experience:
    // White Keys: C, D, E, F, G, A, B, C, D, E
    // Black Keys: C#, D#, F#, G#, A#, C#, D#
    
    val startPitch = (octave + 1) * 12 // e.g. Octave 4 starts at pitch 60 (C4)
    
    val whiteKeyOffsets = listOf(0, 2, 4, 5, 7, 9, 11, 12, 14, 16)
    val whiteKeyLabels = listOf("C", "D", "E", "F", "G", "A", "B", "C+", "D+", "E+")
    
    val blackKeys = listOf(
        BlackKeyConfig(pitchOffset = 1, label = "C#", whiteIndexLeft = 0),
        BlackKeyConfig(pitchOffset = 3, label = "D#", whiteIndexLeft = 1),
        BlackKeyConfig(pitchOffset = 6, label = "F#", whiteIndexLeft = 3),
        BlackKeyConfig(pitchOffset = 8, label = "G#", whiteIndexLeft = 4),
        BlackKeyConfig(pitchOffset = 10, label = "A#", whiteIndexLeft = 5),
        BlackKeyConfig(pitchOffset = 13, label = "C#+", whiteIndexLeft = 7),
        BlackKeyConfig(pitchOffset = 15, label = "D#+", whiteIndexLeft = 8)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(keyHeight)
            .background(Color(0xFF2A2C35), RoundedCornerShape(24.dp))
            .padding(8.dp)
            .testTag("piano_keyboard_container")
    ) {
        // Measure white key widths dynamically to position black keys directly over the division cracks!
        Box(modifier = Modifier.fillMaxSize()) {
            // Render White Keys
            Row(modifier = Modifier.fillMaxSize()) {
                whiteKeyOffsets.forEachIndexed { index, pitchOffset ->
                    val pitch = startPitch + pitchOffset
                    val isPressed = activeKeys.contains(pitch)
                    val label = whiteKeyLabels[index]

                    WhitePianoKey(
                        pitch = pitch,
                        isPressed = isPressed,
                        label = label,
                        onPress = { vel -> onKeyPress(pitch, vel) },
                        onRelease = { onKeyRelease(pitch) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 2.dp)
                            .testTag("white_key_$pitch")
                    )
                }
            }

            // Render Black Keys absolute overlay
            // Position black keys over the cracks of the white keys
            with(LocalDensity.current) {
                // Calculate position relative to white key fractions
                val totalWhiteKeys = whiteKeyOffsets.size
                val whiteKeyWidthFraction = 1f / totalWhiteKeys

                blackKeys.forEach { config ->
                    val pitch = startPitch + config.pitchOffset
                    val isPressed = activeKeys.contains(pitch)
                    
                    // Center the black key over the boundary of (whiteIndexLeft) and (whiteIndexLeft + 1)
                    val alignmentOffsetPercent = (config.whiteIndexLeft + 1).toFloat() / totalWhiteKeys.toFloat()

                    // Estimate dimensions
                    val blackKeyWidth = 26.dp
                    val blackKeyHeight = keyHeight * 0.58f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(whiteKeyWidthFraction)
                            .height(blackKeyHeight)
                            .align(Alignment.TopStart)
                            // Offset proportionally, shifting left by half of black key width to center over the gap
                            .offset(
                                x = (alignmentOffsetPercent * 100).percentWidthOffset() - (blackKeyWidth / 2),
                                y = 0.dp
                            )
                            .padding(horizontal = 1.dp)
                            .testTag("black_key_$pitch")
                    ) {
                        BlackPianoKey(
                            pitch = pitch,
                            isPressed = isPressed,
                            label = config.label,
                            onPress = { vel -> onKeyPress(pitch, vel) },
                            onRelease = { onKeyRelease(pitch) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

private data class BlackKeyConfig(
    val pitchOffset: Int,
    val label: String,
    val whiteIndexLeft: Int // The white key index to the left of this black key
)

// Helper to calculate percent horizontal offsets inside absolute Box layout
@Composable
fun Float.percentWidthOffset(): Dp {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val keyboardPadding = 16.dp
    val screenWidth = configuration.screenWidthDp.dp - keyboardPadding
    return screenWidth * (this / 100f)
}

@Composable
fun WhitePianoKey(
    pitch: Int,
    isPressed: Boolean,
    label: String,
    onPress: (Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic sinking depth and squish shape when pressed
    val elevation by animateDpAsState(if (isPressed) 1.dp else 5.dp, label = "key_elev")
    val squishScale by animateFloatAsState(if (isPressed) 0.96f else 1.0f, label = "key_squish")

    val bgBrush = if (isPressed) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFE2DDD9), Color(0xFFD6CFCB))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFFBF8F6))
        )
    }

    Box(
        modifier = modifier
            .claymorphicShadow(
                backgroundColor = if (isPressed) Color(0xFFD6CFCB) else Color(0xFFFBF8F6),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, topEnd = 4.dp),
                elevation = elevation,
                isPressed = isPressed
            )
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, topEnd = 4.dp))
            .background(bgBrush)
            .pianoTouchKey(pitch, onPress, onRelease)
            .drawBehind {
                // Soft bloated edges
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0x99FFFFFF), Color.Transparent),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.4f, size.height * 0.1f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
                // Bottom-edge dark clay indent
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x18000000)),
                        startY = size.height * 0.85f,
                        endY = size.height
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )
            }
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) Color(0xFF7E7266) else Color(0xFFB0A499)
            )
            Text(
                text = pitch.toString(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                color = if (isPressed) Color(0xFF9E9286) else Color(0xFFD0C4B9)
            )
        }
    }
}

@Composable
fun BlackPianoKey(
    pitch: Int,
    isPressed: Boolean,
    label: String,
    onPress: (Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elevation by animateDpAsState(if (isPressed) 1.dp else 4.dp, label = "black_key_elev")
    val blackKeyColor = if (isPressed) Color(0xFF4A4D59) else Color(0xFF1E2028)

    Box(
        modifier = modifier
            .claymorphicShadow(
                backgroundColor = blackKeyColor,
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp, topStart = 2.dp, topEnd = 2.dp),
                elevation = elevation,
                isPressed = isPressed
            )
            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp, topStart = 2.dp, topEnd = 2.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFF383A42), Color(0xFF26272E))
                    } else {
                        listOf(Color(0xFF2E313D), Color(0xFF13141A))
                    }
                )
            )
            .pianoTouchKey(pitch, onPress, onRelease)
            .drawBehind {
                // Shiny high-contrast white rim on the left for clay dimension
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0x30FFFFFF), Color.Transparent)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
                // Bottom edge shadows
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x60000000)),
                        startY = size.height * 0.8f,
                        endY = size.height
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }
            .padding(bottom = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) Color(0xFF9EA3B5) else Color(0xFF6E7385)
            )
            Text(
                text = pitch.toString(),
                fontSize = 7.sp,
                fontWeight = FontWeight.Normal,
                color = if (isPressed) Color(0xFF7E8395) else Color(0xFF4E5365)
            )
        }
    }
}
