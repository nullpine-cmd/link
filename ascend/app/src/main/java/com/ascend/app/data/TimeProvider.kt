package com.ascend.app.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Абстракция времени — чтобы логика игры тестировалась без реальных часов. */
interface TimeProvider {
    fun now(): Long
    fun zone(): ZoneId

    fun today(): Long = Instant.ofEpochMilli(now()).atZone(zone()).toLocalDate().toEpochDay()
    fun hour(): Int = Instant.ofEpochMilli(now()).atZone(zone()).hour
    fun dayOf(millis: Long): Long = Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate().toEpochDay()
    fun date(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    /** Текущий день, обновляется после полуночи, если приложение открыто. */
    fun todayFlow(): Flow<Long> = flow {
        while (true) {
            emit(today())
            delay(30_000)
        }
    }.distinctUntilChanged()
}

object SystemTimeProvider : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}

/** Обёртка над транзакцией базы: игровые операции атомарны. */
interface TransactionRunner {
    suspend fun <R> transaction(block: suspend () -> R): R
}
