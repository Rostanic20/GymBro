package hr.rostanic20.gymbro.timer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import hr.rostanic20.gymbro.core.MealReminderScheduler
import hr.rostanic20.gymbro.domain.nextMealReminder
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmMealReminderScheduler(private val context: Context) : MealReminderScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // USE_EXACT_ALARM is granted at install on API 33+; lint only knows SCHEDULE_EXACT_ALARM.
    @SuppressLint("MissingPermission")
    override fun reschedule(enabled: Boolean) {
        alarmManager.cancel(pendingIntent(mealSlot = NO_MEAL))
        if (!enabled || !alarmManager.canScheduleExactAlarms()) return
        val next = nextMealReminder(LocalDateTime.now()) ?: return
        val triggerAtMillis = next.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent(next.meal.slot))
    }

    private fun pendingIntent(mealSlot: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, MealReminderReceiver::class.java).putExtra(MealReminderReceiver.EXTRA_MEAL_SLOT, mealSlot),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val REQUEST_CODE = 2
        const val NO_MEAL = -1
    }
}
