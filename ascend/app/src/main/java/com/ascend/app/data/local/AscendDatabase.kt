package com.ascend.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        HeroEntity::class,
        QuestEntity::class,
        BookEntity::class,
        LogEntity::class,
        ShopItemEntity::class,
        AchievementEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AscendDatabase : RoomDatabase() {
    abstract fun heroDao(): HeroDao
    abstract fun questDao(): QuestDao
    abstract fun bookDao(): BookDao
    abstract fun logDao(): LogDao
    abstract fun shopDao(): ShopDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        const val NAME = "ascend.db"
    }
}
