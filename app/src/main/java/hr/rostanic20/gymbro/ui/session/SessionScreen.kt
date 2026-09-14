package hr.rostanic20.gymbro.ui.session

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.Suggestion
import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.LoggedSet
import hr.rostanic20.gymbro.domain.model.SetValues
import hr.rostanic20.gymbro.domain.workingLoad
import hr.rostanic20.gymbro.ui.LocalSnackbarHostState
import hr.rostanic20.gymbro.ui.ObserveAsEvents
import hr.rostanic20.gymbro.ui.common.Tag
import hr.rostanic20.gymbro.ui.common.formatKg
import hr.rostanic20.gymbro.ui.common.rangeLabel
import hr.rostanic20.gymbro.ui.common.restLabel
import hr.rostanic20.gymbro.ui.common.setsRepsLabel
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Locale

private const val TICK_MILLIS = 250L
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L

data class SessionActions(
    val logSet: (SlotState, SetValues) -> Unit,
    val updateSet: (Long, SetValues) -> Unit,
    val deleteSet: (Long) -> Unit,
    val swap: (Int, Long) -> Unit,
    val extendRest: () -> Unit,
    val skipRest: () -> Unit,
    val noteChange: (String) -> Unit,
    val finish: (String) -> Unit,
    val discard: () -> Unit,
)

