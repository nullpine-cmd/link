package com.ascend.app.di

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Ввод-вывод резервных копий через системный диалог выбора файла.
 * Реализация — в MainActivity (Storage Access Framework); здесь только контракт.
 */
interface BackupIo {
    /** Предлагает сохранить [json] в файл с именем [suggestedName]; сообщает, удалось ли. */
    fun export(suggestedName: String, json: String, onResult: (Boolean) -> Unit)

    /** Предлагает выбрать файл копии и возвращает его содержимое (null — отмена или ошибка чтения). */
    fun import(onResult: (String?) -> Unit)

    object None : BackupIo {
        override fun export(suggestedName: String, json: String, onResult: (Boolean) -> Unit) = onResult(false)
        override fun import(onResult: (String?) -> Unit) = onResult(null)
    }
}

val LocalBackupIo = staticCompositionLocalOf<BackupIo> { BackupIo.None }
