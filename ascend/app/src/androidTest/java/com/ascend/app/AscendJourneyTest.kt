package com.ascend.app

import android.graphics.Bitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ascend.app.domain.Celebration
import com.ascend.app.domain.QuestDraft
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

    private fun waitForText(text: String, substring: Boolean = false, timeout: Long = 15_000) {
        compose.waitUntil(timeout) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForDescription(description: String, timeout: Long = 15_000) {
        compose.waitUntil(timeout) {
            compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
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
        compose.onNodeWithText("Начать путь").performClick()
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
        compose.onNodeWithText("Медитация").performClick()
        waitForText("Создать квест")
        screenshot("05_quest_editor")
        compose.onNodeWithText("Создать квест").performScrollTo().performClick()

        // Выполнение с записью прогресса
        waitForDescription("Выполнить")
        compose.onAllNodesWithContentDescription("Выполнить").onFirst().performClick()
        waitForText("Засчитать")
        screenshot("06_log_progress")
        compose.onNodeWithText("Засчитать").performClick()
        waitForText("Отменить")
        screenshot("07_reward")

        // Вкладки
        compose.onNodeWithContentDescription("Квесты").performClick()
        waitForText("Привычки")
        screenshot("08_quests")

        compose.onNodeWithContentDescription("Книги").performClick()
        waitForText("Библиотека")
        compose.onNodeWithContentDescription("Добавить книгу").performClick()
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
        compose.onNodeWithText("Записать чтение").performClick()
        waitForText("Засчитать")
        compose.onNodeWithText("Засчитать").performClick()
        waitForText("стр. 20 из 480")
        screenshot("11_book_detail")
        compose.onNodeWithContentDescription("Назад").performClick()

        // Герой и достижения
        compose.onNodeWithContentDescription("Герой").performClick()
        waitForDescription("Настройки")
        screenshot("12_hero")
        mainList().performScrollToNode(hasText("Достижения"))
        screenshot("13_hero_stats")
        compose.onNodeWithText("Достижения").performClick()
        waitForText("Бронза:", substring = true)
        screenshot("14_achievements")
        compose.onNodeWithContentDescription("Назад").performClick()

        // Лавка наград
        waitForText("Достижения")
        mainList().performScrollToNode(hasText("Лавка наград"))
        compose.onNodeWithText("Лавка наград").performClick()
        waitForText("Твоё золото")
        screenshot("15_shop")
        compose.onNodeWithContentDescription("Назад").performClick()

        // Настройки
        waitForText("Лавка наград")
        mainList().performScrollToNode(hasContentDescription("Настройки"))
        compose.onNodeWithContentDescription("Настройки").performClick()
        waitForText("Начать путь заново")
        screenshot("16_settings")
        compose.onNodeWithContentDescription("Назад").performClick()
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
        compose.onNodeWithText("Продолжить путь").performClick()

        compose.onNodeWithContentDescription("День").performClick()
        waitForText("Тестер", substring = true)
        screenshot("18_today")
    }
}
