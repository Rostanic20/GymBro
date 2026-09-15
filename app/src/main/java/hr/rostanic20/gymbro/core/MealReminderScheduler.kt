package hr.rostanic20.gymbro.core

interface MealReminderScheduler {
    suspend fun reschedule(enabled: Boolean)
}
