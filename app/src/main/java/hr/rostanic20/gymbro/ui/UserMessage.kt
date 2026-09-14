package hr.rostanic20.gymbro.ui

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

enum class UserMessage(@param:StringRes val text: Int) {
    SaveFailed(R.string.message_save_failed),
    TargetsSaved(R.string.message_targets_saved),
    BackupExported(R.string.message_backup_exported),
    BackupFailed(R.string.message_backup_failed),
    RestoreFailed(R.string.message_restore_failed),
}

fun ViewModel.launchReporting(messages: SendChannel<UserMessage>, block: suspend () -> Unit): Job =
    viewModelScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("GymBro", "Saving failed", e)
            messages.send(UserMessage.SaveFailed)
        }
    }
