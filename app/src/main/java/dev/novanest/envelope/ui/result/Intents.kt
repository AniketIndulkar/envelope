package dev.novanest.envelope.ui.result

import android.content.Intent
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneId

/** Opens the calendar app with an all-day event prefilled. No calendar permission needed. */
fun calendarIntent(title: String, description: String, date: LocalDate): Intent {
    val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, title)
        .putExtra(CalendarContract.Events.DESCRIPTION, description)
        .putExtra(CalendarContract.Events.ALL_DAY, true)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 24 * 60 * 60 * 1000)
}

fun shareIntent(text: String): Intent =
    Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null)
