package hr.rostanic20.gymbro.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.model.NutritionPhase
import hr.rostanic20.gymbro.domain.model.NutritionTargets
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.ui.LocalSnackbarHostState
import hr.rostanic20.gymbro.ui.ObserveAsEvents
import hr.rostanic20.gymbro.ui.common.Tag
import hr.rostanic20.gymbro.ui.common.formatCount
import hr.rostanic20.gymbro.ui.common.rememberDayFormatter
import hr.rostanic20.gymbro.ui.common.setsRepsLabel
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import hr.rostanic20.gymbro.ui.workout.WeekBanner
import hr.rostanic20.gymbro.ui.workout.showsWeekBanner
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodayScreen(
    onOpenWorkout: (dayId: Long) -> Unit,
    viewModel: TodayViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.messages) { snackbarHostState.showSnackbar(resources.getString(it.text)) }
    state?.let {
        TodayContent(state = it, onStartProgram = viewModel::startProgram, onOpenWorkout = onOpenWorkout)
    }
}

@Composable
internal fun TodayContent(
    state: TodayUiState,
    onStartProgram: () -> Unit,
    onOpenWorkout: (dayId: Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    val week = state.week
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.s16),
        verticalArrangement = Arrangement.spacedBy(spacing.s16),
    ) {
        item { TodayHeader(state = state) }
        if (state.programStart == null) {
            item { NotStartedCard(state = state, onStartProgram = onStartProgram) }
        }
        if (week != null && showsWeekBanner(week)) {
            item { WeekBanner(week = week) }
        }
        item { TargetsCard(targets = state.targets) }
        item {
            WorkoutSummaryCard(workout = state.workout, status = state.workoutStatus, onOpenWorkout = onOpenWorkout)
        }
    }
}

@Composable
private fun TodayHeader(state: TodayUiState) {
    val formatter = rememberDayFormatter()
    val label = when {
        state.week != null -> stringResource(R.string.today_week, state.week)
        state.programStart != null -> stringResource(R.string.today_starts_on, state.programStart.format(formatter))
        else -> null
    }
    Column {
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = state.date.format(formatter),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
private fun NotStartedCard(state: TodayUiState, onStartProgram: () -> Unit) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    val formatter = rememberDayFormatter()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s8),
        ) {
            Text(
                text = stringResource(R.string.today_not_started_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.today_not_started_body, formatCount(state.targets.kcal, locale)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onStartProgram) {
                Text(
                    if (state.suggestedStart <= state.date) {
                        stringResource(R.string.today_start_this_week)
                    } else {
                        stringResource(R.string.today_start_on, state.suggestedStart.format(formatter))
                    },
                )
            }
        }
    }
}

@Composable
private fun TargetsCard(targets: NutritionTargets) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s12),
        ) {
            Text(
                text = stringResource(
                    when (targets.phase) {
                        NutritionPhase.MAINTENANCE -> R.string.phase_maintenance
                        NutritionPhase.SURPLUS -> R.string.phase_surplus
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TargetValue(value = formatCount(targets.kcal, locale), label = stringResource(R.string.target_kcal))
                TargetValue(
                    value = stringResource(R.string.grams_value, targets.proteinG),
                    label = stringResource(R.string.target_protein),
                )
                TargetValue(
                    value = stringResource(R.string.grams_value, targets.fatG),
                    label = stringResource(R.string.target_fat),
                )
                TargetValue(
                    value = stringResource(R.string.grams_value, targets.carbsG),
                    label = stringResource(R.string.target_carbs),
                )
            }
        }
    }
}

@Composable
private fun TargetValue(value: String, label: String) {
    Column {
        Text(text = value, style = MaterialTheme.typography.titleLarge)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WorkoutSummaryCard(
    workout: WorkoutDay?,
    status: WorkoutStatus,
    onOpenWorkout: (dayId: Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    if (workout == null) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(spacing.s16),
                verticalArrangement = Arrangement.spacedBy(spacing.s8),
            ) {
                Text(
                    text = stringResource(R.string.today_travel_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.today_travel_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        return
    }
    Card(onClick = { onOpenWorkout(workout.id) }, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s8),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s8),
            ) {
                Text(
                    text = stringResource(R.string.day_title, workout.name, workout.emphasis),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                when (status) {
                    WorkoutStatus.IN_PROGRESS -> Tag(
                        text = stringResource(R.string.status_in_progress),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    WorkoutStatus.LOGGED -> Tag(
                        text = stringResource(R.string.status_logged),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    WorkoutStatus.NOT_STARTED -> Unit
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.today_open_workout),
                )
            }
            Text(
                text = pluralStringResource(R.plurals.working_sets, workout.workingSets, workout.workingSets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            workout.exercises.forEach { planned ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s8),
                ) {
                    Text(
                        text = planned.exercise.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = setsRepsLabel(planned),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
