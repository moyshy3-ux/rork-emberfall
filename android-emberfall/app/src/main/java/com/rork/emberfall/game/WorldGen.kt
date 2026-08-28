package com.rork.emberfall.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Everything static about the hollow: a baked background plus the animated props on top. */
class HollowMap(
    val background: ImageBitmap,
    val props: List<Prop>,
    val spawnPoints: List<Pair<Float, Float>>,
    val fireflies: List<Firefly>,
)

private const val RES = 0.5f // background is baked at half world resolution for a chunky look

private fun px(v: Float): Float = v * RES

/** The winding path that threads the hollow from the south gate up to the ruined arch. */
fun pathX(y: Float): Float {
    val t = y / World.HEIGHT
    return World.WIDTH * 0.5f + 210f * sin(t * 5.4f) + 70f * sin(t * 11.3f)
}

fun generateHollow(): HollowMap {
    val rng = Random(20260828)
    val w = (World.WIDTH * RES).toInt()
    val h = (World.HEIGHT * RES).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint().apply { isAntiAlias = false }

    fun rect(x: Float, y: Float, rw: Float, rh: Float, color: Int) {
        paint.color = color
        canvas.drawRect(px(x), px(y), px(x + rw), px(y + rh), paint)
    }

    // --- ground: layered moss noise -------------------------------------------------
    paint.color = 0xFF121A16.toInt()
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

    val groundTones = intArrayOf(
        0xFF15201C.toInt(), 0xFF19271F.toInt(), 0xFF1E3024.toInt(), 0xFF24402F.toInt(), 0xFF101814.toInt(),
    )
    val cell = 8f
    var gy = 0f
    while (gy < World.HEIGHT) {
        var gx = 0f
        while (gx < World.WIDTH) {
            val n = (sin(gx * 0.031f) * cos(gy * 0.027f) + sin((gx + gy) * 0.013f)) * 0.5f
            val idx = ((n + 1f) * 2.2f).toInt().coerceIn(0, groundTones.size - 1)
            val tone = if (rng.nextFloat() < 0.12f) groundTones[rng.nextInt(groundTones.size)] else groundTones[idx]
            rect(gx, gy, cell, cell, tone)
            gx += cell
        }
        gy += cell
    }

    // --- the dirt path --------------------------------------------------------------
    var y = -20f
    while (y < World.HEIGHT + 20f) {
        val cx = pathX(y)
        val halfW = 46f + 12f * sin(y * 0.017f)
        rect(cx - halfW, y, halfW * 2f, 10f, 0xFF3E3122.toInt())
        rect(cx - halfW + 8f, y, halfW * 2f - 16f, 10f, 0xFF4A3A2A.toInt())
        rect(cx - halfW + 20f, y, halfW * 2f - 40f, 10f, 0xFF57462F.toInt())
        // scattered pebbles and worn patches
        if (rng.nextFloat() < 0.3f) {
            val px1 = cx - halfW + rng.nextFloat() * halfW * 2f
            rect(px1, y, 6f, 6f, 0xFF6B5A3E.toInt())
        }
        if (rng.nextFloat() < 0.2f) {
            val px1 = cx - halfW + rng.nextFloat() * halfW * 2f
            rect(px1, y, 8f, 8f, 0xFF332818.toInt())
        }
        y += 10f
    }

    // --- helper props ----------------------------------------------------------------
    fun drawTree(tx: Float, ty: Float, scale: Float, seed: Int) {
        val r = Random(seed)
        val trunkW = 22f * scale
        val trunkH = 70f * scale
        rect(tx - trunkW / 2f, ty - trunkH, trunkW, trunkH, 0xFF2A1E18.toInt())
        rect(tx - trunkW / 2f + 4f * scale, ty - trunkH, 6f * scale, trunkH, 0xFF3A2A22.toInt())
        rect(tx - trunkW / 2f, ty - 8f * scale, trunkW, 8f * scale, 0xFF1A120E.toInt())
        // canopy: overlapping chunky blobs
        val cw = 130f * scale
        val ch = 96f * scale
        val top = ty - trunkH - ch * 0.72f
        for (i in 0 until 16) {
            val bx = tx + (r.nextFloat() - 0.5f) * cw
            val by = top + (r.nextFloat() - 0.4f) * ch
            val bs = (26f + r.nextFloat() * 26f) * scale
            val tone = when (r.nextInt(4)) {
                0 -> 0xFF16281C.toInt()
                1 -> 0xFF1D3324.toInt()
                2 -> 0xFF24402F.toInt()
                else -> 0xFF0F1A14.toInt()
            }
            rect(bx - bs / 2f, by - bs / 2f, bs, bs, tone)
        }
        // moonlit rim on the upper left
        for (i in 0 until 5) {
            val bx = tx - cw * 0.3f + (r.nextFloat() - 0.5f) * cw * 0.5f
            val by = top - ch * 0.1f + (r.nextFloat() - 0.5f) * ch * 0.3f
            rect(bx, by, 14f * scale, 10f * scale, 0xFF2E4E36.toInt())
        }
    }

    fun drawRock(rx: Float, ry: Float, scale: Float, seed: Int) {
        val r = Random(seed)
        val rw = 46f * scale
        val rh = 34f * scale
        rect(rx - rw / 2f, ry - rh, rw, rh, 0xFF2C3532.toInt())
        rect(rx - rw / 2f + 4f, ry - rh + 4f, rw - 12f, rh - 14f, 0xFF3D4744.toInt())
        rect(rx - rw / 2f + 8f, ry - rh + 4f, rw * 0.4f, 8f * scale, 0xFF4A5450.toInt())
        // moss creeping over the top
        for (i in 0 until 4) {
            rect(
                rx - rw / 2f + r.nextFloat() * rw * 0.8f,
                ry - rh + r.nextFloat() * 8f,
                10f * scale, 6f * scale, 0xFF2E4E36.toInt()
            )
        }
        rect(rx - rw / 2f, ry - 6f, rw, 6f, 0xFF1A211F.toInt())
    }

    fun drawPillar(px0: Float, py0: Float, ph: Float) {
        val pw = 34f
        rect(px0 - pw / 2f, py0 - ph, pw, ph, 0xFF2C3532.toInt())
        rect(px0 - pw / 2f + 4f, py0 - ph, 8f, ph, 0xFF4A5450.toInt())
        rect(px0 - pw / 2f - 5f, py0 - ph - 10f, pw + 10f, 12f, 0xFF3D4744.toInt())
        rect(px0 - pw / 2f - 4f, py0 - 12f, pw + 8f, 12f, 0xFF3D4744.toInt())
        var my = py0 - ph
        while (my < py0) {
            if (Random((px0 + my).toInt()).nextFloat() < 0.4f) {
                rect(px0 - pw / 2f + Random(my.toInt()).nextFloat() * pw * 0.7f, my, 12f, 8f, 0xFF2E4E36.toInt())
            }
            my += 14f
        }
    }

    /** The ruined arch that crowns the north end of the hollow. */
    fun drawArch(ax: Float, ay: Float) {
        val legW = 46f
        val legH = 180f
        val span = 150f
        drawPillar(ax - span / 2f, ay, legH)
        drawPillar(ax + span / 2f, ay, legH)
        // arch curve
        for (i in 0..26) {
            val t = i / 26f
            val a = Math.PI.toFloat() * t
            val bx = ax - span / 2f + span * (1f - cos(a)) / 2f
            val by = ay - legH - sin(a) * 74f
            rect(bx - legW / 2f * 0.6f, by - 16f, legW * 0.6f, 22f, 0xFF3D4744.toInt())
            rect(bx - legW / 2f * 0.6f + 3f, by - 13f, legW * 0.3f, 8f, 0xFF4A5450.toInt())
            if (i % 4 == 0) rect(bx - 8f, by - 4f, 14f, 10f, 0xFF2E4E36.toInt())
        }
        // rubble at the base
        rect(ax - span / 2f - 60f, ay - 24f, 44f, 24f, 0xFF2C3532.toInt())
        rect(ax + span / 2f + 26f, ay - 18f, 38f, 18f, 0xFF2C3532.toInt())
    }

    drawArch(pathX(150f), 210f)

    // --- forest mass along both edges -------------------------------------------------
    val props = ArrayList<Prop>()
    var ty = 60f
    while (ty < World.HEIGHT + 120f) {
        val cx = pathX(ty)
        // left treeline
        var lx = 20f + rng.nextFloat() * 40f
        while (lx < cx - 130f) {
            drawTree(lx, ty + rng.nextFloat() * 60f, 0.75f + rng.nextFloat() * 0.5f, rng.nextInt())
            lx += 90f + rng.nextFloat() * 70f
        }
        var rx = cx + 130f + rng.nextFloat() * 50f
        while (rx < World.WIDTH - 10f) {
            drawTree(rx, ty + rng.nextFloat() * 60f, 0.75f + rng.nextFloat() * 0.5f, rng.nextInt())
            rx += 90f + rng.nextFloat() * 70f
        }
        ty += 150f
    }

    // rocks and ruins scattered near the path
    var ry = 320f
    while (ry < World.HEIGHT - 100f) {
        val cx = pathX(ry)
        val side = if (rng.nextBoolean()) -1f else 1f
        drawRock(cx + side * (80f + rng.nextFloat() * 60f), ry, 0.7f + rng.nextFloat() * 0.8f, rng.nextInt())
        if (rng.nextFloat() < 0.35f) {
            drawPillar(cx - side * (95f + rng.nextFloat() * 40f), ry + 40f, 90f + rng.nextFloat() * 60f)
        }
        ry += 190f + rng.nextFloat() * 90f
    }

    // baked grass fringe hugging the path
    var fy = 0f
    while (fy < World.HEIGHT) {
        val cx = pathX(fy)
        for (s in intArrayOf(-1, 1)) {
            if (rng.nextFloat() < 0.7f) {
                val gx2 = cx + s * (50f + rng.nextFloat() * 26f)
                rect(gx2, fy, 10f, 6f, 0xFF2E4E36.toInt())
                rect(gx2 + 4f, fy - 5f, 4f, 6f, 0xFF35603F.toInt())
            }
        }
        fy += 16f
    }

    // --- animated props ----------------------------------------------------------------
    var ly = 300f
    var lanternSide = 1f
    while (ly < World.HEIGHT - 120f) {
        val cx = pathX(ly)
        props.add(Prop(PropKind.LANTERN, cx + lanternSide * (86f + Random(ly.toInt()).nextFloat() * 20f), ly, ly.toInt()))
        lanternSide *= -1f
        ly += 230f + Random(ly.toInt() + 7).nextFloat() * 90f
    }
    props.add(Prop(PropKind.LANTERN, pathX(210f) - 150f, 250f, 991))
    props.add(Prop(PropKind.LANTERN, pathX(210f) + 150f, 250f, 992))

    var my2 = 240f
    while (my2 < World.HEIGHT - 60f) {
        val cx = pathX(my2)
        val side = if (Random(my2.toInt()).nextBoolean()) -1f else 1f
        val clusterSize = 2 + Random(my2.toInt() + 3).nextInt(3)
        val baseX = cx + side * (72f + Random(my2.toInt() + 5).nextFloat() * 50f)
        for (i in 0 until clusterSize) {
            props.add(
                Prop(
                    PropKind.MUSHROOM,
                    baseX + (i - clusterSize / 2f) * 16f + Random(my2.toInt() + i).nextFloat() * 8f,
                    my2 + Random(my2.toInt() + i * 3).nextFloat() * 20f,
                    my2.toInt() + i,
                )
            )
        }
        my2 += 150f + Random(my2.toInt() + 11).nextFloat() * 120f
    }

    var gy2 = 40f
    while (gy2 < World.HEIGHT) {
        val cx = pathX(gy2)
        for (s in intArrayOf(-1, 1)) {
            if (Random(gy2.toInt() + s).nextFloat() < 0.75f) {
                props.add(
                    Prop(
                        PropKind.GRASS,
                        cx + s * (58f + Random(gy2.toInt() + s * 3).nextFloat() * 40f),
                        gy2,
                        gy2.toInt() + s,
                    )
                )
            }
        }
        gy2 += 34f
    }

    // --- spawn points along the path ----------------------------------------------------
    val spawns = ArrayList<Pair<Float, Float>>()
    var sy = 200f
    while (sy < World.HEIGHT - 200f) {
        val cx = pathX(sy)
        spawns.add(cx to sy)
        spawns.add((cx - 90f).coerceIn(60f, World.WIDTH - 60f) to sy + 60f)
        spawns.add((cx + 90f).coerceIn(60f, World.WIDTH - 60f) to sy - 60f)
        sy += 150f
    }

    val flies = ArrayList<Firefly>()
    repeat(90) {
        flies.add(
            Firefly(
                x = rng.nextFloat() * World.WIDTH,
                y = rng.nextFloat() * World.HEIGHT,
                phase = rng.nextFloat() * 6.28f,
                speed = 0.4f + rng.nextFloat() * 0.9f,
                radius = 10f + rng.nextFloat() * 26f,
            )
        )
    }

    return HollowMap(bmp.asImageBitmap(), props, spawns, flies)
}
