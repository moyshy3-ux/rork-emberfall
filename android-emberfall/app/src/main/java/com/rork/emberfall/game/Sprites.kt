package com.rork.emberfall.game

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * A hand-authored pixel sprite: rows of palette keys, '.' meaning transparent.
 * Converted once to an [ImageBitmap] and blitted with nearest-neighbour scaling.
 */
class PixelSprite(
    private val rows: List<String>,
    private val palette: Map<Char, Int>,
) {
    val width: Int = rows.firstOrNull()?.length ?: 0
    val height: Int = rows.size

    private val cache = HashMap<Boolean, ImageBitmap>()

    /** @param flipX mirror horizontally (used for left-facing variants). */
    fun image(flipX: Boolean = false): ImageBitmap = cache.getOrPut(flipX) { build(flipX) }

    private fun build(flipX: Boolean): ImageBitmap {
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val row = rows[y]
            for (x in 0 until width) {
                val srcX = if (flipX) width - 1 - x else x
                val key = row[srcX]
                pixels[y * width + x] = if (key == '.') 0 else palette[key] ?: 0
            }
        }
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp.asImageBitmap()
    }
}

private const val OUTLINE = 0xFF07080B.toInt()

private val heroPalette = mapOf(
    'k' to OUTLINE,
    'h' to 0xFF6B3A22.toInt(),
    'H' to 0xFF8B4A2A.toInt(),
    's' to 0xFFC98A5E.toInt(),
    'd' to 0xFF8B5A3A.toInt(),
    'a' to 0xFF2A3038.toInt(),
    'A' to 0xFF4A5560.toInt(),
    'c' to 0xFF8E2B22.toInt(),
    'C' to 0xFFB8392A.toInt(),
    'b' to 0xFF4A3222.toInt(),
    'B' to 0xFF6B4A2E.toInt(),
)

/** Warrior facing the camera. */
val HeroFront = PixelSprite(
    listOf(
        "....kkkkkk....",
        "...khhhhhhk...",
        "...khhhhhhk...",
        "...khssssHk...",
        "...kskssksk...",
        "...kssssssk...",
        "...kssddssk...",
        ".ccAAAAAAAAcc.",
        ".ccAaaaaaaAcc.",
        ".CcAaaaaaaAcC.",
        ".CcAaaaaaaAcC.",
        "..cAaaaaaaAc..",
        "..cbBbbbbBbc..",
        "...bbbbbbbb...",
        "...bb....bb...",
        "...bb....bb...",
        "...BB....BB...",
        "..kBBk..kBBk..",
    ),
    heroPalette,
)

/** Warrior seen from behind — cloak covers the body. */
val HeroBack = PixelSprite(
    listOf(
        "....kkkkkk....",
        "...khhhhhhk...",
        "...khhhhhhk...",
        "...khhhhhhk...",
        "...khHHHHhk...",
        "...khhhhhhk...",
        "...khhhhhhk...",
        ".ccCCCCCCCCcc.",
        ".cCCCCCCCCCCc.",
        ".cCCCCCCCCCCc.",
        ".cCCcCCCCcCCc.",
        "..cCCCCCCCCc..",
        "..ccCCCCCCcc..",
        "...cccccccc...",
        "...bb....bb...",
        "...bb....bb...",
        "...BB....BB...",
        "..kBBk..kBBk..",
    ),
    heroPalette,
)

/** Warrior in profile, facing right. Mirror for left. */
val HeroSide = PixelSprite(
    listOf(
        "....kkkkkk....",
        "...khhhhhhk...",
        "...khhhhhhk...",
        "...khhssssk...",
        "...khhskssk...",
        "...khhsssdk...",
        "...khhssdsk...",
        "..ccAAAAAAk...",
        ".ccCAaaaaAk...",
        ".ccCAaaaaAk...",
        "..cCAaaaaAk...",
        "..ccAaaaaAk...",
        "...cbBbbbBk...",
        "...kbbbbbbk...",
        "...kbbkbbk....",
        "...kbbkbbk....",
        "...kBBkBBk....",
        "..kBBBkBBBk...",
    ),
    heroPalette,
)

