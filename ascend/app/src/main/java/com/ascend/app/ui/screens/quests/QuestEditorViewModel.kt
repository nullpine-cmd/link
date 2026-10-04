package com.ascend.app.ui.screens.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.GameRepository
import com.ascend.app.data.TimeProvider
import com.ascend.app.data.local.BookEntity
import com.ascend.app.data.local.QuestEntity
import com.ascend.app.domain.QuestDraft
import com.ascend.core.Attribute
import com.ascend.core.Difficulty
import com.ascend.core.QuestKind
import com.ascend.core.QuestTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Откуда открыт редактор квеста. */
sealed interface QuestEditorRequest {
    data class New(
        val dueDay: Long? = null,
        val template: QuestTemplate? = null,
        val kind: QuestKind = QuestKind.HABIT,
        val bookId: Long? = null,
    ) : QuestEditorRequest

    data class Edit(val quest: QuestEntity) : QuestEditorRequest
}

class QuestEditorViewModel(
    private val repository: GameRepository,
    private val time: TimeProvider,
) : ViewModel() {

    val books: StateFlow<List<BookEntity>> = repository.books
        .map { list -> list.filter { it.finishedAt == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val today: Long get() = time.today()

    fun save(draft: QuestDraft) {
        viewModelScope.launch { repository.saveQuest(draft) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteQuest(id) }
    }

    fun setArchived(id: Long, archived: Boolean) {
        viewModelScope.launch { repository.setArchived(id, archived) }
    }

    fun initialDraft(request: QuestEditorRequest): QuestDraft = when (request) {
        is QuestEditorRequest.Edit -> with(request.quest) {
            QuestDraft(
                id = id,
                title = title,
                emoji = emoji,
                kind = kind,
                attribute = attribute,
                difficulty = difficulty,
                target = targetAmount,
                unit = unit,
                scheduleMask = scheduleMask,
                dueDay = dueDay,
                timeMinutes = timeMinutes,
                bookId = bookId,
            )
        }
        is QuestEditorRequest.New -> {
            val template = request.template
            QuestDraft(
                title = template?.title.orEmpty(),
                emoji = template?.emoji ?: "🎯",
                kind = if (request.dueDay != null && request.dueDay != today) QuestKind.TASK else request.kind,
                attribute = template?.attribute ?: Attribute.INTELLECT,
                difficulty = template?.difficulty ?: Difficulty.NORMAL,
                target = template?.target,
                unit = template?.unit,
                scheduleMask = template?.schedule?.mask ?: com.ascend.core.WeekSchedule.EVERY_DAY.mask,
                dueDay = request.dueDay ?: today,
                bookId = request.bookId,
            )
        }
    }
}
