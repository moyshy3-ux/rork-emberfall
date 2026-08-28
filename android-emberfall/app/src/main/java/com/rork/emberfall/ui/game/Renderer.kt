package com.rork.emberfall.ui.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.rork.emberfall.game.Enemy
import com.rork.emberfall.game.EnemyKind
import com.rork.emberfall.game.EnemyState
import com.rork.emberfall.game.Facing
import com.rork.emberfall.game.GameEngine
import com.rork.emberfall.game.GrassTuft
import com.rork.emberfall.game.HeroBack
import com.rork.emberfall.game.HeroFront
import com.rork.emberfall.game.HeroSide
import com.rork.emberfall.game.LanternSprite
import com.rork.emberfall.game.MushroomSprite
import com.rork.emberfall.game.PixelSprite
import com.rork.emberfall.game.PropKind
import com.rork.emberfall.game.SlimeSprite
import com.rork.emberfall.game.WolfSprite
import com.rork.emberfall.game.World
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val ember = Color(0xFFF2B23A)
private val fire = Color(0xFFE2542A)
private val hotWhite = Color(0xFFFFF3D6)
private val ink = Color(0xFF0B0E14)

/** Maps world coordinates onto the screen for one frame. */
private class View(val scale: Float, val camX: Float, val camY: Float, val w: Float, val h: Float) {
    fun sx(x: Float): Float = (x - camX) * scale + w / 2f
    fun sy(y: Float): Float = (y - camY) * scale + h / 2f
}

fun DrawScope.renderGame(engine: GameEngine, pixelTypeface: Typeface?) {
    val scale = size.width / World.VIEW_WIDTH
    val shakeX = if (engine.shake > 0.1f) (sin(engine.time * 63f) * engine.shake) else 0f
    val shakeY = if (engine.shake > 0.1f) (cos(engine.time * 71f) * engine.shake * 0.7f) else 0f
    val view = View(scale, engine.cameraX + shakeX, engine.cameraY + shakeY, size.width, size.height)

    drawRect(ink)
    drawBackground(engine, view)
    drawGroundLight(engine, view)
    drawEntities(engine, view)
    drawParticles(engine, view)
    drawFireflies(engine, view)
    drawLanternGlow(engine, view)
    drawVignette()
    drawFloatingText(engine, view, pixelTypeface)
    drawDamageVeil(engine)
}

// ------------------------------------------------------------------ background

private fun DrawScope.drawBackground(engine: GameEngine, v: View) {
    val bg: ImageBitmap = engine.map.background
    val dstW = (World.WIDTH * v.scale).roundToInt()
    val dstH = (World.HEIGHT * v.scale).roundToInt()
    drawImage(
        image = bg,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bg.width, bg.height),
        dstOffset = IntOffset(v.sx(0f).roundToInt(), v.sy(0f).roundToInt()),
        dstSize = IntSize(dstW, dstH),
        filterQuality = FilterQuality.None,
    )
}

/** Soft warm pools where the lanterns spill onto the ground, drawn under the sprites. */
private fun DrawScope.drawGroundLight(engine: GameEngine, v: View) {
    for (prop in engine.map.props) {
        if (prop.kind != PropKind.LANTERN) continue
        val cx = v.sx(prop.x + 26f)
        val cy = v.sy(prop.y - 30f)
        val r = 150f * v.scale
        if (cx < -r || cx > size.width + r || cy < -r || cy > size.height + r) continue
        val flicker = 0.82f + 0.18f * sin(engine.time * 6.3f + prop.seed)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ember.copy(alpha = 0.20f * flicker), Color.Transparent),
                center = Offset(cx, cy),
                radius = r,
            ),
            radius = r,
            center = Offset(cx, cy),
            blendMode = BlendMode.Plus,
        )
    }
}

// ------------------------------------------------------------------ entities

private class Drawable(val sortY: Float, val draw: DrawScope.() -> Unit)

