package hr.rostanic20.gymbro.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap

private const val START_ANGLE = -90f
private const val FULL_CIRCLE = 360f
private const val STROKE_FRACTION = 0.12f

fun progressRing(sizePx: Int, progress: Float, ringColor: Int, trackColor: Int): Bitmap {
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val stroke = sizePx * STROKE_FRACTION
    val bounds = RectF(stroke / 2, stroke / 2, sizePx - stroke / 2, sizePx - stroke / 2)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }
    paint.color = trackColor
    canvas.drawArc(bounds, START_ANGLE, FULL_CIRCLE, false, paint)
    paint.color = ringColor
    canvas.drawArc(bounds, START_ANGLE, FULL_CIRCLE * progress.coerceIn(0f, 1f), false, paint)
    return bitmap
}
