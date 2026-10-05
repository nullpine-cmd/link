package com.ascend.app.data

import com.ascend.app.data.local.AchievementEntity
import com.ascend.app.data.local.BookEntity
import com.ascend.app.data.local.HeroEntity
import com.ascend.app.data.local.LogEntity
import com.ascend.app.data.local.LogKind
import com.ascend.app.data.local.QuestEntity
import com.ascend.app.data.local.ShopItemEntity
import com.ascend.app.data.local.TalentEntity
import com.ascend.core.Attribute
import com.ascend.core.Difficulty
import com.ascend.core.QuestKind
import org.json.JSONArray
import org.json.JSONObject

/** Снимок всех данных приложения — для экспорта и восстановления. */
data class BackupSnapshot(
    val exportedAt: Long,
    val hero: HeroEntity?,
    val quests: List<QuestEntity>,
    val books: List<BookEntity>,
    val logs: List<LogEntity>,
    val shopItems: List<ShopItemEntity>,
    val achievements: List<AchievementEntity>,
    val talents: List<TalentEntity>,
) {
    val summary: String
        get() = "${quests.size} квестов · ${books.size} книг · ${logs.size} записей журнала"
}

/**
 * Кодек резервной копии. Формат — простой JSON с явными полями, чтобы его можно было прочитать
 * глазами и восстановить даже из другой версии приложения.
 */
object BackupCodec {
    const val FORMAT = "ascend-backup"
    const val VERSION = 1

