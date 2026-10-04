package com.ascend.app.ui.screens.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.data.local.DayActivity
import com.ascend.app.data.local.QuestEntity
import com.ascend.app.data.local.hasTarget
import com.ascend.app.data.local.isHabit
import com.ascend.app.di.appViewModel
import com.ascend.app.domain.DayPlan
import com.ascend.app.domain.HeroState
import com.ascend.app.domain.PlanStatus
import com.ascend.app.ui.components.AscendTextField
import com.ascend.app.ui.components.bottomBarContentPadding
import com.ascend.app.ui.components.CircleIconButton
import com.ascend.app.ui.components.EmptyState
import com.ascend.app.ui.components.GhostButton
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.GlowProgressBar
import com.ascend.app.ui.components.GoldPill
import com.ascend.app.ui.components.HeroEmblem
import com.ascend.app.ui.components.ProgressRing
import com.ascend.app.ui.components.RankBadge
import com.ascend.app.ui.components.RollingNumber
import com.ascend.app.ui.components.SelectChip
import com.ascend.app.ui.components.StreakFlame
import com.ascend.app.ui.components.bounceClick
import com.ascend.app.ui.components.reveal
import com.ascend.app.ui.screens.quests.QuestCard
import com.ascend.app.ui.screens.quests.QuestEditorRequest
import com.ascend.app.ui.screens.quests.QuestMenuAction
import com.ascend.app.ui.screens.quests.QuestProgressSheet
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.Auras
import com.ascend.app.ui.theme.color
import com.ascend.app.ui.util.dateLong
import com.ascend.app.ui.util.daysWord
import com.ascend.app.ui.util.questsWord
import com.ascend.app.ui.util.relativeDay
import com.ascend.app.ui.util.weekdayFull
import com.ascend.app.ui.util.weekdayShort
import com.ascend.core.Motivation
import com.ascend.core.QuestTemplate
import com.ascend.core.QuestTemplates
import com.ascend.core.Quote
import java.time.LocalDate

