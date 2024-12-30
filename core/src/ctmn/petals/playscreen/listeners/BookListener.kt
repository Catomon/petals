package ctmn.petals.playscreen.listeners

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.kotcrab.vis.ui.widget.VisLabel
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.ActionCompletedEvent
import ctmn.petals.playscreen.gui.widgets.Book.Companion.bookSave
import ctmn.petals.playscreen.selfName
import ctmn.petals.playscreen.seqactions.AttackAction
import ctmn.petals.unit.cMatchUp

class BookListener(private val playScreen: PlayScreen) : EventListener {
    override fun handle(event: Event): Boolean {
        when (event) {
            is ActionCompletedEvent -> {
                val action = event.action
                if (action is AttackAction) {
                    if (!bookSave.units.contains(action.attackerUnit.selfName)) bookSave.units.add(action.attackerUnit.selfName)
                    if (!bookSave.units.contains(action.targetUnit.selfName)) bookSave.units.add(action.targetUnit.selfName)

                    val matchups = bookSave.matchups[action.attackerUnit.selfName] ?: mutableSetOf()
                    if (!matchups.contains(action.targetUnit.selfName)) {
                        if (action.attackerUnit.cMatchUp?.containsKey(action.targetUnit.selfName) == true)
                            playScreen.guiStage.bookButton.findActor<VisLabel>("notify").setText("New")
                        matchups.add(action.targetUnit.selfName)
                        bookSave.matchups[action.attackerUnit.selfName] = matchups
                    }
                }
            }
        }

        return false
    }
}