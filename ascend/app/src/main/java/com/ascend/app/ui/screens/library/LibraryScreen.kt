package com.ascend.app.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.data.local.BookEntity
import com.ascend.app.data.local.isFinished
import com.ascend.app.data.local.progress
import com.ascend.app.di.appViewModel
import com.ascend.app.ui.components.BookCover
import com.ascend.app.ui.components.BottomBarSpace
import com.ascend.app.ui.components.CircleIconButton
import com.ascend.app.ui.components.EmptyState
import com.ascend.app.ui.components.GlowProgressBar
import com.ascend.app.ui.components.GradientButton
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.components.SectionLabel
import com.ascend.app.ui.components.StatTile
import com.ascend.app.ui.components.bounceClick
import com.ascend.app.ui.components.reveal
import com.ascend.app.ui.theme.AscendColors

@Composable
fun LibraryRoute(onOpenBook: (Long) -> Unit) {
    val viewModel = appViewModel { LibraryViewModel(it.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    LibraryScreen(state = state, onOpenBook = onOpenBook, onAdd = { adding = true })
    if (adding) {
        BookEditorSheet(initial = null, onDismiss = { adding = false })
    }
}

@Composable
fun LibraryScreen(state: LibraryUiState, onOpenBook: (Long) -> Unit, onAdd: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomBarSpace + 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        fullWidth("header") {
            Row(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 4.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Библиотека", style = MaterialTheme.typography.headlineLarge, color = AscendColors.TextPrimary)
                    Text(
                        "Каждая страница прокачивает интеллект",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AscendColors.TextSecondary,
                    )
                }
                CircleIconButton(Icons.Rounded.Add, "Добавить книгу", onAdd, tint = AscendColors.Cyan)
            }
        }
        fullWidth("stats") {
            Row(Modifier.reveal(1), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(state.reading.size.toLong(), "читаю", Icons.Rounded.AutoStories, AscendColors.Cyan, Modifier.weight(1f))
                StatTile(state.finished.size.toLong(), "прочитано", Icons.Rounded.TaskAlt, AscendColors.Success, Modifier.weight(1f))
                StatTile(state.pagesRead, "страниц", Icons.Rounded.MenuBook, AscendColors.Gold, Modifier.weight(1f))
            }
        }
        if (state.isEmpty && !state.loading) {
            fullWidth("empty") {
                EmptyState(
                    emoji = "📚",
                    title = "Полка ждёт первую книгу",
                    subtitle = "Добавь книгу и поставь посильный квест. Даже одна страница в день — настоящий прогресс",
                ) {
                    GradientButton("Добавить книгу", onClick = onAdd, icon = Icons.Rounded.Add)
                }
            }
        }
        if (state.reading.isNotEmpty()) {
            fullWidth("reading-label") { SectionLabel("Читаю сейчас", Modifier.padding(top = 4.dp)) }
            fullWidth("reading") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(state.reading, key = { _, book -> book.id }) { index, book ->
                        ReadingCard(book, onClick = { onOpenBook(book.id) }, modifier = Modifier.reveal(index + 2))
                    }
                }
            }
        }
        shelf("Хочу прочитать", state.planned, onOpenBook)
        shelf("Прочитано", state.finished, onOpenBook)
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

private fun LazyGridScope.shelf(title: String, books: List<BookEntity>, onOpenBook: (Long) -> Unit) {
    if (books.isEmpty()) return
    fullWidth("label-$title") { SectionLabel(title, Modifier.padding(top = 6.dp)) }
    itemsIndexed(books, key = { _, book -> "book-${book.id}" }) { index, book ->
        Column(
            Modifier
                .reveal(index)
                .bounceClick { onOpenBook(book.id) },
        ) {
            Box {
                BookCover(book.title, book.author, book.palette, Modifier.fillMaxWidth())
                if (book.isFinished) {
                    Pill("✓", AscendColors.Success, filled = true, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                book.title,
                style = MaterialTheme.typography.labelLarge,
                color = AscendColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                book.author.ifBlank { "${book.totalPages} стр." },
                style = MaterialTheme.typography.labelSmall,
                color = AscendColors.TextMuted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ReadingCard(book: BookEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(150.dp)
            .bounceClick(onClick = onClick),
    ) {
        Box {
            BookCover(book.title, book.author, book.palette, Modifier.width(150.dp))
            Pill(
                "${(book.progress * 100).toInt()}%",
                AscendColors.Cyan,
                filled = true,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        GlowProgressBar(book.progress, height = 6.dp, colors = listOf(AscendColors.Cyan, AscendColors.Violet))
        Spacer(Modifier.height(6.dp))
        Text(
            "стр. ${book.currentPage} из ${book.totalPages}",
            style = MaterialTheme.typography.labelMedium,
            color = AscendColors.TextSecondary,
        )
    }
}
