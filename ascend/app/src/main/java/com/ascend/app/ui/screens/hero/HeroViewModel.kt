package com.ascend.app.ui.screens.hero

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.GameRepository
import com.ascend.app.data.TimeProvider
import com.ascend.app.domain.HeroState
import com.ascend.app.domain.HeroStatistics
import com.ascend.app.ui.util.weekdayShort
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class HeroUiState(
    val loading: Boolean = true,
    val hero: HeroState? = null,
    val statistics: HeroStatistics? = null,
    val heatmap: Map<Long, Int> = emptyMap(),
    val weekXp: List<Long> = List(7) { 0L },
    val weekLabels: List<String> = List(7) { "" },
    val today: Long,
)

class HeroViewModel(repository: GameRepository, time: TimeProvider) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HeroUiState> = repository.today.flatMapLatest { today ->
        combine(
            repository.heroState,
            repository.statistics,
            repository.activity(today - HEATMAP_DAYS, today),
        ) { hero, statistics, activity ->
            val days = (today - 6..today).toList()
            HeroUiState(
                loading = false,
                hero = hero,
                statistics = statistics,
                heatmap = activity.mapValues { it.value.count },
                weekXp = days.map { activity[it]?.xp ?: 0L },
                weekLabels = days.map { weekdayShort(it) },
                today = today,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeroUiState(today = time.today()))

    companion object {
        const val HEATMAP_WEEKS = 17
        const val HEATMAP_DAYS = HEATMAP_WEEKS * 7L
    }
}
