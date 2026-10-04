package com.ascend.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.GameRepository
import com.ascend.app.di.appViewModel
import com.ascend.app.ui.components.AscendTextField
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.GradientButton
import com.ascend.app.ui.components.HeroEmblem
import com.ascend.app.ui.components.ParticleBurst
import com.ascend.app.ui.components.SectionLabel
import com.ascend.app.ui.components.bounceClick
import com.ascend.app.ui.components.reveal
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.Auras
import com.ascend.core.QuestTemplates
import com.ascend.core.Rank
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OnboardingViewModel(private val repository: GameRepository) : ViewModel() {
    fun create(name: String, avatar: String, aura: Int) {
        viewModelScope.launch { repository.createHero(name, avatar, aura) }
    }
}

@Composable
fun OnboardingScreen() {
    val viewModel = appViewModel { OnboardingViewModel(it.repository) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableStateOf(QuestTemplates.avatars.first()) }
    var aura by rememberSaveable { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (
                    slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 260f)) { if (forward) it / 2 else -it / 2 } +
                        fadeIn(tween(400))
                    ) togetherWith (
                    slideOutHorizontally(tween(350)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(250))
                    )
            },
            label = "onboarding",
        ) { current ->
            when (current) {
                0 -> IntroStep(onNext = { step = 1 })
                1 -> PrinciplesStep(onNext = { step = 2 })
                else -> CreateHeroStep(
                    name = name,
                    onName = { name = it },
                    avatar = avatar,
                    onAvatar = { avatar = it },
                    aura = aura,
                    onAura = { aura = it },
                    onCreate = { viewModel.create(name, avatar, aura) },
                )
            }
        }
        StepDots(
            step = step,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun StepDots(step: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            val width by animateDpAsState(if (index == step) 26.dp else 8.dp, spring(dampingRatio = 0.6f, stiffness = 400f), label = "dot")
            val color by animateColorAsState(if (index == step) AscendColors.Violet else Color.White.copy(alpha = 0.2f), label = "dotColor")
            Box(
                Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun IntroStep(onNext: () -> Unit) {
    val title = "ASCEND"
    val letters = remember { List(title.length) { Animatable(0f) } }
    val subtitle = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { progress.animateTo(0.72f, tween(2_400, delayMillis = 300, easing = FastOutSlowInEasing)) }
        delay(350)
        letters.forEach { letter ->
            launch { letter.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 260f)) }
            delay(90)
        }
        subtitle.animateTo(1f, tween(700))
    }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        HeroEmblem(
            avatar = "✨",
            aura = Auras.of(0),
            rank = Rank.SSS,
            progress = progress.value,
            diameter = 250.dp,
        )
        Spacer(Modifier.height(18.dp))
        Row {
            title.forEachIndexed { index, char ->
                Text(
                    char.toString(),
                    style = TextStyle(
                        brush = Brush.verticalGradient(listOf(Color.White, AscendColors.VioletLight, AscendColors.Cyan)),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp,
                    ),
                    modifier = Modifier.graphicsLayer {
                        val v = letters[index].value
                        alpha = v.coerceIn(0f, 1f)
                        translationY = (1f - v) * 40.dp.toPx()
                        rotationX = (1f - v) * 70f
                    },
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = subtitle.value
                translationY = (1f - subtitle.value) * 16.dp.toPx()
            },
        ) {
            Text(
                "Прокачивай себя в реальной жизни",
                style = MaterialTheme.typography.titleLarge,
                color = AscendColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Твой герой растёт, когда растёшь ты. Каждое дело — квест, каждый шаг — опыт.",
                style = MaterialTheme.typography.bodyLarge,
                color = AscendColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))
        GradientButton(
            "Начать путь",
            onClick = onNext,
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = subtitle.value },
        )
    }
}

private data class Principle(val emoji: String, val title: String, val text: String)

private val principles = listOf(
    Principle("⚔️", "Жизнь — это квесты", "Чтение, спорт, работа, отдых — превращай любые дела в квесты и планируй свой день."),
    Principle("✨", "Любой шаг засчитывается", "Хотел прочитать 20 страниц, а прочитал одну? Награда всё равно твоя. Цель лишь даёт бонус."),
    Principle("⚖️", "Все пути равноценны", "Отжимания или книги — опыт одинаковый. Ты сам решаешь, кем стать: Воином, Мудрецом или Бардом."),
    Principle("👑", "Ранги и легенды", "Расти от Новичка (E) до Легенды (SSS), открывай достижения и трать золото в лавке наград."),
    Principle("🕊️", "Без наказаний", "Пропустил день? Ничего не сгорит. Серия начнётся заново, а за возвращение — бонус."),
)

