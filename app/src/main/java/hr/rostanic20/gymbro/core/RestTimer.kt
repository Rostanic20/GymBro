package hr.rostanic20.gymbro.core

interface RestTimer {
    fun schedule(endsAtMillis: Long, exerciseName: String)
    fun cancel()
}

fun interface WallClock {
    fun nowMillis(): Long
}
