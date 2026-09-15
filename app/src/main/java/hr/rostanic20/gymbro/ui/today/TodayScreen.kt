package hr.rostanic20.gymbro.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.Nutrition
import hr.rostanic20.gymbro.domain.model.NutritionPhase
import hr.rostanic20.gymbro.domain.model.NutritionTargets
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.ui.LocalSnackbarHostState
import hr.rostanic20.gymbro.ui.ObserveAsEvents
import hr.rostanic20.gymbro.ui.common.Tag
import hr.rostanic20.gymbro.ui.common.formatCount
import hr.rostanic20.gymbro.ui.common.formatKg
import hr.rostanic20.gymbro.ui.common.labelRes
import hr.rostanic20.gymbro.ui.common.parseKg
import hr.rostanic20.gymbro.ui.common.rememberDayFormatter
import hr.rostanic20.gymbro.ui.common.setsRepsLabel
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import hr.rostanic20.gymbro.ui.workout.WeekBanner
import hr.rostanic20.gymbro.ui.workout.showsWeekBanner
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private const val MIN_BODY_WEIGHT_KG = 30.0
private const val MAX_BODY_WEIGHT_KG = 300.0

@Composable
fun TodayScreen(
    onOpenWorkout: (dayId: Long) -> Unit,
    onOpenSession: (sessionId: Long) -> Unit,
    onOpenMeal: (epochDay: Long, slot: Int) -> Unit,
    viewModel: TodayViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.messages) { snackbarHostState.showSnackbar(resources.getString(it.text)) }
    state?.let {
        TodayContent(
            state = it,
            onStartProgram = viewModel::startProgram,
            onOpenWorkout = onOpenWorkout,
            onOpenSession = onOpenSession,
            onOpenMeal = onOpenMeal,
            onSaveWeight = viewModel::saveWeight,
            onPreviousDay = viewModel::showPreviousDay,
            onNextDay = viewModel::showNextDay,
            onToday = viewModel::showToday,
            healthCard = { HealthCard() },
        )
    }
}

@Composable
internal fun TodayContent(
    state: TodayUiState,
    onStartProgram: () -> Unit,
    onOpenWorkout: (dayId: Long) -> Unit,
    onOpenMeal: (epochDay: Long, slot: Int) -> Unit,
    onSaveWeight: (Double) -> Unit,
    onOpenSession: (sessionId: Long) -> Unit = {},
    onPreviousDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onToday: () -> Unit = {},
    healthCard: @Composable () -> Unit = {},
) {
    val spacing = LocalSpacing.current
    val week = state.week
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.s16),
        verticalArrangement = Arrangement.spacedBy(spacing.s16),
    ) {
        item {
            TodayHeader(
                state = state,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onToday = onToday,
            )
        }
        if (state.programStart == null) {
            item { NotStartedCard(state = state, onStartProgram = onStartProgram) }
        }
        if (week != null && showsWeekBanner(week)) {
            item { WeekBanner(week = week) }
        }
        item { NutritionCard(targets = state.targets, eaten = state.eaten) }
        item {
            MealsCard(
                meals = state.mealSettings.activeMeals,
                timeFor = state.mealSettings::timeFor,
                eatenByMeal = state.eatenByMeal,
                onOpenMeal = { onOpenMeal(state.date.toEpochDay(), it.slot) },
            )
        }
        item {
            WorkoutSummaryCard(
                workout = state.workout,
                status = state.workoutStatus,
                onOpen = when {
                    state.isToday -> state.workout?.let { { onOpenWorkout(it.id) } }
                    state.loggedSessionId != null -> ({ onOpenSession(state.loggedSessionId) })
                    else -> null
                },
            )
        }
        item { WeightCard(state = state, onSave = onSaveWeight) }
        item { healthCard() }
    }
}

