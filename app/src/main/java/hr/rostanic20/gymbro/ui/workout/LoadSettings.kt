package hr.rostanic20.gymbro.ui.workout

import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.ui.common.MAX_LOAD_KG
import hr.rostanic20.gymbro.ui.common.parseKg

data class LoadSettings(val startLoadKg: Double?, val incrementKg: Double)

private const val MAX_INCREMENT_KG = 50.0

fun isStartLoadValid(loadType: LoadType, text: String): Boolean =
    text.isBlank() || parseStartLoad(loadType, text) != null

fun isIncrementValid(text: String): Boolean = parseIncrement(text) != null

fun validateLoadSettings(loadType: LoadType, startText: String, incrementText: String): LoadSettings? {
    if (loadType == LoadType.BODYWEIGHT || !isStartLoadValid(loadType, startText)) return null
    val increment = parseIncrement(incrementText) ?: return null
    val start = if (startText.isBlank()) null else parseStartLoad(loadType, startText)
    return LoadSettings(startLoadKg = start, incrementKg = increment)
}

private fun parseStartLoad(loadType: LoadType, text: String): Double? {
    val kg = parseKg(text) ?: return null
    val valid = when (loadType) {
        LoadType.WEIGHT -> kg > 0 && kg <= MAX_LOAD_KG
        LoadType.ASSISTANCE -> kg >= 0 && kg <= MAX_LOAD_KG
        LoadType.BODYWEIGHT -> false
    }
    return kg.takeIf { valid }
}

private fun parseIncrement(text: String): Double? =
    parseKg(text)?.takeIf { it > 0 && it <= MAX_INCREMENT_KG }
