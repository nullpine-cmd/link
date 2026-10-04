package com.ascend.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color
import com.ascend.app.ui.theme.icon
import com.ascend.core.Attribute
import com.ascend.core.LevelInfo
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Паутина характеристик: шестиугольник раскрывается из центра с пружиной,
 * вершины светятся цветами характеристик.
 */
@Composable
fun AttributeRadar(levels: Map<Attribute, LevelInfo>, modifier: Modifier = Modifier) {
    val attributes = Attribute.entries
    val maxLevel = attributes.maxOf { (levels[it]?.level ?: 1) }.coerceAtLeast(4) + 1
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 70f)) }
    val values: List<State<Float>> = attributes.map { attribute ->
        val info = levels[attribute]
        val raw = if (info == null) 0f else (info.level - 1 + info.progress) / (maxLevel - 1).toFloat()
        animateFloatAsState(0.12f + 0.88f * raw.coerceIn(0f, 1f), spring(dampingRatio = 0.7f, stiffness = 80f), label = "radar")
    }
    val transition = rememberInfiniteTransition(label = "radarSpin")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        label = "spin",
    )

    Layout(
        modifier = modifier.fillMaxWidth().aspectRatio(1.05f),
        content = {
            Canvas(Modifier) {
                val c = center
                val radius = size.minDimension / 2f * 0.62f
                val angles = attributes.indices.map { (-90f + 60f * it) * (PI.toFloat() / 180f) }
                fun point(i: Int, scale: Float) = Offset(c.x + cos(angles[i]) * radius * scale, c.y + sin(angles[i]) * radius * scale)

                rotate(spin, c) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = radius * 1.12f,
                        center = c,
                        style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx()))),
                    )
                }
                for (ring in 1..4) {
                    val s = ring / 4f
                    val grid = Path().apply {
                        attributes.indices.forEach { i ->
                            val p = point(i, s)
                            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                        }
                        close()
                    }
                    drawPath(grid, Color.White.copy(alpha = if (ring == 4) 0.12f else 0.05f), style = Stroke(1.dp.toPx()))
                }
                attributes.indices.forEach { i ->
                    drawLine(Color.White.copy(alpha = 0.06f), c, point(i, 1f), 1.dp.toPx())
                }

                val a = appear.value
                val shape = Path().apply {
                    attributes.indices.forEach { i ->
                        val p = point(i, values[i].value * a)
                        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                    }
                    close()
                }
                drawPath(
                    shape,
                    Brush.radialGradient(
                        listOf(AscendColors.Violet.copy(alpha = 0.55f), AscendColors.Cyan.copy(alpha = 0.18f)),
                        center = c,
                        radius = radius,
                    ),
                )
                drawPath(shape, AscendColors.VioletLight.copy(alpha = 0.25f), style = Stroke(6.dp.toPx()))
                drawPath(shape, Color.White.copy(alpha = 0.85f), style = Stroke(1.6.dp.toPx()))
                attributes.forEachIndexed { i, attribute ->
                    val p = point(i, values[i].value * a)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(attribute.color.copy(alpha = 0.7f), Color.Transparent), center = p, radius = 12.dp.toPx()),
                        radius = 12.dp.toPx(),
                        center = p,
                    )
                    drawCircle(attribute.color, 4.dp.toPx(), p)
                    drawCircle(Color.White, 1.8.dp.toPx(), p)
                }
            }
            attributes.forEach { attribute ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(attribute.icon, contentDescription = null, tint = attribute.color, modifier = Modifier.size(18.dp))
                    Text(attribute.title, style = MaterialTheme.typography.labelMedium, color = AscendColors.TextPrimary)
                    Text(
                        "ур. ${levels[attribute]?.level ?: 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = attribute.color,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else width
        val canvas = measurables.first().measure(Constraints.fixed(width, height))
        val labels = measurables.drop(1).map { it.measure(Constraints()) }
        layout(width, height) {
            canvas.place(0, 0)
            val cx = width / 2f
            val cy = height / 2f
            val labelRadius = minOf(width, height) / 2f * 0.84f
            labels.forEachIndexed { i, placeable ->
                val angle = (-90f + 60f * i) * (PI.toFloat() / 180f)
                val x = cx + cos(angle) * labelRadius - placeable.width / 2f
                val y = cy + sin(angle) * labelRadius - placeable.height / 2f
                placeable.place(
                    x.roundToInt().coerceIn(0, (width - placeable.width).coerceAtLeast(0)),
                    y.roundToInt().coerceIn(0, (height - placeable.height).coerceAtLeast(0)),
                )
            }
        }
    }
}

