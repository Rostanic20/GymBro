package hr.rostanic20.gymbro.ui.common

import java.text.NumberFormat
import java.util.Locale

private const val MAX_KG_FRACTION_DIGITS = 2

fun formatKg(kg: Double, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = MAX_KG_FRACTION_DIGITS
        isGroupingUsed = false
    }.format(kg)

fun formatCount(value: Int, locale: Locale): String =
    NumberFormat.getIntegerInstance(locale).format(value)

fun parseKg(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