    fun encode(snapshot: BackupSnapshot): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("exportedAt", snapshot.exportedAt)
        snapshot.hero?.let { hero ->
            root.put(
                "hero",
                JSONObject()
                    .put("name", hero.name)
                    .put("avatar", hero.avatar)
                    .put("aura", hero.aura)
                    .put("createdAt", hero.createdAt)
                    .putOpt("focusText", hero.focusText)
                    .putOpt("focusDay", hero.focusDay)
                    .put("reminderEnabled", hero.reminderEnabled)
                    .put("reminderMinutes", hero.reminderMinutes)
                    .put("soundEnabled", hero.soundEnabled)
                    .put("reduceMotion", hero.reduceMotion),
            )
        }
        root.put(
            "quests",
            JSONArray().apply {
                snapshot.quests.forEach { q ->
                    put(
                        JSONObject()
                            .put("id", q.id)
                            .put("title", q.title)
                            .put("emoji", q.emoji)
                            .put("kind", q.kind.name)
                            .put("attribute", q.attribute.name)
                            .put("difficulty", q.difficulty.name)
                            .putOpt("targetAmount", q.targetAmount)
                            .putOpt("unit", q.unit)
                            .put("scheduleMask", q.scheduleMask)
                            .putOpt("dueDay", q.dueDay)
                            .putOpt("timeMinutes", q.timeMinutes)
                            .putOpt("bookId", q.bookId)
                            .put("createdAt", q.createdAt)
                            .put("createdDay", q.createdDay)
                            .put("archived", q.archived)
                            .putOpt("completedAt", q.completedAt)
                            .put("streak", q.streak)
                            .put("bestStreak", q.bestStreak)
                            .putOpt("lastCompletedDay", q.lastCompletedDay)
                            .put("totalCompletions", q.totalCompletions)
                            .putOpt("note", q.note),
                    )
                }
            },
        )
        root.put(
            "books",
            JSONArray().apply {
                snapshot.books.forEach { b ->
                    put(
                        JSONObject()
                            .put("id", b.id)
                            .put("title", b.title)
                            .put("author", b.author)
                            .put("totalPages", b.totalPages)
                            .put("currentPage", b.currentPage)
                            .put("palette", b.palette)
                            .put("createdAt", b.createdAt)
                            .putOpt("startedAt", b.startedAt)
                            .putOpt("finishedAt", b.finishedAt),
                    )
                }
            },
        )
        root.put(
            "logs",
            JSONArray().apply {
                snapshot.logs.forEach { l ->
                    put(
                        JSONObject()
                            .put("id", l.id)
                            .put("kind", l.kind.name)
                            .putOpt("questId", l.questId)
                            .putOpt("bookId", l.bookId)
                            .putOpt("shopItemId", l.shopItemId)
                            .put("title", l.title)
                            .put("emoji", l.emoji)
                            .putOpt("attribute", l.attribute?.name)
                            .putOpt("unit", l.unit)
                            .put("day", l.day)
                            .put("timestamp", l.timestamp)
                            .put("hour", l.hour)
                            .put("amount", l.amount)
                            .put("xp", l.xp)
                            .put("gold", l.gold)
                            .put("hitTarget", l.hitTarget)
                            .put("overachieved", l.overachieved)
                            .put("comeback", l.comeback)
                            .putOpt("bookPageBefore", l.bookPageBefore)
                            .putOpt("refId", l.refId)
                            .put("comboStep", l.comboStep),
                    )
                }
            },
        )
        root.put(
            "shopItems",
            JSONArray().apply {
                snapshot.shopItems.forEach { s ->
                    put(
                        JSONObject()
                            .put("id", s.id)
                            .put("title", s.title)
                            .put("emoji", s.emoji)
                            .put("cost", s.cost)
                            .put("timesBought", s.timesBought)
                            .put("createdAt", s.createdAt),
                    )
                }
            },
        )
        root.put(
            "achievements",
            JSONArray().apply {
                snapshot.achievements.forEach { put(JSONObject().put("id", it.id).put("unlockedAt", it.unlockedAt)) }
            },
        )
        root.put(
            "talents",
            JSONArray().apply {
                snapshot.talents.forEach { put(JSONObject().put("id", it.id).put("unlockedAt", it.unlockedAt)) }
            },
        )
        return root.toString(2)
    }

    /** Разбирает копию; бросает [IllegalArgumentException], если это не копия Ascend. */
    fun decode(json: String): BackupSnapshot {
        val root = try {
            JSONObject(json)
        } catch (error: Exception) {
            throw IllegalArgumentException("Файл не является резервной копией Ascend", error)
        }
        require(root.optString("format") == FORMAT) { "Файл не является резервной копией Ascend" }
        val hero = root.optJSONObject("hero")?.let { h ->
            HeroEntity(
                name = h.getString("name"),
                avatar = h.optString("avatar", "🦊"),
                aura = h.optInt("aura", 0),
                createdAt = h.optLong("createdAt", 0L),
                focusText = h.optStringOrNull("focusText"),
                focusDay = h.optLongOrNull("focusDay"),
                reminderEnabled = h.optBoolean("reminderEnabled", false),
                reminderMinutes = h.optInt("reminderMinutes", 20 * 60),
                soundEnabled = h.optBoolean("soundEnabled", true),
                reduceMotion = h.optBoolean("reduceMotion", false),
            )
        }
        val quests = root.optJSONArray("quests").objects().map { q ->
            QuestEntity(
                id = q.getLong("id"),
                title = q.getString("title"),
                emoji = q.optString("emoji", "🎯"),
                kind = enumOr(q.optString("kind"), QuestKind.HABIT),
                attribute = enumOr(q.optString("attribute"), Attribute.SPIRIT),
                difficulty = enumOr(q.optString("difficulty"), Difficulty.NORMAL),
                targetAmount = q.optIntOrNull("targetAmount"),
                unit = q.optStringOrNull("unit"),
                scheduleMask = q.optInt("scheduleMask", 0b111_1111),
                dueDay = q.optLongOrNull("dueDay"),
                timeMinutes = q.optIntOrNull("timeMinutes"),
                bookId = q.optLongOrNull("bookId"),
                createdAt = q.optLong("createdAt", 0L),
                createdDay = q.optLong("createdDay", 0L),
                archived = q.optBoolean("archived", false),
                completedAt = q.optLongOrNull("completedAt"),
                streak = q.optInt("streak", 0),
                bestStreak = q.optInt("bestStreak", 0),
                lastCompletedDay = q.optLongOrNull("lastCompletedDay"),
                totalCompletions = q.optInt("totalCompletions", 0),
                note = q.optStringOrNull("note"),
            )
        }
        val books = root.optJSONArray("books").objects().map { b ->
            BookEntity(
                id = b.getLong("id"),
                title = b.getString("title"),
                author = b.optString("author", ""),
                totalPages = b.optInt("totalPages", 1),
                currentPage = b.optInt("currentPage", 0),
                palette = b.optInt("palette", 0),
                createdAt = b.optLong("createdAt", 0L),
                startedAt = b.optLongOrNull("startedAt"),
                finishedAt = b.optLongOrNull("finishedAt"),
            )
        }
        val logs = root.optJSONArray("logs").objects().mapNotNull { l ->
            val kind = LogKind.entries.firstOrNull { it.name == l.optString("kind") } ?: return@mapNotNull null
            LogEntity(
                id = l.getLong("id"),
                kind = kind,
                questId = l.optLongOrNull("questId"),
                bookId = l.optLongOrNull("bookId"),
                shopItemId = l.optLongOrNull("shopItemId"),
                title = l.optString("title", ""),
                emoji = l.optString("emoji", ""),
                attribute = l.optStringOrNull("attribute")?.let { name -> Attribute.entries.firstOrNull { it.name == name } },
                unit = l.optStringOrNull("unit"),
                day = l.getLong("day"),
                timestamp = l.optLong("timestamp", 0L),
                hour = l.optInt("hour", 12),
                amount = l.optInt("amount", 0),
                xp = l.optInt("xp", 0),
                gold = l.optInt("gold", 0),
                hitTarget = l.optBoolean("hitTarget", false),
                overachieved = l.optBoolean("overachieved", false),
                comeback = l.optBoolean("comeback", false),
                bookPageBefore = l.optIntOrNull("bookPageBefore"),
                refId = l.optStringOrNull("refId"),
                comboStep = l.optInt("comboStep", 0),
            )
        }
        val shopItems = root.optJSONArray("shopItems").objects().map { s ->
            ShopItemEntity(
                id = s.getLong("id"),
                title = s.getString("title"),
                emoji = s.optString("emoji", "🎁"),
                cost = s.optInt("cost", 1),
                timesBought = s.optInt("timesBought", 0),
                createdAt = s.optLong("createdAt", 0L),
            )
        }
        val achievements = root.optJSONArray("achievements").objects().map { AchievementEntity(it.getString("id"), it.optLong("unlockedAt", 0L)) }
        val talents = root.optJSONArray("talents").objects().map { TalentEntity(it.getString("id"), it.optLong("unlockedAt", 0L)) }
        return BackupSnapshot(
            exportedAt = root.optLong("exportedAt", 0L),
            hero = hero,
            quests = quests,
            books = books,
            logs = logs,
            shopItems = shopItems,
            achievements = achievements,
            talents = talents,
        )
    }

    private fun JSONArray?.objects(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optJSONObject(it) }
    }

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key) || !has(key)) null else getString(key)
    private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key) || !has(key)) null else getLong(key)
    private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key) || !has(key)) null else getInt(key)

    private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback
}
