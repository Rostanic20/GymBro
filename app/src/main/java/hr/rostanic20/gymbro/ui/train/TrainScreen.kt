package hr.rostanic20.gymbro.ui.train

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.model.PlannedExercise
import hr.rostanic20.gymbro.domain.model.WorkoutDay
import hr.rostanic20.gymbro.ui.common.Tag
import hr.rostanic20.gymbro.ui.common.rangeLabel
import hr.rostanic20.gymbro.ui.common.restLabel
import hr.rostanic20.gymbro.ui.common.setsRepsLabel
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import org.koin.compose.viewmodel.koinViewModel
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TrainScreen(viewModel: TrainViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state?.let { TrainContent(state = it) }
}

@Composable
private fun TrainContent(state: TrainUiState) {
    val spacing = LocalSpacing.current
    LazyColumn(
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
        items(state.days, key = { it.id }) { day ->
            WorkoutDayCard(day = day, isToday = day.dayOfWeek == state.today)
        }
        item {
            Text(
                text = stringResource(R.string.train_rest_days),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkoutDayCard(day: WorkoutDay, isToday: Boolean) {
    val spacing = LocalSpacing.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(spacing.s16)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s8),
            ) {
                Text(
                    text = stringResource(R.string.day_title, day.name, day.emphasis),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (isToday) {
                    Tag(
                        text = stringResource(R.string.today_badge),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                text = stringResource(
                    R.string.day_meta,
                    day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    pluralStringResource(R.plurals.working_sets, day.workingSets, day.workingSets),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            day.exercises.forEachIndexed { index, exercise ->
                if (index > 0) HorizontalDivider()
                ExerciseRow(exercise = exercise)
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: PlannedExercise) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.s12),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s8),
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (exercise.isTop) {
                Tag(
                    text = stringResource(R.string.top_set),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
        Text(
            text = stringResource(
                R.string.exercise_prescription,
                setsRepsLabel(exercise),
                rangeLabel(exercise.rir),
                restLabel(exercise.restSeconds),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        exercise.note?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }
        exercise.substitute?.let {
            Text(
                text = stringResource(R.string.swap, it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
