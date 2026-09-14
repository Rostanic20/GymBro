package hr.rostanic20.gymbro.ui.session

import hr.rostanic20.gymbro.domain.model.LoadType
import hr.rostanic20.gymbro.domain.model.SetValues
import hr.rostanic20.gymbro.ui.common.MAX_LOAD_KG
import hr.rostanic20.gymbro.ui.common.parseKg

private const val MAX_REPS = 100
private const val MAX_RIR = 10

fun validateSetInput(loadType: LoadType, loadText: String, repsText: String, rirText: String): SetValues? {
    val reps = repsText.trim().toIntOrNull()?.takeIf { it in 1..MAX_REPS } ?: return null
    val rir = if (rirText.isBlank()) {
        null
    } else {
        rirText.trim().toIntOrNull()?.takeIf { it in 0..MAX_RIR } ?: return null
    }
    val load = when (loadType) {
        LoadType.WEIGHT -> parseKg(loadText)?.takeIf { it > 0 && it <= MAX_LOAD_KG } ?: return null
        LoadType.ASSISTANCE -> parseKg(loadText)?.takeIf { it >= 0 && it <= MAX_LOAD_KG } ?: return null
        LoadType.BODYWEIGHT -> null
    }
    return SetValues(loadKg = load, reps = reps, rir = rir)
}
