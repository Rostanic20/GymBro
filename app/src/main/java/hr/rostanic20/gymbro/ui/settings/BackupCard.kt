package hr.rostanic20.gymbro.ui.settings

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.ui.LocalSnackbarHostState
import hr.rostanic20.gymbro.ui.ObserveAsEvents
import hr.rostanic20.gymbro.ui.common.rememberDayFormatter
import hr.rostanic20.gymbro.ui.theme.LocalSpacing
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.ZoneId
import kotlin.system.exitProcess

private val BACKUP_MIME_TYPES = arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")

@Composable
fun BackupCard(viewModel: BackupViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current
    val context = LocalContext.current
    ObserveAsEvents(viewModel.messages) { snackbarHostState.showSnackbar(resources.getString(it.text)) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { viewModel.export(it.toString()) }
    }
    val restorer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.restore(it.toString()) }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.chooseFolder(it.toString()) }
    }
    state?.let {
        BackupCardContent(
            state = it,
            onExport = { exporter.launch(viewModel.exportFileName()) },
            onRestore = { restorer.launch(BACKUP_MIME_TYPES) },
            onPickFolder = { folderPicker.launch(null) },
            onTurnOffWeekly = viewModel::turnOffWeeklyBackup,
            onRestart = { restartApp(context) },
        )
    }
}

@Composable
private fun BackupCardContent(
    state: BackupUiState,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onPickFolder: () -> Unit,
    onTurnOffWeekly: () -> Unit,
    onRestart: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val formatter = rememberDayFormatter()
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.s16),
            verticalArrangement = Arrangement.spacedBy(spacing.s8),
        ) {
            Text(text = stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.backup_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s8)) {
                Button(onClick = onExport, enabled = !state.isBusy) {
                    Text(stringResource(R.string.backup_export))
                }
                OutlinedButton(onClick = { confirmRestore = true }, enabled = !state.isBusy) {
                    Text(stringResource(R.string.backup_restore))
                }
            }
            if (state.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.backup_weekly),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.folderLabel != null,
                    onCheckedChange = { enabled -> if (enabled) onPickFolder() else onTurnOffWeekly() },
                )
            }
            if (state.folderLabel == null) {
                Text(
                    text = stringResource(R.string.backup_weekly_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.backup_weekly_folder, state.folderLabel),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = state.lastAutoBackupMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        stringResource(R.string.backup_last_weekly, date.format(formatter))
                    } ?: stringResource(R.string.backup_no_weekly_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onPickFolder) {
                    Text(stringResource(R.string.backup_change_folder))
                }
            }
        }
    }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text(stringResource(R.string.backup_restore_title)) },
            text = { Text(stringResource(R.string.backup_restore_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRestore = false
                        onRestore()
                    },
                ) {
                    Text(stringResource(R.string.backup_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
    if (state.restoreStaged) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.backup_restore_ready_title)) },
            text = { Text(stringResource(R.string.backup_restore_ready_body)) },
            confirmButton = {
                TextButton(onClick = onRestart) {
                    Text(stringResource(R.string.backup_restart))
                }
            },
        )
    }
}

private fun restartApp(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    context.startActivity(Intent.makeRestartActivityTask(launch.component))
    exitProcess(0)
}
