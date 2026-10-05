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
        TalentEntity::class,
        FocusSessionEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AscendDatabase : RoomDatabase() {
    abstract fun heroDao(): HeroDao
    abstract fun questDao(): QuestDao
    abstract fun bookDao(): BookDao
    abstract fun logDao(): LogDao
    abstract fun shopDao(): ShopDao
    abstract fun achievementDao(): AchievementDao
    abstract fun talentDao(): TalentDao
    abstract fun focusDao(): FocusDao

    companion object {
        const val NAME = "ascend.db"
        const val VERSION = 2
    }
}
