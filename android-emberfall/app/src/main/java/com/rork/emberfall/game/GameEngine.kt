package com.rork.emberfall.game

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** Tunables for the Warrior's three abilities. */
object Combat {
    const val ATTACK_CD = 0.42f
    const val ATTACK_DURATION = 0.26f
    const val ATTACK_RANGE = 62f
    const val ATTACK_ARC = 1.9f

    const val POWER_CD = 4.5f
    const val POWER_DURATION = 0.42f
    const val POWER_RANGE = 84f
    const val POWER_ARC = 2.7f
    const val POWER_MULT = 2.6f

    const val DODGE_CD = 2.0f
    const val DODGE_DURATION = 0.24f
    const val DODGE_SPEED = 620f

    const val MOVE_SPEED = 118f
    const val MAX_ENEMIES = 9
}

data class HudSnapshot(
    val hp: Int,
    val maxHp: Int,
    val level: Int,
    val xp: Int,
    val xpToNext: Int,
    val gold: Int,
    val kills: Int,
    val attackCd: Float,
    val powerCd: Float,
    val dodgeCd: Float,
    val dead: Boolean,
)

/**
 * The whole simulation. Deliberately kept outside Compose state — the render loop
 * ticks it and bumps a frame counter so the Canvas redraws.
 */
