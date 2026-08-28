package com.rork.emberfall.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rork.emberfall.game.HudSnapshot
import com.rork.emberfall.ui.theme.Ember

/** Pixel-framed rest panel: what the Warrior has earned so far, saved locally. */
@Composable
fun RestPanel(hud: HudSnapshot, onResume: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .background(Color(0xFF10151A))
            .border(3.dp, Ember.Bark)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeroPortrait(size = 58)
            Column(Modifier.padding(start = 12.dp)) {
                Text("WARRIOR", style = MaterialTheme.typography.titleLarge, color = Ember.Lantern)
                Text(
                    "Level ${hud.level}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ember.BoneDim,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        StatRow("XP", "${hud.xp} / ${hud.xpToNext}", Ember.Lantern)
        StatRow("Gold", "${hud.gold}", Ember.Bone)
        StatRow("Slain", "${hud.kills}", Ember.Bone)

        LabeledBar(
            label = "HP ${hud.hp} / ${hud.maxHp}",
            labelColor = Color(0xFFE0574A),
            fraction = hud.hp.toFloat() / hud.maxHp.coerceAtLeast(1),
            fill = Color(0xFFE0574A),
            fillDim = Ember.Blood,
            track = Color(0xFF2A1512),
        )
        LabeledBar(
            label = "XP",
            labelColor = Ember.Lantern,
            fraction = hud.xp.toFloat() / hud.xpToNext.coerceAtLeast(1),
            fill = Color(0xFFFFD470),
            fillDim = Ember.Lantern,
            track = Color(0xFF2A2415),
        )

        Button(
            onClick = onResume,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Ember.Fire,
                contentColor = Ember.HotWhite,
            ),
        ) {
            Text("Return to the Hollow", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "Progress saved",
            style = MaterialTheme.typography.bodySmall,
            color = Ember.BoneDim.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF171D22))
            .border(2.dp, Color(0xFF2A3238))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ember.BoneDim)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

@Composable
private fun LabeledBar(
    label: String,
    labelColor: Color,
    fraction: Float,
    fill: Color,
    fillDim: Color,
    track: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF171D22))
            .border(2.dp, Color(0xFF2A3238))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = labelColor)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            drawSegments(fraction, fill, fillDim, track)
        }
    }
}

private fun DrawScope.drawSegments(fraction: Float, fill: Color, fillDim: Color, track: Color) {
    val segments = 8
    val gap = 4f
    val segW = (size.width - gap * (segments - 1)) / segments
    val filled = fraction.coerceIn(0f, 1f) * segments
    for (i in 0 until segments) {
        val x = i * (segW + gap)
        drawRect(track, topLeft = Offset(x, 0f), size = Size(segW, size.height))
        val amount = (filled - i).coerceIn(0f, 1f)
        if (amount > 0f) {
            drawRect(fillDim, topLeft = Offset(x, 0f), size = Size(segW * amount, size.height))
            drawRect(fill, topLeft = Offset(x, 0f), size = Size(segW * amount, size.height * 0.5f))
        }
        drawRect(
            Ember.Ink.copy(alpha = 0.6f),
            topLeft = Offset(x, 0f),
            size = Size(segW, size.height),
            style = Stroke(width = 1.5f),
        )
    }
}
