package com.hifz.tracker.logic

import com.hifz.tracker.data.LogEntry
import com.hifz.tracker.data.Quran
import com.hifz.tracker.data.ReviewChunk
import com.hifz.tracker.data.Surah
import com.hifz.tracker.data.SurahProgress
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Pure calculation logic: segments, progress, estimates and review planning. */
object Planner {
    private const val DONE_EPS = 0.001
    /** A leftover smaller than this (in pages) is merged into the previous segment. */
    private const val TINY = 0.05
    const val PACE_WINDOW_DAYS = 14
    const val OVERDUE_DAYS = 14

    data class Segment(
        val index: Int,
        /** Offsets in pages from the start of the surah. */
        val from: Double,
        val to: Double,
        val done: Boolean,
        /** Share of the whole surah this segment represents (0..1). */
        val share: Double,
    )

    // ---------- Memorization ----------

    fun segments(s: Surah, p: SurahProgress?, daily: Double): List<Segment> {
        val total = s.length
        val list = mutableListOf<Segment>()
        var prev = 0.0
        if (p != null && p.memorized && p.ends().isEmpty()) {
            return listOf(Segment(0, 0.0, total, true, 1.0))
        }
        p?.ends()?.forEach { e ->
            val end = min(e, total)
            if (end > prev + 1e-9) {
                list += Segment(list.size, prev, end, true, (end - prev) / total)
                prev = end
            }
        }
        var c = prev
        val step = max(daily, 0.25)
        while (c < total - DONE_EPS) {
            var e = min(c + step, total)
            if (total - e < TINY) e = total
            list += Segment(list.size, c, e, false, (e - c) / total)
            c = e
        }
        return list
    }

    fun memorizedPages(s: Surah, p: SurahProgress?): Double = when {
        p == null -> 0.0
        p.memorized -> s.length
        else -> min(p.lastEnd(), s.length)
    }

    fun fraction(s: Surah, p: SurahProgress?): Double = (memorizedPages(s, p) / s.length).coerceIn(0.0, 1.0)

    fun ordered(backward: Boolean): List<Surah> = if (backward) Quran.surahs.reversed() else Quran.surahs

    /** The surah to work on today: the chosen current surah if still unfinished, else the next one in order. */
    fun todaySurah(progress: Map<Int, SurahProgress>, current: Int, backward: Boolean): Surah? {
        if (current in 1..Quran.TOTAL_SURAHS && progress[current]?.memorized != true) return Quran.byId(current)
        return ordered(backward).firstOrNull { progress[it.id]?.memorized != true }
    }

    data class Stats(
        val pagesMemorized: Double = 0.0,
        val surahsMemorized: Int = 0,
        val remainingPages: Double = Quran.TOTAL_PAGES.toDouble(),
        val plannedDays: Int = 0,
        val plannedEndDay: Long = 0,
        val pacePerDay: Double? = null,
        val paceDays: Int? = null,
        val paceEndDay: Long? = null,
        val todayPages: Double = 0.0,
    )

    fun stats(progress: Map<Int, SurahProgress>, logs: List<LogEntry>, daily: Double, today: Long): Stats {
        var pages = 0.0
        var count = 0
        for (s in Quran.surahs) {
            val p = progress[s.id]
            pages += memorizedPages(s, p)
            if (p?.memorized == true) count++
        }
        val remaining = max(0.0, Quran.TOTAL_PAGES - pages)
        val plannedDays = if (remaining < DONE_EPS) 0 else ceil(remaining / daily - 1e-9).toInt()

        val mem = logs.filter { it.type == "MEM" }
        val todayPages = mem.filter { it.day == today }.sumOf { it.pages }
        var pace: Double? = null
        var paceDays: Int? = null
        var paceEnd: Long? = null
        if (mem.isNotEmpty()) {
            val first = mem.minOf { it.day }
            val window = (today - first + 1).coerceIn(1, PACE_WINDOW_DAYS.toLong())
            val recent = mem.filter { it.day > today - window }.sumOf { it.pages }
            if (recent > DONE_EPS) {
                pace = recent / window
                paceDays = if (remaining < DONE_EPS) 0 else ceil(remaining / pace).toInt()
                paceEnd = today + paceDays
            }
        }
        return Stats(
            pagesMemorized = pages,
            surahsMemorized = count,
            remainingPages = remaining,
            plannedDays = plannedDays,
            plannedEndDay = today + plannedDays,
            pacePerDay = pace,
            paceDays = paceDays,
            paceEndDay = paceEnd,
            todayPages = todayPages,
        )
    }