@Composable
fun SessionScreen(
    sessionId: Long,
    onClose: () -> Unit,
    viewModel: SessionViewModel = koinViewModel(parameters = { parametersOf(sessionId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.messages) { snackbarHostState.showSnackbar(resources.getString(it.text)) }
    ObserveAsEvents(viewModel.events) { onClose() }
    RequestNotificationPermission()
    val actions = remember(viewModel) {
        SessionActions(
            logSet = viewModel::logSet,
            updateSet = viewModel::updateSet,
            deleteSet = viewModel::deleteSet,
            swap = viewModel::swap,
            extendRest = viewModel::extendRest,
            skipRest = viewModel::skipRest,
            noteChange = viewModel::onNoteChange,
            finish = viewModel::finish,
            discard = viewModel::discard,
        )
    }
    state?.let { SessionContent(state = it, onBack = onClose, actions = actions) }
}

@Composable
private fun RequestNotificationPermission() {
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!asked && !granted) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun SessionContent(state: SessionUiState, onBack: () -> Unit, actions: SessionActions) {
    val spacing = LocalSpacing.current
    var note by rememberSaveable { mutableStateOf(state.session.note.orEmpty()) }
    var editingSetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var swappingSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.day_title, state.dayName, state.dayEmphasis),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (state.session.isDeload) {
                    Text(
                        text = stringResource(R.string.session_deload_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = { actions.finish(note) }) {
                Text(stringResource(R.string.session_finish))
            }
        }
        state.session.restEndsAtMillis?.let {
            RestTimerBar(endsAtMillis = it, onExtend = actions.extendRest, onSkip = actions.skipRest)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s16),
        ) {
            if (state.deloadAdvised) {
                item { StallBanner() }
            }
            items(state.slots, key = { it.planned.position }) { slot ->
                SlotCard(
                    slot = slot,
                    onLog = { actions.logSet(slot, it) },
                    onEditSet = { editingSetId = it.id },
                    onSwap = { swappingSlot = slot.planned.position },
                )
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                        actions.noteChange(it)
                    },
                    label = { Text(stringResource(R.string.session_note_label)) },
                    placeholder = { Text(stringResource(R.string.session_note_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s8),
                ) {
                    OutlinedButton(onClick = { confirmDiscard = true }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.session_discard))
                    }
                    Button(onClick = { actions.finish(note) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.session_finish))
                    }
                }
            }
        }
    }

    state.slots.firstNotNullOfOrNull { slot ->
        slot.sets.firstOrNull { it.id == editingSetId }?.let { slot to it }
    }?.let { (slot, set) ->
        EditSetDialog(
            set = set,
            loadType = slot.exercise.loadType,
            onDismiss = { editingSetId = null },
            onSave = {
                actions.updateSet(set.id, it)
                editingSetId = null
            },
            onDelete = {
                actions.deleteSet(set.id)
                editingSetId = null
            },
        )
    }

    state.slots.firstOrNull { it.planned.position == swappingSlot }?.let { slot ->
        SwapDialog(
            slot = slot,
            onDismiss = { swappingSlot = null },
            onSelect = {
                actions.swap(slot.planned.position, it)
                swappingSlot = null
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.session_discard_title)) },
            text = { Text(stringResource(R.string.session_discard_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard = false
                        actions.discard()
                    },
                ) {
                    Text(stringResource(R.string.session_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun RestTimerBar(endsAtMillis: Long, onExtend: () -> Unit, onSkip: () -> Unit) {
    val spacing = LocalSpacing.current
    val remaining by produceState(initialValue = secondsUntil(endsAtMillis), endsAtMillis) {
        while (value > 0) {
            delay(TICK_MILLIS)
            value = secondsUntil(endsAtMillis)
        }
    }
    val running = remaining > 0
    Surface(
        color = if (running) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (running) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.s16, vertical = spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (running) {
                    stringResource(R.string.rest_remaining, formatDuration(remaining))
                } else {
                    stringResource(R.string.rest_over)
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onExtend) {
                Text(stringResource(R.string.rest_add_30))
            }
            TextButton(onClick = onSkip) {
                Text(stringResource(if (running) R.string.rest_skip else R.string.rest_dismiss))
            }
        }
    }
}

private fun secondsUntil(endsAtMillis: Long): Long =
    ((endsAtMillis - System.currentTimeMillis() + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).coerceAtLeast(0)

private fun formatDuration(seconds: Long): String =
    String.format(Locale.ROOT, "%d:%02d", seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)

@Composable
private fun StallBanner() {
    val spacing = LocalSpacing.current
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
            Text(text = stringResource(R.string.stall_title), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(R.string.stall_body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SlotCard(
    slot: SlotState,
    onLog: (SetValues) -> Unit,
    onEditSet: (LoggedSet) -> Unit,
    onSwap: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s8),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s8),
                ) {
                    Text(
                        text = slot.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (slot.planned.isTop) {
                        Tag(
                            text = stringResource(R.string.top_set),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
                if (slot.canSwap) {
                    TextButton(onClick = onSwap) {
                        Text(stringResource(R.string.session_swap))
                    }
                }
            }
            Text(
                text = stringResource(
                    R.string.exercise_prescription,
                    setsRepsLabel(slot.planned),
                    rangeLabel(slot.planned.rir),
                    restLabel(slot.planned.restSeconds),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = suggestionLabel(slot),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (slot.lastSets.isNotEmpty()) {
                Text(
                    text = lastTimeLabel(slot.exercise.loadType, slot.lastSets),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            slot.sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = stringResource(R.string.edit_set_title)) { onEditSet(set) }
                        .padding(vertical = spacing.s4),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s12),
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = setLabel(slot.exercise.loadType, set), style = MaterialTheme.typography.bodyMedium)
                }
            }
            SetEntryRow(slot = slot, onLog = onLog)
            Text(
                text = pluralStringResource(R.plurals.sets_progress, slot.planned.sets, slot.sets.size, slot.planned.sets),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SetEntryRow(slot: SlotState, onLog: (SetValues) -> Unit) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    val loadType = slot.exercise.loadType
    val prefill = slot.prefill()
    var load by rememberSaveable(slot.exercise.id, slot.sets.size, prefill) {
        mutableStateOf(prefill.loadKg?.let { formatKg(it, locale) }.orEmpty())
    }
    var reps by rememberSaveable(slot.exercise.id, slot.sets.size, prefill) {
        mutableStateOf(prefill.reps?.toString().orEmpty())
    }
    var rir by rememberSaveable(slot.exercise.id, slot.sets.size) { mutableStateOf("") }
    val values = validateSetInput(loadType, load, reps, rir)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loadType != LoadType.BODYWEIGHT) {
            OutlinedTextField(
                value = load,
                onValueChange = { load = it },
                label = {
                    Text(stringResource(if (loadType == LoadType.ASSISTANCE) R.string.field_assistance else R.string.field_weight))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1.3f),
            )
        }
        OutlinedTextField(
            value = reps,
            onValueChange = { reps = it.filter(Char::isDigit) },
            label = { Text(stringResource(R.string.field_reps)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = rir,
            onValueChange = { rir = it.filter(Char::isDigit) },
            label = { Text(stringResource(R.string.field_rir)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        FilledIconButton(onClick = { values?.let(onLog) }, enabled = values != null) {
            Icon(imageVector = Icons.Outlined.Check, contentDescription = stringResource(R.string.log_set))
        }
    }
}

@Composable
private fun EditSetDialog(
    set: LoggedSet,
    loadType: LoadType,
    onDismiss: () -> Unit,
    onSave: (SetValues) -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    var load by rememberSaveable(set.id) { mutableStateOf(set.loadKg?.let { formatKg(it, locale) }.orEmpty()) }
    var reps by rememberSaveable(set.id) { mutableStateOf(set.reps.toString()) }
    var rir by rememberSaveable(set.id) { mutableStateOf(set.rir?.toString().orEmpty()) }
    val values = validateSetInput(loadType, load, reps, rir)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_set_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s12)) {
                if (loadType != LoadType.BODYWEIGHT) {
                    OutlinedTextField(
                        value = load,
                        onValueChange = { load = it },
                        label = {
                            Text(
                                stringResource(
                                    if (loadType == LoadType.ASSISTANCE) R.string.field_assistance else R.string.field_weight,
                                ),
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.field_reps)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = rir,
                    onValueChange = { rir = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.field_rir)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { values?.let(onSave) }, enabled = values != null) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.delete_set))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    )
}

@Composable
private fun SwapDialog(slot: SlotState, onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    val spacing = LocalSpacing.current
    val notes = slot.planned.alternatives.associate { it.exercise.id to it.note }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.session_swap_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s4)) {
                slot.options.forEach { option ->
                    val selected = option.id == slot.exercise.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected, onClick = { onSelect(option.id) }, role = Role.RadioButton)
                            .padding(vertical = spacing.s8),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.s12),
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Column {
                            Text(text = option.name, style = MaterialTheme.typography.bodyLarge)
                            notes[option.id]?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun setLabel(loadType: LoadType, set: LoggedSet): String {
    val locale = LocalLocale.current.platformLocale
    val base = when (loadType) {
        LoadType.WEIGHT -> stringResource(R.string.set_weight, formatKg(set.loadKg ?: 0.0, locale), set.reps)
        LoadType.ASSISTANCE -> stringResource(R.string.set_assisted, formatKg(set.loadKg ?: 0.0, locale), set.reps)
        LoadType.BODYWEIGHT -> pluralStringResource(R.plurals.set_bodyweight, set.reps, set.reps)
    }
    return set.rir?.let { stringResource(R.string.set_with_rir, base, it) } ?: base
}

@Composable
private fun lastTimeLabel(loadType: LoadType, sets: List<LoggedSet>): String {
    val locale = LocalLocale.current.platformLocale
    val reps = sets.joinToString(", ") { it.reps.toString() }
    val load = workingLoad(loadType, sets)
    return when {
        loadType == LoadType.WEIGHT && load != null ->
            stringResource(R.string.last_time_weight, formatKg(load, locale), reps)
        loadType == LoadType.ASSISTANCE && load != null ->
            stringResource(R.string.last_time_assisted, formatKg(load, locale), reps)
        else -> stringResource(R.string.last_time_reps, reps)
    }
}

@Composable
private fun suggestionLabel(slot: SlotState): String {
    val locale = LocalLocale.current.platformLocale
    val loadType = slot.exercise.loadType
    val bottom = slot.planned.reps.first
    val rir = rangeLabel(slot.planned.rir)
    return when (val next = slot.suggestion) {
        is Suggestion.FirstTime -> when {
            loadType == LoadType.BODYWEIGHT -> stringResource(R.string.suggest_reps, rangeLabel(slot.planned.reps))
            next.loadKg == null -> stringResource(R.string.suggest_find_weight, rir)
            loadType == LoadType.ASSISTANCE -> stringResource(R.string.suggest_start_assisted, formatKg(next.loadKg, locale))
            else -> stringResource(R.string.suggest_start_weight, formatKg(next.loadKg, locale))
        }
        is Suggestion.Calibrate ->
            if (loadType == LoadType.BODYWEIGHT || next.loadKg == null) {
                stringResource(R.string.suggest_calibrate, rir)
            } else {
                stringResource(R.string.suggest_calibrate_from, formatKg(next.loadKg, locale), rir)
            }
        is Suggestion.Repeat -> {
            val reps = next.lastReps.joinToString(", ")
            when {
                loadType == LoadType.WEIGHT && next.loadKg != null ->
                    stringResource(R.string.suggest_repeat_weight, formatKg(next.loadKg, locale), reps)
                loadType == LoadType.ASSISTANCE && next.loadKg != null ->
                    stringResource(R.string.suggest_repeat_assisted, formatKg(next.loadKg, locale), reps)
                else -> stringResource(R.string.suggest_repeat_reps, reps)
            }
        }
        is Suggestion.Increase ->
            if (loadType == LoadType.ASSISTANCE) {
                stringResource(R.string.suggest_less_assistance, formatKg(next.loadKg, locale), bottom)
            } else {
                stringResource(R.string.suggest_increase, formatKg(next.loadKg, locale), bottom)
            }
        is Suggestion.Deload ->
            if (loadType == LoadType.BODYWEIGHT || next.loadKg == null) {
                stringResource(R.string.suggest_deload)
            } else {
                stringResource(R.string.suggest_deload_weight, formatKg(next.loadKg, locale))
            }
        Suggestion.AddBelt -> stringResource(R.string.suggest_add_belt)
        Suggestion.MakeItHarder -> stringResource(R.string.suggest_make_harder)
    }
}
