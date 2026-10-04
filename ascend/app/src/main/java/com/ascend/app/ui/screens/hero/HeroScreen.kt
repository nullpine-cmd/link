package com.ascend.app.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.di.appViewModel
import com.ascend.app.domain.HeroState
import com.ascend.app.domain.HeroStatistics
import com.ascend.app.ui.components.AchievementMedal
import com.ascend.app.ui.components.ActivityHeatmap
import com.ascend.app.ui.components.AscendSheet
import com.ascend.app.ui.components.AttributeOrb
import com.ascend.app.ui.components.AttributeRadar
import com.ascend.app.ui.components.BottomBarSpace
import com.ascend.app.ui.components.CircleIconButton
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.GlowProgressBar
import com.ascend.app.ui.components.GoldCoin
import com.ascend.app.ui.components.HeroEmblem
import com.ascend.app.ui.components.RankBadge
import com.ascend.app.ui.components.RollingNumber
import com.ascend.app.ui.components.SectionLabel
import com.ascend.app.ui.components.StatTile
import com.ascend.app.ui.components.WeeklyBars
import com.ascend.app.ui.components.bounceClick
import com.ascend.app.ui.components.reveal
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.Auras
import com.ascend.app.ui.theme.color
import com.ascend.app.ui.theme.colorDeep
import com.ascend.app.ui.theme.colors
import com.ascend.core.Achievement
import com.ascend.core.Attribute
import com.ascend.core.Rank

@Composable
fun HeroRoute(
    onOpenAchievements: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel = appViewModel { HeroViewModel(it.repository, it.time) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    HeroScreen(state, onOpenAchievements, onOpenShop, onOpenSettings)
}

@Composable
fun HeroScreen(
    state: HeroUiState,
    onOpenAchievements: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val hero = state.hero ?: return
    val statistics = state.statistics
    var showRanks by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomBarSpace + 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "top") {
            Row(
                Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Герой", style = MaterialTheme.typography.headlineLarge, color = AscendColors.TextPrimary, modifier = Modifier.weight(1f))
                CircleIconButton(Icons.Rounded.Settings, "Настройки", onOpenSettings)
            }
        }
        item(key = "emblem") { HeroBanner(hero, onRanks = { showRanks = true }) }
        item(key = "level") { LevelCard(hero, Modifier.reveal(1)) }
        if (statistics != null) {
            item(key = "stats") { StatsGrid(statistics, Modifier.reveal(2)) }
        }
        item(key = "radar") {
            GlassCard(Modifier.fillMaxWidth().reveal(3)) {
                SectionLabel("Характеристики")
                AttributeRadar(hero.attributes)
                Text(
                    hero.title.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AscendColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item(key = "attributes") { AttributesCard(hero, Modifier.reveal(4)) }
        item(key = "week") {
            GlassCard(Modifier.fillMaxWidth().reveal(5)) {
                SectionLabel("Опыт за неделю")
                WeeklyBars(state.weekXp, state.weekLabels, highlight = 6)
            }
        }
        item(key = "heatmap") {
            GlassCard(Modifier.fillMaxWidth().reveal(6)) {
                SectionLabel("Карта активности")
                ActivityHeatmap(state.heatmap, state.today, weeks = HeroViewModel.HEATMAP_WEEKS)
            }
        }
        if (statistics != null) {
            item(key = "achievements") { AchievementsPreview(statistics, onOpenAchievements, Modifier.reveal(7)) }
        }
        item(key = "shop") {
            GlassCard(Modifier.fillMaxWidth().reveal(8), accent = AscendColors.Gold, onClick = onOpenShop) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Storefront, contentDescription = null, tint = AscendColors.Gold, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Лавка наград", style = MaterialTheme.typography.titleLarge, color = AscendColors.TextPrimary)
                        Text("Обменяй золото на приятности из реальной жизни", style = MaterialTheme.typography.bodySmall, color = AscendColors.TextSecondary)
                    }
                    GoldCoin(diameter = 18.dp, spinning = true)
                    Spacer(Modifier.width(6.dp))
                    RollingNumber(hero.gold, MaterialTheme.typography.titleMedium, color = AscendColors.Gold)
                }
            }
        }
    }

    if (showRanks) {
        RanksSheet(hero.level.level, onDismiss = { showRanks = false })
    }
}

@Composable
private fun HeroBanner(hero: HeroState, onRanks: () -> Unit) {
    val aura = Auras.of(hero.aura)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        HeroEmblem(
            avatar = hero.avatar,
            aura = aura,
            rank = hero.rank,
            progress = hero.level.progress,
            diameter = 240.dp,
        )
        Text(hero.name, style = MaterialTheme.typography.headlineLarge, color = AscendColors.TextPrimary, modifier = Modifier.reveal(0))
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier
                .reveal(1)
                .bounceClick(onClick = onRanks)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankBadge(hero.rank, diameter = 30.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                "${hero.title.title} · Ранг ${hero.rank.letter} «${hero.rank.title}»",
                style = MaterialTheme.typography.labelLarge,
                color = hero.rank.color,
            )
        }
    }
}

