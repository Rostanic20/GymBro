package hr.rostanic20.gymbro.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

interface DateProvider {
    fun today(): LocalDate
    fun todayFlow(): Flow<LocalDate>
}

private const val MIN_WAIT_MILLIS = 1_000L

class SystemDateProvider(
    private val now: () -> ZonedDateTime = { ZonedDateTime.now() },
) : DateProvider {

    override fun today(): LocalDate = now().toLocalDate()

    override fun todayFlow(): Flow<LocalDate> = flow {
        while (true) {
            val current = now()
            emit(current.toLocalDate())
            val nextMidnight = current.toLocalDate().plusDays(1).atStartOfDay(current.zone)
            delay((Duration.between(current, nextMidnight).toMillis() + 1).coerceAtLeast(MIN_WAIT_MILLIS))
        }
    }.distinctUntilChanged()
}
