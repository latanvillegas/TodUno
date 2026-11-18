package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "calendar_notes",
    indices = [
        Index(value = ["dateKey"]),
        Index(value = ["createdAt"])
    ]
)
data class CalendarNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateKey: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val title: String,
    val category: String,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "calculation_history",
    indices = [
        Index(value = ["createdAt"])
    ]
)
data class CalculationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val expression: String,
    val result: String,
    val badgeLabel: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface SeleniaDao {
    @Query("SELECT * FROM calendar_notes ORDER BY reminderHour ASC, reminderMinute ASC, createdAt ASC")
    fun observeAllNotes(): Flow<List<CalendarNoteEntity>>

    @Query("SELECT * FROM calendar_notes ORDER BY year ASC, month ASC, day ASC, reminderHour ASC, reminderMinute ASC, createdAt ASC")
    suspend fun getAllNotesSnapshot(): List<CalendarNoteEntity>

    @Query("SELECT * FROM calendar_notes WHERE reminderEnabled = 1 ORDER BY createdAt ASC")
    suspend fun getNotesWithReminders(): List<CalendarNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: CalendarNoteEntity): Long

    @Query(
        """
        UPDATE calendar_notes
        SET title = :title,
            category = :category,
            reminderEnabled = :reminderEnabled,
            reminderHour = :reminderHour,
            reminderMinute = :reminderMinute
        WHERE id = :noteId
        """
    )
    suspend fun updateNoteFields(
        noteId: Long,
        title: String,
        category: String,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    )

    @Query("DELETE FROM calendar_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("SELECT * FROM calculation_history ORDER BY createdAt DESC LIMIT 30")
    fun observeCalculationHistory(): Flow<List<CalculationHistoryEntity>>

    @Query("SELECT * FROM calculation_history ORDER BY createdAt DESC LIMIT 50")
    suspend fun getAllHistorySnapshot(): List<CalculationHistoryEntity>

    @Query("SELECT COUNT(*) FROM calculation_history")
    suspend fun countHistory(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryRecord(record: CalculationHistoryEntity): Long

    @Query(
        """
        DELETE FROM calculation_history
        WHERE id NOT IN (
            SELECT id FROM calculation_history
            ORDER BY createdAt DESC
            LIMIT :keepLimit
        )
        """
    )
    suspend fun pruneOldHistoryRecords(keepLimit: Int = 50)

    @Transaction
    suspend fun insertAndPruneHistoryRecord(record: CalculationHistoryEntity, keepLimit: Int = 50): Long {
        val insertedId = insertHistoryRecord(record)
        pruneOldHistoryRecords(keepLimit)
        return insertedId
    }

    @Query("DELETE FROM calculation_history")
    suspend fun clearCalculationHistory()
}

@Database(
    entities = [
        CalendarNoteEntity::class,
        CalculationHistoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class SeleniaDatabase : RoomDatabase() {
    abstract fun seleniaDao(): SeleniaDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calendar_notes ADD COLUMN reminderHour INTEGER NOT NULL DEFAULT 9")
                db.execSQL("ALTER TABLE calendar_notes ADD COLUMN reminderMinute INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: SeleniaDatabase? = null

        fun getInstance(context: Context): SeleniaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SeleniaDatabase::class.java,
                    "selenia_lunar_calc.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
