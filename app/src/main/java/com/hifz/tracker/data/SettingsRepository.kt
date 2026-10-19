package com.hifz.tracker.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** A daily review goal: a number of pages (1–10) or a number of juz (1–30). */
data class ReviewGoal(val juz: Boolean, val count: Int) {
    val pages: Double get() = if (juz) count * Quran.PAGES_PER_JUZ else count.toDouble()
    fun encode(): String = (if (juz) "j" else "p") + count

    companion object {
        val ALL: List<ReviewGoal> = (1..10).map { ReviewGoal(false, it) } + (1..30).map { ReviewGoal(true, it) }
        fun decode(s: String?): ReviewGoal {
            if (s == null || s.length < 2) return ReviewGoal(false, 2)
            val n = s.substring(1).toIntOrNull() ?: return ReviewGoal(false, 2)
            return ReviewGoal(s[0] == 'j', n)
        }
    }
}

data class Settings(
    val dailyAmount: Double = 1.0,
    val reviewGoal: ReviewGoal = ReviewGoal(false, 2),
    /** true = start from An-Nas and move backward; false = start from Al-Fatihah. */
    val orderBackward: Boolean = true,
    val arabic: Boolean = false,
    /** Surah the user chose to work on now; 0 = follow the chosen order automatically. */
    val currentSurah: Int = 0,
    val memReminder: Boolean = false,
    val memHour: Int = 6,
    val memMinute: Int = 0,
    val revReminder: Boolean = false,
    val revHour: Int = 20,
    val revMinute: Int = 0,
    /** Today's review plan, stored so it stays stable through the day. */
    val planDay: Long = -1,
    val planKeys: List<String> = emptyList(),
)

class SettingsRepository(private val context: Context) {
    private object K {
        val daily = doublePreferencesKey("daily_amount")
        val goal = stringPreferencesKey("review_goal")
        val backward = booleanPreferencesKey("order_backward")
        val arabic = booleanPreferencesKey("arabic")
        val current = intPreferencesKey("current_surah")
        val memRem = booleanPreferencesKey("mem_reminder")
        val memH = intPreferencesKey("mem_hour")
        val memM = intPreferencesKey("mem_minute")
        val revRem = booleanPreferencesKey("rev_reminder")
        val revH = intPreferencesKey("rev_hour")
        val revM = intPreferencesKey("rev_minute")
        val planDay = longPreferencesKey("plan_day")
        val planKeys = stringPreferencesKey("plan_keys")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p -> fromPrefs(p) }

    private fun fromPrefs(p: Preferences): Settings {
        val d = Settings()
        return Settings(
            dailyAmount = p[K.daily] ?: d.dailyAmount,
            reviewGoal = p[K.goal]?.let { ReviewGoal.decode(it) } ?: d.reviewGoal,
            orderBackward = p[K.backward] ?: d.orderBackward,
            arabic = p[K.arabic] ?: d.arabic,
            currentSurah = p[K.current] ?: d.currentSurah,
            memReminder = p[K.memRem] ?: d.memReminder,
            memHour = p[K.memH] ?: d.memHour,
            memMinute = p[K.memM] ?: d.memMinute,
            revReminder = p[K.revRem] ?: d.revReminder,
            revHour = p[K.revH] ?: d.revHour,
            revMinute = p[K.revM] ?: d.revMinute,
            planDay = p[K.planDay] ?: d.planDay,
            planKeys = p[K.planKeys]?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
        )
    }

    suspend fun setDailyAmount(v: Double) { context.dataStore.edit { it[K.daily] = v } }

    suspend fun setReviewGoal(g: ReviewGoal) {
        context.dataStore.edit {
            it[K.goal] = g.encode()
            it[K.planDay] = -1L // regenerate today's review plan for the new goal
        }
    }

    suspend fun setOrderBackward(v: Boolean) { context.dataStore.edit { it[K.backward] = v } }
    suspend fun setArabic(v: Boolean) { context.dataStore.edit { it[K.arabic] = v } }
    suspend fun setCurrentSurah(id: Int) { context.dataStore.edit { it[K.current] = id } }

    suspend fun setReminder(type: String, enabled: Boolean) {
        val key = if (type == "mem") K.memRem else K.revRem
        context.dataStore.edit { it[key] = enabled }
    }

    suspend fun setReminderTime(type: String, hour: Int, minute: Int) {
        val hKey = if (type == "mem") K.memH else K.revH
        val mKey = if (type == "mem") K.memM else K.revM
        context.dataStore.edit {
            it[hKey] = hour
            it[mKey] = minute
        }
    }

    suspend fun savePlan(day: Long, keys: List<String>) {
        context.dataStore.edit {
            it[K.planDay] = day
            it[K.planKeys] = keys.joinToString("|")
        }
    }
}
