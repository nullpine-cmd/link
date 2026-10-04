package com.ascend.app.data

import com.ascend.app.data.local.AchievementDao
import com.ascend.app.data.local.AchievementEntity
import com.ascend.app.data.local.AttributeXp
import com.ascend.app.data.local.BookDao
import com.ascend.app.data.local.BookEntity
import com.ascend.app.data.local.DayActivity
import com.ascend.app.data.local.HeroDao
import com.ascend.app.data.local.HeroEntity
import com.ascend.app.data.local.LogCounters
import com.ascend.app.data.local.LogDao
import com.ascend.app.data.local.LogEntity
import com.ascend.app.data.local.LogKind
import com.ascend.app.data.local.QuestDao
import com.ascend.app.data.local.QuestEntity
import com.ascend.app.data.local.ShopDao
import com.ascend.app.data.local.ShopItemEntity
import com.ascend.app.data.local.isHabit
import com.ascend.app.data.local.schedule
import com.ascend.app.domain.BookDraft
import com.ascend.app.domain.DayPlan
import com.ascend.app.domain.DayPlanner
import com.ascend.app.domain.HeroState
import com.ascend.app.domain.HeroStatistics
import com.ascend.app.domain.PurchaseResult
import com.ascend.app.domain.QuestDraft
import com.ascend.app.domain.RewardOutcome
import com.ascend.core.Achievement
import com.ascend.core.AchievementRules
import com.ascend.core.Attribute
import com.ascend.core.Difficulty
import com.ascend.core.HeroStats
import com.ascend.core.HeroTitle
import com.ascend.core.Milestones
import com.ascend.core.Motivation
import com.ascend.core.Progression
import com.ascend.core.QuestKind
import com.ascend.core.Rank
import com.ascend.core.Reward
import com.ascend.core.RewardEngine
import com.ascend.core.RewardInput
import com.ascend.core.StreakInfo
import com.ascend.core.Streaks
import com.ascend.core.Units
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Единственная точка входа в игровую механику. Все начисления проходят через журнал
 * в одной транзакции, поэтому опыт, золото и характеристики всегда согласованы,
 * а последнее выполнение можно честно отменить.
 */
