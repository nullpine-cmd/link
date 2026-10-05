package com.ascend.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ascend.app.data.local.AscendDatabase
import com.ascend.app.data.local.LogKind
import com.ascend.app.data.local.Migrations
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Проверяет миграцию с первой версии: база создаётся «как её создал бы Room 1.0» сырым SQL,
 * затем открывается актуальной схемой. Room сам сверяет результат миграции с ожидаемой схемой
 * и падает, если хоть одна колонка, индекс или значение по умолчанию не совпадают.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val name = "migration-test.db"

    @Before
    fun createVersionOne() {
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `hero` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `avatar` TEXT NOT NULL, " +
                    "`aura` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `focusText` TEXT, `focusDay` INTEGER, " +
                    "`reminderEnabled` INTEGER NOT NULL, `reminderMinutes` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `quests` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                    "`emoji` TEXT NOT NULL, `kind` TEXT NOT NULL, `attribute` TEXT NOT NULL, `difficulty` TEXT NOT NULL, " +
                    "`targetAmount` INTEGER, `unit` TEXT, `scheduleMask` INTEGER NOT NULL, `dueDay` INTEGER, `timeMinutes` INTEGER, " +
                    "`bookId` INTEGER, `createdAt` INTEGER NOT NULL, `createdDay` INTEGER NOT NULL, `archived` INTEGER NOT NULL, " +
                    "`completedAt` INTEGER, `streak` INTEGER NOT NULL, `bestStreak` INTEGER NOT NULL, `lastCompletedDay` INTEGER, " +
                    "`totalCompletions` INTEGER NOT NULL)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_quests_bookId` ON `quests` (`bookId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `books` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                    "`author` TEXT NOT NULL, `totalPages` INTEGER NOT NULL, `currentPage` INTEGER NOT NULL, `palette` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, `startedAt` INTEGER, `finishedAt` INTEGER)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `activity_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `kind` TEXT NOT NULL, " +
                    "`questId` INTEGER, `bookId` INTEGER, `shopItemId` INTEGER, `title` TEXT NOT NULL, `emoji` TEXT NOT NULL, " +
                    "`attribute` TEXT, `unit` TEXT, `day` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `hour` INTEGER NOT NULL, " +
                    "`amount` INTEGER NOT NULL, `xp` INTEGER NOT NULL, `gold` INTEGER NOT NULL, `hitTarget` INTEGER NOT NULL, " +
                    "`overachieved` INTEGER NOT NULL, `comeback` INTEGER NOT NULL, `bookPageBefore` INTEGER)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_log_day` ON `activity_log` (`day`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_log_questId` ON `activity_log` (`questId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_log_bookId` ON `activity_log` (`bookId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `shop_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                    "`emoji` TEXT NOT NULL, `cost` INTEGER NOT NULL, `timesBought` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
            )
            db.execSQL("CREATE TABLE IF NOT EXISTS `achievements` (`id` TEXT NOT NULL, `unlockedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")

            db.execSQL(
                "INSERT INTO hero (id, name, avatar, aura, createdAt, reminderEnabled, reminderMinutes) " +
                    "VALUES (1, 'Ветеран', '🦊', 2, 1000, 1, 1230)",
            )
            db.execSQL(
                "INSERT INTO quests (title, emoji, kind, attribute, difficulty, targetAmount, unit, scheduleMask, createdAt, createdDay, " +
                    "archived, streak, bestStreak, totalCompletions) VALUES ('Читать', '📖', 'HABIT', 'INTELLECT', 'NORMAL', 20, 'стр.', 127, 1000, 20000, 0, 3, 5, 9)",
            )
            db.execSQL(
                "INSERT INTO activity_log (kind, questId, title, emoji, attribute, unit, day, timestamp, hour, amount, xp, gold, hitTarget, overachieved, comeback) " +
                    "VALUES ('QUEST', 1, 'Читать', '📖', 'INTELLECT', 'стр.', 20000, 2000, 9, 20, 30, 12, 1, 0, 0)",
            )
            db.version = 1
        }
    }

    @After
    fun cleanup() {
        context.deleteDatabase(name)
    }

    @Test
    fun migratesFromVersionOneKeepingData() = runBlocking {
        val database = Room.databaseBuilder(context, AscendDatabase::class.java, name)
            .addMigrations(*Migrations.ALL)
            .build()
        try {
            val hero = requireNotNull(database.heroDao().get())
            assertEquals("Ветеран", hero.name)
            assertEquals(1230, hero.reminderMinutes)
            assertTrue("новая настройка должна быть включена по умолчанию", hero.soundEnabled)
            assertTrue(!hero.reduceMotion)

            val quest = database.questDao().getAll().single()
            assertEquals("Читать", quest.title)
            assertEquals(3, quest.streak)
            assertNull(quest.note)

            val log = database.logDao().getAll().single()
            assertEquals(LogKind.QUEST, log.kind)
            assertEquals(30, log.xp)
            assertNull(log.refId)
            assertEquals(0, log.comboStep)

            assertTrue(database.talentDao().getAll().isEmpty())
            assertNull(database.focusDao().observe().first())
        } finally {
            database.close()
        }
    }
}
