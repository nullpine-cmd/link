package com.ascend.core

/** Комбо: выполнения подряд с небольшими перерывами усиливают друг друга. */
object Combo {
    const val WINDOW_MILLIS = 30L * 60_000
    const val BASE_CAP = 5
    const val KING_CAP = 8
    const val STEP_RATE = 0.10

    /**
     * Длина цепочки выполнений, в которую вливается новое выполнение в момент [now]:
     * считаем назад от последнего выполнения, пока перерывы не превышают окно.
     */
    fun step(previousTimestamps: Collection<Long>, now: Long, windowMillis: Long = WINDOW_MILLIS): Int {
        var step = 0
        var anchor = now
        for (timestamp in previousTimestamps.sortedDescending()) {
            if (anchor - timestamp > windowMillis || timestamp > anchor) break
            step++
            anchor = timestamp
        }
        return step
    }

    fun cap(talents: Set<Talent>): Int = if (Talent.COMBO_KING in talents) KING_CAP else BASE_CAP
}
