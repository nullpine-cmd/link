package com.ascend.app.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.GameRepository
import com.ascend.app.data.TimeProvider
import com.ascend.app.domain.Celebration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Шина праздничных событий: экраны сообщают о наградах, корневой слой их показывает. */
class CelebrationBus {
    private val events = MutableSharedFlow<Celebration>(extraBufferCapacity = 16)
    val flow: SharedFlow<Celebration> = events.asSharedFlow()

    fun emit(celebration: Celebration) {
        events.tryEmit(celebration)
    }
}

/** Ручное внедрение зависимостей: граф маленький и прозрачный, без кодогенерации. */
class AppContainer(
    val repository: GameRepository,
    val time: TimeProvider,
    val celebrations: CelebrationBus = CelebrationBus(),
    val reminders: ReminderScheduler = ReminderScheduler.None,
)

/** Планировщик ежедневного напоминания (реализация — в Android-слое). */
interface ReminderScheduler {
    fun schedule(hour: Int, minute: Int)
    fun cancel()

    object None : ReminderScheduler {
        override fun schedule(hour: Int, minute: Int) = Unit
        override fun cancel() = Unit
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer не предоставлен") }

/** ViewModel с зависимостями из контейнера. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalAppContainer.current
    val factory = remember(container) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = create(container) as T
        }
    }
    return viewModel(key = key, factory = factory)
}

/** Запрос разрешения на уведомления (на Android 13+ — системный диалог). */
fun interface NotificationPermissionRequester {
    fun request(onResult: (Boolean) -> Unit)
}

val LocalNotificationPermission = staticCompositionLocalOf { NotificationPermissionRequester { it(true) } }