private fun DrawScope.drawEntities(engine: GameEngine, v: View) {
    val list = ArrayList<Drawable>(64)
    val margin = 140f

    for (prop in engine.map.props) {
        val sx = v.sx(prop.x)
        val sy = v.sy(prop.y)
        if (sx < -margin || sx > size.width + margin || sy < -margin * 2 || sy > size.height + margin) continue
        when (prop.kind) {
            PropKind.LANTERN -> list.add(Drawable(prop.y) {
                val flicker = 0.85f + 0.15f * sin(engine.time * 6.3f + prop.seed)
                blit(LanternSprite, v, prop.x, prop.y, 2.6f, alpha = 1f)
                val gx = v.sx(prop.x + 22f)
                val gy = v.sy(prop.y - 46f)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(hotWhite.copy(alpha = 0.5f * flicker), ember.copy(alpha = 0.22f * flicker), Color.Transparent),
                        center = Offset(gx, gy),
                        radius = 34f * v.scale,
                    ),
                    radius = 34f * v.scale,
                    center = Offset(gx, gy),
                    blendMode = BlendMode.Plus,
                )
            })

            PropKind.MUSHROOM -> list.add(Drawable(prop.y) {
                blit(MushroomSprite, v, prop.x, prop.y, 2.2f)
                val pulse = 0.55f + 0.45f * sin(engine.time * 2.1f + prop.seed)
                val gx = v.sx(prop.x)
                val gy = v.sy(prop.y - 4f)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(ember.copy(alpha = 0.30f * pulse), Color.Transparent),
                        center = Offset(gx, gy),
                        radius = 26f * v.scale,
                    ),
                    radius = 26f * v.scale,
                    center = Offset(gx, gy),
                    blendMode = BlendMode.Plus,
                )
            })

            PropKind.GRASS -> list.add(Drawable(prop.y) {
                val sway = sin(engine.time * 1.6f + prop.seed * 0.7f) * 2.2f
                blit(GrassTuft, v, prop.x + sway, prop.y, 2.4f, alpha = 0.95f)
            })
        }
    }

    for (e in engine.enemies) {
        val sx = v.sx(e.x)
        if (sx < -margin || sx > size.width + margin) continue
        val sy = v.sy(e.y)
        if (sy < -margin || sy > size.height + margin) continue
        list.add(Drawable(e.y) { drawEnemy(e, v) })
    }

    list.add(Drawable(engine.player.y) { drawPlayer(engine, v) })

    list.sortBy { it.sortY }
    for (d in list) d.draw(this)
}

private fun DrawScope.drawEnemy(e: Enemy, v: View) {
    val dying = e.state == EnemyState.DYING
    val deathT = if (dying) (e.deathTimer / 0.55f).coerceIn(0f, 1f) else 1f
    val alpha = if (dying) deathT else 1f
    val lift = if (dying) (1f - deathT) * -18f else 0f

    shadow(v, e.x, e.y, e.stats.radius * 1.5f, alpha * 0.45f)

    val windup = e.state == EnemyState.WINDUP
    when (e.kind) {
        EnemyKind.SLIME -> {
            val squash = 1f + 0.14f * sin(e.bob)
            val extra = if (windup) 1.18f else 1f
            blit(
                SlimeSprite, v, e.x, e.y + lift, 2.6f,
                alpha = alpha * 0.94f,
                flipX = !e.facingRight,
                scaleX = extra / squash,
                scaleY = squash * extra,
                flash = e.hitFlash > 0f,
                tint = if (windup) Color(0xFFBFF5EC) else null,
            )
            val gx = v.sx(e.x)
            val gy = v.sy(e.y - 14f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF4FD1C5).copy(alpha = 0.26f * alpha), Color.Transparent),
                    center = Offset(gx, gy), radius = 30f * v.scale,
                ),
                radius = 30f * v.scale, center = Offset(gx, gy), blendMode = BlendMode.Plus,
            )
        }

        EnemyKind.WOLF -> {
            val gait = if (e.state == EnemyState.CHASE) sin(e.bob) * 2.2f else sin(e.bob * 0.4f) * 0.8f
            blit(
                WolfSprite, v, e.x, e.y + lift + gait * 0.4f, 2.5f,
                alpha = alpha,
                flipX = !e.facingRight,
                scaleX = if (windup) 1.1f else 1f,
                scaleY = if (windup) 0.94f else 1f,
                flash = e.hitFlash > 0f,
            )
            // predatory eye-shine
            val ex = v.sx(e.x + (if (e.facingRight) 14f else -14f))
            val ey = v.sy(e.y - 20f)
            drawCircle(fire.copy(alpha = 0.55f * alpha), 3.4f * v.scale, Offset(ex, ey), blendMode = BlendMode.Plus)
        }
    }

    if (!dying && e.hp < e.stats.maxHp) {
        val w = e.stats.radius * 2.2f * v.scale
        val x = v.sx(e.x) - w / 2f
        val y = v.sy(e.y) - (e.stats.radius * 3.4f) * v.scale
        val h = 3.4f * v.scale
        drawRect(ink.copy(alpha = 0.85f), Offset(x - v.scale, y - v.scale), Size(w + v.scale * 2, h + v.scale * 2))
        drawRect(Color(0xFF3A1A16), Offset(x, y), Size(w, h))
        drawRect(Color(0xFFC0392B), Offset(x, y), Size(w * (e.hp / e.stats.maxHp).coerceIn(0f, 1f), h))
    }
}

