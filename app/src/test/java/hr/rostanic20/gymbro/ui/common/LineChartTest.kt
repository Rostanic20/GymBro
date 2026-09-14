package hr.rostanic20.gymbro.ui.common

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LineChartTest {

    @Test
    fun `a single point is not enough for a chart`() {
        assertNull(chartBounds(listOf(ChartPoint(1f, 70f))))
    }

    @Test
    fun `a flat series gets vertical room so it draws mid-chart`() {
        val bounds = chartBounds(listOf(ChartPoint(0f, 70f), ChartPoint(7f, 70f)))!!

        assertEquals(ChartBounds(0f, 7f, 69f, 71f), bounds)
        assertEquals(Offset(100f, 50f), ChartPoint(7f, 70f).toCanvas(bounds, width = 100f, height = 100f))
    }

    @Test
    fun `the lowest value sits at the bottom and the highest at the top`() {
        val bounds = chartBounds(listOf(ChartPoint(0f, 60f), ChartPoint(10f, 80f)))!!

        assertEquals(Offset(0f, 200f), ChartPoint(0f, 60f).toCanvas(bounds, width = 300f, height = 200f))
        assertEquals(Offset(300f, 0f), ChartPoint(10f, 80f).toCanvas(bounds, width = 300f, height = 200f))
    }
}