private val wolfPalette = mapOf(
    'k' to OUTLINE,
    'f' to 0xFF23252B.toInt(),
    'F' to 0xFF3A3D45.toInt(),
    'e' to 0xFFE2542A.toInt(),
    't' to 0xFFEDE3D2.toInt(),
)

/** Wolf in profile, facing right. */
val WolfSprite = PixelSprite(
    listOf(
        "..........kk........kk..",
        ".kk......kffk......kffk.",
        "kffk....kfFFfkkkkkkfFfk.",
        "kfFfk..kfFFFFFFFFFFFFfk.",
        ".kfFfkkfFFffffffffffFfk.",
        "..kfFFFFffkeffkffffffk..",
        "..kffFFffffffkfffffffk..",
        "...kffffkttkffffffffk...",
        "...kffkkkkkkffkffffk....",
        "...kfk......kfk.kffk....",
        "...kfk......kfk.kffk....",
        "...kkk......kkk.kkkk....",
    ),
    wolfPalette,
)

private val slimePalette = mapOf(
    'k' to OUTLINE,
    'g' to 0xFF1F5E5C.toInt(),
    'G' to 0xFF2E8F86.toInt(),
    'l' to 0xFF4FD1C5.toInt(),
    'h' to 0xFFBFF5EC.toInt(),
    'e' to 0xFF0B1F20.toInt(),
)

val SlimeSprite = PixelSprite(
    listOf(
        ".....kkkk.....",
        "...kkGGGGkk...",
        "..kGhhGGGGGk..",
        ".kGhhlGGGGGGk.",
        ".kGhlGGGGGGGk.",
        "kGGlGGGGGGGGGk",
        "kGGGGeGGeGGGGk",
        "kGGGGGGGGGGGGk",
        "kGgGGGGGGGGgGk",
        "kggGGGGGGGGggk",
        ".kggggggggggk.",
        "..kkkkkkkkkk..",
    ),
    slimePalette,
)

private val lanternPalette = mapOf(
    'k' to OUTLINE,
    'w' to 0xFF3A2A22.toInt(),
    'W' to 0xFF54402F.toInt(),
    'm' to 0xFF6B5A3A.toInt(),
    'f' to 0xFFF2B23A.toInt(),
    'F' to 0xFFFFF3D6.toInt(),
    'o' to 0xFFE2542A.toInt(),
)

/** Lantern hanging from a wooden post. */
val LanternSprite = PixelSprite(
    listOf(
        "kWWWWWWWWk..",
        "kWwwwwwwWk..",
        "kWWk...kmk..",
        "kWWk...kkk..",
        "kWWk..kmmmk.",
        "kWWk.kmfffmk",
        "kWWk.kffFffk",
        "kWWk.kfFFFfk",
        "kWWk.kffFffk",
        "kWWk.kfooffk",
        "kWWk.kmmmmmk",
        "kWWk..kkkkk.",
        "kWWk........",
        "kWWk........",
        "kWWk........",
        "kWWk........",
        "kWWk........",
        "kWWk........",
        "kWWk........",
        "kWWk........",
        "kWWk........",
        "kWWk........",
        "kwwk........",
        "kkkk........",
    ),
    lanternPalette,
)

private val mushroomPalette = mapOf(
    'k' to OUTLINE,
    'c' to 0xFF7A2A22.toInt(),
    'C' to 0xFFB8392A.toInt(),
    's' to 0xFFD9C8A8.toInt(),
    'S' to 0xFFF2E4C6.toInt(),
    'g' to 0xFFF2B23A.toInt(),
)

val MushroomSprite = PixelSprite(
    listOf(
        "..kkkk..",
        ".kCCCCk.",
        "kCCSCCCk",
        "kCSSCCCk",
        "kCCCCSCk",
        ".kkssskk",
        "..ksSsk.",
        "..kgsgk.",
        "..kkkk..",
    ),
    mushroomPalette,
)

private val grassPalette = mapOf(
    'k' to 0xFF16281C.toInt(),
    'g' to 0xFF24402F.toInt(),
    'G' to 0xFF35603F.toInt(),
)

val GrassTuft = PixelSprite(
    listOf(
        ".G...g..G.",
        ".G.G.g.GG.",
        "gG.G.G.Gg.",
        "gGgGgGgGgg",
        "kgggggggkk",
    ),
    grassPalette,
)
