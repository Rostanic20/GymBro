package hr.rostanic20.gymbro.ui.program

const val MAX_SETS = 10
const val MAX_REPS = 50

data class Prescription(val sets: Int, val reps: IntRange)

data class PrescriptionForm(val sets: String, val repMin: String, val repMax: String) {

    fun validate(): Prescription? {
        val sets = sets.toIntOrNull()?.takeIf { it in 1..MAX_SETS } ?: return null
        val min = repMin.toIntOrNull()?.takeIf { it in 1..MAX_REPS } ?: return null
        val max = repMax.toIntOrNull()?.takeIf { it in min..MAX_REPS } ?: return null
        return Prescription(sets, min..max)
    }

    companion object {
        fun of(sets: Int, reps: IntRange): PrescriptionForm =
            PrescriptionForm(sets.toString(), reps.first.toString(), reps.last.toString())
    }
}
