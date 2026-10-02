package ctmn.petals.menu

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.GamePref
import ctmn.petals.player.fairy
import ctmn.petals.player.goblin
import ctmn.petals.screens.MenuScreen
import ctmn.petals.strings
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newLabel
import ctmn.petals.widgets.newTextButton

/**
 * The player's own name and species, kept between sessions.
 *
 * The name is what other players see in a lobby, and the species is what a lobby seats this
 * player as. Neither is fixed here: the host can still change anybody's species in the room, and
 * a player can change their own there too.
 */
class ProfileStage(private val menuScreen: MenuScreen) :
    Stage(menuScreen.viewport, menuScreen.batch) {

    private val nameField = VisTextField().apply { messageText = "Name" }

    private val speciesBox = object : VisSelectBox<String>() {
        override fun toString(item: String?): String = when (item) {
            goblin -> "Goblin"
            else -> "Faerie"
        }
    }.apply {
        items = Array<String>().apply {
            add(fairy)
            add(goblin)
        }
    }

    private val returnButton = newTextButton(strings.general.return_)
    private val confirmButton = newTextButton(strings.general.confirm)

    private val table = VisTable()
    private val windowTable = VisTable()

    init {
        nameField.text = GamePref.displayName
        speciesBox.selected = GamePref.species

        returnButton.addChangeListener { menuScreen.stage = menuScreen.menuStage }
        confirmButton.addChangeListener {
            GamePref.displayName = nameField.text
            GamePref.species = speciesBox.selected
            GamePref.save()

            menuScreen.stage = menuScreen.menuStage
        }

        table.setFillParent(true)
        table.center()

        with(table) {
            add(newLabel(strings.general.profile, "default")).padBottom(12f)
            row()

            add(VisTable().apply {
                add(newLabel("Name", "default")).padRight(6f)
                add(nameField).width(200f)
            }).row()

            add(VisTable().apply {
                add(newLabel("Species", "default")).padRight(6f)
                add(speciesBox).width(200f)
            }).padTop(8f).row()

            add(VisTable().apply {
                add(returnButton).width(140f).padRight(6f)
                add(confirmButton).width(140f)
            }).padTop(20f)
        }

        addActor(table)

        windowTable.setFillParent(true)
        addActor(windowTable)
    }

    override fun addActor(actor: Actor?) {
        if (actor is VisWindow) {
            actor.isMovable = false
            windowTable.clear()
            windowTable.add(actor).expand().center().maxWidth(viewport.worldWidth - 10f)
            return
        }

        super.addActor(actor)
    }
}
