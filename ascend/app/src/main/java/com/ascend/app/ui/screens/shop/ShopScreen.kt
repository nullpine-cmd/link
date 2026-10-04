package com.ascend.app.ui.screens.shop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.GameRepository
import com.ascend.app.data.local.ShopItemEntity
import com.ascend.app.di.CelebrationBus
import com.ascend.app.di.appViewModel
import com.ascend.app.domain.Celebration
import com.ascend.app.domain.PurchaseResult
import com.ascend.app.ui.components.AscendSheet
import com.ascend.app.ui.components.AscendTextField
import com.ascend.app.ui.components.CircleIconButton
import com.ascend.app.ui.components.DetailTopBar
import com.ascend.app.ui.components.EmptyState
import com.ascend.app.ui.components.GhostButton
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.GoldCoin
import com.ascend.app.ui.components.GradientButton
import com.ascend.app.ui.components.ParticleBurst
import com.ascend.app.ui.components.RollingNumber
import com.ascend.app.ui.components.SectionLabel
import com.ascend.app.ui.components.SelectChip
import com.ascend.app.ui.components.bounceClick
import com.ascend.app.ui.components.reveal
import com.ascend.app.ui.theme.AscendColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShopUiState(val gold: Long = 0, val items: List<ShopItemEntity> = emptyList())

