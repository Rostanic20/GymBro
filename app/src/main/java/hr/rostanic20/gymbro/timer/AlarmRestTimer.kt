package hr.rostanic20.gymbro.timer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import hr.rostanic20.gymbro.core.RestTimer

class AlarmRestTimer(private val context: Context) : RestTimer {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // USE_EXACT_ALARM is granted at install on API 33+; lint only knows SCHEDULE_EXACT_ALARM.
    @SuppressLint("MissingPermission")
    override fun schedule(endsAtMillis: Long, exerciseName: String) {
        if (!alarmManager.canScheduleExactAlarms()) return
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtMillis, pendingIntent(exerciseName))
    }

    override fun cancel() {
        alarmManager.cancel(pendingIntent(exerciseName = null))
        NotificationManagerCompat.from(context).cancel(RestTimerReceiver.NOTIFICATION_ID)
    }

    private fun pendingIntent(exerciseName: String?): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, RestTimerReceiver::class.java).putExtra(RestTimerReceiver.EXTRA_EXERCISE, exerciseName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val REQUEST_CODE = 1
    }
}
