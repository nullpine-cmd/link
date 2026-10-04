package com.ascend.app.ui.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ascend.app.data.local.BookEntity
import com.ascend.app.di.appViewModel
import com.ascend.app.domain.BookDraft
import com.ascend.app.ui.components.AscendSheet
import com.ascend.app.ui.components.AscendTextField
import com.ascend.app.ui.components.BookCover
import com.ascend.app.ui.components.CircleIconButton
import com.ascend.app.ui.components.GradientButton
import com.ascend.app.ui.components.SectionLabel
import com.ascend.app.ui.components.SelectChip
import com.ascend.app.ui.components.bounceClick
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.BookPalettes

/** Добавление и редактирование книги с живым предпросмотром обложки. */
@Composable
fun BookEditorSheet(initial: BookEntity?, onDismiss: () -> Unit) {
    val viewModel = appViewModel(key = "bookEditor") { BookEditorViewModel(it.repository) }
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var author by rememberSaveable { mutableStateOf(initial?.author.orEmpty()) }
    var pages by rememberSaveable { mutableStateOf(initial?.totalPages?.toString().orEmpty()) }
    var current by rememberSaveable { mutableStateOf(initial?.currentPage?.takeIf { it > 0 }?.toString().orEmpty()) }
    var palette by rememberSaveable { mutableIntStateOf(initial?.palette ?: (0 until BookPalettes.palettes.size).random()) }
    var dailyQuest by rememberSaveable { mutableStateOf(initial == null) }
    var dailyPages by rememberSaveable { mutableIntStateOf(20) }

    val totalPages = pages.toIntOrNull() ?: 0
    val valid = title.isNotBlank() && totalPages > 0

    AscendSheet(onDismiss = onDismiss) { hide ->
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (initial == null) "Новая книга" else "Изменить книгу",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AscendColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                CircleIconButton(Icons.Rounded.Close, "Закрыть", hide, diameter = 40.dp)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Top) {
                BookCover(
                    title = title.ifBlank { "Название" },
                    author = author.ifBlank { "Автор" },
                    palette = palette,
                    modifier = Modifier.width(112.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AscendTextField(title, { title = it }, "Название", accent = AscendColors.Cyan)
                    AscendTextField(author, { author = it }, "Автор", accent = AscendColors.Cyan)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AscendTextField(
                    value = pages,
                    onValueChange = { value -> pages = value.filter { it.isDigit() }.take(5) },
                    placeholder = "Всего страниц",
                    accent = AscendColors.Cyan,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                AscendTextField(
                    value = current,
                    onValueChange = { value -> current = value.filter { it.isDigit() }.take(5) },
                    placeholder = "Уже прочитано",
                    accent = AscendColors.Cyan,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(18.dp))
            SectionLabel("Обложка")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(BookPalettes.palettes.indices.toList()) { index ->
                    val selected = index == palette
                    val scale by animateFloatAsState(
                        if (selected) 1.08f else 0.94f,
                        spring(dampingRatio = 0.5f, stiffness = 400f),
                        label = "paletteScale",
                    )
                    Box(
                        Modifier
                            .width(48.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .bounceClick { palette = index }
                            .border(
                                2.dp,
                                if (selected) Color.White else Color.Transparent,
                                RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 12.dp, bottomEnd = 12.dp),
                            ),
                    ) {
                        BookCover("", "", index, Modifier.width(48.dp), showText = false)
                    }
                }
            }

            if (initial == null) {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Ежедневный квест чтения", style = MaterialTheme.typography.titleMedium, color = AscendColors.TextPrimary)
                        Text(
                            "Прочитаешь хоть страницу — квест засчитается",
                            style = MaterialTheme.typography.bodySmall,
                            color = AscendColors.TextMuted,
                        )
                    }
                    Switch(
                        checked = dailyQuest,
                        onCheckedChange = { dailyQuest = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AscendColors.Cyan,
                            uncheckedThumbColor = AscendColors.TextMuted,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.06f),
                            uncheckedBorderColor = Color.White.copy(alpha = 0.12f),
                        ),
                    )
                }
                AnimatedVisibility(
                    visible = dailyQuest,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(Modifier.padding(top = 10.dp)) {
                        Text("Цель на день, страниц", style = MaterialTheme.typography.labelMedium, color = AscendColors.TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(5, 10, 20, 30, 50)) { value ->
                                SelectChip(
                                    "$value",
                                    selected = dailyPages == value,
                                    color = AscendColors.Cyan,
                                    onClick = { dailyPages = value },
                                )
                            }
                        }
                        if (totalPages > 0) {
                            val days = (totalPages - (current.toIntOrNull() ?: 0)).coerceAtLeast(0) / dailyPages.coerceAtLeast(1) + 1
                            Text(
                                "≈ $days дн. до последней страницы — в своём темпе",
                                style = MaterialTheme.typography.bodySmall,
                                color = AscendColors.TextMuted,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            GradientButton(
                text = if (initial == null) "Поставить на полку" else "Сохранить",
                icon = Icons.Rounded.Check,
                enabled = valid,
                colors = listOf(AscendColors.Cyan, AscendColors.Indigo, AscendColors.Violet),
                onClick = {
                    viewModel.save(
                        BookDraft(
                            id = initial?.id ?: 0,
                            title = title,
                            author = author,
                            totalPages = totalPages,
                            currentPage = current.toIntOrNull() ?: 0,
                            palette = palette,
                            dailyPages = if (initial == null && dailyQuest) dailyPages else null,
                        ),
                    )
                    hide()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
