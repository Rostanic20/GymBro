package hr.rostanic20.gymbro.timer

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import hr.rostanic20.gymbro.R
import hr.rostanic20.gymbro.core.MealReminderScheduler
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.ui.common.labelRes
import hr.rostanic20.gymbro.ui.common.tipRes
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MealReminderReceiver : BroadcastReceiver(), KoinComponent {

    private val scheduler: MealReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val slot = intent.getIntExtra(EXTRA_MEAL_SLOT, -1)
        Meal.entries.firstOrNull { it.slot == slot }?.let { notify(context, it) }
        scheduler.reschedule(enabled = true)
    }

    private fun notify(context: Context, meal: Meal) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        if (permission != PackageManager.PERMISSION_GRANTED) return
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val tip = context.getString(meal.tipRes)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_meal)
            .setContentTitle(context.getString(R.string.meal_reminder_title, context.getString(meal.labelRes)))
            .setContentText(tip)
            .setStyle(NotificationCompat.BigTextStyle().bigText(tip))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "meal_reminders"
        const val NOTIFICATION_ID = 2
        const val EXTRA_MEAL_SLOT = "meal_slot"

        fun createChannel(context: Context) {
            val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.meal_channel_name))
                .build()
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }
}
