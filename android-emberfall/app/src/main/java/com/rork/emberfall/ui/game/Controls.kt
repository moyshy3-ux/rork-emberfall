package com.rork.emberfall.ui.game

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rork.emberfall.ui.theme.Ember
import kotlin.math.hypot
import kotlin.math.min

/** Translucent thumb-stick. Reports a normalised direction; the world reads through it. */
@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    onMove: (Float, Float) -> Unit,
) {
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }
    var active by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(148.dp)
            .semantics { contentDescription = "Movement stick" }
            .pointerInput(Unit) {
                val maxR = size.width / 2f * 0.60f
                val center = Offset(size.width / 2f, size.height / 2f)

                fun apply(pos: Offset) {
                    val dx = pos.x - center.x
                    val dy = pos.y - center.y
                    val d = hypot(dx, dy)
                    val clamped = min(d, maxR)
                    val nx = if (d > 0.001f) dx / d else 0f
                    val ny = if (d > 0.001f) dy / d else 0f
                    knobX = nx * clamped
                    knobY = ny * clamped
                    val strength = (clamped / maxR).coerceIn(0f, 1f)
                    onMove(nx * strength, ny * strength)
                }

                awaitEachGesture {
                    val down = awaitFirstDown()
                    active = true
                    apply(down.position)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        apply(change.position)
                        change.consume()
                    }
                    active = false
                    knobX = 0f
                    knobY = 0f
                    onMove(0f, 0f)
                }
            }
    ) {
        Canvas(Modifier.size(148.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f
            drawCircle(Ember.Ink.copy(alpha = 0.35f), r * 0.94f, c)
            drawCircle(Ember.Bone.copy(alpha = if (active) 0.42f else 0.26f), r * 0.94f, c, style = Stroke(width = 3.dp.toPx()))
            drawCircle(Ember.Bone.copy(alpha = 0.10f), r * 0.62f, c, style = Stroke(width = 1.5.dp.toPx()))
            // directional pips
            for (i in 0 until 4) {
                rotate(i * 90f, c) {
                    val y = c.y - r * 0.78f
                    drawRect(
                        Ember.Bone.copy(alpha = 0.28f),
                        topLeft = Offset(c.x - 3.dp.toPx(), y),
                        size = Size(6.dp.toPx(), 6.dp.toPx()),
                    )
                }
            }
            val knob = Offset(c.x + knobX, c.y + knobY)
            drawCircle(Ember.Ink.copy(alpha = 0.7f), r * 0.30f, knob + Offset(0f, 2.dp.toPx()))
            drawCircle(Ember.Bone.copy(alpha = if (active) 0.92f else 0.72f), r * 0.30f, knob)
            drawCircle(Ember.BoneDim.copy(alpha = 0.5f), r * 0.30f, knob, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

enum class AbilityGlyph { SWORD, POWER, DODGE }

/**
 * Circular ability button with a cooldown sweep.
 * Fires on press-down so combat stays responsive.
 */
@Composable
fun AbilityButton(
    label: String,
    glyph: AbilityGlyph,
    accent: Color,
    diameter: Int,
    cooldownFraction: () -> Float,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 900f),
        label = "abilityPress",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .size(diameter.dp)
                .semantics { contentDescription = label }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        pressed = true
                        onPress()
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) { change.consume(); break }
                        }
                        pressed = false
                    }
                }
        ) {
            Canvas(Modifier.size(diameter.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f
                val cd = cooldownFraction().coerceIn(0f, 1f)
                val s = scale

                drawCircle(Ember.Ink.copy(alpha = 0.82f), r * s, c)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.16f)),
                        center = c, radius = r * s,
                    ),
                    radius = r * 0.88f * s,
                    center = c,
                )
                drawCircle(Ember.Ink, r * 0.88f * s, c, style = Stroke(width = 3.dp.toPx()))
                drawCircle(accent.copy(alpha = 0.9f), r * s, c, style = Stroke(width = 2.5.dp.toPx()))

                // cooldown: dark cover shrinking clockwise
                if (cd > 0f) {
                    drawArc(
                        color = Ember.Ink.copy(alpha = 0.62f),
                        startAngle = -90f,
                        sweepAngle = 360f * cd,
                        useCenter = true,
                        topLeft = Offset(c.x - r * 0.88f * s, c.y - r * 0.88f * s),
                        size = Size(r * 1.76f * s, r * 1.76f * s),
                    )
                    drawArc(
                        color = accent,
                        startAngle = -90f + 360f * cd,
                        sweepAngle = 360f * (1f - cd),
                        useCenter = false,
                        topLeft = Offset(c.x - r * 0.96f * s, c.y - r * 0.96f * s),
                        size = Size(r * 1.92f * s, r * 1.92f * s),
                        style = Stroke(width = 3.dp.toPx()),
                        blendMode = BlendMode.Plus,
                    )
                }

                val ready = cd <= 0f
                drawGlyph(glyph, c, r * 0.52f * s, ready)
            }
        }
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = Ember.Bone.copy(alpha = 0.85f),
        )
    }
}

