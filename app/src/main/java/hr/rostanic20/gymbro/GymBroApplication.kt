package hr.rostanic20.gymbro

import android.app.Application
import hr.rostanic20.gymbro.core.BackupScheduler
import hr.rostanic20.gymbro.core.MealReminderScheduler
import hr.rostanic20.gymbro.data.backup.AndroidBackupStore
import hr.rostanic20.gymbro.di.appModule
import hr.rostanic20.gymbro.di.dataStoreModule
import hr.rostanic20.gymbro.domain.repository.BackupSettingsRepository
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.timer.MealReminderReceiver
import hr.rostanic20.gymbro.timer.RestTimerReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GymBroApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Must run before Koin opens the database or DataStore it replaces.
        AndroidBackupStore.applyPendingRestore(this)
        startKoin {
            androidContext(this@GymBroApplication)
            modules(dataStoreModule, appModule)
        }
        RestTimerReceiver.createChannel(this)
        MealReminderReceiver.createChannel(this)
        val profileRepository = get<ProfileRepository>()
        val mealReminders = get<MealReminderScheduler>()
        val backupSettings = get<BackupSettingsRepository>()
        val backupScheduler = get<BackupScheduler>()
        get<CoroutineScope>().launch {
            mealReminders.reschedule(profileRepository.profile().first().mealRemindersEnabled)
            backupScheduler.schedule(backupSettings.settings().first().folderUri != null)
        }
    }
}
