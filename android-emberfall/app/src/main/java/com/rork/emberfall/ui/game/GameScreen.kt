package com.rork.emberfall.ui.game

import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rork.emberfall.R
import com.rork.emberfall.game.Combat
import com.rork.emberfall.game.GameEngine
import com.rork.emberfall.game.SaveStore
import com.rork.emberfall.game.generateHollow
import com.rork.emberfall.ui.theme.Ember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GameScreen() {
    val context = LocalContext.current
    val saveStore = remember { SaveStore(context) }
    val pixelTypeface = remember {
        runCatching { ResourcesCompat.getFont(context, R.font.press_start_2p) }.getOrNull()
    }

    // The hollow is baked once on a background thread.
    val engineState = produceState<GameEngine?>(initialValue = null) {
        val map = withContext(Dispatchers.Default) { generateHollow() }
        val engine = GameEngine(map) { player -> saveStore.save(player) }
        engine.applySave(saveStore.load())
        engine.seedInitialEnemies()
        value = engine
    }
    val engine = engineState.value

    if (engine == null) {
        LoadingHollow()
        return
    }

    val tick = remember { mutableIntStateOf(0) }
    var hud by remember { mutableStateOf(engine.hud()) }
    var resting by remember { mutableStateOf(false) }

    LaunchedEffect(engine) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else (now - last) / 1_000_000_000f
                last = now
                engine.update(dt)
                tick.intValue++
                val snapshot = engine.hud()
                if (snapshot.hp != hud.hp || snapshot.maxHp != hud.maxHp || snapshot.level != hud.level ||
                    snapshot.xp != hud.xp || snapshot.gold != hud.gold || snapshot.kills != hud.kills ||
                    snapshot.dead != hud.dead
                ) {
                    hud = snapshot
                }
            }
        }
    }

    // Save and freeze the hollow whenever the app goes to the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, engine) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    engine.paused = true
                    engine.saveNow()
                }
                Lifecycle.Event.ON_RESUME -> if (!resting) engine.paused = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            engine.saveNow()
        }
    }

    Box(Modifier.fillMaxSize().background(Ember.Ink)) {
        // ------------------------------------------------ the world, full bleed
        Canvas(Modifier.fillMaxSize()) {
            tick.intValue // redraw every frame
            renderGame(engine, pixelTypeface)
        }

        // ------------------------------------------------ floating HUD
        Box(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            CombatHud(
                hp = hud.hp,
                maxHp = hud.maxHp,
                level = hud.level,
                gold = hud.gold,
                hpFraction = {
                    tick.intValue
                    engine.player.hp / engine.player.maxHp
                },
                xpFraction = {
                    tick.intValue
                    engine.player.xp.toFloat() / engine.player.xpToNext
                },
                modifier = Modifier.align(Alignment.TopStart),
            )

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                GoldCounter(gold = hud.gold)
                RestButton(onClick = {
                    resting = true
                    engine.paused = true
                    engine.saveNow()
                    hud = engine.hud()
                })
            }

            // ------------------------------------------------ controls
            Joystick(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 18.dp),
                onMove = { x, y ->
                    engine.moveX = x
                    engine.moveY = y
                },
            )

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 18.dp),
            ) {
                AbilityButton(
                    label = "ATTACK",
                    glyph = AbilityGlyph.SWORD,
                    accent = Ember.Fire,
                    diameter = 82,
                    cooldownFraction = {
                        tick.intValue
                        (engine.player.attackCd / Combat.ATTACK_CD).coerceIn(0f, 1f)
                    },
                    onPress = { engine.requestAttack() },
                    modifier = Modifier.padding(end = 6.dp, bottom = 10.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AbilityButton(
                        label = "POWER",
                        glyph = AbilityGlyph.POWER,
                        accent = Ember.Blood,
                        diameter = 70,
                        cooldownFraction = {
                            tick.intValue
                            (engine.player.powerCd / Combat.POWER_CD).coerceIn(0f, 1f)
                        },
                        onPress = { engine.requestPower() },
                    )
                    AbilityButton(
                        label = "DODGE",
                        glyph = AbilityGlyph.DODGE,
                        accent = Ember.Spectral,
                        diameter = 70,
                        cooldownFraction = {
                            tick.intValue
                            (engine.player.dodgeCd / Combat.DODGE_CD).coerceIn(0f, 1f)
                        },
                        onPress = { engine.requestDodge() },
                    )
                }
            }
        }

        // ------------------------------------------------ rest panel
        AnimatedVisibility(
            visible = resting,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Ember.Ink.copy(alpha = 0.72f))
                    .pointerInput(Unit) { }
            )
        }
        AnimatedVisibility(
            visible = resting,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(Modifier.systemBarsPadding().padding(bottom = 12.dp)) {
                RestPanel(
                    hud = hud,
                    onResume = {
                        resting = false
                        engine.paused = false
                    },
                )
            }
        }
    }
}

@Composable
private fun RestButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .background(Ember.Ink.copy(alpha = 0.7f))
            .border(2.dp, Ember.Bark)
            .semantics { contentDescription = "Rest and view progress" }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onClick()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp)) {
            drawRect(Ember.Bone, topLeft = Offset(0f, 0f), size = Size(size.width * 0.32f, size.height))
            drawRect(
                Ember.Bone,
                topLeft = Offset(size.width * 0.62f, 0f),
                size = Size(size.width * 0.32f, size.height),
            )
        }
    }
}

@Composable
private fun LoadingHollow() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Ember.Ink),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(60.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(Ember.Lantern.copy(alpha = 0.35f), size.minDimension / 2f, c, style = Stroke(width = 4f))
                drawRect(
                    Ember.Lantern,
                    topLeft = Offset(c.x - 6f, c.y - 18f),
                    size = Size(12f, 36f),
                )
            }
            Text(
                "EMBERFALL",
                style = MaterialTheme.typography.titleLarge,
                color = Ember.Lantern,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                "kindling the hollow",
                style = MaterialTheme.typography.bodySmall,
                color = Ember.BoneDim,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
