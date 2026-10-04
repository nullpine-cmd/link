package com.ascend.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.GameRepository
import com.ascend.app.di.LocalNotificationPermission
import com.ascend.app.di.ReminderScheduler
import com.ascend.app.di.appViewModel
import com.ascend.app.domain.HeroState
import com.ascend.app.ui.components.AscendTextField
import com.ascend.app.ui.components.DetailTopBar
import com.ascend.app.ui.components.GhostButton
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.GradientButton
import com.ascend.app.ui.components.HeroEmblem
import com.ascend.app.ui.components.SectionLabel
import com.ascend.app.ui.components.SelectChip
import com.ascend.app.ui.components.bounceClick
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.Auras
import com.ascend.app.ui.util.formatTime
import com.ascend.core.QuestTemplates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: GameRepository,
    private val reminders: ReminderScheduler,
) : ViewModel() {
    val hero: StateFlow<HeroState?> = repository.heroState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveProfile(name: String, avatar: String, aura: Int) {
        viewModelScope.launch { repository.updateProfile(name, avatar, aura) }
    }

    fun setReminder(enabled: Boolean, minutes: Int) {
        viewModelScope.launch {
            repository.setReminder(enabled, minutes)
            if (enabled) reminders.schedule(minutes / 60, minutes % 60) else reminders.cancel()
        }
    }

    fun reset() {
        viewModelScope.launch {
            reminders.cancel()
            repository.resetProgress()
        }
    }
}

@Composable
fun SettingsRoute(onBack: () -> Unit) {
    val viewModel = appViewModel { SettingsViewModel(it.repository, it.reminders) }
    val hero by viewModel.hero.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        DetailTopBar("Настройки", onBack, Modifier.statusBarsPadding())
        hero?.let { SettingsContent(it, viewModel) }
    }
}

@Composable
private fun SettingsContent(hero: HeroState, viewModel: SettingsViewModel) {
    var name by remember(hero.name) { mutableStateOf(hero.name) }
    var avatar by remember(hero.avatar) { mutableStateOf(hero.avatar) }
    var aura by remember(hero.aura) { mutableIntStateOf(hero.aura) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val permission = LocalNotificationPermission.current
    val changed = name.trim() != hero.name || avatar != hero.avatar || aura != hero.aura

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GlassCard(Modifier.fillMaxWidth(), accent = Auras.of(aura).first()) {
            SectionLabel("Профиль героя")
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeroEmblem(avatar, Auras.of(aura), hero.rank, hero.level.progress, diameter = 96.dp, detailed = false)
                Spacer(Modifier.width(14.dp))
                AscendTextField(name, { name = it }, "Имя героя", maxLength = 24, accent = Auras.of(aura).first(), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QuestTemplates.avatars) { candidate ->
                    Box(
                        Modifier
                            .size(46.dp)
                            .bounceClick { avatar = candidate }
                            .clip(CircleShape)
                            .background(if (candidate == avatar) Auras.of(aura).first().copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(candidate, fontSize = 22.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Auras.palettes.forEachIndexed { index, colors ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .bounceClick { aura = index }
                            .clip(CircleShape)
                            .background(Brush.linearGradient(colors))
                            .border(2.dp, if (index == aura) Color.White else Color.Transparent, CircleShape),
                    )
                }
            }
            if (changed) {
                Spacer(Modifier.height(14.dp))
                GradientButton(
                    "Сохранить профиль",
                    onClick = { viewModel.saveProfile(name, avatar, aura) },
                    icon = Icons.Rounded.Check,
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = AscendColors.Cyan)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ежедневное напоминание", style = MaterialTheme.typography.titleMedium, color = AscendColors.TextPrimary)
                    Text("Мягкий зов к приключениям, без давления", style = MaterialTheme.typography.bodySmall, color = AscendColors.TextMuted)
                }
                Switch(
                    checked = hero.reminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            permission.request { granted ->
                                if (granted) viewModel.setReminder(true, hero.reminderMinutes)
                            }
                        } else {
                            viewModel.setReminder(false, hero.reminderMinutes)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AscendColors.Cyan,
                        uncheckedThumbColor = AscendColors.TextMuted,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.06f),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.12f),
                    ),
                )
            }
            if (hero.reminderEnabled) {
                Spacer(Modifier.height(12.dp))
                SelectChip(
                    text = "В ${formatTime(hero.reminderMinutes)}",
                    icon = Icons.Rounded.Schedule,
                    selected = true,
                    color = AscendColors.Cyan,
                    onClick = { pickTime = true },
                )
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            SectionLabel("Кодекс Ascend")
            listOf(
                "✨ Любой прогресс засчитывается — даже одна страница",
                "⚖️ Все характеристики равноценны: путь выбираешь ты",
                "🕊️ Никаких штрафов: пропуск не отнимает опыт",
                "🔁 За возвращение после перерыва — бонус",
                "🎁 Золото тратится на настоящие награды для себя",
            ).forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AscendColors.TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }

        GlassCard(Modifier.fillMaxWidth(), accent = AscendColors.Danger) {
            SectionLabel("Новое начало", color = AscendColors.Danger)
            Text(
                "Сбросит героя, квесты, книги и достижения. Это действие нельзя отменить.",
                style = MaterialTheme.typography.bodySmall,
                color = AscendColors.TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            GhostButton(
                "Начать путь заново",
                icon = Icons.Rounded.RestartAlt,
                color = AscendColors.Danger,
                onClick = { confirmReset = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            "Ascend 1.0 · сделано, чтобы ты становился сильнее",
            style = MaterialTheme.typography.labelMedium,
            color = AscendColors.TextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }

    if (pickTime) {
        val state = rememberTimePickerState(
            initialHour = hero.reminderMinutes / 60,
            initialMinute = hero.reminderMinutes % 60,
            is24Hour = true,
        )
        BasicAlertDialog(onDismissRequest = { pickTime = false }) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(AscendColors.SurfaceHigh)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Время напоминания", style = MaterialTheme.typography.titleLarge, color = AscendColors.TextPrimary)
                Spacer(Modifier.height(16.dp))
                TimePicker(state = state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { pickTime = false }) { Text("Отмена") }
                    TextButton(onClick = {
                        viewModel.setReminder(true, state.hour * 60 + state.minute)
                        pickTime = false
                    }) { Text("Готово") }
                }
            }
        }
    }

    if (confirmReset) {
        var seconds by remember { mutableIntStateOf(3) }
        LaunchedEffect(Unit) {
            while (seconds > 0) {
                kotlinx.coroutines.delay(1_000)
                seconds--
            }
        }
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = AscendColors.SurfaceHigh,
            title = { Text("Начать заново?") },
            text = { Text("Весь прогресс героя будет стёрт. Уверен?") },
            confirmButton = {
                TextButton(
                    enabled = seconds == 0,
                    onClick = {
                        confirmReset = false
                        viewModel.reset()
                    },
                ) { Text(if (seconds > 0) "Сбросить ($seconds)" else "Сбросить", color = AscendColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } },
        )
    }
}
