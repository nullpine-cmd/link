package com.ascend.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Миграции написаны вручную и зеркально повторяют схему, которую сгенерировал бы Room:
 * типы, NOT NULL и значения по умолчанию должны совпадать, иначе Room откажется открывать базу.
 */
object Migrations {

    /** 1 → 2: настройки героя, заметки квестов, ссылки в журнале, таланты и сессия фокуса. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `hero` ADD COLUMN `soundEnabled` INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE `hero` ADD COLUMN `reduceMotion` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `quests` ADD COLUMN `note` TEXT")
            db.execSQL("ALTER TABLE `activity_log` ADD COLUMN `refId` TEXT")
            db.execSQL("ALTER TABLE `activity_log` ADD COLUMN `comboStep` INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `talents` (`id` TEXT NOT NULL, `unlockedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `focus_session` (" +
                    "`id` INTEGER NOT NULL, `questId` INTEGER, `title` TEXT NOT NULL, `emoji` TEXT NOT NULL, " +
                    "`startedAt` INTEGER NOT NULL, `targetMinutes` INTEGER NOT NULL, `pausedAt` INTEGER, " +
                    "`pausedTotal` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
