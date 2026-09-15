package hr.rostanic20.gymbro.ui.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.model.Exercise
import hr.rostanic20.gymbro.domain.model.PlannedExercise
import hr.rostanic20.gymbro.ui.LocalSnackbarHostState
import hr.rostanic20.gymbro.ui.ObserveAsEvents
import hr.rostanic20.gymbro.ui.common.LoadingScreen
import hr.rostanic20.gymbro.ui.common.rangeLabel
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditDayScreen(
    dayId: Long,
    onBack: () -> Unit,
    viewModel: EditDayViewModel = koinViewModel(parameters = { parametersOf(dayId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.messages) { snackbarHostState.showSnackbar(resources.getString(it.text)) }
    if (state == null) {
        LoadingScreen()
    }
    state?.let {
        EditDayContent(
            state = it,
            onBack = onBack,
            onSave = viewModel::savePrescription,
            onHiddenChange = viewModel::setHidden,
            onAdd = viewModel::addExercise,
        )
    }
}

@Composable
internal fun EditDayContent(
    state: EditDayUiState,
    onBack: () -> Unit,
    onSave: (position: Int, prescription: Prescription) -> Unit,
    onHiddenChange: (position: Int, hidden: Boolean) -> Unit,
    onAdd: (exerciseId: Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val day = state.day
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.s16),
        verticalArrangement = Arrangement.spacedBy(spacing.s12),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
                Text(
                    text = day?.name ?: stringResource(R.string.history_unknown_day),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.edit_day_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(day?.exercises.orEmpty(), key = { it.position }) { planned ->
            ExerciseEditCard(
                planned = planned,
                onEdit = { editing = planned.position },
                onHiddenChange = { onHiddenChange(planned.position, it) },
            )
        }
        item {
            OutlinedButton(onClick = { adding = true }, enabled = state.addable.isNotEmpty()) {
                Text(stringResource(R.string.edit_add_exercise))
            }
        }
    }
    day?.exercises?.firstOrNull { it.position == editing }?.let { planned ->
        PrescriptionDialog(
            planned = planned,
            onDismiss = { editing = null },
            onSave = {
                editing = null
                onSave(planned.position, it)
            },
        )
    }
    if (adding) {
        AddExerciseDialog(
            exercises = state.addable,
            onDismiss = { adding = false },
            onPick = {
                adding = false
                onAdd(it)
            },
        )
    }
}

@Composable
private fun ExerciseEditCard(planned: PlannedExercise, onEdit: () -> Unit, onHiddenChange: (Boolean) -> Unit) {
    val spacing = LocalSpacing.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s4),
        ) {
            Text(
                text = planned.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (planned.isHidden) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = if (planned.isHidden) {
                    stringResource(R.string.edit_removed)
                } else {
                    stringResource(R.string.sets_reps, planned.sets, rangeLabel(planned.reps))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s8)) {
                TextButton(onClick = onEdit, enabled = !planned.isHidden) {
                    Text(stringResource(R.string.edit_sets_reps))
                }
                TextButton(onClick = { onHiddenChange(!planned.isHidden) }) {
                    Text(
                        stringResource(if (planned.isHidden) R.string.edit_restore else R.string.edit_remove),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrescriptionDialog(
    planned: PlannedExercise,
    onDismiss: () -> Unit,
    onSave: (Prescription) -> Unit,
) {
    val spacing = LocalSpacing.current
    val saved = PrescriptionForm.of(planned.sets, planned.reps)
    var sets by rememberSaveable(saved.sets) { mutableStateOf(saved.sets) }
    var repMin by rememberSaveable(saved.repMin) { mutableStateOf(saved.repMin) }
    var repMax by rememberSaveable(saved.repMax) { mutableStateOf(saved.repMax) }
    val prescription = PrescriptionForm(sets, repMin, repMax).validate()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(planned.exercise.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s12)) {
                NumberField(value = sets, onValueChange = { sets = it }, label = R.string.edit_sets)
                NumberField(value = repMin, onValueChange = { repMin = it }, label = R.string.edit_rep_min)
                NumberField(value = repMax, onValueChange = { repMax = it }, label = R.string.edit_rep_max)
                if (prescription == null) {
                    Text(
                        text = stringResource(R.string.edit_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { prescription?.let(onSave) }, enabled = prescription != null) {
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
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AddExerciseDialog(exercises: List<Exercise>, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_add_exercise)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = dimensionResource(R.dimen.food_picker_max_height))) {
                items(exercises, key = { it.id }) { exercise ->
                    TextButton(onClick = { onPick(exercise.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(text = exercise.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
