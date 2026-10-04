package com.ascend.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ascend.core.Attribute
import com.ascend.core.Rank
import com.ascend.core.Tier

object AscendColors {
    val Void = Color(0xFF06050D)
    val Night = Color(0xFF0B0918)
    val Surface = Color(0xFF131027)
    val SurfaceHigh = Color(0xFF1B1735)
    val SurfaceTop = Color(0xFF241F45)
    val Stroke = Color(0x1FFFFFFF)
    val StrokeStrong = Color(0x33FFFFFF)

    val TextPrimary = Color(0xFFF4F1FF)
    val TextSecondary = Color(0xFFADA7CC)
    val TextMuted = Color(0xFF6F6992)

    val Violet = Color(0xFF8B5CF6)
    val VioletLight = Color(0xFFB9A2FF)
    val Indigo = Color(0xFF5B5BF7)
    val Cyan = Color(0xFF22D3EE)
    val Pink = Color(0xFFF472B6)
    val Gold = Color(0xFFFFD25A)
    val GoldDeep = Color(0xFFF59E0B)
    val Ember = Color(0xFFFF7A45)
    val Success = Color(0xFF4ADE80)
    val Danger = Color(0xFFFF5D73)

    /** Градиент опыта — «мана» восхождения. */
    val Xp = listOf(Color(0xFF8B5CF6), Color(0xFF6A7CFF), Color(0xFF22D3EE))
    val GoldGradient = listOf(Color(0xFFFFE08A), Color(0xFFFFC23D), Color(0xFFF59E0B))
    val Fire = listOf(Color(0xFFFFE27A), Color(0xFFFF9F43), Color(0xFFFF5E3A))
}

val Attribute.color: Color
    get() = when (this) {
        Attribute.STRENGTH -> Color(0xFFFF6B4A)
        Attribute.INTELLECT -> Color(0xFF4FA3FF)
        Attribute.VITALITY -> Color(0xFF4ADE80)
        Attribute.SPIRIT -> Color(0xFFA98BFF)
        Attribute.CHARISMA -> Color(0xFFFF6FB5)
        Attribute.MASTERY -> Color(0xFFFFB547)
    }

val Attribute.colorDeep: Color
    get() = when (this) {
        Attribute.STRENGTH -> Color(0xFFD9342B)
        Attribute.INTELLECT -> Color(0xFF2E5BFF)
        Attribute.VITALITY -> Color(0xFF0EA371)
        Attribute.SPIRIT -> Color(0xFF6D3EF2)
        Attribute.CHARISMA -> Color(0xFFD9338A)
        Attribute.MASTERY -> Color(0xFFE67E00)
    }

val Attribute.gradient: List<Color> get() = listOf(color, colorDeep)

val Attribute.icon: ImageVector
    get() = when (this) {
        Attribute.STRENGTH -> Icons.Rounded.FitnessCenter
        Attribute.INTELLECT -> Icons.Rounded.AutoStories
        Attribute.VITALITY -> Icons.Rounded.Favorite
        Attribute.SPIRIT -> Icons.Rounded.SelfImprovement
        Attribute.CHARISMA -> Icons.Rounded.Forum
        Attribute.MASTERY -> Icons.Rounded.RocketLaunch
    }

val Rank.colors: List<Color>
    get() = when (this) {
        Rank.E -> listOf(Color(0xFFCBD5E1), Color(0xFF64748B))
        Rank.D -> listOf(Color(0xFF86EFAC), Color(0xFF16A34A))
        Rank.C -> listOf(Color(0xFF67E8F9), Color(0xFF0891B2))
        Rank.B -> listOf(Color(0xFF93C5FD), Color(0xFF2563EB))
        Rank.A -> listOf(Color(0xFFD8B4FE), Color(0xFF7C3AED))
        Rank.S -> listOf(Color(0xFFFFE08A), Color(0xFFF59E0B))
        Rank.SS -> listOf(Color(0xFFFFA4B4), Color(0xFFE11D48))
        Rank.SSS -> listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFFD93D),
            Color(0xFF6BCB77),
            Color(0xFF4D96FF),
            Color(0xFFB15CFF),
            Color(0xFFFF6B6B),
        )
    }

val Rank.color: Color get() = colors.first()

val Tier.colors: List<Color>
    get() = when (this) {
        Tier.BRONZE -> listOf(Color(0xFFFFC6A0), Color(0xFFB8653A))
        Tier.SILVER -> listOf(Color(0xFFF1F5F9), Color(0xFF8A9BB3))
        Tier.GOLD -> listOf(Color(0xFFFFE9A3), Color(0xFFE0A100))
        Tier.LEGENDARY -> listOf(Color(0xFFFFB1F2), Color(0xFF8B5CF6), Color(0xFF22D3EE))
    }

/** Цвет ауры героя — выбирается при создании персонажа. */
object Auras {
    val palettes: List<List<Color>> = listOf(
        listOf(Color(0xFF8B5CF6), Color(0xFF22D3EE)),
        listOf(Color(0xFFFF7A45), Color(0xFFFFD25A)),
        listOf(Color(0xFF10B981), Color(0xFF22D3EE)),
        listOf(Color(0xFFF472B6), Color(0xFF8B5CF6)),
        listOf(Color(0xFF3B82F6), Color(0xFF6366F1)),
        listOf(Color(0xFFFFD25A), Color(0xFFFFFFFF)),
    )
    val names = listOf("Аркана", "Пламя", "Изумруд", "Сакура", "Лазурь", "Сияние")

    fun of(index: Int): List<Color> = palettes[Math.floorMod(index, palettes.size)]
}

/** Обложки книг — генерируются из градиентов, без загрузки картинок. */
object BookPalettes {
    val palettes: List<List<Color>> = listOf(
        listOf(Color(0xFF7C3AED), Color(0xFF2E1065)),
        listOf(Color(0xFF0EA5E9), Color(0xFF1E3A8A)),
        listOf(Color(0xFFF97316), Color(0xFF7C2D12)),
        listOf(Color(0xFF10B981), Color(0xFF064E3B)),
        listOf(Color(0xFFEC4899), Color(0xFF831843)),
        listOf(Color(0xFFEAB308), Color(0xFF713F12)),
        listOf(Color(0xFF64748B), Color(0xFF0F172A)),
        listOf(Color(0xFFEF4444), Color(0xFF450A0A)),
    )

    fun of(index: Int): List<Color> = palettes[Math.floorMod(index, palettes.size)]
}