/** Тепловая карта активности за последние недели: ячейки вспыхивают волной. */
@Composable
fun ActivityHeatmap(
    counts: Map<Long, Int>,
    today: Long,
    modifier: Modifier = Modifier,
    weeks: Int = 17,
) {
    val wave = remember { Animatable(0f) }
    LaunchedEffect(Unit) { wave.animateTo(1f, tween(1_400, easing = FastOutSlowInEasing)) }
    val todayDate = LocalDate.ofEpochDay(today)
    val currentMonday = today - (todayDate.dayOfWeek.value - 1)
    val start = currentMonday - (weeks - 1) * 7L
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(weeks / 7f * 0.98f)) {
            val gap = 3.dp.toPx()
            val cell = (size.width - gap * (weeks - 1)) / weeks
            val corner = CornerRadius(cell * 0.28f)
            val progress = wave.value * (weeks + 6)
            for (week in 0 until weeks) {
                for (dow in 0 until 7) {
                    val day = start + week * 7 + dow
                    if (day > today) continue
                    val count = counts[day] ?: 0
                    val appear = (progress - week - dow * 0.3f).coerceIn(0f, 1f)
                    if (appear <= 0f) continue
                    val level = when {
                        count == 0 -> 0
                        count == 1 -> 1
                        count <= 3 -> 2
                        count <= 5 -> 3
                        else -> 4
                    }
                    val color = if (level == 0) {
                        Color.White.copy(alpha = 0.05f)
                    } else {
                        lerp(AscendColors.Violet, AscendColors.Cyan, (level - 1) / 3f).copy(alpha = 0.35f + 0.65f * level / 4f)
                    }
                    val s = cell * appear
                    val x = week * (cell + gap) + (cell - s) / 2f
                    val y = dow * (cell + gap) + (cell - s) / 2f
                    drawRoundRect(color, Offset(x, y), Size(s, s), corner)
                    if (day == today) {
                        drawRoundRect(Color.White.copy(alpha = 0.8f), Offset(x, y), Size(s, s), corner, style = Stroke(1.2.dp.toPx()))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Меньше", style = MaterialTheme.typography.labelSmall, color = AscendColors.TextMuted)
            Spacer(Modifier.width(6.dp))
            (0..4).forEach { level ->
                val color = if (level == 0) {
                    Color.White.copy(alpha = 0.05f)
                } else {
                    lerp(AscendColors.Violet, AscendColors.Cyan, (level - 1) / 3f).copy(alpha = 0.35f + 0.65f * level / 4f)
                }
                Box(
                    Modifier
                        .padding(horizontal = 1.5.dp)
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("Больше", style = MaterialTheme.typography.labelSmall, color = AscendColors.TextMuted)
        }
    }
}

/** Столбцы опыта за неделю, вырастают по очереди. */
@Composable
fun WeeklyBars(
    values: List<Long>,
    labels: List<String>,
    highlight: Int,
    modifier: Modifier = Modifier,
) {
    val max = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Row(
        modifier = modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            val target = value.toFloat() / max
            val grow = remember { Animatable(0f) }
            LaunchedEffect(target) {
                delay(index * 70L)
                grow.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 140f))
            }
            val active = index == highlight
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (value > 0) value.toString() else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) AscendColors.TextPrimary else AscendColors.TextMuted,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(20.dp)
                        .height((96 * target).dp.coerceAtLeast(4.dp))
                        .graphicsLayer {
                            scaleY = grow.value
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (value == 0L) {
                                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f)))
                            } else if (active) {
                                Brush.verticalGradient(listOf(AscendColors.Gold, AscendColors.Ember))
                            } else {
                                Brush.verticalGradient(listOf(AscendColors.Cyan, AscendColors.Violet))
                            },
                        ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    labels[index],
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) AscendColors.Gold else AscendColors.TextMuted,
                )
            }
        }
    }
}