@Composable
fun TodayRoute(
    onOpenHero: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenEditor: (QuestEditorRequest) -> Unit,
) {
    val viewModel = appViewModel { TodayViewModel(it.repository, it.celebrations, it.time) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    TodayScreen(
        state = state,
        onSelectDay = viewModel::selectDay,
        onComplete = viewModel::complete,
        onFocus = viewModel::setFocus,
        onPostpone = viewModel::postpone,
        onOpenHero = onOpenHero,
        onOpenShop = onOpenShop,
        onOpenEditor = onOpenEditor,
    )
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    onSelectDay: (Long) -> Unit,
    onComplete: (Long, Int) -> Unit,
    onFocus: (String) -> Unit,
    onPostpone: (Long) -> Unit,
    onOpenHero: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenEditor: (QuestEditorRequest) -> Unit,
) {
    var progressFor by remember { mutableStateOf<QuestEntity?>(null) }
    val hero = state.hero
    val plan = state.plan
    val isToday = state.selectedDay == state.today

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomBarContentPadding()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            TodayHeader(
                name = hero?.name.orEmpty(),
                gold = hero?.gold ?: 0,
                today = state.today,
                hour = state.hour,
                onOpenShop = onOpenShop,
                modifier = Modifier.reveal(0),
            )
        }
        if (hero != null) {
            item(key = "hero") {
                HeroSummaryCard(hero, onOpenHero, Modifier.padding(horizontal = 16.dp).reveal(1))
            }
        }
        item(key = "week") {
            WeekStrip(
                selected = state.selectedDay,
                today = state.today,
                activity = state.activity,
                onSelect = onSelectDay,
                modifier = Modifier.reveal(2),
            )
        }
        if (isToday) {
            item(key = "quote") { QuoteCard(state.quote, Modifier.padding(horizontal = 16.dp).reveal(3)) }
            if (hero != null) {
                item(key = "focus") { FocusCard(hero.focus, onFocus, Modifier.padding(horizontal = 16.dp).reveal(4)) }
            }
        }
        if (plan != null) {
            item(key = "progress") {
                DayProgressCard(plan, Modifier.padding(horizontal = 16.dp).reveal(5))
            }
            if (plan.items.isEmpty()) {
                item(key = "empty") {
                    EmptyDay(
                        plan = plan,
                        onTemplate = { template ->
                            onOpenEditor(QuestEditorRequest.New(dueDay = plan.day, template = template))
                        },
                        onCreate = { onOpenEditor(QuestEditorRequest.New(dueDay = plan.day)) },
                    )
                }
            }
            items(plan.items, key = { "quest-${it.quest.id}" }) { item ->
                val quest = item.quest
                QuestCard(
                    quest = quest,
                    done = item.status == PlanStatus.DONE,
                    canComplete = item.status == PlanStatus.PENDING,
                    streak = item.visibleStreak,
                    log = item.log,
                    badge = if (item.overdue) "С прошлых дней" else null,
                    subtitle = if (item.status == PlanStatus.PLANNED) "Запланировано" else null,
                    onCheck = {
                        if (quest.hasTarget) progressFor = quest else onComplete(quest.id, 1)
                    },
                    onClick = { onOpenEditor(QuestEditorRequest.Edit(quest)) },
                    menu = if (!quest.isHabit && item.status == PlanStatus.PENDING) {
                        listOf(QuestMenuAction("Перенести на завтра", Icons.Rounded.EventRepeat) { onPostpone(quest.id) })
                    } else {
                        emptyList()
                    },
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp),
                )
            }
            if (!plan.isPast) {
                item(key = "add") {
                    GhostButton(
                        text = if (isToday) "Добавить квест" else "Запланировать на ${relativeDay(plan.day, state.today).lowercase()}",
                        icon = Icons.Rounded.Add,
                        onClick = { onOpenEditor(QuestEditorRequest.New(dueDay = plan.day)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                }
            }
        }
    }

    progressFor?.let { quest ->
        QuestProgressSheet(
            quest = quest,
            book = quest.bookId?.let { state.books[it] },
            today = state.today,
            onDismiss = { progressFor = null },
            onConfirm = { amount -> onComplete(quest.id, amount) },
        )
    }
}

@Composable
private fun TodayHeader(
    name: String,
    gold: Long,
    today: Long,
    hour: Int,
    onOpenShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    append("${Motivation.greeting(hour)},\n")
                    withStyle(SpanStyle(brush = Brush.linearGradient(AscendColors.Xp))) { append(name) }
                },
                style = MaterialTheme.typography.headlineLarge,
                color = AscendColors.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${weekdayFull(today).replaceFirstChar { it.uppercase() }}, ${dateLong(today)}",
                style = MaterialTheme.typography.bodyMedium,
                color = AscendColors.TextSecondary,
            )
        }
        GoldPill(gold, Modifier.padding(top = 6.dp).bounceClick(onClick = onOpenShop))
    }
}

