package net.robocode2.gui.model

class BotResults(
        val id: Int,
        val rank: Int,
        val survival: Int,
        val lastSurvivorBonus: Int,
        val bulletDamage: Int,
        val bulletKillBonus: Int,
        val ramDamage: Int,
        val ramKillBonus: Int,
        val totalScore: Int,
        val firstPlaces: Int,
        val secondPlaces: Int,
        val thirdPlaces: Int
) : Content(type = ContentType.BOT_RESULTS.type)
