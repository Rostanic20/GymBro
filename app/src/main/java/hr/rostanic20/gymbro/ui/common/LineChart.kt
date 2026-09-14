package hr.rostanic20.gymbro.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import hr.rostanic20.gymbro.ui.theme.LocalSpacing

data class ChartPoint(val x: Float, val y: Float)

enum class ChartStyle { LINE, DOTS }

data class ChartSeries(val points: List<ChartPoint>, val color: Color, val style: ChartStyle)

data class ChartBounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float)

private const val FLAT_Y_PADDING = 1f
const val MIN_CHART_POINTS = 2

fun chartBounds(points: List<ChartPoint>): ChartBounds? {
    if (points.size < MIN_CHART_POINTS) return null
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }.let { if (it == minX) minX + 1f else it }
    val lowY = points.minOf { it.y }
    val highY = points.maxOf { it.y }
    return if (lowY == highY) {
        ChartBounds(minX, maxX, lowY - FLAT_Y_PADDING, highY + FLAT_Y_PADDING)
    } else {
        ChartBounds(minX, maxX, lowY, highY)
    }
}

fun ChartPoint.toCanvas(bounds: ChartBounds, width: Float, height: Float): Offset = Offset(
    x = (x - bounds.minX) / (bounds.maxX - bounds.minX) * width,
    y = height - (y - bounds.minY) / (bounds.maxY - bounds.minY) * height,
)

@Composable
fun LineChart(series: List<ChartSeries>, modifier: Modifier = Modifier) {
    val bounds = chartBounds(series.flatMap { it.points }) ?: return
    val spacing = LocalSpacing.current
    val density = LocalDensity.current
    val strokeWidth = with(density) { spacing.s2.toPx() }
    val dotRadius = with(density) { spacing.s4.toPx() } * 3 / 4
    Canvas(modifier = modifier) {
        val inset = dotRadius
        val width = size.width - inset * 2
        val height = size.height - inset * 2
        series.forEach { line ->
            val offsets = line.points.map { it.toCanvas(bounds, width, height) + Offset(inset, inset) }
            when (line.style) {
                ChartStyle.LINE -> if (offsets.size >= MIN_CHART_POINTS) {
                    val path = Path().apply {
                        moveTo(offsets.first().x, offsets.first().y)
                        offsets.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path = path, color = line.color, style = Stroke(width = strokeWidth))
                }
                ChartStyle.DOTS -> offsets.forEach { drawCircle(color = line.color, radius = dotRadius, center = it) }
            }
        }
    }
}