@Composable
private fun TodayHeader(
    state: TodayUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
) {
    val formatter = rememberDayFormatter()
    val label = when {
        state.week != null -> stringResource(R.string.today_week, state.week)
        state.programStart != null -> stringResource(R.string.today_starts_on, state.programStart.format(formatter))
        else -> null
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPreviousDay) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.day_previous),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = state.date.format(formatter),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        if (state.isToday) {
            IconButton(onClick = onNextDay, enabled = false) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.day_next),
                )
            }
        } else {
            TextButton(onClick = onToday) {
                Text(stringResource(R.string.today_badge))
            }
            IconButton(onClick = onNextDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.day_next),
                )
            }
        }
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
private fun NutritionCard(targets: NutritionTargets, eaten: Nutrition) {
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
            ProgressRow(
                label = stringResource(R.string.target_kcal),
                value = stringResource(
                    R.string.food_eaten_kcal,
                    formatCount(eaten.kcal.roundToInt(), locale),
                    formatCount(targets.kcal, locale),
                ),
                progress = fraction(eaten.kcal, targets.kcal),
            )
            ProgressRow(
                label = stringResource(R.string.target_protein),
                value = stringResource(
                    R.string.food_eaten_grams,
                    formatCount(eaten.proteinG.roundToInt(), locale),
                    formatCount(targets.proteinG, locale),
                ),
                progress = fraction(eaten.proteinG, targets.proteinG),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MacroText(
                    label = stringResource(R.string.target_carbs),
                    value = stringResource(
                        R.string.food_eaten_grams,
                        formatCount(eaten.carbsG.roundToInt(), locale),
                        formatCount(targets.carbsG, locale),
                    ),
                )
                MacroText(
                    label = stringResource(R.string.target_fat),
                    value = stringResource(
                        R.string.food_eaten_grams,
                        formatCount(eaten.fatG.roundToInt(), locale),
                        formatCount(targets.fatG, locale),
                    ),
                )
            }
        }
    }
}

private fun fraction(eaten: Double, target: Int): Float =
    if (target <= 0) 0f else (eaten / target).toFloat().coerceIn(0f, 1f)

@Composable
private fun ProgressRow(label: String, value: String, progress: Float) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.s4)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(text = value, style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), drawStopIndicator = {})
    }
}

@Composable
private fun MacroText(label: String, value: String) {
    Column {
        Text(text = value, style = MaterialTheme.typography.titleSmall)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MealsCard(
    meals: List<Meal>,
    timeFor: (Meal) -> LocalTime,
    eatenByMeal: Map<Meal, Nutrition>,
    onOpenMeal: (Meal) -> Unit,
) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    val timeFormatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = spacing.s8)) {
            Text(
                text = stringResource(R.string.meals_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = spacing.s16, vertical = spacing.s8),
            )
            meals.forEach { meal ->
                val eaten = eatenByMeal[meal]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMeal(meal) }
                        .padding(horizontal = spacing.s16, vertical = spacing.s12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s12),
                ) {
                    Text(
                        text = timeFor(meal).format(timeFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(meal.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = eaten?.let { stringResource(R.string.meal_kcal, formatCount(it.kcal.roundToInt(), locale)) }
                            ?: stringResource(R.string.meal_not_logged),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (eaten == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutSummaryCard(
    workout: WorkoutDay?,
    status: WorkoutStatus,
    onOpen: (() -> Unit)?,
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
    Card(
        onClick = onOpen ?: {},
        enabled = onOpen != null,
        modifier = Modifier.fillMaxWidth(),
    ) {
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
                if (onOpen != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.today_open_workout),
                    )
                }
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

@Composable
private fun WeightCard(state: TodayUiState, onSave: (Double) -> Unit) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    var text by rememberSaveable(state.weightTodayKg) {
        mutableStateOf(state.weightTodayKg?.let { formatKg(it, locale) }.orEmpty())
    }
    val parsed = parseKg(text)?.takeIf { it in MIN_BODY_WEIGHT_KG..MAX_BODY_WEIGHT_KG }
    val focusManager = LocalFocusManager.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s8),
        ) {
            Text(text = stringResource(R.string.weight_title), style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s8),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = {
                        Text(stringResource(if (state.isToday) R.string.weight_field else R.string.weight_field_past))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        parsed?.let(onSave)
                        focusManager.clearFocus()
                    },
                    enabled = parsed != null && parsed != state.weightTodayKg,
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
            state.weekAverageKg?.let {
                Text(
                    text = stringResource(R.string.weight_week_average, String.format(locale, "%.1f", it)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.weeklyChangeKg?.let {
                Text(
                    text = stringResource(R.string.weight_weekly_change, String.format(locale, "%+.1f", it)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.weight_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
