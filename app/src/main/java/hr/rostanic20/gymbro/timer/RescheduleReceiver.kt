package hr.rostanic20.gymbro.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import hr.rostanic20.gymbro.core.MealReminderScheduler
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val RESCHEDULE_ACTIONS = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
)

class RescheduleReceiver : BroadcastReceiver(), KoinComponent {

    private val profileRepository: ProfileRepository by inject()
    private val scheduler: MealReminderScheduler by inject()
    private val appScope: CoroutineScope by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return
        val pending = goAsync()
        appScope.launch {
            try {
                scheduler.reschedule(profileRepository.profile().first().mealRemindersEnabled)
            } finally {
                pending.finish()
            }
        }
    }
}
