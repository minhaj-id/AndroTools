package com.bimantara.feature.notes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

data class DrawnStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false,
    val alpha: Float = 1.0f
)

@Composable
fun StylusCanvas(
    strokes: MutableList<DrawnStroke>,
    selectedColor: Color,
    strokeWidth: Float,
    isEraser: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White
) {
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(selectedColor, strokeWidth, isEraser) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPoints = listOf(offset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentPoints = currentPoints + change.position
                    },
                    onDragEnd = {
                        if (currentPoints.isNotEmpty()) {
                            strokes.add(
                                DrawnStroke(
                                    points = currentPoints,
                                    color = if (isEraser) backgroundColor else selectedColor,
                                    strokeWidth = if (isEraser) strokeWidth * 2.5f else strokeWidth,
                                    isEraser = isEraser,
                                    alpha = if (selectedColor.alpha < 1f) selectedColor.alpha else 1f
                                )
                            )
                            currentPoints = emptyList()
                        }
                    },
                    onDragCancel = {
                        currentPoints = emptyList()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw completed strokes
            for (stroke in strokes) {
                if (stroke.points.size > 1) {
                    val path = Path().apply {
                        moveTo(stroke.points.first().x, stroke.points.first().y)
                        for (i in 1 until stroke.points.size) {
                            lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = stroke.color,
                        alpha = stroke.alpha,
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else if (stroke.points.size == 1) {
                    drawCircle(
                        color = stroke.color,
                        radius = stroke.strokeWidth / 2,
                        center = stroke.points.first(),
                        alpha = stroke.alpha
                    )
                }
            }

            // Draw currently active in-progress stroke
            if (currentPoints.size > 1) {
                val activePath = Path().apply {
                    moveTo(currentPoints.first().x, currentPoints.first().y)
                    for (i in 1 until currentPoints.size) {
                        lineTo(currentPoints[i].x, currentPoints[i].y)
                    }
                }
                drawPath(
                    path = activePath,
                    color = if (isEraser) backgroundColor else selectedColor,
                    style = Stroke(
                        width = if (isEraser) strokeWidth * 2.5f else strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