class GameEngine(
    val map: HollowMap,
    private val onProgress: (Player) -> Unit,
) {
    val player = Player()
    val enemies = ArrayList<Enemy>()
    val particles = ArrayList<Particle>()
    val texts = ArrayList<FloatingText>()

    var cameraX = 0f
    var cameraY = 0f
    var shake = 0f
    var levelUpGlow = 0f
    var time = 0f
    var paused = false

    private val rng = Random(System.nanoTime())
    private var spawnTimer = 1.2f
    private var saveTimer = 0f

    // queued input
    var moveX = 0f
    var moveY = 0f
    private var wantAttack = false
    private var wantPower = false
    private var wantDodge = false

    fun requestAttack() { wantAttack = true }
    fun requestPower() { wantPower = true }
    fun requestDodge() { wantDodge = true }

    fun hud(): HudSnapshot = HudSnapshot(
        hp = player.hp.toInt().coerceAtLeast(0),
        maxHp = player.maxHp.toInt(),
        level = player.level,
        xp = player.xp,
        xpToNext = player.xpToNext,
        gold = player.gold,
        kills = player.kills,
        attackCd = (player.attackCd / Combat.ATTACK_CD).coerceIn(0f, 1f),
        powerCd = (player.powerCd / Combat.POWER_CD).coerceIn(0f, 1f),
        dodgeCd = (player.dodgeCd / Combat.DODGE_CD).coerceIn(0f, 1f),
        dead = player.deadTimer > 0f,
    )

    fun seedInitialEnemies() {
        repeat(6) { spawnEnemy(minDistance = 260f) }
    }

    fun update(dtRaw: Float) {
        val dt = dtRaw.coerceAtMost(0.05f)
        time += dt
        if (!paused) {
            updatePlayer(dt)
            updateEnemies(dt)
            updateSpawns(dt)
        }
        updateEffects(dt)
        updateCamera(dt)

        saveTimer += dt
        if (saveTimer > 5f) {
            saveTimer = 0f
            onProgress(player)
        }
    }

    // ---------------------------------------------------------------- player

    private fun updatePlayer(dt: Float) {
        val p = player
        p.attackCd = max(0f, p.attackCd - dt)
        p.powerCd = max(0f, p.powerCd - dt)
        p.dodgeCd = max(0f, p.dodgeCd - dt)
        p.hurtFlash = max(0f, p.hurtFlash - dt)
        p.invuln = max(0f, p.invuln - dt)

        if (p.deadTimer > 0f) {
            p.deadTimer -= dt
            if (p.deadTimer <= 0f) {
                p.reset()
                p.gold = (p.gold * 0.9f).toInt()
                enemies.removeAll { dist(it.x, it.y, p.x, p.y) < 320f }
                onProgress(p)
            }
            return
        }

        // dodge overrides normal movement
        if (p.dodgeTimer > 0f) {
            p.dodgeTimer -= dt
            val t = (p.dodgeTimer / Combat.DODGE_DURATION).coerceIn(0f, 1f)
            val speed = Combat.DODGE_SPEED * (0.35f + t * 0.65f)
            p.x += p.dodgeDirX * speed * dt
            p.y += p.dodgeDirY * speed * dt
            if (rng.nextFloat() < 0.7f) {
                particles.add(
                    Particle(
                        p.x + (rng.nextFloat() - 0.5f) * 14f, p.y - 14f,
                        -p.dodgeDirX * 30f, -p.dodgeDirY * 30f,
                        0.32f, 0.32f, 0xFF4FD1C5, 3f + rng.nextFloat() * 3f,
                    )
                )
            }
            clampToWorld(p)
            wantAttack = false; wantPower = false; wantDodge = false
            return
        }

        val mag = hypot(moveX, moveY)
        p.moving = mag > 0.12f
        if (p.moving && p.swingTimer <= 0f) {
            val nx = moveX / mag
            val ny = moveY / mag
            val speed = Combat.MOVE_SPEED * mag.coerceAtMost(1f)
            p.x += nx * speed * dt
            p.y += ny * speed * dt
            p.walkPhase += dt * 9f * mag
            p.facing = when {
                abs(nx) > abs(ny) && nx > 0 -> Facing.RIGHT
                abs(nx) > abs(ny) -> Facing.LEFT
                ny < 0 -> Facing.UP
                else -> Facing.DOWN
            }
            clampToWorld(p)
        }

        if (p.swingTimer > 0f) {
            p.swingTimer -= dt
            val progress = 1f - (p.swingTimer / p.swingDuration).coerceIn(0f, 1f)
            if (!p.swingHitDone && progress > 0.35f) {
                p.swingHitDone = true
                resolveSwing(p.swingIsPower)
            }
        }

        if (wantDodge && p.dodgeCd <= 0f) {
            var dx = moveX
            var dy = moveY
            if (hypot(dx, dy) < 0.15f) {
                val f = facingVector(p.facing)
                dx = f.first; dy = f.second
            }
            val m = hypot(dx, dy)
            p.dodgeDirX = dx / m
            p.dodgeDirY = dy / m
            p.dodgeTimer = Combat.DODGE_DURATION
            p.dodgeCd = Combat.DODGE_CD
            p.invuln = Combat.DODGE_DURATION + 0.12f
            p.swingTimer = 0f
        } else if (wantPower && p.powerCd <= 0f && p.swingTimer <= 0f) {
            startSwing(power = true)
        } else if (wantAttack && p.attackCd <= 0f && p.swingTimer <= 0f) {
            startSwing(power = false)
        }
        wantAttack = false; wantPower = false; wantDodge = false
    }

    private fun startSwing(power: Boolean) {
        val p = player
        p.swingIsPower = power
        p.swingDuration = if (power) Combat.POWER_DURATION else Combat.ATTACK_DURATION
        p.swingTimer = p.swingDuration
        p.swingHitDone = false
        p.swingAngle = aimAngle()
        if (power) {
            p.powerCd = Combat.POWER_CD
            p.attackCd = Combat.ATTACK_CD * 0.6f
            shake = max(shake, 7f)
            repeat(16) {
                val a = rng.nextFloat() * 6.28f
                particles.add(
                    Particle(
                        p.x, p.y - 22f, cos(a) * 70f, sin(a) * 70f,
                        0.4f, 0.4f, 0xFFE2542A, 3f + rng.nextFloat() * 3f,
                    )
                )
            }
        } else {
            p.attackCd = Combat.ATTACK_CD
        }
    }

    /** Aim at the nearest enemy inside a generous cone, otherwise straight ahead. */
    private fun aimAngle(): Float {
        val p = player
        val f = facingVector(p.facing)
        val baseAngle = atan2(f.second, f.first)
        var best: Enemy? = null
        var bestScore = Float.MAX_VALUE
        for (e in enemies) {
            if (!e.alive) continue
            val d = dist(p.x, p.y - 20f, e.x, e.y - 12f)
            if (d > Combat.POWER_RANGE + 30f) continue
            val a = atan2(e.y - 12f - (p.y - 20f), e.x - p.x)
            val diff = abs(angleDiff(a, baseAngle))
            if (diff > 1.6f) continue
            val score = d + diff * 40f
            if (score < bestScore) { bestScore = score; best = e }
        }
        val target = best ?: return baseAngle
        return atan2(target.y - 12f - (player.y - 20f), target.x - player.x)
    }

    private fun resolveSwing(power: Boolean) {
        val p = player
        val range = if (power) Combat.POWER_RANGE else Combat.ATTACK_RANGE
        val arc = if (power) Combat.POWER_ARC else Combat.ATTACK_ARC
        var hitAny = false
        for (e in enemies) {
            if (!e.alive) continue
            val ex = e.x
            val ey = e.y - e.stats.radius * 0.6f
            val d = dist(p.x, p.y - 20f, ex, ey)
            if (d > range + e.stats.radius) continue
            val a = atan2(ey - (p.y - 20f), ex - p.x)
            if (abs(angleDiff(a, p.swingAngle)) > arc / 2f) continue

            val base = p.attackPower * (if (power) Combat.POWER_MULT else 1f)
            val dmg = base * (0.85f + rng.nextFloat() * 0.3f)
            damageEnemy(e, dmg, power, a)
            hitAny = true
        }
        if (hitAny) {
            shake = max(shake, if (power) 11f else 5f)
        }
    }

    private fun damageEnemy(e: Enemy, dmg: Float, power: Boolean, angle: Float) {
        e.hp -= dmg
        e.hitFlash = 0.16f
        e.knockX += cos(angle) * (if (power) 260f else 120f)
        e.knockY += sin(angle) * (if (power) 260f else 120f)
        texts.add(
            FloatingText(
                e.x + (rng.nextFloat() - 0.5f) * 10f, e.y - e.stats.radius * 2f,
                dmg.toInt().coerceAtLeast(1).toString(),
                0.85f, 0.85f,
                if (power) 0xFFF2B23A else 0xFFFFF3D6,
                big = power,
            )
        )
        repeat(if (power) 16 else 9) {
            val a = angle + (rng.nextFloat() - 0.5f) * 2.2f
            val sp = 90f + rng.nextFloat() * 180f
            particles.add(
                Particle(
                    e.x, e.y - e.stats.radius, cos(a) * sp, sin(a) * sp,
                    0.35f, 0.35f,
                    if (rng.nextFloat() < 0.5f) 0xFFFFF3D6 else 0xFFF2B23A,
                    2f + rng.nextFloat() * 3f, gravity = 140f,
                )
            )
        }
        if (e.hp <= 0f && e.state != EnemyState.DYING) killEnemy(e)
        else if (e.state == EnemyState.IDLE) e.state = EnemyState.CHASE
    }

    private fun killEnemy(e: Enemy) {
        e.state = EnemyState.DYING
        e.deathTimer = 0.55f
        player.kills += 1
        player.gold += e.stats.gold
        gainXp(e.stats.xp, e.x, e.y)
        texts.add(FloatingText(e.x + 14f, e.y - 34f, "+${e.stats.gold} GOLD", 1.1f, 1.1f, 0xFFF2B23A))
        repeat(24) {
            val a = rng.nextFloat() * 6.28f
            val sp = 30f + rng.nextFloat() * 110f
            particles.add(
                Particle(
                    e.x, e.y - e.stats.radius, cos(a) * sp, sin(a) * sp - 40f,
                    0.7f, 0.7f,
                    if (e.kind == EnemyKind.SLIME) 0xFF4FD1C5 else 0xFFF2B23A,
                    2f + rng.nextFloat() * 4f, gravity = -30f,
                )
            )
        }
    }

    private fun gainXp(amount: Int, x: Float, y: Float) {
        player.xp += amount
        texts.add(FloatingText(x - 14f, y - 52f, "+$amount XP", 1.1f, 1.1f, 0xFFEDE3D2))
        while (player.xp >= player.xpToNext) {
            player.xp -= player.xpToNext
            player.level += 1
            player.hp = player.maxHp
            levelUpGlow = 1.4f
            shake = max(shake, 9f)
            texts.add(FloatingText(player.x, player.y - 96f, "LEVEL ${player.level}", 1.8f, 1.8f, 0xFFF2B23A, big = true))
            repeat(70) {
                val a = rng.nextFloat() * 6.28f
                val sp = 130f + rng.nextFloat() * 190f
                particles.add(
                    Particle(
                        player.x, player.y - 24f, cos(a) * sp, sin(a) * sp * 0.6f,
                        1.0f, 1.0f,
                        if (rng.nextFloat() < 0.35f) 0xFFFFF3D6 else 0xFFF2B23A,
                        2f + rng.nextFloat() * 4f, gravity = -60f,
                    )
                )
            }
        }
        onProgress(player)
    }

    // ---------------------------------------------------------------- enemies

    private fun updateEnemies(dt: Float) {
        val p = player
        val it = enemies.iterator()
        while (it.hasNext()) {
            val e = it.next()
            e.hitFlash = max(0f, e.hitFlash - dt)
            e.bob += dt * (if (e.kind == EnemyKind.SLIME) 3.4f else 7f)

            // knockback decay
            e.x += e.knockX * dt
            e.y += e.knockY * dt
            e.knockX *= (1f - 8f * dt).coerceAtLeast(0f)
            e.knockY *= (1f - 8f * dt).coerceAtLeast(0f)

            if (e.state == EnemyState.DYING) {
                e.deathTimer -= dt
                if (e.deathTimer <= 0f) it.remove()
                continue
            }

            val d = dist(e.x, e.y, p.x, p.y)
            e.stateTimer -= dt

            when (e.state) {
                EnemyState.IDLE -> {
                    e.wanderTimer -= dt
                    if (e.wanderTimer <= 0f) {
                        e.wanderTimer = 1.2f + rng.nextFloat() * 2.2f
                        e.wanderAngle = rng.nextFloat() * 6.28f
                    }
                    if (dist(e.x, e.y, e.homeX, e.homeY) > 90f) {
                        e.wanderAngle = atan2(e.homeY - e.y, e.homeX - e.x)
                    }
                    val sp = e.stats.speed * 0.35f
                    e.x += cos(e.wanderAngle) * sp * dt
                    e.y += sin(e.wanderAngle) * sp * dt
                    if (cos(e.wanderAngle) != 0f) e.facingRight = cos(e.wanderAngle) > 0f
                    if (d < e.stats.aggroRange && p.deadTimer <= 0f) {
                        e.state = EnemyState.CHASE
                        if (e.kind == EnemyKind.WOLF) {
                            texts.add(FloatingText(e.x, e.y - 40f, "!", 0.7f, 0.7f, 0xFFE2542A))
                        }
                    }
                }

                EnemyState.CHASE -> {
                    if (p.deadTimer > 0f || d > e.stats.aggroRange * 1.9f) {
                        e.state = EnemyState.IDLE
                    } else {
                        val a = atan2(p.y - e.y, p.x - e.x)
                        val sp = e.stats.speed * (if (e.kind == EnemyKind.SLIME) (0.65f + 0.5f * abs(sin(e.bob))) else 1f)
                        e.x += cos(a) * sp * dt
                        e.y += sin(a) * sp * dt
                        e.facingRight = p.x > e.x
                        if (d < e.stats.attackRange) {
                            e.state = EnemyState.WINDUP
                            e.stateTimer = e.stats.attackWindup
                            e.attackLanded = false
                        }
                    }
                }

                EnemyState.WINDUP -> {
                    e.facingRight = p.x > e.x
                    if (e.kind == EnemyKind.WOLF) {
                        val a = atan2(p.y - e.y, p.x - e.x)
                        e.x += cos(a) * 60f * dt
                        e.y += sin(a) * 60f * dt
                    }
                    if (e.stateTimer <= 0f) {
                        if (!e.attackLanded) {
                            e.attackLanded = true
                            if (dist(e.x, e.y, p.x, p.y) < e.stats.attackRange + 14f) hurtPlayer(e)
                        }
                        e.state = EnemyState.RECOVER
                        e.stateTimer = e.stats.attackCooldown
                    }
                }

                EnemyState.RECOVER -> {
                    if (e.stateTimer <= 0f) e.state = EnemyState.CHASE
                }

                EnemyState.DYING -> Unit
            }

            e.x = e.x.coerceIn(30f, World.WIDTH - 30f)
            e.y = e.y.coerceIn(60f, World.HEIGHT - 30f)
        }

        separateEnemies()
    }

    /** Keep bodies from stacking on top of each other. */
    private fun separateEnemies() {
        for (i in enemies.indices) {
            val a = enemies[i]
            if (!a.alive) continue
            for (j in i + 1 until enemies.size) {
                val b = enemies[j]
                if (!b.alive) continue
                val dx = b.x - a.x
                val dy = (b.y - a.y) * 1.4f
                val d = hypot(dx, dy)
                val min = a.stats.radius + b.stats.radius
                if (d in 0.01f..min) {
                    val push = (min - d) * 0.5f
                    val nx = dx / d
                    val ny = dy / d
                    a.x -= nx * push; a.y -= ny * push
                    b.x += nx * push; b.y += ny * push
                }
            }
        }
    }

    private fun hurtPlayer(e: Enemy) {
        val p = player
        if (p.invuln > 0f || p.deadTimer > 0f) return
        val dmg = e.stats.damage * (0.9f + rng.nextFloat() * 0.2f)
        p.hp -= dmg
        p.hurtFlash = 0.35f
        p.invuln = 0.45f
        shake = max(shake, 8f)
        texts.add(FloatingText(p.x + 8f, p.y - 60f, "-${dmg.toInt()}", 0.8f, 0.8f, 0xFFC0392B))
        val a = atan2(p.y - e.y, p.x - e.x)
        repeat(10) {
            val aa = a + (rng.nextFloat() - 0.5f) * 1.6f
            particles.add(
                Particle(
                    p.x, p.y - 24f, cos(aa) * 120f, sin(aa) * 120f,
                    0.4f, 0.4f, 0xFFC0392B, 2f + rng.nextFloat() * 3f, gravity = 200f,
                )
            )
        }
        if (p.hp <= 0f) {
            p.hp = 0f
            p.deadTimer = 2.2f
            shake = 16f
            texts.add(FloatingText(p.x, p.y - 90f, "YOU FALL", 2.0f, 2.0f, 0xFFC0392B, big = true))
            onProgress(p)
        }
    }

    // ---------------------------------------------------------------- spawning

    private fun updateSpawns(dt: Float) {
        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnTimer = 2.2f + rng.nextFloat() * 2.0f
            if (enemies.count { it.alive } < Combat.MAX_ENEMIES) spawnEnemy(minDistance = 300f)
        }
    }

    private fun spawnEnemy(minDistance: Float) {
        val candidates = map.spawnPoints.filter { dist(it.first, it.second, player.x, player.y) > minDistance }
        val point = (if (candidates.isEmpty()) map.spawnPoints else candidates).random(rng)
        val wolfChance = 0.3f + (player.level - 1) * 0.05f
        val kind = if (rng.nextFloat() < wolfChance.coerceAtMost(0.6f)) EnemyKind.WOLF else EnemyKind.SLIME
        val x = (point.first + (rng.nextFloat() - 0.5f) * 60f).coerceIn(40f, World.WIDTH - 40f)
        val y = (point.second + (rng.nextFloat() - 0.5f) * 60f).coerceIn(80f, World.HEIGHT - 40f)
        enemies.add(Enemy(kind, x, y, x, y))
    }

    // ---------------------------------------------------------------- effects

    private fun updateEffects(dt: Float) {
        levelUpGlow = max(0f, levelUpGlow - dt)
        shake = max(0f, shake - dt * 34f)

        val pit = particles.iterator()
        while (pit.hasNext()) {
            val p = pit.next()
            p.life -= dt
            if (p.life <= 0f) { pit.remove(); continue }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += p.gravity * dt
            p.vx *= (1f - 2.4f * dt).coerceAtLeast(0f)
            p.vy *= (1f - 1.4f * dt).coerceAtLeast(0f)
        }

        val tit = texts.iterator()
        while (tit.hasNext()) {
            val t = tit.next()
            t.life -= dt
            if (t.life <= 0f) { tit.remove(); continue }
            t.y -= dt * (if (t.big) 16f else 34f)
        }

        for (f in map.fireflies) {
            f.phase += dt * f.speed
        }
    }

    private fun updateCamera(dt: Float) {
        val halfW = World.VIEW_WIDTH / 2f
        val targetX = player.x
        val targetY = player.y - 40f
        cameraX += (targetX - cameraX) * (1f - Math.exp(-9.0 * dt).toFloat())
        cameraY += (targetY - cameraY) * (1f - Math.exp(-9.0 * dt).toFloat())
        cameraX = cameraX.coerceIn(halfW, World.WIDTH - halfW)
    }

    private fun clampToWorld(p: Player) {
        p.x = p.x.coerceIn(36f, World.WIDTH - 36f)
        p.y = p.y.coerceIn(120f, World.HEIGHT - 40f)
    }

    fun applySave(save: SaveData) {
        player.level = save.level.coerceAtLeast(1)
        player.xp = save.xp.coerceAtLeast(0)
        player.gold = save.gold.coerceAtLeast(0)
        player.kills = save.kills.coerceAtLeast(0)
        player.hp = if (save.hp > 0f) save.hp.coerceAtMost(player.maxHp) else player.maxHp
        cameraX = player.x
        cameraY = player.y - 40f
    }

    fun saveNow() = onProgress(player)
}

fun facingVector(f: Facing): Pair<Float, Float> = when (f) {
    Facing.UP -> 0f to -1f
    Facing.DOWN -> 0f to 1f
    Facing.LEFT -> -1f to 0f
    Facing.RIGHT -> 1f to 0f
}

fun angleDiff(a: Float, b: Float): Float {
    var d = a - b
    while (d > Math.PI) d -= (Math.PI * 2).toFloat()
    while (d < -Math.PI) d += (Math.PI * 2).toFloat()
    return d
}
