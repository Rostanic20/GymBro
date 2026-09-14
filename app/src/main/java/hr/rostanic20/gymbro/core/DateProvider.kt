package hr.rostanic20.gymbro.core

import java.time.LocalDate

fun interface DateProvider {
    fun today(): LocalDate
}
