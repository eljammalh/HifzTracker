package com.hifz.tracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.hifz.tracker.data.ReviewGoal
import com.hifz.tracker.data.Surah
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

val LocalArabic = staticCompositionLocalOf { false }

/** Picks the English or Arabic text depending on the language setting. */
@Composable
@ReadOnlyComposable
fun tr(en: String, ar: String): String = if (LocalArabic.current) ar else en

object Fmt {
    /** 1.0 -> "1", 1.5 -> "1.5", 12.25 -> "12.3" */
    fun num(d: Double): String =
        if (abs(d - d.roundToInt()) < 0.05) d.roundToInt().toString()
        else String.format(Locale.US, "%.1f", d)

    fun pct(fraction: Double): String {
        val p = fraction * 100
        return if (p > 0 && p < 10 && abs(p - p.roundToInt()) > 0.05) String.format(Locale.US, "%.1f%%", p)
        else "${p.roundToInt()}%"
    }

    private fun fractionGlyph(f: Double): String? = when {
        f < 0.04 -> ""
        abs(f - 0.25) < 0.04 -> "¼"
        abs(f - 1.0 / 3) < 0.04 -> "⅓"
        abs(f - 0.5) < 0.04 -> "½"
        abs(f - 2.0 / 3) < 0.04 -> "⅔"
        abs(f - 0.75) < 0.04 -> "¾"
        else -> null
    }

    /** Converts a mushaf position (0 = top of page 1) to a label like "3", "3½". */
    private fun pos(x: Double): String {
        val y = x + 1
        var whole = floor(y + 1e-6).toInt()
        val f = y - whole
        if (f > 0.96) { whole += 1; return whole.toString() }
        val g = fractionGlyph(f) ?: return String.format(Locale.US, "%.1f", y)
        return "$whole$g"
    }

    private fun isWhole(x: Double) = abs(x - x.roundToInt()) < 0.04

    /**
     * Describes a range of the mushaf, given absolute positions.
     * Whole pages read as "Page 3" / "Pages 3–5"; partial pages use fractions,
     * where "3½" means the middle of page 3.
     */
    fun range(a: Double, b: Double, ar: Boolean): String {
        if (isWhole(a) && isWhole(b)) {
            val first = a.roundToInt() + 1
            val last = b.roundToInt()
            return if (first >= last) (if (ar) "صفحة $first" else "Page $first")
            else (if (ar) "الصفحات $first–$last" else "Pages $first–$last")
        }
        return if (ar) "صفحة ${pos(a)} ← ${pos(b)}" else "Page ${pos(a)} → ${pos(b)}"
    }

    fun surahRange(s: Surah, from: Double, to: Double, ar: Boolean) = range(s.start + from, s.start + to, ar)

    fun amount(d: Double, ar: Boolean): String {
        val whole = floor(d + 1e-6).toInt()
        val half = d - whole > 0.25
        if (!ar) {
            val n = if (half) (if (whole == 0) "½" else "$whole½") else "$whole"
            return if (d <= 1.0) "$n page" else "$n pages"
        }
        val base = when (whole) {
            0 -> ""
            1 -> "صفحة"
            2 -> "صفحتان"
            else -> "$whole صفحات"
        }
        return when {
            whole == 0 -> "نصف صفحة"
            half -> "$base ونصف"
            else -> base
        }
    }

    fun goal(g: ReviewGoal, ar: Boolean): String {
        val n = g.count
        return if (!ar) {
            if (g.juz) "$n juz" else if (n == 1) "1 page" else "$n pages"
        } else if (g.juz) {
            when {
                n == 1 -> "جزء واحد"
                n == 2 -> "جزءان"
                n <= 10 -> "$n أجزاء"
                else -> "$n جزءًا"
            }
        } else {
            when {
                n == 1 -> "صفحة واحدة"
                n == 2 -> "صفحتان"
                else -> "$n صفحات"
            }
        }
    }

    fun pages(d: Double, ar: Boolean): String =
        if (ar) "${num(d)} صفحة" else if (abs(d - 1) < 0.05) "1 page" else "${num(d)} pages"

    fun date(epochDay: Long, ar: Boolean): String {
        val locale = if (ar) Locale.forLanguageTag("ar") else Locale.ENGLISH
        return LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
    }

    fun days(n: Int, ar: Boolean): String {
        if (!ar) return if (n == 1) "1 day" else "$n days"
        return when {
            n == 1 -> "يوم واحد"
            n == 2 -> "يومان"
            n in 3..10 -> "$n أيام"
            else -> "$n يومًا"
        }
    }

    /** "Today", "3 days ago", "Never" */
    fun ago(day: Long, today: Long, ar: Boolean): String {
        if (day < 0) return if (ar) "لم تُراجع بعد" else "Never reviewed"
        val d = (today - day).toInt()
        return when {
            d <= 0 -> if (ar) "اليوم" else "Today"
            d == 1 -> if (ar) "أمس" else "Yesterday"
            else -> if (ar) "منذ ${days(d, true)}" else "${days(d, false)} ago"
        }
    }

    /** Converts a duration in days to a friendly "≈ 2 years 3 months" string. */
    fun span(total: Int, ar: Boolean): String {
        if (total < 60) return days(total, ar)
        val months = (total / 30.44).roundToInt()
        val y = months / 12
        val m = months % 12
        return if (!ar) {
            val parts = mutableListOf<String>()
            if (y > 0) parts += if (y == 1) "1 year" else "$y years"
            if (m > 0) parts += if (m == 1) "1 month" else "$m months"
            "≈ " + parts.joinToString(" ")
        } else {
            val parts = mutableListOf<String>()
            if (y > 0) parts += when (y) { 1 -> "سنة"; 2 -> "سنتان"; in 3..10 -> "$y سنوات"; else -> "$y سنة" }
            if (m > 0) parts += when (m) { 1 -> "شهر"; 2 -> "شهران"; in 3..10 -> "$m أشهر"; else -> "$m شهرًا" }
            "≈ " + parts.joinToString(" و")
        }
    }

    fun time(h: Int, m: Int): String = String.format(Locale.US, "%02d:%02d", h, m)
}