private fun DrawScope.drawPlayer(engine: GameEngine, v: View) {
    val p = engine.player
    val dead = p.deadTimer > 0f
    val alpha = when {
        dead -> (p.deadTimer / 2.2f).coerceIn(0f, 1f)
        p.invuln > 0f && ((engine.time * 22f).toInt() % 2 == 0) -> 0.55f
        else -> 1f
    }

    shadow(v, p.x, p.y, 20f, alpha * 0.5f)

    if (engine.levelUpGlow > 0f) {
        val t = 1f - (engine.levelUpGlow / 1.4f)
        val r = (30f + t * 130f) * v.scale
        drawCircle(
            color = ember.copy(alpha = (1f - t) * 0.55f),
            radius = r,
            center = Offset(v.sx(p.x), v.sy(p.y - 20f)),
            style = Stroke(width = 5f * v.scale),
            blendMode = BlendMode.Plus,
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(ember.copy(alpha = (1f - t) * 0.35f), Color.Transparent),
                center = Offset(v.sx(p.x), v.sy(p.y - 26f)), radius = r,
            ),
            radius = r, center = Offset(v.sx(p.x), v.sy(p.y - 26f)), blendMode = BlendMode.Plus,
        )
    }

    val bob = if (p.moving) abs(sin(p.walkPhase)) * 2.4f else sin(engine.time * 2f) * 0.8f
    val sprite: PixelSprite
    var flip = false
    when (p.facing) {
        Facing.UP -> sprite = HeroBack
        Facing.DOWN -> sprite = HeroFront
        Facing.LEFT -> { sprite = HeroSide; flip = true }
        Facing.RIGHT -> sprite = HeroSide
    }

    // sword arc is drawn behind the hero when the swing points away from the camera
    val swingBehind = p.swingTimer > 0f && sin(p.swingAngle) < 0f
    if (swingBehind) drawSwing(engine, v)

    blit(
        sprite, v, p.x, p.y - bob, World.SPRITE_SCALE,
        alpha = alpha,
        flipX = flip,
        flash = p.hurtFlash > 0.2f,
        tint = if (p.hurtFlash > 0f) Color(0xFFC0392B).copy(alpha = 0.6f) else null,
        scaleY = if (dead) 0.7f else 1f,
    )

    if (!swingBehind) drawSwing(engine, v)

    if (p.dodgeTimer > 0f) {
        val cx = v.sx(p.x)
        val cy = v.sy(p.y - 22f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF4FD1C5).copy(alpha = 0.35f), Color.Transparent),
                center = Offset(cx, cy), radius = 40f * v.scale,
            ),
            radius = 40f * v.scale, center = Offset(cx, cy), blendMode = BlendMode.Plus,
        )
    }
}

