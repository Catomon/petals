package ctmn.petals.playscreen.commands

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.UnitDiedEvent
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playscreen.queueAction
import ctmn.petals.playscreen.seqactions.KillUnitAction
import ctmn.petals.playscreen.stageName
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.health

class KillUnitCommand(val unitId: String) : Command() {

    constructor(unitActor: UnitActor) : this(unitActor.stageName)

    override fun canExecute(playScreen: PlayScreen): Boolean {
        val unit = playScreen.playStage.root.findActor<UnitActor>(unitId) ?: return false

        return unit.playStageOrNull != null
    }

    override fun execute(playScreen: PlayScreen): Boolean {
        val unit = playScreen.playStage.root.findActor<UnitActor>(unitId) ?: return false

        unit.health = 0

        unit.remove()

        playScreen.queueAction(KillUnitAction(unit))

        playScreen.playStage.root.fire(UnitDiedEvent(unit))

        return true
    }
}