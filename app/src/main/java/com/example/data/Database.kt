package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortsDraftDao {
    @Query("SELECT * FROM shorts_drafts ORDER BY timestamp DESC")
    fun getAllDrafts(): Flow<List<ShortsDraft>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: ShortsDraft): Long

    @Query("DELETE FROM shorts_drafts WHERE id = :id")
    suspend fun deleteDraftById(id: Int)

    @Query("DELETE FROM shorts_drafts")
    suspend fun clearAllDrafts()
}

@Database(entities = [ShortsDraft::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shortsDao(): ShortsDraftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shorts_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ShortsRepository(private val shortsDraftDao: ShortsDraftDao) {
    val allDrafts: Flow<List<ShortsDraft>> = shortsDraftDao.getAllDrafts()

    suspend fun insertDraft(draft: ShortsDraft): Long {
        return shortsDraftDao.insertDraft(draft)
    }

    suspend fun deleteDraft(id: Int) {
        shortsDraftDao.deleteDraftById(id)
    }

    suspend fun clearAll() {
        shortsDraftDao.clearAllDrafts()
    }
}