/** The ember crescent the Warrior's blade carves through the air. */
private fun DrawScope.drawSwing(engine: GameEngine, v: View) {
    val p = engine.player
    if (p.swingTimer <= 0f) return
    val progress = (1f - p.swingTimer / p.swingDuration).coerceIn(0f, 1f)
    val power = p.swingIsPower
    val arc = if (power) 2.7f else 1.9f
    val range = if (power) 84f else 62f
    val cx = v.sx(p.x)
    val cy = v.sy(p.y - 22f)
    val radius = range * v.scale
    val startDeg = Math.toDegrees((p.swingAngle - arc / 2f).toDouble()).toFloat()
    val sweepDeg = Math.toDegrees((arc * progress).toDouble()).toFloat()
    val fade = (1f - progress * 0.55f)

    drawArc(
        color = (if (power) fire else ember).copy(alpha = 0.55f * fade),
        startAngle = startDeg,
        sweepAngle = sweepDeg,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = (if (power) 16f else 10f) * v.scale),
        blendMode = BlendMode.Plus,
    )
    drawArc(
        color = hotWhite.copy(alpha = 0.75f * fade),
        startAngle = startDeg + sweepDeg * 0.55f,
        sweepAngle = sweepDeg * 0.45f,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = (if (power) 6f else 3.5f) * v.scale),
        blendMode = BlendMode.Plus,
    )

    // the blade itself
    val bladeAngle = p.swingAngle - arc / 2f + arc * progress
    val bx = cx + cos(bladeAngle) * radius
    val by = cy + sin(bladeAngle) * radius
    val hiltX = cx + cos(bladeAngle) * 12f * v.scale
    val hiltY = cy + sin(bladeAngle) * 12f * v.scale
    drawLine(ink, Offset(hiltX, hiltY), Offset(bx, by), strokeWidth = 7f * v.scale)
    drawLine(ember, Offset(hiltX, hiltY), Offset(bx, by), strokeWidth = 4.5f * v.scale)
    drawLine(hotWhite, Offset(hiltX, hiltY), Offset(bx, by), strokeWidth = 1.8f * v.scale)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(hotWhite.copy(alpha = 0.7f * fade), Color.Transparent),
            center = Offset(bx, by), radius = 16f * v.scale,
        ),
        radius = 16f * v.scale, center = Offset(bx, by), blendMode = BlendMode.Plus,
    )
}

// ------------------------------------------------------------------ effects

private fun DrawScope.drawParticles(engine: GameEngine, v: View) {
    for (p in engine.particles) {
        val a = (p.life / p.maxLife).coerceIn(0f, 1f)
        val s = p.size * v.scale * (0.5f + a * 0.5f)
        val x = v.sx(p.x)
        val y = v.sy(p.y)
        if (x < -20f || x > size.width + 20f || y < -20f || y > size.height + 20f) continue
        drawRect(
            color = Color(p.color).copy(alpha = a),
            topLeft = Offset(x - s / 2f, y - s / 2f),
            size = Size(s, s),
            blendMode = BlendMode.Plus,
        )
    }
}

private fun DrawScope.drawFireflies(engine: GameEngine, v: View) {
    for (f in engine.map.fireflies) {
        val x = v.sx(f.x + cos(f.phase) * f.radius)
        val y = v.sy(f.y + sin(f.phase * 0.73f) * f.radius * 0.6f)
        if (x < -10f || x > size.width + 10f || y < -10f || y > size.height + 10f) continue
        val pulse = (0.35f + 0.65f * (0.5f + 0.5f * sin(f.phase * 2.3f)))
        val r = 1.6f * v.scale
        drawCircle(ember.copy(alpha = 0.85f * pulse), r, Offset(x, y), blendMode = BlendMode.Plus)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(ember.copy(alpha = 0.22f * pulse), Color.Transparent),
                center = Offset(x, y), radius = 9f * v.scale,
            ),
            radius = 9f * v.scale, center = Offset(x, y), blendMode = BlendMode.Plus,
        )
    }
}

