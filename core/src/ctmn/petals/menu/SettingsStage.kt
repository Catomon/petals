package ctmn.petals.menu

import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.screens.MenuScreen
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newTextButton

class SettingsStage(private val menuScreen: MenuScreen) : Stage(menuScreen.viewport, menuScreen.batch) {

    private val langButton = newTextButton("Language")
    private val table = VisTable()

    private val returnButton = newTextButton("Return")

    init {
        table.setFillParent(true)
        with(table) {
            bottom()

            add(langButton)
            row()
            add(returnButton).padTop(8f)

            padBottom(30f)
        }
        addActor(table)

        langButton.addChangeListener {
            menuScreen.stage = menuScreen.languageSelectStage
        }
        returnButton.addChangeListener {
            menuScreen.stage = menuScreen.menuStage
        }
    }
}