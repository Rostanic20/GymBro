package hr.rostanic20.gymbro.ui.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import hr.rostanic20.gymbro.MainActivity
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.core.WidgetUpdater
import hr.rostanic20.gymbro.ui.common.formatCount
import hr.rostanic20.gymbro.ui.common.labelRes
import hr.rostanic20.gymbro.ui.theme.GymBroDarkColors
import hr.rostanic20.gymbro.ui.theme.GymBroLightColors
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.mp.KoinPlatform
import java.time.format.DateTimeFormatter

private val widgetColors = ColorProviders(light = GymBroLightColors, dark = GymBroDarkColors)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val cornerRadius = 20.dp
private val compactSize = DpSize(140.dp, 140.dp)
private val wideSize = DpSize(250.dp, 110.dp)
private val ringSize = 96.dp

class TodayWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(compactSize, wideSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loader = KoinPlatform.getKoin().get<TodayWidgetLoader>()
        val data = loader.data()
        provideContent {
            GlanceTheme(colors = widgetColors) {
                val current by data.collectAsState(initial = null)
                val compact = LocalSize.current.width < wideSize.width
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(cornerRadius)
                        .padding(horizontal = if (compact) 8.dp else 16.dp, vertical = 12.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start,
                ) {
                    current?.let { if (compact) CompactContent(it) else WideContent(it) }
                }
            }
        }
    }

    @Composable
    private fun ColumnScope.CompactContent(data: TodayWidgetData) {
        val context = LocalContext.current
        val locale = context.resources.configuration.locales[0]
        Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.size(ringSize)) {
            Image(
                provider = ImageProvider(ring(context, data)),
                contentDescription = null,
                modifier = GlanceModifier.size(ringSize),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatCount(data.kcalLeft.coerceAtLeast(0), locale),
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                        textAlign = TextAlign.Center,
                    ),
                )
                Text(
                    text = context.getString(R.string.widget_kcal_short),
                    style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = compactMealLabel(context, data),
            style = TextStyle(
                fontSize = 12.sp,
                color = GlanceTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            ),
        )
    }

    @Composable
    private fun ColumnScope.WideContent(data: TodayWidgetData) {
        val context = LocalContext.current
        val locale = context.resources.configuration.locales[0]
        Row(verticalAlignment = Alignment.Bottom, modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = context.getString(
                    R.string.widget_kcal_left,
                    formatCount(data.kcalLeft.coerceAtLeast(0), locale),
                ),
                style = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = context.getString(
                    R.string.widget_protein,
                    formatCount(data.proteinEaten, locale),
                    formatCount(data.proteinTarget, locale),
                ),
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant),
            )
        }
        Spacer(modifier = GlanceModifier.height(10.dp))
        LinearProgressIndicator(
            progress = progressOf(data),
            color = GlanceTheme.colors.primary,
            backgroundColor = GlanceTheme.colors.surfaceVariant,
            modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = wideMealLabel(context, data),
            style = TextStyle(fontSize = 15.sp, color = GlanceTheme.colors.onSurface),
        )
        if (canLogPlanned(data)) {
            Spacer(modifier = GlanceModifier.height(10.dp))
            FilledButton(
                text = context.getString(R.string.widget_log_planned),
                onClick = actionRunCallback<LogPlannedMealAction>(),
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }

    private fun ring(context: Context, data: TodayWidgetData) = progressRing(
        sizePx = (ringSize.value * context.resources.displayMetrics.density).toInt(),
        progress = progressOf(data),
        ringColor = colors(context).first,
        trackColor = colors(context).second,
    )

    private fun colors(context: Context): Pair<Int, Int> {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        val scheme = if (night) GymBroDarkColors else GymBroLightColors
        return scheme.primary.toArgb() to scheme.surfaceVariant.toArgb()
    }

    private fun canLogPlanned(data: TodayWidgetData): Boolean =
        data.nextMeal != null && data.hasPlannedRecipe && !data.alreadyLogged

    private fun progressOf(data: TodayWidgetData): Float =
        if (data.kcalTarget <= 0) 0f else (data.kcalEaten.toFloat() / data.kcalTarget).coerceIn(0f, 1f)

    private fun compactMealLabel(context: Context, data: TodayWidgetData): String {
        val meal = data.nextMeal ?: return context.getString(R.string.widget_day_done)
        val time = data.nextMealTime?.format(timeFormat).orEmpty()
        return if (data.alreadyLogged) {
            context.getString(R.string.widget_next_logged, time, context.getString(meal.labelRes))
        } else {
            context.getString(R.string.widget_compact_next, time, context.getString(meal.labelRes))
        }
    }

    private fun wideMealLabel(context: Context, data: TodayWidgetData): String {
        val meal = data.nextMeal ?: return context.getString(R.string.widget_day_done)
        val name = context.getString(meal.labelRes)
        val time = data.nextMealTime?.format(timeFormat).orEmpty()
        return if (data.alreadyLogged) {
            context.getString(R.string.widget_next_logged, time, name)
        } else {
            context.getString(R.string.widget_next, time, name)
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

class GlanceWidgetUpdater(private val context: Context) : WidgetUpdater {
    override suspend fun refresh() {
        TodayWidget().updateAll(context)
    }
}

class LogPlannedMealAction : ActionCallback, KoinComponent {

    private val loader: TodayWidgetLoader by inject()

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        loader.logPlanned()
        TodayWidget().updateAll(context)
    }
}