private fun DrawScope.drawLanternGlow(engine: GameEngine, v: View) {
    // a faint global haze so the forest reads as humid night air
    drawRect(Color(0xFF1C2733).copy(alpha = 0.16f))
}

private fun DrawScope.drawVignette() {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Transparent, ink.copy(alpha = 0.55f), ink.copy(alpha = 0.9f)),
            center = Offset(size.width / 2f, size.height * 0.46f),
            radius = size.height * 0.72f,
        )
    )
}

private fun DrawScope.drawDamageVeil(engine: GameEngine) {
    val p = engine.player
    val hurt = p.hurtFlash / 0.35f
    val lowHp = if (p.hp / p.maxHp < 0.3f && p.deadTimer <= 0f) {
        0.10f + 0.06f * sin(engine.time * 4.2f)
    } else 0f
    val amount = (hurt * 0.5f + lowHp).coerceAtMost(0.6f)
    if (amount <= 0.001f) return
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0xFFC0392B).copy(alpha = amount)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.height * 0.62f,
        )
    )
}

private fun DrawScope.drawFloatingText(engine: GameEngine, v: View, typeface: Typeface?) {
    val canvas = drawContext.canvas.nativeCanvas
    val paint = Paint().apply {
        isAntiAlias = false
        this.typeface = typeface ?: Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    for (t in engine.texts) {
        val a = (t.life / t.maxLife).coerceIn(0f, 1f)
        val x = v.sx(t.x)
        val y = v.sy(t.y)
        if (x < -80f || x > size.width + 80f || y < -60f || y > size.height + 60f) continue
        val rise = (1f - a)
        val textSize = (if (t.big) 15f else 9f) * v.scale * (1f + rise * 0.12f)
        paint.textSize = textSize
        val alpha = (a.coerceAtMost(0.85f) / 0.85f * 255f).toInt().coerceIn(0, 255)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f * v.scale / 3f
        paint.color = android.graphics.Color.argb(alpha, 5, 6, 9)
        canvas.drawText(t.text, x, y, paint)

        paint.style = Paint.Style.FILL
        val c = Color(t.color)
        paint.color = android.graphics.Color.argb(
            alpha, (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt()
        )
        canvas.drawText(t.text, x, y, paint)
    }
}

// ------------------------------------------------------------------ helpers

private fun DrawScope.shadow(v: View, x: Float, y: Float, radius: Float, alpha: Float) {
    val w = radius * 2f * v.scale
    val h = radius * 0.85f * v.scale
    drawOval(
        color = Color.Black.copy(alpha = alpha * 0.5f),
        topLeft = Offset(v.sx(x) - w / 2f, v.sy(y) - h / 2f),
        size = Size(w, h),
    )
}

/**
 * Blit a pixel sprite anchored at its bottom-centre in world space,
 * with nearest-neighbour scaling so the pixel grid stays crisp.
 */
private fun DrawScope.blit(
    sprite: PixelSprite,
    v: View,
    worldX: Float,
    worldY: Float,
    pixelScale: Float,
    alpha: Float = 1f,
    flipX: Boolean = false,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    flash: Boolean = false,
    tint: Color? = null,
) {
    val image = sprite.image(flipX)
    val w = sprite.width * pixelScale * v.scale * scaleX
    val h = sprite.height * pixelScale * v.scale * scaleY
    val left = v.sx(worldX) - w / 2f
    val top = v.sy(worldY) - h
    val filter = when {
        flash -> ColorFilter.tint(Color.White, BlendMode.SrcAtop)
        tint != null -> ColorFilter.tint(tint, BlendMode.Overlay)
        else -> null
    }
    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1)),
        alpha = alpha,
        colorFilter = filter,
        filterQuality = FilterQuality.None,
    )
}
