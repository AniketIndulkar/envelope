package dev.novanest.envelope.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object Dates {
    private val sameYear = DateTimeFormatter.ofPattern("d MMMM", Locale.UK)
    private val otherYear = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)

    fun parse(iso: String?): LocalDate? = iso?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }

    fun daysLeft(iso: String?, today: LocalDate = LocalDate.now()): Int? =
        parse(iso)?.let { ChronoUnit.DAYS.between(today, it).toInt() }

    fun pretty(iso: String?, today: LocalDate = LocalDate.now()): String? =
        parse(iso)?.let { (if (it.year == today.year) sameYear else otherYear).format(it) }
}
