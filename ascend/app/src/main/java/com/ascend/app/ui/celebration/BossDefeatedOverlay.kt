package com.ascend.app.ui.celebration

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.components.ConfettiRain
import com.ascend.app.ui.components.GradientButton
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.RuneLabel

/** Победа над боссом недели. Базовая версия церемонии — визуальная проработка в отдельном шаге. */
@Composable
fun BossDefeatedOverlay(item: CelebrationItem.BossDefeated, onContinue: () -> Unit) {
    BackHandler(onBack = onContinue)
    val boss = item.boss
    Box(
        Modifier
            .fillMaxSize()
            .background(AscendColors.Void.copy(alpha = 0.92f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        ConfettiRain(trigger = 1)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text("БОСС ПОВЕРЖЕН", style = RuneLabel.copy(fontSize = 14.sp, letterSpacing = 8.sp), color = AscendColors.Gold)
            Spacer(Modifier.height(18.dp))
            Text(boss.emoji, fontSize = 96.sp)
            Spacer(Modifier.height(12.dp))
            Text(boss.name, style = MaterialTheme.typography.headlineMedium, color = AscendColors.TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "+${boss.rewardXp} XP · +${boss.rewardGold} золота",
                style = MaterialTheme.typography.titleMedium,
                color = AscendColors.Cyan,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            GradientButton(
                text = "Забрать трофей",
                onClick = onContinue,
                colors = AscendColors.GoldGradient,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}