private fun DrawScope.drawGlyph(glyph: AbilityGlyph, c: Offset, r: Float, ready: Boolean) {
    val blade = if (ready) Ember.Lantern else Ember.BoneDim
    val core = if (ready) Ember.HotWhite else Ember.Bone.copy(alpha = 0.6f)
    when (glyph) {
        AbilityGlyph.SWORD, AbilityGlyph.POWER -> {
            rotate(if (glyph == AbilityGlyph.SWORD) -38f else 0f, c) {
                val len = r * 1.5f
                val w = r * 0.24f
                // blade
                drawRect(Ember.Ink, Offset(c.x - w / 2f - 2f, c.y - len / 2f - 2f), Size(w + 4f, len * 0.78f + 4f))
                drawRect(blade, Offset(c.x - w / 2f, c.y - len / 2f), Size(w, len * 0.78f))
                drawRect(core, Offset(c.x - w * 0.18f, c.y - len / 2f + 2f), Size(w * 0.36f, len * 0.7f))
                // tip
                drawRect(blade, Offset(c.x - w * 0.24f, c.y - len / 2f - w * 0.6f), Size(w * 0.48f, w * 0.7f))
                // guard
                drawRect(Ember.Ink, Offset(c.x - r * 0.62f, c.y + len * 0.26f), Size(r * 1.24f, w * 0.9f))
                drawRect(Ember.Bark, Offset(c.x - r * 0.56f, c.y + len * 0.27f), Size(r * 1.12f, w * 0.7f))
                // grip
                drawRect(Ember.Bark, Offset(c.x - w * 0.36f, c.y + len * 0.3f), Size(w * 0.72f, len * 0.2f))
            }
            if (glyph == AbilityGlyph.POWER && ready) {
                // impact sparks radiating from the blade
                for (i in 0 until 3) {
                    val yy = c.y - r * 0.7f + i * r * 0.5f
                    drawRect(Ember.HotWhite.copy(alpha = 0.85f), Offset(c.x + r * 0.62f, yy), Size(r * 0.42f, r * 0.13f))
                    drawRect(Ember.Fire.copy(alpha = 0.85f), Offset(c.x - r * 1.02f, yy + r * 0.16f), Size(r * 0.36f, r * 0.13f))
                }
            }
        }

        AbilityGlyph.DODGE -> {
            val w = r * 0.28f
            for (i in 0 until 3) {
                val x = c.x - r * 0.9f + i * r * 0.72f
                val alpha = 0.4f + i * 0.3f
                val col = (if (ready) Ember.Spectral else Ember.BoneDim).copy(alpha = alpha)
                drawRect(Ember.Ink.copy(alpha = alpha), Offset(x - 2f, c.y - r * 0.62f - 2f), Size(w + 4f, r * 0.62f + 4f))
                drawRect(col, Offset(x, c.y - r * 0.62f), Size(w, r * 0.62f))
                drawRect(col, Offset(x + w * 0.6f, c.y - r * 0.1f), Size(w, r * 0.62f))
            }
        }
    }
}
