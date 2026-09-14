package hr.rostanic20.gymbro

import android.app.Application
import hr.rostanic20.gymbro.core.MealReminderScheduler
import hr.rostanic20.gymbro.di.appModule
import hr.rostanic20.gymbro.di.dataStoreModule
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
        startKoin {
            androidContext(this@GymBroApplication)
            modules(dataStoreModule, appModule)
        }
        RestTimerReceiver.createChannel(this)
        MealReminderReceiver.createChannel(this)
        val profileRepository = get<ProfileRepository>()
        val scheduler = get<MealReminderScheduler>()
        get<CoroutineScope>().launch {
            scheduler.reschedule(profileRepository.profile().first().mealRemindersEnabled)
        }
    }
}
