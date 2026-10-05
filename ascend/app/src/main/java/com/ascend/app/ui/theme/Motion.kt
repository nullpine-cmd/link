package com.ascend.app.ui.theme

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState

/**
 * Настройки движения. [reduced] включается пользователем в настройках или системой
 * (отключённые анимации в параметрах разработчика / специальных возможностях):
 * бесконечные фоновые анимации останавливаются, остаются только короткие отклики на действия.
 */
data class MotionSettings(val reduced: Boolean = false)

val LocalMotion = compositionLocalOf { MotionSettings() }

/** Бесконечная анимация, которая при экономии движения замирает на [restValue]. */
@Composable
fun InfiniteTransition.animateFloatOrRest(
    initialValue: Float,
    targetValue: Float,
    animationSpec: InfiniteRepeatableSpec<Float>,
    label: String,
    restValue: Float = initialValue,
): State<Float> {
    val reduced = LocalMotion.current.reduced
    return if (reduced) {
        rememberUpdatedState(restValue)
    } else {
        animateFloat(initialValue = initialValue, targetValue = targetValue, animationSpec = animationSpec, label = label)
    }
}

/** Удобный флаг для веток «рисовать ли живой фон». */
@Composable
fun motionReduced(): Boolean = LocalMotion.current.reduced
