package hr.rostanic20.gymbro.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.isCalibrationWeek
import hr.rostanic20.gymbro.domain.isDeloadWeek
import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.PlannedExercise
import hr.rostanic20.gymbro.domain.warmupRamp
import hr.rostanic20.gymbro.ui.common.Tag
import hr.rostanic20.gymbro.ui.common.formatKg
import hr.rostanic20.gymbro.ui.common.rangeLabel
import hr.rostanic20.gymbro.ui.common.restLabel
import hr.rostanic20.gymbro.ui.common.setsRepsLabel
import hr.rostanic20.gymbro.ui.theme.LocalSpacing

fun showsWeekBanner(week: Int?): Boolean = week == 1 || isDeloadWeek(week)

@Composable
fun WeekBanner(week: Int) {
    val spacing = LocalSpacing.current
    val deload = isDeloadWeek(week)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s4),
        ) {
            Text(
                text = if (deload) {
                    stringResource(R.string.week_deload_title, week)
                } else {
                    stringResource(R.string.week_calibration_title)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(if (deload) R.string.week_deload_body else R.string.week_calibration_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun WarmupCard(firstExercise: Exercise, week: Int?) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    val workingLoadKg = firstExercise.startLoadKg?.takeIf { isCalibrationWeek(week) }
    val ramp = workingLoadKg?.let { firstExercise.warmupRamp(it) }.orEmpty()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s4),
        ) {
            Text(
                text = stringResource(R.string.warmup_title),
                style = MaterialTheme.typography.titleMedium,
            )
            when {
                ramp.isNotEmpty() -> {
                    Text(
                        text = stringResource(R.string.warmup_bike, firstExercise.name),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    ramp.forEach { set ->
                        Text(
                            text = stringResource(R.string.warmup_set, formatKg(set.loadKg, locale), set.reps),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                firstExercise.barWeightKg != null -> Text(
                    text = stringResource(R.string.warmup_generic, firstExercise.name),
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> Text(
                    text = stringResource(R.string.warmup_no_bar, firstExercise.name),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun ExerciseItem(
    planned: PlannedExercise,
    onEditLoads: (Exercise) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val exercise = planned.exercise
    var expanded by rememberSaveable(exercise.id) { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(if (expanded) R.string.collapse_details else R.string.expand_details),
            ) { expanded = !expanded }
            .padding(vertical = spacing.s12),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s8),
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (planned.isTop) {
                    Tag(
                        text = stringResource(R.string.top_set),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(
                R.string.exercise_prescription,
                setsRepsLabel(planned),
                rangeLabel(planned.rir),
                restLabel(planned.restSeconds),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = loadLabel(exercise),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AnimatedVisibility(visible = expanded) {
            ExerciseDetails(planned = planned, onEditLoads = onEditLoads)
        }
    }
}

@Composable
private fun ExerciseDetails(planned: PlannedExercise, onEditLoads: (Exercise) -> Unit) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier.padding(top = spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s8),
    ) {
        planned.note?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
        if (planned.alternatives.isNotEmpty()) {
            Text(
                text = stringResource(R.string.alternatives_title),
                style = MaterialTheme.typography.labelLarge,
            )
            planned.alternatives.forEach { alternative ->
                Text(
                    text = alternative.note?.let { stringResource(R.string.alternative_with_note, alternative.name, it) }
                        ?: alternative.name,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (planned.exercise.loadType != LoadType.BODYWEIGHT) {
            TextButton(onClick = { onEditLoads(planned.exercise) }) {
                Text(stringResource(R.string.edit_weights))
            }
        }
    }
}

@Composable
private fun loadLabel(exercise: Exercise): String {
    val locale = LocalLocale.current.platformLocale
    val increment = exercise.incrementKg?.let { formatKg(it, locale) }
    val start = exercise.startLoadKg?.let { formatKg(it, locale) }
    return when {
        exercise.loadType == LoadType.BODYWEIGHT || increment == null -> stringResource(R.string.load_bodyweight)
        exercise.loadType == LoadType.ASSISTANCE && start != null ->
            stringResource(R.string.load_assistance, start, increment)
        exercise.loadType == LoadType.ASSISTANCE -> stringResource(R.string.load_assistance_unknown, increment)
        start != null -> stringResource(R.string.load_weight, start, increment)
        else -> stringResource(R.string.load_weight_unknown, increment)
    }
}

@Composable
fun LoadSettingsDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onSave: (LoadSettings) -> Unit,
) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    var start by rememberSaveable(exercise.id) {
        mutableStateOf(exercise.startLoadKg?.let { formatKg(it, locale) }.orEmpty())
    }
    var increment by rememberSaveable(exercise.id) {
        mutableStateOf(exercise.incrementKg?.let { formatKg(it, locale) }.orEmpty())
    }
    val assistance = exercise.loadType == LoadType.ASSISTANCE
    val settings = validateLoadSettings(exercise.loadType, start, increment)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s12)) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = {
                        Text(
                            stringResource(
                                if (assistance) R.string.load_dialog_start_assistance else R.string.load_dialog_start_weight,
                            ),
                        )
                    },
                    supportingText = { Text(stringResource(R.string.load_dialog_start_hint)) },
                    isError = !isStartLoadValid(exercise.loadType, start),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = increment,
                    onValueChange = { increment = it },
                    label = { Text(stringResource(R.string.load_dialog_increment)) },
                    supportingText = {
                        Text(
                            stringResource(
                                if (assistance) {
                                    R.string.load_dialog_assistance_increment_hint
                                } else {
                                    R.string.load_dialog_increment_hint
                                },
                            ),
                        )
                    },
                    isError = !isIncrementValid(increment),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { settings?.let(onSave) }, enabled = settings != null) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun LoadSettingsDialogHost(
    exercises: List<Exercise>,
    editingExerciseId: Long?,
    onDismiss: () -> Unit,
    onSave: (exerciseId: Long, settings: LoadSettings) -> Unit,
) {
    val exercise = exercises.firstOrNull { it.id == editingExerciseId } ?: return
    LoadSettingsDialog(
        exercise = exercise,
        onDismiss = onDismiss,
        onSave = {
            onSave(exercise.id, it)
            onDismiss()
        },
    )
}
