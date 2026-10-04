package com.ascend.app.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.GameRepository
import com.ascend.app.data.TimeProvider
import com.ascend.app.data.local.BookEntity
import com.ascend.app.data.local.DayActivity
import com.ascend.app.di.CelebrationBus
import com.ascend.app.domain.Celebration
import com.ascend.app.domain.DayPlan
import com.ascend.app.domain.HeroState
import com.ascend.core.Motivation
import com.ascend.core.Quote
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val loading: Boolean = true,
    val hero: HeroState? = null,
    val plan: DayPlan? = null,
    val selectedDay: Long,
    val today: Long,
    val activity: Map<Long, DayActivity> = emptyMap(),
    val books: Map<Long, BookEntity> = emptyMap(),
    val quote: Quote,
    val hour: Int,
)

class TodayViewModel(
    private val repository: GameRepository,
    private val celebrations: CelebrationBus,
    private val time: TimeProvider,
) : ViewModel() {

    /** null — следовать за сегодняшним днём (переключится после полуночи). */
    private val selected = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TodayUiState> = combine(repository.today, selected) { today, day -> today to (day ?: today) }
        .flatMapLatest { (today, day) ->
            combine(
                repository.heroState,
                repository.dayPlan(day),
                repository.activity(today - WEEK_STRIP_PAST, today + WEEK_STRIP_FUTURE),
                repository.books,
            ) { hero, plan, activity, books ->
                TodayUiState(
                    loading = false,
                    hero = hero,
                    plan = plan,
                    selectedDay = day,
                    today = today,
                    activity = activity,
                    books = books.associateBy { it.id },
                    quote = Motivation.quoteOfDay(today),
                    hour = time.hour(),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(
                selectedDay = time.today(),
                today = time.today(),
                quote = Motivation.quoteOfDay(time.today()),
                hour = time.hour(),
            ),
        )

    fun selectDay(day: Long) {
        selected.value = if (day == time.today()) null else day
    }

    fun complete(questId: Long, amount: Int) {
        viewModelScope.launch {
            repository.completeQuest(questId, amount)?.let { celebrations.emit(Celebration.Completed(it)) }
        }
    }

    fun setFocus(text: String) {
        viewModelScope.launch { repository.setFocus(text) }
    }

    fun postpone(questId: Long) {
        viewModelScope.launch { repository.rescheduleTask(questId, time.today() + 1) }
    }

    companion object {
        const val WEEK_STRIP_PAST = 21L
        const val WEEK_STRIP_FUTURE = 14L
    }
}
