package com.ascend.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.GameRepository
import com.ascend.app.data.TimeProvider
import com.ascend.app.data.local.BookEntity
import com.ascend.app.data.local.LogEntity
import com.ascend.app.data.local.QuestEntity
import com.ascend.app.data.local.isFinished
import com.ascend.app.data.local.pagesLeft
import com.ascend.app.di.CelebrationBus
import com.ascend.app.domain.BookDraft
import com.ascend.app.domain.Celebration
import com.ascend.app.domain.QuestDraft
import com.ascend.core.Attribute
import com.ascend.core.Difficulty
import com.ascend.core.QuestKind
import com.ascend.core.Units
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val loading: Boolean = true,
    val reading: List<BookEntity> = emptyList(),
    val planned: List<BookEntity> = emptyList(),
    val finished: List<BookEntity> = emptyList(),
    val pagesRead: Long = 0,
) {
    val isEmpty: Boolean get() = reading.isEmpty() && planned.isEmpty() && finished.isEmpty()
}

class LibraryViewModel(repository: GameRepository) : ViewModel() {
    val state: StateFlow<LibraryUiState> = combine(repository.books, repository.statistics) { books, statistics ->
        LibraryUiState(
            loading = false,
            reading = books.filter { !it.isFinished && it.currentPage > 0 },
            planned = books.filter { !it.isFinished && it.currentPage == 0 },
            finished = books.filter { it.isFinished }.sortedByDescending { it.finishedAt },
            pagesRead = statistics.stats.pagesRead,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())
}

/** Сохранение книги — общее для шторки редактора. */
class BookEditorViewModel(private val repository: GameRepository) : ViewModel() {
    fun save(draft: BookDraft) {
        viewModelScope.launch { repository.saveBook(draft) }
    }
}

data class BookDetailUiState(
    val loading: Boolean = true,
    val book: BookEntity? = null,
    val logs: List<LogEntity> = emptyList(),
    val quests: List<QuestEntity> = emptyList(),
    val today: Long,
)

class BookDetailViewModel(
    private val bookId: Long,
    private val repository: GameRepository,
    private val celebrations: CelebrationBus,
    time: TimeProvider,
) : ViewModel() {

    val state: StateFlow<BookDetailUiState> = combine(
        repository.book(bookId),
        repository.bookLogs(bookId),
        repository.bookQuests(bookId),
        repository.today,
    ) { book, logs, quests, today ->
        BookDetailUiState(loading = false, book = book, logs = logs, quests = quests, today = today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookDetailUiState(today = time.today()))

    fun logReading(pages: Int) {
        viewModelScope.launch {
            repository.logReading(bookId, pages)?.let { celebrations.emit(Celebration.Completed(it)) }
        }
    }

    /** «Дочитал!» — засчитываем оставшиеся страницы как чтение. */
    fun finish() {
        val book = state.value.book ?: return
        if (book.pagesLeft > 0) logReading(book.pagesLeft)
    }

    fun createReadingQuest(pages: Int) {
        val book = state.value.book ?: return
        viewModelScope.launch {
            repository.saveQuest(
                QuestDraft(
                    title = "Читать «${book.title}»",
                    emoji = "📖",
                    kind = QuestKind.HABIT,
                    attribute = Attribute.INTELLECT,
                    difficulty = Difficulty.NORMAL,
                    target = pages,
                    unit = Units.PAGES,
                    bookId = book.id,
                ),
            )
        }
    }

    fun delete() {
        viewModelScope.launch { repository.deleteBook(bookId) }
    }
}