    // ---------- Review ----------

    data class ReviewUnit(
        val key: String,
        val surahId: Int,
        val fromChunk: Int,
        val toChunk: Int,
        /** Offsets in pages from the start of the surah. */
        val from: Double,
        val to: Double,
        val wholeSurah: Boolean,
        /** Oldest review day among the unit's chunks; -1 = at least part never reviewed. */
        val lastReviewed: Long,
        val doneToday: Boolean,
    ) {
        val pages: Double get() = to - from
        val chunkKeys: List<String> get() = (fromChunk..toChunk).map { chunkKey(surahId, it) }
    }

    fun chunkKey(surahId: Int, index: Int) = "$surahId:$index"

    fun chunkCount(s: Surah): Int = max(1, ceil(s.length - TINY).toInt())

    private fun makeUnit(s: Surah, a: Int, b: Int, chunks: Map<String, ReviewChunk>, today: Long): ReviewUnit {
        val n = chunkCount(s)
        val from = a.toDouble()
        val to = if (b >= n - 1) s.length else (b + 1).toDouble()
        val days = (a..b).map { chunks[chunkKey(s.id, it)]?.lastReviewed ?: -1L }
        return ReviewUnit(
            key = "${s.id}:$a-$b",
            surahId = s.id,
            fromChunk = a,
            toChunk = b,
            from = from,
            to = to,
            wholeSurah = a == 0 && b >= n - 1,
            lastReviewed = days.minOrNull() ?: -1L,
            doneToday = days.all { it == today },
        )
    }

    /** All review units for the memorized surahs, sized so no unit is bigger than the goal. */
    fun units(memorized: List<Surah>, goalPages: Double, chunks: Map<String, ReviewChunk>, today: Long): List<ReviewUnit> {
        val group = max(1, goalPages.toInt())
        val out = mutableListOf<ReviewUnit>()
        for (s in memorized) {
            val n = chunkCount(s)
            if (s.length <= goalPages + TINY) {
                out += makeUnit(s, 0, n - 1, chunks, today)
            } else {
                var a = 0
                while (a < n) {
                    val b = min(n - 1, a + group - 1)
                    out += makeUnit(s, a, b, chunks, today)
                    a = b + 1
                }
            }
        }
        return out
    }

    fun parseUnit(key: String, memorizedIds: Set<Int>, chunks: Map<String, ReviewChunk>, today: Long): ReviewUnit? {
        val parts = key.split(":")
        if (parts.size != 2) return null
        val id = parts[0].toIntOrNull() ?: return null
        if (id !in memorizedIds) return null
        val range = parts[1].split("-")
        if (range.size != 2) return null
        val s = Quran.byId(id)
        val n = chunkCount(s)
        val a = range[0].toIntOrNull()?.coerceIn(0, n - 1) ?: return null
        val b = range[1].toIntOrNull()?.coerceIn(a, n - 1) ?: return null
        return makeUnit(s, a, b, chunks, today)
    }

    /** Picks the units that have waited longest until the daily goal is met. */
    fun suggest(units: List<ReviewUnit>, goalPages: Double, orderIndex: Map<Int, Int>): List<ReviewUnit> {
        val total = units.sumOf { it.pages }
        val sorted = units.sortedWith(
            compareBy<ReviewUnit> { it.lastReviewed }
                .thenBy { orderIndex[it.surahId] ?: it.surahId }
                .thenBy { it.fromChunk },
        )
        if (total <= goalPages + TINY) return sorted
        val picked = mutableListOf<ReviewUnit>()
        var sum = 0.0
        for (u in sorted) {
            if (sum >= goalPages - TINY) break
            picked += u
            sum += u.pages
        }
        return picked
    }
}
