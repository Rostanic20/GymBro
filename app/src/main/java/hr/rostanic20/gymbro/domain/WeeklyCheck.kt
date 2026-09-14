package hr.rostanic20.gymbro.domain

import hr.rostanic20.gymbro.domain.model.BodyWeight
import hr.rostanic20.gymbro.domain.model.WaistMeasurement
import java.time.LocalDate

const val CHECK_FROM_WEEK = 5
const val CALORIE_STEP = 150
const val RECHECK_AFTER_DAYS = 14L
const val WEIGHT_CHECK_HISTORY_DAYS = 20L
const val WAIST_WINDOW_DAYS = 28L
val CALORIE_ADJUSTMENT_RANGE = -600..900

private const val FLAT_WEEKLY_CHANGE_KG = 0.1
private const val FAST_WEEKLY_CHANGE_KG = 0.5
private const val WAIST_LIMIT_CM = 1.0
private const val DAYS_PER_WEEK = 7L

sealed interface WeeklyCheck {
    data object TooEarly : WeeklyCheck
    data class RecentlyAdjusted(val recheckOn: LocalDate) : WeeklyCheck
    data object NotEnoughData : WeeklyCheck
    data class OnTrack(val weeklyChangeKg: Double) : WeeklyCheck
    data class EatMore(val weeklyChangeKg: Double) : WeeklyCheck
    data class EatLessFastGain(val weeklyChangeKg: Double) : WeeklyCheck
    data class EatLessWaist(val waistGainCm: Double) : WeeklyCheck
}

val WeeklyCheck.calorieChange: Int
    get() = when (this) {
        is WeeklyCheck.EatMore -> CALORIE_STEP
        is WeeklyCheck.EatLessFastGain, is WeeklyCheck.EatLessWaist -> -CALORIE_STEP
        else -> 0
    }

fun weeklyCheck(
    week: Int?,
    today: LocalDate,
    weights: List<BodyWeight>,
    waists: List<WaistMeasurement>,
    lastAdjustment: LocalDate?,
): WeeklyCheck {
    if (week == null || week < CHECK_FROM_WEEK) return WeeklyCheck.TooEarly
    if (lastAdjustment != null) {
        val recheckOn = lastAdjustment.plusDays(RECHECK_AFTER_DAYS)
        if (today < recheckOn) return WeeklyCheck.RecentlyAdjusted(recheckOn)
    }
    waistGain(waists, today)?.takeIf { it >= WAIST_LIMIT_CM }?.let { return WeeklyCheck.EatLessWaist(it) }
    val thisWeek = weights.weeklyChange(today) ?: return WeeklyCheck.NotEnoughData
    val lastWeek = weights.weeklyChange(today.minusDays(DAYS_PER_WEEK)) ?: return WeeklyCheck.NotEnoughData
    return when {
        thisWeek > FAST_WEEKLY_CHANGE_KG && lastWeek > FAST_WEEKLY_CHANGE_KG -> WeeklyCheck.EatLessFastGain(thisWeek)
        thisWeek < FLAT_WEEKLY_CHANGE_KG && lastWeek < FLAT_WEEKLY_CHANGE_KG -> WeeklyCheck.EatMore(thisWeek)
        else -> WeeklyCheck.OnTrack(thisWeek)
    }
}

fun waistGain(waists: List<WaistMeasurement>, today: LocalDate): Double? {
    val latest = waists.filter { it.date <= today }.maxByOrNull { it.date } ?: return null
    val baseline = waists.filter { it.date <= latest.date.minusDays(WAIST_WINDOW_DAYS) }.maxByOrNull { it.date }
        ?: return null
    return latest.waistCm - baseline.waistCm
}
