package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.clay.claymorphicShadow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs

@Composable
fun HolisticMorphingView(
    amplitude: Float,
    activePitches: List<Int>,
    rubatoDeviation: Float, // -1f (early) to 1f (late)
    beatProgress: Float,    // 0f to 1f
    currentBeat: Int,       // 0 to 3
    modifier: Modifier = Modifier
) {
    // Infinite transition for continuous wave animation of the clay blob
    val timeAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        timeAnim.animateTo(
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing)
            )
        )
    }

    // Dynamic coloring based on voicing density and rubato expression
    val activeNotesCount = activePitches.size
    val isDissonant = remember(activePitches) { checkDissonance(activePitches) }
    
    // Core clay theme colors: Soft Peach/Rose (consonant, gentle), transitioning to Violet/Lavender (dense, chordal), or Spiky Ochre (dissonant)
    val clayColor = when {
        isDissonant -> Color(0xFFFBC4B6) // spiky dissonant orange/peach
        activeNotesCount > 3 -> Color(0xFFE1BEE7) // dense chord lavender
        activeNotesCount > 0 -> Color(0xFFFFD1DC) // melodic soft pink
        else -> Color(0xFFF5EBE0) // resting warm sand
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .claymorphicShadow(backgroundColor = Color(0xFF23252F), shape = RoundedCornerShape(24.dp), elevation = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF23252F))
            .padding(12.dp)
            .testTag("holistic_morphing_card")
    ) {
        // Aesthetic Grid lines representing metronomic structural guidelines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val gridColor = Color(0x0CFFFFFF)
            
            // Draw radial target coordinates (bullseye structure)
            drawCircle(color = gridColor, radius = h * 0.25f, center = Offset(w/2, h/2), style = Stroke(1.dp.toPx()))
            drawCircle(color = gridColor, radius = h * 0.40f, center = Offset(w/2, h/2), style = Stroke(1.dp.toPx()))

            // Crosshairs
            drawLine(color = gridColor, start = Offset(w/2, h * 0.05f), end = Offset(w/2, h * 0.95f), strokeWidth = 1.dp.toPx())
            drawLine(color = gridColor, start = Offset(w * 0.1f, h/2), end = Offset(w * 0.9f, h/2), strokeWidth = 1.dp.toPx())
        }

        // Active Interactive Morphing Layer
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("holistic_morphing_canvas")
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = size.height * 0.23f
            val time = timeAnim.value.toDouble()

            // 1. Draw Strict Metronomic Orbit Ring (Precision clock)
            val orbitRadius = size.height * 0.41f
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(Color(0x22FFFFFF), Color(0x66C4E0E5), Color(0x22FFFFFF))
                ),
                radius = orbitRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw 4 metronomic beat coordinate nodes (at 0, 90, 180, 270 degrees)
            val angles = listOf(-PI/2, 0.0, PI/2, PI) // beat 1 (top), 2 (right), 3 (bottom), 4 (left)
            angles.forEachIndexed { idx, angle ->
                val bx = cx + orbitRadius * cos(angle).toFloat()
                val by = cy + orbitRadius * sin(angle).toFloat()
                
                // If it is the current active beat, we pulse it visually
                val isActiveBeat = idx == currentBeat
                val nodeScale = if (isActiveBeat) (1f + (1f - beatProgress) * 0.6f) else 1f
                val nodeColor = if (isActiveBeat) Color(0xFFC4E0E5) else Color(0x33FFFFFF)
                val nodeRadius = (if (isActiveBeat) 9.dp else 5.dp).toPx() * nodeScale

                // Node clay glow
                drawCircle(
                    color = nodeColor,
                    radius = nodeRadius,
                    center = Offset(bx, by)
                )
                if (isActiveBeat) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = nodeRadius * 1.5f,
                        center = Offset(bx, by),
                        style = Stroke(2.dp.toPx())
                    )
                }
            }

            // 2. Calculate the center displacement for Rubato stretching
            // If the deviation is early (negative), displace blob left/forward. Late (positive) is right/backwards.
            val targetDisplacementX = rubatoDeviation * 35.dp.toPx()
            val targetDisplacementY = if (activeNotesCount > 0) sin(time).toFloat() * 6.dp.toPx() else 0f
            
            val blobCx = cx + targetDisplacementX
            val blobCy = cy + targetDisplacementY

            // 3. Render the connecting rubber-band elastic line showing "metronomic flexibility"
            // Displays how the emotive center is connected to the rigid beat node
            if (activeNotesCount > 0) {
                val activeBeatAngle = angles[currentBeat]
                val activeBeatX = cx + orbitRadius * cos(activeBeatAngle).toFloat()
                val activeBeatY = cy + orbitRadius * sin(activeBeatAngle).toFloat()

                // Draw a dynamic bezier line representing the tension of metronomic flex
                val flexPath = Path().apply {
                    moveTo(blobCx, blobCy)
                    quadraticTo(
                        (blobCx + activeBeatX) / 2f + sin(time).toFloat() * 15.dp.toPx(),
                        (blobCy + activeBeatY) / 2f + cos(time).toFloat() * 15.dp.toPx(),
                        activeBeatX,
                        activeBeatY
                    )
                }
                drawPath(
                    path = flexPath,
                    color = Color(0x55C4E0E5),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 4. Calculate coordinates for the 120-point organic morphing polygon
            val numPoints = 120
            val path = Path()
            
            // Generate list of active pitches wrapped to a 12-semitone circle
            val pitchClasses = activePitches.map { it % 12 }

            for (i in 0 until numPoints) {
                val theta = (i.toDouble() / numPoints.toDouble()) * 2.0 * PI
                
                // Base structure + general slow breathing wave
                var r = baseRadius + (sin(5 * theta + time * 1.5) * 8.dp.toPx() * (0.3f + amplitude * 1.2f))
                
                // Inflate overall shape based on audio amplitude
                r += amplitude * 45.dp.toPx()

                // Add dynamic bulging "lobes" for each sounding note
                // Pitch class map to angle offsets (e.g. C is at top angle, G is at 7/12 around)
                pitchClasses.forEach { pc ->
                    val lobeAngle = (pc.toDouble() / 12.0) * 2.0 * PI - PI/2.0
                    val angleDiff = theta - lobeAngle
                    // Wrap difference to -PI to PI
                    val wrappedDiff = Math.atan2(sin(angleDiff), cos(angleDiff))
                    
                    // Gaussian-like hump for the lobe
                    val lobeFactor = cos(wrappedDiff).coerceAtLeast(0.0)
                    val exponentLobe = Math.pow(lobeFactor, 8.0) // sharp, clean bulge
                    r += (exponentLobe * 28.dp.toPx() * (0.5f + amplitude)).toFloat()
                }

                // Add high-frequency "wrinkles" to represent Dissonance tension
                if (isDissonant) {
                    r += (sin(28 * theta + time * 4.0) * 5.dp.toPx()).toFloat()
                }

                // Apply Rubato stretching: stretch the circle into an oval along the direction of timing deviation
                val stretchFactorX = 1.0f + abs(rubatoDeviation) * 0.15f
                val stretchFactorY = 1.0f - abs(rubatoDeviation) * 0.1f

                val rx = r * stretchFactorX
                val ry = r * stretchFactorY

                val px = (blobCx + rx * cos(theta)).toFloat()
                val py = (blobCy + ry * sin(theta)).toFloat()

                if (i == 0) {
                    path.moveTo(px, py)
                } else {
                    path.lineTo(px, py)
                }
            }
            path.close()

            // 5. Draw the Claymorphic 3D blob
            // Inner color shading gradient
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(clayColor, clayColor.darken(0.12f)),
                    center = Offset(blobCx - 15.dp.toPx(), blobCy - 15.dp.toPx()),
                    radius = baseRadius * 1.6f
                )
            )

            // Overlap inner bloated light shines (3D highlight contour)
            val shinyPath = Path()
            for (i in 0 until numPoints) {
                val theta = (i.toDouble() / numPoints.toDouble()) * 2.0 * PI
                var r = baseRadius + (sin(5 * theta + time * 1.5) * 8.dp.toPx() * (0.3f + amplitude * 1.2f))
                r += amplitude * 45.dp.toPx()
                pitchClasses.forEach { pc ->
                    val lobeAngle = (pc.toDouble() / 12.0) * 2.0 * PI - PI/2.0
                    val angleDiff = theta - lobeAngle
                    val wrappedDiff = Math.atan2(sin(angleDiff), cos(angleDiff))
                    r += (Math.pow(cos(wrappedDiff).coerceAtLeast(0.0), 8.0) * 28.dp.toPx() * (0.5f + amplitude)).toFloat()
                }
                if (isDissonant) r += (sin(28 * theta + time * 4.0) * 5.dp.toPx()).toFloat()

                // Shrink slightly to draw inner rim glow
                val rimR = r * 0.88f
                val px = (blobCx + rimR * cos(theta)).toFloat()
                val py = (blobCy + rimR * sin(theta)).toFloat()

                if (i == 0) shinyPath.moveTo(px, py) else shinyPath.lineTo(px, py)
            }
            shinyPath.close()

            // Draw shiny highlighted crescent overlay (bloom)
            drawPath(
                path = shinyPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                    start = Offset(blobCx - baseRadius * 0.5f, blobCy - baseRadius * 0.5f),
                    end = Offset(blobCx + baseRadius * 0.6f, blobCy + baseRadius * 0.6f)
                )
            )

            // Draw outer sleek clay outline
            drawPath(
                path = path,
                color = clayColor.darken(0.25f).copy(alpha = 0.7f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Live text telemetry overlay indicating Expressive Rubato quality
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val timingText = when {
                rubatoDeviation < -0.3f -> "RUSHING (Micro-rubato early)"
                rubatoDeviation > 0.3f -> "DRAGGING (Micro-rubato late)"
                activeNotesCount > 0 -> "BALANCED Expressive Focus"
                else -> "Awaiting Performance..."
            }
            val harmonyText = when {
                isDissonant -> "TENSE Dissonant Tension"
                activeNotesCount > 2 -> "CONSONANT Voice Structure"
                activeNotesCount > 0 -> "PURE Monophonic Line"
                else -> "Metronomic Framework Stable"
            }

            Text(
                text = timingText,
                color = Color(0xFFC4E0E5),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = harmonyText,
                color = Color(0x88FFFFFF),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Determines whether the active keys contain standard harmonic dissonances
 * (Minor Second = 1 semitone apart, Triton = 6 semitones apart, Major Seventh = 11 semitones)
 */
fun checkDissonance(pitches: List<Int>): Boolean {
    if (pitches.size < 2) return false
    val sorted = pitches.distinct().sorted()
    for (i in 0 until sorted.size - 1) {
        for (j in i + 1 until sorted.size) {
            val interval = abs(sorted[j] - sorted[i]) % 12
            if (interval == 1 || interval == 6 || interval == 11) {
                return true
            }
        }
    }
    return false
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
