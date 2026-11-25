package dev.novanest.envelope.ui.home

import dev.novanest.envelope.privacy.PiiType
import dev.novanest.envelope.ui.Dates
import dev.novanest.envelope.ui.LetterDisplay
import java.time.LocalDate
import kotlin.math.abs

/** One letter as the wallet shows it: everything for the pass front, its strip, and its back (privacy receipt). */
data class WalletLetter(
    val id: String,
    val from: String,
    /** Days left (negative when overdue); null for no deadline or a suspected scam. */
    val days: Int?,
    val scam: Boolean,
    val due: String?,
    val next: String,
    /** Stand-ins that went up to Gemini, in letter order ("Name 1", "Reference 2"). */
    val standIns: List<String>,
    /** What stayed on the phone, by kind: "Names" to 2. */
    val ledger: List<Pair<String, Int>>,
) {
    val kept: Int get() = standIns.size

    val unit: String
        get() = when {
            days == null -> ""
            days < 0 -> if (abs(days) == 1) "day\noverdue" else "days\noverdue"
            else -> if (days == 1) "day\nleft" else "days\nleft"
        }

    /** Short right-hand label on a stacked strip. */
    val stripLabel: String
        get() = when {
            scam -> "Scam?"
            days == null -> "–"
            days < 0 -> "Overdue"
            days == 0 -> "Today"
            else -> "$days ${if (days == 1) "day" else "days"}"
        }

    val frontA11y: String
        get() = buildString {
            append("Letter from $from. ")
            append(
                when {
                    scam -> "Possible scam: check before you do anything."
                    days == null -> "No deadline."
                    days < 0 -> "${abs(days)} ${if (abs(days) == 1) "day" else "days"} overdue."
                    else -> "$days ${if (days == 1) "day" else "days"} left."
                },
            )
            due?.let { append(" $it.") }
        }

    companion object {
        private val TOKEN = Regex("\\[[A-Z_]+_\\d+]")

        private val KIND = mapOf(
            PiiType.NAME to ("Name" to "Names"),
            PiiType.ADDRESS to ("Home address" to "Home addresses"),
            PiiType.POSTCODE to ("Postcode" to "Postcodes"),
            PiiType.NI_NUMBER to ("National Insurance number" to "National Insurance numbers"),
            PiiType.REFERENCE to ("Tax or payment reference" to "Tax and payment references"),
            PiiType.ACCOUNT to ("Account number" to "Account numbers"),
            PiiType.PHONE to ("Phone number" to "Phone numbers"),
            PiiType.EMAIL to ("Email address" to "Email addresses"),
            PiiType.DOB to ("Date of birth" to "Dates of birth"),
        )

        /** Stand-in tokens in [redacted], first appearance order, no repeats. */
        fun tokensIn(redacted: String): List<String> = TOKEN.findAll(redacted).map { it.value }.distinct().toList()

        /** "Names" to 2, "Postcode" to 1…, in the prototype's order (names, addresses, postcodes, NI, references, rest). */
        fun ledgerOf(tokens: List<String>): List<Pair<String, Int>> {
            val counts = tokens.mapNotNull { t ->
                PiiType.entries.firstOrNull { t.startsWith("[${it.name}_") }
            }.groupingBy { it }.eachCount()
            return KIND.keys.filter { counts.containsKey(it) }.map { type ->
                val n = counts.getValue(type)
                val (one, many) = KIND.getValue(type)
                (if (n == 1) one else many) to n
            }
        }

        fun of(item: HomeItem, today: LocalDate = LocalDate.now()): WalletLetter {
            val r = item.record
            val scam = !r.explanation?.redFlags.isNullOrEmpty()
            val days = if (scam) null else Dates.daysLeft(r.deadline, today)
            val pretty = Dates.pretty(r.deadline, today)
            val tokens = tokensIn(r.redactedText)
            return WalletLetter(
                id = r.id,
                from = r.quickCheck?.sender ?: "Letter",
                days = days,
                scam = scam,
                due = if (scam) null else pretty?.let { if (days != null && days < 0) "Was due $it" else "Due $it" },
                next = item.line,
                standIns = tokens.map { LetterDisplay.label(it) },
                ledger = ledgerOf(tokens),
            )
        }
    }
}
