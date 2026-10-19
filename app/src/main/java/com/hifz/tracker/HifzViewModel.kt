package com.hifz.tracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hifz.tracker.data.AppDatabase
import com.hifz.tracker.data.LogEntry
import com.hifz.tracker.data.Quran
import com.hifz.tracker.data.ReviewChunk
import com.hifz.tracker.data.ReviewGoal
import com.hifz.tracker.data.Settings
import com.hifz.tracker.data.SettingsRepository
import com.hifz.tracker.data.Surah
import com.hifz.tracker.data.SurahProgress
import com.hifz.tracker.logic.Planner
import com.hifz.tracker.notify.Reminders
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UiState(
    val loaded: Boolean = false,
    val settings: Settings = Settings(),
    val progress: Map<Int, SurahProgress> = emptyMap(),
    val chunks: Map<String, ReviewChunk> = emptyMap(),
    val logs: List<LogEntry> = emptyList(),
    val today: Long = LocalDate.now().toEpochDay(),
    val stats: Planner.Stats = Planner.Stats(),
    val ordered: List<Surah> = Quran.surahs,
    val todaySurah: Surah? = null,
    val todaySegment: Planner.Segment? = null,
    val memorized: List<Surah> = emptyList(),
    val memorizedPagesTotal: Double = 0.0,
    val allUnits: List<Planner.ReviewUnit> = emptyList(),
    val plan: List<Planner.ReviewUnit> = emptyList(),
    /** True when the stored plan belongs to today (false = fresh suggestion not yet saved). */
    val planSaved: Boolean = false,
    /** True when the review goal is larger than everything memorized so far. */
    val goalCapped: Boolean = false,
)

class HifzViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val repo = SettingsRepository(app)

    private val todayFlow = flow {
        while (true) {
            emit(LocalDate.now().toEpochDay())
            delay(60_000)
        }
    }.distinctUntilChanged()

    val state: StateFlow<UiState> = combine(
        dao.progress(), dao.chunks(), dao.logs(), repo.settings, todayFlow,
    ) { progress, chunks, logs, settings, today ->
        build(progress.associateBy { it.surahId }, chunks.associateBy { it.chunkKey }, logs, settings, today)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init {
        // Keep today's review plan stable: save the suggestion once per day.
        viewModelScope.launch {
            state.collect { s ->
                if (s.loaded && !s.planSaved && s.plan.isNotEmpty()) {
                    repo.savePlan(s.today, s.plan.map { it.key })
                }
            }
        }
    }

    private fun build(
        progress: Map<Int, SurahProgress>,
        chunks: Map<String, ReviewChunk>,
        logs: List<LogEntry>,
        settings: Settings,
        today: Long,
    ): UiState {
        val ordered = Planner.ordered(settings.orderBackward)
        val orderIndex = ordered.withIndex().associate { it.value.id to it.index }
        val todaySurah = Planner.todaySurah(progress, settings.currentSurah, settings.orderBackward)
        val todaySegment = todaySurah?.let { s ->
            Planner.segments(s, progress[s.id], settings.dailyAmount).firstOrNull { !it.done }
        }
        val memorized = ordered.filter { progress[it.id]?.memorized == true }
        val memorizedIds = memorized.map { it.id }.toSet()
        val memorizedTotal = memorized.sumOf { it.length }
        val goal = settings.reviewGoal.pages
        val units = Planner.units(memorized, goal, chunks, today)

        val stored = if (settings.planDay == today) {
            settings.planKeys.mapNotNull { Planner.parseUnit(it, memorizedIds, chunks, today) }
        } else emptyList()
        val planSaved = stored.isNotEmpty()
        val plan = if (planSaved) stored else Planner.suggest(units, goal, orderIndex)

        return UiState(
            loaded = true,
            settings = settings,
            progress = progress,
            chunks = chunks,
            logs = logs,
            today = today,
            stats = Planner.stats(progress, logs, settings.dailyAmount, today),
            ordered = ordered,
            todaySurah = todaySurah,
            todaySegment = todaySegment,
            memorized = memorized,
            memorizedPagesTotal = memorizedTotal,
            allUnits = units,
            plan = plan,
            planSaved = planSaved,
            goalCapped = memorized.isNotEmpty() && memorizedTotal < goal - 0.05,
        )
    }

    private fun today() = LocalDate.now().toEpochDay()

    // ---------- Memorization actions ----------

    fun markNextSegment(surahId: Int) = viewModelScope.launch {
        val s = Quran.byId(surahId)
        val p = dao.progressOf(surahId) ?: SurahProgress(surahId)
        if (p.memorized) return@launch
        val daily = repo.settings.first().dailyAmount
        val next = Planner.segments(s, p, daily).firstOrNull { !it.done } ?: return@launch
        val ends = p.ends() + next.to
        val complete = next.to >= s.length - 0.001
        dao.upsertProgress(
            p.copy(
                completedEnds = ends.joinToString(","),
                memorized = complete,
                memorizedDay = if (complete) today() else null,
            ),
        )
        dao.insertLog(LogEntry(day = today(), type = "MEM", pages = next.to - next.from, surahId = surahId))
    }

    fun undoLastSegment(surahId: Int) = viewModelScope.launch {
        val p = dao.progressOf(surahId) ?: return@launch
        if (p.imported) {
            dao.upsertProgress(SurahProgress(surahId))
            return@launch
        }
        val ends = p.ends()
        if (ends.isEmpty()) return@launch
        val removed = ends.last() - (ends.getOrNull(ends.size - 2) ?: 0.0)
        dao.upsertProgress(p.copy(completedEnds = ends.dropLast(1).joinToString(","), memorized = false, memorizedDay = null))
        dao.insertLog(LogEntry(day = today(), type = "MEM", pages = -removed, surahId = surahId, note = "undo"))
    }

    /** For surahs memorized before using the app: counts as memorized but not toward your pace. */
    fun markAlreadyMemorized(surahId: Int) = viewModelScope.launch {
        val s = Quran.byId(surahId)
        val p = dao.progressOf(surahId)
        val done = p?.let { Planner.memorizedPages(s, it) } ?: 0.0
        dao.upsertProgress(
            SurahProgress(
                surahId = surahId,
                completedEnds = s.length.toString(),
                memorized = true,
                memorizedDay = today(),
                imported = true,
            ),
        )
        dao.insertLog(LogEntry(day = today(), type = "IMP", pages = s.length - done, surahId = surahId))
    }

    fun resetSurah(surahId: Int) = viewModelScope.launch {
        val s = Quran.byId(surahId)
        val p = dao.progressOf(surahId) ?: return@launch
        val counted = if (p.imported) 0.0 else Planner.memorizedPages(s, p)
        dao.upsertProgress(SurahProgress(surahId))
        if (counted > 0) {
            dao.insertLog(LogEntry(day = today(), type = "MEM", pages = -counted, surahId = surahId, note = "reset"))
        }
    }

    fun setCurrentSurah(surahId: Int) = viewModelScope.launch { repo.setCurrentSurah(surahId) }

    // ---------- Review actions ----------

    fun toggleReviewed(unit: Planner.ReviewUnit) = viewModelScope.launch {
        val day = today()
        val s = state.value
        // Freeze today's plan before changing anything so it doesn't reshuffle.
        if (!s.planSaved) repo.savePlan(day, s.plan.map { it.key })
        val keys = unit.chunkKeys
        val existing = dao.chunksByKeys(keys).associateBy { it.chunkKey }
        if (!unit.doneToday) {
            dao.upsertChunks(keys.map { k ->
                val e = existing[k]
                when {
                    e == null -> ReviewChunk(k, day, -1)
                    e.lastReviewed == day -> e
                    else -> ReviewChunk(k, day, e.lastReviewed)
                }
            })
            dao.insertLog(LogEntry(day = day, type = "REV", pages = unit.pages, surahId = unit.surahId, note = unit.key))
        } else {
            dao.upsertChunks(keys.mapNotNull { k ->
                val e = existing[k] ?: return@mapNotNull null
                if (e.lastReviewed == day) ReviewChunk(k, e.previousReviewed, e.previousReviewed) else e
            })
            dao.deleteLogs("REV", day, unit.key)
        }
    }

    fun setPlan(keys: List<String>) = viewModelScope.launch { repo.savePlan(today(), keys) }

    fun newSuggestion() = viewModelScope.launch { repo.savePlan(-1, emptyList()) }

    // ---------- Settings ----------

    fun setDailyAmount(v: Double) = viewModelScope.launch { repo.setDailyAmount(v) }
    fun setReviewGoal(g: ReviewGoal) = viewModelScope.launch { repo.setReviewGoal(g) }
    fun setOrderBackward(v: Boolean) = viewModelScope.launch { repo.setOrderBackward(v) }
    fun setArabic(v: Boolean) = viewModelScope.launch { repo.setArabic(v) }

    fun setReminder(type: String, enabled: Boolean) = viewModelScope.launch {
        repo.setReminder(type, enabled)
        reschedule(type)
    }

    fun setReminderTime(type: String, hour: Int, minute: Int) = viewModelScope.launch {
        repo.setReminderTime(type, hour, minute)
        reschedule(type)
    }

    private suspend fun reschedule(type: String) {
        val s = repo.settings.first()
        if (type == "mem") Reminders.schedule(getApplication(), "mem", s.memReminder, s.memHour, s.memMinute)
        else Reminders.schedule(getApplication(), "rev", s.revReminder, s.revHour, s.revMinute)
    }
}
