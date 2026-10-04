package com.ascend.app

import android.app.Application
import androidx.room.Room
import androidx.room.withTransaction
import com.ascend.app.data.GameRepository
import com.ascend.app.data.SystemTimeProvider
import com.ascend.app.data.TransactionRunner
import com.ascend.app.data.local.AscendDatabase
import com.ascend.app.di.AppContainer
import com.ascend.app.reminder.ReminderNotifications
import com.ascend.app.reminder.WorkReminderScheduler

class AscendApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(this, AscendDatabase::class.java, AscendDatabase.NAME).build()
        val transactions = object : TransactionRunner {
            override suspend fun <R> transaction(block: suspend () -> R): R = database.withTransaction(block)
        }
        val repository = GameRepository(
            heroDao = database.heroDao(),
            questDao = database.questDao(),
            bookDao = database.bookDao(),
            logDao = database.logDao(),
            shopDao = database.shopDao(),
            achievementDao = database.achievementDao(),
            tx = transactions,
            time = SystemTimeProvider,
        )
        container = AppContainer(
            repository = repository,
            time = SystemTimeProvider,
            reminders = WorkReminderScheduler(this),
        )
        ReminderNotifications.createChannel(this)
    }
}
