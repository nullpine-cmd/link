package com.ascend.app.ui.screens.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.GameRepository
import com.ascend.app.domain.HeroStatistics
import com.ascend.app.di.appViewModel
import com.ascend.app.ui.components.AchievementMedal
import com.ascend.app.ui.components.DetailTopBar
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.GlowProgressBar
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.components.reveal
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.colors
import com.ascend.core.Achievement
import com.ascend.core.AchievementRules
import com.ascend.core.Tier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AchievementsViewModel(repository: GameRepository) : ViewModel() {
    val statistics: StateFlow<HeroStatistics?> = repository.statistics
        .map<HeroStatistics, HeroStatistics?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun AchievementsRoute(onBack: () -> Unit) {
    val viewModel = appViewModel { AchievementsViewModel(it.repository) }
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    AchievementsScreen(statistics, onBack)
}

@Composable
fun AchievementsScreen(statistics: HeroStatistics?, onBack: () -> Unit) {
    val unlocked = statistics?.unlocked.orEmpty()
    val ordered = Achievement.entries.sortedWith(
        compareByDescending<Achievement> { it in unlocked }
            .thenByDescending { statistics?.let { s -> AchievementRules.progress(it, s.stats) } ?: 0f }
            .thenBy { it.tier.ordinal },
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            DetailTopBar("Достижения", onBack, Modifier.statusBarsPadding())
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            GlassCard(Modifier.fillMaxWidth(), accent = AscendColors.Gold) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${unlocked.size}",
                        style = MaterialTheme.typography.displayMedium,
                        color = AscendColors.Gold,
                    )
                    Text(
                        " / ${Achievement.entries.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AscendColors.TextSecondary,
                    )
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Tier.entries.forEach { tier ->
                            val count = unlocked.keys.count { it.tier == tier }
                            val total = Achievement.entries.count { it.tier == tier }
                            Text("${tier.title}: $count/$total", style = MaterialTheme.typography.labelMedium, color = tier.colors.first())
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                GlowProgressBar(
                    unlocked.size.toFloat() / Achievement.entries.size,
                    colors = AscendColors.GoldGradient,
                    height = 10.dp,
                )
            }
        }
        itemsIndexed(ordered, key = { _, achievement -> achievement.name }) { index, achievement ->
            val isUnlocked = achievement in unlocked
            val progress = statistics?.let { AchievementRules.progress(achievement, it.stats) } ?: 0f
            GlassCard(
                modifier = Modifier.reveal(index),
                accent = if (isUnlocked) achievement.tier.colors.first() else null,
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    AchievementMedal(achievement, unlocked = isUnlocked, diameter = 68.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        achievement.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isUnlocked) AscendColors.TextPrimary else AscendColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = AscendColors.TextMuted,
                        textAlign = TextAlign.Center,
                        minLines = 2,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(10.dp))
                    if (isUnlocked) {
                        Pill("+${achievement.tier.xp} XP · ${achievement.tier.title}", achievement.tier.colors.first())
                    } else {
                        GlowProgressBar(progress, colors = listOf(AscendColors.Violet, AscendColors.Cyan), height = 6.dp, shimmer = false)
                        Spacer(Modifier.height(4.dp))
                        val value = statistics?.stats?.value(achievement.metric) ?: 0
                        Text(
                            "${value.coerceAtMost(achievement.goal)} / ${achievement.goal}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AscendColors.TextMuted,
                        )
                    }
                }
            }
        }
    }
}
