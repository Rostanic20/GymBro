package hr.rostanic20.gymbro.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.ui.LocalSnackbarHostState
import hr.rostanic20.gymbro.ui.ObserveAsEvents
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WorkoutScreen(
    dayId: Long,
    onBack: () -> Unit,
    onOpenSession: (sessionId: Long) -> Unit,
    viewModel: WorkoutViewModel = koinViewModel(parameters = { parametersOf(dayId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.messages) { snackbarHostState.showSnackbar(resources.getString(it.text)) }
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is WorkoutEvent.OpenSession -> onOpenSession(event.sessionId)
        }
    }
    state?.let {
        WorkoutContent(
            state = it,
            dayId = dayId,
            onBack = onBack,
            onStartSession = viewModel::startSession,
            onOpenSession = onOpenSession,
            onSaveLoads = viewModel::updateLoadSettings,
        )
    }
}

@Composable
private fun WorkoutContent(
    state: WorkoutUiState,
    dayId: Long,
    onBack: () -> Unit,
    onStartSession: () -> Unit,
    onOpenSession: (sessionId: Long) -> Unit,
    onSaveLoads: (exerciseId: Long, settings: LoadSettings) -> Unit,
) {
    val spacing = LocalSpacing.current
    val day = state.day
    var editingExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            if (day != null) {
                Text(
                    text = stringResource(R.string.day_title, day.name, day.emphasis),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        if (day == null) return@Column
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = spacing.s16, end = spacing.s16, bottom = spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s16),
        ) {
            item {
                SessionButtons(
                    state = state,
                    dayId = dayId,
                    onStart = onStartSession,
                    onOpenSession = onOpenSession,
                )
            }
            val week = state.week
            if (week != null && showsWeekBanner(week)) {
                item { WeekBanner(week = week) }
            }
            day.exercises.firstOrNull()?.let { first ->
                item { WarmupCard(firstExercise = first.exercise, week = week) }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = spacing.s16)) {
                        day.exercises.forEachIndexed { index, planned ->
                            if (index > 0) HorizontalDivider()
                            ExerciseItem(planned = planned, onEditLoads = { editingExerciseId = it.id })
                        }
                    }
                }
            }
        }
    }
    LoadSettingsDialogHost(
        exercises = day?.exercises.orEmpty().map { it.exercise },
        editingExerciseId = editingExerciseId,
        onDismiss = { editingExerciseId = null },
        onSave = onSaveLoads,
    )
}

@Composable
private fun SessionButtons(
    state: WorkoutUiState,
    dayId: Long,
    onStart: () -> Unit,
    onOpenSession: (sessionId: Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    val active = state.activeSession
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.s8),
    ) {
        when {
            active != null && active.dayId == dayId ->
                Button(onClick = { onOpenSession(active.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.continue_workout))
                }
            active != null -> {
                Text(
                    text = stringResource(R.string.finish_other_first, state.activeSessionDayName.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = { onOpenSession(active.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.open_other_session, state.activeSessionDayName.orEmpty()))
                }
            }
            else -> {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.start_workout))
                }
                if (state.loggedToday) {
                    Text(
                        text = stringResource(R.string.logged_today),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
