package com.rork.emberfall.game

import kotlin.math.hypot

object World {
    const val WIDTH = 1100f
    const val HEIGHT = 1900f

    /** How many world units fit across the screen. Smaller = more zoomed in. */
    const val VIEW_WIDTH = 360f

    /** Pixel scale of authored sprites when drawn into the world. */
    const val SPRITE_SCALE = 3f
}

enum class Facing { UP, DOWN, LEFT, RIGHT }

enum class EnemyKind { SLIME, WOLF }

data class EnemyStats(
    val maxHp: Float,
    val damage: Float,
    val speed: Float,
    val aggroRange: Float,
    val attackRange: Float,
    val attackWindup: Float,
    val attackCooldown: Float,
    val xp: Int,
    val gold: Int,
    val radius: Float,
)

val slimeStats = EnemyStats(
    maxHp = 24f, damage = 6f, speed = 34f, aggroRange = 150f, attackRange = 26f,
    attackWindup = 0.45f, attackCooldown = 1.5f, xp = 12, gold = 3, radius = 15f,
)

val wolfStats = EnemyStats(
    maxHp = 38f, damage = 10f, speed = 92f, aggroRange = 260f, attackRange = 32f,
    attackWindup = 0.3f, attackCooldown = 1.1f, xp = 24, gold = 7, radius = 20f,
)

fun statsFor(kind: EnemyKind): EnemyStats = if (kind == EnemyKind.SLIME) slimeStats else wolfStats

enum class EnemyState { IDLE, CHASE, WINDUP, RECOVER, DYING }

class Enemy(
    val kind: EnemyKind,
    var x: Float,
    var y: Float,
    val homeX: Float,
    val homeY: Float,
) {
    val stats: EnemyStats = statsFor(kind)
    var hp: Float = stats.maxHp
    var state: EnemyState = EnemyState.IDLE
    var stateTimer: Float = 0f
    var facingRight: Boolean = true
    var hitFlash: Float = 0f
    var deathTimer: Float = 0f
    var wanderAngle: Float = 0f
    var wanderTimer: Float = 0f
    var bob: Float = (x + y) % 6.28f
    var knockX: Float = 0f
    var knockY: Float = 0f
    var attackLanded: Boolean = false

    val alive: Boolean get() = state != EnemyState.DYING
}

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Long,
    val size: Float,
    val gravity: Float = 0f,
)

class FloatingText(
    var x: Float,
    var y: Float,
    val text: String,
    var life: Float,
    val maxLife: Float,
    val color: Long,
    val big: Boolean = false,
)

class Firefly(var x: Float, var y: Float, var phase: Float, var speed: Float, var radius: Float)

class Prop(val kind: PropKind, val x: Float, val y: Float, val seed: Int)

enum class PropKind { LANTERN, MUSHROOM, GRASS }

class Player {
    var x: Float = World.WIDTH / 2f
    var y: Float = World.HEIGHT - 260f
    var facing: Facing = Facing.UP
    var hp: Float = 80f
    var level: Int = 1
    var xp: Int = 0
    var gold: Int = 0
    var kills: Int = 0

    var attackCd: Float = 0f
    var powerCd: Float = 0f
    var dodgeCd: Float = 0f

    var swingTimer: Float = 0f
    var swingDuration: Float = 0f
    var swingIsPower: Boolean = false
    var swingAngle: Float = 0f
    var swingHitDone: Boolean = false

    var dodgeTimer: Float = 0f
    var dodgeDirX: Float = 0f
    var dodgeDirY: Float = 0f

    var hurtFlash: Float = 0f
    var invuln: Float = 0f
    var walkPhase: Float = 0f
    var moving: Boolean = false
    var deadTimer: Float = 0f

    val maxHp: Float get() = 80f + 20f * (level - 1)
    val attackPower: Float get() = 9f + 2.5f * (level - 1)
    val xpToNext: Int get() = 40 + level * 16

    fun reset() {
        hp = maxHp
        x = World.WIDTH / 2f
        y = World.HEIGHT - 260f
        facing = Facing.UP
        attackCd = 0f; powerCd = 0f; dodgeCd = 0f
        swingTimer = 0f; dodgeTimer = 0f; invuln = 1.2f; deadTimer = 0f
    }
}

fun dist(ax: Float, ay: Float, bx: Float, by: Float): Float = hypot(bx - ax, by - ay)
