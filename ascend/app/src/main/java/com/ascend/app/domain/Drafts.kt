package com.ascend.app.domain

import com.ascend.core.Attribute
import com.ascend.core.Difficulty
import com.ascend.core.QuestKind
import com.ascend.core.WeekSchedule

data class QuestDraft(
    val id: Long = 0,
    val title: String,
    val emoji: String,
    val kind: QuestKind,
    val attribute: Attribute,
    val difficulty: Difficulty,
    val target: Int? = null,
    val unit: String? = null,
    val scheduleMask: Int = WeekSchedule.EVERY_DAY.mask,
    val dueDay: Long? = null,
    val timeMinutes: Int? = null,
    val bookId: Long? = null,
    val note: String? = null,
)

data class BookDraft(
    val id: Long = 0,
    val title: String,
    val author: String,
    val totalPages: Int,
    val currentPage: Int,
    val palette: Int,
    /** Для новой книги: создать ежедневный квест чтения на столько страниц. */
    val dailyPages: Int? = null,
)
