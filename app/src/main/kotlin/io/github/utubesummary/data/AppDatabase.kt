package io.github.utubesummary.data

import androidx.room3.Database
import androidx.room3.RoomDatabase
import io.github.utubesummary.data.local.database.entity.HistoryEntity

@Database(
    entities = [HistoryEntity::class, AIProviderConfigEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun aiProviderConfigDao(): AIProviderConfigDao
}