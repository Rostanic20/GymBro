package hr.rostanic20.gymbro.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.domain.MAINTENANCE_WEEKS
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.MealSettings
import hr.rostanic20.gymbro.domain.model.Profile
import hr.rostanic20.gymbro.domain.nutritionTargets
import hr.rostanic20.gymbro.ui.LocalSnackbarHostState
import hr.rostanic20.gymbro.ui.ObserveAsEvents
import hr.rostanic20.gymbro.ui.common.formatCount
import hr.rostanic20.gymbro.ui.common.LoadingScreen
import hr.rostanic20.gymbro.ui.common.labelRes
import hr.rostanic20.gymbro.ui.common.rememberDayFormatter
import hr.rostanic20.gymbro.ui.common.toUtcMillis
import hr.rostanic20.gymbro.ui.common.utcMillisToLocalDate
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class SettingsActions(
    val changeStart: (LocalDate) -> Unit,
    val reset: () -> Unit,
    val saveTargets: (Targets) -> Unit,
    val setMealReminders: (Boolean) -> Unit,
    val setMealTime: (Meal, LocalTime) -> Unit,
    val setMealEnabled: (Meal, Boolean) -> Unit,
    val openFoods: () -> Unit,
)

@Composable
fun SettingsScreen(
    onOpenFoods: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val mealSettings by viewModel.mealSettings.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.messages) { snackbarHostState.showSnackbar(resources.getString(it.text)) }
    if (profile == null) {
        LoadingScreen()
    }
    profile?.let {
        SettingsContent(
            profile = it,
            mealSettings = mealSettings,
            actions = SettingsActions(
                changeStart = viewModel::changeProgramStart,
                reset = viewModel::resetProgram,
                saveTargets = viewModel::saveTargets,
                setMealReminders = viewModel::setMealReminders,
                setMealTime = viewModel::setMealTime,
                setMealEnabled = viewModel::setMealEnabled,
                openFoods = onOpenFoods,
            ),
            backupCard = { BackupCard() },
        )
    }
}

@Composable
internal fun SettingsContent(
    profile: Profile,
    actions: SettingsActions,
    mealSettings: MealSettings = MealSettings(),
    backupCard: @Composable () -> Unit = {},
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.s16),
        verticalArrangement = Arrangement.spacedBy(spacing.s16),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        ProgramCard(programStart = profile.programStart, onChangeStart = actions.changeStart, onReset = actions.reset)
        MealsCard(
            remindersEnabled = profile.mealRemindersEnabled,
            mealSettings = mealSettings,
            actions = actions,
        )
        TargetsCard(profile = profile, onSave = actions.saveTargets)
        backupCard()
    }
}

