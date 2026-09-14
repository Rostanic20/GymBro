package hr.rostanic20.gymbro.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.core.BackupScheduler
import hr.rostanic20.gymbro.core.BackupStore
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.domain.repository.BackupSettingsRepository
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.ui.launchReporting
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder

data class BackupUiState(
    val folderLabel: String?,
    val lastAutoBackupMillis: Long?,
    val isBusy: Boolean,
    val restoreStaged: Boolean,
)

fun folderLabel(folderUri: String): String =
    URLDecoder.decode(folderUri.substringAfterLast('/'), Charsets.UTF_8).substringAfter(':')

class BackupViewModel(
    private val store: BackupStore,
    private val settingsRepository: BackupSettingsRepository,
    private val scheduler: BackupScheduler,
    private val dates: DateProvider,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    private val busy = MutableStateFlow(false)
    private val restoreStaged = MutableStateFlow(false)

    val state: StateFlow<BackupUiState?> =
        combine(settingsRepository.settings(), busy, restoreStaged) { settings, isBusy, staged ->
            BackupUiState(
                folderLabel = settings.folderUri?.let(::folderLabel),
                lastAutoBackupMillis = settings.lastAutoBackupMillis,
                isBusy = isBusy,
                restoreStaged = staged,
            )
        }.stateInWhileSubscribed(viewModelScope, null)

    fun exportFileName(): String = "gymbro-${dates.today()}.zip"

    fun export(destinationUri: String) {
        runExclusive(UserMessage.BackupFailed) {
            store.exportTo(destinationUri)
            _messages.send(UserMessage.BackupExported)
        }
    }

    fun restore(sourceUri: String) {
        runExclusive(UserMessage.RestoreFailed) {
            store.stageRestore(sourceUri)
            restoreStaged.value = true
        }
    }

    fun chooseFolder(folderUri: String) {
        launchReporting(_messages) {
            val previous = settingsRepository.settings().first().folderUri
            store.keepFolderAccess(folderUri)
            settingsRepository.setFolder(folderUri)
            scheduler.schedule(true)
            if (previous != null && previous != folderUri) store.releaseFolderAccess(previous)
        }
    }

    fun turnOffWeeklyBackup() {
        launchReporting(_messages) {
            val previous = settingsRepository.settings().first().folderUri
            settingsRepository.setFolder(null)
            scheduler.schedule(false)
            previous?.let(store::releaseFolderAccess)
        }
    }

    private fun runExclusive(failure: UserMessage, block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("GymBro", "Backup operation failed", e)
                _messages.send(failure)
            } finally {
                busy.value = false
            }
        }
    }
}