@Composable
private fun HeroSummaryCard(hero: HeroState, onOpenHero: () -> Unit, modifier: Modifier = Modifier) {
    val aura = Auras.of(hero.aura)
    GlassCard(modifier = modifier.fillMaxWidth(), accent = aura.first(), onClick = onOpenHero) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeroEmblem(
                avatar = hero.avatar,
                aura = aura,
                rank = hero.rank,
                progress = hero.level.progress,
                diameter = 104.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ур. ", style = MaterialTheme.typography.headlineMedium, color = AscendColors.TextSecondary)
                    RollingNumber(
                        value = hero.level.level.toLong(),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = AscendColors.TextPrimary,
                    )
                    Spacer(Modifier.weight(1f))
                    RankBadge(hero.rank, diameter = 40.dp)
                }
                Text(
                    "${hero.title.title} · ${hero.rank.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AscendColors.TextSecondary,
                    maxLines = 1,
                )
                Spacer(Modifier.height(10.dp))
                GlowProgressBar(hero.level.progress, height = 9.dp)
                Spacer(Modifier.height(6.dp))
                Row {
                    Text(
                        "${hero.level.xpInLevel} / ${hero.level.xpForNext} XP",
                        style = MaterialTheme.typography.labelMedium,
                        color = AscendColors.Cyan,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "до ур. ${hero.level.level + 1}: ${hero.level.xpToNext}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AscendColors.TextMuted,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StreakFlame(diameter = 24.dp, lit = hero.streak.current > 0)
            Spacer(Modifier.width(6.dp))
            Text(
                if (hero.streak.current > 0) "Серия: ${daysWord(hero.streak.current)}" else "Начни серию сегодня",
                style = MaterialTheme.typography.labelLarge,
                color = if (hero.streak.current > 0) AscendColors.Ember else AscendColors.TextSecondary,
            )
            Spacer(Modifier.weight(1f))
            if (hero.streak.best > 0) {
                Text("Рекорд: ${hero.streak.best}", style = MaterialTheme.typography.labelMedium, color = AscendColors.TextMuted)
            }
        }
    }
}

@Composable
private fun WeekStrip(
    selected: Long,
    today: Long,
    activity: Map<Long, DayActivity>,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val past = TodayViewModel.WEEK_STRIP_PAST
    val days = remember(today) { (today - past..today + TodayViewModel.WEEK_STRIP_FUTURE).toList() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (past - 3).toInt())
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(days, key = { it }) { day ->
            DayCell(
                day = day,
                selected = day == selected,
                isToday = day == today,
                isFuture = day > today,
                count = activity[day]?.count ?: 0,
                onClick = { onSelect(day) },
            )
        }
    }
}

@Composable
private fun DayCell(
    day: Long,
    selected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    val selection by animateFloatAsState(if (selected) 1f else 0f, label = "daySelection")
    val textColor by animateColorAsState(
        when {
            selected -> Color.White
            isFuture -> AscendColors.TextMuted
            else -> AscendColors.TextSecondary
        },
        label = "dayText",
    )
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .width(50.dp)
            .bounceClick(pressedScale = 0.9f, onClick = onClick)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.04f))
            .background(
                Brush.verticalGradient(
                    listOf(
                        AscendColors.Violet.copy(alpha = 0.85f * selection),
                        AscendColors.Indigo.copy(alpha = 0.55f * selection),
                    ),
                ),
            )
            .border(
                1.dp,
                when {
                    selected -> AscendColors.VioletLight.copy(alpha = 0.7f)
                    isToday -> AscendColors.Gold.copy(alpha = 0.6f)
                    else -> Color.White.copy(alpha = 0.06f)
                },
                shape,
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(weekdayShort(day), style = MaterialTheme.typography.labelSmall, color = textColor)
        Spacer(Modifier.height(4.dp))
        Text(
            LocalDate.ofEpochDay(day).dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) Color.White else AscendColors.TextPrimary.copy(alpha = if (isFuture) 0.6f else 1f),
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .size(if (count > 0) (4 + count.coerceAtMost(4)).dp else 4.dp)
                .clip(CircleShape)
                .background(
                    when {
                        count > 0 && selected -> Color.White
                        count > 0 -> AscendColors.Cyan
                        else -> Color.Transparent
                    },
                ),
        )
    }
}

@Composable
private fun QuoteCard(quote: Quote, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Box {
            Text(
                "“",
                fontSize = 72.sp,
                color = AscendColors.Violet.copy(alpha = 0.45f),
                modifier = Modifier.offset(x = (-4).dp, y = (-26).dp),
            )
            Column(Modifier.padding(top = 18.dp)) {
                Text(
                    quote.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                    color = AscendColors.TextPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text("— ${quote.author}", style = MaterialTheme.typography.labelMedium, color = AscendColors.VioletLight)
            }
        }
    }
}

