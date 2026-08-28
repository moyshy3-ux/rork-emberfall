package com.rork.emberfall.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rork.emberfall.game.HeroFront
import com.rork.emberfall.ui.theme.Ember
import kotlin.math.roundToInt

/** Pixel portrait chip cut from the hero sprite's head. */
@Composable
fun HeroPortrait(size: Int, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size.dp)) {
        val image = HeroFront.image(false)
        drawRect(Ember.Ink)
        drawRect(Color(0xFF3A1F1A), topLeft = Offset(2f, 2f), size = Size(this.size.width - 4f, this.size.height - 4f))
        val srcH = 8
        val inset = 3f
        drawImage(
            image = image,
            srcOffset = IntOffset(2, 0),
            srcSize = IntSize(10, srcH),
            dstOffset = IntOffset(inset.roundToInt(), inset.roundToInt()),
            dstSize = IntSize(
                (this.size.width - inset * 2).roundToInt(),
                (this.size.height - inset * 2).roundToInt(),
            ),
            filterQuality = FilterQuality.None,
        )
        drawRect(Ember.Bark, style = Stroke(width = 3f))
        drawRect(Ember.BoneDim.copy(alpha = 0.35f), style = Stroke(width = 1f))
    }
}

/** Hard-edged pixel bar with an outlined frame and an inner segment lattice. */
private fun DrawScope.pixelBar(
    fraction: Float,
    fill: Color,
    fillDim: Color,
    track: Color,
    segments: Int,
) {
    val w = size.width
    val h = size.height
    drawRect(Ember.Ink, size = Size(w, h))
    drawRect(track, topLeft = Offset(2f, 2f), size = Size(w - 4f, h - 4f))
    val innerW = (w - 6f) * fraction.coerceIn(0f, 1f)
    if (innerW > 0f) {
        drawRect(fillDim, topLeft = Offset(3f, 3f), size = Size(innerW, h - 6f))
        drawRect(fill, topLeft = Offset(3f, 3f), size = Size(innerW, (h - 6f) * 0.55f))
    }
    // segment ticks
    val step = (w - 6f) / segments
    for (i in 1 until segments) {
        drawRect(Ember.Ink.copy(alpha = 0.55f), topLeft = Offset(3f + step * i, 3f), size = Size(1.5f, h - 6f))
    }
    drawRect(Ember.BoneDim.copy(alpha = 0.30f), style = Stroke(width = 1.5f))
}

/**
 * Floating combat HUD. Text values recompose only when the numbers change;
 * the bars redraw every frame from [tick] so they slide smoothly.
 */
@Composable
fun CombatHud(
    hp: Int,
    maxHp: Int,
    level: Int,
    gold: Int,
    hpFraction: () -> Float,
    xpFraction: () -> Float,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HeroPortrait(size = 46)
            Text(
                text = "Lv $level",
                style = MaterialTheme.typography.bodySmall,
                color = Ember.Bone,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Column(
            modifier = Modifier.padding(start = 6.dp, top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                Canvas(
                    Modifier
                        .width(150.dp)
                        .height(20.dp)
                ) {
                    pixelBar(
                        fraction = hpFraction(),
                        fill = Color(0xFFE0574A),
                        fillDim = Ember.Blood,
                        track = Color(0xFF2A1512),
                        segments = 8,
                    )
                }
                Text(
                    text = "HP $hp/$maxHp",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ember.HotWhite,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Canvas(
                Modifier
                    .width(150.dp)
                    .height(8.dp)
            ) {
                pixelBar(
                    fraction = xpFraction(),
                    fill = Color(0xFFFFD470),
                    fillDim = Ember.Lantern,
                    track = Color(0xFF2A2415),
                    segments = 8,
                )
            }
        }
    }
}

@Composable
fun GoldCounter(gold: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(18.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f
            drawCircle(Ember.Ink, r, c)
            drawCircle(Color(0xFFC98A1E), r * 0.86f, c)
            drawCircle(Ember.Lantern, r * 0.66f, c)
            drawRect(
                Color(0xFFFFF3D6),
                topLeft = Offset(c.x - r * 0.5f, c.y - r * 0.42f),
                size = Size(r * 0.42f, r * 0.3f),
            )
        }
        Text(
            text = "$gold",
            style = MaterialTheme.typography.bodyMedium,
            color = Ember.Bone,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