class ShopViewModel(
    private val repository: GameRepository,
    private val celebrations: CelebrationBus,
) : ViewModel() {
    val state: StateFlow<ShopUiState> = combine(repository.heroState, repository.shopItems) { hero, items ->
        ShopUiState(gold = hero?.gold ?: 0, items = items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopUiState())

    fun buy(id: Long, onResult: (PurchaseResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.buy(id)
            if (result is PurchaseResult.Success) celebrations.emit(Celebration.Purchased(result))
            onResult(result)
        }
    }

    fun save(id: Long, title: String, emoji: String, cost: Int) {
        viewModelScope.launch { repository.saveShopItem(id, title, emoji, cost) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteShopItem(id) }
    }
}

private val rewardEmojis = listOf("☕", "🎬", "🍰", "🎮", "🛍️", "🍕", "🛀", "😴", "🎧", "📺", "🍫", "🎁", "✈️", "🏖️", "🎨", "📚")

@Composable
fun ShopRoute(onBack: () -> Unit) {
    val viewModel = appViewModel { ShopViewModel(it.repository, it.celebrations) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ShopItemEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "bar") { DetailTopBar("Лавка наград", onBack, Modifier.statusBarsPadding()) }
        item(key = "balance") { BalanceCard(state.gold, Modifier.padding(horizontal = 16.dp).reveal(0)) }
        item(key = "hint") {
            Text(
                "Золото приходит за любые выполненные квесты. Награждай себя честно — ты это заработал.",
                style = MaterialTheme.typography.bodyMedium,
                color = AscendColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        item(key = "label") { SectionLabel("Награды", Modifier.padding(start = 20.dp, top = 6.dp)) }
        if (state.items.isEmpty()) {
            item(key = "empty") {
                EmptyState("🎁", "Лавка пуста", "Добавь то, что тебя радует: кофе, кино, отдых")
            }
        }
        items(state.items, key = { it.id }) { item ->
            ShopItemCard(
                item = item,
                gold = state.gold,
                onBuy = { onResult -> viewModel.buy(item.id, onResult) },
                onEdit = { editing = item },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .animateItem(),
            )
        }
        item(key = "add") {
            GhostButton(
                "Добавить награду",
                icon = Icons.Rounded.Add,
                color = AscendColors.Gold,
                onClick = { creating = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
    }

    if (creating || editing != null) {
        ShopItemSheet(
            initial = editing,
            onSave = { title, emoji, cost -> viewModel.save(editing?.id ?: 0, title, emoji, cost) },
            onDelete = editing?.let { item -> { viewModel.delete(item.id) } },
            onDismiss = {
                creating = false
                editing = null
            },
        )
    }
}

@Composable
private fun BalanceCard(gold: Long, modifier: Modifier = Modifier) {
    GlassCard(modifier.fillMaxWidth(), accent = AscendColors.Gold) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoldCoin(diameter = 64.dp, spinning = true)
            Spacer(Modifier.width(18.dp))
            Column {
                Text("Твоё золото", style = MaterialTheme.typography.labelLarge, color = AscendColors.TextSecondary)
                RollingNumber(gold, MaterialTheme.typography.displayMedium, color = AscendColors.Gold)
            }
        }
    }
}

@Composable
private fun ShopItemCard(
    item: ShopItemEntity,
    gold: Long,
    onBuy: ((PurchaseResult) -> Unit) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val affordable = gold >= item.cost
    val shake = remember { Animatable(0f) }
    var shakeKey by remember { mutableIntStateOf(0) }
    var burst by remember { mutableIntStateOf(0) }
    LaunchedEffect(shakeKey) {
        if (shakeKey > 0) {
            shake.animateTo(
                0f,
                keyframes {
                    durationMillis = 420
                    -14f at 60
                    12f at 130
                    -9f at 200
                    6f at 270
                    -3f at 340
                },
            )
        }
    }
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shake.value * density },
        onClick = onEdit,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AscendColors.Pink.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.emoji, fontSize = 26.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = AscendColors.TextPrimary, maxLines = 2)
                Text(
                    if (item.timesBought > 0) "Получено: ${item.timesBought}" else if (affordable) "Можно взять!" else "Ещё ${item.cost - gold} золота",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (affordable) AscendColors.Success else AscendColors.TextMuted,
                )
            }
            Box(contentAlignment = Alignment.Center) {
                ParticleBurst(
                    trigger = burst,
                    colors = AscendColors.GoldGradient + Color.White,
                    modifier = Modifier.size(40.dp),
                    count = 26,
                    power = 60.dp,
                )
                Row(
                    Modifier
                        .bounceClick(pressedScale = 0.9f) {
                            onBuy { result ->
                                if (result is PurchaseResult.Success) burst++ else shakeKey++
                            }
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (affordable) AscendColors.Gold.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GoldCoin(diameter = 16.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${item.cost}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (affordable) AscendColors.Gold else AscendColors.TextMuted,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShopItemSheet(
    initial: ShopItemEntity?,
    onSave: (String, String, Int) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var emoji by rememberSaveable { mutableStateOf(initial?.emoji ?: "🎁") }
    var cost by rememberSaveable { mutableStateOf(initial?.cost?.toString() ?: "100") }
    val costValue = cost.toIntOrNull() ?: 0

    AscendSheet(onDismiss = onDismiss) { hide ->
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (initial == null) "Новая награда" else "Изменить награду",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AscendColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                CircleIconButton(Icons.Rounded.Close, "Закрыть", hide, diameter = 40.dp)
            }
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rewardEmojis.forEach { candidate ->
                    Box(
                        Modifier
                            .size(42.dp)
                            .bounceClick { emoji = candidate }
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (candidate == emoji) AscendColors.Pink.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(candidate, fontSize = 22.sp)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            AscendTextField(title, { title = it }, "Например: вечер кино", accent = AscendColors.Pink)
            Spacer(Modifier.height(14.dp))
            SectionLabel("Цена в золоте")
            AscendTextField(
                value = cost,
                onValueChange = { value -> cost = value.filter { it.isDigit() }.take(5) },
                placeholder = "Стоимость",
                accent = AscendColors.Gold,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leading = { GoldCoin(diameter = 18.dp) },
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(50, 100, 200, 300, 500, 1000)) { value ->
                    SelectChip("$value", selected = costValue == value, color = AscendColors.Gold, onClick = { cost = value.toString() })
                }
            }
            Spacer(Modifier.height(22.dp))
            GradientButton(
                text = "Сохранить",
                icon = Icons.Rounded.Check,
                enabled = title.isNotBlank() && costValue > 0,
                colors = listOf(AscendColors.Gold, AscendColors.GoldDeep, AscendColors.Pink),
                onClick = {
                    onSave(title, emoji, costValue)
                    hide()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (onDelete != null) {
                Spacer(Modifier.height(10.dp))
                GhostButton(
                    "Удалить",
                    icon = Icons.Rounded.DeleteOutline,
                    color = AscendColors.Danger,
                    onClick = {
                        onDelete()
                        hide()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
