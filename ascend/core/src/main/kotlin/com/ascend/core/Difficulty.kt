package com.ascend.core

/** Сложность квеста определяет базовую награду. Любой прогресс приносит её целиком. */
enum class Difficulty(val title: String, val baseXp: Int, val stars: Int) {
    EASY("Лёгкий", 10, 1),
    NORMAL("Обычный", 20, 2),
    HARD("Сложный", 35, 3),
    EPIC("Эпический", 60, 4),
}