class GameRepository(
    private val heroDao: HeroDao,
    private val questDao: QuestDao,
    private val bookDao: BookDao,
    private val logDao: LogDao,
    private val shopDao: ShopDao,
    private val achievementDao: AchievementDao,
    private val tx: TransactionRunner,
    private val time: TimeProvider,
) {

    // region Наблюдение

    val hasHero: Flow<Boolean> = heroDao.observe().map { it != null }

    val heroState: Flow<HeroState?> = combine(
        heroDao.observe(),
        logDao.observeTotalXp(),
        logDao.observeGold(),
        logDao.observeAttributeXp(),
        combine(logDao.observeActiveDays(), time.todayFlow()) { days, today -> days to today },
    ) { hero, totalXp, gold, attributeXp, daysAndToday ->
        hero?.let { buildHeroState(it, totalXp, gold, attributeXp, daysAndToday.first, daysAndToday.second) }
    }

    val quests: Flow<List<QuestEntity>> = questDao.observeAll()

    val books: Flow<List<BookEntity>> = bookDao.observeAll()

    val shopItems: Flow<List<ShopItemEntity>> = shopDao.observeAll()

    val today: Flow<Long> = time.todayFlow()

    val statistics: Flow<HeroStatistics> = combine(
        logDao.observeCounters(),
        bookDao.observeFinishedCount(),
        logDao.observeTotalXp(),
        logDao.observeAttributeXp(),
        combine(logDao.observeActiveDays(), achievementDao.observeAll(), time.todayFlow()) { days, unlocked, today ->
            Triple(days, unlocked, today)
        },
    ) { counters, books, totalXp, attributeXp, extra ->
        val (days, unlocked, today) = extra
        HeroStatistics(
            stats = statsOf(counters, books, totalXp, attributeXp, days, today),
            activeDays = days.size,
            totalXp = totalXp,
            unlocked = unlocked.mapNotNull { entity -> achievementById(entity.id)?.let { it to entity.unlockedAt } }.toMap(),
        )
    }

    fun dayPlan(day: Long): Flow<DayPlan> = combine(
        questDao.observeAll(),
        logDao.observeDay(day),
        time.todayFlow(),
    ) { quests, logs, today -> DayPlanner.build(quests, logs, day, today) }

    fun activity(from: Long, to: Long): Flow<Map<Long, DayActivity>> =
        logDao.observeActivity(from, to).map { list -> list.associateBy { it.day } }

    fun book(id: Long): Flow<BookEntity?> = bookDao.observe(id)

    fun bookLogs(id: Long): Flow<List<LogEntity>> = logDao.observeForBook(id)

    fun bookQuests(id: Long): Flow<List<QuestEntity>> = questDao.observeForBook(id)

    fun recentLogs(limit: Int = 30): Flow<List<LogEntity>> = logDao.observeRecent(limit)

    suspend fun quest(id: Long): QuestEntity? = questDao.get(id)

    // endregion

    // region Герой

    suspend fun createHero(name: String, avatar: String, aura: Int) = tx.transaction {
        heroDao.upsert(HeroEntity(name = name.trim(), avatar = avatar, aura = aura, createdAt = time.now()))
        if (shopDao.count() == 0) seedShop()
    }

    suspend fun updateProfile(name: String, avatar: String, aura: Int) = tx.transaction {
        val hero = heroDao.get() ?: return@transaction
        heroDao.upsert(hero.copy(name = name.trim().ifEmpty { hero.name }, avatar = avatar, aura = aura))
    }

    suspend fun setFocus(text: String) = tx.transaction {
        val hero = heroDao.get() ?: return@transaction
        heroDao.upsert(hero.copy(focusText = text.trim().take(120), focusDay = time.today()))
    }

    suspend fun setReminder(enabled: Boolean, minutes: Int) = tx.transaction {
        val hero = heroDao.get() ?: return@transaction
        heroDao.upsert(hero.copy(reminderEnabled = enabled, reminderMinutes = minutes.coerceIn(0, 24 * 60 - 1)))
    }

    suspend fun resetProgress() = tx.transaction {
        logDao.clear()
        achievementDao.clear()
        questDao.clear()
        bookDao.clear()
        shopDao.clear()
        heroDao.clear()
    }

    // endregion

    // region Квесты

    suspend fun saveQuest(draft: QuestDraft): Long = tx.transaction {
        val target = draft.target?.takeIf { it > 0 }
        val unit = draft.unit?.trim()?.takeIf { target != null && it.isNotEmpty() }
        val isHabit = draft.kind == QuestKind.HABIT
        val existing = if (draft.id != 0L) questDao.get(draft.id) else null
        if (existing == null) {
            val now = time.now()
            questDao.insert(
                QuestEntity(
                    title = draft.title.trim(),
                    emoji = draft.emoji,
                    kind = draft.kind,
                    attribute = draft.attribute,
                    difficulty = draft.difficulty,
                    targetAmount = target,
                    unit = unit,
                    scheduleMask = draft.scheduleMask,
                    dueDay = if (isHabit) null else draft.dueDay ?: time.today(),
                    timeMinutes = draft.timeMinutes,
                    bookId = draft.bookId,
                    createdAt = now,
                    createdDay = time.today(),
                ),
            )
        } else {
            questDao.update(
                existing.copy(
                    title = draft.title.trim(),
                    emoji = draft.emoji,
                    kind = draft.kind,
                    attribute = draft.attribute,
                    difficulty = draft.difficulty,
                    targetAmount = target,
                    unit = unit,
                    scheduleMask = draft.scheduleMask,
                    dueDay = if (isHabit) null else draft.dueDay ?: existing.dueDay ?: time.today(),
                    timeMinutes = draft.timeMinutes,
                    bookId = draft.bookId,
                ),
            )
            existing.id
        }
    }

    suspend fun setArchived(id: Long, archived: Boolean) = tx.transaction {
        val quest = questDao.get(id) ?: return@transaction
        questDao.update(quest.copy(archived = archived))
    }

    suspend fun rescheduleTask(id: Long, day: Long) = tx.transaction {
        val quest = questDao.get(id) ?: return@transaction
        if (quest.isHabit || quest.completedAt != null) return@transaction
        questDao.update(quest.copy(dueDay = day))
    }

    /** Удаление квеста не отнимает заработанный опыт — история остаётся в журнале. */
    suspend fun deleteQuest(id: Long) = tx.transaction { questDao.delete(id) }

    suspend fun completeQuest(questId: Long, amount: Int): RewardOutcome? = tx.transaction {
        val quest = questDao.get(questId) ?: return@transaction null
        val today = time.today()
        val blocked = quest.archived || amount <= 0 ||
            (quest.isHabit && quest.lastCompletedDay == today) ||
            (!quest.isHabit && quest.completedAt != null)
        if (blocked) null else completeInternal(quest, amount, today)
    }

    /** Отмена последнего выполнения (на случай случайного нажатия). */
    suspend fun undo(logId: Long) = tx.transaction {
        val log = logDao.get(logId) ?: return@transaction
        if (log.kind != LogKind.QUEST && log.kind != LogKind.READING) return@transaction
        logDao.delete(logId)
        log.bookId?.let { restoreBook(it, log) }
        log.questId?.let { restoreQuest(it) }
        if (logDao.perfectDayCount(log.day) > 0) {
            val plan = DayPlanner.build(questDao.getAll(), logDao.getDay(log.day), log.day, time.today())
            if (plan.total < Milestones.PERFECT_DAY_MIN_QUESTS || plan.done < plan.total) {
                logDao.deletePerfectDay(log.day)
            }
        }
    }

    // endregion

    // region Библиотека

    suspend fun saveBook(draft: BookDraft): Long = tx.transaction {
        val total = draft.totalPages.coerceAtLeast(1)
        val page = draft.currentPage.coerceIn(0, total)
        val now = time.now()
        val existing = if (draft.id != 0L) bookDao.get(draft.id) else null
        if (existing == null) {
            val id = bookDao.insert(
                BookEntity(
                    title = draft.title.trim(),
                    author = draft.author.trim(),
                    totalPages = total,
                    currentPage = page,
                    palette = draft.palette,
                    createdAt = now,
                    startedAt = if (page > 0) now else null,
                    finishedAt = if (page >= total) now else null,
                ),
            )
            val daily = draft.dailyPages?.takeIf { it > 0 }
            if (daily != null && page < total) {
                questDao.insert(
                    QuestEntity(
                        title = "Читать «${draft.title.trim()}»",
                        emoji = "📖",
                        kind = QuestKind.HABIT,
                        attribute = Attribute.INTELLECT,
                        difficulty = Difficulty.NORMAL,
                        targetAmount = daily,
                        unit = Units.PAGES,
                        bookId = id,
                        createdAt = now,
                        createdDay = time.today(),
                    ),
                )
            }
            id
        } else {
            bookDao.update(
                existing.copy(
                    title = draft.title.trim(),
                    author = draft.author.trim(),
                    totalPages = total,
                    currentPage = page,
                    palette = draft.palette,
                    startedAt = existing.startedAt ?: if (page > 0) now else null,
                    finishedAt = when {
                        page < total -> null
                        else -> existing.finishedAt ?: now
                    },
                ),
            )
            existing.id
        }
    }

    suspend fun deleteBook(id: Long) = tx.transaction {
        questDao.detachBook(id)
        logDao.detachBook(id)
        bookDao.delete(id)
    }

    /**
     * Запись чтения из библиотеки. Если к книге привязана привычка, ещё не выполненная сегодня,
     * чтение засчитывается как её выполнение — с полноценной наградой и серией.
     */
    suspend fun logReading(bookId: Long, pages: Int): RewardOutcome? = tx.transaction {
        val book = bookDao.get(bookId)
        if (book == null || pages <= 0) return@transaction null
        val today = time.today()
        val linked = questDao.getAll().firstOrNull { quest ->
            quest.bookId == bookId && !quest.archived && quest.isHabit &&
                quest.createdDay <= today && quest.schedule.isDue(today) && quest.lastCompletedDay != today
        }
        if (linked != null) return@transaction completeInternal(linked, pages, today)

        val lastActive = logDao.activeDays().firstOrNull()
        val reward = RewardEngine.readingSession(
            pages = pages,
            isFirstCompletionToday = lastActive != today,
            daysSinceLastActive = lastActive?.let { today - it },
        )
        record(
            reward = reward,
            kind = LogKind.READING,
            quest = null,
            book = book,
            title = "Чтение: ${book.title}",
            emoji = "📖",
            attribute = Attribute.INTELLECT,
            unit = Units.PAGES,
            amount = pages,
            target = null,
        )
    }

    // endregion

    // region Лавка наград

    suspend fun saveShopItem(id: Long, title: String, emoji: String, cost: Int): Unit = tx.transaction {
        val existing = if (id != 0L) shopDao.get(id) else null
        if (existing == null) {
            shopDao.insert(ShopItemEntity(title = title.trim(), emoji = emoji, cost = cost.coerceAtLeast(1), createdAt = time.now()))
        } else {
            shopDao.update(existing.copy(title = title.trim(), emoji = emoji, cost = cost.coerceAtLeast(1)))
        }
        Unit
    }

    suspend fun deleteShopItem(id: Long) = tx.transaction { shopDao.delete(id) }

    suspend fun buy(itemId: Long): PurchaseResult = tx.transaction {
        val item = shopDao.get(itemId) ?: return@transaction PurchaseResult.NotFound
        val gold = logDao.gold()
        if (gold < item.cost) return@transaction PurchaseResult.NotEnoughGold(item.cost - gold)
        val today = time.today()
        val now = time.now()
        val hour = time.hour()
        val before = Progression.heroLevel(logDao.totalXp())
        logDao.insert(
            LogEntity(
                kind = LogKind.PURCHASE,
                shopItemId = item.id,
                title = item.title,
                emoji = item.emoji,
                day = today,
                timestamp = now,
                hour = hour,
                gold = -item.cost,
            ),
        )
        shopDao.update(item.copy(timesBought = item.timesBought + 1))
        val achievements = unlockAchievements(today, now, hour)
        PurchaseResult.Success(
            title = item.title,
            emoji = item.emoji,
            cost = item.cost,
            achievements = achievements,
            levelBefore = before,
            levelAfter = Progression.heroLevel(logDao.totalXp()),
        )
    }

    // endregion

    // region Внутренняя механика

    private suspend fun completeInternal(quest: QuestEntity, amount: Int, today: Long): RewardOutcome {
        val lastActive = logDao.activeDays().firstOrNull()
        val target = quest.targetAmount?.takeIf { it > 0 }
        val reward = RewardEngine.calculate(
            RewardInput(
                difficulty = quest.difficulty,
                amount = amount,
                target = target,
                isHabit = quest.isHabit,
                streakBefore = quest.streak,
                lastCompletedDay = quest.lastCompletedDay,
                previousDueDay = if (quest.isHabit) quest.schedule.previousDueDay(today) else null,
                isFirstCompletionToday = lastActive != today,
                daysSinceLastActive = lastActive?.let { today - it },
            ),
        )
        return record(
            reward = reward,
            kind = LogKind.QUEST,
            quest = quest,
            book = quest.bookId?.let { bookDao.get(it) },
            title = quest.title,
            emoji = quest.emoji,
            attribute = quest.attribute,
            unit = quest.unit,
            amount = amount,
            target = target,
        )
    }

    private suspend fun record(
        reward: Reward,
        kind: LogKind,
        quest: QuestEntity?,
        book: BookEntity?,
        title: String,
        emoji: String,
        attribute: Attribute,
        unit: String?,
        amount: Int,
        target: Int?,
    ): RewardOutcome {
        val today = time.today()
        val now = time.now()
        val hour = time.hour()
        val levelBefore = Progression.heroLevel(logDao.totalXp())
        val attributeBefore = Progression.attributeLevel(attributeXpOf(attribute))

        val logId = logDao.insert(
            LogEntity(
                kind = kind,
                questId = quest?.id,
                bookId = book?.id,
                title = title,
                emoji = emoji,
                attribute = attribute,
                unit = unit,
                day = today,
                timestamp = now,
                hour = hour,
                amount = amount,
                xp = reward.totalXp,
                gold = reward.gold,
                hitTarget = reward.hitTarget,
                overachieved = reward.overachieved,
                comeback = reward.comeback,
                bookPageBefore = book?.currentPage,
            ),
        )
        if (quest != null) {
            questDao.update(
                quest.copy(
                    streak = if (quest.isHabit) reward.newStreak else 0,
                    bestStreak = maxOf(quest.bestStreak, reward.newStreak),
                    lastCompletedDay = today,
                    totalCompletions = quest.totalCompletions + 1,
                    completedAt = if (quest.isHabit) null else now,
                ),
            )
        }
        val finishedBook = book?.let { advanceBook(it, amount, now, today, hour) }
        val perfectDay = awardPerfectDay(today, now, hour)
        val achievements = unlockAchievements(today, now, hour)

        return RewardOutcome(
            logId = logId,
            title = title,
            emoji = emoji,
            attribute = attribute,
            xp = reward.totalXp,
            gold = reward.gold,
            baseXp = reward.baseXp,
            bonuses = reward.bonuses,
            amount = amount,
            unit = unit,
            target = target,
            streak = reward.newStreak,
            message = Motivation.encouragement(reward, hasTarget = target != null, seed = (now / 1000).toInt()),
            levelBefore = levelBefore,
            levelAfter = Progression.heroLevel(logDao.totalXp()),
            attributeBefore = attributeBefore,
            attributeAfter = Progression.attributeLevel(attributeXpOf(attribute)),
            achievements = achievements,
            finishedBook = finishedBook?.title,
            perfectDay = perfectDay,
        )
    }

    /** Двигает закладку; возвращает книгу, если она только что дочитана. */
    private suspend fun advanceBook(book: BookEntity, pages: Int, now: Long, today: Long, hour: Int): BookEntity? {
        val page = (book.currentPage + pages).coerceAtMost(book.totalPages)
        val finishes = book.finishedAt == null && page >= book.totalPages
        val updated = book.copy(
            currentPage = page,
            startedAt = book.startedAt ?: now,
            finishedAt = if (finishes) now else book.finishedAt,
        )
        bookDao.update(updated)
        if (!finishes) return null
        logDao.insert(
            LogEntity(
                kind = LogKind.BOOK_FINISHED,
                bookId = book.id,
                title = book.title,
                emoji = "🏆",
                attribute = Attribute.INTELLECT,
                day = today,
                timestamp = now,
                hour = hour,
                xp = Milestones.BOOK_FINISHED_XP,
                gold = Milestones.BOOK_FINISHED_GOLD,
            ),
        )
        return updated
    }

    private suspend fun awardPerfectDay(today: Long, now: Long, hour: Int): Boolean {
        if (logDao.perfectDayCount(today) > 0) return false
        val plan = DayPlanner.build(questDao.getAll(), logDao.getDay(today), today, today)
        if (plan.total < Milestones.PERFECT_DAY_MIN_QUESTS || plan.done < plan.total) return false
        logDao.insert(
            LogEntity(
                kind = LogKind.PERFECT_DAY,
                title = "Идеальный день",
                emoji = "🌟",
                day = today,
                timestamp = now,
                hour = hour,
                xp = Milestones.PERFECT_DAY_XP,
                gold = Milestones.PERFECT_DAY_GOLD,
            ),
        )
        return true
    }

    /** Открывает заслуженные достижения; повторяет проверку, ведь награда может поднять уровень. */
    private suspend fun unlockAchievements(today: Long, now: Long, hour: Int): List<Achievement> {
        val unlocked = mutableListOf<Achievement>()
        repeat(4) {
            val known = achievementDao.ids().mapNotNull(::achievementById).toSet()
            val fresh = AchievementRules.newlyEarned(currentStats(today), known)
            if (fresh.isEmpty()) return unlocked
            for (achievement in fresh) {
                achievementDao.insert(AchievementEntity(achievement.name, now))
                logDao.insert(
                    LogEntity(
                        kind = LogKind.ACHIEVEMENT,
                        title = achievement.title,
                        emoji = "🏅",
                        day = today,
                        timestamp = now,
                        hour = hour,
                        xp = achievement.tier.xp,
                        gold = achievement.tier.gold,
                    ),
                )
                unlocked += achievement
            }
        }
        return unlocked
    }

    private suspend fun restoreQuest(id: Long) {
        val quest = questDao.get(id) ?: return
        val days = logDao.questDays(id)
        val streak = if (quest.isHabit) Streaks.habit(quest.schedule, days, time.today()) else StreakInfo(0, 0)
        questDao.update(
            quest.copy(
                streak = streak.current,
                bestStreak = streak.best,
                lastCompletedDay = days.lastOrNull(),
                totalCompletions = days.size,
                completedAt = null,
            ),
        )
    }

    private suspend fun restoreBook(bookId: Long, log: LogEntity) {
        val book = bookDao.get(bookId) ?: return
        val restored = (log.bookPageBefore ?: (book.currentPage - log.amount)).coerceIn(0, book.totalPages)
        val reopen = book.finishedAt != null && restored < book.totalPages
        bookDao.update(book.copy(currentPage = restored, finishedAt = if (reopen) null else book.finishedAt))
        if (reopen) logDao.deleteBookFinished(bookId)
    }

    private suspend fun attributeXpOf(attribute: Attribute): Long =
        logDao.attributeXp().firstOrNull { it.attribute == attribute }?.xp ?: 0L

    private suspend fun currentStats(today: Long): HeroStats = statsOf(
        counters = logDao.counters(),
        booksFinished = bookDao.finishedCount(),
        totalXp = logDao.totalXp(),
        attributeXp = logDao.attributeXp(),
        activeDays = logDao.activeDays(),
        today = today,
    )

    private fun statsOf(
        counters: LogCounters,
        booksFinished: Long,
        totalXp: Long,
        attributeXp: List<AttributeXp>,
        activeDays: List<Long>,
        today: Long,
    ): HeroStats = HeroStats(
        questsCompleted = counters.quests,
        bestDayStreak = Streaks.daily(activeDays, today).best.toLong(),
        pagesRead = counters.pages,
        booksFinished = booksFinished,
        level = Progression.heroLevel(totalXp).level,
        attributeLevels = Attribute.entries.associateWith { attribute ->
            Progression.attributeLevel(attributeXp.firstOrNull { it.attribute == attribute }?.xp ?: 0L).level
        },
        earlyCompletions = counters.early,
        lateCompletions = counters.late,
        overachievements = counters.overachieved,
        comebacks = counters.comebacks,
        purchases = counters.purchases,
        perfectDays = counters.perfectDays,
    )

    private fun buildHeroState(
        hero: HeroEntity,
        totalXp: Long,
        gold: Long,
        attributeXp: List<AttributeXp>,
        activeDays: List<Long>,
        today: Long,
    ): HeroState {
        val xp = Attribute.entries.associateWith { attribute ->
            attributeXp.firstOrNull { it.attribute == attribute }?.xp ?: 0L
        }
        val level = Progression.heroLevel(totalXp)
        return HeroState(
            name = hero.name,
            avatar = hero.avatar,
            aura = hero.aura,
            level = level,
            rank = Rank.forLevel(level.level),
            gold = gold,
            attributeXp = xp,
            attributes = xp.mapValues { Progression.attributeLevel(it.value) },
            title = HeroTitle.forAttributes(xp),
            streak = Streaks.daily(activeDays, today),
            focus = hero.focusText?.takeIf { hero.focusDay == today && it.isNotBlank() },
            createdAt = hero.createdAt,
            reminderEnabled = hero.reminderEnabled,
            reminderMinutes = hero.reminderMinutes,
        )
    }

    private suspend fun seedShop() {
        val now = time.now()
        listOf(
            Triple("☕", "Любимый кофе", 60),
            Triple("🎬", "Серия любимого сериала", 100),
            Triple("🍰", "Вкусный десерт", 120),
            Triple("🎮", "Час игр без угрызений", 150),
            Triple("🛍️", "Покупка для себя", 500),
        ).forEach { (emoji, title, cost) ->
            shopDao.insert(ShopItemEntity(title = title, emoji = emoji, cost = cost, createdAt = now))
        }
    }

    private fun achievementById(id: String): Achievement? = Achievement.entries.firstOrNull { it.name == id }

    // endregion
}
