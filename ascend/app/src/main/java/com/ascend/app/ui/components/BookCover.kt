package com.ascend.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.theme.BookPalettes

/** Сгенерированная обложка: градиент, корешок с тенью, орнамент и название. */
@Composable
fun BookCover(
    title: String,
    author: String,
    palette: Int,
    modifier: Modifier = Modifier,
    showText: Boolean = true,
) {
    val colors = BookPalettes.of(palette)
    val shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 12.dp, bottomEnd = 12.dp)
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(0.68f)
            .clip(shape)
            .background(Brush.linearGradient(colors, start = Offset.Zero, end = Offset(0f, Float.POSITIVE_INFINITY)))
            .drawWithContent {
                val w = size.width
                val h = size.height
                // Орнамент — концентрические дуги в углу
                for (i in 1..4) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.07f),
                        radius = w * 0.22f * i,
                        center = Offset(w * 1.02f, h * 0.02f),
                        style = Stroke(1.dp.toPx()),
                    )
                }
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))),
                    topLeft = Offset(0f, h * 0.55f),
                    size = Size(w, h * 0.45f),
                )
                drawLine(Color.White.copy(alpha = 0.18f), Offset(w * 0.14f, h * 0.80f), Offset(w * 0.92f, h * 0.80f), 1.dp.toPx())
                drawContent()
                // Корешок
                val spine = w * 0.075f
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.05f)),
                        startX = 0f,
                        endX = spine,
                    ),
                    size = Size(spine, h),
                )
                drawLine(Color.White.copy(alpha = 0.22f), Offset(spine, 0f), Offset(spine, h), 1.dp.toPx())
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                        start = Offset(spine, 0f),
                        end = Offset(w * 0.6f, h * 0.4f),
                    ),
                )
            },
    ) {
        if (showText) {
            val base = maxWidth.value
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = (base * 0.13f).dp, end = (base * 0.07f).dp, top = (base * 0.10f).dp, bottom = (base * 0.08f).dp),
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (base * 0.115f).sp,
                        lineHeight = (base * 0.135f).sp,
                        shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 6f),
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = author,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.78f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (base * 0.075f).sp,
                        letterSpacing = 0.3.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