@Composable
private fun LevelCard(hero: HeroState, modifier: Modifier = Modifier) {
    val next = hero.rank.next
    GlassCard(modifier.fillMaxWidth(), accent = AscendColors.Violet) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Уровень ", style = MaterialTheme.typography.titleLarge, color = AscendColors.TextSecondary)
            RollingNumber(
                hero.level.level.toLong(),
                MaterialTheme.typography.displayMedium,
                color = AscendColors.TextPrimary,
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("всего опыта", style = MaterialTheme.typography.labelSmall, color = AscendColors.TextMuted)
                Text("${hero.level.totalXp} XP", style = MaterialTheme.typography.titleMedium, color = AscendColors.Cyan)
            }
        }
        Spacer(Modifier.height(12.dp))
        GlowProgressBar(hero.level.progress, height = 12.dp)
        Spacer(Modifier.height(8.dp))
        Row {
            Text("${hero.level.xpInLevel} / ${hero.level.xpForNext} XP", style = MaterialTheme.typography.labelMedium, color = AscendColors.TextSecondary)
            Spacer(Modifier.weight(1f))
            Text(
                if (next != null) "Ранг ${next.letter} — с ${next.minLevel} уровня" else "Высший ранг достигнут",
                style = MaterialTheme.typography.labelMedium,
                color = next?.color ?: AscendColors.Gold,
            )
        }
    }
}

@Composable
private fun StatsGrid(statistics: HeroStatistics, modifier: Modifier = Modifier) {
    val stats = statistics.stats
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(stats.questsCompleted, "квестов выполнено", Icons.Rounded.TaskAlt, AscendColors.Violet, Modifier.weight(1f))
            StatTile(stats.bestDayStreak, "лучшая серия, дней", Icons.Rounded.LocalFireDepartment, AscendColors.Ember, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(stats.pagesRead, "страниц прочитано", Icons.Rounded.MenuBook, AscendColors.Cyan, Modifier.weight(1f))
            StatTile(statistics.activeDays.toLong(), "дней в пути", Icons.Rounded.CalendarMonth, AscendColors.Success, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AttributesCard(hero: HeroState, modifier: Modifier = Modifier) {
    GlassCard(modifier.fillMaxWidth()) {
        SectionLabel("Прокачка")
        Attribute.entries.forEachIndexed { index, attribute ->
            val info = hero.attribute(attribute)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                AttributeOrb(attribute, diameter = 38.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(attribute.title, style = MaterialTheme.typography.titleSmall, color = AscendColors.TextPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(attribute.examples, style = MaterialTheme.typography.bodySmall, color = AscendColors.TextMuted, maxLines = 1, modifier = Modifier.weight(1f))
                        Text(
                            "ур. ${info.level}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = attribute.color,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    GlowProgressBar(info.progress, colors = listOf(attribute.color, attribute.colorDeep), height = 7.dp, shimmer = index % 2 == 0)
                }
            }
        }
    }
}

@Composable
private fun AchievementsPreview(statistics: HeroStatistics, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val unlocked = statistics.unlocked.entries.sortedByDescending { it.value }.map { it.key }
    GlassCard(modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Достижения", style = MaterialTheme.typography.titleLarge, color = AscendColors.TextPrimary, modifier = Modifier.weight(1f))
            Text(
                "${unlocked.size} / ${Achievement.entries.size}",
                style = MaterialTheme.typography.labelLarge,
                color = AscendColors.Gold,
            )
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = AscendColors.TextMuted, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(14.dp))
        val preview = (unlocked + Achievement.entries.filterNot { it in unlocked }).take(5)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            preview.forEach { achievement ->
                AchievementMedal(achievement, unlocked = achievement in unlocked, diameter = 54.dp)
            }
        }
    }
}

@Composable
private fun RanksSheet(level: Int, onDismiss: () -> Unit) {
    val current = Rank.forLevel(level)
    AscendSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text("Путь рангов", style = MaterialTheme.typography.headlineMedium, color = AscendColors.TextPrimary)
            Text(
                "Ранг растёт вместе с уровнем — любым путём, который ты выберешь",
                style = MaterialTheme.typography.bodyMedium,
                color = AscendColors.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            Rank.entries.forEachIndexed { index, rank ->
                val reached = level >= rank.minLevel
                val isCurrent = rank == current
                Row(
                    Modifier
                        .fillMaxWidth()
                        .reveal(index, delayStep = 45L)
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isCurrent) {
                                Brush.horizontalGradient(listOf(rank.color.copy(alpha = 0.25f), Color.Transparent))
                            } else {
                                Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.03f), Color.White.copy(alpha = 0.03f)))
                            },
                        )
                        .border(1.dp, if (isCurrent) rank.color.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(18.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        if (reached) RankBadge(rank, diameter = 44.dp) else Text(rank.letter, style = MaterialTheme.typography.titleLarge, color = AscendColors.TextMuted)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            rank.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (reached) AscendColors.TextPrimary else AscendColors.TextMuted,
                        )
                        Text(rank.motto, style = MaterialTheme.typography.bodySmall, color = AscendColors.TextMuted, maxLines = 2)
                    }
                    Text(
                        "ур. ${rank.minLevel}+",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (reached) rank.colors.last() else AscendColors.TextMuted,
                    )
                }
            }
        }
    }
}
