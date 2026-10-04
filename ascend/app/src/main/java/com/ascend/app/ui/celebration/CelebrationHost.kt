package com.ascend.app.ui.celebration

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Корневой слой празднований поверх всего приложения. */
@Composable
fun CelebrationHost(
    state: CelebrationState,
    onUndo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = state.current
    val overlay = current as? CelebrationItem.LevelUp
    val toast = current?.takeUnless { it is CelebrationItem.LevelUp }

    Box(modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = overlay,
            transitionSpec = {
                (fadeIn(tween(350)) + scaleIn(initialScale = 1.08f, animationSpec = tween(450))) togetherWith
                    (fadeOut(tween(300)) + scaleOut(targetScale = 0.96f, animationSpec = tween(300)))
            },
            contentKey = { it?.id },
            label = "overlay",
        ) { item ->
            if (item != null) {
                LevelUpOverlay(item, onContinue = { state.dismiss(item.id) })
            } else {
                Box(Modifier)
            }
        }

        AnimatedContent(
            targetState = toast,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            transitionSpec = {
                (
                    slideInVertically(spring(dampingRatio = 0.68f, stiffness = 320f)) { -it - 60 } +
                        fadeIn(tween(220)) +
                        scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f))
                    ) togetherWith (
                    slideOutVertically(tween(260)) { -it - 60 } + fadeOut(tween(200))
                    ) using SizeTransform(clip = false)
            },
            contentKey = { it?.id },
            label = "toast",
        ) { item ->
            if (item == null) {
                Box(Modifier)
            } else {
                LaunchedEffect(item.id) {
                    delay(item.durationMillis)
                    state.dismiss(item.id)
                }
                val dismiss = { state.dismiss(item.id) }
                when (item) {
                    is CelebrationItem.Reward -> RewardToast(
                        outcome = item.outcome,
                        onUndo = {
                            onUndo(item.outcome.logId)
                            dismiss()
                        },
                        onDismiss = dismiss,
                    )
                    is CelebrationItem.Milestone -> MilestoneToast(item, onDismiss = dismiss)
                    is CelebrationItem.AchievementUnlocked -> AchievementToast(item.achievement, onDismiss = dismiss)
                    is CelebrationItem.Purchase -> PurchaseToast(item, onDismiss = dismiss)
                    is CelebrationItem.LevelUp -> Box(Modifier)
                }
            }
        }
    }
}
