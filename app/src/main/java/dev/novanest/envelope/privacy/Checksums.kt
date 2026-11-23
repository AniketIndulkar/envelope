package dev.novanest.envelope.privacy

object Checksums {
    fun luhn(digits: String): Boolean {
        val d = digits.filter(Char::isDigit)
        if (d.length < 12) return false
        val sum = d.reversed().mapIndexed { i, c ->
            val n = c.digitToInt()
            if (i % 2 == 1) (n * 2).let { if (it > 9) it - 9 else it } else n
        }.sum()
        return sum % 10 == 0
    }

    /** NHS number: 10 digits, weights 10..2 on first nine, check = 11 - (sum % 11); 11→0, 10→invalid. */
    fun nhsMod11(digits: String): Boolean {
        val d = digits.filter(Char::isDigit)
        if (d.length != 10) return false
        val sum = (0 until 9).sumOf { d[it].digitToInt() * (10 - it) }
        val check = when (val c = 11 - sum % 11) { 11 -> 0; 10 -> return false; else -> c }
        return check == d[9].digitToInt()
    }
}
