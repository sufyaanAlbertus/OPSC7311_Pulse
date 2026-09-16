package com.pulse.budget.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin

/** Flat-top hexagon outline used for the app icon, category icons, and badge icons. */
class HexagonShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = minOf(cx, cy)
        for (i in 0..5) {
            val angle = Math.PI / 180.0 * (60 * i - 30)
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

fun hexagonPoints(size: Size): List<Offset> {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = minOf(cx, cy)
    return (0..5).map { i ->
        val angle = Math.PI / 180.0 * (60 * i - 30)
        Offset(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat())
    }
}
