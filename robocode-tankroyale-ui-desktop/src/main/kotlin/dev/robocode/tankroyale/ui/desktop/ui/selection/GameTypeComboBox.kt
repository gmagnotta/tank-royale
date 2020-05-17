package dev.robocode.tankroyale.ui.desktop.ui.selection

import dev.robocode.tankroyale.ui.desktop.settings.GameType
import dev.robocode.tankroyale.ui.desktop.settings.GamesSettings
import javax.swing.JComboBox

class GameTypeComboBox : JComboBox<String>(GamesSettings.games.keys.toTypedArray()) {

    val selectedGameType: String get() = (selectedItem ?: GameType.CLASSIC.displayName) as String

    fun setSelectedGameType(gameType: GameType) {
        model.selectedItem = gameType.displayName
    }
}