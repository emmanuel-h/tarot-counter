package fr.mandarine.tarotcounter

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

// ─────────────────────────────────────────────────────────────────────────────
// Date formatting for the "Past games" list on the home screen (issue #197).
// Pure JVM code (no Compose), unit-tested in GameDatesTest.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Formats the date a game was saved, in the app's language.
 *
 * - Same year as [now]: short form without the year — "Sat 13 Sep" / "sam. 13 sept."
 * - Another year: the year is appended — "Sat 13 Sep 2025".
 *
 * @param datestamp When the game was saved (milliseconds since 1970, UTC).
 * @param locale    The app language; decides day and month names.
 * @param now       The current time — a parameter (not read from the clock) so
 *                  tests can pin it.
 * @param timeZone  The zone the date is shown in; the device zone by default.
 */
fun formatGameDate(
    datestamp: Long,
    locale: AppLocale,
    now: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    // Calendar lets us read the year of each instant in the chosen time zone.
    fun yearOf(millis: Long): Int =
        Calendar.getInstance(timeZone).apply { timeInMillis = millis }.get(Calendar.YEAR)

    // EEE = short weekday, d = day of month, MMM = short month, yyyy = year.
    val pattern = if (yearOf(datestamp) == yearOf(now)) "EEE d MMM" else "EEE d MMM yyyy"
    val format  = SimpleDateFormat(pattern, locale.javaLocale)
    format.timeZone = timeZone
    return format.format(Date(datestamp))
}
