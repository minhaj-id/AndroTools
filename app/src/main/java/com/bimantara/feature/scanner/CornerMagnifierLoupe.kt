package com.bimantara.feature.scanner

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Magnified view overlay (Loupe) that appears dynamically when the user drags a corner handle
 * in the manual document cropping & perspective rectification tool.
 *
 * Provides ultra-precise sub-pixel alignment visual feedback with high-contrast crosshairs,
 * corner labeling, and pixel coordinates.
 */
@Composable
fun CornerMagnifierLoupe(
    bitmap: Bitmap,
    bmpPoint: Offset,
    touchPosition: Offset,
    cornerIndex: Int,
    containerSize: Size,
    modifier: Modifier = Modifier,
    zoomFactor: Float = 2.5f,
    loupeDiameter: Dp = 124.dp
) {
    val density = LocalDensity.current
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    val loupePx = with(density) { loupeDiameter.toPx() }
    val marginPx = with(density) { 12.dp.toPx() }
    val offsetAbovePx = with(density) { 48.dp.toPx() }
    val offsetBelowPx = with(density) { 48.dp.toPx() }

    // Calculate clamped horizontal position
    val targetX = touchPosition.x - (loupePx / 2f)
    val maxAvailableW = if (containerSize.width > 0f) containerSize.width else 1000f
    val maxAvailableH = if (containerSize.height > 0f) containerSize.height else 1000f

    val clampedX = targetX.coerceIn(marginPx, (maxAvailableW - loupePx - marginPx).coerceAtLeast(marginPx))

    // Position vertically: prefer placing above the finger; if near top edge, place below finger
    val isAbove = (touchPosition.y - loupePx - offsetAbovePx) >= marginPx
    val targetY = if (isAbove) {
        touchPosition.y - loupePx - offsetAbovePx
    } else {
        touchPosition.y + offsetBelowPx
    }
    val clampedY = targetY.coerceIn(marginPx, (maxAvailableH - loupePx - marginPx).coerceAtLeast(marginPx))

    val cornerName = when (cornerIndex) {
        0 -> "Kiri Atas (TL)"
        1 -> "Kanan Atas (TR)"
        2 -> "Kanan Bawah (BR)"
        3 -> "Kiri Bawah (BL)"
        else -> "Sudut #${cornerIndex + 1}"
    }

    // Optional connecting dashed pointer line
    Canvas(
        modifier = Modifier
            .offset { IntOffset(0, 0) }
            .size(
                width = with(density) { maxAvailableW.toDp() },
                height = with(density) { maxAvailableH.toDp() }
            )
    ) {
        val loupeCenter = Offset(clampedX + loupePx / 2f, clampedY + loupePx / 2f)
        // Draw thin connecting guide line from loupe edge to touch point
        val dir = touchPosition - loupeCenter
        val dist = dir.getDistance()
        if (dist > loupePx / 2f + 8f) {
            val start = loupeCenter + (dir / dist) * (loupePx / 2f)
            drawLine(
                color = Color(0x6610B981),
                start = start,
                end = touchPosition,
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )
            drawCircle(
                color = Color(0xFF10B981),
                radius = 3.dp.toPx(),
                center = touchPosition
            )
        }
    }

    // Main Loupe Bubble
    Box(
        modifier = modifier
            .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
            .size(loupeDiameter)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF0F172A))
            .border(3.dp, Color.White, CircleShape)
            .border(4.5.dp, Color(0xFF10B981).copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // High-performance hardware-accelerated magnified canvas
        Canvas(modifier = Modifier.size(loupeDiameter)) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = size.width / 2f

            val circleClip = Path().apply {
                addOval(Rect(0f, 0f, size.width, size.height))
            }

            clipPath(circleClip) {
                // Background dark fill
                drawRect(Color(0xFF111827))

                // Transform and draw the magnified bitmap
                withTransform({
                    translate(centerX, centerY)
                    scale(zoomFactor, zoomFactor, Offset.Zero)
                    translate(-bmpPoint.x, -bmpPoint.y)
                }) {
                    drawImage(imageBitmap)
                }

                // Concentric reference rings
                drawCircle(
                    color = Color.White.copy(alpha = 0.18f),
                    radius = radius * 0.5f,
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
                )

                // High-precision Crosshairs (Dual-tone for dark & light documents)
                val chLen = 22.dp.toPx()
                val chGap = 4.dp.toPx()

                // 1. Dark drop shadow for contrast
                drawLine(
                    color = Color.Black.copy(alpha = 0.65f),
                    start = Offset(centerX - chLen, centerY),
                    end = Offset(centerX - chGap, centerY),
                    strokeWidth = 3.5.dp.toPx()
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.65f),
                    start = Offset(centerX + chGap, centerY),
                    end = Offset(centerX + chLen, centerY),
                    strokeWidth = 3.5.dp.toPx()
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.65f),
                    start = Offset(centerX, centerY - chLen),
                    end = Offset(centerX, centerY - chGap),
                    strokeWidth = 3.5.dp.toPx()
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.65f),
                    start = Offset(centerX, centerY + chGap),
                    end = Offset(centerX, centerY + chLen),
                    strokeWidth = 3.5.dp.toPx()
                )

                // 2. Vivid emerald precision crosshairs
                val accentColor = Color(0xFF10B981)
                drawLine(
                    color = accentColor,
                    start = Offset(centerX - chLen, centerY),
                    end = Offset(centerX - chGap, centerY),
                    strokeWidth = 1.8.dp.toPx()
                )
                drawLine(
                    color = accentColor,
                    start = Offset(centerX + chGap, centerY),
                    end = Offset(centerX + chLen, centerY),
                    strokeWidth = 1.8.dp.toPx()
                )
                drawLine(
                    color = accentColor,
                    start = Offset(centerX, centerY - chLen),
                    end = Offset(centerX, centerY - chGap),
                    strokeWidth = 1.8.dp.toPx()
                )
                drawLine(
                    color = accentColor,
                    start = Offset(centerX, centerY + chGap),
                    end = Offset(centerX, centerY + chLen),
                    strokeWidth = 1.8.dp.toPx()
                )

                // 3. Center target reticle
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = 3.dp.toPx(),
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = accentColor,
                    radius = 1.5.dp.toPx(),
                    center = Offset(centerX, centerY)
                )

                // Lens glare effect at top-left rim
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = radius * 0.85f,
                    center = Offset(centerX - radius * 0.3f, centerY - radius * 0.3f),
                    style = Stroke(width = 8.dp.toPx())
                )
            }
        }

        // Overlay Pill Badge at bottom showing corner name & coordinate
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = cornerName,
                color = Color(0xFF34D399),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 10.sp
            )
            Text(
                text = "${bmpPoint.x.toInt()}, ${bmpPoint.y.toInt()} px",
                color = Color.LightGray,
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 9.sp
            )
        }
    }
}
