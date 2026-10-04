package com.ascend.core

/** Класс героя складывается сам — из того, чем человек на самом деле занимается. */
enum class HeroTitle(val title: String, val description: String) {
    AWAKENED("Пробудившийся", "Путь только начинается — каждый квест формирует твой класс"),
    WARRIOR("Воин", "Твоя сила растёт быстрее остальных"),
    SAGE("Мудрец", "Знания — твоё главное оружие"),
    DRUID("Друид", "Ты бережёшь тело и восстанавливаешь силы"),
    MONK("Монах", "Спокойствие и воля ведут тебя"),
    BARD("Бард", "Люди тянутся к тебе"),
    ARTISAN("Творец", "Ты создаёшь и совершенствуешь мастерство"),
    HARMONY("Хранитель равновесия", "Все стороны жизни развиваются в гармонии"),
    ;

    companion object {
        const val AWAKENED_THRESHOLD = 60L
        private const val HARMONY_SPREAD = 0.25

        fun of(attribute: Attribute): HeroTitle = when (attribute) {
            Attribute.STRENGTH -> WARRIOR
            Attribute.INTELLECT -> SAGE
            Attribute.VITALITY -> DRUID
            Attribute.SPIRIT -> MONK
            Attribute.CHARISMA -> BARD
            Attribute.MASTERY -> ARTISAN
        }

        fun forAttributes(xp: Map<Attribute, Long>): HeroTitle {
            val values = Attribute.entries.map { xp[it] ?: 0L }
            if (values.sum() < AWAKENED_THRESHOLD) return AWAKENED
            val max = values.max()
            val min = values.min()
            if (min > 0 && (max - min) <= max * HARMONY_SPREAD) return HARMONY
            val dominant = Attribute.entries.maxBy { xp[it] ?: 0L }
            return of(dominant)
        }
    }
}
