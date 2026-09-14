package hr.rostanic20.gymbro.ui.train

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.TopSetPoint
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.domain.trendValue
import hr.rostanic20.gymbro.ui.LocalSnackbarHostState
import hr.rostanic20.gymbro.ui.ObserveAsEvents
import hr.rostanic20.gymbro.ui.common.ChartPoint
import hr.rostanic20.gymbro.ui.common.ChartSeries
import hr.rostanic20.gymbro.ui.common.ChartStyle
import hr.rostanic20.gymbro.ui.common.LineChart
import hr.rostanic20.gymbro.ui.common.MIN_CHART_POINTS
import hr.rostanic20.gymbro.ui.common.Tag
import hr.rostanic20.gymbro.ui.common.formatKg
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import hr.rostanic20.gymbro.ui.workout.ExerciseItem
import hr.rostanic20.gymbro.ui.workout.LoadSettings
import hr.rostanic20.gymbro.ui.workout.LoadSettingsDialogHost
import hr.rostanic20.gymbro.ui.workout.WeekBanner
import hr.rostanic20.gymbro.ui.workout.showsWeekBanner
import org.koin.compose.viewmodel.koinViewModel
import java.time.format.TextStyle

@Composable
fun TrainScreen(viewModel: TrainViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.messages) { snackbarHostState.showSnackbar(resources.getString(it.text)) }
    state?.let { TrainContent(state = it, onSaveLoads = viewModel::updateLoadSettings) }
}

@Composable
private fun TrainContent(
    state: TrainUiState,
    onSaveLoads: (exerciseId: Long, settings: LoadSettings) -> Unit,
) {
    val spacing = LocalSpacing.current
    val listState = rememberLazyListState()
    val week = state.week
    val showBanner = week != null && showsWeekBanner(week)
    var editingExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var scrolledToToday by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.defaultExpandedDayId) {
        if (scrolledToToday) return@LaunchedEffect
        val dayIndex = state.days.indexOfFirst { it.id == state.defaultExpandedDayId }
        if (dayIndex > 0) listState.scrollToItem(dayIndex + if (showBanner) 3 else 2)
        scrolledToToday = true
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.s16),
        verticalArrangement = Arrangement.spacedBy(spacing.s16),
    ) {
        item {
            Text(
                text = stringResource(R.string.train_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        if (week != null && showBanner) {
            item { WeekBanner(week = week) }
        }
        item { LiftHistoryCard(history = state.liftHistory) }
        items(state.days, key = { it.id }) { day ->
            var expanded by rememberSaveable { mutableStateOf(day.id == state.defaultExpandedDayId) }
            WorkoutDayCard(
                day = day,
                isToday = day.dayOfWeek == state.today,
                expanded = expanded,
                onToggle = { expanded = !expanded },
                onEditLoads = { editingExerciseId = it.id },
            )
        }
        item {
            Text(
                text = stringResource(R.string.train_rest_days),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    LoadSettingsDialogHost(
        exercises = state.days.flatMap { day -> day.exercises.map { it.exercise } },
        editingExerciseId = editingExerciseId,
        onDismiss = { editingExerciseId = null },
        onSave = onSaveLoads,
    )
}

@Composable
private fun WorkoutDayCard(
    day: WorkoutDay,
    isToday: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEditLoads: (Exercise) -> Unit,
) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = stringResource(if (expanded) R.string.collapse_day else R.string.expand_day),
                        onClick = onToggle,
                    )
                    .padding(spacing.s16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s8),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.day_title, day.name, day.emphasis),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(
                            R.string.day_meta,
                            day.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                            pluralStringResource(R.plurals.working_sets, day.workingSets, day.workingSets),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isToday) {
                    Tag(
                        text = stringResource(R.string.today_badge),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = spacing.s16, end = spacing.s16, bottom = spacing.s4)) {
                    day.exercises.forEach { planned ->
                        HorizontalDivider()
                        ExerciseItem(planned = planned, onEditLoads = onEditLoads)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiftHistoryCard(history: List<LiftHistory>) {
    val spacing = LocalSpacing.current
    val lineColor = MaterialTheme.colorScheme.primary
    val logged = history.filter { it.points.isNotEmpty() }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s12),
        ) {
            Text(text = stringResource(R.string.lifts_title), style = MaterialTheme.typography.titleMedium)
            if (logged.isEmpty()) {
                Text(
                    text = stringResource(R.string.lifts_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (logged.any { it.points.size >= MIN_CHART_POINTS }) {
                Text(
                    text = stringResource(R.string.lift_trend_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            logged.forEach { lift ->
                val loadType = lift.exercise.loadType
                val latest = topSetLabel(loadType, lift.points.last())
                Column(verticalArrangement = Arrangement.spacedBy(spacing.s4)) {
                    Text(text = lift.exercise.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (lift.points.size == 1) {
                            latest
                        } else {
                            stringResource(R.string.lift_progress, topSetLabel(loadType, lift.points.first()), latest)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (lift.points.size >= MIN_CHART_POINTS) {
                        LineChart(
                            series = listOf(
                                ChartSeries(
                                    points = lift.points.map {
                                        ChartPoint(it.date.toEpochDay().toFloat(), it.trendValue(loadType).toFloat())
                                    },
                                    color = lineColor,
                                    style = ChartStyle.LINE,
                                ),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(LIFT_CHART_ASPECT_RATIO),
                        )
                    }
                }
            }
        }
    }
}

private const val LIFT_CHART_ASPECT_RATIO = 3f

@Composable
private fun topSetLabel(loadType: LoadType, point: TopSetPoint): String {
    val locale = LocalLocale.current.platformLocale
    val load = point.loadKg
    return when {
        load == null || loadType == LoadType.BODYWEIGHT ->
            pluralStringResource(R.plurals.set_bodyweight, point.reps, point.reps)
        loadType == LoadType.ASSISTANCE -> stringResource(R.string.set_assisted, formatKg(load, locale), point.reps)
        else -> stringResource(R.string.set_weight, formatKg(load, locale), point.reps)
    }
}
