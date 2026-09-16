package hr.rostanic20.gymbro.ui.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.updateAll
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

class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loader = KoinPlatform.getKoin().get<TodayWidgetLoader>()
        val data = loader.load()
        provideContent {
            GlanceTheme(colors = widgetColors) {
                Scaffold(
                    horizontalPadding = 12.dp,
                    modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
                ) {
                    WidgetContent(context = context, data = data)
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(context: Context, data: TodayWidgetData) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.Bottom, modifier = GlanceModifier.fillMaxWidth()) {
                val locale = context.resources.configuration.locales[0]
                Text(
                    text = context.getString(
                        R.string.widget_kcal_left,
                        formatCount(data.kcalLeft.coerceAtLeast(0), locale),
                    ),
                    style = TextStyle(
                        fontSize = 24.sp,
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
            Spacer(modifier = GlanceModifier.height(8.dp))
            LinearProgressIndicator(
                progress = progressOf(data),
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.surfaceVariant,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp),
            )
            Spacer(modifier = GlanceModifier.height(12.dp))
            Text(
                text = nextMealLabel(context, data),
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            if (data.plannedRecipe != null && !data.alreadyLogged) {
                androidx.glance.appwidget.components.FilledButton(
                    text = context.getString(R.string.widget_log_planned),
                    onClick = actionRunCallback<LogPlannedMealAction>(),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
            }
        }
    }

    private fun progressOf(data: TodayWidgetData): Float =
        if (data.kcalTarget <= 0) 0f else (data.kcalEaten.toFloat() / data.kcalTarget).coerceIn(0f, 1f)

    private fun nextMealLabel(context: Context, data: TodayWidgetData): String {
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

class LogPlannedMealAction : androidx.glance.appwidget.action.ActionCallback, KoinComponent {

    private val loader: TodayWidgetLoader by inject()

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters,
    ) {
        loader.logPlanned()
        TodayWidget().updateAll(context)
    }
}
