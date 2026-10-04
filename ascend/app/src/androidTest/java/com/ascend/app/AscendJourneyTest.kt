package com.ascend.app

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ascend.app.domain.Celebration
import com.ascend.app.domain.QuestDraft
import com.ascend.app.ui.celebration.CELEBRATION_TOAST_TAG
import com.ascend.core.Attribute
import com.ascend.core.Difficulty
import com.ascend.core.QuestKind
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Сквозной сценарий на реальном устройстве: от пробуждения героя до церемонии уровня.
 * Попутно сохраняет скриншоты ключевых экранов.
 */
@RunWith(AndroidJUnit4::class)
class AscendJourneyTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app: AscendApplication get() = ApplicationProvider.getApplicationContext()

    @Before
    fun freshStart() = runBlocking { app.container.repository.resetProgress() }

    private fun waitForText(text: String, substring: Boolean = false, timeout: Long = 15_000) =
        waitFor("текст «$text»", hasText(text, substring = substring), timeout)

    private fun waitForDescription(description: String, timeout: Long = 15_000) =
        waitFor("описание «$description»", hasContentDescription(description), timeout)

    /** Ждёт элемент, а при неудаче перечисляет всё, что видно на экране, — чтобы причина была в логе CI. */
    private fun waitFor(what: String, matcher: SemanticsMatcher, timeout: Long) {
        try {
            compose.waitUntil(timeout) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        } catch (error: ComposeTimeoutException) {
            val visible = compose.onAllNodes(SemanticsMatcher("любой узел") { true })
                .fetchSemanticsNodes()
                .mapNotNull { node ->
                    node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ")
                        ?: node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
                }
                .filter { it.isNotBlank() }
                .distinct()
                .take(80)
            Log.e("AscendTest", "Не дождались $what. На экране: $visible")
            screenshot("failure")
            throw AssertionError("Не дождались $what. На экране: $visible", error)
        }
    }

    private fun tap(text: String) {
        waitForText(text)
        compose.onAllNodesWithText(text).onFirst().performClick()
    }

    private fun tapDescription(description: String) {
        waitForDescription(description)
        compose.onAllNodesWithContentDescription(description).onFirst().performClick()
    }

    private fun toastCount(): Int = compose.onAllNodes(hasTestTag(CELEBRATION_TOAST_TAG)).fetchSemanticsNodes().size

    /**
     * Дожидается тостов празднований и закрывает их касанием, пока экран не будет чист
     * хотя бы секунду: тосты перекрывают верх экрана, где живут кнопки «назад» и «добавить».
     */
    private fun dismissCelebrations() {
        runCatching { compose.waitUntil(4_000) { toastCount() > 0 } }
        var quietSince = System.currentTimeMillis()
        val deadline = quietSince + 25_000
        while (System.currentTimeMillis() < deadline) {
            if (toastCount() > 0) {
                runCatching { compose.onAllNodes(hasTestTag(CELEBRATION_TOAST_TAG)).onFirst().performClick() }
                Thread.sleep(400)
                quietSince = System.currentTimeMillis()
            } else {
                if (System.currentTimeMillis() - quietSince > 1_200) return
                Thread.sleep(200)
            }
        }
    }

    private fun mainList(): SemanticsNodeInteraction = compose.onAllNodes(hasScrollToNodeAction()).onFirst()

    private fun screenshot(name: String) {
        compose.waitForIdle()
        Thread.sleep(700)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        listOfNotNull(app.getExternalFilesDir(null), app.filesDir).forEach { root ->
            val dir = File(root, "screenshots").apply { mkdirs() }
            FileOutputStream(File(dir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test
    fun heroJourney() {
        // Онбординг
        waitForText("Начать путь")
        screenshot("01_intro")
        tap("Начать путь")
        waitForText("Создать героя")
        screenshot("02_principles")
        compose.onNodeWithText("Создать героя").performScrollTo().performClick()
        waitForText("Пробудить героя")
        compose.onNode(hasSetTextAction()).performTextInput("Тестер")
        screenshot("03_create_hero")
        compose.onNodeWithText("Пробудить героя").performScrollTo().performClick()

        // Первый квест из шаблона
        waitForDescription("День")
        waitForText("Тестер", substring = true)
        screenshot("04_today_empty")
        mainList().performScrollToNode(hasText("Медитация"))
        tap("Медитация")
        waitForText("Создать квест")
        screenshot("05_quest_editor")
        compose.onNodeWithText("Создать квест").performScrollTo().performClick()

        // Выполнение с записью прогресса
        waitForDescription("Выполнить")
        compose.onAllNodesWithContentDescription("Выполнить").onFirst().performClick()
        waitForText("Засчитать")
        screenshot("06_log_progress")
        tap("Засчитать")
        waitForText("Отменить")
        screenshot("07_reward")
        dismissCelebrations()

        // Вкладки
        tapDescription("Квесты")
        waitForText("Привычки")
        screenshot("08_quests")

        tapDescription("Книги")
        waitForText("Библиотека")
        tapDescription("Добавить книгу")
        waitForText("Поставить на полку")
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("Мастер и Маргарита")
        fields[1].performTextInput("Михаил Булгаков")
        fields[2].performTextInput("480")
        screenshot("09_book_editor")
        compose.onNodeWithText("Поставить на полку").performScrollTo().performClick()
        waitForText("Хочу прочитать".uppercase())
        screenshot("10_library")
        compose.onAllNodesWithText("Мастер и Маргарита").onFirst().performClick()
        waitForText("Записать чтение")
        tap("Записать чтение")
        waitForText("Засчитать")
        tap("Засчитать")
        waitForText("стр. 20 из 480")
        screenshot("11_book_detail")
        dismissCelebrations()
        tapDescription("Назад")
        waitForText("Библиотека")

        // Герой и достижения
        tapDescription("Герой")
        waitForDescription("Настройки")
        screenshot("12_hero")
        mainList().performScrollToNode(hasText("Достижения"))
        screenshot("13_hero_stats")
        tap("Достижения")
        waitForText("Бронза:", substring = true)
        screenshot("14_achievements")
        tapDescription("Назад")

        // Лавка наград
        waitForText("Достижения")
        mainList().performScrollToNode(hasText("Лавка наград"))
        tap("Лавка наград")
        waitForText("Твоё золото")
        screenshot("15_shop")
        tapDescription("Назад")

        // Настройки
        waitForText("Лавка наград")
        mainList().performScrollToNode(hasContentDescription("Настройки"))
        tapDescription("Настройки")
        waitForText("Начать путь заново")
        screenshot("16_settings")
        tapDescription("Назад")
        waitForDescription("Настройки")

        // Церемония нового уровня
        runBlocking {
            val repository = app.container.repository
            val id = repository.saveQuest(
                QuestDraft(
                    title = "Эпический поход",
                    emoji = "⚔️",
                    kind = QuestKind.TASK,
                    attribute = Attribute.STRENGTH,
                    difficulty = Difficulty.EPIC,
                ),
            )
            val outcome = requireNotNull(repository.completeQuest(id, 1))
            check(outcome.leveledUp) { "Ожидался новый уровень, получено ${outcome.levelAfter}" }
            app.container.celebrations.emit(Celebration.Completed(outcome))
        }
        waitForText("НОВЫЙ УРОВЕНЬ")
        Thread.sleep(1_500)
        screenshot("17_level_up")
        tap("Продолжить путь")

        tapDescription("День")
        waitForText("Прогресс дня")
        mainList().performScrollToIndex(0)
        waitForText("Тестер", substring = true)
        screenshot("18_today")
    }
}
