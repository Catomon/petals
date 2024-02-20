package ctmn.petals.menu

import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.GamePref
import ctmn.petals.screens.MenuScreen
import ctmn.petals.getLangStringsByPrefs
import ctmn.petals.strings
import ctmn.petals.widgets.StageCover
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newTextButton

class LanguageSelectStage(private val menuScreen: MenuScreen) : Stage(menuScreen.viewport, menuScreen.batch) {

    private val english = newTextButton("English").addChangeListener {
        select("en")
    }

    private val russian = newTextButton("Russian").addChangeListener {
        select("ru")
    }

    private val spanish = newTextButton("Spanish").addChangeListener {
        select("es")
    }

    private val back = newTextButton("Back").addChangeListener {
        menuScreen.stage = menuScreen.menuStage
    }

    private val table = VisTable()

    init {
        addActor(StageCover().also { it.color.a = 0.7f })

        addActor(table)

        table.setFillParent(true)

        table.add(english).center()
        table.row()
        table.add(spanish).center()
        table.row()
        table.add(russian).center()
        table.row()
        table.add(back).center().padTop(10f)
    }

    private fun select(locale: String) {
        GamePref.locale = locale
        GamePref.save()

        strings = getLangStringsByPrefs()

        menuScreen.game.screen = MenuScreen(menuScreen.game)
    }
}