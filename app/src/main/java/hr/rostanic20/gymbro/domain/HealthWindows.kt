package hr.rostanic20.gymbro.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class TimeWindow(val start: ZonedDateTime, val end: ZonedDateTime)

private val SLEEP_WINDOW_START: LocalTime = LocalTime.of(18, 0)
private val SLEEP_WINDOW_END: LocalTime = LocalTime.NOON

fun stepsWindow(date: LocalDate, now: ZonedDateTime): TimeWindow {
    val start = date.atStartOfDay(now.zone)
    val endOfDay = date.plusDays(1).atStartOfDay(now.zone)
    val end = when {
        now.isBefore(start) -> start
        now.isBefore(endOfDay) -> now
        else -> endOfDay
    }
    return TimeWindow(start, end)
}

fun sleepWindow(date: LocalDate, zone: ZoneId): TimeWindow =
    TimeWindow(
        start = date.minusDays(1).atTime(SLEEP_WINDOW_START).atZone(zone),
        end = date.atTime(SLEEP_WINDOW_END).atZone(zone),
    )
