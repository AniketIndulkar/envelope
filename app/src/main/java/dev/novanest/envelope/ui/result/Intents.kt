package dev.novanest.envelope.ui.result

import android.content.Intent
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneOffset

/** Opens the calendar app with an all-day event prefilled. No calendar permission needed. */
/** All-day events in CalendarContract are UTC midnight to midnight, whatever the phone's time zone. */
fun allDayMillis(date: LocalDate): Pair<Long, Long> {
    val start = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    return start to start + 24 * 60 * 60 * 1000
}

fun calendarIntent(title: String, description: String, date: LocalDate): Intent {
    val (start, end) = allDayMillis(date)
    return Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, title)
        .putExtra(CalendarContract.Events.DESCRIPTION, description)
        .putExtra(CalendarContract.Events.ALL_DAY, true)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
}

fun shareIntent(text: String): Intent =
    Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null)
