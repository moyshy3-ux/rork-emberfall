package com.rork.emberfall.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.emberfall.R

/** Emberfall's night-forest palette. Light is the only source of warmth. */
object Ember {
    val Ink = Color(0xFF0B0E14)
    val Forest = Color(0xFF15201C)
    val Moss = Color(0xFF24402F)
    val MossLit = Color(0xFF35603F)
    val Bark = Color(0xFF3A2A22)
    val Fog = Color(0xFF1C2733)
    val Dirt = Color(0xFF4A3A2A)
    val DirtLit = Color(0xFF5E4A34)
    val Stone = Color(0xFF4A5450)
    val StoneDark = Color(0xFF2C3532)

    val Lantern = Color(0xFFF2B23A)
    val Fire = Color(0xFFE2542A)
    val Blood = Color(0xFFC0392B)
    val Spectral = Color(0xFF4FD1C5)

    val Bone = Color(0xFFEDE3D2)
    val BoneDim = Color(0xFF9AA6A0)
    val HotWhite = Color(0xFFFFF3D6)
}

val PixelFont = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))

private val EmberColors = darkColorScheme(
    primary = Ember.Fire,
    onPrimary = Ember.HotWhite,
    secondary = Ember.Lantern,
    onSecondary = Ember.Ink,
    tertiary = Ember.Spectral,
    background = Ember.Ink,
    onBackground = Ember.Bone,
    surface = Ember.Forest,
    onSurface = Ember.Bone,
    surfaceVariant = Ember.StoneDark,
    onSurfaceVariant = Ember.BoneDim,
    error = Ember.Blood,
)

private val EmberTypography = Typography(
    displaySmall = TextStyle(fontFamily = PixelFont, fontSize = 20.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = PixelFont, fontSize = 14.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = PixelFont, fontSize = 11.sp, lineHeight = 18.sp),
    bodyMedium = TextStyle(fontFamily = PixelFont, fontSize = 9.sp, lineHeight = 16.sp),
    bodySmall = TextStyle(fontFamily = PixelFont, fontSize = 7.sp, lineHeight = 13.sp),
    labelLarge = TextStyle(fontFamily = PixelFont, fontSize = 10.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = PixelFont, fontSize = 6.sp, lineHeight = 11.sp),
)

/** Hard pixel corners everywhere — no modern rounded cards over the world. */
private val PixelShapes = androidx.compose.material3.Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EmberColors,
        typography = EmberTypography,
        shapes = PixelShapes,
        content = content
    )
}
