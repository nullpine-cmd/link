package com.ascend.core

/** Босс недели: воплощение того, что мешает расти. Каждый заработанный опыт — удар по нему. */
data class Boss(
    val weekStart: Long,
    val emoji: String,
    val name: String,
    val taunt: String,
    val hp: Int,
    val rewardXp: Int,
    val rewardGold: Int,
) {
    val id: String get() = "W:$weekStart"
}

data class BossFight(val boss: Boss, val damage: Long) {
    val hpLeft: Int get() = (boss.hp - damage).coerceIn(0, boss.hp.toLong()).toInt()
    val fraction: Float get() = (hpLeft.toFloat() / boss.hp).coerceIn(0f, 1f)
    val defeated: Boolean get() = damage >= boss.hp
}

object WeeklyBoss {
    private data class Monster(val emoji: String, val name: String, val taunt: String)

    private val monsters = listOf(
        Monster("🐉", "Дракон Прокрастинации", "«Начнёшь завтра… как всегда?»"),
        Monster("🐍", "Гидра Отговорок", "«Отрубишь одну причину — вырастут две»"),
        Monster("🗿", "Голем Лени", "«Зачем вставать, если можно лежать?»"),
        Monster("👻", "Призрак Сомнений", "«У тебя всё равно не получится»"),
        Monster("🧜", "Сирена Соцсетей", "«Ещё пять минуточек… ещё одно видео…»"),
        Monster("🧌", "Тролль Усталости", "«Ты заслужил отдых. Вечный»"),
        Monster("🦁", "Химера Хаоса", "«Планы? Какие планы?»"),
        Monster("💀", "Лич Выгорания", "«Сгорай ярко — и никогда больше»"),
        Monster("🌫️", "Туман Рассеянности", "«О чём ты вообще думал?»"),
        Monster("🐙", "Кракен Дедлайнов", "«Всё и сразу — иначе ничего»"),
    )

    fun forWeek(day: Long, level: Int): Boss {
        val start = Challenges.weekStart(day)
        val monster = monsters[Math.floorMod(start / 7, monsters.size.toLong()).toInt()]
        val safeLevel = level.coerceAtLeast(1)
        return Boss(
            weekStart = start,
            emoji = monster.emoji,
            name = monster.name,
            taunt = monster.taunt,
            hp = ((240 + 45 * safeLevel + 5) / 10) * 10,
            rewardXp = 80 + 6 * safeLevel,
            rewardGold = 50 + 4 * safeLevel,
        )
    }

    fun fight(boss: Boss, weekXp: Long): BossFight = BossFight(boss, weekXp.coerceAtLeast(0))
}
