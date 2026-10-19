package com.hifz.tracker.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Memorization progress for one surah.
 * [completedEnds] holds the end of each completed segment, measured in pages from the start
 * of the surah (e.g. "0.5,1.0,1.5"). Keeping each completed segment means changing the daily
 * amount later only re-divides what is still left.
 */
@Entity(tableName = "surah_progress")
data class SurahProgress(
    @PrimaryKey val surahId: Int,
    val completedEnds: String = "",
    val memorized: Boolean = false,
    val memorizedDay: Long? = null,
    /** True when the surah was marked as memorized before using the app. */
    val imported: Boolean = false,
) {
    fun ends(): List<Double> =
        if (completedEnds.isBlank()) emptyList()
        else completedEnds.split(",").mapNotNull { it.trim().toDoubleOrNull() }

    fun lastEnd(): Double = ends().lastOrNull() ?: 0.0
}

/**
 * Review tracking. Each memorized surah is split into one-page chunks (key "surahId:chunkIndex").
 * Day values are epoch days; -1 means never reviewed.
 */
@Entity(tableName = "review_chunks")
data class ReviewChunk(
    @PrimaryKey val chunkKey: String,
    val lastReviewed: Long,
    val previousReviewed: Long,
)

/** Activity log. type = MEM (memorized), REV (reviewed), IMP (marked as previously memorized). */
@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val type: String,
    val pages: Double,
    val surahId: Int,
    val note: String = "",
)

@Dao
interface HifzDao {
    @Query("SELECT * FROM surah_progress")
    fun progress(): Flow<List<SurahProgress>>

    @Query("SELECT * FROM surah_progress WHERE surahId = :id")
    suspend fun progressOf(id: Int): SurahProgress?

    @Upsert
    suspend fun upsertProgress(p: SurahProgress)

    @Query("SELECT * FROM review_chunks")
    fun chunks(): Flow<List<ReviewChunk>>

    @Query("SELECT * FROM review_chunks WHERE chunkKey IN (:keys)")
    suspend fun chunksByKeys(keys: List<String>): List<ReviewChunk>

    @Upsert
    suspend fun upsertChunks(c: List<ReviewChunk>)

    @Query("SELECT * FROM logs ORDER BY day DESC, id DESC")
    fun logs(): Flow<List<LogEntry>>

    @Insert
    suspend fun insertLog(l: LogEntry)

    @Query("DELETE FROM logs WHERE type = :type AND day = :day AND note = :note")
    suspend fun deleteLogs(type: String, day: Long, note: String)
}

@Database(
    entities = [SurahProgress::class, ReviewChunk::class, LogEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): HifzDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hifz.db",
                ).build().also { instance = it }
            }
    }
}