@Composable
private fun FocusCard(focus: String?, onSave: (String) -> Unit, modifier: Modifier = Modifier) {
    var editing by remember(focus) { mutableStateOf(focus == null) }
    var text by remember(focus) { mutableStateOf(focus.orEmpty()) }
    val focusManager = LocalFocusManager.current
    val save = {
        if (text.isNotBlank()) {
            onSave(text)
            editing = false
        }
        focusManager.clearFocus()
    }
    GlassCard(modifier = modifier.fillMaxWidth(), accent = if (focus != null) AscendColors.Gold else null) {
        Text("🎯  ГЛАВНАЯ ЦЕЛЬ ДНЯ", style = MaterialTheme.typography.labelSmall, color = AscendColors.Gold)
        Spacer(Modifier.height(10.dp))
        AnimatedContent(
            targetState = editing,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "focus",
        ) { isEditing ->
            if (isEditing) {
                AscendTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "Что сделает этот день успешным?",
                    maxLength = 120,
                    accent = AscendColors.Gold,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    trailing = {
                        if (text.isNotBlank()) {
                            CircleIconButton(Icons.Rounded.Check, "Сохранить", onClick = save, tint = AscendColors.Gold, diameter = 32.dp)
                        }
                    },
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        focus.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = AscendColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    CircleIconButton(Icons.Rounded.Edit, "Изменить", onClick = { editing = true }, diameter = 36.dp)
                }
            }
        }
    }
}

@Composable
private fun DayProgressCard(plan: DayPlan, modifier: Modifier = Modifier) {
    val title = when {
        plan.isToday -> "Прогресс дня"
        plan.isPast -> "Летопись: ${relativeDay(plan.day, plan.today).lowercase()}"
        else -> "План: ${relativeDay(plan.day, plan.today).lowercase()}"
    }
    val message = when {
        plan.isToday -> Motivation.dayProgressMessage(plan.done, plan.total)
        plan.isPast && plan.done > 0 -> "Выполнено: ${questsWord(plan.done)}. Каждый шаг остался с тобой"
        plan.isPast -> "День отдыха — тоже часть пути"
        plan.total > 0 -> "Запланировано: ${questsWord(plan.total)}"
        else -> "Пока пусто — запланируй что-нибудь приятное"
    }
    val complete = plan.total > 0 && plan.done == plan.total
    GlassCard(modifier = modifier.fillMaxWidth(), accent = if (complete) AscendColors.Gold else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = plan.progress,
                modifier = Modifier.size(78.dp),
                colors = if (complete) AscendColors.GoldGradient else AscendColors.Xp,
                stroke = 8.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RollingNumber(
                        value = plan.done.toLong(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = AscendColors.TextPrimary,
                    )
                    Text("из ${plan.total}", style = MaterialTheme.typography.labelSmall, color = AscendColors.TextMuted)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = AscendColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = AscendColors.TextSecondary)
                if (complete && plan.total >= 3 && plan.isToday) {
                    Spacer(Modifier.height(6.dp))
                    Text("🌟 Идеальный день", style = MaterialTheme.typography.labelLarge, color = AscendColors.Gold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyDay(
    plan: DayPlan,
    onTemplate: (QuestTemplate) -> Unit,
    onCreate: () -> Unit,
) {
    if (plan.isPast) {
        EmptyState("🌙", "День отдыха", "Отдых — тоже часть пути героя. Здесь нет штрафов, только движение вперёд.")
        return
    }
    EmptyState(
        emoji = "🌱",
        title = "Чистый лист",
        subtitle = "Выбери квест — даже самый маленький шаг засчитается и прокачает героя",
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuestTemplates.all.take(6).forEach { template ->
                SelectChip(
                    text = template.title,
                    leading = template.emoji,
                    selected = false,
                    color = template.attribute.color,
                    onClick = { onTemplate(template) },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        GhostButton("Свой квест", onClick = onCreate, icon = Icons.Rounded.Add)
    }
}
