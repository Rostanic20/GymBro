package hr.rostanic20.gymbro.data.backup

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import hr.rostanic20.gymbro.core.BackupScheduler
import hr.rostanic20.gymbro.core.BackupStore
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.core.WallClock
import hr.rostanic20.gymbro.domain.repository.BackupSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

private const val WEEKLY_BACKUP_WORK = "weekly-backup"
private const val BACKUP_INTERVAL_DAYS = 7L
private const val MAX_ATTEMPTS = 3

class WorkManagerBackupScheduler(private val context: Context) : BackupScheduler {

    override fun schedule(enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<WeeklyBackupWorker>(BACKUP_INTERVAL_DAYS, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            workManager.enqueueUniquePeriodicWork(WEEKLY_BACKUP_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            workManager.cancelUniqueWork(WEEKLY_BACKUP_WORK)
        }
    }
}

class WeeklyBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val store: BackupStore by inject()
    private val settings: BackupSettingsRepository by inject()
    private val dates: DateProvider by inject()
    private val clock: WallClock by inject()

    override suspend fun doWork(): Result {
        val folderUri = settings.settings().first().folderUri ?: return Result.success()
        return try {
            store.exportToFolder(folderUri, dates.today())
            settings.recordAutoBackup(clock.nowMillis())
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("GymBro", "Weekly backup failed", e)
            if (runAttemptCount + 1 < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }
}
