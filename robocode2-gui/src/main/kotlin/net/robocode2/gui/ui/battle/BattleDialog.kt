package net.robocode2.gui.ui.battle

import net.robocode2.gui.extensions.WindowExt.onClosing
import net.robocode2.gui.ui.ResourceBundles
import java.awt.Dimension
import java.awt.EventQueue
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JTabbedPane
import javax.swing.UIManager

class BattleDialog(frame: JFrame? = null) : JDialog(frame, ResourceBundles.UI_TITLES.get("battle_dialog")) {

    private val tabbedPane = JTabbedPane()
    private val selectBotsPanel = SelectBotsPanel()
    private val setupRulesPanel = SetupRulesPanel()

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE

        size = Dimension(600,450)

        setLocationRelativeTo(null) // center on screen

        contentPane.add(tabbedPane)
        tabbedPane.addTab(ResourceBundles.UI_TITLES.get("select_bots_tab"), selectBotsPanel)
        tabbedPane.addTab(ResourceBundles.UI_TITLES.get("setup_rules_tab"), setupRulesPanel)

        tabbedPane.selectedComponent = setupRulesPanel

        onClosing {
            // Explicit cleanup in order to remove disposables on panels as finalize() seems to never be called
            selectBotsPanel.dispose()
            setupRulesPanel.dispose()
        }
    }

    fun selectBotsTab() {
        tabbedPane.selectedComponent = selectBotsPanel
    }

    fun selectSetupRulesTab() {
        tabbedPane.selectedComponent = setupRulesPanel
    }
}

fun main() {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

    EventQueue.invokeLater {
        BattleDialog().isVisible = true
    }
}