@Composable
private fun ProgramCard(
    programStart: LocalDate?,
    onChangeStart: (LocalDate) -> Unit,
    onReset: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val formatter = rememberDayFormatter()
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s8),
        ) {
            Text(
                text = stringResource(R.string.settings_program_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = programStart?.let { stringResource(R.string.settings_program_started, it.format(formatter)) }
                    ?: stringResource(R.string.settings_program_not_started),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (programStart == null) {
                Text(
                    text = stringResource(R.string.settings_program_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s8)) {
                Button(onClick = { showPicker = true }) {
                    Text(
                        stringResource(
                            if (programStart != null) R.string.settings_change_start else R.string.settings_start_program,
                        ),
                    )
                }
                if (programStart != null) {
                    OutlinedButton(onClick = { confirmReset = true }) {
                        Text(stringResource(R.string.settings_reset_program))
                    }
                }
            }
        }
    }
    if (showPicker) {
        StartDatePickerDialog(
            initial = programStart,
            onDismiss = { showPicker = false },
            onConfirm = {
                showPicker = false
                onChangeStart(it)
            },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.settings_reset_title)) },
            text = { Text(stringResource(R.string.settings_reset_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        onReset()
                    },
                ) {
                    Text(stringResource(R.string.settings_reset_program))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDatePickerDialog(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial?.toUtcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(utcMillisToLocalDate(it)) } },
                enabled = state.selectedDateMillis != null,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun MealsCard(
    remindersEnabled: Boolean,
    mealSettings: MealSettings,
    actions: SettingsActions,
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val onRemindersChange = actions.setMealReminders
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onRemindersChange(true)
    }
    var editing by rememberSaveable { mutableStateOf<Meal?>(null) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s8),
        ) {
            Text(
                text = stringResource(R.string.settings_meals_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.settings_meal_reminders),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { enabled ->
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                        if (enabled && !granted) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onRemindersChange(enabled)
                        }
                    },
                )
            }
            if (!remindersEnabled) {
                Text(
                    text = stringResource(R.string.settings_meal_reminders_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            mealSettings.orderedMeals.forEach { meal ->
                MealRow(
                    meal = meal,
                    time = mealSettings.timeFor(meal),
                    enabled = mealSettings.isEnabled(meal),
                    onEditTime = { editing = meal },
                    onEnabledChange = { actions.setMealEnabled(meal, it) },
                )
            }
            OutlinedButton(onClick = actions.openFoods) {
                Text(stringResource(R.string.settings_open_foods))
            }
        }
    }
    editing?.let { meal ->
        MealTimeDialog(
            meal = meal,
            time = mealSettings.timeFor(meal),
            onDismiss = { editing = null },
            onConfirm = {
                editing = null
                actions.setMealTime(meal, it)
            },
        )
    }
}

@Composable
private fun MealRow(
    meal: Meal,
    time: LocalTime,
    enabled: Boolean,
    onEditTime: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s8),
    ) {
        Text(
            text = stringResource(meal.labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onEditTime, enabled = enabled) {
            Text(time.format(formatter))
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealTimeDialog(
    meal: Meal,
    time: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(meal.labelRes)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
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
private fun TargetsCard(profile: Profile, onSave: (Targets) -> Unit) {
    val spacing = LocalSpacing.current
    val locale = LocalLocale.current.platformLocale
    val saved = profile.toTargetsForm()
    var maintenance by rememberSaveable(saved.maintenanceKcal) { mutableStateOf(saved.maintenanceKcal) }
    var surplus by rememberSaveable(saved.surplusKcal) { mutableStateOf(saved.surplusKcal) }
    var protein by rememberSaveable(saved.proteinG) { mutableStateOf(saved.proteinG) }
    var fat by rememberSaveable(saved.fatG) { mutableStateOf(saved.fatG) }
    val form = TargetsForm(maintenanceKcal = maintenance, surplusKcal = surplus, proteinG = protein, fatG = fat)
    val targets = form.validate()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s12),
        ) {
            Text(
                text = stringResource(R.string.settings_targets_title),
                style = MaterialTheme.typography.titleMedium,
            )
            NumberField(value = maintenance, onValueChange = { maintenance = it }, label = R.string.settings_maintenance)
            NumberField(value = surplus, onValueChange = { surplus = it }, label = R.string.settings_surplus)
            NumberField(value = protein, onValueChange = { protein = it }, label = R.string.settings_protein)
            NumberField(value = fat, onValueChange = { fat = it }, label = R.string.settings_fat)
            if (targets == null) {
                Text(
                    text = stringResource(R.string.settings_targets_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                val surplusDay = profile.copy(
                    maintenanceKcal = targets.maintenanceKcal,
                    surplusKcal = targets.surplusKcal,
                    proteinG = targets.proteinG,
                    fatG = targets.fatG,
                ).nutritionTargets(week = MAINTENANCE_WEEKS + 1)
                Text(
                    text = stringResource(
                        R.string.settings_targets_preview,
                        formatCount(surplusDay.kcal, locale),
                        formatCount(surplusDay.carbsG, locale),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = { targets?.let(onSave) }, enabled = targets != null && form != saved) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, @StringRes label: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
