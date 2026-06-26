package com.example.ui.clay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom modifier that renders claymorphic outer shadows and inner bloating gradients.
 */
fun Modifier.claymorphicShadow(
    backgroundColor: Color,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    elevation: Dp = 8.dp,
    isPressed: Boolean = false,
    accentColor: Color? = null
) = this.drawBehind {
    val radius = shape.topStart.toPx(size, this)
    val density = this.density

    // 1. Calculate shadow metrics dynamically based on press state
    val scale = if (isPressed) 0.6f else 1.0f
    val shadowBlur = (elevation.value * 1.5f * scale).coerceAtLeast(1f)
    val shadowOffset = (elevation.value * scale).coerceAtLeast(1f)

    // Outer dark shadow (down-right)
    val darkShadowColor = Color(0x33000000).toArgb()
    
    // Outer white ambient light glow (up-left)
    val lightShadowColor = Color(0x33FFFFFF).toArgb()

    drawIntoCanvas { canvas ->
        // Draw bottom-right soft shadow
        val paintDark = Paint().asFrameworkPaint().apply {
            color = backgroundColor.toArgb()
            setShadowLayer(
                shadowBlur * density,
                shadowOffset * density,
                shadowOffset * density,
                darkShadowColor
            )
        }
        canvas.nativeCanvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            radius, radius,
            paintDark
        )

        // Draw top-left soft glow
        val paintLight = Paint().asFrameworkPaint().apply {
            color = backgroundColor.toArgb()
            setShadowLayer(
                (shadowBlur * 0.8f) * density,
                -shadowOffset * 0.5f * density,
                -shadowOffset * 0.5f * density,
                lightShadowColor
            )
        }
        canvas.nativeCanvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            radius, radius,
            paintLight
        )
    }
}

@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFF5EBE0), // Default Warm Clay Peach
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    elevation: Dp = 8.dp,
    accentColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .claymorphicShadow(
                backgroundColor = backgroundColor,
                shape = shape,
                elevation = elevation,
                isPressed = false,
                accentColor = accentColor
            )
            .clip(shape)
            .background(backgroundColor)
            .drawBehind {
                // Draw Inner Bloating Highlights and Shadows using overlapping linear gradients
                
                // Top-Left Soft Rim Highlight
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xAAFFFFFF), Color.Transparent),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.4f, size.height * 0.4f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(shape.topStart.toPx(size, this))
                )

                // Bottom-Right Deep Contour Shade
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color(0x2A000000)),
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.6f, size.height * 0.6f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(shape.topStart.toPx(size, this))
                )

                // Subtle inner accent rim if specified
                if (accentColor != null) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(shape.topStart.toPx(size, this))
                    )
                }
            }
            .padding(16.dp),
        content = content
    )
}

@Composable
fun ClayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFC4E0E5), // Default Soft Blue Clay
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    elevation: Dp = 6.dp,
    enabled: Boolean = true,
    accentColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile squish animations
    val scaleY by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "squish_y"
    )
    val scaleX by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "squish_x"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                this.scaleX = scaleX
                this.scaleY = scaleY
            }
            .claymorphicShadow(
                backgroundColor = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
                shape = shape,
                elevation = if (isPressed) elevation * 0.3f else elevation,
                isPressed = isPressed || !enabled,
                accentColor = accentColor
            )
            .clip(shape)
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.6f))
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable standard ripple as claymorphism squishes dynamically!
                enabled = enabled,
                onClick = onClick
            )
            .drawBehind {
                // Inner Highlights (Soft swollen top edge)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xCCFFFFFF), Color.Transparent),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.3f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(shape.topStart.toPx(size, this))
                )

                // Inner Shadow (Bottom edge contour)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color(0x30000000)),
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height * 0.7f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(shape.topStart.toPx(size, this))
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ClaySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    barColor: Color = Color(0xFFECE6E2),
    knobColor: Color = Color(0xFFFFD1DC) // Pastel Pink knob
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        // The track slider background (Claymorphic channel)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .claymorphicShadow(backgroundColor = barColor, shape = RoundedCornerShape(8.dp), elevation = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(barColor)
                .drawBehind {
                    // Track inner highlights and deep shadow representing a hollowed-out ditch
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x33000000), Color(0x11FFFFFF))
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                    )
                }
        )

        // Floating dynamic slider track indicator and draggable thumb
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Color.Transparent, // Hidden behind our custom clay knob
                activeTrackColor = knobColor.copy(alpha = 0.5f),
                inactiveTrackColor = Color.Transparent
            ),
            thumb = {
                // Custom claymorphic thumb knob
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .claymorphicShadow(backgroundColor = knobColor, shape = RoundedCornerShape(50), elevation = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(knobColor)
                        .drawBehind {
                            // Knobs soft bloated shine
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xEEFFFFFF), Color.Transparent),
                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.25f),
                                    radius = size.width * 0.4f
                                )
                            )
                        }
                )
            }
        )
    }
}

@Composable
fun ClayToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFC4E0E5), // Soft Blue
    inactiveColor: Color = Color(0xFFECE6E2) // Clay Grey
) {
    val trackBg = if (checked) activeColor else inactiveColor
    val alignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    
    Box(
        modifier = modifier
            .size(width = 64.dp, height = 36.dp)
            .claymorphicShadow(backgroundColor = trackBg, shape = RoundedCornerShape(18.dp), elevation = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(trackBg)
            .clickable { onCheckedChange(!checked) }
            .drawBehind {
                // Inner hollow
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x18000000), Color(0x05FFFFFF))
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
                )
            }
            .padding(4.dp),
        contentAlignment = alignment
    ) {
        // Swollen floating knob
        Box(
            modifier = Modifier
                .size(28.dp)
                .claymorphicShadow(backgroundColor = Color.White, shape = RoundedCornerShape(50), elevation = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xAAFFFFFF), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.3f),
                            radius = size.width * 0.5f
                        )
                    )
                }
        )
    }
}
