package com.ascend.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Display = FontFamily.Default

val AscendTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 64.sp, lineHeight = 68.sp, letterSpacing = (-2).sp),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1.2).sp),
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp),
)

/** Надписи-«руны»: мелкие, разреженные, капсом. */
val RuneLabel = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.2.sp)