@Composable
private fun PrinciplesStep(onNext: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 20.dp, bottom = 48.dp),
    ) {
        Text("Как это работает", style = MaterialTheme.typography.headlineLarge, color = AscendColors.TextPrimary, modifier = Modifier.reveal(0))
        Spacer(Modifier.height(6.dp))
        Text(
            "Приложение ни к чему не обязывает — оно поощряет за всё, что делает тебя лучше.",
            style = MaterialTheme.typography.bodyLarge,
            color = AscendColors.TextSecondary,
            modifier = Modifier.reveal(1),
        )
        Spacer(Modifier.height(20.dp))
        principles.forEachIndexed { index, principle ->
            GlassCard(Modifier.fillMaxWidth().padding(bottom = 12.dp).reveal(index + 2, delayStep = 110L)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(principle.emoji, fontSize = 30.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(principle.title, style = MaterialTheme.typography.titleLarge, color = AscendColors.TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(principle.text, style = MaterialTheme.typography.bodyMedium, color = AscendColors.TextSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GradientButton(
            "Создать героя",
            onClick = onNext,
            icon = Icons.Rounded.AutoAwesome,
            modifier = Modifier.fillMaxWidth().reveal(8),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateHeroStep(
    name: String,
    onName: (String) -> Unit,
    avatar: String,
    onAvatar: (String) -> Unit,
    aura: Int,
    onAura: (Int) -> Unit,
    onCreate: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var awakening by remember { mutableStateOf(false) }
    var burst by remember { mutableIntStateOf(0) }
    val emblemScale by animateFloatAsState(
        targetValue = if (awakening) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 120f),
        label = "awaken",
    )
    val palette = Auras.of(aura)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
            .padding(top = 12.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Твой герой", style = MaterialTheme.typography.headlineLarge, color = AscendColors.TextPrimary)
        Box(contentAlignment = Alignment.Center) {
            ParticleBurst(
                trigger = burst,
                colors = palette + AscendColors.Gold + Color.White,
                modifier = Modifier.size(200.dp),
                count = 60,
                power = 160.dp,
                durationMillis = 1_100,
                gravity = 0.15f,
            )
            HeroEmblem(
                avatar = avatar,
                aura = palette,
                rank = Rank.E,
                progress = if (awakening) 1f else 0.08f,
                diameter = 200.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = emblemScale
                    scaleY = emblemScale
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        AscendTextField(
            value = name,
            onValueChange = onName,
            placeholder = "Имя героя",
            maxLength = 24,
            accent = palette.first(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            textStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        SectionLabel("Облик", Modifier.fillMaxWidth())
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuestTemplates.avatars.forEach { candidate ->
                val selected = candidate == avatar
                val scale by animateFloatAsState(
                    if (selected) 1.12f else 1f,
                    spring(dampingRatio = 0.45f, stiffness = 420f),
                    label = "avatarScale",
                )
                Box(
                    Modifier
                        .size(54.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .bounceClick { onAvatar(candidate) }
                        .clip(CircleShape)
                        .background(
                            if (selected) {
                                Brush.radialGradient(listOf(palette.first().copy(alpha = 0.6f), palette.last().copy(alpha = 0.15f)))
                            } else {
                                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.03f)))
                            },
                        )
                        .border(1.5.dp, if (selected) palette.first() else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(candidate, fontSize = 26.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionLabel("Аура", Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Auras.palettes.forEachIndexed { index, colors ->
                val selected = index == aura
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(if (selected) 44.dp else 36.dp)
                            .bounceClick { onAura(index) }
                            .clip(CircleShape)
                            .background(Brush.linearGradient(colors))
                            .border(2.dp, if (selected) Color.White else Color.Transparent, CircleShape),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Auras.names[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) AscendColors.TextPrimary else AscendColors.TextMuted,
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        GradientButton(
            text = if (awakening) "Пробуждение…" else "Пробудить героя",
            icon = Icons.Rounded.AutoAwesome,
            enabled = name.isNotBlank() && !awakening,
            colors = palette + AscendColors.Violet,
            onClick = {
                awakening = true
                burst++
                scope.launch {
                    delay(900)
                    onCreate()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
