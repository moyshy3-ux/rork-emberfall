package com.rork.emberfall.game

import android.content.Context

data class SaveData(
    val level: Int = 1,
    val xp: Int = 0,
    val gold: Int = 0,
    val kills: Int = 0,
    val hp: Float = 0f,
)

/** Local-only progress, written to SharedPreferences. */
class SaveStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("emberfall_progress", Context.MODE_PRIVATE)

    fun load(): SaveData = SaveData(
        level = prefs.getInt(KEY_LEVEL, 1),
        xp = prefs.getInt(KEY_XP, 0),
        gold = prefs.getInt(KEY_GOLD, 0),
        kills = prefs.getInt(KEY_KILLS, 0),
        hp = prefs.getFloat(KEY_HP, 0f),
    )

    fun save(player: Player) {
        prefs.edit()
            .putInt(KEY_LEVEL, player.level)
            .putInt(KEY_XP, player.xp)
            .putInt(KEY_GOLD, player.gold)
            .putInt(KEY_KILLS, player.kills)
            .putFloat(KEY_HP, player.hp)
            .apply()
    }

    private companion object {
        const val KEY_LEVEL = "level"
        const val KEY_XP = "xp"
        const val KEY_GOLD = "gold"
        const val KEY_KILLS = "kills"
        const val KEY_HP = "hp"
    }
}
